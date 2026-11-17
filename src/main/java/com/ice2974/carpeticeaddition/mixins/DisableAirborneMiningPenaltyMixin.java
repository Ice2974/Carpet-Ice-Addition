package com.ice2974.carpeticeaddition.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class DisableAirborneMiningPenaltyMixin {

    @ModifyExpressionValue(
            method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"
            )
    )
    private boolean carpetIceAddition$skipAirborneMiningPenalty(boolean onGround) {
        return CarpetIceAdditionSettings.disableAirborneMiningPenalty || onGround;
    }
}
