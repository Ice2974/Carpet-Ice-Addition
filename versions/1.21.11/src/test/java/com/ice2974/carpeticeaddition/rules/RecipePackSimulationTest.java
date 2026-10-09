package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 状态机 + 规划器的组合模拟测试（纯 Java，无 Minecraft 依赖）。
 *
 * <p>{@link Harness} 复刻协调器的编排顺序（静默点 → pass → 收敛判定 → 启动 reload → 结算 → 投递 END），
 * 用于验证第五轮计划的轨迹 T1–T12 与边界约束：启动必须消费 {@code pendingSyncPass}、静默状态下的规则变化
 * 要能重跑未完成的 pass、{@code ready} 语义、非服务器线程拒绝、{@code onPackDisabled} 即时清理时序。
 */
class RecipePackSimulationTest {
    private static final String CORAL = "carpet-ice-addition:craftable_coral_blocks";
    private static final String CALCITE = "carpet-ice-addition:calcite_stonecutting";

    /** 复刻协调器编排：与 RecipePackCoordinator 的调用顺序一致。 */
    private static final class Harness {
        final RecipeReloadGate gate = new RecipeReloadGate();
        final LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla"));
        final List<RecipePackReconciler.PackDesire> desires = new ArrayList<>(List.of(
                new RecipePackReconciler.PackDesire(CORAL, "craftableCoralBlocks", false),
                new RecipePackReconciler.PackDesire(CALCITE, "calciteStonecuttingRecipe", false)));

        final Deque<Boolean> pendingEnds = new ArrayDeque<>();
        final List<String> eventLog = new ArrayList<>();
        final List<LinkedHashSet<String>> reloadTargets = new ArrayList<>();
        /** 每次 reload 尝试「结算之后」的 inFlightCount，用于验证补偿语义。 */
        final List<Integer> countAfterSettle = new ArrayList<>();
        /** 每次 reload 尝试的结算结局（FAILURE/SUCCESS），用于验证单次结算与重试语义。 */
        final List<RecipeReloadGate.Outcome> settleOutcomes = new ArrayList<>();

        int reloadCount;
        int passCount;
        int passFailureCount;
        int noProgressStops;
        int deferredWarns;
        boolean nextReloadFails;
        boolean alwaysFailReloads;
        boolean nextReloadThrowsBeforeStart;
        boolean nextReloadThrowsAfterStart;
        boolean nextPassFails;

        void boot() {
            gate.bindServer();
            gate.markWorldsLoaded();
            orchestrate();
        }

        void setDesired(String packId, boolean value) {
            for (int i = 0; i < desires.size(); i++) {
                RecipePackReconciler.PackDesire desire = desires.get(i);
                if (desire.packId().equals(packId) && desire.desired() != value) {
                    desires.set(i, new RecipePackReconciler.PackDesire(packId, desire.ruleName(), value));
                    gate.onDesiredChanged();
                    orchestrate();
                    return;
                }
            }
        }

        /** 协调器的静默点编排：quiescenceAction → pass → scheduleNext → requestReconcile。 */
        void orchestrate() {
            long epoch = gate.epoch();
            switch (gate.quiescenceAction(epoch)) {
                case NONE -> {
                    // 因仍有 reload 在进行而推迟：只做一次/epoch 诊断，绝不强制清零。
                    if (gate.inFlightCount() > 0 && gate.noteDeferredAndShouldWarn(epoch)) {
                        deferredWarns++;
                    }
                }
                case RUN_PASS_AND_SCHEDULE -> {
                    gate.onPassFinished(runPass());
                    scheduleNext();
                }
                case SCHEDULE -> scheduleNext();
            }
        }

        private boolean runPass() {
            passCount++;
            eventLog.add("pass");
            if (nextPassFails) {
                nextPassFails = false;
                passFailureCount++;
                return false;
            }
            return true;
        }

        private void scheduleNext() {
            boolean forceRetry = gate.consumeRetryRequest();
            reconcile(forceRetry);
        }

