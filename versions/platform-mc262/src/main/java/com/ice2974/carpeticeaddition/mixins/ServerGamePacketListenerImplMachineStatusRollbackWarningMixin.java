package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.command.MachineStatusRollbackCommandMatcher;
import com.ice2974.carpeticeaddition.command.MachineStatusRollbackWarningHandlerMc262;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMachineStatusRollbackWarningMixin {
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

        var server = this.player.level().getServer();
        if (server != null) {
            server.execute(() -> MachineStatusRollbackWarningHandlerMc262.warnIfNeeded(server, this.player, rawInput));
        }
    }
}
