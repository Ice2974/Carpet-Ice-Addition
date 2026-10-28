package com.ice2974.carpeticeaddition.rules;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared external stonecutting result matching and platform result resolution. */
public final class StonecuttingRecipeConflictScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final AtomicBoolean UNRESOLVABLE_RECIPE_WARNED = new AtomicBoolean(false);
    private StonecuttingRecipeConflictScanner() {}

    public static boolean hasConflictingResult(Recipe<?> recipe, String recipeId, ContextMap ctx, RecipeConflictDefinition definition, Set<String> targets) {
        if (!RecipeConflictScanner.matchesType(definition, recipe.getType())) {
            return false;
        }
        Resolution resolution = resolveResultStacks(recipe, ctx);
        if (resolution.sawFailure()) {
            warnUnresolvableRecipe(recipeId, resolution.firstFailure());
            return true;
        }
        for (ItemStack result : resolution.stacks()) {
            Identifier resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId != null && targets.contains(resultId.toString())) {
                return true;
            }
        }
        return false;
    }

    /** 一次性告警「产物无法解析的外部切石配方」，包含配方 id 与异常类型；重复命中静默。 */
    private static void warnUnresolvableRecipe(String recipeId, Throwable failure) {
        if (UNRESOLVABLE_RECIPE_WARNED.compareAndSet(false, true)) {
            LOGGER.warn("[Carpet Ice Addition] External stonecutting recipe '{}' failed to resolve its result ({})"
                    + "; treating it as a conflict", recipeId, failure == null ? null : failure.getClass().getName());
        }
    }

    record Resolution(java.util.List<ItemStack> stacks, boolean sawFailure, Throwable firstFailure) {
    }

    /** 汇总配方全部 display 解析出的非空结果栈；单条 display 异常只跳过自身，但记录失败信号。 */
    static Resolution resolveResultStacks(Recipe<?> recipe, ContextMap ctx) {
        java.util.List<ItemStack> results = new ArrayList<>();
        Throwable firstFailure = null;
        try {
            java.util.List<RecipeDisplay> displays = recipe.display();
            if (displays == null) {
                return new Resolution(results, false, null);
            }
            for (RecipeDisplay display : displays) {
                try {
                    SlotDisplay resultDisplay = display.result();
                    if (resultDisplay == null) {
                        continue;
                    }
                    java.util.List<ItemStack> resolved = resultDisplay.resolveForStacks(ctx);
                    if (resolved == null) {
                        continue;
                    }
                    for (ItemStack stack : resolved) {
                        if (stack != null && !stack.isEmpty()) {
                            results.add(stack);
                        }
                    }
                } catch (Throwable failure) {
                    // 跳过无法解析的 display，但保留失败信号（fail-closed 判定依据）
                    if (firstFailure == null) {
                        firstFailure = failure;
                    }
                }
            }
        } catch (Throwable failure) {
            // 无法提取 display 列表的配方：整体视为解析失败
            return new Resolution(java.util.List.of(), true, failure);
        }
        return new Resolution(results, firstFailure != null, firstFailure);
    }

}
