package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralCraftingRefresher;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerCraftableCoralBlocksMixin implements CraftableCoralCraftingRefresher {
    @Shadow @Final private RecipeInputInventory input;
    @Shadow @Final private CraftingResultInventory result;
    @Shadow @Final private ScreenHandlerContext context;
    @Shadow @Final private PlayerEntity player;

    @Override
    public void carpetIceAddition$refreshCraftingResult() {
        context.run((world, pos) -> {
            if (world instanceof ServerWorld serverWorld) {
                CraftingScreenHandler self = (CraftingScreenHandler) (Object) this;
                RecipeEntry<?> last = result.getLastRecipe();
                @SuppressWarnings("unchecked")
                RecipeEntry<CraftingRecipe> lastCrafting = last == null ? null : (RecipeEntry<CraftingRecipe>) last;
                invokeUpdateResult(self, serverWorld, player, input, result, lastCrafting);
            }
        });
    }

    @org.spongepowered.asm.mixin.gen.Invoker("updateResult")
    private static native void invokeUpdateResult(
            ScreenHandler handler,
            ServerWorld world,
            PlayerEntity playerEntity,
            RecipeInputInventory craftingInventory,
            CraftingResultInventory resultInventory,
            RecipeEntry<CraftingRecipe> lastRecipe);
}
