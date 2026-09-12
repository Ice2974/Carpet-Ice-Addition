package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnhancedTridentSweepTest {

    // ---- isSignificantSegment ----

    @Test
    void zeroSegmentIsNotSignificant() {
        assertFalse(EnhancedTridentHelper.isSignificantSegment(0.0D, 0.0D, 0.0D));
        assertFalse(EnhancedTridentHelper.isSignificantSegment(1.0E-8, 0.0D, 0.0D));
    }

    @Test
    void realDisplacementIsSignificant() {
        assertTrue(EnhancedTridentHelper.isSignificantSegment(0.001D, 0.0D, 0.0D));
        assertTrue(EnhancedTridentHelper.isSignificantSegment(0.0D, -0.5D, 0.0D));
        assertTrue(EnhancedTridentHelper.isSignificantSegment(1.0D, 1.0D, 1.0D));
    }

    // ---- segmentBoxEntryT ----

    @Test
    void segmentEnteringBoxAlongX() {
        // 段 (0,0,0) → (4,0,0)，盒 [2,-1,-1]..[3,1,1]，入射参数应为 0.5
        assertEquals(0.5D, EnhancedTridentHelper.segmentBoxEntryT(
                0.0D, 0.0D, 0.0D, 4.0D, 0.0D, 0.0D,
                2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D), 1.0E-12D);
    }

    @Test
    void segmentMissingBoxReturnsNaN() {
        // 段沿 X 穿过 y=2 高度，盒在 y ∈ [-1,1]
        assertTrue(Double.isNaN(EnhancedTridentHelper.segmentBoxEntryT(
                0.0D, 2.0D, 0.0D, 4.0D, 0.0D, 0.0D,
                2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D)));
    }

    @Test
    void segmentEndingBeforeBoxReturnsNaN() {
        // 盒在段终点之外：完整参数域 [0,1] 内无交点
        assertTrue(Double.isNaN(EnhancedTridentHelper.segmentBoxEntryT(
                0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D,
                2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D)));
    }

    @Test
    void startInsideBoxYieldsZero() {
        // 段起点在盒内 → 组 0 语义：入射参数为 0
        assertEquals(0.0D, EnhancedTridentHelper.segmentBoxEntryT(
                2.5D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D,
                2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D), 0.0D);
    }

    @Test
    void diagonalSegmentEntry() {
        // 段 (0,0,0) → (2,2,0)，盒 [1,-1,-1]..[3,3,1]，x 平面先于 y 平面 → t = 0.5
        assertEquals(0.5D, EnhancedTridentHelper.segmentBoxEntryT(
                0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 0.0D,
                1.0D, -1.0D, -1.0D, 3.0D, 3.0D, 1.0D), 1.0E-12D);
    }

    @Test
    void axisAlignedSegmentParallelToSlab() {
        // 段沿 X，起点 y/z 均在盒范围外沿上（含边界）→ 命中
        assertEquals(0.5D, EnhancedTridentHelper.segmentBoxEntryT(
                0.0D, 1.0D, -1.0D, 4.0D, 0.0D, 0.0D,
                2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D), 1.0E-12D);
    }

    // ---- boxContains ----

    @Test
    void boxContainsBoundaryInclusive() {
        assertTrue(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.5D, 1.0D));
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.5D, 0.5D, 0.5D));
    }

    // ---- centerProjection（组 0 排序键）----

    @Test
    void centerProjectionAlongMovementDirection() {
        // 段 (0,0,0) → (2,0,0)：盒 [0,-1,-1]..[1,1,1]（中心 x=0.5）投影 0.5×2=1；
        // 盒 [2,-1,-1]..[3,1,1]（中心 x=2.5）投影 2.5×2=5
        assertEquals(1.0D, EnhancedTridentHelper.centerProjection(
                0.0D, 0.0D, 0.0D, 2.0D, 0.0D, 0.0D,
                0.0D, -1.0D, -1.0D, 1.0D, 1.0D, 1.0D), 1.0E-12D);
        assertEquals(5.0D, EnhancedTridentHelper.centerProjection(
                0.0D, 0.0D, 0.0D, 2.0D, 0.0D, 0.0D,
                2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D), 1.0E-12D);
    }

    @Test
    void centerProjectionCanFallBehindStart() {
        // 中心在段起点后方 → 负投影：仍可与前方候选全序比较
        assertTrue(EnhancedTridentHelper.centerProjection(
                2.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D,
                0.0D, -1.0D, -1.0D, 1.0D, 1.0D, 1.0D) < 0.0D);
    }

    // ---- compareHits ----

    @Test
    void groupZeroAlwaysSortsFirst() {
        // 组 0（起点在盒内）优先于任何组 1，即使组 1 的排序键更小
        assertTrue(EnhancedTridentHelper.compareHits(0, 9.0D, 42, 1, 0.0D, 1) < 0);
        assertTrue(EnhancedTridentHelper.compareHits(1, 0.1D, 42, 0, -5.0D, 1) > 0);
    }

    @Test
    void groupZeroSortedByCenterProjection() {
        // 两个组 0 候选：中心投影更靠移动方向前方的排在前面（成为 head）
        assertTrue(EnhancedTridentHelper.compareHits(0, 1.0D, 9, 0, 5.0D, 2) < 0);
        assertTrue(EnhancedTridentHelper.compareHits(0, 5.0D, 2, 0, 1.0D, 9) > 0);
    }

    @Test
    void groupOneSortedByEntryParam() {
        assertTrue(EnhancedTridentHelper.compareHits(1, 0.25D, 9, 1, 0.75D, 2) < 0);
        assertTrue(EnhancedTridentHelper.compareHits(1, 0.75D, 2, 1, 0.25D, 9) > 0);
    }

    @Test
    void equalSortKeyResolvedByEntityId() {
        assertTrue(EnhancedTridentHelper.compareHits(1, 0.5D, 3, 1, 0.5D, 7) < 0);
        assertTrue(EnhancedTridentHelper.compareHits(1, 0.5D, 7, 1, 0.5D, 3) > 0);
        assertTrue(EnhancedTridentHelper.compareHits(0, 2.0D, 3, 0, 2.0D, 7) < 0);
        assertTrue(EnhancedTridentHelper.compareHits(0, 2.0D, 7, 0, 2.0D, 3) > 0);
        assertEquals(0, EnhancedTridentHelper.compareHits(0, 0.0D, 5, 0, 0.0D, 5));
    }
}
