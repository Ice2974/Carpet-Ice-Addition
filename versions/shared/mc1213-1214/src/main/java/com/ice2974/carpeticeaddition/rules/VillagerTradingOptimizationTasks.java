package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.ForgetCompletedPointOfInterestTask;
import net.minecraft.entity.ai.brain.task.LoseJobOnSiteLossTask;
import net.minecraft.entity.ai.brain.task.ScheduleActivityTask;
import net.minecraft.entity.ai.brain.task.StayAboveWaterTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.UpdateJobSiteTask;
import net.minecraft.entity.ai.brain.task.VillagerWorkTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;

/**
 * villagerTradingOptimization 规则的极简任务列表（MC 1.21.2 ~ 1.21.4，新任务名 + VillagerProfession 参数）。
 * 面向固定式村民交易所：CORE 保留防溺水、工作站有效性校验、无寻路的近距离工作站认领链
 * （NearbyJobSiteAcquireTask 占票写 POTENTIAL_JOB_SITE，由原版 UpdateJobSiteTask 转换为 JOB_SITE
 * 并在失业时按工作站类型赋职业）、原版失业重置 LoseJobOnSiteLossTask 与日程切换；
 * WORK 仅保留补货唯一入口 VillagerWorkTask（不再保留原版随机组包装与移动分支，
 * WorkAtPoi 自身的启动冷却与村民每日补货次数、间隔、跨日重置等硬语义不变）。
 * 村民不会为工作站寻路或移动，工作站需位于补货判定距离（约 1.73 格）内才会被认领。
 */
public final class VillagerTradingOptimizationTasks {

    private VillagerTradingOptimizationTasks() {
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createCoreTasks(
            VillagerProfession profession, float speed) {
        return ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>of(
                Pair.of(0, new StayAboveWaterTask<>(0.8F)),
                Pair.of(0, ForgetCompletedPointOfInterestTask.create(profession.heldWorkstation(), MemoryModuleType.JOB_SITE)),
                Pair.of(0, ForgetCompletedPointOfInterestTask.create(profession.acquirableWorkstation(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(6, new NearbyJobSiteAcquireTask(profession)),
                Pair.of(10, UpdateJobSiteTask.create()),
                Pair.of(10, LoseJobOnSiteLossTask.create()),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createWorkTasks(float speed) {
        return ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>of(
                Pair.of(5, new VillagerWorkTask())
        );
    }
}
