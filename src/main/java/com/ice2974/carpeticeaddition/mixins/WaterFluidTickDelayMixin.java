//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides {@code getTickDelay} for vanilla water (both source and flowing)
 * based on the {@code waterFluidTickDelay} rule.
 *
 * <p>When the rule is {@code vanilla} (the default) or {@code freeze}, vanilla's
 * own body runs untouched, so other mods hooking the same method also take
 * effect. Under {@code freeze} the actual flow is frozen by
 * {@code FlowingFluidFreezeMixin}, which re-schedules keep-alive ticks using
 * the runtime {@code getTickDelay} result. Otherwise the configured delay is
 * forced, shadowing vanilla.
 *
 * <p>The identity check ({@code self == Fluids.WATER || self == Fluids.FLOWING_WATER})
 * ensures third-party fluids that extend {@link WaterFluid} are not affected.
 */
@Mixin(WaterFluid.class)
public abstract class WaterFluidTickDelayMixin {

    @Inject(method = "getTickDelay", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$waterTickRate(LevelReader world, CallbackInfoReturnable<Integer> cir) {
        Fluid self = (Fluid) (Object) this;
        if (self != Fluids.WATER && self != Fluids.FLOWING_WATER) {
            return;
        }
        if (CarpetIceAdditionFluidSettings.waterFrozen) {
            return;
        }
        if (CarpetIceAdditionFluidSettings.waterVanilla) {
            return;
        }
        cir.setReturnValue(CarpetIceAdditionFluidSettings.waterDelay);
    }
}
//#endif
