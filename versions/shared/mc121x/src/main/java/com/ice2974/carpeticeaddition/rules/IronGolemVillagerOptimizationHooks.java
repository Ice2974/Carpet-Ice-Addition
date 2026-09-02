package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;

import net.minecraft.entity.LivingEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ironGolemSpawningOptimization 规则的启动否决判定与构建期标记工具（MC 1.21.x）。
 * 否决只在任务启动路径（MultiTickTask / SingleTickTask / CompositeTask 的 tryStarting HEAD）调用，
 * 判定为 true 时以“启动条件不成立”语义跳过该任务；不干预已 RUNNING 任务的 tick 与 stop。
 * 名称命中结果由村民侧 mixin 以 setter/lazy 双驱动缓存维护，热路径只读布尔字段，不做 Text 解析。
 */
public final class IronGolemVillagerOptimizationHooks {

    private static final Logger LOGGER = LoggerFactory.getLogger("CarpetIceAddition/IronGolemSpawningOptimization");

    private static final Set<String> REPORTED_UNMARKABLE_TASKS = ConcurrentHashMap.newKeySet();

    private IronGolemVillagerOptimizationHooks() {
    }

    /**
     * 是否应否决该任务的本次启动尝试。
     * 判定顺序按成本从低到高排列，规则关闭时只付出一次静态字段读取。
     */
    public static boolean shouldVetoStart(Object task, LivingEntity entity) {
        if (!CarpetIceAdditionSettings.ironGolemSpawningOptimization) {
            return false;
        }
        if (!(entity instanceof IronGolemVillagerOptimizationAccess access) || !access.carpetIceAddition$isIronGolemOptimizationTarget()) {
            return false;
        }
        if (task instanceof IronGolemSkipMarked marked && marked.carpetIceAddition$isIronGolemOptimizationSkipped()) {
            return true;
        }
        return IronGolemVillagerSkipClasses.SKIP_CLASSES.contains(task.getClass());
    }

    /**
     * 构建期标记入口：把工厂 / 任务列表构建点产生的任务实例标记为可跳过。
     * 标记与规则开关、村民名称完全解耦，否决时才做实体门控；
     * 其它模组经同一工厂创建的实例若被连带标记，也会因实体门控而永不被否决。
     */
    public static void markTaskInstance(Object task, String source) {
        if (task == null) {
            return;
        }
        if (task instanceof IronGolemSkipMarked marked) {
            marked.carpetIceAddition$markIronGolemOptimizationSkipped();
        } else if (REPORTED_UNMARKABLE_TASKS.add(task.getClass().getName())) {
            LOGGER.debug("Skipping iron golem optimization marking for unmarkable task {} in {}", task.getClass().getName(), source);
        }
    }
}
