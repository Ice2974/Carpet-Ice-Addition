package com.ice2974.carpeticeaddition.rules;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings;
import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 配方类内置数据包规则的注册表与生命周期入口。
 *
 * <p>入口类（各平台 {@code CarpetIceAdditionMod}）只与本类交互，规则自身的启停、冲突锁定、
 * 资源 reload、配方书 / 菜单同步全部由 {@link RecipePackCoordinator} 统一承担。
 * 新增一条配方类规则只需在此登记一个 {@link RecipePackCoordinator.ManagedPack}。
 */
public final class RecipeDatapackRegistry {
    /** {@code craftableCoralBlocks} 的内置数据包 id。 */
    public static final Identifier CRAFTABLE_CORAL_BLOCKS_PACK =
            Identifier.tryParse("carpet-ice-addition:craftable_coral_blocks");

    /** {@code calciteStonecuttingRecipe} 的内置数据包 id。 */
    public static final Identifier CALCITE_STONECUTTING_PACK =
            Identifier.tryParse("carpet-ice-addition:calcite_stonecutting");

    private RecipeDatapackRegistry() {
    }

    /** 注册全部受管内置数据包并挂载唯一的 reload 监听器（在 {@code onInitialize} 中调用）。 */
    public static void initialize() {
        RecipePackCoordinator.register(coralPack());
        RecipePackCoordinator.register(calcitePack());
        RecipePackCoordinator.initialize();
    }

    /** 解析全部规则定义类（在 {@code onGameStarted} 中调用）。 */
    public static void parseSettings() {
        CarpetServer.settingsManager.parseSettingsClass(CraftableCoralBlocksSettings.class);
        CarpetServer.settingsManager.parseSettingsClass(CalciteStonecuttingRecipeSettings.class);
    }

    /**
     * 规则值变化入口（在 rule observer 中调用）。
     *
     * @return true 表示该规则名由本注册表接管
     */
    public static boolean handleRuleChanged(String ruleName, MinecraftServer server) {
        return RecipePackCoordinator.onRuleChanged(ruleName, server);
    }

    /** 玩家登录：锁定期提示 + （本包已收敛时）逐包同步。 */
    public static void onPlayerLoggedIn(ServerPlayer player) {
        RecipePackCoordinator.onPlayerLoggedIn(player);
    }

    /** 世界文件加载完成、RecipeManager 就绪：触发首次静默点处理。 */
    public static void onServerLoadedWorlds(MinecraftServer server) {
        RecipePackCoordinator.onServerLoadedWorlds(server);
    }

    /** 服务器关闭：复位锁状态与协调器状态。 */
    public static void onServerClosed(MinecraftServer server) {
        RecipePackCoordinator.onServerClosed(server);
    }

    private static RecipePackCoordinator.ManagedPack coralPack() {
        return new RecipePackCoordinator.ManagedPack(
                CRAFTABLE_CORAL_BLOCKS_PACK,
                "craftableCoralBlocks",
                "carpet.rule.craftableCoralBlocks.conflict.locked",
                CraftableCoralBlocksState::isConflictLocked,
                CraftableCoralBlocksSettings::effective,
                CraftableCoralBlocksConflictDetector::recomputeAndNotify,
                CraftableCoralBlocksRecipeBookHelper::onReload,
                CraftableCoralBlocksRecipeBookHelper::onPackDisable,
                CraftableCoralBlocksRecipeBookHelper::onPlayerJoin,
                () -> {
                    CraftableCoralBlocksState.setConflictLocked(false);
                    CraftableCoralBlocksState.setDesiredValue(null);
                });
    }

    private static RecipePackCoordinator.ManagedPack calcitePack() {
        return new RecipePackCoordinator.ManagedPack(
                CALCITE_STONECUTTING_PACK,
                "calciteStonecuttingRecipe",
                "carpet.rule.calciteStonecuttingRecipe.conflict.locked",
                CalciteStonecuttingRecipeState::isConflictLocked,
                CalciteStonecuttingRecipeSettings::effective,
                CalciteStonecuttingRecipeConflictDetector::recomputeAndNotify,
                CalciteStonecuttingMenuSyncHelper::onReload,
                CalciteStonecuttingMenuSyncHelper::onPackDisabled,
                // 登录补齐解锁状态（切石配方没有配方书界面，但解锁提示由 RecipeBookAdd 包驱动）；
                // 是否真的授予由「本包已收敛 + 规则生效 + 本包已选中 + 配方可解析」共同决定，
                // 通用锁定提示仍由协调器统一处理。
                CalciteStonecuttingMenuSyncHelper::onPlayerJoin,
                () -> {
                    CalciteStonecuttingRecipeState.setConflictLocked(false);
                    CalciteStonecuttingRecipeState.setDesiredValue(null);
                });
    }
}
