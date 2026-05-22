package com.ice2974.carpeticeaddition.command;

import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.command.CommandStringParsingUtil.ParsedToken;
import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager.MachineRecord;
import com.ice2974.carpeticeaddition.command.MachineStatusStateUtil.ParsedState;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.mojang.brigadier.StringReader;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class MachineStatusCommand {
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

    private MachineStatusCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("machineStatus")
                .requires(MachineStatusCommand::canUseMachineStatus)
                .then(literal("add")
                        .then(argument("dimension", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                        context.getSource().getServer().getWorldRegistryKeys().stream().map(RegistryKey::getValue),
                                        builder
                                ))
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .suggests(MachineStatusCommand::suggestUnusedMachineNames)
                                                .executes(context -> addMachine(
                                                        context,
                                                        IdentifierArgumentType.getIdentifier(context, "dimension"),
                                                        BlockPosArgumentType.getBlockPos(context, "pos"),
                                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                                ))))))
                .then(literal("remove")
                        .then(argument("name", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommand::suggestMachineNames)
                                .executes(context -> removeMachine(
                                        context,
                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                ))))
                .then(literal("rename")
                        .then(argument("arguments", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommand::suggestMachineNames)
                                .executes(context -> {
                                    ParsedRenameArguments arguments = parseRenameArguments(StringArgumentType.getString(context, "arguments"));
                                    return renameMachine(context, arguments.name(), arguments.newName());
                                })))
                .then(literal("update")
                        .then(argument("name", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommand::suggestMachineNames)
                                .executes(context -> updateMachine(
                                        context,
                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                ))))
                .then(literal("move")
                        .then(argument("arguments", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommand::suggestMachineNames)
                                .executes(context -> {
                                    ParsedMoveArguments arguments = parseMoveArguments(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "arguments")
                                    );
                                    return moveMachine(context, arguments.name(), arguments.dimensionId(), arguments.pos());
                                })))
                .then(literal("list")
                        .executes(context -> listMachines(context, null))
                        .then(literal("running").executes(context -> listMachines(context, MachineStatusKind.RUNNING)))
                        .then(literal("stopped").executes(context -> listMachines(context, MachineStatusKind.STOPPED)))
                        .then(literal("invalid").executes(context -> listMachines(context, MachineStatusKind.INVALID)))
                        .then(literal("unloaded").executes(context -> listMachines(context, MachineStatusKind.UNLOADED))))
                .then(literal("info")
                        .then(argument("name", StringArgumentType.greedyString())
                                .suggests(MachineStatusCommand::suggestMachineNames)
                                .executes(context -> showInfo(
                                        context,
                                        parseSingleMachineName(StringArgumentType.getString(context, "name"))
                                )))));
    }

    private static int addMachine(
            CommandContext<ServerCommandSource> context,
            Identifier dimensionId,
            BlockPos pos,
            String name
    ) throws CommandSyntaxException {
        ensureMachineDoesNotExist(name);
        ServerWorld world = getWorld(context.getSource().getServer(), dimensionId);
        ensureChunkLoaded(world, pos, dimensionId);
        String shutdownState = serializeBlockState(world.getBlockState(pos));

        try {
            MachineStatusConfigManager.addMachine(name, dimensionId.toString(), pos.getX(), pos.getY(), pos.getZ(), shutdownState);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendFeedback(
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

    private static int removeMachine(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        ensureMachineExists(name);
        try {
            MachineStatusConfigManager.removeMachine(name);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendFeedback(
                () -> tr("command.carpet-ice-addition.machine_status.result.removed", name),
                false
        );
        return 1;
    }

    private static int renameMachine(CommandContext<ServerCommandSource> context, String name, String newName) throws CommandSyntaxException {
        ensureMachineExists(name);
        ensureMachineDoesNotExist(newName);

        try {
            MachineStatusConfigManager.renameMachine(name, newName);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendFeedback(
                () -> tr(
                        "command.carpet-ice-addition.machine_status.result.renamed",
                        name,
                        newName
                ),
                false
        );
        return 1;
    }

    private static int updateMachine(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        MachineRecord record = getMachineOrThrow(name);
        Identifier dimensionId = parseIdentifier(record.dimension());
        ServerWorld world = getWorld(context.getSource().getServer(), dimensionId);
        BlockPos pos = new BlockPos(record.x(), record.y(), record.z());
        ensureChunkLoaded(world, pos, dimensionId);

        String oldState = record.shutdownBlockState();
        String newState = serializeBlockState(world.getBlockState(pos));
        try {
            MachineStatusConfigManager.updateMachineState(name, newState);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendFeedback(
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
            CommandContext<ServerCommandSource> context,
            String name,
            Identifier dimensionId,
            BlockPos pos
    ) throws CommandSyntaxException {
        getMachineOrThrow(name);
        ServerWorld world = getWorld(context.getSource().getServer(), dimensionId);
        ensureChunkLoaded(world, pos, dimensionId);
        String shutdownState = serializeBlockState(world.getBlockState(pos));

        try {
            MachineStatusConfigManager.moveMachine(name, dimensionId.toString(), pos.getX(), pos.getY(), pos.getZ(), shutdownState);
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }

        context.getSource().sendFeedback(
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

    private static int listMachines(CommandContext<ServerCommandSource> context, MachineStatusKind filter) {
        List<MachineWithStatus> machines = collectMachines(context.getSource().getServer(), filter);

        context.getSource().sendFeedback(() -> listHeaderLine(filter), false);
        if (filter == null) {
            context.getSource().sendFeedback(() -> summaryLine(machines), false);
        } else if (!machines.isEmpty()) {
            context.getSource().sendFeedback(() -> countLine(machines.size()), false);
        }

        if (machines.isEmpty()) {
            context.getSource().sendFeedback(
                    () -> emptyListLine(filter),
                    false
            );
            return 0;
        }

        for (MachineWithStatus machine : machines) {
            context.getSource().sendFeedback(() -> formatMachineLine(machine.record, machine.status.kind()), false);
        }
        return machines.size();
    }

    private static int showInfo(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        MachineRecord record = getMachineOrThrow(name);
        MachineRuntimeStatus status = evaluateStatus(context.getSource().getServer(), record);
        MachineStatusStateUtil.ParsedState savedState = MachineStatusStateUtil.parse(record.shutdownBlockState());
        MachineStatusStateUtil.ParsedState currentState = status.currentStateRaw() == null
                ? null
                : MachineStatusStateUtil.parse(status.currentStateRaw());
        String position = formatPos(new BlockPos(record.x(), record.y(), record.z()));

        context.getSource().sendFeedback(MachineStatusCommand::detailHeaderLine, false);
        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.machine", white(record.name())), false);
        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.status", statusTag(status.kind())), false);
        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.dimension", white(record.dimension())), false);
        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.position", white(position)), false);

        switch (status.kind()) {
            case RUNNING, STOPPED -> {
                context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.block", white(blockId(savedState, record.shutdownBlockState()))), false);
                context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.saved_state", white(propertiesText(savedState, record.shutdownBlockState()))), false);
                context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.current_state", white(propertiesText(currentState, status.currentStateRaw()))), false);
            }
            case UNLOADED -> {
                context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.saved_block", white(blockId(savedState, record.shutdownBlockState()))), false);
                context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.saved_state", white(propertiesText(savedState, record.shutdownBlockState()))), false);
                context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.current_state", white(trString("command.carpet-ice-addition.machine_status.current_state.chunk_not_loaded"))), false);
            }
            case INVALID -> {
                switch (status.reason()) {
                    case DIMENSION_UNAVAILABLE -> {
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.current_state", white(trString("command.carpet-ice-addition.machine_status.current_state.dimension_not_found"))), false);
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.reason", white(trString("command.carpet-ice-addition.machine_status.info.reason.dimension_unavailable"))), false);
                    }
                    case TARGET_BLOCK_MISSING, BLOCK_TYPE_CHANGED -> {
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.saved_block", white(blockId(savedState, record.shutdownBlockState()))), false);
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.current_block", white(blockId(currentState, status.currentStateRaw()))), false);
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.reason", white(trString(status.reason().translationKey()))), false);
                    }
                    case INVALID_STATE -> {
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.saved_block", white(blockId(savedState, record.shutdownBlockState()))), false);
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.saved_state", white(propertiesText(savedState, record.shutdownBlockState()))), false);
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.current_state", white(status.currentStateText().getString())), false);
                        context.getSource().sendFeedback(() -> fieldLine("command.carpet-ice-addition.machine_status.info.label.reason", white(trString("command.carpet-ice-addition.machine_status.info.reason.invalid_state"))), false);
                    }
                    case NONE -> {
                    }
                }
            }
        }
        return 1;
    }

    public static List<MachineRecord> getMachineRecordsByStatus(MinecraftServer server, MachineStatusKind filter) {
        return collectMachines(server, filter).stream()
                .map(MachineWithStatus::record)
                .toList();
    }

    public static Text formatMachineStatusLine(MachineRecord record, MachineStatusKind kind) {
        return formatMachineLine(record, kind);
    }

    private static MachineRuntimeStatus evaluateStatus(MinecraftServer server, MachineRecord record) {
        Identifier dimensionId = Identifier.tryParse(MachineStatusStateUtil.normalizeIdentifier(record.dimension()));
        if (dimensionId == null) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.INVALID,
                    tr("command.carpet-ice-addition.machine_status.current_state.dimension_not_found"),
                    null,
                    MachineStatusIssueReason.DIMENSION_UNAVAILABLE
            );
        }

        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (world == null) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.INVALID,
                    tr("command.carpet-ice-addition.machine_status.current_state.dimension_not_found"),
                    null,
                    MachineStatusIssueReason.DIMENSION_UNAVAILABLE
            );
        }

        BlockPos pos = new BlockPos(record.x(), record.y(), record.z());
        if (!isChunkLoaded(world, pos)) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.UNLOADED,
                    tr("command.carpet-ice-addition.machine_status.current_state.chunk_not_loaded"),
                    null,
                    MachineStatusIssueReason.NONE
            );
        }

        String currentStateString = serializeBlockState(world.getBlockState(pos));
        ParsedState savedState = MachineStatusStateUtil.parse(record.shutdownBlockState());
        ParsedState currentState = MachineStatusStateUtil.parse(currentStateString);
        if (savedState == null || currentState == null) {
            return new MachineRuntimeStatus(MachineStatusKind.INVALID, Text.literal(currentStateString), currentStateString, MachineStatusIssueReason.INVALID_STATE);
        }

        if (!savedState.blockId().equals(currentState.blockId())) {
            MachineStatusIssueReason reason = currentState.blockId().equals("minecraft:air") && !savedState.blockId().equals("minecraft:air")
                    ? MachineStatusIssueReason.TARGET_BLOCK_MISSING
                    : MachineStatusIssueReason.BLOCK_TYPE_CHANGED;
            return new MachineRuntimeStatus(MachineStatusKind.INVALID, Text.literal(currentStateString), currentStateString, reason);
        }

        if (savedState.properties().equals(currentState.properties())) {
            return new MachineRuntimeStatus(MachineStatusKind.STOPPED, Text.literal(currentStateString), currentStateString, MachineStatusIssueReason.NONE);
        }
        return new MachineRuntimeStatus(MachineStatusKind.RUNNING, Text.literal(currentStateString), currentStateString, MachineStatusIssueReason.NONE);
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

    private static ServerWorld getWorld(MinecraftServer server, Identifier dimensionId) throws CommandSyntaxException {
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (world == null) {
            throw DIMENSION_NOT_FOUND.create(dimensionId);
        }
        return world;
    }

    private static void ensureChunkLoaded(ServerWorld world, BlockPos pos, Identifier dimensionId) throws CommandSyntaxException {
        if (!isChunkLoaded(world, pos)) {
            throw CHUNK_NOT_LOADED.create(formatChunkTarget(dimensionId, pos));
        }
    }

    private static boolean isChunkLoaded(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
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

    private static ParsedMoveArguments parseMoveArguments(ServerCommandSource source, String rawArguments) throws CommandSyntaxException {
        ParsedToken nameToken = requireToken(rawArguments, 0);
        ParsedToken dimensionToken = requireToken(rawArguments, nameToken.nextIndex());
        int positionStart = CommandStringParsingUtil.skipWhitespace(rawArguments, dimensionToken.nextIndex());
        if (positionStart >= rawArguments.length()) {
            throw INVALID_ARGUMENTS.create(rawArguments);
        }

        String positionArgument = rawArguments.substring(positionStart);
        StringReader reader = new StringReader(positionArgument);
        PosArgument posArgument = BlockPosArgumentType.blockPos().parse(reader);
        String trailing = positionArgument.substring(reader.getCursor()).trim();
        if (!trailing.isEmpty()) {
            throw INVALID_ARGUMENTS.create(rawArguments);
        }

        return new ParsedMoveArguments(
                validateMachineName(nameToken.value()),
                parseIdentifier(dimensionToken.value()),
                posArgument.toAbsoluteBlockPos(source)
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

    private static CompletableFuture<Suggestions> suggestUnusedMachineNames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
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

    private static CompletableFuture<Suggestions> suggestMachineNames(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
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

    private static Text listHeaderLine(MachineStatusKind filter) {
        return titleLine(filter == null
                ? "command.carpet-ice-addition.machine_status.title.list"
                : switch (filter) {
                    case RUNNING -> "command.carpet-ice-addition.machine_status.title.list.running";
                    case INVALID -> "command.carpet-ice-addition.machine_status.title.list.invalid";
                    case STOPPED -> "command.carpet-ice-addition.machine_status.title.list.stopped";
                    case UNLOADED -> "command.carpet-ice-addition.machine_status.title.list.unloaded";
                });
    }

    private static Text detailHeaderLine() {
        return titleLine("command.carpet-ice-addition.machine_status.title.info");
    }

    private static Text titleLine(String key) {
        String title = trString(key);
        int titleVisualWidth = visualWidth(title);
        int totalWidth = Math.max(38, titleVisualWidth + 12);
        int leftWidth = Math.max(4, (totalWidth - titleVisualWidth - 2) / 2);
        int rightWidth = Math.max(4, totalWidth - titleVisualWidth - 2 - leftWidth);

        MutableText line = Text.literal(repeat('=', leftWidth) + " ").formatted(Formatting.GOLD);
        line.append(Text.literal(title).formatted(Formatting.YELLOW));
        line.append(Text.literal(" " + repeat('=', rightWidth)).formatted(Formatting.GOLD));
        return line;
    }

    private static String serializeBlockState(BlockState state) {
        String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
        String properties = state.getEntries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Property::getName)))
                .map(entry -> entry.getKey().getName() + "=" + propertyValueName(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(","));
        return properties.isEmpty() ? blockId : blockId + "[" + properties + "]";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValueName(Property property, Comparable value) {
        return property.name(value);
    }

    private static Text formatMachineLine(MachineRecord record, MachineStatusKind kind) {
        MutableText line = Text.empty();
        line.append(statusTag(kind));
        line.append(Text.literal(" ").formatted(Formatting.GRAY));
        line.append(Text.literal(record.name()).formatted(Formatting.WHITE));
        line.append(Text.literal(" - " + record.dimension() + " " + record.x() + " " + record.y() + " " + record.z() + " ").formatted(Formatting.GRAY));
        line.append(infoButton(record.name()));
        return line;
    }

    private static Text statusTag(MachineStatusKind kind) {
        MutableText tag = Text.literal("[");
        tag.append(tr(kind.translationKey()));
        tag.append(Text.literal("]"));
        return tag.formatted(statusFormatting(kind));
    }

    private static Text infoButton(String name) {
        Style style = Style.EMPTY
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/machineStatus info " + quoteMachineName(name)))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        tr("command.carpet-ice-addition.machine_status.info.hover")
                ));
        return Text.literal("[i]").setStyle(style);
    }

    private static Text summaryLine(List<MachineWithStatus> machines) {
        long running = machines.stream().filter(machine -> machine.status.kind() == MachineStatusKind.RUNNING).count();
        long invalid = machines.stream().filter(machine -> machine.status.kind() == MachineStatusKind.INVALID).count();
        long stopped = machines.stream().filter(machine -> machine.status.kind() == MachineStatusKind.STOPPED).count();
        long unloaded = machines.stream().filter(machine -> machine.status.kind() == MachineStatusKind.UNLOADED).count();

        MutableText line = Text.empty();
        appendSummarySegment(line, "command.carpet-ice-addition.machine_status.summary.total", machines.size(), false);
        appendSummarySegment(line, "command.carpet-ice-addition.machine_status.summary.running", running, true);
        appendSummarySegment(line, "command.carpet-ice-addition.machine_status.summary.invalid", invalid, true);
        appendSummarySegment(line, "command.carpet-ice-addition.machine_status.summary.stopped", stopped, true);
        appendSummarySegment(line, "command.carpet-ice-addition.machine_status.summary.unloaded", unloaded, true);
        return line;
    }

    private static void appendSummarySegment(MutableText line, String labelKey, long value, boolean withSeparator) {
        if (withSeparator) {
            line.append(Text.literal(" | ").formatted(Formatting.DARK_GRAY));
        }
        line.append(Text.literal(trString(labelKey)).formatted(Formatting.GRAY));
        line.append(Text.literal(Long.toString(value)).formatted(Formatting.WHITE));
    }

    private static Text countLine(int count) {
        return Text.literal(trString("command.carpet-ice-addition.machine_status.list.count", count)).formatted(Formatting.GRAY);
    }

    private static Text emptyListLine(MachineStatusKind filter) {
        String key = filter == null
                ? "command.carpet-ice-addition.machine_status.list.empty"
                : switch (filter) {
                    case RUNNING -> "command.carpet-ice-addition.machine_status.list.empty.running";
                    case INVALID -> "command.carpet-ice-addition.machine_status.list.empty.invalid";
                    case STOPPED -> "command.carpet-ice-addition.machine_status.list.empty.stopped";
                    case UNLOADED -> "command.carpet-ice-addition.machine_status.list.empty.unloaded";
                };
        return Text.literal(trString(key)).formatted(Formatting.GRAY);
    }

    private static Text fieldLine(String labelKey, Text value) {
        MutableText line = Text.literal(trString(labelKey)).formatted(Formatting.GRAY);
        line.append(value);
        return line;
    }

    private static Text white(String value) {
        return Text.literal(value).formatted(Formatting.WHITE);
    }

    private static String blockId(MachineStatusStateUtil.ParsedState state, String rawState) {
        if (state != null) {
            return state.blockId();
        }
        if (rawState == null) {
            return trString("command.carpet-ice-addition.machine_status.state_properties.none");
        }
        int bracketIndex = rawState.indexOf('[');
        return bracketIndex >= 0 ? rawState.substring(0, bracketIndex).trim() : rawState.trim();
    }

    private static String propertiesText(MachineStatusStateUtil.ParsedState state, String rawState) {
        if (state != null) {
            if (state.properties().isEmpty()) {
                return trString("command.carpet-ice-addition.machine_status.state_properties.none");
            }
            return state.properties().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "));
        }
        if (rawState == null || rawState.isBlank()) {
            return trString("command.carpet-ice-addition.machine_status.state_properties.none");
        }
        int start = rawState.indexOf('[');
        int end = rawState.lastIndexOf(']');
        if (start >= 0 && end > start) {
            String properties = rawState.substring(start + 1, end).trim();
            return properties.isEmpty()
                    ? trString("command.carpet-ice-addition.machine_status.state_properties.none")
                    : properties.replace(",", ", ");
        }
        return trString("command.carpet-ice-addition.machine_status.state_properties.none");
    }

    private static MutableText tr(String key, Object... args) {
        return Text.literal(TranslationFormatUtil.translate(key, args));
    }

    private static String trString(String key, Object... args) {
        return TranslationFormatUtil.translate(key, args);
    }

    private static Formatting statusFormatting(MachineStatusKind kind) {
        return switch (kind) {
            case INVALID -> Formatting.RED;
            case RUNNING -> Formatting.YELLOW;
            case STOPPED -> Formatting.GREEN;
            case UNLOADED -> Formatting.GRAY;
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

    private static boolean canUseMachineStatus(ServerCommandSource source) {
        return CommandHelper.canUseCommand(source, CarpetIceAdditionSettings.commandMachineStatus);
    }

    private enum MachineStatusIssueReason {
        NONE(null),
        DIMENSION_UNAVAILABLE("command.carpet-ice-addition.machine_status.info.reason.dimension_unavailable"),
        BLOCK_TYPE_CHANGED("command.carpet-ice-addition.machine_status.info.reason.block_type_changed"),
        TARGET_BLOCK_MISSING("command.carpet-ice-addition.machine_status.info.reason.block_missing"),
        INVALID_STATE("command.carpet-ice-addition.machine_status.info.reason.invalid_state");

        private final String translationKey;

        MachineStatusIssueReason(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    private record MachineRuntimeStatus(MachineStatusKind kind, Text currentStateText, String currentStateRaw, MachineStatusIssueReason reason) {
    }

    private record MachineWithStatus(MachineRecord record, MachineRuntimeStatus status) {
    }

    private record ParsedRenameArguments(String name, String newName) {
    }

    private record ParsedMoveArguments(String name, Identifier dimensionId, BlockPos pos) {
    }
}
