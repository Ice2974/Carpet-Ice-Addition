package com.ice2974.carpeticeaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.command.KillItemCommandMc261;
import com.ice2974.carpeticeaddition.command.MachineStatusCommandMc261;
import com.ice2974.carpeticeaddition.rules.BotTabListNameHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionEndPlatformSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionHighVersionSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.CarpetIceAdditionTranslations;
import com.ice2974.carpeticeaddition.command.KillItemConfigManager;
import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
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
    private static final AtomicBoolean CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CAN_MINE_FROSTED_ICE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean ICE_LIKE_MAGMA_BLOCKS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_NYLIUM_DECAY_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean BOT_TAB_LIST_NAME_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_PLAYER_ATTACKING_TAMED_MOBS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean FAKE_PLAYER_IGNORE_THORNS_DAMAGE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean EASY_WATERLOGGED_BLOCK_PLACEMENT_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean PHANTOM_SPAWN_WARNING_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean NEUTRAL_PHANTOMS_ERROR_REPORTED = new AtomicBoolean(false);
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
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionEndPlatformSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionHighVersionSettings.class);
        CarpetServer.settingsManager.registerRuleObserver((source, rule, userInput) -> {
            String ruleName = rule.name();
            if ("commandKillItem".equals(ruleName) || "commandMachineStatus".equals(ruleName)) {
                MinecraftServer server = source != null ? source.getServer() : CarpetServer.minecraft_server;
                if (server != null) {
                    CommandHelper.notifyPlayersCommandsChanged(server);
                }
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
    }

    @Override
    public String version() {
        return version;
    }



    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        KillItemCommandMc261.register(dispatcher);
        MachineStatusCommandMc261.register(dispatcher);
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        KillItemConfigManager.initialize(server.getWorldPath(LevelResource.ROOT));
        MachineStatusConfigManager.initialize(server.getWorldPath(LevelResource.ROOT));
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        KillItemConfigManager.shutdown();
        MachineStatusConfigManager.shutdown();
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
        } else if ("silkTouchBuddingAmethyst".equals(featureName)) {
            flag = CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED;
        } else if ("silkTouchFrostedIce".equals(featureName)) {
            flag = CAN_MINE_FROSTED_ICE_ERROR_REPORTED;
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
