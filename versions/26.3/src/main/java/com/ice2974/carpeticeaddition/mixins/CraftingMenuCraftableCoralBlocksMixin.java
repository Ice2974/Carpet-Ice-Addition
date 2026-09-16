package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralCraftingRefresher;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 3×3 工作台刷新 mixin（26.x）。
 *
 * <p>规则切换时强制重算结果槽：直接调用 vanilla public {@link CraftingMenu#slotsChanged}，
 * 让 vanilla 自行走完 {@code slotChangedCraftingGrid} 流程（B2 过滤在此生效，结果槽被清空/填充并由 vanilla 发包同步客户端）。
 *
 * <p>不 {@code @Shadow} 任何字段：{@code craftSlots} 位于父类 {@code AbstractCraftingMenu}，
 * 运行时 mixin target（CraftingMenu）自身不持有该字段、且项目未生成 refmap，会触发
 * {@code @Shadow field ... was not located} 崩溃。改为通过 vanilla public
 * {@code getInputGridSlots()} 取输入槽、再取其 {@code container} 作为 {@code slotsChanged} 参数。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuCraftableCoralBlocksMixin implements CraftableCoralCraftingRefresher {
    @Override
    public void carpetIceAddition$refreshCraftingResult() {
        CraftingMenu self = (CraftingMenu) (Object) this;
        Container craftingContainer = self.getInputGridSlots().isEmpty() ? null : self.getInputGridSlots().get(0).container;
        if (craftingContainer != null) {
            self.slotsChanged(craftingContainer);
        }
    }
}
