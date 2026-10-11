package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 配方类内置数据包的生产编排核心（自 {@link RecipePackCoordinator} 迁移，语义不变）。
 *
 * <p><b>服务器身份令牌与钩子引用分离</b>：{@code token} 仅用于身份比较（绑定 / 换服 / 迟到事件
 * 判定），{@code hookServerRef} 仅透传给 {@link RecipePackCoordinator.ManagedPack} 钩子。生产中
 * 二者为同一对象；单元测试以互异 {@code Object} 令牌区分不同服务器身份、以 {@code null} 作为
 * 钩子引用（测试钩子不解引用）。所有生命周期判定统一基于 {@code bound} 绑定状态与令牌比较，
 * 不做 null 判空短路——因此 {@code null} 令牌亦可驱动完整绑定路径。
 *
 * <p><b>单一所有者</b>：服务器绑定状态与 epoch 防护（经独占持有的 {@link RecipeReloadGate}）
 * 全部归本类；门面不持有任何绑定 / 身份 / epoch 副本。{@link ServerAdapter} 的目标服务器镜像
 * 只在本类的绑定迁移 / 解绑成功时点同步。
 *
 * <p>状态归属、静默点、重试与收敛语义见 {@link RecipeReloadGate} 与
 * {@link RecipePackCoordinator}（Fabric 事件接线与注册在门面，本类不触碰 Fabric API）。
 * 全部状态由服务器主线程持有；每个钩子独立 try/catch，规则间异常隔离。
 */
