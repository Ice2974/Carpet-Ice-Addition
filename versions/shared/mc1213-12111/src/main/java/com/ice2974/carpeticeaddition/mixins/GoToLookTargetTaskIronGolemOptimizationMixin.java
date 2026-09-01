package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.GoToLookTargetTask;
import net.minecraft.entity.ai.brain.task.SingleTickTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.3+）：IDLE / PLAY 闲逛任务的工厂标记。
 * GoToLookTargetTask.create 返回匿名 SingleTickTask，无法在启动期按类识别，
 * 只能在工厂返回处对实例打标。已核实该工厂仅用于村民的 PLAY 与 IDLE 束，
 * 不出现在 PANIC / REST / MEET 等铁傀儡生成依赖路径中。
 */
@Mixin(GoToLookTargetTask.class)
public abstract class GoToLookTargetTaskIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markLookTargetWalkTask(CallbackInfoReturnable<SingleTickTask<LivingEntity>> cir) {
        Object task = cir.getReturnValue();
        if (task instanceof IronGolemSkipMarked marked) {
            marked.carpetIceAddition$markIronGolemOptimizationSkipped();
        }
    }
}
