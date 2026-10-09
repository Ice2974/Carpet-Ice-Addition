package com.ice2974.carpeticeaddition.rules;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 规则切换 / 资源 reload 后重置在线玩家当前打开的切石机菜单。
 *
 * <p>通过 {@link StonecutterRecipeRefresher} duck 接口识别目标菜单，避免依赖具体版本类名，
 * 与既有的 {@code CraftingRefresherDispatcher} 同构。
 */
public final class StonecutterRefresherDispatcher {
    private StonecutterRefresherDispatcher() {
    }

    /** 强制重建已打开切石机菜单的配方列表（重置选中索引与结果槽）。 */
    public static void refreshOpenStonecutterMenu(Player player) {
        AbstractContainerMenu handler = player.containerMenu;
        if (handler instanceof StonecutterRecipeRefresher refresher) {
            try {
                refresher.carpetIceAddition$refreshStonecutterRecipes();
            } catch (Throwable ignored) {
                // 刷新失败不影响主流程
            }
        }
    }

    /** 仅清空已打开切石机菜单的结果槽。 */
    public static void clearOpenStonecutterResult(Player player) {
        AbstractContainerMenu handler = player.containerMenu;
        if (handler instanceof StonecutterRecipeRefresher refresher) {
            try {
                refresher.carpetIceAddition$clearStonecutterResult();
            } catch (Throwable ignored) {
                // 刷新失败不影响主流程
            }
        }
    }
}
