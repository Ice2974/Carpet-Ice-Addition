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
 * ironGolemSpawningOptimization 规则（MC 1.21.1）：WalkToNearestVisibleWantedItemTask 工厂标记。
 * 纯工厂 holder（create 返回匿名 SingleTickTask），不能进类名单。
 * 该任务是 CORE 的主动寻路捡取物品走位（读取 NearestItemsSensor 写入的
 * NEAREST_VISIBLE_WANTED_ITEM），与铁傀儡生成链无关；Sensor 本身不动，
 * 物品进入原版近身拾取范围时仍可被 MobEntity 原版拾取路径捡起。
 * 村民只使用 create(float, boolean, int) 三参重载，故以参数描述符精确匹配，
 * 避免命中带自定义谓词的四参重载。WalkToNearestVisibleWantedItemTask 是 1.21.1 的
 * Yarn 类名，1.21.3 起更名为 WalkTowardsNearestVisibleWantedItemTask（见 mc1213-12111 档）。
 */
@Mixin(GoToWantedItem.class)
public abstract class GoToWantedItemIronGolemOptimizationMixin {

    @Inject(method = "create(FZI)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(float speed, boolean requiresWalkTarget, int radius, CallbackInfoReturnable<BehaviorControl<LivingEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "WalkToNearestVisibleWantedItemTask.create");
    }
}
