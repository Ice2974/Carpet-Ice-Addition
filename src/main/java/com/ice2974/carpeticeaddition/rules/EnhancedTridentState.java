package com.ice2974.carpeticeaddition.rules;

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
 * grounded-rearm 资格同样 @Unique、非持久：在真正离地时授予（见
 * {@code EnhancedTridentGroundedRearmMixin}），任何非空扫掠建轮时消费，代际校验
 * 见 {@link EnhancedTridentRearmEpoch}。
 */
public interface EnhancedTridentState {

    EnhancedTridentRound carpetIceAddition$getRound();

    void carpetIceAddition$setRound(EnhancedTridentRound round);

    boolean carpetIceAddition$isDispatching();

    void carpetIceAddition$setDispatching(boolean dispatching);

    /**
     * grounded-rearm 资格在指定规则代际下是否仍有效（授予代际 == currentGeneration，
     * 无资格时字段为 -1 永不匹配）。
     *
     * @param currentGeneration 当前规则代际（{@link EnhancedTridentRearmEpoch#current()}）
     */
    boolean carpetIceAddition$isGroundedRearm(int currentGeneration);

    /**
     * 授予一次 grounded-rearm 资格并记录授予时的规则代际（幂等：重复授予刷新代际）。
     */
    void carpetIceAddition$grantGroundedRearm(int generation);

    /**
     * 消费 grounded-rearm 资格（置为无资格）。任何非空扫掠建轮（R1 飞行轮或 R2
     * 活塞轮）时调用——队首普通命中、免伤（hurt=false）、Enderman 或被弹开
     * （deflection 不经过 onHit）均视为资格已使用。
     */
    void carpetIceAddition$consumeGroundedRearm();

    /**
     * 本规则多目标扫掠的统一容差（候选盒 inflate 量），全部受支持版本固定为
     * {@code 0.3}。
     *
     * <p>取值与 1.21.1～1.21.5 的 vanilla {@code AbstractArrow#findHitEntity} 路径
     * 一致（字面量 {@code 0.3f}）；有意不复刻 1.21.6+ 该路径的
     * {@code ProjectileUtil#computeMargin} 随实体年龄从 0 爬坡到 0.3 的行为——本规则
     * 是独立定义的增强行为，命中宽度不应随三叉戟存在时间变化。
     */
    static double sweepMargin() {
        return 0.3D;
    }

    /**
     * 一次实际移动的攻击轮：建轮 tick、预期首目标实体 ID、其余目标（含各自的排序
     * 键与命中位置），以及本轮入射向量（派发前注入 deltaMovement，决定击退方向与
     * deflection 判定）。
     */
    final class EnhancedTridentRound {

        public final int tick;
        public final int expectedFirstEntityId;
        public final List<EnhancedTridentSweeper.SweepHit> secondaryTargets;
        public final Vec3 incoming;

        public EnhancedTridentRound(
                int tick,
                int expectedFirstEntityId,
                List<EnhancedTridentSweeper.SweepHit> secondaryTargets,
                Vec3 incoming) {
            this.tick = tick;
            this.expectedFirstEntityId = expectedFirstEntityId;
            this.secondaryTargets = secondaryTargets;
            this.incoming = incoming;
        }
    }
}
