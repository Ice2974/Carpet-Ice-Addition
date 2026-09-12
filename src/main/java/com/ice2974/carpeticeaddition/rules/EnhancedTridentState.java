package com.ice2974.carpeticeaddition.rules;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * enhancedTrident 规则的攻击轮状态 duck 接口，由 ThrownTrident 上的 Mixin
 * （{@code EnhancedTridentMixin}）实现。
 *
 * <p>攻击轮 = 一次实际非零位移段。轮状态完全 invocation-local / tick-local：不写 NBT、
 * 不持久化，建轮与消费发生在同一次实体 tick 的调用栈内；未被消费的轮最迟在下一次
 * 建轮尝试时因 tick 不匹配被清除。{@code dispatching} 防止 secondary 派发经
 * {@code hitTargetOrDeflectSelf → onHit} 重入消费逻辑，必须以 try/finally 恢复。
 */
public interface EnhancedTridentState {

    EnhancedTridentRound carpetIceAddition$getRound();

    void carpetIceAddition$setRound(EnhancedTridentRound round);

    boolean carpetIceAddition$isDispatching();

    void carpetIceAddition$setDispatching(boolean dispatching);

    /**
     * 本版本 vanilla 扫掠容差（候选盒 inflate 量），与 {@code AbstractArrow#findHitEntity}
     * 实际使用的取值保持一致。
     *
     * <p>1.21.6 起该路径经 {@code ProjectileUtil.getEntityHitResult(…, float)} 使用
     * {@code computeMargin(entity)}：随实体年龄从 0 爬坡到 0.3（已对 1.21.6 / 1.21.9 /
     * 1.21.10 / 1.21.11 / 26.1.2 / 26.2 字节码核实公式一致）；1.21.1～1.21.5 的同路径为
     * 字面量 {@code 0.3f}。这里按各版本 vanilla 自身的解析取值，不引入统一常数。
     */
    static double sweepMargin(Entity projectile) {
        //#if MC>=12106
        return ProjectileUtil.computeMargin(projectile);
        //#else
        //$$ return 0.3D;
        //#endif
    }

    /**
     * 一次实际移动的攻击轮：建轮 tick、预期首目标实体 ID、其余目标（含各自的命中
     * 参数）、本轮入射向量（派发前注入 deltaMovement，决定击退方向与 deflection
     * 判定），以及本轮扫掠段起点与段向量（计算各目标的命中位置）。
     */
    final class EnhancedTridentRound {

        public final int tick;
        public final int expectedFirstEntityId;
        public final List<EnhancedTridentSweeper.SweepHit> secondaryTargets;
        public final Vec3 incoming;
        public final Vec3 sweepStart;
        public final Vec3 sweepSegment;

        public EnhancedTridentRound(
                int tick,
                int expectedFirstEntityId,
                List<EnhancedTridentSweeper.SweepHit> secondaryTargets,
                Vec3 incoming,
                Vec3 sweepStart,
                Vec3 sweepSegment) {
            this.tick = tick;
            this.expectedFirstEntityId = expectedFirstEntityId;
            this.secondaryTargets = secondaryTargets;
            this.incoming = incoming;
            this.sweepStart = sweepStart;
            this.sweepSegment = sweepSegment;
        }
    }
}
