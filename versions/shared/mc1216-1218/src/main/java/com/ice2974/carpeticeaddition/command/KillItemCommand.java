package com.ice2974.carpeticeaddition.command;

import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class KillItemCommand {
    private static final double MIN_RADIUS = 1.0D;
    private static final double MAX_RADIUS = 1024.0D;
    private static final int SUMMARY_ENTRY_LIMIT = 5;
    private static final int DETAIL_PAGE_SIZE = 10;
    private static final int MAX_CACHED_RESULTS_PER_PLAYER = 5;
    private static final long DETAIL_CACHE_TTL_MILLIS = 10L * 60L * 1000L;
    private static final Map<UUID, LinkedHashMap<String, CachedKillItemResult>> DETAIL_CACHE = new HashMap<>();

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
                .then(literal("detail")
                        .then(argument("resultId", StringArgumentType.word())
                                .then(argument("page", IntegerArgumentType.integer())
                                        .executes(context -> showDetail(
                                                context,
                                                StringArgumentType.getString(context, "resultId"),
                                                IntegerArgumentType.getInteger(context, "page")
                                        )))))
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
        ServerWorld world = (ServerWorld) player.getWorld();
        ClearResult result = clearInBox(world, box, itemEntity -> itemEntity.squaredDistanceTo(center) <= radiusSquared, config);
        String formattedRadius = formatRadius(radius);
        sendClearResult(
                context.getSource(),
                result,
                trString("command.carpet-ice-addition.killitem.result.title.range", formattedRadius),
                trString("command.carpet-ice-addition.killitem.detail.scope.range", world.getRegistryKey().getValue().toString(), formattedRadius)
        );
        return result.entityCount;
    }

    private static int executeDimension(CommandContext<ServerCommandSource> context, Identifier dimensionId) throws CommandSyntaxException {
        ServerWorld world = getWorld(context.getSource().getServer(), dimensionId);
        KillItemConfigManager.Snapshot config = KillItemConfigManager.snapshot();
        ClearResult result = clearInWorld(world, config);
        sendClearResult(
                context.getSource(),
                result,
                trString("command.carpet-ice-addition.killitem.result.title.dimension", dimensionId.toString()),
                dimensionId.toString()
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
        sendClearResult(
                context.getSource(),
                total,
                trString("command.carpet-ice-addition.killitem.result.title.all"),
                trString("command.carpet-ice-addition.killitem.detail.scope.all")
        );
        return total.entityCount;
    }

    private static int showDetail(CommandContext<ServerCommandSource> context, String resultId, int page) {
        Entity entity = context.getSource().getEntity();
        if (!(entity instanceof ServerPlayerEntity player)) {
            context.getSource().sendFeedback(KillItemCommand::expiredDetailText, false);
            return 0;
        }

        CachedKillItemResult result = getCachedResult(player.getUuid(), resultId);
        if (result == null || result.entries.isEmpty()) {
            context.getSource().sendFeedback(KillItemCommand::expiredDetailText, false);
            return 0;
        }

        int totalPages = Math.max(1, (result.entries.size() + DETAIL_PAGE_SIZE - 1) / DETAIL_PAGE_SIZE);
        int resolvedPage = Math.max(1, Math.min(page, totalPages));
        context.getSource().sendFeedback(() -> detailText(result, resolvedPage, totalPages), false);
        return resolvedPage;
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

    private static void sendClearResult(ServerCommandSource source, ClearResult result, String title, String detailScope) {
        Entity entity = source.getEntity();
        String resultId = null;
        if (entity instanceof ServerPlayerEntity player && result.summaryEntries.size() > SUMMARY_ENTRY_LIMIT) {
            resultId = cacheResult(player.getUuid(), detailScope, result);
        }
        String finalResultId = resultId;
        source.sendFeedback(() -> summaryText(result, title, finalResultId), false);
    }

    private static Text summaryText(ClearResult result, String title, String resultId) {
        List<SummaryEntry> entries = result.sortedEntries();
        MutableText text = Text.literal("\n[KillItem] " + title);
        text.append(Text.literal("\n"));
        text.append(tr("command.carpet-ice-addition.killitem.result.entities", formatCount(result.entityCount)));
        text.append(Text.literal("\n"));
        text.append(tr("command.carpet-ice-addition.killitem.result.items", formatCount(result.itemCount)));
        text.append(Text.literal("\n"));
        text.append(tr("command.carpet-ice-addition.killitem.result.details", formatSummaryEntries(entries)));

        int omittedEntries = Math.max(0, entries.size() - SUMMARY_ENTRY_LIMIT);
        if (omittedEntries > 0) {
            text.append(Text.literal("\n"));
            text.append(tr("command.carpet-ice-addition.killitem.result.more", omittedEntries));
            if (resultId != null) {
                text.append(Text.literal(" "));
                text.append(commandButton(
                        "command.carpet-ice-addition.killitem.button.expand",
                        "/killitem detail " + resultId + " 1"
                ));
            }
        }
        return text;
    }

    private static Text detailText(CachedKillItemResult result, int page, int totalPages) {
        MutableText text = Text.literal("\n[KillItem] ");
        text.append(tr("command.carpet-ice-addition.killitem.detail.header", page, totalPages));
        text.append(Text.literal("\n"));
        text.append(tr("command.carpet-ice-addition.killitem.detail.scope", result.scopeText));
        text.append(Text.literal("\n"));
        text.append(tr(
                "command.carpet-ice-addition.killitem.detail.total",
                formatCount(result.entityCount),
                formatCount(result.itemCount)
        ));

        int startIndex = (page - 1) * DETAIL_PAGE_SIZE;
        int endIndex = Math.min(startIndex + DETAIL_PAGE_SIZE, result.entries.size());
        for (int index = startIndex; index < endIndex; index++) {
            CachedSummaryEntry entry = result.entries.get(index);
            text.append(Text.literal("\n"));
            text.append(tr(
                    "command.carpet-ice-addition.killitem.detail.entry",
                    String.format(Locale.ROOT, "%02d", index + 1),
                    entry.displayName,
                    formatCount(entry.itemCount)
            ));
        }

        if (totalPages > 1) {
            text.append(Text.literal("\n"));
            if (page > 1) {
                text.append(commandButton(
                        "command.carpet-ice-addition.killitem.button.previous",
                        "/killitem detail " + result.resultId + " " + (page - 1)
                ));
            }
            if (page > 1 && page < totalPages) {
                text.append(Text.literal(" "));
            }
            if (page < totalPages) {
                text.append(commandButton(
                        "command.carpet-ice-addition.killitem.button.next",
                        "/killitem detail " + result.resultId + " " + (page + 1)
                ));
            }
        }
        return text;
    }

    private static Text expiredDetailText() {
        return Text.literal("\n[KillItem] ").append(tr("command.carpet-ice-addition.killitem.detail.expired"));
    }

    private static String formatSummaryEntries(List<SummaryEntry> entries) {
        if (entries.isEmpty()) {
            return trString("command.carpet-ice-addition.killitem.summary.none");
        }
        List<String> formattedEntries = new ArrayList<>();
        int limit = Math.min(SUMMARY_ENTRY_LIMIT, entries.size());
        for (int index = 0; index < limit; index++) {
            SummaryEntry entry = entries.get(index);
            formattedEntries.add(trString(
                    "command.carpet-ice-addition.killitem.summary.entry",
                    entry.displayName,
                    formatCount(entry.itemCount)
            ));
        }
        return String.join(trString("command.carpet-ice-addition.killitem.summary.separator"), formattedEntries);
    }

    private static String cacheResult(UUID playerUuid, String scopeText, ClearResult result) {
        cleanupExpiredCache();
        String resultId = UUID.randomUUID().toString().replace("-", "");
        LinkedHashMap<String, CachedKillItemResult> playerCache = DETAIL_CACHE.computeIfAbsent(playerUuid, ignored -> new LinkedHashMap<>());
        playerCache.put(resultId, new CachedKillItemResult(
                resultId,
                scopeText,
                result.entityCount,
                result.itemCount,
                result.sortedEntries().stream().map(CachedSummaryEntry::new).toList(),
                System.currentTimeMillis() + DETAIL_CACHE_TTL_MILLIS
        ));
        while (playerCache.size() > MAX_CACHED_RESULTS_PER_PLAYER) {
            Iterator<String> iterator = playerCache.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
        return resultId;
    }

    private static CachedKillItemResult getCachedResult(UUID playerUuid, String resultId) {
        cleanupExpiredCache();
        Map<String, CachedKillItemResult> playerCache = DETAIL_CACHE.get(playerUuid);
        if (playerCache == null) {
            return null;
        }
        return playerCache.get(resultId);
    }

    private static void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, LinkedHashMap<String, CachedKillItemResult>>> playerIterator = DETAIL_CACHE.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, LinkedHashMap<String, CachedKillItemResult>> playerEntry = playerIterator.next();
            playerEntry.getValue().values().removeIf(result -> result.expiresAtMillis <= now);
            if (playerEntry.getValue().isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private static Text commandButton(String key, String command) {
        return tr(key).setStyle(commandButtonStyle(command));
    }

    private static Style commandButtonStyle(String command) {
        Style style = Style.EMPTY.withColor(Formatting.AQUA);
        style = withClickEvent(style, command);
        return withHoverEvent(style);
    }

    // ClickEvent 与 HoverEvent 的反射解析结果各自独立缓存、独立失败标志，
    // 一个事件构造失败不会全局禁用另一个事件。解析得到的不可变 EventBindings
    // 一次性缓存 event class / action class / action 常量 / event 构造器 / Style setter，
    // 之后每次按钮生成只执行 newInstance + Method.invoke，不再重复任何反射查找。
    // 懒加载用 synchronized 双检锁，保证要么发布完整可用的 bindings，要么保持 null；
    // 不会出现半初始化状态。
    private static volatile EventBindings<String> clickBindings;
    private static volatile EventBindings<Text> hoverBindings;
    private static final AtomicBoolean CLICK_EVENT_FAILED = new AtomicBoolean(false);
    private static final AtomicBoolean HOVER_EVENT_FAILED = new AtomicBoolean(false);

    private static Style withClickEvent(Style style, String command) {
        if (CLICK_EVENT_FAILED.get()) {
            return style;
        }
        EventBindings<String> bindings = clickBindings;
        if (bindings == null) {
            try {
                bindings = loadClickBindings();
            } catch (Throwable throwable) {
                CLICK_EVENT_FAILED.set(true);
                reportTextEventFailure("killitemTextEvents", throwable);
                return style;
            }
        }
        try {
            Object clickEvent = bindings.eventFactory.apply(command);
            return (Style) bindings.styleSetter.invoke(style, clickEvent);
        } catch (Throwable throwable) {
            CLICK_EVENT_FAILED.set(true);
            reportTextEventFailure("killitemTextEvents", throwable);
        }
        return style;
    }

    private static Style withHoverEvent(Style style) {
        if (HOVER_EVENT_FAILED.get()) {
            return style;
        }
        EventBindings<Text> bindings = hoverBindings;
        if (bindings == null) {
            try {
                bindings = loadHoverBindings();
            } catch (Throwable throwable) {
                HOVER_EVENT_FAILED.set(true);
                reportTextEventFailure("killitemTextEvents", throwable);
                return style;
            }
        }
        try {
            Object hoverEvent = bindings.eventFactory.apply(tr("command.carpet-ice-addition.killitem.button.hover"));
            return (Style) bindings.styleSetter.invoke(style, hoverEvent);
        } catch (Throwable throwable) {
            HOVER_EVENT_FAILED.set(true);
            reportTextEventFailure("killitemTextEvents", throwable);
        }
        return style;
    }

    private static void reportTextEventFailure(String featureName, Throwable throwable) {
        // 委托给项目既有的 feature-level 兼容性上报机制，首次失败 warn 一次，后续静默 fallback。
        CarpetIceAdditionMod.reportFeatureCompatibilityIssue(featureName, throwable);
    }

    // 不可变：解析完成后所有字段不再变化，可安全发布给其它线程。
    private static final class EventBindings<A> {
        final Method styleSetter;
        final Function<A, Object> eventFactory;

        EventBindings(Method styleSetter, Function<A, Object> eventFactory) {
            this.styleSetter = styleSetter;
            this.eventFactory = eventFactory;
        }
    }

    private static EventBindings<String> loadClickBindings() throws ReflectiveOperationException {
        EventBindings<String> existing = clickBindings;
        if (existing != null) {
            return existing;
        }
        synchronized (KillItemCommand.class) {
            if (clickBindings == null) {
                clickBindings = resolveClickBindings();
            }
            return clickBindings;
        }
    }

    private static EventBindings<Text> loadHoverBindings() throws ReflectiveOperationException {
        EventBindings<Text> existing = hoverBindings;
        if (existing != null) {
            return existing;
        }
        synchronized (KillItemCommand.class) {
            if (hoverBindings == null) {
                hoverBindings = resolveHoverBindings();
            }
            return hoverBindings;
        }
    }

    // 一次性解析 ClickEvent 全部反射元数据：event class、action class、action 常量、
    // event 构造器、Style setter。解析期复用既有反射原语，解析结果全部缓存进 EventBindings。
    private static EventBindings<String> resolveClickBindings() throws ReflectiveOperationException {
        Class<?> clickEventClass = findStyleEventType("RUN_COMMAND");
        Class<?> actionClass = requireActionClass(clickEventClass, "RUN_COMMAND");
        Object actionConstant = actionConstant(actionClass, "RUN_COMMAND");
        Constructor<?> constructor;
        if (clickEventClass.isInterface()) {
            constructor = findInterfaceEventConstructor(clickEventClass, actionClass, "RUN_COMMAND", String.class);
        } else {
            constructor = clickEventClass.getConstructor(actionClass, String.class);
        }
        Method styleSetter = findStyleSetter(clickEventClass);
        Function<String, Object> factory = command -> {
            try {
                if (clickEventClass.isInterface()) {
                    return constructor.newInstance(command);
                }
                return constructor.newInstance(actionConstant, command);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        };
        return new EventBindings<>(styleSetter, factory);
    }

    // 一次性解析 HoverEvent 全部反射元数据，结构与 resolveClickBindings 对称。
    private static EventBindings<Text> resolveHoverBindings() throws ReflectiveOperationException {
        Class<?> hoverEventClass = findStyleEventType("SHOW_TEXT");
        Class<?> actionClass = requireActionClass(hoverEventClass, "SHOW_TEXT");
        Object actionConstant = actionConstant(actionClass, "SHOW_TEXT");
        Constructor<?> constructor;
        if (hoverEventClass.isInterface()) {
            constructor = findInterfaceEventConstructor(hoverEventClass, actionClass, "SHOW_TEXT", Text.class);
        } else {
            constructor = findLegacyHoverConstructor(hoverEventClass, actionClass);
        }
        Method styleSetter = findStyleSetter(hoverEventClass);
        Function<Text, Object> factory = text -> {
            try {
                if (hoverEventClass.isInterface()) {
                    return constructor.newInstance(text);
                }
                return constructor.newInstance(actionConstant, text);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        };
        return new EventBindings<>(styleSetter, factory);
    }

    // 接口形态（1.21.5+）：在嵌套实现类中找到形如 (paramType) 的构造器，
    // 并用 hasAction 验证其产出的事件 action 正确。仅解析期调用一次。
    private static Constructor<?> findInterfaceEventConstructor(Class<?> eventClass, Class<?> actionClass,
                                                                String actionName, Class<?> paramType) throws ReflectiveOperationException {
        for (Class<?> eventImplClass : eventClass.getDeclaredClasses()) {
            if (eventClass.isAssignableFrom(eventImplClass)) {
                try {
                    Constructor<?> constructor = eventImplClass.getDeclaredConstructor(paramType);
                    Object sample = constructor.newInstance(paramType == String.class ? "" : Text.empty());
                    if (hasAction(sample, eventClass, actionClass, actionName)) {
                        return constructor;
                    }
                } catch (NoSuchMethodException ignored) {
                    // Try the next nested event implementation.
                }
            }
        }
        throw new NoSuchMethodException("No " + actionName + " event implementation");
    }

    // 旧式 class 形态 HoverEvent：在公有构造器中匹配 (actionClass, Text)。
    private static Constructor<?> findLegacyHoverConstructor(Class<?> hoverEventClass, Class<?> actionClass) throws NoSuchMethodException {
        for (Constructor<?> constructor : hoverEventClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 2
                    && parameterTypes[0] == actionClass
                    && parameterTypes[1].isAssignableFrom(Text.class)) {
                return constructor;
            }
        }
        throw new NoSuchMethodException("No legacy show text hover event constructor");
    }

    // 在 Style 上找到接受 eventClass 的 setter（返回 Style、单参、参数可赋值）。
    private static Method findStyleSetter(Class<?> eventClass) throws NoSuchMethodException {
        for (Method method : Style.class.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getReturnType() == Style.class
                    && parameterTypes.length == 1
                    && parameterTypes[0].isAssignableFrom(eventClass)) {
                return method;
            }
        }
        throw new NoSuchMethodException("No style event setter for " + eventClass.getName());
    }

    private static Class<?> findStyleEventType(String actionName) throws NoSuchMethodException {
        for (Field field : Style.class.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (findActionClass(fieldType, actionName) != null) {
                return fieldType;
            }
        }
        throw new NoSuchMethodException("No style event type for action " + actionName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object actionConstant(Class<?> actionClass, String actionName) throws ReflectiveOperationException {
        // 新式（1.21.5+）Action 为 enum，常量名不被运行期映射重写，可直接按名取。
        if (actionClass.isEnum()) {
            return Enum.valueOf((Class<Enum>) actionClass.asSubclass(Enum.class), actionName);
        }
        // 旧式（1.21.1-1.21.4）HoverEvent$Action 为普通类，常量字段名在运行期
        // intermediary 映射下被改写为 field_XXXX，不能按名查找；改为遍历静态常量
        // 并按其序列化名（值内 String 实例字段，如 "show_text"）匹配，与字段映射命名无关。
        Object constant = findPlainActionConstant(actionClass, actionName);
        if (constant == null) {
            throw new NoSuchFieldException("No action constant " + actionName + " on " + actionClass.getName());
        }
        return constant;
    }

    // 普通类形态 Action 常量查找：运行期字段名被 intermediary 重映射，不能按名取字段；
    // 改为遍历该 Action 类自身的 public static final 字段，读取常量值后比较其序列化名。
    // actionName 为开发期名称（如 "SHOW_TEXT"），序列化名为其小写形式（如 "show_text"）。
    private static Object findPlainActionConstant(Class<?> actionClass, String actionName) {
        String serialized = actionName.toLowerCase(Locale.ROOT);
        try {
            for (Field field : actionClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
                    continue;
                }
                if (!actionClass.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null && serialized.equals(readActionSerializedName(value))) {
                    return value;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // 字段访问失败则视为该常量未找到，继续尝试其它字段。
        }
        return null;
    }

    // 读取 Action 常量的序列化名（如 "show_text"），不依赖字段/方法名（运行期会被重映射）。
    // HoverEvent$Action 实现了 StringIdentifiable，序列化名存储在唯一的 String 实例字段中。
    private static String readActionSerializedName(Object action) throws ReflectiveOperationException {
        for (Field field : action.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(action);
            if (value instanceof String s) {
                return s;
            }
        }
        return null;
    }

    private static boolean hasAction(Object event, Class<?> eventClass, Class<?> actionClass, String actionName) throws ReflectiveOperationException {
        for (Method method : eventClass.getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == actionClass) {
                Object action = method.invoke(event);
                return action instanceof Enum<?> actionEnum && actionEnum.name().equals(actionName);
            }
        }
        return false;
    }

    private static Class<?> requireActionClass(Class<?> eventClass, String actionName) throws NoSuchMethodException {
        Class<?> actionClass = findActionClass(eventClass, actionName);
        if (actionClass == null) {
            throw new NoSuchMethodException("No action " + actionName + " on " + eventClass.getName());
        }
        return actionClass;
    }

    private static Class<?> findActionClass(Class<?> eventClass, String actionName) {
        // 1) enum 形态（1.21.5+ ClickEvent/HoverEvent Action 与 1.21.1-1.21.4 ClickEvent$Action）：
        // 遍历嵌套枚举类的常量，按 enum 常量名匹配（enum 名不被运行期映射重写）。
        for (Class<?> nestedClass : eventClass.getDeclaredClasses()) {
            if (nestedClass.isEnum()) {
                for (Object constant : nestedClass.getEnumConstants()) {
                    if (constant instanceof Enum<?> action && action.name().equals(actionName)) {
                        return nestedClass;
                    }
                }
            }
        }
        // 2) 普通类形态（1.21.1-1.21.4 HoverEvent$Action）：运行期字段名被 intermediary
        // 重映射为 field_XXXX，不能按名查找字段；改为遍历嵌套类的静态 final 字段，
        // 读取常量值的序列化名（如 "show_text"）匹配。返回该嵌套类作为 actionClass。
        String serialized = actionName.toLowerCase(Locale.ROOT);
        for (Class<?> nestedClass : eventClass.getDeclaredClasses()) {
            if (nestedClass.isEnum()) {
                continue;
            }
            if (hasPlainActionConstant(nestedClass, serialized)) {
                return nestedClass;
            }
        }
        return null;
    }

    // 普通类形态 Action 是否含序列化名匹配的静态 final 常量。运行期字段名不可用，
    // 故按字段值的序列化名匹配；字段类型必须为该 Action 类自身或其超类/接口。
    private static boolean hasPlainActionConstant(Class<?> actionClass, String serialized) {
        try {
            for (Field field : actionClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
                    continue;
                }
                if (!actionClass.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null && serialized.equals(readActionSerializedName(value))) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // 字段访问失败则视为该类无匹配常量。
        }
        return false;
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

    private static String formatBlacklist(Set<String> blacklist) {
        if (blacklist.isEmpty()) {
            return trString("command.carpet-ice-addition.killitem.summary.none");
        }
        return String.join(", ", blacklist);
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

    private static String formatCount(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private static boolean canUseKillItem(ServerCommandSource source) {
        return CommandHelper.canUseCommand(source, com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings.commandKillItem);
    }

    private static MutableText tr(String key, Object... args) {
        return Text.literal(TranslationFormatUtil.translate(key, args));
    }

    private static String trString(String key, Object... args) {
        return TranslationFormatUtil.translate(key, args);
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
            String displayName = itemEntity.hasCustomName()
                    ? itemEntity.getCustomName().getString()
                    : stack.getName().getString();
            String summaryKey = itemId + "|" + displayName;

            SummaryEntry entry = this.summaryEntries.computeIfAbsent(summaryKey, key -> new SummaryEntry(itemId, displayName));
            entry.itemCount += stack.getCount();
        }

        private void merge(ClearResult other) {
            this.entityCount += other.entityCount;
            this.itemCount += other.itemCount;
            other.summaryEntries.forEach((key, value) -> {
                SummaryEntry entry = this.summaryEntries.computeIfAbsent(key, ignored -> new SummaryEntry(value.itemId, value.displayName));
                entry.itemCount += value.itemCount;
            });
        }

        private List<SummaryEntry> sortedEntries() {
            return this.summaryEntries.values().stream()
                    .map(SummaryEntry::copy)
                    .sorted(Comparator
                            .comparingLong((SummaryEntry entry) -> entry.itemCount).reversed()
                            .thenComparing(entry -> entry.displayName)
                            .thenComparing(entry -> entry.itemId))
                    .toList();
        }
    }

    private static final class SummaryEntry {
        private final String itemId;
        private final String displayName;
        private long itemCount;

        private SummaryEntry(String itemId, String displayName) {
            this.itemId = itemId;
            this.displayName = displayName;
        }

        private SummaryEntry copy() {
            SummaryEntry copy = new SummaryEntry(this.itemId, this.displayName);
            copy.itemCount = this.itemCount;
            return copy;
        }
    }

    private static final class CachedSummaryEntry {
        private final String displayName;
        private final long itemCount;

        private CachedSummaryEntry(SummaryEntry entry) {
            this.displayName = entry.displayName;
            this.itemCount = entry.itemCount;
        }
    }

    private static final class CachedKillItemResult {
        private final String resultId;
        private final String scopeText;
        private final int entityCount;
        private final long itemCount;
        private final List<CachedSummaryEntry> entries;
        private final long expiresAtMillis;

        private CachedKillItemResult(
                String resultId,
                String scopeText,
                int entityCount,
                long itemCount,
                List<CachedSummaryEntry> entries,
                long expiresAtMillis
        ) {
            this.resultId = resultId;
            this.scopeText = scopeText;
            this.entityCount = entityCount;
            this.itemCount = itemCount;
            this.entries = List.copyOf(entries);
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
