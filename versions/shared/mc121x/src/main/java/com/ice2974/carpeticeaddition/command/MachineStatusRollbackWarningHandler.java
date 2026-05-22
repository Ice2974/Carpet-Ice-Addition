package com.ice2974.carpeticeaddition.command;

import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager.MachineRecord;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MachineStatusRollbackWarningHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineStatusRollbackWarningHandler.class);

    private MachineStatusRollbackWarningHandler() {
    }

    public static void warnIfNeeded(MinecraftServer server, ServerPlayerEntity player, String rawInput) {
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

        player.sendMessage(titleLine(), false);
        player.sendMessage(commandLine(rawInput), false);
        player.sendMessage(introLine(), false);
        for (MachineRecord record : runningMachines) {
            player.sendMessage(formatMachineLine(record, MachineStatusKind.RUNNING), false);
        }
        for (MachineRecord record : invalidMachines) {
            player.sendMessage(formatMachineLine(record, MachineStatusKind.INVALID), false);
        }
        player.sendMessage(footerLine(), false);
        LOGGER.debug(
                "Sent rollback warning to player '{}' for {} running machines and {} invalid machines.",
                player.getName().getString(),
                runningMachines.size(),
                invalidMachines.size()
        );
    }

    private static Text titleLine() {
        String title = TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.title");
        int titleVisualWidth = visualWidth(title);
        int totalWidth = Math.max(42, titleVisualWidth + 12);
        int leftWidth = Math.max(4, (totalWidth - titleVisualWidth - 2) / 2);
        int rightWidth = Math.max(4, totalWidth - titleVisualWidth - 2 - leftWidth);

        MutableText line = Text.literal(repeat('=', leftWidth) + " ").formatted(Formatting.GOLD);
        line.append(Text.literal(title).formatted(Formatting.YELLOW));
        line.append(Text.literal(" " + repeat('=', rightWidth)).formatted(Formatting.GOLD));
        return line;
    }

    private static Text commandLine(String rawInput) {
        MutableText line = Text.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.detected_prefix"))
                .formatted(Formatting.GRAY);
        line.append(Text.literal(rawInput).formatted(Formatting.WHITE));
        return line;
    }

    private static Text introLine() {
        return Text.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.intro"))
                .formatted(Formatting.YELLOW);
    }

    private static Text footerLine() {
        return Text.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.machine_status_rollback_warning.footer"))
                .formatted(Formatting.YELLOW);
    }

    private static Text formatMachineLine(MachineRecord record, MachineStatusKind kind) {
        MutableText line = Text.empty();
        line.append(statusTag(kind));
        line.append(Text.literal(" ").formatted(Formatting.GRAY));
        line.append(Text.literal(record.name()).formatted(Formatting.WHITE));
        line.append(Text.literal(" - " + record.dimension() + " " + record.x() + " " + record.y() + " " + record.z() + " ")
                .formatted(Formatting.GRAY));
        line.append(infoButton(record.name()));
        return line;
    }

    private static Text statusTag(MachineStatusKind kind) {
        Formatting formatting = kind == MachineStatusKind.INVALID ? Formatting.RED : Formatting.YELLOW;
        MutableText tag = Text.literal("[").formatted(formatting);
        tag.append(Text.literal(TranslationFormatUtil.translate(kind.translationKey())).formatted(formatting));
        tag.append(Text.literal("]").formatted(formatting));
        return tag;
    }

    private static Text infoButton(String name) {
        Style style = Style.EMPTY
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/machineStatus info " + quoteMachineName(name)))
                .withHoverEvent(new HoverEvent.ShowText(
                        Text.literal(TranslationFormatUtil.translate("command.carpet-ice-addition.machine_status.info.hover"))
                ));
        return Text.literal("[i]").setStyle(style);
    }

    private static String quoteMachineName(String name) {
        if (name.indexOf(' ') < 0 && name.indexOf('"') < 0 && name.indexOf('\\') < 0) {
            return name;
        }
        return "\"" + name
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
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
