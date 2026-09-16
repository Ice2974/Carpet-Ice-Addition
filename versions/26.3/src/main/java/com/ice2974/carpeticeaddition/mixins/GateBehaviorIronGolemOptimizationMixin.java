package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：复合行为启动否决与标记载体。
 * GateBehavior#tryStart 为 public final，RunOne 继承且无法重写（仅声明构造器），
 * 对 RunOne 实例的调用同样经过此处；WORK / PLAY 顶层复合束整体标记后，
 * 其内部子行为（含匿名 OneShot、DoNothing）随之永不启动。
 */
@Mixin(GateBehavior.class)
public abstract class GateBehaviorIronGolemOptimizationMixin implements IronGolemSkipMarked {

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
