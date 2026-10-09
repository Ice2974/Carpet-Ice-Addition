package com.ice2974.carpeticeaddition.rules;

/**
 * 配方类内置数据包资源 reload 的纯状态机（无 Minecraft 依赖，可直接单元测试）。
 *
 * <p>Fabric 的 {@code START/END_DATA_PACK_RELOAD} 是**无操作 ID 的全局事件**，因此本状态机把两件事
 * 严格分层：
 * <ul>
 *   <li><b>全局事件层</b>：{@link #inFlightCount} 只由 START 自增、只由**正常** END 自减（外加同步异常
 *       补偿），仅用于判断「系统是否静默」；全局 END <b>不得</b>改写自有请求的结局、不得消耗重试预算、
 *       不得直接触发 reload。无匹配 START 的 END 只做诊断（{@link EndKind#ANOMALOUS}）。</li>
 *   <li><b>自有请求层</b>：{@link #ownOp} 的结局只由本次 {@code reloadResources(...)} 的动作结算
 *       （返回后按 future 是否异常完成，或调用点同步异常），并带 {@code settled} 互斥标记，
 *       保证同一次失败只结算一次。</li>
 * </ul>
 *
 * <p>静默点（quiescence）判定：{@code epoch} 匹配 ∧ {@code worldsLoaded} ∧ {@code inFlightCount == 0}。
 * 冲突重算与菜单同步只允许在静默点执行；重试的启动也只在静默点由 {@link #consumeRetryRequest()} 单一路径消费。
 *
 * <p>线程模型：本类不做同步，调用方必须保证全部方法在服务器线程上调用；非服务器线程的请求由
 * {@link #markRequestStarted(String, long, boolean)} 显式拒绝，避免创建无法结算的 {@link OwnOp}。
 */
public final class RecipeReloadGate {
    /** {@link #onReloadStart(long)} 的结果。 */
    public enum StartKind {
        /** epoch 不匹配（迟到/跨服务器事件），未修改任何状态。 */
        IGNORED_STALE,
        /** 外部（或无法归属）的 reload 开始。 */
        FOREIGN,
        /** 本模组自己发起的 reload 开始（消费了一次性 {@code ownStartPending}）。 */
        OWN
    }

    /** {@link #onReloadEnd(long, boolean)} 的结果。 */
    public enum EndKind {
        /** epoch 不匹配（迟到/跨服务器事件），未修改任何状态。 */
        IGNORED_STALE,
        /** 无匹配 START 的 END：只做诊断，不改结局、不耗预算、不跑静默点、不触发 reload。 */
        ANOMALOUS,
        /** 正常配对的一次 reload 结束。 */
        NORMAL
    }

    /** {@link #quiescenceAction(long)} 的结果：静默点应做的工作。 */
    public enum QuiescenceAction {
        /** 非静默点（未绑定 / 世界未载入 / 仍有 reload 在进行）：什么都不做。 */
        NONE,
        /** 欠一次同步 pass，且随后需要走一次收敛判定。 */
        RUN_PASS_AND_SCHEDULE,
        /** 无欠账，只需走一次收敛判定。 */
        SCHEDULE
    }

    /** 自有请求的结局。 */
    public enum Outcome {
        PENDING,
        SUCCESS,
        FAILURE
    }

    private static final long NO_EPOCH = Long.MIN_VALUE;

    /** 自有请求记录：target 用规范化键保存，避免依赖集合实例。 */
    public record OwnOp(String targetKey, long epoch, Outcome outcome, boolean settled) {
        OwnOp settledAs(Outcome newOutcome) {
            return new OwnOp(targetKey, epoch, newOutcome, true);
        }
    }

    private long serverEpoch;
    private boolean bound;
    private boolean worldsLoaded;
    private int inFlightCount;
    private boolean ownStartPending;
    private boolean pendingSyncPass;
    private boolean hooksOk;
    private boolean ready;
    private boolean retryUsed;
    private boolean retryRequested;
    private boolean degraded;
    private boolean desiredChangedSinceRequest;
    private OwnOp ownOp;

    private long anomalousEndWarnedAtEpoch = NO_EPOCH;
    private long deferredWarnedAtEpoch = NO_EPOCH;

    private int reloadRequestCount;
    private int failureCount;
    private int anomalousEndCount;
    private int staleEventCount;
    private int refusedOffThreadRequestCount;

    // ------------------------------------------------------------------ 生命周期

    /** 绑定新服务器（含首次绑定）：推进 epoch 并复位全部运行期状态。 */
    public void bindServer() {
        serverEpoch++;
        bound = true;
        resetRuntimeState();
    }

    /** 服务器关闭：推进 epoch（使迟到回调失效）并复位全部运行期状态。 */
    public void closeServer() {
        serverEpoch++;
        bound = false;
        resetRuntimeState();
    }

