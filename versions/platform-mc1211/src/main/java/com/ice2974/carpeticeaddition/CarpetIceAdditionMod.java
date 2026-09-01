package com.ice2974.carpeticeaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.rules.BotTabListNameHelper;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksDataPackController;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksState;
import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationRuleHelper;
import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionEndPlatformSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionLowVersionSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.CarpetIceAdditionTranslations;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsLogger121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import com.ice2974.carpeticeaddition.command.KillItemCommand;
import com.ice2974.carpeticeaddition.command.KillItemConfigManager;
import com.ice2974.carpeticeaddition.command.MachineStatusCommand;
import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CarpetIceAdditionMod implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "carpet-ice-addition";
    public static final String MOD_NAME = "Carpet Ice Addition";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    private static final CarpetIceAdditionMod INSTANCE = new CarpetIceAdditionMod();
    private static final AtomicBoolean SAFE_SCAFFOLDING_BREAK_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CRAFTER_OUTPUT_RULE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean RECORD_WORLD_EVENT_FIX_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean SPAWNERS_IGNORE_INVISIBLE_PLAYERS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_KELP_NATURAL_GROWTH_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_AMETHYST_GROWTH_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CAN_MINE_FROSTED_ICE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean FROSTED_ICE_PROPER_TOOL_FIX_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean BEACON_PROPER_TOOL_FIX_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean ICE_LIKE_MAGMA_BLOCKS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_NYLIUM_DECAY_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean BOT_TAB_LIST_NAME_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_PLAYER_ATTACKING_TAMED_MOBS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean FAKE_PLAYER_IGNORE_THORNS_DAMAGE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean EASY_WATERLOGGED_BLOCK_PLACEMENT_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean PHANTOM_SPAWN_WARNING_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean NEUTRAL_PHANTOMS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean KILLITEM_TEXT_EVENTS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CRAFTABLE_CORAL_BLOCKS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean VILLAGER_TRADING_OPTIMIZATION_ERROR_REPORTED = new AtomicBoolean(false);
    private static String version;

    @Override
    public void onInitialize() {
        version = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .orElseThrow(RuntimeException::new)
                .getMetadata()
                .getVersion()
                .getFriendlyString();
        CarpetServer.manageExtension(INSTANCE);
        CraftableCoralBlocksDataPackController.initialize();
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionEndPlatformSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionLowVersionSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CraftableCoralBlocksSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionFluidSettings.class);
        CarpetServer.settingsManager.registerRuleObserver((source, rule, userInput) -> {
            String ruleName = rule.name();
            if ("commandKillItem".equals(ruleName) || "commandMachineStatus".equals(ruleName)) {
                MinecraftServer server = source != null ? source.getServer() : CarpetServer.minecraft_server;
                if (server != null) {
                    CommandHelper.notifyPlayersCommandsChanged(server);
                }
                return;
            }
            if ("craftableCoralBlocks".equals(ruleName)) {
                MinecraftServer server = source != null ? source.getServer() : CarpetServer.minecraft_server;
                CraftableCoralBlocksDataPackController.onRuleChanged(server);
                return;
            }
            if ("waterFluidTickDelay".equals(ruleName) || "lavaFluidTickDelay".equals(ruleName)) {
                CarpetIceAdditionFluidSettings.refreshCachedValues();
                return;
            }
            if ("villagerTradingOptimization".equals(ruleName)) {
                VillagerTradingOptimizationRuleHelper.rebuildMismatchedVillagers(
                        source != null ? source.getServer() : CarpetServer.minecraft_server);
                return;
            }
            if (!"botTabListNamePrefix".equals(ruleName) && !"botTabListNameSuffix".equals(ruleName)) {
                return;
            }

            try {
                BotTabListNameHelper.refreshFakePlayerDisplayNames();
            } catch (Throwable throwable) {
                reportFeatureCompatibilityIssue("botTabListName", throwable);
            }
        });

        CarpetIceAdditionFluidSettings.refreshCachedValues();
    }

    @Override public void registerLoggers() { VillagerEventsLogger121.register(); }

    @Override
    public void onPlayerLoggedIn(ServerPlayerEntity player) {
        try {
            // 锁定后字段已被直接压成 false，不能再以字段值作为提示门槛：只要 conflictLocked 即提示加入玩家
            if (CraftableCoralBlocksState.isConflictLocked()) {
                player.sendMessage(net.minecraft.text.Text.literal(
                        com.ice2974.carpeticeaddition.translation.TranslationFormatUtil.translate(
                                "carpet.rule.craftableCoralBlocks.conflict.locked")));
            }
            CraftableCoralBlocksDataPackController.onPlayerJoin(CarpetServer.minecraft_server, player);
        } catch (Throwable throwable) {
            reportFeatureCompatibilityIssue("craftableCoralBlocks", throwable);
        }
    }

    @Override
    public String version() {
        return version;
    }



    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext) {
        KillItemCommand.register(dispatcher);
        MachineStatusCommand.register(dispatcher);
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        KillItemConfigManager.initialize(server.getSavePath(WorldSavePath.ROOT));
        MachineStatusConfigManager.initialize(server.getSavePath(WorldSavePath.ROOT));
        VillagerEventsRuntime121.onServerLoaded(server);
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        // 在世界文件完全加载后检测冲突。1.21.1 检测器虽不依赖 overworld，但统一在此处检测可使
        // 三平台生命周期一致，且语义更准确（世界加载完成后检测）。onServerLoadedWorlds 注入
        // MinecraftServer.loadLevel 的 RETURN，overworld 与 RecipeManager 均已就绪，
        // integrated / dedicated server 均触发。此时通常无在线玩家，仅写日志；玩家加入时再提示。
        try {
            CraftableCoralBlocksDataPackController.onServerLoadedWorlds(server);
        } catch (Throwable throwable) {
            reportFeatureCompatibilityIssue("craftableCoralBlocks", throwable);
        }

        CarpetIceAdditionFluidSettings.refreshCachedValues();
        VillagerTradingOptimizationRuleHelper.rebuildMismatchedVillagers(server);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        VillagerEventsRuntime121.onServerClosed(server);
        KillItemConfigManager.shutdown();
        MachineStatusConfigManager.shutdown();
        try {
            CraftableCoralBlocksDataPackController.onServerClosed(server);
        } catch (Throwable throwable) {
            reportFeatureCompatibilityIssue("craftableCoralBlocks", throwable);
        }
    }

    public static void reportFeatureCompatibilityIssue(String featureName, Throwable throwable) {
        AtomicBoolean flag;
        if ("safeScaffoldingBreak".equals(featureName)) {
            flag = SAFE_SCAFFOLDING_BREAK_ERROR_REPORTED;
        } else if ("crafterStopsWhenOutputBlocked".equals(featureName)) {
            flag = CRAFTER_OUTPUT_RULE_ERROR_REPORTED;
        } else if ("recordWorldEventFix".equals(featureName)) {
            flag = RECORD_WORLD_EVENT_FIX_ERROR_REPORTED;
        } else if ("spawnersIgnoreInvisiblePlayers".equals(featureName)) {
            flag = SPAWNERS_IGNORE_INVISIBLE_PLAYERS_ERROR_REPORTED;
        } else if ("disableKelpNaturalGrowth".equals(featureName)) {
            flag = DISABLE_KELP_NATURAL_GROWTH_ERROR_REPORTED;
        } else if ("disableAmethystGrowth".equals(featureName)) {
            flag = DISABLE_AMETHYST_GROWTH_ERROR_REPORTED;
        } else if ("silkTouchBuddingAmethyst".equals(featureName)) {
            flag = CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED;
        } else if ("silkTouchFrostedIce".equals(featureName)) {
            flag = CAN_MINE_FROSTED_ICE_ERROR_REPORTED;
        } else if ("frostedIceProperToolFix".equals(featureName)) {
            flag = FROSTED_ICE_PROPER_TOOL_FIX_ERROR_REPORTED;
        } else if ("beaconProperToolFix".equals(featureName)) {
            flag = BEACON_PROPER_TOOL_FIX_ERROR_REPORTED;
        } else if ("iceLikeMagmaBlocks".equals(featureName)) {
            flag = ICE_LIKE_MAGMA_BLOCKS_ERROR_REPORTED;
        } else if ("disableNyliumDecay".equals(featureName)) {
            flag = DISABLE_NYLIUM_DECAY_ERROR_REPORTED;
        } else if ("botTabListName".equals(featureName)
                || "botTabListNamePrefix".equals(featureName)
                || "botTabListNameSuffix".equals(featureName)) {
            flag = BOT_TAB_LIST_NAME_ERROR_REPORTED;
        } else if ("disablePlayerAttackingTamedMobs".equals(featureName)) {
            flag = DISABLE_PLAYER_ATTACKING_TAMED_MOBS_ERROR_REPORTED;
        } else if ("fakePlayerIgnoreThornsDamage".equals(featureName)) {
            flag = FAKE_PLAYER_IGNORE_THORNS_DAMAGE_ERROR_REPORTED;
        } else if ("easyWaterloggedBlockPlacement".equals(featureName)) {
            flag = EASY_WATERLOGGED_BLOCK_PLACEMENT_ERROR_REPORTED;
        } else if ("phantomSpawnWarning".equals(featureName)) {
            flag = PHANTOM_SPAWN_WARNING_ERROR_REPORTED;
        } else if ("neutralPhantoms".equals(featureName)) {
            flag = NEUTRAL_PHANTOMS_ERROR_REPORTED;
        } else if ("killitemTextEvents".equals(featureName)) {
            flag = KILLITEM_TEXT_EVENTS_ERROR_REPORTED;
        } else if ("craftableCoralBlocks".equals(featureName)) {
            flag = CRAFTABLE_CORAL_BLOCKS_ERROR_REPORTED;
        } else if ("villagerTradingOptimization".equals(featureName)) {
            flag = VILLAGER_TRADING_OPTIMIZATION_ERROR_REPORTED;
        } else {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}: {}", featureName, throwable.toString());
            return;
        }

        if (flag.compareAndSet(false, true)) {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}. Feature will be safely skipped. Cause: {}",
                    featureName, throwable.toString());
        }
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return CarpetIceAdditionTranslations.get(lang);
    }
}
