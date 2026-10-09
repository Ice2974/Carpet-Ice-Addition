package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.StonecutterRecipeRefresher;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 切石机菜单刷新 Mixin（全部受支持版本共用，无需 per-version override）。
 *
 * <p>{@code StonecutterMenu.container}（public final）、{@code slotsChanged(Container)}（public）、
 * {@code INPUT_SLOT} / {@code RESULT_SLOT}（public static final）与缓存字段 {@code input} 在 1.21.1 ~ 26.3
 * 全部版本同名同类型，因此单一 root 实现即可覆盖 12 个平台。
 *
 * <p>为什么必须让守卫必然通过：{@code slotsChanged(Container)} 的实体是
 * {@code if (!stack.is(this.input.getItem())) { this.input = stack.copy(); this.setupRecipeList(stack); }} ——
 * 只比较 Item，玩家已放入滴水石块（物品未变）时调用它是空操作，陈旧配方缓存不会被重建。
 * 因此先清空缓存字段，再由 vanilla 自己完成「写回快照 + 重建列表 + 重置索引 + 清空结果槽」。
 *
 * <p>空输入槽分支：此时槽内为空、缓存也为空，{@code EMPTY.is(AIR)} 为真 ⇒ 守卫不成立、不会重建；
 * 而空输入下列表本就已在「物品被移出」时被重建为空、索引已置 -1、结果槽已清空。此处仍显式清一次结果槽，
 * 使「无残留」不依赖 vanilla 不变量。
 */
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuCalciteStonecuttingRecipeMixin implements StonecutterRecipeRefresher {
    @Shadow
    private ItemStack input;

    @Override
    public void carpetIceAddition$refreshStonecutterRecipes() {
        StonecutterMenu self = (StonecutterMenu) (Object) this;
        ItemStack inSlot = self.getSlot(StonecutterMenu.INPUT_SLOT).getItem();
        if (inSlot.isEmpty()) {
            self.getSlot(StonecutterMenu.RESULT_SLOT).set(ItemStack.EMPTY);
            return;
        }
        this.input = ItemStack.EMPTY;
        self.slotsChanged(self.container);
    }

    @Override
    public void carpetIceAddition$clearStonecutterResult() {
        StonecutterMenu self = (StonecutterMenu) (Object) this;
        self.getSlot(StonecutterMenu.RESULT_SLOT).set(ItemStack.EMPTY);
    }
}
