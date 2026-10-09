package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code calciteStonecuttingRecipe} 的菜单同步器。
 *
 * <p>切石配方**没有**独立的原版配方书界面（{@code StonecutterScreen} 不含 {@code RecipeBookComponent}，
 * 也不存在切石机配方书组件；切石机可用列表来自 {@code Level.recipeAccess().stonecutterRecipes()}，
 * 与玩家配方书无关）。因此本规则不做 {@code awardRecipes} / {@code resetRecipes}，只做两件事：
 * <ol>
 *   <li>{@link #onReload(MinecraftServer)}：静默点同步——重建在线玩家已打开切石机菜单的配方列表，
 *       使规则启停 / 冲突锁定迁移 / {@code /reload} 后菜单与服务端配方管理器一致；</li>
 *   <li>{@link #onPackDisabled(MinecraftServer)}：内置包被取消选中的**即时清理**——立刻清空结果槽，
 *       掐断已算好的结果与 pending 取出（该时序与既有 {@code craftableCoralBlocks} 的
 *       {@code onPackDisable} 一致，不得因协调器迁移而退化）。</li>
 * </ol>
 *
 * <p><b>失败上报</b>：逐玩家动作经 {@link IsolatedHookRunner} 执行——单个玩家失败不影响其它玩家，
 * 但只要有一条失败就抛出聚合异常，由协调器的钩子失败路径记账
 * （{@code safeRun} → 兼容性上报一次 → {@code RecipeReloadGate.onPassFinished(false)}：
 * 重新欠一次 pass 且不得报告 {@code ready}）。既不能吞掉失败，也不额外写日志造成刷屏。
 */
public final class CalciteStonecuttingMenuSyncHelper {
    private static final String SYNC_DESCRIPTION = "calciteStonecuttingRecipe: stonecutter menu sync";
    private static final String CLEANUP_DESCRIPTION = "calciteStonecuttingRecipe: stonecutter result cleanup";

    private CalciteStonecuttingMenuSyncHelper() {
    }

    /** 静默点同步：重建全部在线玩家已打开切石机菜单的配方列表与结果槽。 */
    public static void onReload(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        List<Runnable> refreshes = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshes.add(() -> StonecutterRefresherDispatcher.refreshOpenStonecutterMenu(player));
        }
        throwIfFailed(IsolatedHookRunner.runAll(refreshes), SYNC_DESCRIPTION);
    }

    /** 内置包被取消选中时的即时清理。 */
    public static void onPackDisabled(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        List<Runnable> cleanups = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            cleanups.add(() -> StonecutterRefresherDispatcher.clearOpenStonecutterResult(player));
        }
        throwIfFailed(IsolatedHookRunner.runAll(cleanups), CLEANUP_DESCRIPTION);
    }

    /** 单玩家异常已被隔离；只要存在失败就抛出聚合异常，交给协调器记账（不在此处写日志）。 */
    private static void throwIfFailed(IsolatedHookRunner.Result result, String description) {
        if (!result.ok()) {
            throw result.aggregatedException(description);
        }
    }
}
