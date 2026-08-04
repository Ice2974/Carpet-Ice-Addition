package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freezes the flow of vanilla water and/or lava by cancelling the scheduled
 * fluid tick (the flow entry point) and re-scheduling a keep-alive tick so the
 * fluid does not permanently lose its schedule.
 *
 * <p>Only enters a freeze branch when the tick's target fluid is vanilla
 * water or lava AND the corresponding rule is {@code freeze}. Non-target
 * fluids and non-frozen rules are passed through unchanged.
 *
 * <p>The keep-alive re-schedule uses the fluid currently at the position (not
 * the stale {@code this}), so that if the block has been replaced the stale
 * schedule dies out instead of creating ghost ticks.
 *
 * <p>1.21.3-1.21.11 variant: {@code onScheduledTick(World, BlockPos, BlockState, FluidState)}.
 */
@Mixin(FlowableFluid.class)
public abstract class FlowableFluidFreezeMixin {

    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$freezeFluidTick(
            World world, BlockPos pos, BlockState blockState, FluidState state, CallbackInfo ci) {
        Fluid self = (Fluid) (Object) this;

        boolean isWater = self == Fluids.WATER || self == Fluids.FLOWING_WATER;
        boolean isLava = self == Fluids.LAVA || self == Fluids.FLOWING_LAVA;

        boolean shouldFreeze = (isWater && CarpetIceAdditionFluidSettings.waterFrozen)
                || (isLava && CarpetIceAdditionFluidSettings.lavaFrozen);
        if (!shouldFreeze) {
            return;
        }

        Fluid currentFluid = world.getFluidState(pos).getFluid();
        boolean stillTarget = (isWater && (currentFluid == Fluids.WATER || currentFluid == Fluids.FLOWING_WATER))
                || (isLava && (currentFluid == Fluids.LAVA || currentFluid == Fluids.FLOWING_LAVA));

        if (stillTarget) {
            int keepAliveDelay = currentFluid.getTickRate(world);
            world.scheduleFluidTick(pos, currentFluid, keepAliveDelay);
        }

        ci.cancel();
    }
}
