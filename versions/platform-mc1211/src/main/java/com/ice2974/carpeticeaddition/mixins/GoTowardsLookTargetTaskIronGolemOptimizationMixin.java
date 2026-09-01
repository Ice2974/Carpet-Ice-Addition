package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.GoTowardsLookTargetTask;
import net.minecraft.entity.ai.brain.task.SingleTickTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21 ~ 1.21.1）：IDLE / PLAY 闲逛任务的工厂标记。
 * 1.21.1 中该工厂类名为 GoTowardsLookTargetTask（1.21.3 起更名 GoToLookTargetTask），
 * 其 create 同样返回匿名 SingleTickTask，只能在工厂返回处对实例打标。
 * 已核实仅用于村民的 PLAY 与 IDLE 束，不在 PANIC / REST / MEET 等生成依赖路径中。
 */
@Mixin(GoTowardsLookTargetTask.class)
public abstract class GoTowardsLookTargetTaskIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markLookTargetWalkTask(CallbackInfoReturnable<SingleTickTask<LivingEntity>> cir) {
        Object task = cir.getReturnValue();
        if (task instanceof IronGolemSkipMarked marked) {
            marked.carpetIceAddition$markIronGolemOptimizationSkipped();
        }
    }
}
