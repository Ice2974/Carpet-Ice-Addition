package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.mixins.EnhancedTridentAbstractArrowAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * enhancedTrident 攻击轮的目标收集：对一次实际移动段做有序多目标扫掠。
 *
 * <p>broadphase 与 vanilla {@code AbstractArrow#findHitEntity} 的构造一致
 * （起点盒 {@code expandTowards(段向量).inflate(1.0)}）。逐候选几何直接复用 vanilla
 * {@code AABB}（全部受支持版本字节码核实一致，与 vanilla
 * {@code ProjectileUtil#getEntityHitResult} 的逐候选测试同构）：膨胀 {@code margin}
 * 的候选 AABB 按半开区间 contains 判定包含段起点 → 组 0（{@code hitVec = start}，
 * 本规则对 vanilla 飞行的唯一扩展）；否则 {@code clip(start, end)} 求交，命中点存在
 * → 组 1（命中位置即 vanilla 返回的命中点，排序参数 t 经
 * {@link EnhancedTridentHelper#paramAlong} 反推；vanilla 严格保证 d ∈ (0, 1)，无需
 * 额外 maxT 裁剪）。组 0 恒先于组 1；组 0 内按候选盒中心沿段向量的投影升序，组 1
 * 内按 t 升序，均以实体 ID 升序 tie-break，得到与 broadphase 迭代顺序无关的确定性
 * 全序；{@code Level#getEntities} 返回的候选天然按实体去重。
 */
public final class EnhancedTridentSweeper {

    private EnhancedTridentSweeper() {
    }

    public static final class SweepHit {

        public final Entity entity;
        /** 0 = 段起点位于候选有效盒内（半开 contains）；1 = 段经 vanilla clip 射入候选有效盒。 */
        public final int group;
        /** 组内排序键：组 0 为候选盒中心沿段向量的投影；组 1 为命中点参数 t。 */
        public final double sortKey;
        /** 命中位置：组 0 为段起点；组 1 为 vanilla {@code AABB#clip} 返回的命中点。 */
        public final Vec3 location;

        public SweepHit(Entity entity, int group, double sortKey, Vec3 location) {
            this.entity = entity;
            this.group = group;
            this.sortKey = sortKey;
            this.location = location;
        }
    }

    public static List<SweepHit> collect(
            ThrownTrident trident, Vec3 start, Vec3 end, AABB startBox, double margin) {
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
                hits.add(new SweepHit(candidate, 0, EnhancedTridentHelper.centerProjection(
                        start.x, start.y, start.z, segment.x, segment.y, segment.z,
                        box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ), start));
            } else {
                Optional<Vec3> hitVec = box.clip(start, end);
                if (hitVec.isPresent()) {
                    double t = EnhancedTridentHelper.paramAlong(
                            start.x, start.y, start.z, segment.x, segment.y, segment.z,
                            hitVec.get().x, hitVec.get().y, hitVec.get().z);
                    hits.add(new SweepHit(candidate, 1, t, hitVec.get()));
                }
            }
        }

        hits.sort((hitA, hitB) -> EnhancedTridentHelper.compareHits(
                hitA.group, hitA.sortKey, hitA.entity.getId(),
                hitB.group, hitB.sortKey, hitB.entity.getId()));
        return hits;
    }
}
