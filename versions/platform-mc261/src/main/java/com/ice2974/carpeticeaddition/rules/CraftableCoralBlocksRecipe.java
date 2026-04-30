package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class CraftableCoralBlocksRecipe implements CraftingRecipe {
    private static final ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> ID = ResourceKey.create(
            Registries.RECIPE,
            Identifier.fromNamespaceAndPath("carpet-ice-addition", "craftable_coral_blocks")
    );
    private static final CraftableCoralBlocksRecipe INSTANCE = new CraftableCoralBlocksRecipe();
    private static final RecipeHolder<CraftingRecipe> ENTRY = new RecipeHolder<>(ID, INSTANCE);

    private CraftableCoralBlocksRecipe() {
    }

    public static Optional<RecipeHolder<CraftingRecipe>> match(CraftingInput input) {
        if (!CarpetIceAdditionSettings.craftableCoralBlocks || getOutput(input).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ENTRY);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return CarpetIceAdditionSettings.craftableCoralBlocks && !getOutput(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return getOutput(input);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return (RecipeSerializer) net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.getValue(Identifier.withDefaultNamespace("crafting_shaped"));
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return CraftingRecipe.super.recipeBookCategory();
    }

    private static ItemStack getOutput(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3 || input.size() != 9) {
            return ItemStack.EMPTY;
        }

        Item fan = input.getItem(0).getItem();
        if (input.getItem(0).isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int slot = 1; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
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
