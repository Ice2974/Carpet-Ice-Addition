package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentHelper;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentSweeper;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentState;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

/**
 * enhancedTrident 规则的 R2（活塞轮）：活塞推动三叉戟产生实际位移时，无视历史
 * {@code dealtDamage} 独立建立攻击轮。
 *
 * <p>原版活塞推动链（{@code PistonMovingBlockEntity#moveEntityByPiston →
 * Entity.move(MoverType.PISTON, …)}，26.x 同名已核实）在 AbstractArrow.move 的
 * override（全部受支持版本存在，super.move + 非 SELF 时 startFalling）中使插地
 * 投射物恢复下落，但不恢复伤害能力——dealtDamage 仍为 true，R1 闸门持续返回 null。
 * 本 WrapMethod 在原版 move 完成后，以「移动前位置/包围盒 + 实际位移段」为扫掠输入：
 *
 * <ul>
 *   <li>只支持 {@code MoverType.PISTON}（SHULKER / SHULKER_BOX 不在本规则范围）；
 *       位移段长不大于 ε（零位移）不建轮、不改速度、不扫描；无候选时不建轮、不写速度。</li>
 *   <li>扫掠用移动<b>前</b>的包围盒与段向量（显式传参，不隐式读移动后状态），段经
 *       方块 clip 裁剪，不隔墙命中；段起点位于候选有效盒内（组 0）即命中——活塞小幅
 *       位移即使始终未离开实体 AABB 也能再次命中。</li>
 *   <li>入射向量 = 段向量本身（伤害固定 8.0+附魔、与速度无关；击退方向与 deflection
 *       依据 deltaMovement，故派发前注入段向量）。队首经 @Invoker
 *       hitTargetOrDeflectSelf 走完整原版链，secondary 由 onHit 包装在同一调用栈内
 *       派发；全部 NONE 后速度恢复为队首响应后的 afterFirst，速度响应每次移动仅按
 *       队首执行一次。队首被盾反（onHit 未被调用、轮未消费）时保留 deflect 写入的
 *       原版状态，finally 中清除残留轮。</li>
 * </ul>
 *
 * <p>R2 完成后不写 {@code dealtDamage}——onHitEntity 链自身在每次命中时置位；下一
 * 个飞行 tick 的 R1 闸门不受影响，重新武装只来自下一次实际位移。规则关闭时不做
 * 任何快照、扫描或状态写入（首行直接透传）。
 */
@Mixin(AbstractArrow.class)
public abstract class EnhancedTridentPistonMoveMixin {

    @WrapMethod(method = "move")
    private void carpetIceAddition$pistonSweep(MoverType type, Vec3 movement, Operation<Void> original) {
        if (type != MoverType.PISTON
                || !((Object) this instanceof ThrownTrident self)
                || !CarpetIceAdditionSettings.enhancedTrident
                || self.level().isClientSide()
                || self.noPhysics) {
            original.call(type, movement);
            return;
        }
        Vec3 pre = self.position();
        AABB preBox = self.getBoundingBox();
        original.call(type, movement);
        try {
            Vec3 post = self.position();
            Vec3 segment = post.subtract(pre);
            if (!EnhancedTridentHelper.isSignificantSegment(segment.x, segment.y, segment.z)) {
                return;
            }
            double maxT = 1.0D;
            BlockHitResult blockHit = self.level().clip(
                    new ClipContext(pre, post, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self));
            if (blockHit.getType() != HitResult.Type.MISS) {
                Vec3 location = blockHit.getLocation();
                maxT = EnhancedTridentHelper.paramAlong(
                        pre.x, pre.y, pre.z, segment.x, segment.y, segment.z,
                        location.x, location.y, location.z);
            }
            List<EnhancedTridentSweeper.SweepHit> hits = EnhancedTridentSweeper.collect(
                    self, pre, post, preBox, EnhancedTridentState.sweepMargin(self), maxT);
            if (hits.isEmpty()) {
                return;
            }
            EnhancedTridentSweeper.SweepHit head = hits.get(0);
            List<EnhancedTridentSweeper.SweepHit> secondaries = new ArrayList<>(hits.size() - 1);
            for (int i = 1; i < hits.size(); i++) {
                secondaries.add(hits.get(i));
            }
            EnhancedTridentState state = (EnhancedTridentState) self;
            state.carpetIceAddition$setRound(new EnhancedTridentState.EnhancedTridentRound(
                    self.tickCount, head.entity.getId(), secondaries, segment, pre, segment));
            try {
                self.setDeltaMovement(segment);
                ((EnhancedTridentProjectileAccessor) (Object) this).carpetIceAddition$hitTargetOrDeflectSelf(
                        new EntityHitResult(head.entity, EnhancedTridentSweeper.hitLocation(pre, segment, head)));
            } finally {
                // 已消费时轮已被 onHit 包装取走（此处为 no-op）；未消费（队首盾反等
                // deflection 路径不经过 onHit）时清除残留，速度保持 deflect 的写入
                state.carpetIceAddition$setRound(null);
            }
        } catch (Throwable throwable) {
            if ((Object) this instanceof EnhancedTridentState state) {
                state.carpetIceAddition$setRound(null);
            }
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("enhancedTrident", throwable);
        }
    }
}
