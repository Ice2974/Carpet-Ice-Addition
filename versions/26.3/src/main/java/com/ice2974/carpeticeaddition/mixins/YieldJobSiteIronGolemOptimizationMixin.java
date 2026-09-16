package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.YieldJobSite;
import net.minecraft.world.entity.npc.villager.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：YieldJobSite 工厂标记。
 * 纯工厂 holder（create 返回匿名 OneShot），不能进类名单。
 * 该行为属于 CORE 的职业链（让出被竞争的工作站），与铁傀儡生成链无关；
 * 仅被 VillagerGoalPackages 使用。类名与 create(float) 签名在 26.1.2 / 26.2 稳定。
 */
@Mixin(YieldJobSite.class)
public abstract class YieldJobSiteIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(float speedModifier, CallbackInfoReturnable<BehaviorControl<Villager>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "YieldJobSite.create");
    }
}
