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
 * 建脑（初建 / refreshBrain）都会重走 WORK / PLAY 包构建点，在此对顶层行为打可跳过标记；
 * 标记与规则开关、村民名称完全解耦，之后任意时刻改名或开关规则都能即时生效。
 * 优先级 99 的日程切换项（UpdateActivityFromSchedule.create 返回的匿名 OneShot）必须排除：
 * 若被否决，村民将永久停留在 WORK / PLAY，无法按日程切换到 REST 睡眠，破坏铁傀儡生成链。
 * 另标记 IDLE / MEET / PLAY 共用的张望组合（getFullLookBehavior 返回的 RunOne，
 * 即各活动包中唯一的 priority 5 顶层张望组合，按工厂方法直接标记而非按优先级粗标，
 * 不会波及第三方 Brain 模组加入的其它行为）；REST / PANIC / WORK 等包的
 * getMinimalLookBehavior 不标记。优先级常量已对 26.1.2 / 26.2 反编译源码与字节码逐一核对
 * （getWorkPackage 与 getPlayPackage 均为 99；getFullLookBehavior 为 5）。
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

    @Inject(method = "getFullLookBehavior", at = @At("RETURN"))
    private static void carpetIceAddition$markFullLookBehaviorForIronGolemOptimization(CallbackInfoReturnable<Pair<Integer, BehaviorControl<LivingEntity>>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue().getSecond(), "getFullLookBehavior");
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
