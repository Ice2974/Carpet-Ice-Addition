package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：IDLE / PLAY 闲逛行为的工厂标记。
 * SetWalkTargetFromLookTarget.create 返回匿名 OneShot，无法在启动期按类识别，
 * 只能在工厂返回处对实例打标。已对 26.1.2 / 26.2 字节码矩阵核实该工厂仅用于
 * 村民的 PLAY 与 IDLE 束，不出现在 PANIC / REST / MEET 等铁傀儡生成依赖路径中。
 */
@Mixin(SetWalkTargetFromLookTarget.class)
public abstract class SetWalkTargetFromLookTargetIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markLookTargetWalkBehavior(CallbackInfoReturnable<OneShot<LivingEntity>> cir) {
        Object behavior = cir.getReturnValue();
        if (behavior instanceof IronGolemSkipMarked marked) {
            marked.carpetIceAddition$markIronGolemOptimizationSkipped();
        }
    }
}
