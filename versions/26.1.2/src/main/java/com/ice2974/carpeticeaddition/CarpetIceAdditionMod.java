package com.ice2974.carpeticeaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.command.KillItemCommand;
import com.ice2974.carpeticeaddition.command.MachineStatusCommand;
import com.ice2974.carpeticeaddition.rules.BetterTridentDespawnEpoch;
import com.ice2974.carpeticeaddition.rules.BotTabListNameHelper;
import com.ice2974.carpeticeaddition.rules.DelayedJukeboxStartEventManager;
import com.ice2974.carpeticeaddition.rules.DrownedOceanRuinSpawns;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentRearmEpoch;
import com.ice2974.carpeticeaddition.rules.RecipeDatapackRegistry;
import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationRuleHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionEndPlatformSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionHighVersionSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionFluidSettings;
import com.ice2974.carpeticeaddition.translation.CarpetIceAdditionTranslations;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsLogger;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime;
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

import java.util.Map;

public final class CarpetIceAdditionMod implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "carpet-ice-addition";
    public static final String MOD_NAME = "Carpet Ice Addition";

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
        RecipeDatapackRegistry.initialize();
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionEndPlatformSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionHighVersionSettings.class);
        RecipeDatapackRegistry.parseSettings();
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionFluidSettings.class);
        try {
            DrownedOceanRuinSpawns.register();
        } catch (LinkageError | RuntimeException error) {
            reportFeatureCompatibilityIssue("drownedSpawningInOceanRuins", error);
        }
        CarpetServer.settingsManager.registerRuleObserver((source, rule, userInput) -> {
            String ruleName = rule.name();
            if ("commandKillItem".equals(ruleName) || "commandMachineStatus".equals(ruleName)) {
                MinecraftServer server = source != null ? source.getServer() : CarpetServer.minecraft_server;
                if (server != null) {
                    CommandHelper.notifyPlayersCommandsChanged(server);
                }
                return;
            }
            if (RecipeDatapackRegistry.handleRuleChanged(
                    ruleName, source != null ? source.getServer() : CarpetServer.minecraft_server)) {
                return;
            }
            if ("waterFluidTickDelay".equals(ruleName) || "lavaFluidTickDelay".equals(ruleName)) {
                CarpetIceAdditionFluidSettings.refreshCachedValues();
                return;
            }
            if ("recordWorldEventFix".equals(ruleName)) {
                // 关闭瞬间即建立生命周期边界：pending 不得跨一次 false 状态存活，
                // 否则 true→false→true 同 tick 切换后旧 pending 会在重新开启后被补发。
                if (!CarpetIceAdditionSettings.recordWorldEventFix) {
                    DelayedJukeboxStartEventManager.clearAll();
                }
                return;
            }
            if ("enhancedTrident".equals(ruleName)) {
                // 规则值变化即推进 grounded-rearm 代际：true→false→true 在实体下一
                // tick 前快速完成时，旧授予的资格不得在重新开启后复活
                EnhancedTridentRearmEpoch.advance();
                return;
            }
            if ("betterTridentDespawnCondition".equals(ruleName)) {
                // 规则值变化即推进静止计时代际：规则关闭期间实体可能被移动而未被
                // 采样，旧累积的静止计时不得在重新开启后复活
                BetterTridentDespawnEpoch.advance();
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

    @Override public void registerLoggers() { VillagerEventsLogger.register(); }

    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        try {
            // 锁定期提示与逐规则同步（含 coral 配方书；calcite 无配方书界面）统一由注册表处理。
            RecipeDatapackRegistry.onPlayerLoggedIn(player);
        } catch (Throwable throwable) {
            reportFeatureCompatibilityIssue("recipeDatapacks", throwable);
        }
        DeltaruneEasterEggs.onPlayerLoggedIn(player);
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
        VillagerEventsRuntime.onServerLoaded(server);
        DeltaruneEasterEggs.onServerLoaded();
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        // 在世界文件完全加载后检测冲突：Carpet onServerLoaded 注入 MinecraftServer.loadLevel 的 HEAD，
        // 此时 server.overworld() 仍为 null，检测器构造 SlotDisplayContext 需要非 null overworld，
        // 会在 onServerLoaded 中早返回 false 导致启动漏报。onServerLoadedWorlds 注入 loadLevel RETURN，
        // overworld 与 RecipeManager 均已就绪。integrated / dedicated server 均触发。
        try {
            // 触发首次静默点处理：冲突重算 + 菜单/配方书同步（即使无需 reload 也会消费 pendingSyncPass）
            RecipeDatapackRegistry.onServerLoadedWorlds(server);
        } catch (Throwable throwable) {
            reportFeatureCompatibilityIssue("recipeDatapacks", throwable);
        }

        CarpetIceAdditionFluidSettings.refreshCachedValues();
        VillagerTradingOptimizationRuleHelper.rebuildMismatchedVillagers(server);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        // 静态 pending map 不能跨 server 生命周期持有 ServerLevel 强引用。
        DelayedJukeboxStartEventManager.clearAll();
        VillagerEventsRuntime.onServerClosed(server);
        KillItemConfigManager.shutdown();
        MachineStatusConfigManager.shutdown();
        DeltaruneEasterEggs.onServerClosed();
        try {
            RecipeDatapackRegistry.onServerClosed(server);
        } catch (Throwable throwable) {
            reportFeatureCompatibilityIssue("recipeDatapacks", throwable);
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
