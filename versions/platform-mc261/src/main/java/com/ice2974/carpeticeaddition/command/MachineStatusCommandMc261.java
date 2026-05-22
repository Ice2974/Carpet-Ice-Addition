package com.ice2974.carpeticeaddition.command;

import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.command.CommandStringParsingUtil.ParsedToken;
import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager.MachineRecord;
import com.ice2974.carpeticeaddition.command.MachineStatusStateUtil.ParsedState;
import com.mojang.brigadier.StringReader;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class MachineStatusCommandMc261 {
    private static final DynamicCommandExceptionType INVALID_IDENTIFIER = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.machine_status.error.invalid_identifier", value)
    );
    private static final DynamicCommandExceptionType MACHINE_EXISTS = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.machine_status.error.name_exists", value)
    );
    private static final DynamicCommandExceptionType MACHINE_NOT_FOUND = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.machine_status.error.name_not_found", value)
    );
    private static final DynamicCommandExceptionType DIMENSION_NOT_FOUND = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.machine_status.error.dimension_not_found", value)
    );
    private static final DynamicCommandExceptionType CHUNK_NOT_LOADED = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.machine_status.error.chunk_not_loaded", value)
    );
    private static final DynamicCommandExceptionType INVALID_ARGUMENTS = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.machine_status.error.invalid_arguments", value)
    );
    private static final SimpleCommandExceptionType CONFIG_SAVE_FAILED = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.machine_status.error.config_save_failed")
    );

    private MachineStatusCommandMc261() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("machineStatus")
                .requires(MachineStatusCommandMc261::canUseMachineStatus)
                .then(Commands.literal("add")
                        .then(Commands.argument("dimension", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().getServer().levelKeys().stream().map(ResourceKey::identifier),
                                        builder
                                ))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .suggests(MachineStatusCommandMc261::suggestUnusedMachineNames)
                                                .executes(context -> addMachine(
                                                        context,
                                                        parseDimensionIdentifier(context, "dimension"),
                                                        BlockPosArgument.getBlockPos(context, "pos"),
                                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                                ))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommandMc261::suggestMachineNames)
                                .executes(context -> removeMachine(
                                        context,
                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                ))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("arguments", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommandMc261::suggestMachineNames)
                                .executes(context -> {
                                    ParsedRenameArguments arguments = parseRenameArguments(StringArgumentType.getString(context, "arguments"));
                                    return renameMachine(context, arguments.name(), arguments.newName());
                                })))
                .then(Commands.literal("update")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommandMc261::suggestMachineNames)
                                .executes(context -> updateMachine(
                                        context,
                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                ))))
                .then(Commands.literal("move")
                        .then(Commands.argument("arguments", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommandMc261::suggestMachineNames)
                                .executes(context -> {
                                    ParsedMoveArguments arguments = parseMoveArguments(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "arguments")
                                    );
                                    return moveMachine(context, arguments.name(), arguments.dimensionId(), arguments.pos());
                                })))
                .then(Commands.literal("list")
                        .executes(context -> listMachines(context, null))
                        .then(Commands.literal("running").executes(context -> listMachines(context, MachineStatusKind.RUNNING)))
                        .then(Commands.literal("stopped").executes(context -> listMachines(context, MachineStatusKind.STOPPED)))
                        .then(Commands.literal("invalid").executes(context -> listMachines(context, MachineStatusKind.INVALID)))
                        .then(Commands.literal("unloaded").executes(context -> listMachines(context, MachineStatusKind.UNLOADED))))
                .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommandMc261::suggestMachineNames)
                                .executes(context -> showInfo(
                                        context,
                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                )))));
    }

    private static int addMachine(
            CommandContext<CommandSourceStack> context,
            Identifier dimensionId,
            BlockPos pos,
            String name
    ) throws CommandSyntaxException {
        ensureMachineDoesNotExist(name);
        ServerLevel world = getWorld(context.getSource().getServer(), dimensionId);
        ensureChunkLoaded(world, pos, dimensionId);
        String shutdownState = serializeBlockState(world.getBlockState(pos));

        try {
            MachineStatusConfigManager.addMachine(name, dimensionId.toString(), pos.getX(), pos.getY(), pos.getZ(), shutdownState);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.result.added",
                        name,
                        dimensionId.toString(),
                        formatPos(pos),
                        shutdownState
                ),
                false
        );
        return 1;
    }

    private static int removeMachine(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        ensureMachineExists(name);
        try {
            MachineStatusConfigManager.removeMachine(name);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendSuccess(
                () -> tr("command.carpet-ice-addition.machine_status.result.removed", name),
                false
        );
        return 1;
    }

    private static int renameMachine(CommandContext<CommandSourceStack> context, String name, String newName) throws CommandSyntaxException {
        ensureMachineExists(name);
        ensureMachineDoesNotExist(newName);

        try {
            MachineStatusConfigManager.renameMachine(name, newName);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.result.renamed",
                        name,
                        newName
                ),
                false
        );
        return 1;
    }

    private static int updateMachine(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        MachineRecord record = getMachineOrThrow(name);
        Identifier dimensionId = parseIdentifier(record.dimension());
        ServerLevel world = getWorld(context.getSource().getServer(), dimensionId);
        BlockPos pos = new BlockPos(record.x(), record.y(), record.z());
        ensureChunkLoaded(world, pos, dimensionId);

        String oldState = record.shutdownBlockState();
        String newState = serializeBlockState(world.getBlockState(pos));
        try {
            MachineStatusConfigManager.updateMachineState(name, newState);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.result.updated",
                        name,
                        oldState,
                        newState
                ),
                false
        );
        return 1;
    }

    private static int moveMachine(
            CommandContext<CommandSourceStack> context,
            String name,
            Identifier dimensionId,
            BlockPos pos
    ) throws CommandSyntaxException {
        getMachineOrThrow(name);
        ServerLevel world = getWorld(context.getSource().getServer(), dimensionId);
        ensureChunkLoaded(world, pos, dimensionId);
        String shutdownState = serializeBlockState(world.getBlockState(pos));

        try {
            MachineStatusConfigManager.moveMachine(name, dimensionId.toString(), pos.getX(), pos.getY(), pos.getZ(), shutdownState);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.result.moved",
                        name,
                        dimensionId.toString(),
                        formatPos(pos),
                        shutdownState
                ),
                false
        );
        return 1;
    }

    private static int listMachines(CommandContext<CommandSourceStack> context, MachineStatusKind filter) {
        List<MachineWithStatus> machines = collectMachines(context.getSource().getServer(), filter);

        context.getSource().sendSuccess(MachineStatusCommandMc261::headerLine, false);

        if (machines.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> tr("command.carpet-ice-addition.machine_status.list.empty"),
                    false
            );
            return 0;
        }

        for (MachineWithStatus machine : machines) {
            context.getSource().sendSuccess(() -> formatMachineLine(machine.record, machine.status.kind()), false);
        }
        return machines.size();
    }

    private static int showInfo(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        MachineRecord record = getMachineOrThrow(name);
        MachineRuntimeStatus status = evaluateStatus(context.getSource().getServer(), record);

        context.getSource().sendSuccess(MachineStatusCommandMc261::headerLine, false);

        context.getSource().sendSuccess(
                () -> tr("command.carpet-ice-addition.machine_status.info.machine", record.name()),
                false
        );
        context.getSource().sendSuccess(
                () -> tr("command.carpet-ice-addition.machine_status.info.dimension", record.dimension()),
                false
        );
        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.info.position",
                        Integer.toString(record.x()),
                        Integer.toString(record.y()),
                        Integer.toString(record.z())
                ),
                false
        );
        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.info.saved_state",
                        record.shutdownBlockState()
                ),
                false
        );
        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.info.current_state",
                        status.currentStateText().getString()
                ),
                false
        );
        context.getSource().sendSuccess(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.info.status",
                        trString(status.kind.translationKey())
                ),
                false
        );
        return 1;
    }

    public static List<MachineRecord> getMachineRecordsByStatus(MinecraftServer server, MachineStatusKind filter) {
        return collectMachines(server, filter).stream()
                .map(MachineWithStatus::record)
                .toList();
    }

    public static Component formatMachineStatusLine(MachineRecord record, MachineStatusKind kind) {
        return formatMachineLine(record, kind);
    }

    private static MachineRuntimeStatus evaluateStatus(MinecraftServer server, MachineRecord record) {
        Identifier dimensionId = Identifier.tryParse(MachineStatusStateUtil.normalizeIdentifier(record.dimension()));
        if (dimensionId == null) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.INVALID,
                    tr("command.carpet-ice-addition.machine_status.current_state.dimension_not_found")
            );
        }

        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (world == null) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.INVALID,
                    tr("command.carpet-ice-addition.machine_status.current_state.dimension_not_found")
            );
        }

        BlockPos pos = new BlockPos(record.x(), record.y(), record.z());
        if (!isChunkLoaded(world, pos)) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.UNLOADED,
                    tr("command.carpet-ice-addition.machine_status.current_state.chunk_not_loaded")
            );
        }

        String currentStateString = serializeBlockState(world.getBlockState(pos));
        ParsedState savedState = MachineStatusStateUtil.parse(record.shutdownBlockState());
        ParsedState currentState = MachineStatusStateUtil.parse(currentStateString);
        if (savedState == null || currentState == null) {
            return new MachineRuntimeStatus(MachineStatusKind.INVALID, Component.literal(currentStateString));
        }

        if (!savedState.blockId().equals(currentState.blockId())) {
            return new MachineRuntimeStatus(MachineStatusKind.INVALID, Component.literal(currentStateString));
        }

        if (savedState.properties().equals(currentState.properties())) {
            return new MachineRuntimeStatus(MachineStatusKind.STOPPED, Component.literal(currentStateString));
        }
        return new MachineRuntimeStatus(MachineStatusKind.RUNNING, Component.literal(currentStateString));
    }

    private static MachineRecord getMachineOrThrow(String name) throws CommandSyntaxException {
        MachineRecord record = MachineStatusConfigManager.getMachine(name);
        if (record == null) {
            throw MACHINE_NOT_FOUND.create(name);
        }
        return record;
    }

    private static void ensureMachineExists(String name) throws CommandSyntaxException {
        if (!MachineStatusConfigManager.containsMachine(name)) {
            throw MACHINE_NOT_FOUND.create(name);
        }
    }

    private static void ensureMachineDoesNotExist(String name) throws CommandSyntaxException {
        if (MachineStatusConfigManager.containsMachine(name)) {
            throw MACHINE_EXISTS.create(name);
        }
    }

    private static Identifier parseIdentifier(String rawIdentifier) throws CommandSyntaxException {
        Identifier identifier = Identifier.tryParse(MachineStatusStateUtil.normalizeIdentifier(rawIdentifier));
        if (identifier == null) {
            throw INVALID_IDENTIFIER.create(rawIdentifier);
        }
        return identifier;
    }

    private static Identifier parseDimensionIdentifier(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
        return parseIdentifier(context.getArgument(argumentName, String.class));
    }

    private static String validateMachineName(String rawName) throws CommandSyntaxException {
        String normalized = rawName == null ? "" : rawName.trim();
        if (normalized.isEmpty()) {
            throw INVALID_IDENTIFIER.create(rawName);
        }
        return normalized;
    }

    private static String parseSingleMachineName(String rawArgument) throws CommandSyntaxException {
        ParsedToken token = requireToken(rawArgument, 0);
        ensureNoTrailingContent(rawArgument, token.nextIndex());
        return validateMachineName(token.value());
    }

    private static ParsedRenameArguments parseRenameArguments(String rawArguments) throws CommandSyntaxException {
        ParsedToken nameToken = requireToken(rawArguments, 0);
        ParsedToken newNameToken = requireToken(rawArguments, nameToken.nextIndex());
        ensureNoTrailingContent(rawArguments, newNameToken.nextIndex());
        return new ParsedRenameArguments(
                validateMachineName(nameToken.value()),
                validateMachineName(newNameToken.value())
        );
    }

    private static ParsedMoveArguments parseMoveArguments(CommandSourceStack source, String rawArguments) throws CommandSyntaxException {
        ParsedToken nameToken = requireToken(rawArguments, 0);
        ParsedToken dimensionToken = requireToken(rawArguments, nameToken.nextIndex());
        int positionStart = CommandStringParsingUtil.skipWhitespace(rawArguments, dimensionToken.nextIndex());
        if (positionStart >= rawArguments.length()) {
            throw INVALID_ARGUMENTS.create(rawArguments);
        }

        String positionArgument = rawArguments.substring(positionStart);
        StringReader reader = new StringReader(positionArgument);
        Coordinates coordinates = BlockPosArgument.blockPos().parse(reader);
        String trailing = positionArgument.substring(reader.getCursor()).trim();
        if (!trailing.isEmpty()) {
            throw INVALID_ARGUMENTS.create(rawArguments);
        }

        return new ParsedMoveArguments(
                validateMachineName(nameToken.value()),
                parseIdentifier(dimensionToken.value()),
                coordinates.getBlockPos(source)
        );
    }

    private static ParsedToken requireToken(String rawArguments, int startIndex) throws CommandSyntaxException {
        ParsedToken token = CommandStringParsingUtil.parseNextToken(rawArguments, startIndex);
        if (token == null) {
            throw INVALID_ARGUMENTS.create(rawArguments);
        }
        return token;
    }

    private static void ensureNoTrailingContent(String rawArguments, int nextIndex) throws CommandSyntaxException {
        if (CommandStringParsingUtil.skipWhitespace(rawArguments, nextIndex) != rawArguments.length()) {
            throw INVALID_ARGUMENTS.create(rawArguments);
        }
    }

    private static ServerLevel getWorld(MinecraftServer server, Identifier dimensionId) throws CommandSyntaxException {
        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (world == null) {
            throw DIMENSION_NOT_FOUND.create(dimensionId);
        }
        return world;
    }

    private static void ensureChunkLoaded(ServerLevel world, BlockPos pos, Identifier dimensionId) throws CommandSyntaxException {
        if (!isChunkLoaded(world, pos)) {
            throw CHUNK_NOT_LOADED.create(formatChunkTarget(dimensionId, pos));
        }
    }

    private static boolean isChunkLoaded(ServerLevel world, BlockPos pos) {
        return world.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static CompletableFuture<Suggestions> suggestUnusedMachineNames(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.indexOf(' ') >= 0) {
            return builder.buildFuture();
        }

        String lower = remaining.toLowerCase(Locale.ROOT);
        for (String name : machineNames()) {
            if (!name.toLowerCase(Locale.ROOT).startsWith(lower)) {
                continue;
            }
            if (!name.equals(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMachineNames(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        String unquotedRemaining = remaining.startsWith("\"") ? remaining.substring(1) : remaining;
        for (String name : machineNames()) {
            String suggestion = quoteMachineName(name);
            String lowerSuggestion = suggestion.toLowerCase(Locale.ROOT);
            String lowerName = name.toLowerCase(Locale.ROOT);
            if (lowerSuggestion.startsWith(remaining) || lowerName.startsWith(unquotedRemaining)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    private static List<MachineWithStatus> collectMachines(MinecraftServer server, MachineStatusKind filter) {
        return MachineStatusConfigManager.snapshot().stream()
                .map(record -> new MachineWithStatus(record, evaluateStatus(server, record)))
                .filter(machine -> filter == null || machine.status.kind() == filter)
                .sorted(Comparator
                        .comparingInt((MachineWithStatus machine) -> machine.status.kind().sortOrder())
                        .thenComparing(machine -> machine.record.name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(machine -> machine.record.name()))
                .toList();
    }

    private static Component headerLine() {
        return tr("command.carpet-ice-addition.machine_status.header").withStyle(ChatFormatting.GOLD);
    }

    private static String serializeBlockState(BlockState state) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String properties = state.getValues()
                .sorted(Comparator.comparing(value -> value.property().getName()))
                .map(value -> value.property().getName() + "=" + value.valueName())
                .collect(Collectors.joining(","));
        return properties.isEmpty() ? blockId : blockId + "[" + properties + "]";
    }

    private static Component formatMachineLine(MachineRecord record, MachineStatusKind kind) {
        MutableComponent line = Component.empty();
        line.append(statusTag(kind));
        line.append(" ");
        line.append(Component.literal(record.name()));
        line.append(" - ");
        line.append(Component.literal(record.dimension()));
        line.append(" " + record.x() + " " + record.y() + " " + record.z() + " ");
        line.append(infoButton(record.name()));
        return line;
    }

    private static Component statusTag(MachineStatusKind kind) {
        MutableComponent tag = Component.literal("[");
        tag.append(tr(kind.translationKey()));
        tag.append("]");
        return tag.withStyle(statusFormatting(kind));
    }

    private static Component infoButton(String name) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/machineStatus info " + quoteMachineName(name)))
                .withHoverEvent(new HoverEvent.ShowText(
                        tr("command.carpet-ice-addition.machine_status.info.hover")
                ));
        return Component.literal("[i]").setStyle(style);
    }

    private static MutableComponent tr(String key, Object... args) {
        return Component.literal(TranslationFormatUtil.translate(key, args));
    }

    private static String trString(String key, Object... args) {
        return TranslationFormatUtil.translate(key, args);
    }

    private static ChatFormatting statusFormatting(MachineStatusKind kind) {
        return switch (kind) {
            case INVALID -> ChatFormatting.RED;
            case RUNNING -> ChatFormatting.YELLOW;
            case STOPPED -> ChatFormatting.GREEN;
            case UNLOADED -> ChatFormatting.GRAY;
        };
    }

    private static List<String> machineNames() {
        return MachineStatusConfigManager.snapshot().stream()
                .map(MachineRecord::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static String formatPos(BlockPos pos) {
        return String.format(Locale.ROOT, "%d %d %d", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String formatChunkTarget(Identifier dimensionId, BlockPos pos) {
        return dimensionId + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String quoteMachineName(String name) {
        if (name.indexOf(' ') < 0 && name.indexOf('"') < 0 && name.indexOf('\\') < 0) {
            return name;
        }
        return "\"" + name
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    private static boolean canUseMachineStatus(CommandSourceStack source) {
        return CommandHelper.canUseCommand(source, CarpetIceAdditionSettings.commandMachineStatus);
    }

    private record MachineRuntimeStatus(MachineStatusKind kind, Component currentStateText) {
    }

    private record MachineWithStatus(MachineRecord record, MachineRuntimeStatus status) {
    }

    private record ParsedRenameArguments(String name, String newName) {
    }

    private record ParsedMoveArguments(String name, Identifier dimensionId, BlockPos pos) {
    }
}