    private void resetRuntimeState() {
        worldsLoaded = false;
        inFlightCount = 0;
        ownStartPending = false;
        pendingSyncPass = false;
        hooksOk = false;
        ready = false;
        retryUsed = false;
        retryRequested = false;
        degraded = false;
        desiredChangedSinceRequest = false;
        ownOp = null;
        anomalousEndWarnedAtEpoch = NO_EPOCH;
        deferredWarnedAtEpoch = NO_EPOCH;
        reloadRequestCount = 0;
        failureCount = 0;
        anomalousEndCount = 0;
        staleEventCount = 0;
        refusedOffThreadRequestCount = 0;
    }

    public long epoch() {
        return serverEpoch;
    }

    public boolean isBound() {
        return bound;
    }

    /**
     * 世界文件加载完成、{@code RecipeManager} 已就绪。
     *
     * <p>此时必须欠下一次静默点 pass：即使本次判定不需要 reload，也要让 {@code pendingSyncPass} 被消费，
     * 否则 {@code ready} 会永久为 false。
     */
    public void markWorldsLoaded() {
        worldsLoaded = true;
        pendingSyncPass = true;
    }

    /** 显式欠下一次同步 pass（启动、以及任何需要重新同步的迁移）。 */
    public void armPendingPass() {
        pendingSyncPass = true;
    }

    // ------------------------------------------------------------------ 全局事件层

    /** START：全局计数唯一自增点。 */
    public StartKind onReloadStart(long epoch) {
        if (epoch != serverEpoch) {
            staleEventCount++;
            return StartKind.IGNORED_STALE;
        }
        inFlightCount++;
        if (ownStartPending) {
            ownStartPending = false;
            return StartKind.OWN;
        }
        return StartKind.FOREIGN;
    }

    /**
     * END：全局计数唯一自减点。
     *
     * <p>只更新全局层：结局、重试预算、{@code degraded} 一律不动；无匹配 START 时仅诊断。
     */
    public EndKind onReloadEnd(long epoch, boolean success) {
        if (epoch != serverEpoch) {
            staleEventCount++;
            return EndKind.IGNORED_STALE;
        }
        if (inFlightCount <= 0) {
            // 无匹配 START：不 clamp、不静默，仅诊断。
            anomalousEndCount++;
            return EndKind.ANOMALOUS;
        }
        inFlightCount--;
        pendingSyncPass = true;
        return EndKind.NORMAL;
    }

    /** 无匹配 START 的 END 是否应打印一次告警（每个 epoch 至多一次）。 */
    public boolean noteAnomalousEndAndShouldWarn(long epoch) {
        if (epoch != serverEpoch || anomalousEndWarnedAtEpoch == serverEpoch) {
            return false;
        }
        anomalousEndWarnedAtEpoch = serverEpoch;
        return true;
    }

    /** 因仍有 reload 在进行而推迟时，是否应打印一次诊断告警（每个 epoch 至多一次）。 */
    public boolean noteDeferredAndShouldWarn(long epoch) {
        if (epoch != serverEpoch || deferredWarnedAtEpoch == serverEpoch) {
            return false;
        }
        deferredWarnedAtEpoch = serverEpoch;
        return true;
    }

    // ------------------------------------------------------------------ 自有请求层

    /**
     * 记录一次自有 reload 请求。
     *
     * @param onServerThread {@code reloadResources} 只允许在服务器线程调用；非服务器线程请求直接拒绝，
     *                       不创建 {@link OwnOp}（否则会留下无法结算的请求）
     * @return true 表示已记账（可以继续调用 {@code reloadResources}）
     */
    public boolean markRequestStarted(String targetKey, long epoch, boolean onServerThread) {
        if (epoch != serverEpoch) {
            staleEventCount++;
            return false;
        }
        if (!onServerThread) {
            refusedOffThreadRequestCount++;
            return false;
        }
        // 自愈：直接覆盖可能残留的一次性标志，避免把后续外部 START 误认成本模组操作。
        ownStartPending = true;
        ownOp = new OwnOp(targetKey, epoch, Outcome.PENDING, false);
        desiredChangedSinceRequest = false;
        reloadRequestCount++;
        return true;
    }

    /**
     * 结算「返回的 future」。
     *
     * <p>服务器线程上 {@code reloadResources} 会阻塞到 reload 完成，返回时 future 已完成；
     * 因此 {@code futureDone == false} 属防御分支：不做结算（保持 {@code PENDING}），避免误判成败。
     */
    public void settleReturnedFuture(long epoch, boolean futureDone, boolean completedExceptionally) {
        if (!futureDone) {
            return;
        }
        settle(epoch, completedExceptionally ? Outcome.FAILURE : Outcome.SUCCESS);
    }

