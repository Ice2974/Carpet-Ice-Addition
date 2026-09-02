package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
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
 * 面向固定式村民交易所：CORE 仅保留防溺水、工作站有效性校验与日程切换，
 * WORK 仅保留补货唯一入口 WorkAtPoi（不再保留原版随机组包装与移动分支，
 * WorkAtPoi 自身的启动冷却与村民每日补货次数、间隔、跨日重置等硬语义不变）。
 * WORK 的 JOB_SITE 存在性门控必须保留：未注册门控的活动无法通过 canDoActivity 进入，
 * 删除门控会导致 WORK 永远无法激活。JOB_SITE / LOOK_TARGET 记忆由保留行为的
 * 必需记忆声明自动注册（26.x Brain.Provider 两参构造经 getRequiredMemories 收集）。
 * 村民应在命名前已就职并绑定工作站，且固定在工作站约 1.73 格内；维护时先改名或关闭规则恢复原版 AI。
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
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> createWorkPackage(float speed) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>of(
                Pair.of(5, new WorkAtPoi())
        );
    }
}
