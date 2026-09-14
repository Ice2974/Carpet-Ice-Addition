package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * betterTridentDespawnCondition 静止计时状态机的时间线单测（core 平台）。
 *
 * <p>锁定关键语义：首次有效静止采样计 1（非 0）；第 1–1199 次采样不允许消失，
 * 第 1200 次恰达条件；无扰动时静止阈值与 vanilla {@code life >= 1200} 在同一采样
 * tick 达成（无 +1 偏差）；移动（数值不等 / 位移事件 / 代际失效）后自 1 重新累积，
 * 与落地语义对称。
 */
class BetterTridentDespawnTrackerTest {

    private static final double X = 10.0D;
    private static final double Y = 64.0D;
    private static final double Z = -5.0D;

    private static BetterTridentDespawnTracker.State newStationaryState() {
        BetterTridentDespawnTracker.State state = new BetterTridentDespawnTracker.State();
        state.onGroundedSample(X, Y, Z, 0);
        return state;
    }

    private static void sampleTimes(BetterTridentDespawnTracker.State state, int times) {
        for (int i = 0; i < times; i++) {
            state.onGroundedSample(X, Y, Z, 0);
        }
    }

    // ---- 首次有效静止采样 = 1，1199 拒绝 / 1200 放行 ----

    @Test
    void firstValidSampleCountsOne() {
        BetterTridentDespawnTracker.State state = new BetterTridentDespawnTracker.State();
        assertEquals(-1, state.epochGeneration);
        state.onGroundedSample(X, Y, Z, 0);
        assertEquals(1, state.stationaryTicks());
        assertFalse(state.isDespawnPermitted());
    }

    @Test
    void samplesOneThrough1199AreNotPermitted() {
        BetterTridentDespawnTracker.State state = new BetterTridentDespawnTracker.State();
        for (int i = 1; i <= 1199; i++) {
            state.onGroundedSample(X, Y, Z, 0);
            assertEquals(i, state.stationaryTicks());
            assertFalse(state.isDespawnPermitted());
        }
    }

    @Test
    void twelveHundredthSamplePermits() {
        BetterTridentDespawnTracker.State state = new BetterTridentDespawnTracker.State();
        sampleTimes(state, 1200);
        assertEquals(1200, state.stationaryTicks());
        assertTrue(state.isDespawnPermitted());
    }

    @Test
    void counterSaturatesAtThreshold() {
        BetterTridentDespawnTracker.State state = new BetterTridentDespawnTracker.State();
        sampleTimes(state, 1300);
        assertEquals(BetterTridentDespawnTracker.STATIONARY_TICKS_REQUIRED, state.stationaryTicks());
    }

    // ---- 与 vanilla life 同 tick 对齐（无 +1 偏差）----

    @Test
    void undisturbedPermitAlignsWithVanillaDespawnTick() {
        BetterTridentDespawnTracker.State state = new BetterTridentDespawnTracker.State();
        int life = 0;
        int firstVanillaDiscardTick = -1;
        int firstRulePermittedTick = -1;
        for (int tick = 0; tick < 1300; tick++) {
            state.onGroundedSample(X, Y, Z, 0);
            life++;
            boolean vanillaDiscard = !BetterTridentDespawnTracker.vanillaWouldNotDiscard(life - 1);
            if (vanillaDiscard && firstVanillaDiscardTick < 0) {
                firstVanillaDiscardTick = tick;
            }
            if (vanillaDiscard && state.isDespawnPermitted() && firstRulePermittedTick < 0) {
                firstRulePermittedTick = tick;
            }
        }
        assertEquals(1199, firstVanillaDiscardTick);
        assertEquals(firstVanillaDiscardTick, firstRulePermittedTick);
    }

    // ---- 移动重置：位移后第 1 / 1199 / 1200 次采样 ----

