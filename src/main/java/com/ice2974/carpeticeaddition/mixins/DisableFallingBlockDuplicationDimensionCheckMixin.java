package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class DisableFallingBlockDuplicationDimensionCheckMixin {
    @Inject(method = "teleport", at = @At("RETURN"))
    private void carpetIceAddition$disableFallingBlockDuplicationDimensionCheck(
            TeleportTransition transition,
            CallbackInfoReturnable<Entity> cir
    ) {
        if (!CarpetIceAdditionSettings.disableFallingBlockDuplicationDimensionCheck || cir.getReturnValue() == null) {
            return;
        }

        FallingBlockEntity source = (FallingBlockEntity) (Object) this;
        // Cross-dimensional vanilla teleport returns a replacement entity and leaves this source level unchanged.
        if (source.level().dimension() != transition.newLevel().dimension()) {
            source.forceTickAfterTeleportToDuplicate = true;
        }
    }
}
