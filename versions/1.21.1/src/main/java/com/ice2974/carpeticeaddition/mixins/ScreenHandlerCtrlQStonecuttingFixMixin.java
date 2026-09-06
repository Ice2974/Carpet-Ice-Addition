package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionLowVersionSettings;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ctrlQStonecuttingFix：backport 原版 1.21.2 对 ScreenHandler THROW 分支的循环修复，
 * 使切石机输出槽（handler index 1）可用 Ctrl+Q 一次丢出全部可生产产物。
 * 1.21.2 循环中的 canDropItems / dropCreativeStack 在逻辑服务端分别为恒真与空操作，
 * 且 1.21.1 中不存在这两个方法，故此处省略。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerCtrlQStonecuttingFixMixin {
    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Shadow
    public abstract ItemStack getCarried();

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$ctrlQStonecuttingFix(int slotIndex, int button, ClickType actionType,
                                                        Player player, CallbackInfo ci) {
        // 仅逻辑服务端执行：客户端预测保持 1.21/1.21.1 原版，纯原版客户端也能正常同步
        if (player.level().isClientSide()) {
            return;
        }
        // 只接管切石机输出槽的 Ctrl+Q；其余点击（普通 Q、Shift 点击、左右键、拖拽）走原版路径
        if (!CarpetIceAdditionLowVersionSettings.ctrlQStonecuttingFix
                || actionType != ClickType.THROW
                || button != 1
                || slotIndex != 1
                || !this.getCarried().isEmpty()
                || !((Object) this instanceof StonecutterMenu)) {
            return;
        }

        Slot slot = this.slots.get(slotIndex);
        int j = slot.getItem().getCount();
        ItemStack itemStack = slot.safeTake(j, Integer.MAX_VALUE, player);
        player.drop(itemStack, true);
        while (!itemStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), itemStack)) {
            itemStack = slot.safeTake(j, Integer.MAX_VALUE, player);
            player.drop(itemStack, true);
        }
        ci.cancel();
    }
}
