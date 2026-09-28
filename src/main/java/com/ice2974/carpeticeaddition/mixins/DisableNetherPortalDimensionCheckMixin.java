package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseFireBlock.class)
public abstract class DisableNetherPortalDimensionCheckMixin {

    @Inject(method = "inPortalDimension", at = @At("RETURN"), cancellable = true)
    private static void carpetIceAddition$disableNetherPortalDimensionCheck(
            Level level,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CarpetIceAdditionSettings.disableNetherPortalDimensionCheck
                && Boolean.FALSE.equals(cir.getReturnValue())) {
            cir.setReturnValue(true);
        }
    }
}
