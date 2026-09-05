package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：名称命中缓存的 setter 驱动刷新点。
 * 命名牌使用、/summon、/data、NBT 载入等改动 CustomName 的路径都会经过 setCustomName；
 * 这里只刷新村民侧的名称命中缓存，不触发任何 Brain 重建，也不影响其它规则。
 */
@Mixin(Entity.class)
public abstract class EntitySetCustomNameIronGolemOptimizationMixin {

    @Inject(method = "setCustomName", at = @At("TAIL"))
    private void carpetIceAddition$refreshIronGolemNameMatchCache(Component name, CallbackInfo ci) {
        if ((Object) this instanceof IronGolemVillagerOptimizationAccess access) {
            access.carpetIceAddition$refreshIronGolemNameMatch();
        }
    }
}