final class RecipePackOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private final List<RecipePackCoordinator.ManagedPack> packs;
    private final ServerAdapter adapter;
    private final RecipeReloadGate gate = new RecipeReloadGate();

    private boolean bound;
    private Object boundToken;
    private MinecraftServer hookServerRef;
    private boolean offThreadRequestWarned;

    RecipePackOrchestrator(List<RecipePackCoordinator.ManagedPack> packs, ServerAdapter adapter) {
        this.packs = List.copyOf(packs);
        this.adapter = adapter;
    }

    // ------------------------------------------------------------------ 生命周期入口

    void onServerLoadedWorlds(Object token, MinecraftServer hookServer) {
        bind(token, hookServer);
        // 欠下首次静默点：即使本次判定无需 reload，也要消费 pendingSyncPass，否则 ready 永远为 false。
        gate.markWorldsLoaded();
        orchestrate();
    }

    /** @return true 表示该规则名受管理（门面据此结束 observer 分支）；未知规则不绑定、不更新目标。 */
    boolean onRuleChanged(String ruleName, Object token, MinecraftServer hookServer) {
        if (findByRule(ruleName) == null) {
            return false;
        }
        bind(token, hookServer);
        gate.onDesiredChanged();
        // 静默状态下必须在这里把未完成的 pass 跑掉：不能只依赖 END 事件。
        if (orchestrate() == RecipeReloadGate.QuiescenceAction.NONE) {
            // 规则变化因仍有 reload 在进行而推迟：只诊断，等最后一个 END 的静默点收敛。
            deferralDiagnostic(gate.epoch());
        }
        return true;
    }

    /**
     * 逐包登录同步：锁定提示经 {@code lockedMessageKeySink} 回调（翻译与发送由门面组装），
     * 同步钩子按包独立判定（见 {@link #shouldSyncPackOnJoin}）。
     *
     * <p>核心方法不做 player null 短路（null 校验在门面），使单测可以 null player 驱动生产编排。
     */
    void onPlayerLoggedIn(ServerPlayer player, Consumer<String> lockedMessageKeySink) {
        if (!bound) {
            return;
        }
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            if (safeBool(pack.ruleName(), pack.locked())) {
                lockedMessageKeySink.accept(pack.lockedMessageKey());
            }
        }
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            // 逐包判定：本包未收敛时不同步，其它规则的失败 / 迁移与本包无关。
            if (shouldSyncPackOnJoin(pack)) {
                safeRun(pack.ruleName(), () -> pack.onPlayerJoin().accept(hookServerRef, player));
            }
        }
    }

    /** 服务器关闭：令牌匹配才解绑；重复关闭幂等，迟到 END 由 {@code !bound} / 令牌比较忽略。 */
    void onServerClosed(Object token) {
        if (!bound || token != boundToken) {
            return;
        }
        bound = false;
        boundToken = null;
        hookServerRef = null;
        gate.closeServer();
        offThreadRequestWarned = false;
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            safeRun(pack.ruleName(), pack.resetLockState());
        }
        adapter.updateTarget(null);
    }

    // ------------------------------------------------------------------ 查询

    /** 当前是否仍有无法归属的进行中 reload（诊断用；初始化前的安全语义由门面保证）。 */
    boolean isReconcileInFlight() {
        return gate.inFlightCount() > 0;
    }

    /**
     * 状态机只读视图（包私有，供单测断言结算 / epoch / 预算语义；不提供任何可变访问）。
     * {@link RecipeReloadGate} 自身的公开读方法即为其单测/诊断视图。
     */
    RecipeReloadGate gate() {
        return gate;
    }

    // ------------------------------------------------------------------ 生命周期事件

    void onReloadStart(Object token, MinecraftServer hookServer) {
        bind(token, hookServer);
        gate.onReloadStart(gate.epoch());
    }

    void onReloadEnd(Object token, boolean success) {
        if (!bound || token != boundToken) {
            return;
        }
        RecipeReloadGate.EndKind kind = gate.onReloadEnd(gate.epoch(), success);
        if (kind == RecipeReloadGate.EndKind.ANOMALOUS) {
            // 无匹配 START：只诊断，不改结局、不耗预算、不跑静默点、不触发 reload。
            if (gate.noteAnomalousEndAndShouldWarn(gate.epoch())) {
                LOGGER.warn("[Carpet Ice Addition] Unmatched datapack reload END observed; ignoring it "
                        + "(no matching START, inflight={})", gate.inFlightCount());
            }
            return;
        }
        if (kind == RecipeReloadGate.EndKind.IGNORED_STALE) {
            return;
        }
        if (!success) {
            LOGGER.warn("[Carpet Ice Addition] Datapack reload reported failure; rule state left unchanged "
                    + "until the owning rule settles its own request");
        }
        orchestrate();
        if (gate.inFlightCount() > 0) {
            // 本次 END 后仍有 reload 在进行：静默点被推迟，不在此处做冲突重算与菜单同步。
            deferralDiagnostic(gate.epoch());
        }
    }

    // ------------------------------------------------------------------ 静默点编排

    private RecipeReloadGate.QuiescenceAction orchestrate() {
        if (!bound) {
            return RecipeReloadGate.QuiescenceAction.NONE;
        }
        long epoch = gate.epoch();
        RecipeReloadGate.QuiescenceAction action = gate.quiescenceAction(epoch);
        switch (action) {
            case NONE -> {
            }
            case RUN_PASS_AND_SCHEDULE -> {
                gate.onPassFinished(runPass());
                scheduleNext(epoch);
            }
            case SCHEDULE -> scheduleNext(epoch);
        }
        return action;
    }

    /**
     * 静默点同步 pass：先全部重算冲突（锁定状态必须先落定），再全部同步菜单/配方书。
     *
     * @return 全部钩子均未抛异常
     */
    private boolean runPass() {
        boolean allOk = true;
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            allOk &= safeRun(pack.ruleName(), () -> pack.recomputeConflict().accept(hookServerRef));
        }
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            allOk &= safeRun(pack.ruleName(), () -> pack.syncMenus().accept(hookServerRef));
        }
        return allOk;
    }

    private void scheduleNext(long epoch) {
        boolean forceRetry = gate.consumeRetryRequest();
        requestReconcile(epoch, forceRetry);
    }

    private void requestReconcile(long epoch, boolean forceRetry) {
        if (!bound || epoch != gate.epoch()) {
            return;
        }
        if (!gate.isQuiet(epoch)) {
            deferralDiagnostic(epoch);
            return;
        }
        if (gate.isDegraded() && !forceRetry) {
            return;
        }
        if (!adapter.isServerThread()) {
            // reloadResources 只允许在服务器线程调用；拒不建立请求账，避免留下无法结算的 ownOp。
            if (!offThreadRequestWarned) {
                offThreadRequestWarned = true;
                LOGGER.warn("[Carpet Ice Addition] Refused datapack reconcile off the server thread; "
                        + "no reload request was created");
            }
            return;
        }

        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(adapter.selectedPackIds(), desires());
        if (!plan.changed()) {
            gate.onPlanEvaluated(false);
            return;
        }
        if (gate.isNoProgress(plan.targetKey())) {
            // 成功结局 + 同目标 + 无期望变化：目标未被实际接受，停止重复尝试而不是无限 reload。
            gate.markNoProgressStop();
            LOGGER.warn("[Carpet Ice Addition] Datapack selection did not converge to the requested set {}; "
                    + "stopping automatic reloads until the rule state changes again", plan.next());
            return;
        }
        if (!gate.canStartReload(epoch, forceRetry)) {
            return;
        }

        // onPackDisabled 属于即时清理，必须先于改选执行（与既有控制器时序一致）。
        for (String packId : plan.disabledPackIds()) {
            RecipePackCoordinator.ManagedPack pack = findById(packId);
            if (pack != null) {
                safeRun(pack.ruleName(), () -> pack.onPackDisabled().accept(hookServerRef));
            }
        }

        gate.onPlanEvaluated(true);
        startReload(plan, epoch);
    }

    private void startReload(RecipePackReconciler.Plan plan, long epoch) {
        if (!gate.markRequestStarted(plan.targetKey(), epoch, true)) {
            return;
        }
        CompletableFuture<Void> future;
        try {
            future = adapter.reloadResources(plan.next());
        } catch (Throwable throwable) {
            // START 注入在 HEAD、END 注入在 TAIL：同步异常意味着该次 reload 永远不会收到 END，
            // 由状态机按 ownStartPending 判定是否需要补偿递减。
            gate.settleSyncThrow(epoch);
            LOGGER.warn("[Carpet Ice Addition] Datapack reload failed synchronously", throwable);
            orchestrate();
            return;
        }
        boolean exceptional = future.isCompletedExceptionally() || future.isCancelled();
        gate.settleReturnedFuture(epoch, future.isDone(), exceptional);
        orchestrate();
    }

    // ------------------------------------------------------------------ 辅助

    /**
     * 绑定 / 换服：首次绑定与令牌更换均执行完整重置（epoch 推进 + 全部包锁状态复位 + 目标同步）；
     * 同令牌重复绑定跳过。生产语义与迁移前一致（真实 server 非 null 时判空结论等价）。
     */
    private void bind(Object token, MinecraftServer hookServer) {
        if (bound && token == boundToken) {
            return;
        }
        bound = true;
        boundToken = token;
        hookServerRef = hookServer;
        gate.bindServer();
        offThreadRequestWarned = false;
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            safeRun(pack.ruleName(), pack.resetLockState());
        }
        adapter.updateTarget(hookServer);
    }

    /**
     * 逐包登录同步门：只取决于「本包自身的选中状态是否已收敛到期望」。
     *
     * <p>刻意不使用全局 {@code ready}：该标志要求全部规则的钩子成功且目标集合全部收敛，任一条规则的
     * 失败 / 迁移都会误压制另一条未出错规则的登录同步。
     */
    private boolean shouldSyncPackOnJoin(RecipePackCoordinator.ManagedPack pack) {
        if (!gate.isWorldsLoaded()) {
            return false;
        }
        boolean desired = safeBool(pack.ruleName(), pack.desired());
        return RecipePackReconciler.packConverged(adapter.selectedPackIds(), pack.packIdString(), desired);
    }

    private List<RecipePackReconciler.PackDesire> desires() {
        List<RecipePackReconciler.PackDesire> list = new ArrayList<>(packs.size());
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            list.add(new RecipePackReconciler.PackDesire(
                    pack.packIdString(), pack.ruleName(), safeBool(pack.ruleName(), pack.desired())));
        }
        return list;
    }

    private void deferralDiagnostic(long epoch) {
        if (gate.inFlightCount() > 0 && gate.noteDeferredAndShouldWarn(epoch)) {
            LOGGER.warn("[Carpet Ice Addition] Datapack reconcile deferred: {} reload(s) still in flight; "
                    + "no forced counter reset is performed", gate.inFlightCount());
        }
    }

    private RecipePackCoordinator.ManagedPack findByRule(String ruleName) {
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            if (pack.ruleName().equals(ruleName)) {
                return pack;
            }
        }
        return null;
    }

    private RecipePackCoordinator.ManagedPack findById(String packId) {
        for (RecipePackCoordinator.ManagedPack pack : packs) {
            if (pack.packIdString().equals(packId)) {
                return pack;
            }
        }
        return null;
    }

    /** 单规则钩子异常隔离：异常只上报并按失败返回，不影响其它规则与编排记账。 */
    private boolean safeRun(String ruleName, Runnable action) {
        try {
            action.run();
            return true;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue(ruleName, throwable);
            return false;
        }
    }

    private boolean safeBool(String ruleName, BooleanSupplier supplier) {
        try {
            return supplier.getAsBoolean();
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue(ruleName, throwable);
            return false;
        }
    }
}
