package com.ice2974.carpeticeaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.command.KillItemCommand;
import com.ice2974.carpeticeaddition.command.MachineStatusCommand;
import com.ice2974.carpeticeaddition.rules.BotTabListNameHelper;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksDataPackController;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksState;
import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationRuleHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionEndPlatformSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionHighVersionSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import com.ice2974.carpeticeaddition.translation.CarpetIceAdditionTranslations;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsLogger26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime26;
import com.ice2974.carpeticeaddition.command.KillItemConfigManager;
import com.ice2974.carpeticeaddition.command.MachineStatusConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class CarpetIceAdditionMod implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "carpet-ice-addition";
    public static final String MOD_NAME = "Carpet Ice Addition";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    private static final CarpetIceAdditionMod INSTANCE = new CarpetIceAdditionMod();
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
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionHighVersionSettings.class);
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

    @Override public void registerLoggers() { VillagerEventsLogger26.register(); }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        try {
            // 锁定后字段已被直接压成 false，不能再以字段值作为提示门槛：只要 conflictLocked 即提示加入玩家
            if (CraftableCoralBlocksState.isConflictLocked()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
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
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        KillItemCommand.register(dispatcher);
        MachineStatusCommand.register(dispatcher);
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        KillItemConfigManager.initialize(server.getWorldPath(LevelResource.ROOT));
        MachineStatusConfigManager.initialize(server.getWorldPath(LevelResource.ROOT));
        VillagerEventsRuntime26.onServerLoaded(server);
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        // 在世界文件完全加载后检测冲突：Carpet onServerLoaded 注入 MinecraftServer.loadLevel 的 HEAD，
        // 此时 server.overworld() 仍为 null，检测器构造 SlotDisplayContext 需要非 null overworld，
        // 会在 onServerLoaded 中早返回 false 导致启动漏报。onServerLoadedWorlds 注入 loadLevel RETURN，
        // overworld 与 RecipeManager 均已就绪。integrated / dedicated server 均触发。
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
        VillagerEventsRuntime26.onServerClosed(server);
        KillItemConfigManager.shutdown();
        MachineStatusConfigManager.shutdown();
        try {
            CraftableCoralBlocksDataPackController.onServerClosed(server);
        } catch (Throwable throwable) {
            reportFeatureCompatibilityIssue("craftableCoralBlocks", throwable);
        }
    }

    public static void reportFeatureCompatibilityIssue(String featureName, Throwable throwable) {
        FeatureCompatibilityReporter.reportFeatureCompatibilityIssue(featureName, throwable);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return CarpetIceAdditionTranslations.get(lang);
    }
}
