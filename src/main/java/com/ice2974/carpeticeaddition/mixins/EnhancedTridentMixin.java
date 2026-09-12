package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentHelper;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentSweeper;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentState;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * enhancedTrident 规则的 R1（正常飞行轮）：在 {@code ThrownTrident#findHitEntity}
 * HEAD 处将单目标扫描替换为多目标有序扫掠。
 *
 * <p>注入点是全部受支持版本飞行命中路径的唯一汇合点（字节码核实）：1.21.1 的
 * tick 直调该方法；1.21.3～1.21.10 经 stepMoveAndHit 调用；1.21.11+ 的复数
 * {@code findHitEntities} 在 ThrownTrident 中委托回本方法。返回队首后，vanilla
 * 自身完成 {@code hitTargetOrDeflectSelf(队首)}，由 {@code EnhancedTridentOnHitMixin}
 * 在 {@code Projectile#onHit} 处接管并派发 secondary（每个目标都走完整原版命中链）。
 *
 * <p>R1 门控：仅当 {@code dealtDamage == false} 时建立攻击轮——{@code dealtDamage}
 * 在首次实体命中时即置位（onHitEntity 首段），因此命中后的残余速度段在后续 tick
 * 落入 vanilla 自身的 {@code if (dealtDamage) return null} 闸门，不会跨 tick 重打；
 * 本轮已收集的 secondary 不受首目标置位影响（派发在同一次调用栈内完成）。忠诚
 * 返回段（noPhysics）与客户端侧直接走原版路径；段长不大于 ε 时同样不建轮（静止
 * 绝不伤害）。规则关闭时不做任何额外扫描，完全原版。
 *
 * <p>本 Mixin 同时实现 {@link EnhancedTridentState} duck 接口，承载轮状态与
 * dispatching 防重入标志（均 @Unique、非持久、随实体对象生灭）。
 */
@Mixin(ThrownTrident.class)
public abstract class EnhancedTridentMixin implements EnhancedTridentState {

    @Shadow
    private boolean dealtDamage;

    @Unique
    private EnhancedTridentState.EnhancedTridentRound carpetIceAddition$round;

    @Unique
    private boolean carpetIceAddition$dispatching;

    @Override
    public EnhancedTridentState.EnhancedTridentRound carpetIceAddition$getRound() {
        return this.carpetIceAddition$round;
    }

    @Override
    public void carpetIceAddition$setRound(EnhancedTridentState.EnhancedTridentRound round) {
        this.carpetIceAddition$round = round;
    }

    @Override
    public boolean carpetIceAddition$isDispatching() {
        return this.carpetIceAddition$dispatching;
    }

    @Override
    public void carpetIceAddition$setDispatching(boolean dispatching) {
        this.carpetIceAddition$dispatching = dispatching;
    }

    @Inject(method = "findHitEntity", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$sweepFlight(Vec3 start, Vec3 end, CallbackInfoReturnable<EntityHitResult> cir) {
        if (!CarpetIceAdditionSettings.enhancedTrident) {
            return;
        }
        ThrownTrident self = (ThrownTrident) (Object) this;
        if (self.level().isClientSide() || self.noPhysics || this.dealtDamage) {
            return;
        }
        Vec3 segment = end.subtract(start);
        if (!EnhancedTridentHelper.isSignificantSegment(segment.x, segment.y, segment.z)) {
            return;
        }
        try {
            if (this.carpetIceAddition$round != null && this.carpetIceAddition$round.tick != self.tickCount) {
                this.carpetIceAddition$round = null;
            }
            // R1 的 end 已被 vanilla tick 按方块碰撞裁剪，maxT 恒为 1
            List<EnhancedTridentSweeper.SweepHit> hits = EnhancedTridentSweeper.collect(
                    self, start, end, self.getBoundingBox(),
                    EnhancedTridentState.sweepMargin(), 1.0D);
            if (hits.isEmpty()) {
                return;
            }
            EnhancedTridentSweeper.SweepHit head = hits.get(0);
            List<EnhancedTridentSweeper.SweepHit> secondaries = new ArrayList<>(hits.size() - 1);
            for (int i = 1; i < hits.size(); i++) {
                secondaries.add(hits.get(i));
            }
            this.carpetIceAddition$round = new EnhancedTridentState.EnhancedTridentRound(
                    self.tickCount, head.entity.getId(), secondaries, self.getDeltaMovement(), start, segment);
            cir.setReturnValue(new EntityHitResult(
                    head.entity, EnhancedTridentSweeper.hitLocation(start, segment, head)));
        } catch (Throwable throwable) {
            this.carpetIceAddition$round = null;
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("enhancedTrident", throwable);
        }
    }
}
