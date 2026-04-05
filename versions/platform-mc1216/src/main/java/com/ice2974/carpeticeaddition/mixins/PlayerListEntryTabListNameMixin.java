package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.BotTabListNameHelper;
import com.ice2974.carpeticeaddition.rules.RealPlayerHelper;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerListS2CPacket.Entry.class)
public abstract class PlayerListEntryTabListNameMixin {
    @Mutable
    @Shadow
    @Final
    private Text displayName;

    @Shadow
    @Final
    private UUID profileId;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void carpetIceAddition$modifyBotTabListName(CallbackInfo ci) {
        if (!BotTabListNameHelper.shouldApply()) {
            return;
        }

        try {
            MinecraftServer server = CarpetServer.minecraft_server;
            if (server == null) {
                return;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(this.profileId);
            if (player == null || !RealPlayerHelper.isFakePlayer(player)) {
                return;
            }

            this.displayName = BotTabListNameHelper.buildDisplayName(player, this.displayName);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("botTabListName", throwable);
        }
    }
}
