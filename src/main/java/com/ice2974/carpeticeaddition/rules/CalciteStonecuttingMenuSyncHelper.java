package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
 */
public final class CalciteStonecuttingMenuSyncHelper {
    private CalciteStonecuttingMenuSyncHelper() {
    }

    /** 静默点同步：重建全部在线玩家已打开切石机菜单的配方列表与结果槽。 */
    public static void onReload(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            StonecutterRefresherDispatcher.refreshOpenStonecutterMenu(player);
        }
    }

    /** 内置包被取消选中时的即时清理。 */
    public static void onPackDisabled(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            StonecutterRefresherDispatcher.clearOpenStonecutterResult(player);
        }
    }
}
