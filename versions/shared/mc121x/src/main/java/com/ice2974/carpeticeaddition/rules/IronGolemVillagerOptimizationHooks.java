package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;

/**
 * ironGolemSpawningOptimization 规则的启动否决判定（MC 1.21.x）。
 * 只在任务启动路径（MultiTickTask / SingleTickTask / CompositeTask 的 tryStarting HEAD）调用，
 * 判定为 true 时以“启动条件不成立”语义跳过该任务；不干预已 RUNNING 任务的 tick 与 stop。
 */
public final class IronGolemVillagerOptimizationHooks {

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
        if (!(entity instanceof VillagerEntity villager)) {
            return false;
        }
        Text customName = villager.getCustomName();
        if (customName == null || !IronGolemVillagerOptimizer.matchesOptimizedVillagerName(customName.getString())) {
            return false;
        }
        if (task instanceof IronGolemSkipMarked marked && marked.carpetIceAddition$isIronGolemOptimizationSkipped()) {
            return true;
        }
        return IronGolemVillagerSkipClasses.SKIP_CLASSES.contains(task.getClass());
    }
}
