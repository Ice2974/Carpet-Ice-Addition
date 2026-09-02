package com.ice2974.carpeticeaddition.mixins;

import com.google.common.collect.ImmutableList;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.villager.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：构建期标记层。
 * 26.x 村民活动列表在 Brain 构造期经 ActivitySupplier 调用本类静态方法装配，
 * 建脑（初建 / refreshBrain）都会重走 WORK / PLAY / IDLE / MEET 包构建点，在此对顶层行为
 * 打可跳过标记；标记与规则开关、村民名称完全解耦，之后任意时刻改名或开关规则都能即时生效。
 * 优先级 99 的日程切换项（UpdateActivityFromSchedule.create 返回的匿名 OneShot）必须排除：
 * 若被否决，村民将永久停留在对应活动，无法按日程切换到 REST 睡眠，破坏铁傀儡生成链。
 * IDLE / MEET 在本规则语义（现代恐吓式刷铁机）下整表禁用，不保留 gossip 链：
 * 命名村民白天保持静止，仅经 p99 日程切换在夜间进入 REST 睡眠、或经 CORE 的
 * VillagerPanicTrigger 进入 PANIC 触发铁傀儡生成；命名时已处于 MEET 的村民由保留的
 * p99 日程切换自然退出，不会卡死。
 * 另标记 WORK / REST / PANIC / PRE_RAID / RAID / HIDE 共用的 minimalLook 张望组合
 * （getMinimalLookBehavior 返回的 RunOne，纯张望行为，与生成链无关）；
 * getFullLookBehavior 只被 PLAY / MEET / IDLE 三个已整表标记的包使用，无需单独标记。
 * 优先级常量已对 26.1.2 / 26.2 反编译源码逐一核对（各包 p99 为
 * UpdateActivityFromSchedule、两个张望组合均为 priority 5）。
 * 本标记只作用于原版 VillagerGoalPackages 返回的列表；第三方模组对 Brain 任务表的
 * 修改不在此覆盖范围内，属于待人工确认的兼容项。
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesIronGolemOptimizationMixin {

    private static final int ACTIVITY_SCHEDULE_UPDATE_PRIORITY = 99;

    @Inject(method = "getWorkPackage", at = @At("RETURN"))
    private static void carpetIceAddition$markWorkPackageForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        carpetIceAddition$markSkippedTopLevelBehaviors(cir.getReturnValue(), "getWorkPackage");
    }

    @Inject(method = "getPlayPackage", at = @At("RETURN"))
    private static void carpetIceAddition$markPlayPackageForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        carpetIceAddition$markSkippedTopLevelBehaviors(cir.getReturnValue(), "getPlayPackage");
    }

    @Inject(method = "getIdlePackage", at = @At("RETURN"))
    private static void carpetIceAddition$markIdlePackageForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        carpetIceAddition$markSkippedTopLevelBehaviors(cir.getReturnValue(), "getIdlePackage");
    }

    @Inject(method = "getMeetPackage", at = @At("RETURN"))
    private static void carpetIceAddition$markMeetPackageForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        carpetIceAddition$markSkippedTopLevelBehaviors(cir.getReturnValue(), "getMeetPackage");
    }

    @Inject(method = "getMinimalLookBehavior", at = @At("RETURN"))
    private static void carpetIceAddition$markMinimalLookBehaviorForIronGolemOptimization(CallbackInfoReturnable<Pair<Integer, BehaviorControl<LivingEntity>>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue().getSecond(), "getMinimalLookBehavior");
    }

    private static void carpetIceAddition$markSkippedTopLevelBehaviors(ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors, String source) {
        for (Pair<Integer, ? extends BehaviorControl<? super Villager>> pair : behaviors) {
            if (pair.getFirst() == ACTIVITY_SCHEDULE_UPDATE_PRIORITY) {
                continue;
            }
            IronGolemVillagerOptimizationHooks.markTaskInstance(pair.getSecond(), source);
        }
    }
}