    /**
     * 结算「{@code reloadResources} 同步抛异常」。
     *
     * <p>{@code ownStartPending} 仍为 true ⇒ START 未投递（既无 START 也无 END，不递减）；
     * 否则 ⇒ START 已投递但 TAIL 未执行（有 START 无 END，补偿递减一次）。
     */
    public void settleSyncThrow(long epoch) {
        if (epoch != serverEpoch) {
            staleEventCount++;
            return;
        }
        if (ownStartPending) {
            ownStartPending = false;
        } else if (inFlightCount > 0) {
            inFlightCount--;
        }
        settle(epoch, Outcome.FAILURE);
    }

    private void settle(long epoch, Outcome outcome) {
        if (epoch != serverEpoch) {
            staleEventCount++;
            return;
        }
        OwnOp op = ownOp;
        if (op == null || op.settled() || op.epoch() != epoch) {
            return;
        }
        ownOp = op.settledAs(outcome);
        if (outcome == Outcome.FAILURE) {
            handleFailure();
        }
    }

    private void handleFailure() {
        failureCount++;
        if (!retryUsed) {
            retryUsed = true;
            retryRequested = true;
            return;
        }
        degraded = true;
        retryRequested = false;
        ready = false;
    }

    // ------------------------------------------------------------------ 静默点与收敛

    public boolean isQuiet(long epoch) {
        return epoch == serverEpoch && worldsLoaded && inFlightCount == 0;
    }

    /** 静默点唯一编排决策：调用方据此决定是否跑 pass、随后是否走收敛判定。 */
    public QuiescenceAction quiescenceAction(long epoch) {
        if (!isQuiet(epoch)) {
            return QuiescenceAction.NONE;
        }
        return pendingSyncPass ? QuiescenceAction.RUN_PASS_AND_SCHEDULE : QuiescenceAction.SCHEDULE;
    }

    /**
     * 同步 pass 结束。
     *
     * @param allHooksOk 全部受管包的冲突重算与菜单/配方书同步均未抛异常
     */
    public void onPassFinished(boolean allHooksOk) {
        hooksOk = allHooksOk;
        if (allHooksOk) {
            pendingSyncPass = false;
        } else {
            // 同步失败：重新欠一次，且不得被误判为 ready。
            pendingSyncPass = true;
            ready = false;
        }
    }

    /**
     * 收敛判定完成（{@code plan} 已算出）。
     *
     * <p>{@code ready} 只在「pass 钩子全成功 ∧ 未 degraded ∧ 选中集合已与期望一致」时为 true；
     * 进入 degraded 且目标状态未收敛时不得报告 ready。
     */
    public void onPlanEvaluated(boolean planChanged) {
        if (planChanged) {
            ready = false;
            return;
        }
        ready = hooksOk && !degraded;
    }

    /** 消费重试请求：true 表示本次收敛应以 {@code forceRetry = true} 启动（单一重试路径）。 */
    public boolean consumeRetryRequest() {
        if (!retryRequested) {
            return false;
        }
        retryRequested = false;
        return true;
    }

    public boolean canStartReload(long epoch, boolean forceRetry) {
        if (epoch != serverEpoch || !worldsLoaded) {
            return false;
        }
        if (inFlightCount > 0) {
            return false;
        }
        return !degraded || forceRetry;
    }

    /** 规则值变化 / 冲突锁定迁移：豁免非进展判定并复位失败预算。 */
    public void onDesiredChanged() {
        desiredChangedSinceRequest = true;
        retryUsed = false;
        degraded = false;
    }

    /**
     * 非进展判定：四条件同时成立才判定为「成功但目标未生效」。
     *
     * <p>{@code 目标相同 ∧ 期间无期望变化 ∧ 上次结局为 SUCCESS}。失败重试（结局 FAILURE）因此天然豁免，
     * 因为同目标重发正是重试语义。
     */
    public boolean isNoProgress(String nextTargetKey) {
        OwnOp op = ownOp;
        return op != null
                && !desiredChangedSinceRequest
                && op.outcome() == Outcome.SUCCESS
                && op.targetKey().equals(nextTargetKey);
    }

    public void markNoProgressStop() {
        degraded = true;
        ready = false;
    }

    // ------------------------------------------------------------------ 只读视图（单测/诊断）

    public int inFlightCount() {
        return inFlightCount;
    }

    public boolean isPendingSyncPass() {
        return pendingSyncPass;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public boolean isRetryRequested() {
        return retryRequested;
    }

    public boolean isRetryUsed() {
        return retryUsed;
    }

    public boolean isWorldsLoaded() {
        return worldsLoaded;
    }

    public boolean isDesiredChangedSinceRequest() {
        return desiredChangedSinceRequest;
    }

    public OwnOp ownOp() {
        return ownOp;
    }

    public Outcome ownOutcome() {
        return ownOp == null ? null : ownOp.outcome();
    }

    public int reloadRequestCount() {
        return reloadRequestCount;
    }

    public int failureCount() {
        return failureCount;
    }

    public int anomalousEndCount() {
        return anomalousEndCount;
    }

    public int staleEventCount() {
        return staleEventCount;
    }

    public int refusedOffThreadRequestCount() {
        return refusedOffThreadRequestCount;
    }
}
