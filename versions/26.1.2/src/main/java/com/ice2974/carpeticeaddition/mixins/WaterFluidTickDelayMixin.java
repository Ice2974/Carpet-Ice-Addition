package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides {@code getTickDelay} for vanilla water (both source and flowing)
 * based on the {@code waterFluidTickDelay} rule (26.x Mojang mappings).
 *
 * <p>When the rule is {@code freeze}, vanilla's default delay is returned so the
 * freeze Mixin can use it as a keep-alive period. Otherwise the configured delay
 * is returned.
 *
 * <p>The identity check ({@code self == Fluids.WATER || self == Fluids.FLOWING_WATER})
 * ensures third-party fluids that extend {@link WaterFluid} are not affected.
 */
@Mixin(WaterFluid.class)
public abstract class WaterFluidTickDelayMixin {

    @Inject(method = "getTickDelay", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$waterTickDelay(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        Fluid self = (Fluid) (Object) this;
        if (self != Fluids.WATER && self != Fluids.FLOWING_WATER) {
            return;
        }
        if (CarpetIceAdditionFluidSettings.waterFrozen) {
            return;
        }
        cir.setReturnValue(CarpetIceAdditionFluidSettings.waterDelay);
    }
}
