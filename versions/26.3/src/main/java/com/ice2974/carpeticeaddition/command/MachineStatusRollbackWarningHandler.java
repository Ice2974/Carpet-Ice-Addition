package com.ice2974.carpeticeaddition.command;

import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager.MachineRecord;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MachineStatusRollbackWarningHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineStatusRollbackWarningHandler.class);

    private MachineStatusRollbackWarningHandler() {
    }

    public static void warnIfNeeded(MinecraftServer server, ServerPlayer player, String rawInput) {
        if (!CarpetIceAdditionSettings.machineStatusRollbackWarning || player == null || server == null) {
            return;
        }
        if (!MachineStatusRollbackCommandMatcher.matches(rawInput)) {
            return;
        }

        List<MachineRecord> runningMachines = MachineStatusCommand.getMachineRecordsByStatus(server, MachineStatusKind.RUNNING);
        List<MachineRecord> invalidMachines = MachineStatusCommand.getMachineRecordsByStatus(server, MachineStatusKind.INVALID);
        if (runningMachines.isEmpty() && invalidMachines.isEmpty()) {
            LOGGER.debug("Matched rollback command '{}' from player '{}' but found no running or invalid machines.", rawInput, player.getName().getString());
            return;
        }

        player.sendSystemMessage(titleLine(), false);
        player.sendSystemMessage(commandLine(rawInput), false);
        player.sendSystemMessage(introLine(), false);
        for (MachineRecord record : runningMachines) {
            player.sendSystemMessage(formatMachineLine(record, MachineStatusKind.RUNNING), false);
        }
        for (MachineRecord record : invalidMachines) {
            player.sendSystemMessage(formatMachineLine(record, MachineStatusKind.INVALID), false);
        }
        player.sendSystemMessage(footerLine(), false);
        LOGGER.debug(
                "Sent rollback warning to player '{}' for {} running machines and {} invalid machines.",
                player.getName().getString(),
                runningMachines.size(),
                invalidMachines.size()
        );
    }

    private static Component titleLine() {
        String title = TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.title");
        int titleVisualWidth = visualWidth(title);
        int totalWidth = Math.max(42, titleVisualWidth + 12);
        int leftWidth = Math.max(4, (totalWidth - titleVisualWidth - 2) / 2);
        int rightWidth = Math.max(4, totalWidth - titleVisualWidth - 2 - leftWidth);

        MutableComponent line = Component.literal(repeat('=', leftWidth) + " ").withStyle(ChatFormatting.GOLD);
        line.append(Component.literal(title).withStyle(ChatFormatting.YELLOW));
        line.append(Component.literal(" " + repeat('=', rightWidth)).withStyle(ChatFormatting.GOLD));
        return line;
    }

    private static Component commandLine(String rawInput) {
        MutableComponent line = Component.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.detected_prefix"))
                .withStyle(ChatFormatting.GRAY);
        line.append(Component.literal(rawInput).withStyle(ChatFormatting.WHITE));
        return line;
    }

    private static Component introLine() {
        return Component.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.intro"))
                .withStyle(ChatFormatting.YELLOW);
    }

    private static Component footerLine() {
        return Component.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.footer"))
                .withStyle(ChatFormatting.YELLOW);
    }

    private static Component formatMachineLine(MachineRecord record, MachineStatusKind kind) {
        MutableComponent line = Component.empty();
        line.append(statusTag(kind));
        line.append(Component.literal(" ").withStyle(ChatFormatting.GRAY));
        line.append(Component.literal(record.name()).withStyle(ChatFormatting.WHITE));
        line.append(Component.literal(" - " + record.dimension() + " " + record.x() + " " + record.y() + " " + record.z() + " ")
                .withStyle(ChatFormatting.GRAY));
        line.append(infoButton(record.name()));
        return line;
    }

    private static Component statusTag(MachineStatusKind kind) {
        ChatFormatting formatting = kind == MachineStatusKind.INVALID ? ChatFormatting.RED : ChatFormatting.YELLOW;
        MutableComponent tag = Component.literal("[").withStyle(formatting);
        tag.append(Component.literal(TranslationFormatUtil.translate(kind.translationKey())).withStyle(formatting));
        tag.append(Component.literal("]").withStyle(formatting));
        return tag;
    }

    private static Component infoButton(String name) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/machineStatus info " + quoteMachineName(name)))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal(TranslationFormatUtil.translate("command.carpet-ice-addition.machine_status.info.hover"))
                ));
        return Component.literal("[i]").setStyle(style);
    }

    private static String quoteMachineName(String name) {
        if (name.chars().allMatch(character -> character < 128 && com.mojang.brigadier.StringReader.isAllowedInUnquotedString((char) character))) {
            return name;
        }
        return StringArgumentType.escapeIfRequired(name);
    }

    private static int visualWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += text.charAt(i) <= 0x7F ? 1 : 2;
        }
        return width;
    }

    private static String repeat(char ch, int count) {
        return String.valueOf(ch).repeat(Math.max(0, count));
    }
}
