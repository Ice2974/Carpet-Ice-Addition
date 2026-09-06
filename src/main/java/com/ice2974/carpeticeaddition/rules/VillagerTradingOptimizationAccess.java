package com.ice2974.carpeticeaddition.rules;

/**
 * 由村民实体 mixin 实现的村民交易优化状态接口。
 * 只在 Brain 构建、传感器到期触发与名称变更等低频路径上调用，不在每 tick 热路径上使用。
 */
public interface VillagerTradingOptimizationAccess {

    String CARPET_ICE_ADDITION_TRADE_NAME = "trade";

    /**
     * 当前是否应当对该村民启用交易优化（规则开启且名称精确等于小写 trade）。
     */
    boolean carpetIceAddition$isTradingOptimizationTarget();

    /**
     * 该村民当前的 Brain 是否按优化态构建。
     */
    boolean carpetIceAddition$isTradingOptimizationBaked();

    /**
     * 记录本次 Brain 构建是否为优化态。
     */
    void carpetIceAddition$markTradingOptimizationBaked(boolean baked);

    /**
     * 当优化目标状态发生变化时，通过原版 Brain 重建路径刷新该村民的 AI。
     */
    void carpetIceAddition$refreshTradingOptimizationBrain();
}
