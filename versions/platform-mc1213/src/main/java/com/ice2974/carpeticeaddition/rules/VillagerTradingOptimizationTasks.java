package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.FindPointOfInterestTask;
import net.minecraft.entity.ai.brain.task.FollowCustomerTask;
import net.minecraft.entity.ai.brain.task.ForgetCompletedPointOfInterestTask;
import net.minecraft.entity.ai.brain.task.GoAroundTask;
import net.minecraft.entity.ai.brain.task.GoToPosTask;
import net.minecraft.entity.ai.brain.task.LoseJobOnSiteLossTask;
import net.minecraft.entity.ai.brain.task.MoveToTargetTask;
import net.minecraft.entity.ai.brain.task.OpenDoorsTask;
import net.minecraft.entity.ai.brain.task.RandomTask;
import net.minecraft.entity.ai.brain.task.ScheduleActivityTask;
import net.minecraft.entity.ai.brain.task.StayAboveWaterTask;
import net.minecraft.entity.ai.brain.task.TakeJobSiteTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.UpdateJobSiteTask;
import net.minecraft.entity.ai.brain.task.UpdateLookControlTask;
import net.minecraft.entity.ai.brain.task.VillagerWalkTowardsTask;
import net.minecraft.entity.ai.brain.task.VillagerWorkTask;
import net.minecraft.entity.ai.brain.task.WalkTowardsJobSiteTask;
import net.minecraft.entity.ai.brain.task.WorkStationCompetitionTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;

import java.util.Optional;

/**
 * villagerTradingOptimization 规则的精简任务列表（MC 1.21.2 ~ 1.21.3，新任务名 + VillagerProfession 参数，
 * FindPointOfInterestTask 职业链为 5 参重载）。
 * 仅按原版工厂重建交易/职业/补货链所需任务；CORE 额外携带日程刷新任务，
 * 保证空闲时段（其余活动为空列表）第二天仍能重新进入 WORK。
 */
public final class VillagerTradingOptimizationTasks {

    private VillagerTradingOptimizationTasks() {
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createCoreTasks(
            VillagerProfession profession, float speed) {
        return ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>of(
                Pair.of(0, new StayAboveWaterTask<>(0.8F)),
                Pair.of(0, OpenDoorsTask.create()),
                Pair.of(0, new UpdateLookControlTask(45, 90)),
                Pair.of(0, ForgetCompletedPointOfInterestTask.create(profession.heldWorkstation(), MemoryModuleType.JOB_SITE)),
                Pair.of(0, ForgetCompletedPointOfInterestTask.create(profession.acquirableWorkstation(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(1, new MoveToTargetTask()),
                Pair.of(2, WorkStationCompetitionTask.create()),
                Pair.of(3, new FollowCustomerTask(speed)),
                Pair.of(
                        6,
                        FindPointOfInterestTask.create(
                                profession.acquirableWorkstation(),
                                MemoryModuleType.JOB_SITE,
                                MemoryModuleType.POTENTIAL_JOB_SITE,
                                true,
                                Optional.empty()
                        )
                ),
                Pair.of(7, new WalkTowardsJobSiteTask(speed)),
                Pair.of(8, TakeJobSiteTask.create(speed)),
                Pair.of(10, UpdateJobSiteTask.create()),
                Pair.of(10, LoseJobOnSiteLossTask.create()),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> createWorkTasks(float speed) {
        return ImmutableList.<Pair<Integer, ? extends Task<? super VillagerEntity>>>of(
                Pair.of(
                        5,
                        new RandomTask<>(
                                ImmutableList.of(
                                        Pair.of(new VillagerWorkTask(), 7),
                                        Pair.of(GoAroundTask.create(MemoryModuleType.JOB_SITE, 0.4F, 4), 2),
                                        Pair.of(GoToPosTask.create(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5)
                                )
                        )
                ),
                Pair.of(2, VillagerWalkTowardsTask.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)),
                Pair.of(99, ScheduleActivityTask.create())
        );
    }
}
