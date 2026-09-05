package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.FluidTickDelayUtil;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides {@code getTickDelay} for vanilla lava (both source and flowing)
 * based on the {@code lavaFluidTickDelay} rule (26.x Mojang mappings).
 *
 * <p>When the rule is {@code freeze}, vanilla's default delay is returned so the
 * freeze Mixin can use it as a keep-alive period. Otherwise the configured delay
 * is returned, divided by 3 (minimum 1) in fast-lava dimensions.
 *
 * <p>The identity check ({@code self == Fluids.LAVA || self == Fluids.FLOWING_LAVA})
 * ensures third-party fluids that extend {@link LavaFluid} are not affected.
 */
@Mixin(LavaFluid.class)
public abstract class LavaFluidTickDelayMixin {

    @Inject(method = "getTickDelay", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$lavaTickDelay(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        Fluid self = (Fluid) (Object) this;
        if (self != Fluids.LAVA && self != Fluids.FLOWING_LAVA) {
            return;
        }
        if (CarpetIceAdditionFluidSettings.lavaFrozen) {
            return;
        }
        boolean fastLava = carpetIceAddition$isFastLava(level);
        cir.setReturnValue(FluidTickDelayUtil.getLavaDelay(CarpetIceAdditionFluidSettings.lavaDelay, fastLava));
    }

    /**
     * Replicates vanilla's fast-lava logic using the 26.x
     * {@link EnvironmentAttributes#FAST_LAVA} environment attribute.
     *
     * <p>Uses {@link Unique} to avoid shadowing vanilla's own private
     * {@code LavaFluid.isFastLava(LevelReader)} method.
     */
    @Unique
    private static boolean carpetIceAddition$isFastLava(LevelReader level) {
        Boolean value = level.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA);
        return value != null && value;
    }
}
