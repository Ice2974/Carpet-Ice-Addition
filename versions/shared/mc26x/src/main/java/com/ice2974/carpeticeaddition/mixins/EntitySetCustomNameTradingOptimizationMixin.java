package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * villagerTradingOptimization 规则（MC 26.x）：名称变化的唯一汇聚点。
 * 命名牌使用、/summon、/data 等改动 CustomName 的路径都会经过 setCustomName；
 * 实体 NBT 载入期触发的重建是无害瞬态（随后会被 LivingEntity 的 Brain 反序列化
 * 与村民自身的原版 refreshBrain 覆盖为最终正确状态）。
 */
@Mixin(Entity.class)
public abstract class EntitySetCustomNameTradingOptimizationMixin {

    @Inject(method = "setCustomName", at = @At("TAIL"))
    private void carpetIceAddition$refreshTradingOptimizationBrain(Component name, CallbackInfo ci) {
        if (!((Object) this instanceof VillagerTradingOptimizationAccess access)) {
            return;
        }
        if (access.carpetIceAddition$isTradingOptimizationTarget() == access.carpetIceAddition$isTradingOptimizationBaked()) {
            return;
        }
        access.carpetIceAddition$refreshTradingOptimizationBrain();
    }
}
