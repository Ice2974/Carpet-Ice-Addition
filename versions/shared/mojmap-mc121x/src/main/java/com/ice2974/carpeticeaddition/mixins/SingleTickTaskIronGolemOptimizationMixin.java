package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：单 tick 行为启动否决与标记载体。
 * 各工厂方法（TaskTriggerer 等）返回的匿名 SingleTickTask 子类共享此处唯一的
 * final tryStarting 实现，是工厂类任务唯一的运行时识别载体。
 */
@Mixin(OneShot.class)
public abstract class SingleTickTaskIronGolemOptimizationMixin implements IronGolemSkipMarked {

    @Unique
    private boolean carpetIceAddition$ironGolemOptimizationSkipped;

    @Override
    public boolean carpetIceAddition$isIronGolemOptimizationSkipped() {
        return carpetIceAddition$ironGolemOptimizationSkipped;
    }

    @Override
    public void carpetIceAddition$markIronGolemOptimizationSkipped() {
        carpetIceAddition$ironGolemOptimizationSkipped = true;
    }

    @Inject(method = "tryStart", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$vetoIronGolemOptimizationStart(ServerLevel world, LivingEntity entity, long time, CallbackInfoReturnable<Boolean> cir) {
        if (IronGolemVillagerOptimizationHooks.shouldVetoStart(this, entity)) {
            cir.setReturnValue(false);
        }
    }
}