        private void reconcile(boolean forceRetry) {
            long epoch = gate.epoch();
            if (!gate.isQuiet(epoch)) {
                return;
            }
            if (gate.isDegraded() && !forceRetry) {
                return;
            }
            RecipePackReconciler.Plan plan = RecipePackReconciler.plan(selected, desires);
            if (!plan.changed()) {
                gate.onPlanEvaluated(false);
                return;
            }
            if (gate.isNoProgress(plan.targetKey())) {
                noProgressStops++;
                gate.markNoProgressStop();
                return;
            }
            if (!gate.canStartReload(epoch, forceRetry)) {
                return;
            }
            gate.onPlanEvaluated(true);   // 目标状态尚未收敛
            startReload(plan, epoch);
        }

        private void startReload(RecipePackReconciler.Plan plan, long epoch) {
            // onPackDisabled 必须先于改选（即时清理不得因协调器迁移而退化）
            for (String packId : plan.disabledPackIds()) {
                eventLog.add("cleanup:" + packId);
            }
            if (!gate.markRequestStarted(plan.targetKey(), epoch, true)) {
                return;
            }
            reloadCount++;
            reloadTargets.add(new LinkedHashSet<>(plan.next()));

            if (nextReloadThrowsBeforeStart) {
                nextReloadThrowsBeforeStart = false;
                gate.settleSyncThrow(epoch);        // START 未投递 ⇒ 不递减
                countAfterSettle.add(gate.inFlightCount());
                settleOutcomes.add(gate.ownOutcome());
                orchestrate();
                return;
            }

            gate.onReloadStart(epoch);              // [HEAD] START 同步到达
            if (nextReloadThrowsAfterStart) {
                nextReloadThrowsAfterStart = false;
                gate.settleSyncThrow(epoch);        // TAIL 未执行 ⇒ 不投递 END
                countAfterSettle.add(gate.inFlightCount());
                settleOutcomes.add(gate.ownOutcome());
                orchestrate();
                return;
            }

            boolean fails = alwaysFailReloads || nextReloadFails;
            nextReloadFails = false;
            if (!fails) {
                eventLog.add("select");
                selected.clear();
                selected.addAll(plan.next());       // $4：换 resources + setSelected（阻塞内完成）
            }
            gate.settleReturnedFuture(epoch, true, fails);
            countAfterSettle.add(gate.inFlightCount());
            settleOutcomes.add(gate.ownOutcome());
            pendingEnds.add(!fails);                // [TAIL] 之后投递 END
            orchestrate();
        }

        void deliverEnd(boolean success) {
            pendingEnds.poll();
            gate.onReloadEnd(gate.epoch(), success);
            orchestrate();
        }

        void deliverOurs() {
            Boolean success = pendingEnds.peek();
            deliverEnd(success == null || success);
        }

        void foreignStart() {
            gate.onReloadStart(gate.epoch());
        }

        /** 无匹配 START 的 END：只诊断，不编排（不得借此触发 pass 或 reload）。 */
        void deliverAnomalousEnd(boolean success) {
            assertEquals(0, gate.inFlightCount(), "异常 END 场景要求无匹配 START");
            gate.onReloadEnd(gate.epoch(), success);
            if (gate.noteAnomalousEndAndShouldWarn(gate.epoch())) {
                deferredWarns++;
            }
        }
    }

    // ---------------------------------------------------------------- T1 正常

    @Test
    void t1_normalReloadConvergesWithoutExtraReload() {
        Harness h = new Harness();
        h.boot();
        assertEquals(0, h.reloadCount, "启动时无需 reload");
        assertTrue(h.gate.isReady(), "启动的静默点 pass 必须被消费，ready 才能成立");

        int passesAfterBoot = h.passCount;
        h.setDesired(CORAL, true);

        assertEquals(1, h.reloadCount);
        assertEquals(1, h.gate.inFlightCount());
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.gate.ownOutcome());
        assertFalse(h.gate.isReady(), "目标状态未收敛时不得 ready");

