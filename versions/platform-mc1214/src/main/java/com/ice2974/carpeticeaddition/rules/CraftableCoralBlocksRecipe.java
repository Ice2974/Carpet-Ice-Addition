package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Optional;

public final class CraftableCoralBlocksRecipe implements CraftingRecipe {
    private static final RegistryKey<net.minecraft.recipe.Recipe<?>> ID = RegistryKey.of(
            RegistryKeys.RECIPE,
            Identifier.of("carpet-ice-addition", "craftable_coral_blocks")
    );
    private static final CraftableCoralBlocksRecipe INSTANCE = new CraftableCoralBlocksRecipe();
    private static final RecipeEntry<CraftingRecipe> ENTRY = new RecipeEntry<>(ID, INSTANCE);

    private CraftableCoralBlocksRecipe() {
    }

    public static Optional<RecipeEntry<CraftingRecipe>> match(CraftingRecipeInput input) {
        if (!CarpetIceAdditionSettings.craftableCoralBlocks || getOutput(input).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ENTRY);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return CarpetIceAdditionSettings.craftableCoralBlocks && !getOutput(input).isEmpty();
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        return getOutput(input);
    }

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return (RecipeSerializer) RecipeSerializer.SHAPED;
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return CraftingRecipeCategory.MISC;
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.NONE;
    }

    @Override
    public net.minecraft.recipe.book.RecipeBookCategory getRecipeBookCategory() {
        return CraftingRecipe.super.getRecipeBookCategory();
    }

    private static ItemStack getOutput(CraftingRecipeInput input) {
        if (input.getWidth() != 3 || input.getHeight() != 3 || input.size() != 9) {
            return ItemStack.EMPTY;
        }

        Item fan = input.getStackInSlot(0).getItem();
        if (input.getStackInSlot(0).isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int slot = 1; slot < input.size(); slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getItem() != fan) {
                return ItemStack.EMPTY;
            }
        }

        Item block = getCoralBlock(fan);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    private static Item getCoralBlock(Item fan) {
        if (fan == Items.TUBE_CORAL_FAN) return Items.TUBE_CORAL_BLOCK;
        if (fan == Items.BRAIN_CORAL_FAN) return Items.BRAIN_CORAL_BLOCK;
        if (fan == Items.BUBBLE_CORAL_FAN) return Items.BUBBLE_CORAL_BLOCK;
        if (fan == Items.FIRE_CORAL_FAN) return Items.FIRE_CORAL_BLOCK;
        if (fan == Items.HORN_CORAL_FAN) return Items.HORN_CORAL_BLOCK;
        if (fan == Items.DEAD_TUBE_CORAL_FAN) return Items.DEAD_TUBE_CORAL_BLOCK;
        if (fan == Items.DEAD_BRAIN_CORAL_FAN) return Items.DEAD_BRAIN_CORAL_BLOCK;
        if (fan == Items.DEAD_BUBBLE_CORAL_FAN) return Items.DEAD_BUBBLE_CORAL_BLOCK;
        if (fan == Items.DEAD_FIRE_CORAL_FAN) return Items.DEAD_FIRE_CORAL_BLOCK;
        if (fan == Items.DEAD_HORN_CORAL_FAN) return Items.DEAD_HORN_CORAL_BLOCK;
        return null;
    }
}
