package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//#if MC>=12111
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
//#else
//$$import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
//$$import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
//#endif
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 配方类内置数据包的共享协调器：全部「配方数据包规则」共用**一条** reload 管线。
 *
 * <p>为什么不每个规则一份控制器：Fabric 的 START/END 是全局事件，两份控制器会互相抢占
 * {@code ownReloadStart} 并各自发起 reload，导致状态抢占与 reload 放大。因此计数、静默点、
 * 重试预算与收敛判定全部集中在本类，规则只提供钩子（{@link ManagedPack}）。
 *
 * <p>状态归属（详见 {@link RecipeReloadGate}）：
 * <ul>
 *   <li>全局事件层只决定「是否静默」；全局 END 不改写自有请求结局、不消耗重试预算；</li>
 *   <li>自有请求结局只由本次 {@code reloadResources(...)} 的动作结算；</li>
 *   <li>冲突重算与菜单/配方书同步**只允许在静默点**执行；</li>
 *   <li>重试只由静默点单一路径启动；</li>
 *   <li>{@code reloadResources} 只在服务器线程调用，非服务器线程请求不建立请求账。</li>
 * </ul>
 *
 * <p>全部状态由服务器主线程持有；每个钩子独立 try/catch，保证规则间异常隔离。
 */
public final class RecipePackCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    /** 受管内置数据包：规则侧只需提供数据与钩子，不接触 reload 生命周期。 */
    public record ManagedPack(
            Identifier packId,
            String ruleName,
            String lockedMessageKey,
            BooleanSupplier locked,
            BooleanSupplier desired,
            Consumer<MinecraftServer> recomputeConflict,
            Consumer<MinecraftServer> syncMenus,
            Consumer<MinecraftServer> onPackDisabled,
            BiConsumer<MinecraftServer, ServerPlayer> onPlayerJoin,
            Runnable resetLockState) {

        public String packIdString() {
            return packId.toString();
        }
    }

    private static final List<ManagedPack> PACKS = new ArrayList<>();
    private static final RecipeReloadGate GATE = new RecipeReloadGate();

    private static boolean initialized;
    private static MinecraftServer boundServer;
    private static boolean offThreadRequestWarned;

    private RecipePackCoordinator() {
    }

    // ------------------------------------------------------------------ 注册与初始化

    /** 注册一个受管内置数据包（必须在 {@link #initialize()} 之前调用）。 */
    public static void register(ManagedPack pack) {
        if (initialized) {
            throw new IllegalStateException("RecipePackCoordinator already initialized");
        }
        PACKS.add(pack);
    }

    /** 注册全部内置数据包并挂载唯一的 START/END 监听器（幂等）。 */
    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        var container = FabricLoader.getInstance()
                .getModContainer(CarpetIceAdditionMod.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing " + CarpetIceAdditionMod.MOD_ID + " mod container"));
        for (ManagedPack pack : PACKS) {
            //#if MC>=12111
            boolean registered = ResourceLoader.registerBuiltinPack(pack.packId(), container, PackActivationType.NORMAL);
            //#else
            //$$            boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(pack.packId(), container, ResourcePackActivationType.NORMAL);
            //#endif
            if (!registered) {
                LOGGER.warn("[Carpet Ice Addition] Failed to register builtin datapack {} for rule {}",
                        pack.packId(), pack.ruleName());
            }
        }
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register(
                (minecraftServer, ignored) -> onReloadStart(minecraftServer));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (minecraftServer, ignored, success) -> onReloadEnd(minecraftServer, success));
    }

    // ------------------------------------------------------------------ 生命周期入口

    public static void onServerLoadedWorlds(MinecraftServer server) {
        if (server == null) {
            return;
        }
        bind(server);
        // 欠下首次静默点：即使本次判定无需 reload，也要消费 pendingSyncPass，否则 ready 永远为 false。
        GATE.markWorldsLoaded();
        orchestrate();
    }

    /** @return true 表示该规则名由本协调器接管（入口类据此结束 observer 分支） */
    public static boolean onRuleChanged(String ruleName, MinecraftServer server) {
        if (server == null || ruleName == null) {
            return false;
        }
        if (findByRule(ruleName) == null) {
            return false;
        }
        bind(server);
        GATE.onDesiredChanged();
        // 静默状态下必须在这里把未完成的 pass 跑掉：不能只依赖 END 事件。
        if (orchestrate() == RecipeReloadGate.QuiescenceAction.NONE) {
            // 规则变化因仍有 reload 在进行而推迟：只诊断，等最后一个 END 的静默点收敛。
            deferralDiagnostic(GATE.epoch());
        }
        return true;
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        MinecraftServer server = boundServer;
        if (server == null || player == null) {
            return;
        }
        for (ManagedPack pack : PACKS) {
            if (safeBool(pack.ruleName(), pack.locked())) {
                player.sendSystemMessage(Component.literal(TranslationFormatUtil.translate(pack.lockedMessageKey())));
            }
        }
        if (!GATE.isReady()) {
            return;
        }
        for (ManagedPack pack : PACKS) {
            safeRun(pack.ruleName(), () -> pack.onPlayerJoin().accept(server, player));
        }
    }

    public static void onServerClosed(MinecraftServer server) {
        if (server == null || boundServer != server) {
            return;
        }
        boundServer = null;
        GATE.closeServer();
        offThreadRequestWarned = false;
        for (ManagedPack pack : PACKS) {
            safeRun(pack.ruleName(), pack.resetLockState());
        }
    }

    // ------------------------------------------------------------------ 查询

    /** 判断某个内置数据包当前是否被选中（供同 ID 覆盖检测使用）。 */
    public static boolean isBuiltinPackSelected(MinecraftServer server, String packId) {
        return server != null && server.getPackRepository().getSelectedIds().contains(packId);
    }

    /** 当前是否仍有无法归属的进行中 reload（诊断用；保守停摆边界）。 */
    public static boolean isReconcileInFlight() {
        return GATE.inFlightCount() > 0;
    }

    // ------------------------------------------------------------------ 生命周期事件

    private static void onReloadStart(MinecraftServer server) {
        if (server == null) {
            return;
        }
        bind(server);
        GATE.onReloadStart(GATE.epoch());
    }

    private static void onReloadEnd(MinecraftServer server, boolean success) {
        if (server == null || boundServer != server) {
            return;
        }
        RecipeReloadGate.EndKind kind = GATE.onReloadEnd(GATE.epoch(), success);
        if (kind == RecipeReloadGate.EndKind.ANOMALOUS) {
            // 无匹配 START：只诊断，不改结局、不耗预算、不跑静默点、不触发 reload。
            if (GATE.noteAnomalousEndAndShouldWarn(GATE.epoch())) {
                LOGGER.warn("[Carpet Ice Addition] Unmatched datapack reload END observed; ignoring it "
                        + "(no matching START, inflight={})", GATE.inFlightCount());
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
        if (GATE.inFlightCount() > 0) {
            // 本次 END 后仍有 reload 在进行：静默点被推迟，不在此处做冲突重算与菜单同步。
            deferralDiagnostic(GATE.epoch());
        }
    }

    // ------------------------------------------------------------------ 静默点编排

    private static RecipeReloadGate.QuiescenceAction orchestrate() {
        MinecraftServer server = boundServer;
        if (server == null) {
            return RecipeReloadGate.QuiescenceAction.NONE;
        }
        long epoch = GATE.epoch();
        RecipeReloadGate.QuiescenceAction action = GATE.quiescenceAction(epoch);
        switch (action) {
            case NONE -> {
            }
            case RUN_PASS_AND_SCHEDULE -> {
                GATE.onPassFinished(runPass(server));
                scheduleNext(server, epoch);
            }
            case SCHEDULE -> scheduleNext(server, epoch);
        }
        return action;
    }

    /**
     * 静默点同步 pass：先全部重算冲突（锁定状态必须先落定），再全部同步菜单/配方书。
     *
     * @return 全部钩子均未抛异常
     */
    private static boolean runPass(MinecraftServer server) {
        boolean allOk = true;
        for (ManagedPack pack : PACKS) {
            allOk &= safeRun(pack.ruleName(), () -> pack.recomputeConflict().accept(server));
        }
        for (ManagedPack pack : PACKS) {
            allOk &= safeRun(pack.ruleName(), () -> pack.syncMenus().accept(server));
        }
        return allOk;
    }

    private static void scheduleNext(MinecraftServer server, long epoch) {
        boolean forceRetry = GATE.consumeRetryRequest();
        requestReconcile(server, epoch, forceRetry);
    }

    private static void requestReconcile(MinecraftServer server, long epoch, boolean forceRetry) {
        if (boundServer != server || epoch != GATE.epoch()) {
            return;
        }
        if (!GATE.isQuiet(epoch)) {
            deferralDiagnostic(epoch);
            return;
        }
        if (GATE.isDegraded() && !forceRetry) {
            return;
        }
        if (!server.isSameThread()) {
            // reloadResources 只允许在服务器线程调用；拒不建立请求账，避免留下无法结算的 ownOp。
            if (!offThreadRequestWarned) {
                offThreadRequestWarned = true;
                LOGGER.warn("[Carpet Ice Addition] Refused datapack reconcile off the server thread; "
                        + "no reload request was created");
            }
            return;
        }

        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(
                server.getPackRepository().getSelectedIds(), desires());
        if (!plan.changed()) {
            GATE.onPlanEvaluated(false);
            return;
        }
        if (GATE.isNoProgress(plan.targetKey())) {
            // 成功结局 + 同目标 + 无期望变化：目标未被实际接受，停止重复尝试而不是无限 reload。
            GATE.markNoProgressStop();
            LOGGER.warn("[Carpet Ice Addition] Datapack selection did not converge to the requested set {}; "
                    + "stopping automatic reloads until the rule state changes again", plan.next());
            return;
        }
        if (!GATE.canStartReload(epoch, forceRetry)) {
            return;
        }

        // onPackDisabled 属于即时清理，必须先于改选执行（与既有控制器时序一致）。
        for (String packId : plan.disabledPackIds()) {
            ManagedPack pack = findById(packId);
            if (pack != null) {
                safeRun(pack.ruleName(), () -> pack.onPackDisabled().accept(server));
            }
        }

        GATE.onPlanEvaluated(true);
        startReload(server, plan, epoch);
    }

    private static void startReload(MinecraftServer server, RecipePackReconciler.Plan plan, long epoch) {
        if (!GATE.markRequestStarted(plan.targetKey(), epoch, true)) {
            return;
        }
        CompletableFuture<Void> future;
        try {
            future = server.reloadResources(plan.next());
        } catch (Throwable throwable) {
            // START 注入在 HEAD、END 注入在 TAIL：同步异常意味着该次 reload 永远不会收到 END，
            // 由状态机按 ownStartPending 判定是否需要补偿递减。
            GATE.settleSyncThrow(epoch);
            LOGGER.warn("[Carpet Ice Addition] Datapack reload failed synchronously", throwable);
            orchestrate();
            return;
        }
        boolean exceptional = future.isCompletedExceptionally() || future.isCancelled();
        GATE.settleReturnedFuture(epoch, future.isDone(), exceptional);
        orchestrate();
    }

    // ------------------------------------------------------------------ 辅助

    private static void bind(MinecraftServer server) {
        if (boundServer == server) {
            return;
        }
        boundServer = server;
        GATE.bindServer();
        offThreadRequestWarned = false;
        for (ManagedPack pack : PACKS) {
            safeRun(pack.ruleName(), pack.resetLockState());
        }
    }

    private static List<RecipePackReconciler.PackDesire> desires() {
        List<RecipePackReconciler.PackDesire> list = new ArrayList<>(PACKS.size());
        for (ManagedPack pack : PACKS) {
            list.add(new RecipePackReconciler.PackDesire(
                    pack.packIdString(), pack.ruleName(), safeBool(pack.ruleName(), pack.desired())));
        }
        return list;
    }

    private static void deferralDiagnostic(long epoch) {
        if (GATE.inFlightCount() > 0 && GATE.noteDeferredAndShouldWarn(epoch)) {
            LOGGER.warn("[Carpet Ice Addition] Datapack reconcile deferred: {} reload(s) still in flight; "
                    + "no forced counter reset is performed", GATE.inFlightCount());
        }
    }

    private static ManagedPack findByRule(String ruleName) {
        for (ManagedPack pack : PACKS) {
            if (pack.ruleName().equals(ruleName)) {
                return pack;
            }
        }
        return null;
    }

    private static ManagedPack findById(String packId) {
        for (ManagedPack pack : PACKS) {
            if (pack.packIdString().equals(packId)) {
                return pack;
            }
        }
        return null;
    }

    /** 单规则钩子异常隔离：异常只上报并按失败返回，不影响其它规则与协调器记账。 */
    private static boolean safeRun(String ruleName, Runnable action) {
        try {
            action.run();
            return true;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue(ruleName, throwable);
            return false;
        }
    }

    private static boolean safeBool(String ruleName, BooleanSupplier supplier) {
        try {
            return supplier.getAsBoolean();
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue(ruleName, throwable);
            return false;
        }
    }
}
