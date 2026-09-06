package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：GoToWantedItem 工厂标记。
 * 纯工厂 holder（create 返回匿名 OneShot），不能进类名单。
 * 该行为是 CORE 的主动寻路捡取物品走位（读取 NearestItemSensor 写入的
 * NEAREST_VISIBLE_WANTED_ITEM），与铁傀儡生成链无关；Sensor 本身不动，
 * 物品进入原版近身拾取范围时仍可被 Mob 原版拾取路径捡起。
 * 村民只使用 create(float, boolean, int) 三参重载，故以参数描述符精确匹配。
 * 该工厂另被 AllayAi / PiglinAi 使用：其它实体的实例会被一并标记，
 * 但否决以“实体为 iron_golem 命名村民”为前置门，标记对它们永远不生效（惰性无害）。
 * 类名与签名在 26.1.2 / 26.2 稳定。
 */
@Mixin(GoToWantedItem.class)
public abstract class GoToWantedItemIronGolemOptimizationMixin {

    @Inject(method = "create(FZI)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(float speedModifier, boolean interruptOngoingWalk, int maxDistToWalk, CallbackInfoReturnable<BehaviorControl<LivingEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "GoToWantedItem.create");
    }
}