        h.deliverOurs();
        assertEquals(0, h.gate.inFlightCount());
        assertEquals(passesAfterBoot + 1, h.passCount, "END 后静默点 pass 恰执行一次");
        assertTrue(h.gate.isReady());
        assertFalse(h.gate.isPendingSyncPass());
        assertEquals(1, h.reloadCount, "收敛后不得再有 reload");
    }

    // ---------------------------------------------------------------- T2/T3 失败与重试

    @Test
    void t2_failureRetryReusesSameTargetWithoutNoProgressStop() {
        Harness h = new Harness();
        h.boot();
        h.nextReloadFails = true;
        h.setDesired(CORAL, true);

        assertEquals(RecipeReloadGate.Outcome.FAILURE, h.gate.ownOutcome());
        assertTrue(h.gate.isRetryRequested(), "首次失败排出一次重试");
        assertEquals(1, h.gate.failureCount());
        assertEquals(1, h.reloadCount, "结算阶段不得直接发起 reload");
        assertEquals(1, h.gate.inFlightCount());

        h.deliverEnd(false);
        assertEquals(2, h.reloadCount, "重试由静默点单一路径启动");
        assertEquals(0, h.noProgressStops, "失败重试必须豁免非进展判定");

        h.deliverOurs();
        assertTrue(h.gate.isReady());
        assertFalse(h.gate.isDegraded());
        assertEquals(2, h.reloadCount);
    }

    @Test
    void t3_twoFailuresEnterDegradedAndStopReloading() {
        Harness h = new Harness();
        h.boot();
        h.alwaysFailReloads = true;
        h.setDesired(CORAL, true);
        assertEquals(1, h.reloadCount);

        h.deliverEnd(false);          // 重试在同一次 END 处理内启动，并再次失败

        assertEquals(2, h.reloadCount);
        assertTrue(h.gate.isDegraded(), "第二次失败进入显式终态");
        assertFalse(h.gate.isRetryRequested());
        assertFalse(h.gate.isReady(), "degraded 且未收敛时不得 ready");
        assertEquals(2, h.gate.failureCount(), "两次失败各结算一次（预算只被消耗一次）");
        assertTrue(h.gate.isRetryUsed());

        h.deliverEnd(false);          // 最后一次 END：degraded 守卫拦下
        assertEquals(2, h.reloadCount, "degraded 后不得再有 reload");
    }

    // ---------------------------------------------------------------- T4/T5 同步异常

    @Test
    void t4_syncThrowBeforeStartKeepsCounterAtZero() {
        Harness h = new Harness();
        h.boot();
        h.nextReloadThrowsBeforeStart = true;
        h.setDesired(CORAL, true);

        assertEquals(2, h.reloadCount, "原次同步异常后，重试在同栈内、静默后启动");
        assertEquals(List.of(0, 1), h.countAfterSettle,
                "START 未投递 ⇒ 结算后计数必须仍为 0（不得递减成负数），重试 START 后为 1");
        assertEquals(List.of(RecipeReloadGate.Outcome.FAILURE, RecipeReloadGate.Outcome.SUCCESS),
                h.settleOutcomes, "原次结算为 FAILURE，重试结算为 SUCCESS");
        assertEquals(1, h.gate.failureCount(), "同一次失败只结算一次");
    }

    @Test
    void t5_syncThrowAfterStartCompensatesThenRetries() {
        Harness h = new Harness();
        h.boot();
        h.nextReloadThrowsAfterStart = true;
        h.setDesired(CORAL, true);

        assertEquals(2, h.reloadCount);
        assertEquals(List.of(0, 1), h.countAfterSettle,
                "有 START 无 END ⇒ 必须补偿递减一次，然后重试 START 使计数回到 1");
        assertEquals(List.of(RecipeReloadGate.Outcome.FAILURE, RecipeReloadGate.Outcome.SUCCESS),
                h.settleOutcomes);
        assertTrue(h.pendingEnds.size() <= 1, "同步异常路径不产生 END");
    }

    // ---------------------------------------------------------------- T6/T7 重叠与外部 END

    @Test
    void t6_foreignFailedEndDoesNotPolluteOwnOutcomeOrBudget() {
        Harness h = new Harness();
        h.boot();
        h.setDesired(CORAL, true);
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.gate.ownOutcome());

        h.foreignStart();
        assertEquals(2, h.gate.inFlightCount());
        h.deliverEnd(false);          // 外部 reload 失败

        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.gate.ownOutcome(), "外部 END 不得改写自有结局");
        assertEquals(0, h.gate.failureCount(), "外部失败不得消耗本模组重试预算");
        assertFalse(h.gate.isDegraded());
        assertFalse(h.gate.isRetryRequested());
        assertEquals(1, h.reloadCount, "外部失败 END 不得直接触发新 reload");

        h.deliverOurs();
        assertTrue(h.gate.isReady());
        assertEquals(1, h.reloadCount);
    }

    @Test
    void t7_overlappingReloadsDeferPassUntilLastEnd() {
        Harness h = new Harness();
        h.boot();
        h.setDesired(CORAL, true);
        int passes = h.passCount;

        h.foreignStart();
        h.deliverEnd(true);           // 外部先结束：仍有 1 个在进行
        assertEquals(1, h.gate.inFlightCount());
        assertEquals(passes, h.passCount, "仍有 reload 在进行时不得跑静默点 pass");

        h.deliverOurs();              // 最后一个 END
        assertEquals(0, h.gate.inFlightCount());
        assertEquals(passes + 1, h.passCount, "最后一次 END 必须消费静默点");
        assertTrue(h.gate.isReady());
    }

    // ---------------------------------------------------------------- T8 pass 失败

    @Test
    void t8_failedPassReArmsAndRuleChangeReRunsIt() {
        Harness h = new Harness();
        h.boot();
        h.setDesired(CORAL, true);
        h.nextPassFails = true;
        h.deliverOurs();

        assertTrue(h.gate.isPendingSyncPass(), "同步钩子失败要重新欠一次 pass");
        assertFalse(h.gate.isReady(), "同步失败必须 ready=false");
        assertEquals(1, h.passFailureCount);

        h.setDesired(CALCITE, true);
        assertEquals(1, h.passFailureCount, "规则变化重跑的 pass 成功");
        assertFalse(h.gate.isPendingSyncPass(), "重跑成功后 pendingSyncPass 被消费");
    }

    // ---------------------------------------------------------------- T9/T10 异常与迟到 END

    @Test
    void t9_anomalousEndIsDiagnosticOnly() {
        Harness h = new Harness();
        h.boot();
        h.setDesired(CORAL, true);
        h.deliverOurs();
        assertTrue(h.gate.isReady());

        int reloads = h.reloadCount;
        int passes = h.passCount;
        int warns = h.deferredWarns;
        boolean readyBefore = h.gate.isReady();

        h.deliverAnomalousEnd(false);

        assertEquals(1, h.gate.anomalousEndCount());
        assertEquals(reloads, h.reloadCount, "异常 END 不得触发 reload");
        assertEquals(passes, h.passCount, "异常 END 不得跑 pass");
        assertEquals(warns + 1, h.deferredWarns, "异常 END 应告警一次");
        assertEquals(readyBefore, h.gate.isReady());
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.gate.ownOutcome());
        assertEquals(0, h.gate.failureCount());
        assertFalse(h.gate.isDegraded());
        assertFalse(h.gate.isPendingSyncPass(), "异常 END 不得置 pendingSyncPass");
    }

    @Test
    void t10_staleEndAfterServerCloseIsIgnored() {
        Harness h = new Harness();
        h.boot();
        h.setDesired(CORAL, true);
        long oldEpoch = h.gate.epoch();
        assertEquals(1, h.gate.inFlightCount());

        h.gate.closeServer();
        h.gate.onReloadEnd(oldEpoch, true);
        h.gate.settleReturnedFuture(oldEpoch, true, true);

        assertEquals(0, h.gate.inFlightCount());
        assertEquals(0, h.gate.failureCount(), "迟到回调不得消耗预算");
        assertTrue(h.gate.staleEventCount() >= 2);
    }

    // ---------------------------------------------------------------- T11 保守停摆边界

    @Test
    void t11_foreignStartWithoutEndStallsConservatively() {
        Harness h = new Harness();
        h.boot();
        h.foreignStart();
        assertEquals(1, h.gate.inFlightCount());

        h.setDesired(CORAL, true);

        assertEquals(0, h.reloadCount, "存在无法归属的进行中 reload 时不得发起新 reload");
        assertEquals(1, h.deferredWarns, "推迟只产生一次诊断告警");
        assertEquals(1, h.gate.inFlightCount(), "不得强制清零");
    }

    // ---------------------------------------------------------------- T12 双规则

    @Test
    void t12_dualRuleSameTickIsBoundedByTwoReloadsWithoutConflictOrFailure() {
        Harness h = new Harness();
        h.boot();

        h.setDesired(CORAL, true);
        h.setDesired(CALCITE, true);   // 第一次 reload 仍在进行 ⇒ 推迟

        assertEquals(1, h.reloadCount);
        h.deliverOurs();               // 第一次结束 ⇒ 收敛 pass 应用第二次变化

        assertEquals(2, h.reloadCount, "无冲突无失败场景：两次变化至多 2 次 reload");
        assertNotEquals(h.reloadTargets.get(0), h.reloadTargets.get(1), "第二次 reload 必须带来实际集合变化");

        h.deliverOurs();
        assertEquals(2, h.reloadCount, "不得出现第三次 reload");
        assertTrue(h.gate.isReady());
        assertTrue(h.selected.contains(CORAL));
        assertTrue(h.selected.contains(CALCITE));
    }

    // ---------------------------------------------------------------- 边界约束

    @Test
    void startupAlwaysConsumesPendingPassSoReadyIsNotStuckFalse() {
        Harness h = new Harness();
        h.boot();
        assertEquals(0, h.reloadCount);
        assertEquals(1, h.passCount, "启动必须执行一次静默点 pass");
        assertTrue(h.gate.isReady());
        assertFalse(h.gate.isPendingSyncPass());
    }

    @Test
    void ruleChangeReRunsUnfinishedPassWithoutWaitingForAnEnd() {
        Harness h = new Harness();
        h.boot();
        h.gate.armPendingPass();          // 欠一次 pass，且不会有任何 END 到达

        h.setDesired(CORAL, true);

        assertTrue(h.passCount >= 2, "静默状态下的规则变化必须自己重跑未完成的 pass，而不是只等 END");
        assertFalse(h.gate.isPendingSyncPass());
    }

    @Test
    void immediateCleanupRunsBeforeSelectionChange() {
        Harness h = new Harness();
        h.boot();
        h.setDesired(CORAL, true);
        h.deliverOurs();

        h.setDesired(CORAL, false);

        int cleanupIndex = h.eventLog.indexOf("cleanup:" + CORAL);
        int selectIndex = h.eventLog.lastIndexOf("select");
        assertTrue(cleanupIndex >= 0, "热关闭必须执行 onPackDisabled 即时清理");
        assertTrue(selectIndex >= 0);
        assertTrue(cleanupIndex < selectIndex, "即时清理必须先于选中集合变更");
        assertEquals(0, h.gate.failureCount());
    }

    @Test
    void offThreadReconcileCreatesNoUnsettleableRequest() {
        Harness h = new Harness();
        h.boot();
        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(
                h.selected, List.of(new RecipePackReconciler.PackDesire(CORAL, "craftableCoralBlocks", true)));

        assertFalse(h.gate.markRequestStarted(plan.targetKey(), h.gate.epoch(), false));
        assertEquals(0, h.gate.inFlightCount());
        assertNull(h.gate.ownOp(), "非服务器线程请求不得留下无法结算的 ownOp");
        assertEquals(1, h.gate.refusedOffThreadRequestCount());
        assertEquals(0, h.gate.reloadRequestCount(), "被拒绝的请求不计入请求账");
    }
}
