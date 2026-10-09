package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RecipeReloadGate} 状态归属与失败闭环测试。
 *
 * <p>覆盖：全局事件层与自有请求层分离、计数时机唯一、单次结算、静默点编排、非进展四条件、
 * 非服务器线程拒绝、ready 语义。
 */
class RecipeReloadGateTest {
    private RecipeReloadGate boundGate() {
        RecipeReloadGate gate = new RecipeReloadGate();
        gate.bindServer();
        gate.markWorldsLoaded();
        return gate;
    }

    // ---------------------------------------------------------------- 计数时机唯一

    @Test
    void markRequestStartedDoesNotTouchInFlightCount() {
        RecipeReloadGate gate = boundGate();
        assertTrue(gate.markRequestStarted("a", gate.epoch(), true));
        assertEquals(0, gate.inFlightCount(), "请求记账不得自增计数（唯一自增点是 START）");

        assertEquals(RecipeReloadGate.StartKind.OWN, gate.onReloadStart(gate.epoch()));
        assertEquals(1, gate.inFlightCount());
    }

    @Test
    void ownStartIsConsumedExactlyOnce() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        assertEquals(RecipeReloadGate.StartKind.OWN, gate.onReloadStart(gate.epoch()));
        assertEquals(RecipeReloadGate.StartKind.FOREIGN, gate.onReloadStart(gate.epoch()),
                "一次性标志只能消费一次，后续 START 一律视为外部");
        assertEquals(2, gate.inFlightCount());
    }

    @Test
    void normalReloadCountsDownToZero() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, false);
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, gate.ownOutcome());
        assertEquals(RecipeReloadGate.EndKind.NORMAL, gate.onReloadEnd(gate.epoch(), true));
        assertEquals(0, gate.inFlightCount());
        assertEquals(0, gate.failureCount());
    }

    // ---------------------------------------------------------------- 单次结算

    @Test
    void settlementIsIdempotentPerOperation() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, true);
        gate.settleReturnedFuture(gate.epoch(), true, true);
        gate.settleSyncThrow(gate.epoch());
        assertEquals(1, gate.failureCount(), "同一次失败只允许结算一次（不得重复消耗重试预算）");
        assertTrue(gate.isRetryRequested());
        assertFalse(gate.isDegraded());
        assertEquals(1, gate.inFlightCount(),
                "该次 reload 的 END 尚未到达：已结算后的 settleSyncThrow 不得吃掉它的在飞名额");
        assertEquals(1, gate.unmatchedSyncThrowCount(), "已结算后的重复结算只计数、不改状态");
    }

    @Test
    void unsettledFutureIsNotSettledDefensively() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.settleReturnedFuture(gate.epoch(), false, true);
        assertEquals(RecipeReloadGate.Outcome.PENDING, gate.ownOutcome(), "future 未完成不得结算");
        assertEquals(0, gate.failureCount());
    }

    // ---------------------------------------------------------------- 同步异常两分支

    @Test
    void syncThrowBeforeStartDoesNotDecrement() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.settleSyncThrow(gate.epoch());
        assertEquals(0, gate.inFlightCount(), "START 未投递：既无 START 也无 END，不得递减");
        assertEquals(RecipeReloadGate.Outcome.FAILURE, gate.ownOutcome());
        assertEquals(1, gate.failureCount());
    }

    @Test
    void syncThrowAfterStartCompensatesExactlyOnce() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        assertEquals(1, gate.inFlightCount());
        gate.settleSyncThrow(gate.epoch());
        assertEquals(0, gate.inFlightCount(), "有 START 无 END：必须补偿递减一次");
        assertEquals(RecipeReloadGate.EndKind.ANOMALOUS, gate.onReloadEnd(gate.epoch(), true),
                "该操作永不产生 END；若出现 END 只能是无匹配 START 的异常 END");
        assertEquals(0, gate.inFlightCount(), "异常 END 不得把计数压成负数");
    }

    /**
     * 合法补偿路径不得被身份守卫破坏，且重复调用不再触碰全局状态。
     */
    @Test
    void syncThrowCompensationStillHappensExactlyOnce() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        assertEquals(1, gate.inFlightCount());

        gate.settleSyncThrow(gate.epoch());
        assertEquals(0, gate.inFlightCount(), "有 START 无 END：合法补偿恰好递减一次");
        assertEquals(RecipeReloadGate.Outcome.FAILURE, gate.ownOutcome());
        assertEquals(1, gate.failureCount());
        assertEquals(0, gate.unmatchedSyncThrowCount(), "合法结算不得计入无归属计数");

        gate.settleSyncThrow(gate.epoch());
        assertEquals(0, gate.inFlightCount(), "重复结算不得把计数压成负数");
        assertEquals(1, gate.failureCount(), "同一次失败只结算一次");
        assertEquals(1, gate.unmatchedSyncThrowCount());
    }

    /**
     * 已结算后，若出现与本模组无关的 reload，多余的 settleSyncThrow 不得吃掉它的在飞名额。
     */
    @Test
    void repeatedSyncThrowLeavesForeignInflightCountUntouched() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.settleSyncThrow(gate.epoch());     // 合法：START 未投递 ⇒ 不递减
        assertEquals(RecipeReloadGate.Outcome.FAILURE, gate.ownOutcome());
        assertEquals(1, gate.failureCount());
        assertTrue(gate.isRetryRequested());

        assertEquals(RecipeReloadGate.StartKind.FOREIGN, gate.onReloadStart(gate.epoch()));
        assertEquals(1, gate.inFlightCount());

        gate.settleSyncThrow(gate.epoch());

        assertEquals(1, gate.inFlightCount(), "重复结算不得递减其它 reload 的在飞计数");
        assertFalse(gate.isQuiet(gate.epoch()), "外部 reload 仍在进行时不得被误判为静默点");
        assertEquals(1, gate.failureCount(), "重复结算不得再次消耗失败预算");
        assertTrue(gate.isRetryRequested(), "重复结算不得改动重试请求");
        assertEquals(RecipeReloadGate.Outcome.FAILURE, gate.ownOutcome());
        assertEquals(1, gate.unmatchedSyncThrowCount());

        assertEquals(RecipeReloadGate.EndKind.NORMAL, gate.onReloadEnd(gate.epoch(), true),
                "外部 reload 的 END 仍必须能正常配对（未被误减成异常 END）");
        assertEquals(0, gate.inFlightCount());
    }

    /**
     * 无效的重复结算不得消费 {@code ownStartPending}：否则后续真正的 START 会被误判为外部 reload。
     *
     * <p>为让该一次性标志可观测，本用例从一个刻意构造的入口状态出发（已结算但标志尚未被消费）：
     * 断言的是「守卫分支不得改动任何全局重载状态」这一契约，而不是该入口状态本身的可达性。
     */
    @Test
    void straySyncThrowDoesNotConsumeOwnStartFlag() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.settleReturnedFuture(gate.epoch(), true, true);
        assertEquals(RecipeReloadGate.Outcome.FAILURE, gate.ownOutcome());

        gate.settleSyncThrow(gate.epoch());

        assertEquals(1, gate.unmatchedSyncThrowCount());
        assertEquals(RecipeReloadGate.StartKind.OWN, gate.onReloadStart(gate.epoch()),
                "重复结算不得消费 ownStartPending（否则真正的 START 会被误判为外部）");
        assertEquals(1, gate.inFlightCount());
    }

    /**
     * 从未建立自有请求时，结算调用不得凭空修改全局状态。
     */
    @Test
    void syncThrowWithoutOwnRequestNeverDecrements() {
        RecipeReloadGate gate = boundGate();
        assertEquals(RecipeReloadGate.StartKind.FOREIGN, gate.onReloadStart(gate.epoch()));
        assertEquals(1, gate.inFlightCount());

        gate.settleSyncThrow(gate.epoch());

        assertNull(gate.ownOp(), "无自有请求时不得凭空建立请求账");
        assertEquals(1, gate.inFlightCount(), "无归属的结算调用不得递减全局计数");
        assertEquals(0, gate.failureCount());
        assertFalse(gate.isDegraded());
        assertFalse(gate.isRetryRequested());
        assertEquals(1, gate.unmatchedSyncThrowCount());
        assertFalse(gate.isQuiet(gate.epoch()), "外部 reload 仍在进行时不得被误判为静默点");
    }

    // ---------------------------------------------------------------- 迟到 / 异常事件

    @Test
    void staleEventsAreIgnoredAcrossServerBindings() {
        RecipeReloadGate gate = boundGate();
        long oldEpoch = gate.epoch();
        gate.markRequestStarted("a", oldEpoch, true);
        gate.onReloadStart(oldEpoch);
        assertEquals(1, gate.inFlightCount());

        gate.closeServer();
        long newEpoch = gate.epoch();
        assertEquals(0, gate.inFlightCount(), "关闭必须复位");

        assertEquals(RecipeReloadGate.StartKind.IGNORED_STALE, gate.onReloadStart(oldEpoch));
        assertEquals(RecipeReloadGate.EndKind.IGNORED_STALE, gate.onReloadEnd(oldEpoch, true));
        gate.settleReturnedFuture(oldEpoch, true, true);
        gate.settleSyncThrow(oldEpoch);
        assertEquals(0, gate.inFlightCount(), "迟到回调不得改变任何状态（尤其不得递减）");
        assertEquals(0, gate.failureCount(), "迟到回调不得消耗重试预算");
        assertEquals(0, gate.unmatchedSyncThrowCount(), "迟到回调计入 stale 诊断，不计入无归属结算");
        assertTrue(gate.staleEventCount() >= 4);
        assertTrue(newEpoch > oldEpoch);
    }

    @Test
    void anomalousEndIsDiagnosticOnly() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, false);
        gate.onReloadEnd(gate.epoch(), true);
        gate.onPassFinished(true);
        gate.onPlanEvaluated(false);
        assertTrue(gate.isReady());
        boolean readyBefore = gate.isReady();
        boolean retryUsedBefore = gate.isRetryUsed();

        assertEquals(RecipeReloadGate.EndKind.ANOMALOUS, gate.onReloadEnd(gate.epoch(), false));
        assertEquals(1, gate.anomalousEndCount());
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, gate.ownOutcome(), "异常 END 不得改写自有结局");
        assertEquals(retryUsedBefore, gate.isRetryUsed(), "异常 END 不得消耗重试预算");
        assertFalse(gate.isDegraded(), "异常 END 不得进入 degraded");
        assertFalse(gate.isPendingSyncPass(), "异常 END 不得置 pendingSyncPass（只诊断）");
        assertEquals(readyBefore, gate.isReady());
        assertTrue(gate.noteAnomalousEndAndShouldWarn(gate.epoch()), "首次异常 END 应告警一次");
        assertFalse(gate.noteAnomalousEndAndShouldWarn(gate.epoch()), "同一 epoch 只告警一次");
    }

    // ---------------------------------------------------------------- 失败预算

    @Test
    void firstFailureRequestsRetrySecondEntersDegraded() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, true);
        assertTrue(gate.isRetryRequested());
        assertFalse(gate.isDegraded());

        gate.onReloadEnd(gate.epoch(), false);
        assertTrue(gate.consumeRetryRequest(), "唯一重试路径：由静默点消费");
        assertFalse(gate.consumeRetryRequest(), "消费后不得再次返回 true（防止双启动）");

        // 重试请求：期间不得有期望值变化，否则失败预算会被合法复位
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, true);
        assertTrue(gate.isDegraded(), "第二次失败进入显式终态");
        assertFalse(gate.isRetryRequested(), "degraded 后不得再排重试");
        assertFalse(gate.isReady());
    }

    // ---------------------------------------------------------------- ready 语义

    @Test
    void readyRequiresHooksOkNoPlanChangeAndNotDegraded() {
        RecipeReloadGate gate = boundGate();

        gate.onPassFinished(true);
        gate.onPlanEvaluated(false);
        assertTrue(gate.isReady(), "pass 全成功且无需 reload 时应为 ready");

        gate.onPlanEvaluated(true);
        assertFalse(gate.isReady(), "目标状态未收敛不得报告 ready");

        gate.onPassFinished(true);
        gate.onPlanEvaluated(false);
        assertTrue(gate.isReady());
        gate.onPassFinished(false);
        assertFalse(gate.isReady(), "同步钩子失败必须 ready=false");
        assertTrue(gate.isPendingSyncPass(), "同步失败应重新欠一次 pass");
    }

    @Test
    void degradedWithUnconvergedTargetNeverReportsReady() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, true);
        gate.onReloadEnd(gate.epoch(), false);
        gate.consumeRetryRequest();
        // 第二次尝试同样失败（期间无期望值变化 ⇒ 预算已耗尽）
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, true);
        assertTrue(gate.isDegraded());

        gate.onPassFinished(true);
        gate.onPlanEvaluated(false);
        assertFalse(gate.isReady(), "degraded 且目标未收敛时不得报告 ready");
    }

    // ---------------------------------------------------------------- 静默点与重试

    @Test
    void startupPassIsOwedSoReadyCanBecomeTrueWithoutAnyReload() {
        RecipeReloadGate gate = boundGate();
        assertTrue(gate.isPendingSyncPass(), "载入世界后必须欠一次 pass");
        assertEquals(RecipeReloadGate.QuiescenceAction.RUN_PASS_AND_SCHEDULE, gate.quiescenceAction(gate.epoch()));
        gate.onPassFinished(true);
        gate.onPlanEvaluated(false);
        assertTrue(gate.isReady(), "即使不需要 reload，也要消费 pendingSyncPass 使 ready 成立");
        assertEquals(RecipeReloadGate.QuiescenceAction.SCHEDULE, gate.quiescenceAction(gate.epoch()));
    }

    @Test
    void reloadInFlightDefersQuiescenceAction() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        assertEquals(RecipeReloadGate.QuiescenceAction.NONE, gate.quiescenceAction(gate.epoch()),
                "仍有 reload 在进行时不得跑 pass，也不得发起 reload");
        assertFalse(gate.canStartReload(gate.epoch(), true));
    }

    @Test
    void deferredWarningIsOncePerEpoch() {
        RecipeReloadGate gate = boundGate();
        assertTrue(gate.noteDeferredAndShouldWarn(gate.epoch()));
        assertFalse(gate.noteDeferredAndShouldWarn(gate.epoch()));
        gate.bindServer();
        gate.markWorldsLoaded();
        assertTrue(gate.noteDeferredAndShouldWarn(gate.epoch()), "新绑定后允许再次告警");
    }

    // ---------------------------------------------------------------- 非进展四条件

    @Test
    void noProgressRequiresAllFourConditions() {
        // 1) 上次结局 SUCCESS + 同目标 + 无期望变化 ⇒ 判定非进展
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("k", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, false);
        gate.onReloadEnd(gate.epoch(), true);
        assertTrue(gate.isNoProgress("k"));

        // 2) 期望变化 ⇒ 豁免
        gate.onDesiredChanged();
        assertFalse(gate.isNoProgress("k"), "期间期望变化过 ⇒ 不判非进展");

        // 3) 目标不同 ⇒ 豁免
        assertFalse(gate.isNoProgress("other"));

        // 4) 上次结局 FAILURE ⇒ 豁免（同目标重发正是重试语义）
        RecipeReloadGate failing = boundGate();
        failing.markRequestStarted("k", failing.epoch(), true);
        failing.onReloadStart(failing.epoch());
        failing.settleReturnedFuture(failing.epoch(), true, true);
        assertEquals(RecipeReloadGate.Outcome.FAILURE, failing.ownOutcome());
        assertFalse(failing.isNoProgress("k"), "失败重试必须豁免非进展判定");
    }

    @Test
    void noProgressStopIsRecoverableByDesiredChange() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("k", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, false);
        gate.onReloadEnd(gate.epoch(), true);
        gate.markNoProgressStop();
        assertTrue(gate.isDegraded());
        assertFalse(gate.canStartReload(gate.epoch(), false));

        gate.onDesiredChanged();
        assertFalse(gate.isDegraded(), "规则值变化必须能恢复");
        assertTrue(gate.canStartReload(gate.epoch(), false));
    }

    // ---------------------------------------------------------------- 非服务器线程

    @Test
    void offThreadRequestIsRefusedAndCreatesNoOwnOp() {
        RecipeReloadGate gate = boundGate();
        assertFalse(gate.markRequestStarted("a", gate.epoch(), false));
        assertNull(gate.ownOp(), "非服务器线程请求不得创建无法结算的 ownOp");
        assertEquals(1, gate.refusedOffThreadRequestCount());
        assertEquals(0, gate.reloadRequestCount());
        assertEquals(0, gate.inFlightCount());
    }

    @Test
    void settledOwnOpIsRetainedForDiagnostics() {
        RecipeReloadGate gate = boundGate();
        gate.markRequestStarted("a", gate.epoch(), true);
        gate.onReloadStart(gate.epoch());
        gate.settleReturnedFuture(gate.epoch(), true, false);
        RecipeReloadGate.OwnOp op = gate.ownOp();
        assertNotNull(op);
        assertTrue(op.settled());
        assertEquals("a", op.targetKey());
        assertEquals(gate.epoch(), op.epoch());
    }
}
