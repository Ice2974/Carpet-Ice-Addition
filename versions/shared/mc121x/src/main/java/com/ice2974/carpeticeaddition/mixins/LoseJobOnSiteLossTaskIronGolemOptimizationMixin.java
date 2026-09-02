package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.entity.ai.brain.task.LoseJobOnSiteLossTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.passive.VillagerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：LoseJobOnSiteLossTask 工厂标记。
 * 纯工厂 holder（create 返回匿名 SingleTickTask），不能进类名单。
 * 该任务属于 CORE 的职业链（失去工作站时失业重置），与铁傀儡生成链无关；
 * 类名与 create() 签名在 1.21.1-1.21.11 全版本稳定。
 */
@Mixin(LoseJobOnSiteLossTask.class)
public abstract class LoseJobOnSiteLossTaskIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(CallbackInfoReturnable<Task<VillagerEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "LoseJobOnSiteLossTask.create");
    }
}
