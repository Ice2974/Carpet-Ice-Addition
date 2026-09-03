package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Set;

/**
 * villagerTradingOptimization 规则的极简活动列表（MC 26.x，Mojang 官方映射）。
 * 面向固定式村民交易所：CORE 保留防溺水、工作站有效性校验、无寻路的近距离工作站认领链
 * （NearbyJobSiteAcquireTask 占票写 POTENTIAL_JOB_SITE，由原版 AssignProfessionFromJobSite 转换为
 * JOB_SITE 并在失业时按工作站类型赋职业）、原版失业重置 ResetProfession 与日程切换；
 * WORK 仅保留补货唯一入口 WorkAtPoi（不再保留原版随机组包装与移动分支，
 * WorkAtPoi 自身的启动冷却与村民每日补货次数、间隔、跨日重置等硬语义不变）。
 * WORK 的 JOB_SITE 存在性门控必须保留：未注册门控的活动无法通过 canDoActivity 进入，
 * 删除门控会导致 WORK 永远无法激活。JOB_SITE / LOOK_TARGET 记忆由保留行为的
 * 必需记忆声明自动注册（26.x Brain.Provider 两参构造经 getRequiredMemories 收集）。
 * 村民不会为工作站寻路或移动，工作站需位于补货判定距离（约 1.73 格）内才会被认领。
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
                Pair.of(0, ValidateNearbyPoi.create(profession.value().heldJobSite(), MemoryModuleType.JOB_SITE)),
                Pair.of(0, ValidateNearbyPoi.create(profession.value().acquirableJobSite(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(6, new NearbyJobSiteAcquireTask(profession)),
                Pair.of(10, AssignProfessionFromJobSite.create()),
                Pair.of(10, ResetProfession.create()),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> createWorkPackage(float speed) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>of(
                Pair.of(5, new WorkAtPoi())
        );
    }
}
