package com.ice2974.carpeticeaddition.command;

import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager.MachineRecord;
import com.ice2974.carpeticeaddition.command.MachineStatusStateUtil.ParsedState;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
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
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
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
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

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
    private static final String MACHINE_NAME_ARGUMENT = "machine_name";
    private static final String HEADER_TEXT = "========== Machine Status ==========";
    private static final DynamicCommandExceptionType INVALID_IDENTIFIER = new DynamicCommandExceptionType(
            value -> Text.translatable("command.carpet-ice-addition.machine_status.error.invalid_identifier", value)
    );
    private static final DynamicCommandExceptionType MACHINE_EXISTS = new DynamicCommandExceptionType(
            value -> Text.translatable("command.carpet-ice-addition.machine_status.error.name_exists", value)
    );
    private static final DynamicCommandExceptionType MACHINE_NOT_FOUND = new DynamicCommandExceptionType(
            value -> Text.translatable("command.carpet-ice-addition.machine_status.error.name_not_found", value)
    );
    private static final DynamicCommandExceptionType DIMENSION_NOT_FOUND = new DynamicCommandExceptionType(
            value -> Text.translatable("command.carpet-ice-addition.machine_status.error.dimension_not_found", value)
    );
    private static final DynamicCommandExceptionType CHUNK_NOT_LOADED = new DynamicCommandExceptionType(
            value -> Text.translatable("command.carpet-ice-addition.machine_status.error.chunk_not_loaded", value)
    );
    private static final SimpleCommandExceptionType CONFIG_SAVE_FAILED = new SimpleCommandExceptionType(
            Text.translatable("command.carpet-ice-addition.machine_status.error.config_save_failed")
    );

    private MachineStatusCommand() {
    }

    public static void registerArgumentType() {
        ArgumentTypeRegistry.registerArgumentType(
                Identifier.of("carpet-ice-addition", "machine_status_single_token"),
                SingleTokenArgumentType.class,
                ConstantArgumentSerializer.of(SingleTokenArgumentType::singleToken)
        );
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
                                        .then(argument("name", SingleTokenArgumentType.singleToken())
                                                .suggests(MachineStatusCommand::suggestUnusedMachineNames)
                                                .executes(context -> addMachine(
                                                        context,
                                                        IdentifierArgumentType.getIdentifier(context, "dimension"),
                                                        BlockPosArgumentType.getBlockPos(context, "pos"),
                                                        getMachineName(context, "name")
                                                ))))))
                .then(literal("remove")
                        .then(argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> CommandSource.suggestMatching(machineNames(), builder))
                                .executes(context -> removeMachine(context, getMachineName(context, "name")))))
                .then(literal("rename")
                        .then(argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> CommandSource.suggestMatching(machineNames(), builder))
                                .then(argument("newName", SingleTokenArgumentType.singleToken())
                                        .suggests(MachineStatusCommand::suggestUnusedMachineNames)
                                        .executes(context -> renameMachine(
                                                context,
                                                getMachineName(context, "name"),
                                                getMachineName(context, "newName")
                                        )))))
                .then(literal("update")
                        .then(argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> CommandSource.suggestMatching(machineNames(), builder))
                                .executes(context -> updateMachine(context, getMachineName(context, "name")))))
                .then(literal("move")
                        .then(argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> CommandSource.suggestMatching(machineNames(), builder))
                                .then(argument("dimension", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                                context.getSource().getServer().getWorldRegistryKeys().stream().map(RegistryKey::getValue),
                                                builder
                                        ))
                                        .then(argument("pos", BlockPosArgumentType.blockPos())
                                                .executes(context -> moveMachine(
                                                        context,
                                                        getMachineName(context, "name"),
                                                        IdentifierArgumentType.getIdentifier(context, "dimension"),
                                                        BlockPosArgumentType.getBlockPos(context, "pos")
                                                ))))))
                .then(literal("list")
                        .executes(context -> listMachines(context, null))
                        .then(literal("running").executes(context -> listMachines(context, MachineStatusKind.RUNNING)))
                        .then(literal("stopped").executes(context -> listMachines(context, MachineStatusKind.STOPPED)))
                        .then(literal("invalid").executes(context -> listMachines(context, MachineStatusKind.INVALID)))
                        .then(literal("unloaded").executes(context -> listMachines(context, MachineStatusKind.UNLOADED))))
                .then(literal("info")
                        .then(argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> CommandSource.suggestMatching(machineNames(), builder))
                                .executes(context -> showInfo(context, getMachineName(context, "name"))))));
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
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.result.added",
                        Text.literal(name),
                        Text.literal(dimensionId.toString()),
                        Text.literal(formatPos(pos)),
                        Text.literal(shutdownState)
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
                () -> Text.translatable("command.carpet-ice-addition.machine_status.result.removed", Text.literal(name)),
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
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.result.renamed",
                        Text.literal(name),
                        Text.literal(newName)
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
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.result.updated",
                        Text.literal(name),
                        Text.literal(oldState),
                        Text.literal(newState)
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
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.result.moved",
                        Text.literal(name),
                        Text.literal(dimensionId.toString()),
                        Text.literal(formatPos(pos)),
                        Text.literal(shutdownState)
                ),
                false
        );
        return 1;
    }

    private static int listMachines(CommandContext<ServerCommandSource> context, MachineStatusKind filter) {
        List<MachineWithStatus> machines = MachineStatusConfigManager.snapshot().stream()
                .map(record -> new MachineWithStatus(record, evaluateStatus(context.getSource().getServer(), record)))
                .filter(machine -> filter == null || machine.status.kind() == filter)
                .sorted(Comparator
                        .comparingInt((MachineWithStatus machine) -> machine.status.kind().sortOrder())
                        .thenComparing(machine -> machine.record.name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(machine -> machine.record.name()))
                .toList();

        context.getSource().sendFeedback(MachineStatusCommand::headerLine, false);

        if (machines.isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.translatable("command.carpet-ice-addition.machine_status.list.empty"),
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

        context.getSource().sendFeedback(MachineStatusCommand::headerLine, false);

        context.getSource().sendFeedback(
                () -> Text.translatable("command.carpet-ice-addition.machine_status.info.machine", Text.literal(record.name())),
                false
        );
        context.getSource().sendFeedback(
                () -> Text.translatable("command.carpet-ice-addition.machine_status.info.dimension", Text.literal(record.dimension())),
                false
        );
        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.info.position",
                        Text.literal(Integer.toString(record.x())),
                        Text.literal(Integer.toString(record.y())),
                        Text.literal(Integer.toString(record.z()))
                ),
                false
        );
        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.info.saved_state",
                        Text.literal(record.shutdownBlockState())
                ),
                false
        );
        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.info.current_state",
                        status.currentStateText()
                ),
                false
        );
        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "command.carpet-ice-addition.machine_status.info.status",
                        Text.translatable(status.kind.translationKey())
                ),
                false
        );
        return 1;
    }

    private static MachineRuntimeStatus evaluateStatus(MinecraftServer server, MachineRecord record) {
        Identifier dimensionId = Identifier.tryParse(MachineStatusStateUtil.normalizeIdentifier(record.dimension()));
        if (dimensionId == null) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.INVALID,
                    Text.translatable("command.carpet-ice-addition.machine_status.current_state.dimension_not_found")
            );
        }

        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, dimensionId));
        if (world == null) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.INVALID,
                    Text.translatable("command.carpet-ice-addition.machine_status.current_state.dimension_not_found")
            );
        }

        BlockPos pos = new BlockPos(record.x(), record.y(), record.z());
        if (!isChunkLoaded(world, pos)) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.UNLOADED,
                    Text.translatable("command.carpet-ice-addition.machine_status.current_state.chunk_not_loaded")
            );
        }

        String currentStateString = serializeBlockState(world.getBlockState(pos));
        ParsedState savedState = MachineStatusStateUtil.parse(record.shutdownBlockState());
        ParsedState currentState = MachineStatusStateUtil.parse(currentStateString);
        if (savedState == null || currentState == null) {
            return new MachineRuntimeStatus(MachineStatusKind.INVALID, Text.literal(currentStateString));
        }

        if (!savedState.blockId().equals(currentState.blockId())) {
            return new MachineRuntimeStatus(MachineStatusKind.INVALID, Text.literal(currentStateString));
        }

        if (savedState.properties().equals(currentState.properties())) {
            return new MachineRuntimeStatus(MachineStatusKind.STOPPED, Text.literal(currentStateString));
        }
        return new MachineRuntimeStatus(MachineStatusKind.RUNNING, Text.literal(currentStateString));
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

    private static String getMachineName(CommandContext<ServerCommandSource> context, String argumentName) throws CommandSyntaxException {
        return validateMachineName(context.getArgument(argumentName, String.class));
    }

    private static String validateMachineName(String rawName) throws CommandSyntaxException {
        String normalized = rawName == null ? "" : rawName.trim();
        if (normalized.isEmpty() || containsWhitespace(normalized)) {
            throw INVALID_IDENTIFIER.create(rawName);
        }
        return normalized;
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
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

    private static Text headerLine() {
        return Text.literal(HEADER_TEXT).formatted(Formatting.GOLD);
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
        line.append(Text.literal(" "));
        line.append(Text.literal(record.name()));
        line.append(Text.literal(" - "));
        line.append(Text.literal(record.dimension()));
        line.append(Text.literal(" " + record.x() + " " + record.y() + " " + record.z() + " "));
        line.append(infoButton(record.name()));
        return line;
    }

    private static Text statusTag(MachineStatusKind kind) {
        MutableText tag = Text.literal("[");
        tag.append(Text.translatable(kind.translationKey()));
        tag.append(Text.literal("]"));
        return tag.formatted(statusFormatting(kind));
    }

    private static Text infoButton(String name) {
        Style style = Style.EMPTY
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/machineStatus info " + name))
                .withHoverEvent(new HoverEvent.ShowText(
                        Text.translatable("command.carpet-ice-addition.machine_status.info.hover")
                ));
        return Text.literal("[i]").setStyle(style);
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

    private static boolean canUseMachineStatus(ServerCommandSource source) {
        return CommandHelper.canUseCommand(source, CarpetIceAdditionSettings.commandMachineStatus);
    }

    private record MachineRuntimeStatus(MachineStatusKind kind, Text currentStateText) {
    }

    private record MachineWithStatus(MachineRecord record, MachineRuntimeStatus status) {
    }

    public static final class SingleTokenArgumentType implements ArgumentType<String> {
        private SingleTokenArgumentType() {
        }

        private static SingleTokenArgumentType singleToken() {
            return new SingleTokenArgumentType();
        }

        @Override
        public String parse(StringReader reader) {
            int start = reader.getCursor();
            while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
                reader.skip();
            }
            return reader.getString().substring(start, reader.getCursor());
        }
    }
}
