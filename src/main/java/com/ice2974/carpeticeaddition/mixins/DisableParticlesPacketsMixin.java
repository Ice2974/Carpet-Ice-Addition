package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class DisableParticlesPacketsMixin {

    @Inject(
            method = "sendToPlayerIfNearby(Lnet/minecraft/server/network/ServerPlayerEntity;ZDDDLnet/minecraft/network/packet/Packet;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carpetIceAddition$disableParticlesPackets(
            ServerPlayerEntity player,
            boolean force,
            double x,
            double y,
            double z,
            Packet<?> packet,
            CallbackInfoReturnable<Boolean> cir
    ) {
        try {
            if (CarpetIceAdditionSettings.disableParticlesPackets
                    && CarpetIceAdditionMod.shouldEnableDisableParticlesPackets()) {
                cir.setReturnValue(false);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("disableParticlesPackets", throwable);
        }
    }
}
