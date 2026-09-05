package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ReactToBell;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：ReactToBell 工厂标记（CORE p0）。
 * 纯工厂 holder（create 返回匿名 OneShot），不能进类名单。
 * 原版逻辑：读 HEARD_BELL_TIME，无袭击时 setActiveActivityIfPossible(HIDE)，
 * 不写任何 memory——否决本任务即从根源阻止命名村民因响钟进入 HIDE 活动。
 * 命名时已处于 HIDE 的村民不受影响：HIDE 包本体未标记，其 p0 的
 * SetHiddenState 在超时后 forget HEARD_BELL_TIME / HIDING_PLACE 并
 * refreshActivities 退出；响钟事件写入的 HEARD_BELL_TIME 在解除优化后按原版机制继续使用。
 * 类名与 create() 签名已对 26.1.2 / 26.2 反编译源码核对一致。
 */
@Mixin(ReactToBell.class)
public abstract class ReactToBellIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(CallbackInfoReturnable<BehaviorControl<LivingEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "ReactToBell.create");
    }
}
