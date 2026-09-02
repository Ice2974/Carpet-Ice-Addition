package com.ice2974.carpeticeaddition.rules;

/**
 * ironGolemSpawningOptimization 规则的村民侧名称命中缓存接口。
 * 由村民实体 mixin（1.21.x 的 VillagerEntity，26.x 的 Villager）实现：
 * 命中结果由 setCustomName 驱动刷新（setter 驱动），未经过 setter 的路径
 * （NBT 载入等）由首次读取时的惰性计算兜底；热路径只读布尔字段，
 * 不做 Text / Component 的纯文本解析。缓存只反映名称形态，与规则开关无关，
 * 也不写入任何实体 NBT。
 */
public interface IronGolemVillagerOptimizationAccess {

    /**
     * 该村民的 CustomName 纯文本是否精确等于小写 iron_golem。
     * 结果已缓存；首次调用会惰性计算。
     */
    boolean carpetIceAddition$isIronGolemOptimizationTarget();

    /**
     * 依据当前 CustomName 重新计算并覆盖缓存。
     * 由 setCustomName 注入点与惰性计算路径调用。
     */
    void carpetIceAddition$refreshIronGolemNameMatch();
}
