package com.ice2974.carpeticeaddition.rules;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Set;

/** Shared crafting result scan; platform-specific extraction preserves existing error handling. */
public final class CraftingRecipeConflictScanner {
    private CraftingRecipeConflictScanner() {}

    public static boolean detectConflict(MinecraftServer server, RecipeConflictDefinition definition) {
        if (server == null) {
            return false;
        }
        RecipeManager manager = server.getRecipeManager();
        HolderLookup.Provider lookup = server.registryAccess();
        Set<String> targets = definition.targets();
        return RecipeConflictScanner.scan(manager.getRecipes(), definition,
                entry -> RecipeConflictScanner.matchesType(definition, entry.value().getType()),
                entry -> entry.id().toString(), entry -> {}, entry -> {
            Recipe<?> recipe = entry.value();
            try {
                ItemStack result = recipe.getResultItem(lookup);
                if (result == null || result.isEmpty()) {
                    return false;
                }
                Item item = result.getItem();
                if (item == null) {
                    return false;
                }
                ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(item);
                if (targets.contains(resultId.toString())) {
                    return true;
                }
            } catch (Throwable ignored) {
                // 跳过无法提取产物的 recipe，避免单条异常导致整体检测失败
            }
            return false;
        });
    }

}
