package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralCraftingRefresher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuCraftableCoralBlocksMixin implements CraftableCoralCraftingRefresher {
    @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private ResultContainer resultSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private Player player;

    @Override
    public void carpetIceAddition$refreshCraftingResult() {
        access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                CraftingMenu self = (CraftingMenu) (Object) this;
                RecipeHolder<?> used = resultSlots.getRecipeUsed();
                @SuppressWarnings("unchecked")
                RecipeHolder<CraftingRecipe> usedCrafting = used == null ? null : (RecipeHolder<CraftingRecipe>) used;
                invokeSlotChangedCraftingGrid((AbstractContainerMenu) self, serverLevel, player, craftSlots, resultSlots, usedCrafting);
            }
        });
    }

    @org.spongepowered.asm.mixin.gen.Invoker("slotChangedCraftingGrid")
    private static native void invokeSlotChangedCraftingGrid(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipeUsed);
}
