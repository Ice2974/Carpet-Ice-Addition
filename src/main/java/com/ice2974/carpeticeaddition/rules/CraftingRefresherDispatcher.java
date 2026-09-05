package com.ice2974.carpeticeaddition.rules;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 规则切换时强制重算在线玩家当前打开的 3×3 合成菜单结果槽。
 *
 * <p>珊瑚块配方为 3×3，玩家只能在 3×3 工作台合成，故只需刷新 {@code CraftingScreenHandler}
 * （yarn）/ {@code CraftingMenu}（mojmap）。通过 {@link CraftableCoralCraftingRefresher} duck 接口
 * 识别目标菜单，避免依赖具体版本类名。
 */
public final class CraftingRefresherDispatcher {
    private CraftingRefresherDispatcher() {
    }

    public static void refreshOpenCraftingMenu(Player player) {
        AbstractContainerMenu handler = player.containerMenu;
        if (handler instanceof CraftableCoralCraftingRefresher refresher) {
            try {
                refresher.carpetIceAddition$refreshCraftingResult();
            } catch (Throwable ignored) {
                // 刷新失败不影响配方书同步主流程
            }
        }
    }
}
