package com.ice2974.carpeticeaddition.rules;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.util.RuleTextFormatUtil;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class BotTabListNameHelper {
    private BotTabListNameHelper() {
    }

    public static Text buildDisplayName(ServerPlayerEntity player, @Nullable Text currentDisplayName) {
        MutableText result = Text.empty();

        if (!isNone(CarpetIceAdditionSettings.botTabListNamePrefix)) {
            result.append(Text.literal(RuleTextFormatUtil.formatAmpersandCodes(CarpetIceAdditionSettings.botTabListNamePrefix)));
        }

        Text baseName = currentDisplayName != null ? currentDisplayName.copy() : player.getDisplayName().copy();
        result.append(baseName);

        if (!isNone(CarpetIceAdditionSettings.botTabListNameSuffix)) {
            result.append(Text.literal(RuleTextFormatUtil.formatAmpersandCodes(CarpetIceAdditionSettings.botTabListNameSuffix)));
        }

        return result;
    }

    public static boolean shouldApply() {
        return !isNone(CarpetIceAdditionSettings.botTabListNamePrefix)
                || !isNone(CarpetIceAdditionSettings.botTabListNameSuffix);
    }

    public static void refreshFakePlayerDisplayNames() {
        MinecraftServer server = CarpetServer.minecraft_server;
        if (server == null) {
            return;
        }

        PlayerManager playerManager = server.getPlayerManager();
        List<ServerPlayerEntity> fakePlayers = playerManager.getPlayerList()
                .stream()
                .filter(RealPlayerHelper::isFakePlayer)
                .toList();
        if (fakePlayers.isEmpty()) {
            return;
        }

        playerManager.sendToAll(new PlayerListS2CPacket(
                EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME),
                fakePlayers
        ));
    }

    private static boolean isNone(@Nullable String value) {
        return "#none".equals(value);
    }
}
