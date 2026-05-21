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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
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
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class MachineStatusCommandMc261 {
    private static final String HEADER_TEXT = "========== Machine Status ==========";
    private static final DynamicCommandExceptionType INVALID_IDENTIFIER = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.machine_status.error.invalid_identifier", value)
    );
    private static final DynamicCommandExceptionType MACHINE_EXISTS = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.machine_status.error.name_exists", value)
    );
    private static final DynamicCommandExceptionType MACHINE_NOT_FOUND = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.machine_status.error.name_not_found", value)
    );
    private static final DynamicCommandExceptionType DIMENSION_NOT_FOUND = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.machine_status.error.dimension_not_found", value)
    );
    private static final DynamicCommandExceptionType CHUNK_NOT_LOADED = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.machine_status.error.chunk_not_loaded", value)
    );
    private static final SimpleCommandExceptionType CONFIG_SAVE_FAILED = new SimpleCommandExceptionType(
            Component.translatable("command.carpet-ice-addition.machine_status.error.config_save_failed")
    );

    private MachineStatusCommandMc261() {
    }

    public static void registerArgumentType() {
        ArgumentTypeRegistry.registerArgumentType(
                Identifier.tryParse("carpet-ice-addition:machine_status_single_token"),
                SingleTokenArgumentType.class,
                SingletonArgumentInfo.contextFree(SingleTokenArgumentType::singleToken)
        );
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("machineStatus")
                .requires(MachineStatusCommandMc261::canUseMachineStatus)
                .then(Commands.literal("add")
                        .then(Commands.argument("dimension", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().getServer().levelKeys().stream().map(ResourceKey::identifier),
                                        builder
                                ))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("name", SingleTokenArgumentType.singleToken())
                                                .suggests(MachineStatusCommandMc261::suggestUnusedMachineNames)
                                                .executes(context -> addMachine(
                                                        context,
                                                        parseDimensionIdentifier(context, "dimension"),
                                                        BlockPosArgument.getBlockPos(context, "pos"),
                                                        getMachineName(context, "name")
                                                ))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(machineNames(), builder))
                                .executes(context -> removeMachine(context, getMachineName(context, "name")))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(machineNames(), builder))
                                .then(Commands.argument("newName", SingleTokenArgumentType.singleToken())
                                        .suggests(MachineStatusCommandMc261::suggestUnusedMachineNames)
                                        .executes(context -> renameMachine(
                                                context,
                                                getMachineName(context, "name"),
                                                getMachineName(context, "newName")
                                        )))))
                .then(Commands.literal("update")
                        .then(Commands.argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(machineNames(), builder))
                                .executes(context -> updateMachine(context, getMachineName(context, "name")))))
                .then(Commands.literal("move")
                        .then(Commands.argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(machineNames(), builder))
                                .then(Commands.argument("dimension", SingleTokenArgumentType.singleToken())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                context.getSource().getServer().levelKeys().stream().map(ResourceKey::identifier),
                                                builder
                                        ))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(context -> moveMachine(
                                                        context,
                                                        getMachineName(context, "name"),
                                                        parseDimensionIdentifier(context, "dimension"),
                                                        BlockPosArgument.getBlockPos(context, "pos")
                                                ))))))
                .then(Commands.literal("list")
                        .executes(context -> listMachines(context, null))
                        .then(Commands.literal("running").executes(context -> listMachines(context, MachineStatusKind.RUNNING)))
                        .then(Commands.literal("stopped").executes(context -> listMachines(context, MachineStatusKind.STOPPED)))
                        .then(Commands.literal("invalid").executes(context -> listMachines(context, MachineStatusKind.INVALID)))
                        .then(Commands.literal("unloaded").executes(context -> listMachines(context, MachineStatusKind.UNLOADED))))
                .then(Commands.literal("info")
                        .then(Commands.argument("name", SingleTokenArgumentType.singleToken())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(machineNames(), builder))
                                .executes(context -> showInfo(context, getMachineName(context, "name"))))));
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
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.result.added",
                        Component.literal(name),
                        Component.literal(dimensionId.toString()),
                        Component.literal(formatPos(pos)),
                        Component.literal(shutdownState)
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
                () -> Component.translatable("command.carpet-ice-addition.machine_status.result.removed", Component.literal(name)),
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
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.result.renamed",
                        Component.literal(name),
                        Component.literal(newName)
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
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.result.updated",
                        Component.literal(name),
                        Component.literal(oldState),
                        Component.literal(newState)
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
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.result.moved",
                        Component.literal(name),
                        Component.literal(dimensionId.toString()),
                        Component.literal(formatPos(pos)),
                        Component.literal(shutdownState)
                ),
                false
        );
        return 1;
    }

    private static int listMachines(CommandContext<CommandSourceStack> context, MachineStatusKind filter) {
        List<MachineWithStatus> machines = MachineStatusConfigManager.snapshot().stream()
                .map(record -> new MachineWithStatus(record, evaluateStatus(context.getSource().getServer(), record)))
                .filter(machine -> filter == null || machine.status.kind() == filter)
                .sorted(Comparator
                        .comparingInt((MachineWithStatus machine) -> machine.status.kind().sortOrder())
                        .thenComparing(machine -> machine.record.name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(machine -> machine.record.name()))
                .toList();

        context.getSource().sendSuccess(MachineStatusCommandMc261::headerLine, false);

        if (machines.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("command.carpet-ice-addition.machine_status.list.empty"),
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
                () -> Component.translatable("command.carpet-ice-addition.machine_status.info.machine", Component.literal(record.name())),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("command.carpet-ice-addition.machine_status.info.dimension", Component.literal(record.dimension())),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.info.position",
                        Component.literal(Integer.toString(record.x())),
                        Component.literal(Integer.toString(record.y())),
                        Component.literal(Integer.toString(record.z()))
                ),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.info.saved_state",
                        Component.literal(record.shutdownBlockState())
                ),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.info.current_state",
                        status.currentStateText()
                ),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.machine_status.info.status",
                        Component.translatable(status.kind.translationKey())
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
                    Component.translatable("command.carpet-ice-addition.machine_status.current_state.dimension_not_found")
            );
        }

        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (world == null) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.INVALID,
                    Component.translatable("command.carpet-ice-addition.machine_status.current_state.dimension_not_found")
            );
        }

        BlockPos pos = new BlockPos(record.x(), record.y(), record.z());
        if (!isChunkLoaded(world, pos)) {
            return new MachineRuntimeStatus(
                    MachineStatusKind.UNLOADED,
                    Component.translatable("command.carpet-ice-addition.machine_status.current_state.chunk_not_loaded")
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

    private static String getMachineName(CommandContext<CommandSourceStack> context, String argumentName) throws CommandSyntaxException {
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

    private static Component headerLine() {
        return Component.literal(HEADER_TEXT).withStyle(ChatFormatting.GOLD);
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
        tag.append(Component.translatable(kind.translationKey()));
        tag.append("]");
        return tag.withStyle(statusFormatting(kind));
    }

    private static Component infoButton(String name) {
        Style style = Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/machineStatus info " + name))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.translatable("command.carpet-ice-addition.machine_status.info.hover")
                ));
        return Component.literal("[i]").setStyle(style);
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

    private static boolean canUseMachineStatus(CommandSourceStack source) {
        return CommandHelper.canUseCommand(source, CarpetIceAdditionSettings.commandMachineStatus);
    }

    private record MachineRuntimeStatus(MachineStatusKind kind, Component currentStateText) {
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
