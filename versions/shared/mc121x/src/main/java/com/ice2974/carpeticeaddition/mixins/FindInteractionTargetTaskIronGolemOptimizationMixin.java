package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.FindInteractionTargetTask;
import net.minecraft.entity.ai.brain.task.Task;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：FindInteractionTargetTask 工厂标记。
 * 纯工厂 holder（create 返回匿名 SingleTickTask），不能进类名单。
 * 村民的全部调用点（IDLE p3 / WORK p10 / MEET p10）实参均为 (EntityType.PLAYER, 4)，
 * 属玩家张望交互向，与铁傀儡生成链无关（gossip 由 IDLE p2 的 FindEntityTask(VILLAGER) 与
 * p3 的 GatherItemsVillagerTask / TradeWithVillager 承担，不受影响）；
 * 类名与 create(EntityType, int) 签名在 1.21.1-1.21.11 全版本稳定。
 */
@Mixin(FindInteractionTargetTask.class)
public abstract class FindInteractionTargetTaskIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(EntityType<?> type, int maxDistance, CallbackInfoReturnable<Task<LivingEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "FindInteractionTargetTask.create");
    }
}