    @Test
    void movementDetectedAtSampleRestartsFromOne() {
        BetterTridentDespawnTracker.State state = newStationaryState();
        sampleTimes(state, 1198);
        assertEquals(1199, state.stationaryTicks());
        // 第 1200 次采样位置数值不等（位移发生在本次采样前）：重置为 1
        state.onGroundedSample(X + 1.0D, Y, Z, 0);
        assertEquals(1, state.stationaryTicks());
        assertFalse(state.isDespawnPermitted());
        // 其后第 1198 次累计 1199（拒绝），第 1199 次累计 1200（放行）
        for (int i = 2; i <= 1199; i++) {
            state.onGroundedSample(X + 1.0D, Y, Z, 0);
            assertEquals(i, state.stationaryTicks());
            assertFalse(state.isDespawnPermitted());
        }
        state.onGroundedSample(X + 1.0D, Y, Z, 0);
        assertEquals(1200, state.stationaryTicks());
        assertTrue(state.isDespawnPermitted());
    }

    @Test
    void ulpLevelCoordinateChangeIsMovement() {
        BetterTridentDespawnTracker.State state = newStationaryState();
        sampleTimes(state, 100);
        state.onGroundedSample(Math.nextUp(X), Y, Z, 0);
        assertEquals(1, state.stationaryTicks());

        BetterTridentDespawnTracker.State stateY = newStationaryState();
        sampleTimes(stateY, 100);
        stateY.onGroundedSample(X, Math.nextDown(Y), Z, 0);
        assertEquals(1, stateY.stationaryTicks());

        BetterTridentDespawnTracker.State stateZ = newStationaryState();
        sampleTimes(stateZ, 100);
        stateZ.onGroundedSample(X, Y, Math.nextUp(Z), 0);
        assertEquals(1, stateZ.stationaryTicks());
    }

    @Test
    void negativeZeroVersusPositiveZeroIsNotMovement() {
        BetterTridentDespawnTracker.State state = new BetterTridentDespawnTracker.State();
        state.onGroundedSample(0.0D, 0.0D, 0.0D, 0);
        state.onGroundedSample(-0.0D, 0.0D, 0.0D, 0);
        assertEquals(2, state.stationaryTicks());
        assertTrue(BetterTridentDespawnTracker.samePosition(0.0D, 0.0D, 0.0D, -0.0D, -0.0D, -0.0D));
    }

    @Test
    void displacementEventThenReturnToOldAnchorStillRestarts() {
        // 同 tick 往返：move() 观测通道先重置，即使位置回到旧 anchor 也不得复原计数
        BetterTridentDespawnTracker.State state = newStationaryState();
        sampleTimes(state, 100);
        state.onDisplacement();
        assertEquals(0, state.stationaryTicks());
        assertFalse(state.hasAnchor);
        state.onGroundedSample(X, Y, Z, 0);
        assertEquals(1, state.stationaryTicks());
        assertFalse(state.isDespawnPermitted());
    }

    // ---- 代际失效 ----

    @Test
    void epochChangeRestartsFromOne() {
        BetterTridentDespawnTracker.State state = newStationaryState();
        sampleTimes(state, 500);
        state.onGroundedSample(X, Y, Z, 1);
        assertEquals(1, state.stationaryTicks());
        assertFalse(state.isDespawnPermitted());
        // 新代际内正常继续累积
        state.onGroundedSample(X, Y, Z, 1);
        assertEquals(2, state.stationaryTicks());
    }

    // ---- anchor 无蠕变 ----

    @Test
    void anchorNeverAdvancesDuringStationaryRun() {
        BetterTridentDespawnTracker.State state = newStationaryState();
        sampleTimes(state, 1500);
        assertEquals(X, state.anchorX);
        assertEquals(Y, state.anchorY);
        assertEquals(Z, state.anchorZ);
    }

    // ---- 预测直通真值表（门控扣留仅发生于「vanilla 将 discard 且静止不足」）----

