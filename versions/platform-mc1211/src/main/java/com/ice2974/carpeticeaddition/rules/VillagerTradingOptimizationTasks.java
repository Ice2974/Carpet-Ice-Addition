package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.ForgetCompletedPointOfInterestTask;
import net.minecraft.entity.ai.brain.task.ScheduleActivityTask;
import net.minecraft.entity.ai.brain.task.StayAboveWaterTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerWorkTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;

/**
 * villagerTradingOptimization 规则的极简任务列表（MC 1.21 ~ 1.21.1，旧任务名 + VillagerProfession 参数）。
 * 面向固定式村民交易所：CORE 仅保留防溺水、工作站有效性校验与日程切换，
 * WORK 仅保留补货唯一入口 VillagerWorkTask（不再保留原版随机组包装与移动分支，
 * WorkAtPoi 自身的启动冷却与村民每日补货次数、间隔、跨日重置等硬语义不变）。
 * 村民应在命名前已就职并绑定工作站，且固定在工作站约 1.73 格内；维护时先改名或关闭规则恢复原版 AI。
 */
public final class VillagerTradingOptimizationTasks {

    private VillagerTradingOptimizationTasks() {
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createCoreTasks(
            VillagerProfession profession, float speed) {
        return ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>of(
                Pair.of(0, new StayAboveWaterTask(0.8F)),
                Pair.of(0, ForgetCompletedPointOfInterestTask.create(profession.heldWorkstation(), MemoryModuleType.JOB_SITE)),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createWorkTasks(float speed) {
        return ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>of(
                Pair.of(5, new VillagerWorkTask())
        );
    }
}
