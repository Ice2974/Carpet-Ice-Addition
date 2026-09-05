//#if MC>=12103 && MC<260000

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
 * ironGolemSpawningOptimization 规则（MC 1.21.3-1.21.11）：WalkTowardsNearestVisibleWantedItemTask 工厂标记。
 * 纯工厂 holder（create 返回匿名 SingleTickTask），不能进类名单。
 * 该任务是 CORE 的主动寻路捡取物品走位（读取 NearestItemsSensor 写入的
 * NEAREST_VISIBLE_WANTED_ITEM），与铁傀儡生成链无关；Sensor 本身不动，
 * 物品进入原版近身拾取范围时仍可被 MobEntity 原版拾取路径捡起。
 * 村民只使用 create(float, boolean, int) 三参重载，故以参数描述符精确匹配。
 * WalkTowardsNearestVisibleWantedItemTask 是 1.21.3 起 WalkToNearestVisibleWantedItemTask
 * 的更名（Yarn 改名断点，语义不变），1.21.1 版本见 platform-mc1211 档。
 */
@Mixin(GoToWantedItem.class)
public abstract class WalkTowardsNearestVisibleWantedItemTaskIronGolemOptimizationMixin {

    @Inject(method = "create(FZI)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(float speed, boolean requiresWalkTarget, int radius, CallbackInfoReturnable<BehaviorControl<LivingEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "WalkTowardsNearestVisibleWantedItemTask.create");
    }
}
//#endif
