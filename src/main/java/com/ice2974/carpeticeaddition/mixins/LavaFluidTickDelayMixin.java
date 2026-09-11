//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.FluidTickDelayUtil;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
//#if MC>=12111
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.attribute.EnvironmentAttributes;
//#endif
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides {@code getTickDelay} for vanilla lava (both source and flowing)
 * based on the {@code lavaFluidTickDelay} rule.
 *
 * <p>1.21.11 variant: ultrawarm is accessed via the environment attribute
 * {@code EnvironmentAttributes.FAST_LAVA}, which replaced
 * {@code DimensionType.ultrawarm()} starting from 1.21.11.
 *
 * <p>When the rule is {@code vanilla} (the default) or {@code freeze}, vanilla's
 * own body runs untouched, so other mods hooking the same method also take
 * effect. Under {@code freeze} the actual flow is frozen by
 * {@code FlowingFluidFreezeMixin}, which re-schedules keep-alive ticks using
 * the runtime {@code getTickDelay} result. Otherwise the configured delay is
 * forced, divided by 3 (minimum 1) in ultrawarm dimensions.
 *
 * <p>The identity check ({@code self == Fluids.LAVA || self == Fluids.FLOWING_LAVA})
 * ensures third-party fluids that extend {@link LavaFluid} are not affected.
 */
@Mixin(LavaFluid.class)
public abstract class LavaFluidTickDelayMixin {

    @Inject(method = "getTickDelay", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$lavaTickRate(LevelReader world, CallbackInfoReturnable<Integer> cir) {
        Fluid self = (Fluid) (Object) this;
        if (self != Fluids.LAVA && self != Fluids.FLOWING_LAVA) {
            return;
        }
        if (CarpetIceAdditionFluidSettings.lavaFrozen) {
            return;
        }
        if (CarpetIceAdditionFluidSettings.lavaVanilla) {
            return;
        }
//#if MC>=12111
        EnvironmentAttributeReader access = world.environmentAttributes();
        Boolean fastLava = access.getDimensionValue(EnvironmentAttributes.FAST_LAVA);
        boolean ultrawarm = fastLava != null && fastLava;
//#else
//$$        boolean ultrawarm = world.dimensionType().ultraWarm();
//#endif
        cir.setReturnValue(FluidTickDelayUtil.getLavaDelay(CarpetIceAdditionFluidSettings.lavaDelay, ultrawarm));
    }
}
//#endif
