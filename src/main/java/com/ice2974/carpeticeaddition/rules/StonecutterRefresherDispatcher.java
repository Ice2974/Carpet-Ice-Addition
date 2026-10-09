package com.ice2974.carpeticeaddition.rules;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 规则切换 / 资源 reload 后重置在线玩家当前打开的切石机菜单。
 *
 * <p>通过 {@link StonecutterRecipeRefresher} duck 接口识别目标菜单，避免依赖具体版本类名。
 *
 * <p><b>异常不在此处吞掉</b>：逐玩家调用必须经 {@link IsolatedHookRunner} 执行——由它完成单玩家
 * 异常隔离，并把失败聚合成一个异常上报给协调器（{@code runPass} → {@link RecipeReloadGate}）。
 * 若在此处 {@code catch (Throwable ignored)}，实际失败的菜单同步会被上层当成成功，
 * 结果是误报 {@code ready} 且不再重新欠 pass、没有任何诊断。
 *
 * <p>不对称说明：珊瑚侧的 {@link CraftingRefresherDispatcher} 仍保留既有的静默行为
 * （本次不改动珊瑚语义），因此两个 dispatcher 的异常语义目前不同。
 */
public final class StonecutterRefresherDispatcher {
    private StonecutterRefresherDispatcher() {
    }

    /** 强制重建已打开切石机菜单的配方列表（重置选中索引与结果槽）；异常交由调用方的隔离器处理。 */
    public static void refreshOpenStonecutterMenu(Player player) {
        AbstractContainerMenu handler = player.containerMenu;
        if (handler instanceof StonecutterRecipeRefresher refresher) {
            refresher.carpetIceAddition$refreshStonecutterRecipes();
        }
    }

    /** 仅清空已打开切石机菜单的结果槽；异常交由调用方的隔离器处理。 */
    public static void clearOpenStonecutterResult(Player player) {
        AbstractContainerMenu handler = player.containerMenu;
        if (handler instanceof StonecutterRecipeRefresher refresher) {
            refresher.carpetIceAddition$clearStonecutterResult();
        }
    }
}
