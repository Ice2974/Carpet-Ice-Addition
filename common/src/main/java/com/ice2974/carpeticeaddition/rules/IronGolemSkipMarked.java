package com.ice2974.carpeticeaddition.rules;

/**
 * ironGolemSpawningOptimization 规则的构建期标记接口。
 * 由任务基类 mixin（1.21.x 的 MultiTickTask / SingleTickTask / CompositeTask，
 * 26.x 的 Behavior / OneShot / GateBehavior）实现：
 * 村民 Brain 构建时对可整体跳过的顶层任务打标，启动期否决逻辑只读取标记。
 * 标记仅存在于任务实例的内存字段中，任务列表从不序列化，卸载或重建后自然消失。
 */
public interface IronGolemSkipMarked {

    /**
     * 该任务实例是否被标记为 iron_golem 优化下可跳过的任务。
     */
    boolean carpetIceAddition$isIronGolemOptimizationSkipped();

    /**
     * 标记该任务实例为可跳过。仅在 Brain 构建期调用，重复调用无副作用。
     */
    void carpetIceAddition$markIronGolemOptimizationSkipped();
}
