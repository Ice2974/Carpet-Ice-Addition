package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.command.MachineStatusRollbackCommandMatcher;
import com.ice2974.carpeticeaddition.command.MachineStatusRollbackWarningHandler;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMachineStatusRollbackWarningMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPlayNetworkHandlerMachineStatusRollbackWarningMixin.class);

    @Shadow public ServerPlayer player;

    @Inject(method = "handleChatCommand", at = @At("HEAD"))
    private void carpetIceAddition$warnOnUnsignedRollbackCommandPacket(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        this.carpetIceAddition$scheduleRollbackWarning(packet.command());
    }

    @Inject(method = "handleSignedChatCommand", at = @At("HEAD"))
    private void carpetIceAddition$warnOnSignedRollbackCommandPacket(ServerboundChatCommandSignedPacket packet, CallbackInfo ci) {
        this.carpetIceAddition$scheduleRollbackWarning(packet.command());
    }

    @Inject(method = "handleChat", at = @At("HEAD"))
    private void carpetIceAddition$warnOnRollbackChatPacket(ServerboundChatPacket packet, CallbackInfo ci) {
        this.carpetIceAddition$scheduleRollbackWarning(packet.message());
    }

    private void carpetIceAddition$scheduleRollbackWarning(String rawInput) {
        if (!MachineStatusRollbackCommandMatcher.matches(rawInput)) {
            return;
        }

        MinecraftServer server = ((ServerCommonNetworkHandlerAccessor) this).carpetIceAddition$getServer();
        if (server == null) {
            LOGGER.warn("Matched rollback command but could not resolve MinecraftServer for player {}", this.player.getName().getString());
            return;
        }

        server.execute(() -> MachineStatusRollbackWarningHandler.warnIfNeeded(server, this.player, rawInput));
    }
}
