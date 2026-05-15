package com.ice2974.carpeticeaddition.command;

import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public final class KillItemCommandMc261 {
    private static final double MIN_RADIUS = 1.0D;
    private static final double MAX_RADIUS = 1024.0D;

    private static final SimpleCommandExceptionType PLAYER_ONLY = new SimpleCommandExceptionType(
            Component.translatable("command.carpet-ice-addition.killitem.error.player_only")
    );
    private static final DynamicCommandExceptionType INVALID_IDENTIFIER = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.killitem.error.invalid_identifier", value)
    );
    private static final DynamicCommandExceptionType DIMENSION_NOT_FOUND = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.killitem.error.dimension_not_found", value)
    );
    private static final DynamicCommandExceptionType ITEM_NOT_FOUND = new DynamicCommandExceptionType(
            value -> Component.translatable("command.carpet-ice-addition.killitem.error.item_not_found", value)
    );
    private static final SimpleCommandExceptionType CONFIG_SAVE_FAILED = new SimpleCommandExceptionType(
            Component.translatable("command.carpet-ice-addition.killitem.error.config_save_failed")
    );

    private KillItemCommandMc261() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("killitem")
                .requires(KillItemCommandMc261::canUseKillItem)
                .then(Commands.literal("range")
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(MIN_RADIUS, MAX_RADIUS))
                                .executes(context -> executeRange(context, DoubleArgumentType.getDouble(context, "radius")))))
                .then(Commands.literal("dimension")
                        .then(Commands.argument("dimension", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().getServer().levelKeys().stream().map(ResourceKey::identifier),
                                        builder
                                ))
                                .executes(context -> executeDimension(
                                        context,
                                        parseIdentifier(StringArgumentType.getString(context, "dimension"))
                                ))))
                .then(Commands.literal("all")
                        .executes(KillItemCommandMc261::executeAll))
                .then(Commands.literal("config")
                        .then(Commands.literal("blacklist")
                                .executes(KillItemCommandMc261::showBlacklist)
                                .then(Commands.literal("add")
                                        .then(Commands.argument("item", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        BuiltInRegistries.ITEM.keySet(),
                                                        builder
                                                ))
                                                .executes(context -> addBlacklistItem(
                                                        context,
                                                        parseIdentifier(StringArgumentType.getString(context, "item"))
                                                ))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("item", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        KillItemConfigManager.snapshot().blacklist(),
                                                        builder
                                                ))
                                                .executes(context -> removeBlacklistItem(
                                                        context,
                                                        parseIdentifier(StringArgumentType.getString(context, "item"))
                                                ))))
                                .then(Commands.literal("clear")
                                        .executes(KillItemCommandMc261::clearBlacklist)))
                        .then(Commands.literal("clearNamedItems")
                                .executes(KillItemCommandMc261::showClearNamedItems)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setClearNamedItems(
                                                context,
                                                BoolArgumentType.getBool(context, "value")
                                        ))))));
    }

    private static int executeRange(CommandContext<CommandSourceStack> context, double radius) throws CommandSyntaxException {
        ServerPlayer player = getPlayer(context.getSource());
        KillItemConfigManager.Snapshot config = KillItemConfigManager.snapshot();
        Vec3 center = player.position();
        double radiusSquared = radius * radius;
        AABB box = new AABB(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );
        ClearResult result = clearInBox(player.level(), box, itemEntity -> itemEntity.distanceToSqr(center) <= radiusSquared, config);
        context.getSource().sendSuccess(
                () -> Component.translatable(
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

    private static int executeDimension(CommandContext<CommandSourceStack> context, Identifier dimensionId) throws CommandSyntaxException {
        ServerLevel world = getWorld(context.getSource().getServer(), dimensionId);
        KillItemConfigManager.Snapshot config = KillItemConfigManager.snapshot();
        ClearResult result = clearInWorld(world, config);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.killitem.result.dimension",
                        result.entityCount,
                        Component.literal(dimensionId.toString()),
                        result.itemCount,
                        result.summaryText()
                ),
                false
        );
        return result.entityCount;
    }

    private static int executeAll(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        KillItemConfigManager.Snapshot config = KillItemConfigManager.snapshot();
        ClearResult total = new ClearResult();
        for (ServerLevel world : server.getAllLevels()) {
            total.merge(clearInWorld(world, config));
        }
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.killitem.result.all",
                        total.entityCount,
                        total.itemCount,
                        total.summaryText()
                ),
                false
        );
        return total.entityCount;
    }

    private static int showBlacklist(CommandContext<CommandSourceStack> context) {
        Set<String> blacklist = KillItemConfigManager.snapshot().blacklist();
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.killitem.config.blacklist.list",
                        blacklist.size(),
                        formatBlacklist(blacklist)
                ),
                false
        );
        return blacklist.size();
    }

    private static int addBlacklistItem(CommandContext<CommandSourceStack> context, Identifier itemId) throws CommandSyntaxException {
        ensureValidItem(itemId);
        try {
            boolean changed = KillItemConfigManager.addBlacklistItem(itemId.toString());
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            changed
                                    ? "command.carpet-ice-addition.killitem.config.blacklist.added"
                                    : "command.carpet-ice-addition.killitem.config.blacklist.already_present",
                            Component.literal(itemId.toString())
                    ),
                    false
            );
            return changed ? 1 : 0;
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }
    }

    private static int removeBlacklistItem(CommandContext<CommandSourceStack> context, Identifier itemId) throws CommandSyntaxException {
        ensureValidItem(itemId);
        try {
            boolean changed = KillItemConfigManager.removeBlacklistItem(itemId.toString());
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            changed
                                    ? "command.carpet-ice-addition.killitem.config.blacklist.removed"
                                    : "command.carpet-ice-addition.killitem.config.blacklist.not_present",
                            Component.literal(itemId.toString())
                    ),
                    false
            );
            return changed ? 1 : 0;
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }
    }

    private static int clearBlacklist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            boolean changed = KillItemConfigManager.clearBlacklist();
            context.getSource().sendSuccess(
                    () -> Component.translatable(
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

    private static int showClearNamedItems(CommandContext<CommandSourceStack> context) {
        boolean clearNamedItems = KillItemConfigManager.snapshot().clearNamedItems();
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.carpet-ice-addition.killitem.config.clear_named_items.value",
                        Component.translatable(booleanKey(clearNamedItems))
                ),
                false
        );
        return clearNamedItems ? 1 : 0;
    }

    private static int setClearNamedItems(CommandContext<CommandSourceStack> context, boolean value) throws CommandSyntaxException {
        try {
            boolean changed = KillItemConfigManager.setClearNamedItems(value);
            context.getSource().sendSuccess(
                    () -> Component.translatable(
                            changed
                                    ? "command.carpet-ice-addition.killitem.config.clear_named_items.updated"
                                    : "command.carpet-ice-addition.killitem.config.clear_named_items.unchanged",
                            Component.translatable(booleanKey(value))
                    ),
                    false
            );
            return value ? 1 : 0;
        } catch (IOException exception) {
            throw CONFIG_SAVE_FAILED.create();
        }
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        throw PLAYER_ONLY.create();
    }

    private static Identifier parseIdentifier(String value) throws CommandSyntaxException {
        Identifier identifier = Identifier.tryParse(value);
        if (identifier == null) {
            throw INVALID_IDENTIFIER.create(value);
        }
        return identifier;
    }

    private static ServerLevel getWorld(MinecraftServer server, Identifier dimensionId) throws CommandSyntaxException {
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel world = server.getLevel(worldKey);
        if (world == null) {
            throw DIMENSION_NOT_FOUND.create(dimensionId);
        }
        return world;
    }

    private static void ensureValidItem(Identifier itemId) throws CommandSyntaxException {
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            throw ITEM_NOT_FOUND.create(itemId);
        }
    }

    private static ClearResult clearInWorld(ServerLevel world, KillItemConfigManager.Snapshot config) {
        ClearResult result = new ClearResult();
        for (Entity entity : world.getAllEntities()) {
            if (entity instanceof ItemEntity itemEntity && shouldClear(itemEntity, config)) {
                result.record(itemEntity);
                itemEntity.discard();
            }
        }
        return result;
    }

    private static ClearResult clearInBox(
            ServerLevel world,
            AABB box,
            Predicate<ItemEntity> extraFilter,
            KillItemConfigManager.Snapshot config
    ) {
        ClearResult result = new ClearResult();
        for (ItemEntity itemEntity : world.getEntities(
                EntityTypeTest.forClass(ItemEntity.class),
                item -> item.getBoundingBox().intersects(box)
        )) {
            if (!extraFilter.test(itemEntity) || !shouldClear(itemEntity, config)) {
                continue;
            }
            result.record(itemEntity);
            itemEntity.discard();
        }
        return result;
    }

    private static boolean shouldClear(ItemEntity itemEntity, KillItemConfigManager.Snapshot config) {
        ItemStack stack = itemEntity.getItem();
        if (config.blacklist().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            return false;
        }
        return config.clearNamedItems() || (stack.get(DataComponents.CUSTOM_NAME) == null && !itemEntity.hasCustomName());
    }

    private static Component formatBlacklist(Set<String> blacklist) {
        if (blacklist.isEmpty()) {
            return Component.translatable("command.carpet-ice-addition.killitem.summary.none");
        }
        return Component.literal(String.join(", ", blacklist));
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

    private static boolean canUseKillItem(CommandSourceStack source) {
        return CommandHelper.canUseCommand(source, CarpetIceAdditionSettings.commandKillItem);
    }

    private static final class ClearResult {
        private int entityCount;
        private int itemCount;
        private final LinkedHashMap<String, SummaryEntry> summaryEntries = new LinkedHashMap<>();

        private void record(ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            this.entityCount++;
            this.itemCount += stack.getCount();

            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            Component displayText = itemEntity.hasCustomName()
                    ? itemEntity.getCustomName().copy()
                    : stack.getDisplayName().copy();
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

        private Component summaryText() {
            if (this.summaryEntries.isEmpty()) {
                return Component.translatable("command.carpet-ice-addition.killitem.summary.none");
            }

            MutableComponent text = Component.empty();
            boolean first = true;
            for (SummaryEntry entry : this.summaryEntries.values()) {
                if (!first) {
                    text.append(", ");
                }
                first = false;
                text.append(Component.translatable(
                        "command.carpet-ice-addition.killitem.summary.entry",
                        entry.displayText.copy(),
                        entry.itemCount
                ));
            }
            return text;
        }
    }

    private static final class SummaryEntry {
        private final Component displayText;
        private int itemCount;

        private SummaryEntry(Component displayText) {
            this.displayText = displayText;
        }
    }
}
