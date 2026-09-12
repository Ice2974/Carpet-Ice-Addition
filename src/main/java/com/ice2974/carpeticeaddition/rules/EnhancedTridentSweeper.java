package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.mixins.EnhancedTridentAbstractArrowAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * enhancedTrident 攻击轮的目标收集：对一次实际移动段做有序多目标扫掠。
 *
 * <p>broadphase 与 vanilla {@code AbstractArrow#findHitEntity} 的构造一致
 * （起点盒 {@code expandTowards(段向量).inflate(1.0)}），逐候选做精确测试：膨胀
 * {@code margin} 的候选 AABB 包含段起点 → 组 0（{@code hitVec = start}）；否则取
 * 段对该盒的入射参数 t，t 超出 {@code maxT} 的候选丢弃 → 组 1。组 0 恒先于组 1；
 * 组 0 内按候选盒中心沿段向量的投影升序，组 1 内按入射参数 t 升序，均以实体 ID
 * 升序 tie-break，得到与 broadphase 迭代顺序无关的确定性全序；
 * {@code Level#getEntities} 返回的候选天然按实体去重。
 */
public final class EnhancedTridentSweeper {

    private EnhancedTridentSweeper() {
    }

    public static final class SweepHit {

        public final Entity entity;
        /** 0 = 段起点位于候选有效盒内；1 = 段射入候选有效盒。 */
        public final int group;
        /** 组 1 的入射参数，∈ [0, maxT]；组 0 未使用（命中位置为段起点）。 */
        public final double t;
        /** 组内排序键：组 0 为候选盒中心沿段向量的投影，组 1 与 t 相同。 */
        public final double sortKey;

        public SweepHit(Entity entity, int group, double t, double sortKey) {
            this.entity = entity;
            this.group = group;
            this.t = t;
            this.sortKey = sortKey;
        }
    }

    public static List<SweepHit> collect(
            ThrownTrident trident, Vec3 start, Vec3 end, AABB startBox, double margin, double maxT) {
        Vec3 segment = end.subtract(start);
        AABB sweepBox = startBox.expandTowards(segment).inflate(1.0D);
        EnhancedTridentAbstractArrowAccessor accessor = (EnhancedTridentAbstractArrowAccessor) trident;
        List<Entity> candidates =
                trident.level().getEntities(trident, sweepBox, accessor::carpetIceAddition$canHitEntity);

        List<SweepHit> hits = new ArrayList<>(candidates.size());
        for (Entity candidate : candidates) {
            AABB box = candidate.getBoundingBox().inflate(margin);
            if (EnhancedTridentHelper.boxContains(
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                    start.x, start.y, start.z)) {
                hits.add(new SweepHit(candidate, 0, 0.0D, EnhancedTridentHelper.centerProjection(
                        start.x, start.y, start.z, segment.x, segment.y, segment.z,
                        box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)));
            } else {
                double t = EnhancedTridentHelper.segmentBoxEntryT(
                        start.x, start.y, start.z, segment.x, segment.y, segment.z,
                        box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                if (!Double.isNaN(t) && t <= maxT) {
                    hits.add(new SweepHit(candidate, 1, t, t));
                }
            }
        }

        hits.sort((hitA, hitB) -> EnhancedTridentHelper.compareHits(
                hitA.group, hitA.sortKey, hitA.entity.getId(),
                hitB.group, hitB.sortKey, hitB.entity.getId()));
        return hits;
    }

    /** 该命中的命中位置：组 0 为段起点，组 1 为段上参数 t 处。 */
    public static Vec3 hitLocation(Vec3 start, Vec3 segment, SweepHit hit) {
        return hit.group == 0 ? start : start.add(segment.scale(hit.t));
    }
}
