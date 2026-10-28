package com.ice2974.carpeticeaddition.rules;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;
import java.util.Set;

/** Shared crafting result scan; platform-specific extraction preserves existing error handling. */
public final class CraftingRecipeConflictScanner {
    private CraftingRecipeConflictScanner() {}

    public static boolean detectConflict(MinecraftServer server, RecipeConflictDefinition definition) {
        if (server == null) {
            return false;
        }
        ServerLevel world = server.overworld();
        if (world == null) {
            return false;
        }
        RecipeManager manager = server.getRecipeManager();
        ContextMap ctx = SlotDisplayContext.fromLevel(world);
        Set<String> targets = definition.targets();
        return RecipeConflictScanner.scan(manager.getRecipes(), definition,
                entry -> RecipeConflictScanner.matchesType(definition, entry.value().getType()),
                entry -> entry.id().identifier().toString(), entry -> {}, entry -> {
            Recipe<?> recipe = entry.value();
            try {
                List<RecipeDisplay> displays = recipe.display();
                if (displays == null || displays.isEmpty()) {
                    return false;
                }
                for (RecipeDisplay display : displays) {
                    try {
                        SlotDisplay resultDisplay = display.result();
                        if (resultDisplay == null) {
                            continue;
                        }
                        List<ItemStack> results = resultDisplay.resolveForStacks(ctx);
                        if (results == null) {
                            continue;
                        }
                        for (ItemStack result : results) {
                            try {
                                if (result == null || result.isEmpty()) {
                                    continue;
                                }
                                Item item = result.getItem();
                                if (item == null) {
                                    continue;
                                }
                                Identifier resultId = BuiltInRegistries.ITEM.getKey(item);
                                if (targets.contains(resultId.toString())) {
                                    return true;
                                }
                            } catch (Throwable ignored) {
                                // 跳过无法解析的 result，继续检查同一 display 的其他 result
                            }
                        }
                    } catch (Throwable ignored) {
                        // 跳过无法提取的 display，继续检查同一 recipe 的其他 display
                    }
                }
            } catch (Throwable ignored) {
                // 跳过无法提取产物的 recipe
            }
            return false;
        });
    }

}
