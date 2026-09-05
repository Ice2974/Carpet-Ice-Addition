package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.1）：GoToWorkTask 工厂标记。
 * 纯工厂 holder（create 返回匿名 SingleTickTask），不能进类名单。
 * 该任务属于 CORE 的职业链（走到工作站获得职业），与铁傀儡生成链无关。
 * GoToWorkTask 是 1.21.1 的 Yarn 类名，1.21.3 起更名为 UpdateJobSiteTask
 * （对应版本见 mc1213-12111 档），故本类仅存在于 platform-mc1211。
 */
@Mixin(AssignProfessionFromJobSite.class)
public abstract class AssignProfessionFromJobSiteIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(CallbackInfoReturnable<BehaviorControl<Villager>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "GoToWorkTask.create");
    }
}
