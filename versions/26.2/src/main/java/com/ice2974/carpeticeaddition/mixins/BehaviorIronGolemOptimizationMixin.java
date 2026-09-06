package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：多 tick 行为启动否决与标记载体。
 * tryStart 为 public final，Brain 与 GateBehavior 对具体子类实例的调用都会经过此处。
 * 只否决 STOPPED 任务的启动尝试，绝不干预已 RUNNING 任务的 tickOrStop / doStop。
 */
@Mixin(Behavior.class)
public abstract class BehaviorIronGolemOptimizationMixin implements IronGolemSkipMarked {

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
    private void carpetIceAddition$vetoIronGolemOptimizationStart(ServerLevel level, LivingEntity entity, long gameTime, CallbackInfoReturnable<Boolean> cir) {
        if (IronGolemVillagerOptimizationHooks.shouldVetoStart(this, entity)) {
            cir.setReturnValue(false);
        }
    }
}
