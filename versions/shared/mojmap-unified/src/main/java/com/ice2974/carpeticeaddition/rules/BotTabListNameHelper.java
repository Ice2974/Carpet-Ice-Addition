package com.ice2974.carpeticeaddition.rules;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.util.RuleTextFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class BotTabListNameHelper {
    private BotTabListNameHelper() {
    }

    public static Component buildDisplayName(ServerPlayer player, @Nullable Component currentDisplayName) {
        MutableComponent result = Component.empty();

        if (!isNone(CarpetIceAdditionSettings.botTabListNamePrefix)) {
            result.append(Component.literal(RuleTextFormatUtil.formatAmpersandCodes(CarpetIceAdditionSettings.botTabListNamePrefix)));
        }

        Component baseName = currentDisplayName != null ? currentDisplayName.copy() : player.getDisplayName().copy();
        result.append(baseName);

        if (!isNone(CarpetIceAdditionSettings.botTabListNameSuffix)) {
            result.append(Component.literal(RuleTextFormatUtil.formatAmpersandCodes(CarpetIceAdditionSettings.botTabListNameSuffix)));
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

        PlayerList playerManager = server.getPlayerList();
        List<ServerPlayer> fakePlayers = playerManager.getPlayers()
                .stream()
                .filter(RealPlayerHelper::isFakePlayer)
                .toList();
        if (fakePlayers.isEmpty()) {
            return;
        }

        playerManager.broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                fakePlayers
        ));
    }

    private static boolean isNone(@Nullable String value) {
        return "#none".equals(value);
    }
}
