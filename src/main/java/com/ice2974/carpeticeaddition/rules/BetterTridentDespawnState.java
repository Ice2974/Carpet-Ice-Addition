package com.ice2974.carpeticeaddition.rules;

/**
 * betterTridentDespawnCondition 规则的静止计时 duck 接口，由 ThrownTrident 上的
 * Mixin（{@code BetterTridentDespawnConditionMixin}）实现；采样与重置入口在
 * AbstractArrow 级的 {@code BetterTridentDespawnConditionGateMixin}。
 *
 * <p>状态完全 @Unique、非持久、不写 NBT：区块卸载 / 实体保存重载 / 服务器重启后
 * 静止计时重新开始（与 vanilla 插地 tick 计数 {@code inGroundTime} 的非持久语义
 * 一致；vanilla despawn 计时器 {@code life} 自身的持久化不受影响）。规则值变化
 * 通过代际（{@link BetterTridentDespawnEpoch}）使旧计时失效。
 */
public interface BetterTridentDespawnState {

    /**
     * 记录一次「服务端插地 tick」的静止采样：以当前坐标与 anchor 做数值精确比较，
     * 更新连续有效静止采样数（anchor 建立 / 位移检出 / 代际失效后的首次采样计 1，
     * 见 {@link BetterTridentDespawnTracker}）。
     *
     * @param currentEpoch 当前规则代际（{@link BetterTridentDespawnEpoch#current()}）
     */
    void carpetIceAddition$sampleGroundedStationary(double x, double y, double z, int currentEpoch);

    /**
     * 位移事件：anchor 失效、静止计时清零（move() 外力位移观测与 startFalling
     * 离地两个入口共用）。后续首次有效静止采样重新从 1 计数。
     */
    void carpetIceAddition$onDisplacement();

    /** 当前连续有效静止采样数（饱和于 {@link BetterTridentDespawnTracker#STATIONARY_TICKS_REQUIRED}）。 */
    int carpetIceAddition$stationaryTicks();
}
