//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralCraftingRefresher;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 3×3 工作台刷新 mixin（1.21.3~1.21.11）。
 *
 * <p>规则切换时强制重算结果槽：直接调用 vanilla public {@link CraftingMenu#slotsChanged}，
 * 让 vanilla 自行走完 {@code updateResult} 流程（B2 过滤在此生效，结果槽被清空/填充并由 vanilla 发包同步客户端）。
 *
 * <p>不 {@code @Shadow} 任何字段：输入栏位于父类 {@code AbstractCraftingScreenHandler}（1.21.3+），
 * 不同版本 {@code @Shadow} 引用的 intermediate 字段（field_52559）在运行时 mixin target（class_1714
 * CraftingScreenHandler）自身不存在、且项目未生成 refmap，会触发
 * {@code @Shadow field ... was not located} 崩溃。改为通过 vanilla public
 * {@code getInputSlots()} 取输入槽、再取其 {@code inventory} 作为 {@code onContentChanged} 参数。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingScreenHandlerCraftableCoralBlocksMixin implements CraftableCoralCraftingRefresher {
    @Override
    public void carpetIceAddition$refreshCraftingResult() {
        CraftingMenu self = (CraftingMenu) (Object) this;
        Container craftingInventory = self.getInputGridSlots().isEmpty() ? null : self.getInputGridSlots().get(0).container;
        if (craftingInventory != null) {
            self.slotsChanged(craftingInventory);
        }
    }
}
//#endif
