package com.ice2974.carpeticeaddition.mixins;

import com.google.common.collect.ImmutableList;
import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：构建期标记层。
 * 26.x 村民活动列表在 Brain 构造期经 ActivitySupplier 调用本类静态方法装配，
 * 建脑（初建 / refreshBrain）都会重走 WORK / PLAY 包构建点，在此对顶层行为打可跳过标记；
 * 标记与规则开关、村民名称完全解耦，之后任意时刻改名或开关规则都能即时生效。
 * 优先级 99 的日程切换项（UpdateActivityFromSchedule.create 返回的匿名 OneShot）必须排除：
 * 若被否决，村民将永久停留在 WORK / PLAY，无法按日程切换到 REST 睡眠，破坏铁傀儡生成链。
 * 优先级常量已对 26.1.2 / 26.2 字节码逐一核对（getWorkPackage 与 getPlayPackage 均为 99）。
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesIronGolemOptimizationMixin {

    private static final int ACTIVITY_SCHEDULE_UPDATE_PRIORITY = 99;

    private static final Logger LOGGER = LoggerFactory.getLogger("CarpetIceAddition/IronGolemSpawningOptimization");

    private static final Set<String> REPORTED_UNMARKABLE_TASKS = ConcurrentHashMap.newKeySet();

    @Inject(method = "getWorkPackage", at = @At("RETURN"))
    private static void carpetIceAddition$markWorkPackageForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        carpetIceAddition$markSkippedTopLevelBehaviors(cir.getReturnValue(), "getWorkPackage");
    }

    @Inject(method = "getPlayPackage", at = @At("RETURN"))
    private static void carpetIceAddition$markPlayPackageForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        carpetIceAddition$markSkippedTopLevelBehaviors(cir.getReturnValue(), "getPlayPackage");
    }

    private static void carpetIceAddition$markSkippedTopLevelBehaviors(ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors, String source) {
        for (Pair<Integer, ? extends BehaviorControl<? super Villager>> pair : behaviors) {
            if (pair.getFirst() == ACTIVITY_SCHEDULE_UPDATE_PRIORITY) {
                continue;
            }
            BehaviorControl<?> behavior = pair.getSecond();
            if (behavior instanceof IronGolemSkipMarked marked) {
                marked.carpetIceAddition$markIronGolemOptimizationSkipped();
            } else if (REPORTED_UNMARKABLE_TASKS.add(behavior.getClass().getName())) {
                LOGGER.debug("Skipping iron golem optimization marking for unmarkable behavior {} in {}", behavior.getClass().getName(), source);
            }
        }
    }
}
