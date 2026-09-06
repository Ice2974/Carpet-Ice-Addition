package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralCraftingRefresher;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 3×3 工作台刷新 mixin（1.21.1）。
 *
 * <p>规则切换时强制重算结果槽：直接调用 vanilla public {@link CraftingMenu#slotsChanged}，
 * 让 vanilla 自行走完 {@code updateResult} 流程（B2 过滤在此生效，结果槽被清空/填充并由 vanilla 发包同步客户端）。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuCraftableCoralBlocksMixin implements CraftableCoralCraftingRefresher {
    @Shadow @Final private CraftingContainer craftSlots;

    @Override
    public void carpetIceAddition$refreshCraftingResult() {
        CraftingMenu self = (CraftingMenu) (Object) this;
        self.slotsChanged(craftSlots);
    }
}
