package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.command.MachineStatusRollbackCommandMatcher;
import com.ice2974.carpeticeaddition.command.MachineStatusRollbackWarningHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMachineStatusRollbackWarningMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerPlayNetworkHandlerMachineStatusRollbackWarningMixin.class);

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onCommandExecution", at = @At("HEAD"))
    private void carpetIceAddition$warnOnUnsignedRollbackCommandPacket(CommandExecutionC2SPacket packet, CallbackInfo ci) {
        this.carpetIceAddition$scheduleRollbackWarning(packet.command());
    }

    @Inject(method = "onChatCommandSigned", at = @At("HEAD"))
    private void carpetIceAddition$warnOnSignedRollbackCommandPacket(ChatCommandSignedC2SPacket packet, CallbackInfo ci) {
        this.carpetIceAddition$scheduleRollbackWarning(packet.command());
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void carpetIceAddition$warnOnRollbackChatPacket(ChatMessageC2SPacket packet, CallbackInfo ci) {
        this.carpetIceAddition$scheduleRollbackWarning(packet.chatMessage());
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
