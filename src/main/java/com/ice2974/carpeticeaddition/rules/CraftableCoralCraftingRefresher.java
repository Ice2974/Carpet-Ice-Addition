package com.ice2974.carpeticeaddition.rules;

/**
 * Duck-typing 接口：由各版本 3×3 合成菜单（yarn {@code CraftingScreenHandler} /
 * mojmap {@code CraftingMenu}）的 Mixin 实现，用于在规则切换时强制重算结果槽。
 *
 * <p>珊瑚块配方为 3×3，玩家只能在 3×3 工作台合成，故只需覆盖 3×3 菜单，
 * 无需处理 2×2 背包合成区。
 *
 * <p>放在 common（不依赖 Minecraft 类），各版本 Mixin 通过 {@code implements} 接入。
 */
public interface CraftableCoralCraftingRefresher {
    /**
     * 强制重算当前合成菜单的结果槽。服务端调用有效，重算后由 vanilla 自行同步给客户端。
     */
    void carpetIceAddition$refreshCraftingResult();
}
