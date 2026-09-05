package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.FluidTickDelayUtil;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides {@code getTickRate} for vanilla lava (both source and flowing)
 * based on the {@code lavaFluidTickDelay} rule.
 *
 * <p>1.21.11 variant: ultrawarm is accessed via the environment attribute
 * {@code EnvironmentAttributes.FAST_LAVA_GAMEPLAY}, which replaced
 * {@code DimensionType.ultrawarm()} starting from 1.21.11.
 *
 * <p>When the rule is {@code freeze}, vanilla's default delay is returned so the
 * freeze Mixin can use it as a keep-alive period. Otherwise the configured delay
 * is returned, divided by 3 (minimum 1) in ultrawarm dimensions.
 *
 * <p>The identity check ({@code self == Fluids.LAVA || self == Fluids.FLOWING_LAVA})
 * ensures third-party fluids that extend {@link LavaFluid} are not affected.
 */
@Mixin(LavaFluid.class)
public abstract class LavaFluidTickRateMixin {

    @Inject(method = "getTickDelay", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$lavaTickRate(LevelReader world, CallbackInfoReturnable<Integer> cir) {
        Fluid self = (Fluid) (Object) this;
        if (self != Fluids.LAVA && self != Fluids.FLOWING_LAVA) {
            return;
        }
        if (CarpetIceAdditionFluidSettings.lavaFrozen) {
            return;
        }
        EnvironmentAttributeReader access = world.environmentAttributes();
        Boolean fastLava = access.getDimensionValue(EnvironmentAttributes.FAST_LAVA);
        boolean ultrawarm = fastLava != null && fastLava;
        cir.setReturnValue(FluidTickDelayUtil.getLavaDelay(CarpetIceAdditionFluidSettings.lavaDelay, ultrawarm));
    }
}
