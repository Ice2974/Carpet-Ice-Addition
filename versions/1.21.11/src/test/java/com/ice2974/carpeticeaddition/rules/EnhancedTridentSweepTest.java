package com.ice2974.carpeticeaddition.rules;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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

    // ---- boxContains（与 vanilla AABB.contains 半开语义严格等价）----

    @Test
    void boxContainsInterior() {
        assertTrue(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.5D, 0.5D, 0.5D));
    }

    @Test
    void boxContainsMinBoundaryInclusive() {
        // vanilla contains: min <= v（min 面含）——min 角同样包含
        assertTrue(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.5D, 0.5D));
        assertTrue(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.5D, 0.0D, 0.5D));
        assertTrue(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.5D, 0.5D, 0.0D));
        assertTrue(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D));
    }

    @Test
    void boxContainsMaxBoundaryExclusive() {
        // vanilla contains: v < max（max 面不含）
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.0D, 0.5D, 0.5D));
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.5D, 1.0D, 0.5D));
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.5D, 0.5D, 1.0D));
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D));
    }

    @Test
    void boxContainsOutside() {
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.5D, 0.5D, 0.5D));
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, -0.5D, 0.5D, 0.5D));
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.5D, -0.5D, 0.5D));
        assertFalse(EnhancedTridentHelper.boxContains(
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 0.5D, 0.5D, 1.5D));
    }

    // ---- vanilla AABB.contains 语义锁定（group-0 判定的边界来源）----

    @Test
    void vanillaAABBContainsIsHalfOpen() {
        AABB box = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        assertTrue(box.contains(0.5D, 0.5D, 0.5D));
        assertTrue(box.contains(0.0D, 0.0D, 0.0D));
        assertFalse(box.contains(1.0D, 0.5D, 0.5D));
        assertFalse(box.contains(0.5D, 1.0D, 0.5D));
        assertFalse(box.contains(0.5D, 0.5D, 1.0D));
        assertFalse(box.contains(1.5D, 0.5D, 0.5D));
        assertFalse(box.contains(-0.1D, 0.5D, 0.5D));
    }

    // ---- vanilla AABB.clip 语义锁定（group-1 几何直接委托 vanilla）----

    @Test
    void vanillaClipNormalEntry() {
        AABB box = new AABB(2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        Optional<Vec3> hit = box.clip(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(4.0D, 0.0D, 0.0D));
        assertTrue(hit.isPresent());
        assertEquals(2.0D, hit.get().x, 1.0E-12D);
        assertEquals(0.0D, hit.get().y, 1.0E-12D);
        assertEquals(0.0D, hit.get().z, 1.0E-12D);
    }

    @Test
    void vanillaClipDiagonalEntry() {
        AABB box = new AABB(1.0D, -1.0D, -1.0D, 3.0D, 3.0D, 1.0D);
        Optional<Vec3> hit = box.clip(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(2.0D, 2.0D, 0.0D));
        assertTrue(hit.isPresent());
        assertEquals(1.0D, hit.get().x, 1.0E-12D);
        assertEquals(1.0D, hit.get().y, 1.0E-12D);
    }

    @Test
    void vanillaClipMissReturnsEmpty() {
        AABB box = new AABB(2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        Optional<Vec3> hit = box.clip(new Vec3(0.0D, 2.0D, 0.0D), new Vec3(4.0D, 2.0D, 0.0D));
        assertFalse(hit.isPresent());
    }

    @Test
    void vanillaClipEndpointExactlyOnFaceMisses() {
        // 段终点恰落在入射面：vanilla 要求 d < t[0]（初值 1.0），d == 1.0 不命中
        AABB box = new AABB(2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        assertFalse(box.clip(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(2.0D, 0.0D, 0.0D)).isPresent());
    }

    @Test
    void vanillaClipTangentialFaceContactHits() {
        // 段与 minY 面精确相切（交叉轴 ±1.0E-7 容差内）→ 命中
        AABB box = new AABB(2.0D, 0.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        Optional<Vec3> hit = box.clip(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(4.0D, 0.0D, 0.0D));
        assertTrue(hit.isPresent());
    }

    @Test
    void vanillaClipStartInsideReturnsEmpty() {
        // 起点在盒内：vanilla 只测试入射面，d < 0 → 空（这些候选由 group-0 处理）
        AABB box = new AABB(-1.0D, -1.0D, -1.0D, 3.0D, 3.0D, 3.0D);
        assertFalse(box.clip(new Vec3(0.0D, 0.0D, 0.0D), new Vec3(4.0D, 0.0D, 0.0D)).isPresent());
    }

    @Test
    void vanillaClipStartOnMinFaceReturnsEmpty() {
        // 起点恰在 min 面上（contains 为 true → group-0）；vanilla clip 因 d == 0 不命中
        AABB box = new AABB(2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        assertFalse(box.clip(new Vec3(2.0D, 0.0D, 0.0D), new Vec3(4.0D, 0.0D, 0.0D)).isPresent());
    }

    @Test
    void vanillaClipStartOnMaxFaceMovingInwardReturnsEmpty() {
        // 起点恰在 max 面上向内移动：contains 为 false（半开）、clip 因 d == 0 为空 → 不命中
        AABB box = new AABB(2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        assertFalse(box.clip(new Vec3(3.0D, 0.0D, 0.0D), new Vec3(2.0D, 0.0D, 0.0D)).isPresent());
    }

    // ---- paramAlong（group-1 排序参数反推）----

    @Test
    void paramAlongComputesClampedFraction() {
        assertEquals(0.25D, EnhancedTridentHelper.paramAlong(
                0.0D, 0.0D, 0.0D, 4.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D), 1.0E-12D);
        assertEquals(0.75D, EnhancedTridentHelper.paramAlong(
                0.0D, 0.0D, 0.0D, 0.0D, -4.0D, 0.0D, 0.0D, -3.0D, 0.0D), 1.0E-12D);
        assertEquals(1.0D, EnhancedTridentHelper.paramAlong(
                0.0D, 0.0D, 0.0D, 2.0D, 0.0D, 0.0D, 9.0D, 0.0D, 0.0D), 0.0D);
        assertEquals(0.0D, EnhancedTridentHelper.paramAlong(
                0.0D, 0.0D, 0.0D, 2.0D, 0.0D, 0.0D, -9.0D, 0.0D, 0.0D), 0.0D);
        assertEquals(0.0D, EnhancedTridentHelper.paramAlong(
                1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D), 0.0D);
    }

    @Test
    void paramAlongMatchesVanillaClipHitPoint() {
        // vanilla clip 命中点 (2,0,0) 对段 (0,0,0)→(4,0,0) 的参数应为 0.5
        AABB box = new AABB(2.0D, -1.0D, -1.0D, 3.0D, 1.0D, 1.0D);
        Vec3 from = new Vec3(0.0D, 0.0D, 0.0D);
        Vec3 to = new Vec3(4.0D, 0.0D, 0.0D);
        Optional<Vec3> hit = box.clip(from, to);
        assertTrue(hit.isPresent());
        assertEquals(0.5D, EnhancedTridentHelper.paramAlong(
                from.x, from.y, from.z, to.x - from.x, to.y - from.y, to.z - from.z,
                hit.get().x, hit.get().y, hit.get().z), 1.0E-12D);
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
    void groupZeroSortedByCenterProjectionAscending() {
        // 两个组 0 候选：投影值升序排列（投影较小者为 head），投影相同按 ID
        assertTrue(EnhancedTridentHelper.compareHits(0, 1.0D, 9, 0, 5.0D, 2) < 0);
        assertTrue(EnhancedTridentHelper.compareHits(0, 5.0D, 2, 0, 1.0D, 9) > 0);
    }

    @Test
    void groupOneSortedByEntryParamAscending() {
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
