package com.ice2974.carpeticeaddition.rules;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Shared external stonecutting result matching and platform result resolution. */
public final class StonecuttingRecipeConflictScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final Set<RecipeConflictDefinition> UNRESOLVABLE_RECIPE_WARNED = ConcurrentHashMap.newKeySet();
    private StonecuttingRecipeConflictScanner() {}

    public static boolean hasConflictingResult(Recipe<?> recipe, String recipeId, HolderLookup.Provider lookup, RecipeConflictDefinition definition, Set<String> targets) {
        if (!RecipeConflictScanner.matchesType(definition, recipe.getType())) {
            return false;
        }
        Resolution resolution = resolveResult(recipe, lookup);
        if (resolution.sawFailure()) {
            warnUnresolvableRecipe(definition, recipeId, resolution.firstFailure());
            return true;
        }
        ItemStack result = resolution.stack();
        if (result == null || result.isEmpty()) {
            return false;
        }
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        return resultId != null && targets.contains(resultId.toString());
    }

    /** 每个规则在 JVM 运行期至多告警一次，规则由其冲突描述标识。 */
    private static void warnUnresolvableRecipe(RecipeConflictDefinition definition, String recipeId, Throwable failure) {
        if (UNRESOLVABLE_RECIPE_WARNED.add(definition)) {
            LOGGER.warn("[Carpet Ice Addition] External stonecutting recipe '{}' failed to resolve its result ({})"
                    + " for recipes {}; treating it as a conflict", recipeId,
                    failure == null ? null : failure.getClass().getName(), definition.recipeIds());
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
