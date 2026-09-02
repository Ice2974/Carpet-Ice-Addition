package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.entity.ai.brain.task.TakeJobSiteTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.passive.VillagerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：TakeJobSiteTask 工厂标记。
 * TakeJobSiteTask 是纯工厂 holder（不继承 Task，create 返回匿名 SingleTickTask），
 * 因此不能进类名单，只能在 create RETURN 标记返回实例。
 * 该任务属于 CORE 的职业链（认领潜在工作站），与铁傀儡生成链无关；
 * 类名与 create(float) 签名在 1.21.1-1.21.11 全版本稳定。
 */
@Mixin(TakeJobSiteTask.class)
public abstract class TakeJobSiteTaskIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(float speed, CallbackInfoReturnable<Task<VillagerEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "TakeJobSiteTask.create");
    }
}
