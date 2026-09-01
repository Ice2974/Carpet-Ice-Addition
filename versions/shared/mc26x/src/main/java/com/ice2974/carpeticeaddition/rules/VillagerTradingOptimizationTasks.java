package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GoToPotentialJobSite;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAndFollowTradingPlayerSink;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PoiCompetitorScan;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.behavior.YieldJobSite;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Optional;
import java.util.Set;

/**
 * villagerTradingOptimization 规则的精简活动列表（MC 26.x，Mojang 官方映射）。
 * 返回的 ActivityData 列表会被原版 Brain 构造器按原样注册（含 WORK 的 JOB_SITE 门控条件），
 * CORE 中额外携带日程刷新任务，保证空闲时段（IDLE 为空列表）第二天仍能重新进入 WORK。
 */
public final class VillagerTradingOptimizationTasks {

    private VillagerTradingOptimizationTasks() {
    }

    public static ImmutableList<ActivityData<Villager>> createActivities(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        ImmutableList.Builder<ActivityData<Villager>> builder = ImmutableList.builder();
        builder.add(ActivityData.<Villager>create(Activity.CORE, createCorePackage(profession, 0.5F), Set.of()));
        if (!villager.isBaby()) {
            builder.add(ActivityData.<Villager>create(
                    Activity.WORK,
                    createWorkPackage(0.5F),
                    Set.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT))));
        }
        builder.add(ActivityData.<Villager>create(Activity.IDLE, ImmutableList.of(), Set.of()));
        return builder.build();
    }

    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> createCorePackage(
            Holder<VillagerProfession> profession, float speed) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>of(
                Pair.of(0, new Swim<>(0.8F)),
                Pair.of(0, InteractWithDoor.create()),
                Pair.of(0, new LookAtTargetSink(45, 90)),
                Pair.of(0, ValidateNearbyPoi.create(profession.value().heldJobSite(), MemoryModuleType.JOB_SITE)),
                Pair.of(0, ValidateNearbyPoi.create(profession.value().acquirableJobSite(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(1, new MoveToTargetSink()),
                Pair.of(2, PoiCompetitorScan.create()),
                Pair.of(3, new LookAndFollowTradingPlayerSink(speed)),
                Pair.of(
                        6,
                        AcquirePoi.create(
                                profession.value().acquirableJobSite(),
                                MemoryModuleType.JOB_SITE,
                                MemoryModuleType.POTENTIAL_JOB_SITE,
                                true,
                                Optional.empty(),
                                (serverLevel, blockPos) -> true
                        )
                ),
                Pair.of(7, new GoToPotentialJobSite(speed)),
                Pair.of(8, YieldJobSite.create(speed)),
                Pair.of(10, AssignProfessionFromJobSite.create()),
                Pair.of(10, ResetProfession.create()),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> createWorkPackage(float speed) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>of(
                Pair.of(
                        5,
                        new RunOne<>(
                                ImmutableList.of(
                                        Pair.of(new WorkAtPoi(), 7),
                                        Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE, 0.4F, 4), 2),
                                        Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5)
                                )
                        )
                ),
                Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }
}
