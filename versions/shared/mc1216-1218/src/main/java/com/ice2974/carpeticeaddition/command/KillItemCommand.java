package com.ice2974.carpeticeaddition.command;

import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class KillItemCommand {
    private static final double MIN_RADIUS = 1.0D;
    private static final double MAX_RADIUS = 1024.0D;
    private static final int SUMMARY_ENTRY_LIMIT = 20;

    private static final SimpleCommandExceptionType PLAYER_ONLY = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.killitem.error.player_only")
    );
    private static final DynamicCommandExceptionType INVALID_IDENTIFIER = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.killitem.error.invalid_identifier", value)
    );
    private static final DynamicCommandExceptionType DIMENSION_NOT_FOUND = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.killitem.error.dimension_not_found", value)
    );
    private static final DynamicCommandExceptionType ITEM_NOT_FOUND = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.killitem.error.item_not_found", value)
    );
    private static final SimpleCommandExceptionType CONFIG_SAVE_FAILED = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.killitem.error.config_save_failed")
    );

    private KillItemCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("killitem")
                .requires(KillItemCommand::canUseKillItem)
                .then(literal("range")
                        .then(argument("radius", DoubleArgumentType.doubleArg(MIN_RADIUS, MAX_RADIUS))
                                .executes(context -> executeRange(context, DoubleArgumentType.getDouble(context, "radius")))))
                .then(literal("dimension")
                        .then(argument("dimension", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                        context.getSource().getServer().getWorldRegistryKeys().stream().map(RegistryKey::getValue),
                                        builder
                                ))
                                .executes(context -> executeDimension(
                                        context,
                                        IdentifierArgumentType.getIdentifier(context, "dimension")
                                ))))
                .then(literal("all")
                        .executes(KillItemCommand::executeAll))
                .then(literal("config")
                        .then(literal("blacklist")
                                .executes(KillItemCommand::showBlacklist)
                                .then(literal("add")
                                        .then(argument("item", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(Registries.ITEM.getIds(), builder))
                                                .executes(context -> addBlacklistItem(
                                                        context,
                                                        parseItemIdentifier(StringArgumentType.getString(context, "item"))
                                                ))))
                                .then(literal("remove")
                                        .then(argument("item", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        KillItemConfigManager.snapshot().blacklist(),
                                                        builder
                                                ))
                                                .executes(context -> removeBlacklistItem(
                                                        context,
                                                        parseItemIdentifier(StringArgumentType.getString(context, "item"))
                                                ))))
                                .then(literal("clear")
                                        .executes(KillItemCommand::clearBlacklist)))
                        .then(literal("clearNamedItems")
                                .executes(KillItemCommand::showClearNamedItems)
                                .then(argument("value", BoolArgumentType.bool())
                                        .executes(context -> setClearNamedItems(
                                                context,
                                                BoolArgumentType.getBool(context, "value")
                                        ))))));
    }

    private static int executeRange(CommandContext<ServerCommandSource> context, double radius) throws CommandSyntaxException {
        ServerPlayerEntity player = getPlayer(context.getSource());
        KillItemConfigManager.Snapshot config = KillItemConfigManager.snapshot();
        Vec3d center = new Vec3d(player.getX(), player.getY(), player.getZ());
        double radiusSquared = radius * radius;
        Box box = new Box(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );
        ClearResult result = clearInBox((ServerWorld) player.getWorld(), box, itemEntity -> itemEntity.squaredDistanceTo(center) <= radiusSquared, config);
        context.getSource().sendFeedback(
                () -> tr(
                        "command.carpet-ice-addition.killitem.result.range",
                        result.entityCount,
                        formatRadius(radius),
                        result.itemCount,
                        result.summaryText()
                ),
                false
        );
        return result.entityCount;
    }

    private static int executeDimension(CommandContext<ServerCommandSource> context, Identifier dimensionId) throws CommandSyntaxException {
        ServerWorld world = getWorld(context.getSource().getServer(), dimensionId);
        KillItemConfigManager.Snapshot config = KillItemConfigManager.snapshot();
        ClearResult result = clearInWorld(world, config);
        context.getSource().sendFeedback(
                () -> tr(
                        "command.carpet-ice-addition.killitem.result.dimension",
                        result.entityCount,
                        dimensionId.toString(),
                        result.itemCount,
                        result.summaryText()
                ),
                false
        );
        return result.entityCount;
    }

    private static int executeAll(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        KillItemConfigManager.Snapshot config = KillItemConfigManager.snapshot();
        ClearResult total = new ClearResult();
        for (RegistryKey<World> worldKey : server.getWorldRegistryKeys()) {
            ServerWorld world = server.getWorld(worldKey);
            if (world != null) {
                total.merge(clearInWorld(world, config));
            }
        }
        context.getSource().sendFeedback(
                () -> tr(
                        "command.carpet-ice-addition.killitem.result.all",
                        total.entityCount,
                        total.itemCount,
                        total.summaryText()
                ),
                false
        );
        return total.entityCount;
    }

    private static int showBlacklist(CommandContext<ServerCommandSource> context) {
        Set<String> blacklist = KillItemConfigManager.snapshot().blacklist();
        context.getSource().sendFeedback(
                () -> tr(
                        "command.carpet-ice-addition.killitem.config.blacklist.list",
                        blacklist.size(),
                        formatBlacklist(blacklist)
                ),
                false
        );
        return blacklist.size();
    }

    private static int addBlacklistItem(CommandContext<ServerCommandSource> context, Identifier itemId) throws CommandSyntaxException {
        ensureValidItem(itemId);
        try {
            boolean changed = KillItemConfigManager.addBlacklistItem(itemId.toString());
            context.getSource().sendFeedback(
                    () -> tr(
                            changed
                                    ? "command.carpet-ice-addition.killitem.config.blacklist.added"
                                    : "command.carpet-ice-addition.killitem.config.blacklist.already_present",
                            itemId.toString()
                    ),
                    false
            );
            return changed ? 1 : 0;
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }
    }

    private static int removeBlacklistItem(CommandContext<ServerCommandSource> context, Identifier itemId) throws CommandSyntaxException {
        ensureValidItem(itemId);
        try {
            boolean changed = KillItemConfigManager.removeBlacklistItem(itemId.toString());
            context.getSource().sendFeedback(
                    () -> tr(
                            changed
                                    ? "command.carpet-ice-addition.killitem.config.blacklist.removed"
                                    : "command.carpet-ice-addition.killitem.config.blacklist.not_present",
                            itemId.toString()
                    ),
                    false
            );
            return changed ? 1 : 0;
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }
    }

    private static int clearBlacklist(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            boolean changed = KillItemConfigManager.clearBlacklist();
            context.getSource().sendFeedback(
                    () -> tr(
                            changed
                                    ? "command.carpet-ice-addition.killitem.config.blacklist.cleared"
                                    : "command.carpet-ice-addition.killitem.config.blacklist.already_empty"
                    ),
                    false
            );
            return changed ? 1 : 0;
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }
    }

    private static int showClearNamedItems(CommandContext<ServerCommandSource> context) {
        boolean clearNamedItems = KillItemConfigManager.snapshot().clearNamedItems();
        context.getSource().sendFeedback(
                () -> tr(
                        "command.carpet-ice-addition.killitem.config.clear_named_items.value",
                        trString(booleanKey(clearNamedItems))
                ),
                false
        );
        return clearNamedItems ? 1 : 0;
    }

    private static int setClearNamedItems(CommandContext<ServerCommandSource> context, boolean value) throws CommandSyntaxException {
        try {
            boolean changed = KillItemConfigManager.setClearNamedItems(value);
            context.getSource().sendFeedback(
                    () -> tr(
                            changed
                                    ? "command.carpet-ice-addition.killitem.config.clear_named_items.updated"
                                    : "command.carpet-ice-addition.killitem.config.clear_named_items.unchanged",
                            trString(booleanKey(value))
                    ),
                    false
            );
            return value ? 1 : 0;
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }
    }

    private static ServerPlayerEntity getPlayer(ServerCommandSource source) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayerEntity player) {
            return player;
        }
        throw PLAYER_ONLY.create();
    }

    private static ServerWorld getWorld(MinecraftServer server, Identifier dimensionId) throws CommandSyntaxException {
        RegistryKey<World> worldKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimensionId);
        ServerWorld world = server.getWorld(worldKey);
        if (world == null) {
            throw DIMENSION_NOT_FOUND.create(dimensionId);
        }
        return world;
    }

    private static Identifier parseItemIdentifier(String value) throws CommandSyntaxException {
        String normalized = value.trim();
        if (normalized.indexOf(':') < 0) {
            normalized = "minecraft:" + normalized;
        }

        Identifier identifier = Identifier.tryParse(normalized);
        if (identifier == null) {
            throw INVALID_IDENTIFIER.create(value);
        }
        return identifier;
    }

    private static void ensureValidItem(Identifier itemId) throws CommandSyntaxException {
        if (!Registries.ITEM.containsId(itemId)) {
            throw ITEM_NOT_FOUND.create(itemId);
        }
    }

    private static ClearResult clearInWorld(ServerWorld world, KillItemConfigManager.Snapshot config) {
        ClearResult result = new ClearResult();
        List<ItemEntity> itemsToClear = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof ItemEntity itemEntity && shouldClear(itemEntity, config)) {
                itemsToClear.add(itemEntity);
            }
        }
        for (ItemEntity itemEntity : itemsToClear) {
            if (!itemEntity.isRemoved() && shouldClear(itemEntity, config)) {
                result.record(itemEntity);
                itemEntity.discard();
            }
        }
        return result;
    }

    private static ClearResult clearInBox(
            ServerWorld world,
            Box box,
            Predicate<ItemEntity> extraFilter,
            KillItemConfigManager.Snapshot config
    ) {
        ClearResult result = new ClearResult();
        for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, box, item -> true)) {
            if (!extraFilter.test(itemEntity) || !shouldClear(itemEntity, config)) {
                continue;
            }
            result.record(itemEntity);
            itemEntity.discard();
        }
        return result;
    }

    private static boolean shouldClear(ItemEntity itemEntity, KillItemConfigManager.Snapshot config) {
        ItemStack stack = itemEntity.getStack();
        if (config.blacklist().contains(Registries.ITEM.getId(stack.getItem()).toString())) {
            return false;
        }
        return config.clearNamedItems() || (stack.get(DataComponentTypes.CUSTOM_NAME) == null && !itemEntity.hasCustomName());
    }

    private static Text formatBlacklist(Set<String> blacklist) {
        if (blacklist.isEmpty()) {
            return tr("command.carpet-ice-addition.killitem.summary.none");
        }
        return Text.literal(String.join(", ", blacklist));
    }

    private static String booleanKey(boolean value) {
        return value
                ? "command.carpet-ice-addition.killitem.boolean.true"
                : "command.carpet-ice-addition.killitem.boolean.false";
    }

    private static String formatRadius(double radius) {
        if (Math.rint(radius) == radius) {
            return Integer.toString((int) radius);
        }
        return String.format(Locale.ROOT, "%.2f", radius);
    }

    private static boolean canUseKillItem(ServerCommandSource source) {
        return CommandHelper.canUseCommand(source, com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings.commandKillItem);
    }

    private static final class ClearResult {
        private int entityCount;
        private long itemCount;
        private final LinkedHashMap<String, SummaryEntry> summaryEntries = new LinkedHashMap<>();

        private void record(ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            this.entityCount++;
            this.itemCount += stack.getCount();

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            Text displayText = itemEntity.hasCustomName()
                    ? itemEntity.getCustomName().copy()
                    : stack.getName().copy();
            String displayKey = displayText.getString();
            String summaryKey = itemId + "|" + displayKey;

            SummaryEntry entry = this.summaryEntries.computeIfAbsent(summaryKey, key -> new SummaryEntry(displayText));
            entry.itemCount += stack.getCount();
        }

        private void merge(ClearResult other) {
            this.entityCount += other.entityCount;
            this.itemCount += other.itemCount;
            other.summaryEntries.forEach((key, value) -> {
                SummaryEntry entry = this.summaryEntries.computeIfAbsent(key, ignored -> new SummaryEntry(value.displayText.copy()));
                entry.itemCount += value.itemCount;
            });
        }

        private Text summaryText() {
            if (this.summaryEntries.isEmpty()) {
                return tr("command.carpet-ice-addition.killitem.summary.none");
            }

            MutableText text = Text.empty();
            boolean first = true;
            int index = 0;
            for (SummaryEntry entry : this.summaryEntries.values()) {
                if (index >= SUMMARY_ENTRY_LIMIT) {
                    break;
                }
                if (!first) {
                    text.append(Text.literal(", "));
                }
                first = false;
                text.append(tr(
                        "command.carpet-ice-addition.killitem.summary.entry",
                        entry.displayText.getString(),
                        entry.itemCount
                ));
                index++;
            }
            int omittedEntries = this.summaryEntries.size() - SUMMARY_ENTRY_LIMIT;
            if (omittedEntries > 0) {
                if (!first) {
                    text.append(Text.literal(", "));
                }
                text.append(tr(
                        "command.carpet-ice-addition.killitem.summary.truncated",
                        omittedEntries
                ));
            }
            return text;
        }
    }

    private static MutableText tr(String key, Object... args) {
        return Text.literal(TranslationFormatUtil.translate(key, args));
    }

    private static String trString(String key, Object... args) {
        return TranslationFormatUtil.translate(key, args);
    }

    private static final class SummaryEntry {
        private final Text displayText;
        private long itemCount;

        private SummaryEntry(Text displayText) {
            this.displayText = displayText;
        }
    }
}
