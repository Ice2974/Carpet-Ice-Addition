package com.ice2974.carpeticeaddition.rules;

/**
 * Duck-typing 接口：由各版本切石机菜单（{@code StonecutterMenu}）的 Mixin 实现，
 * 供 {@code calciteStonecuttingRecipe} 规则在规则切换 / 资源 reload 后重置已打开菜单的配方缓存。
 *
 * <p>放在 common（不依赖 Minecraft 类），平台 Mixin 通过 {@code implements} 接入。
 *
 * <p>为什么必须显式刷新：{@code StonecutterMenu.slotsChanged(Container)} 带「输入物品未变则不重建」守卫
 * （只比较 Item），因此玩家已放入滴水石块时调用它对配方缓存是空操作，必须由调用方先让守卫必然通过。
 */
public interface StonecutterRecipeRefresher {
    /**
     * 强制重建当前切石机菜单的配方列表，并重置选中索引与结果槽（服务端调用有效，
     * 重算后由 vanilla 的容器同步下发给客户端）。
     */
    void carpetIceAddition$refreshStonecutterRecipes();

    /**
     * 仅清空结果槽，用于内置包被取消选中的瞬间掐断「已算好的结果 + pending 取出」。
     */
    void carpetIceAddition$clearStonecutterResult();
}
