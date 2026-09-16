package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.villager.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：AssignProfessionFromJobSite 工厂标记。
 * 纯工厂 holder（create 返回匿名 OneShot），不能进类名单。
 * 该行为属于 CORE 的职业链（在工作站获得职业），与铁傀儡生成链无关；
 * 仅被 VillagerGoalPackages 使用。类名与 create() 签名在 26.1.2 / 26.2 稳定。
 * 命名村民在命名期间不会获得职业，改名后按原版周期恢复就职能力。
 */
@Mixin(AssignProfessionFromJobSite.class)
public abstract class AssignProfessionFromJobSiteIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(CallbackInfoReturnable<BehaviorControl<Villager>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "AssignProfessionFromJobSite.create");
    }
}
