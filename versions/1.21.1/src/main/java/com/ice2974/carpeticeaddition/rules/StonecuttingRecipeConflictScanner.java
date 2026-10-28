package com.ice2974.carpeticeaddition.rules;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared external stonecutting result matching and platform result resolution. */
public final class StonecuttingRecipeConflictScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final AtomicBoolean UNRESOLVABLE_RECIPE_WARNED = new AtomicBoolean(false);
    private StonecuttingRecipeConflictScanner() {}

    public static boolean hasConflictingResult(Recipe<?> recipe, String recipeId, HolderLookup.Provider lookup, RecipeConflictDefinition definition, Set<String> targets) {
        if (!RecipeConflictScanner.matchesType(definition, recipe.getType())) {
            return false;
        }
        Resolution resolution = resolveResult(recipe, lookup);
        if (resolution.sawFailure()) {
            warnUnresolvableRecipe(recipeId, resolution.firstFailure());
            return true;
        }
        ItemStack result = resolution.stack();
        if (result == null || result.isEmpty()) {
            return false;
        }
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        return resultId != null && targets.contains(resultId.toString());
    }

    /** 一次性告警「产物无法解析的外部切石配方」，包含配方 id 与异常类型；重复命中静默。 */
    private static void warnUnresolvableRecipe(String recipeId, Throwable failure) {
        if (UNRESOLVABLE_RECIPE_WARNED.compareAndSet(false, true)) {
            LOGGER.warn("[Carpet Ice Addition] External stonecutting recipe '{}' failed to resolve its result ({})"
                    + "; treating it as a conflict", recipeId, failure == null ? null : failure.getClass().getName());
        }
    }

    record Resolution(ItemStack stack, boolean sawFailure, Throwable firstFailure) {
    }

    static Resolution resolveResult(Recipe<?> recipe, HolderLookup.Provider lookup) {
        try {
            return new Resolution(recipe.getResultItem(lookup), false, null);
        } catch (Throwable failure) {
            return new Resolution(null, true, failure);
        }
    }

}
