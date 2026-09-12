package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentState;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentSweeper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

/**
 * enhancedTrident 规则的轮消费点：在 {@code Projectile#onHit} 处接管队首命中并
 * 派发同轮 secondary。
 *
 * <p>命中链（字节码核实，全版本一致）：{@code hitTargetOrDeflectSelf → onHit →
 * onHitEntity + PROJECTILE_LAND}；目标 {@code entity.deflection(this)} 非 NONE
 * （盾反等）时直接 deflect 返回、不经过 onHit。本 WrapMethod 仅在「轮存在 ∧ tick
 * 匹配 ∧ 结果为 EntityHitResult 且实体为预期首目标 ∧ 非 dispatching」时一次性
 * 取走并消费轮，其余情况原样透传（含规则关闭：轮从不会被创建）。R1 的队首由
 * vanilla tick 自行调用（包装在vanilla调用栈内触发）；R2 的队首由活塞轮驱动处经
 * invoker 调用后在此触发。
 *
 * <p>速度隔离：消费时先注入本轮入射向量（R1 = 建轮时速度快照，R2 = 段向量），队首
 * 走原版 onHit（其 onHitEntity 的速度响应——deflect(REVERSE)+缩放——只按队首执行
 * 一次），快照 afterFirst；每个 secondary 派发前再次注入入射向量（击退方向与
 * deflection 判定读取 deltaMovement）、NONE 后恢复 afterFirst；secondary 出现真
 * deflection 时保留 deflect 写入的原版状态并终止本轮。secondary 各自经 invoker
 * {@code hitTargetOrDeflectSelf} 走完整原版链，故伤害、附魔、击退、音效与
 * PROJECTILE_LAND 均按原版逐目标结算。dispatching 标志以 try/finally 恢复，
 * secondary 的嵌套 onHit 直通原版。
 */
@Mixin(Projectile.class)
public abstract class EnhancedTridentOnHitMixin {

    @WrapMethod(method = "onHit")
    private void carpetIceAddition$dispatchRound(HitResult result, Operation<Void> original) {
        if (!((Object) this instanceof ThrownTrident self)
                || !CarpetIceAdditionSettings.enhancedTrident
                || !(self instanceof EnhancedTridentState state)
                || state.carpetIceAddition$isDispatching()) {
            original.call(result);
            return;
        }
        EnhancedTridentState.EnhancedTridentRound round = state.carpetIceAddition$getRound();
        if (round == null
                || !(result instanceof EntityHitResult entityHitResult)
                || entityHitResult.getEntity().getId() != round.expectedFirstEntityId
                || round.tick != self.tickCount) {
            if (round != null && round.tick != self.tickCount) {
                state.carpetIceAddition$setRound(null);
            }
            original.call(result);
            return;
        }
        state.carpetIceAddition$setRound(null);
        state.carpetIceAddition$setDispatching(true);
        try {
            self.setDeltaMovement(round.incoming);
            original.call(result);
            Vec3 afterFirst = self.getDeltaMovement();
            for (EnhancedTridentSweeper.SweepHit secondary : round.secondaryTargets) {
                if (!self.isAlive()) {
                    return;
                }
                Entity target = secondary.entity;
                self.setDeltaMovement(round.incoming);
                try {
                    ProjectileDeflection deflection =
                            ((EnhancedTridentProjectileAccessor) (Object) this)
                                    .carpetIceAddition$hitTargetOrDeflectSelf(
                                            new EntityHitResult(target, secondary.location));
                    if (deflection != ProjectileDeflection.NONE) {
                        return;
                    }
                } catch (Throwable throwable) {
                    CarpetIceAdditionMod.reportFeatureCompatibilityIssue("enhancedTrident", throwable);
                    self.setDeltaMovement(afterFirst);
                    return;
                }
                self.setDeltaMovement(afterFirst);
            }
        } finally {
            state.carpetIceAddition$setDispatching(false);
        }
    }
}