    @Test
    void vanillaWouldNotDiscardTruthTable() {
        assertTrue(BetterTridentDespawnTracker.vanillaWouldNotDiscard(0));
        assertTrue(BetterTridentDespawnTracker.vanillaWouldNotDiscard(1197));
        assertTrue(BetterTridentDespawnTracker.vanillaWouldNotDiscard(1198));
        assertFalse(BetterTridentDespawnTracker.vanillaWouldNotDiscard(1199));
        assertFalse(BetterTridentDespawnTracker.vanillaWouldNotDiscard(1200));
        assertFalse(BetterTridentDespawnTracker.vanillaWouldNotDiscard(5000));
        // int 上界：预测判断不得因 life+1 溢出为负而错误直通 vanilla
        assertFalse(BetterTridentDespawnTracker.vanillaWouldNotDiscard(Integer.MAX_VALUE));
        // 历史溢出存量（NBT 回绕为负）：vanilla 原样执行不会 discard，直通按原路径自然恢复
        assertTrue(BetterTridentDespawnTracker.vanillaWouldNotDiscard(-32768));
    }

    // ---- 扣留 tick 的 life 饱和推进（恒不超过 vanilla despawn 阈值）----

    @Test
    void detainedLifeAdvanceTruthTable() {
        assertEquals(1199, BetterTridentDespawnTracker.detainedLifeAdvance(1198));
        assertEquals(1200, BetterTridentDespawnTracker.detainedLifeAdvance(1199));
        assertEquals(1200, BetterTridentDespawnTracker.detainedLifeAdvance(1200));
        assertEquals(1200, BetterTridentDespawnTracker.detainedLifeAdvance(32767));
        // int 上界：先判断后自增，不得执行 life+1 溢出运算
        assertEquals(1200, BetterTridentDespawnTracker.detainedLifeAdvance(Integer.MAX_VALUE));
        // 历史溢出存量：跟随 vanilla 的 +1 语义自然恢复
        assertEquals(-32767, BetterTridentDespawnTracker.detainedLifeAdvance(-32768));
    }

    @Test
    void detainedLifeNeverExceedsThresholdAcrossLongDetention() {
        int life = 1199;
        for (int tick = 0; tick < 100_000; tick++) {
            life = BetterTridentDespawnTracker.detainedLifeAdvance(life);
        }
        assertEquals(BetterTridentDespawnTracker.VANILLA_DESPAWN_THRESHOLD, life);
        // 恢复 vanilla 直通（规则关闭 / 静止已满）后：1200 状态仍按 vanilla 时机 discard
        assertFalse(BetterTridentDespawnTracker.vanillaWouldNotDiscard(life));
    }

    @Test
    void gateHoldsOnlyWhenVanillaWouldDiscardAndStationaryInsufficient() {
        // stationary=1199 且 life+1>=1200：唯一扣留组合
        assertFalse(1199 >= BetterTridentDespawnTracker.STATIONARY_TICKS_REQUIRED
                || BetterTridentDespawnTracker.vanillaWouldNotDiscard(1199));
        // 静止已满：放行（vanilla 原样执行）
        assertTrue(BetterTridentDespawnTracker.STATIONARY_TICKS_REQUIRED >= BetterTridentDespawnTracker.STATIONARY_TICKS_REQUIRED
                || BetterTridentDespawnTracker.vanillaWouldNotDiscard(5000));
        // vanilla 本就不会 discard：直通
        assertTrue(1 >= BetterTridentDespawnTracker.STATIONARY_TICKS_REQUIRED
                || BetterTridentDespawnTracker.vanillaWouldNotDiscard(100));
    }

    @Test
    void samePositionTruthTable() {
        assertTrue(BetterTridentDespawnTracker.samePosition(X, Y, Z, X, Y, Z));
        assertFalse(BetterTridentDespawnTracker.samePosition(X, Y, Z, Math.nextUp(X), Y, Z));
        assertFalse(BetterTridentDespawnTracker.samePosition(X, Y, Z, X, Y, Z + 1.0D));
    }
}
