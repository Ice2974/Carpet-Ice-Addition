package com.ice2974.carpeticeaddition.rules;

import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

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
        if (player instanceof ServerPlayer serverPlayer) {
            refreshOpenStonecutterMenu(player.containerMenu, serverPlayer.connection::send);
        }
    }

    static void refreshOpenStonecutterMenu(AbstractContainerMenu handler,
                                         Consumer<ClientboundBundlePacket> sendBundle) {
        if (handler instanceof StonecutterRecipeRefresher refresher) {
            refresher.carpetIceAddition$refreshStonecutterRecipes();
            // 原版客户端也按输入 Item 类型缓存配方；相同物品的全量同步不会重建缓存。
            // 在同一 bundle 中清空并恢复客户端输入，避免网络分片暴露可操作的临时空槽。
            // 服务端物品不移动；随后全量同步真实槽位/光标/选中索引并更新 remote 缓存。
            if (handler.getSlot(StonecutterMenu.INPUT_SLOT).hasItem()) {
                sendBundle.accept(new ClientboundBundlePacket(List.of(
                        new ClientboundContainerSetSlotPacket(handler.containerId, handler.incrementStateId(),
                                StonecutterMenu.INPUT_SLOT, ItemStack.EMPTY),
                        new ClientboundContainerSetSlotPacket(handler.containerId, handler.incrementStateId(),
                                StonecutterMenu.INPUT_SLOT, handler.getSlot(StonecutterMenu.INPUT_SLOT).getItem()))));
            }
            handler.sendAllDataToRemote();
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
