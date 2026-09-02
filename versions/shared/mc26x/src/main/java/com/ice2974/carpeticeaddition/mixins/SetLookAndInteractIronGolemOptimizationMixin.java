package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：SetLookAndInteract 工厂标记
 * （1.21.x 的 FindInteractionTargetTask 对应物）。
 * 纯工厂 holder（create 返回匿名 OneShot），不能进类名单。
 * 村民的全部调用点（IDLE p3 / WORK p10 / MEET p10）实参均为 (EntityType.PLAYER, 4)，
 * 属玩家张望交互向，与铁傀儡生成链无关（gossip 由 IDLE p2 的 InteractWith(VILLAGER)
 * 与 p3 的 TradeWithVillager 承担，不受影响）。
 * 该工厂另被 PiglinAi 使用：其它实体的实例会被一并标记，但否决以
 * “实体为 iron_golem 命名村民”为前置门，标记对它们永远不生效（惰性无害）。
 * 26.2 将常量拆分到 EntityTypes 类，但 create 参数类型仍为 EntityType，注入不受影响。
 */
@Mixin(SetLookAndInteract.class)
public abstract class SetLookAndInteractIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(EntityType<?> type, int interactionRange, CallbackInfoReturnable<BehaviorControl<LivingEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "SetLookAndInteract.create");
    }
}
