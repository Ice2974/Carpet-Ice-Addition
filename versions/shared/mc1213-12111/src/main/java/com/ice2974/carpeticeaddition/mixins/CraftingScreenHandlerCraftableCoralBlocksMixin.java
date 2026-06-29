package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralCraftingRefresher;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 3×3 工作台刷新 mixin（1.21.3~1.21.11）。
 *
 * <p>规则切换时强制重算结果槽：直接调用 vanilla public {@link CraftingScreenHandler#onContentChanged}，
 * 让 vanilla 自行走完 {@code updateResult} 流程（B2 过滤在此生效，结果槽被清空/填充并由 vanilla 发包同步客户端）。
 *
 * <p>1.21.3+ 的输入/结果栏位于父类 {@code AbstractCraftingScreenHandler}（{@code craftingInventory} /
 * {@code craftingResultInventory}，protected final）。这里只 shadow 输入栏作为 {@code onContentChanged} 的参数，
 * 避免依赖具体内部状态。
 */
@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerCraftableCoralBlocksMixin implements CraftableCoralCraftingRefresher {
    @Shadow @Final protected RecipeInputInventory craftingInventory;

    @Override
    public void carpetIceAddition$refreshCraftingResult() {
        CraftingScreenHandler self = (CraftingScreenHandler) (Object) this;
        self.onContentChanged(craftingInventory);
    }
}
