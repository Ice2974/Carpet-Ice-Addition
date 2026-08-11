package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Synchronizes only the craftableCoralBlocks recipe-book state.
 *
 * <p>Recipe IDs are the authoritative identity.  RecipeEntry instances are
 * resolved from the current RecipeManager for each synchronization and are
 * never retained across a datapack reload.
 */
public final class CraftableCoralBlocksRecipeBookHelper {
    private CraftableCoralBlocksRecipeBookHelper() {
    }

    private static List<RecipeEntry<?>> findCurrentRecipes(MinecraftServer server) {
        Map<String, RecipeEntry<?>> byId = new HashMap<>();
        for (RecipeEntry<?> entry : server.getRecipeManager().values()) {
            Identifier id = entry.id();
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
                byId.put(id.toString(), entry);
            }
        }
        List<RecipeEntry<?>> result = new ArrayList<>();
        for (String path : CraftableCoralBlocksRecipes.RECIPE_PATHS) {
            RecipeEntry<?> entry = byId.get(CraftableCoralBlocksRecipes.NAMESPACE + ":" + path);
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }

    private static void apply(ServerPlayerEntity player, List<RecipeEntry<?>> recipes) {
        if (recipes.isEmpty()) {
            return;
        }
        if (CraftableCoralBlocksSettings.effective()) {
            player.unlockRecipes(recipes);
        } else {
            player.lockRecipes(recipes);
        }
    }

    private static void syncPlayers(MinecraftServer server) {
        List<RecipeEntry<?>> recipes = findCurrentRecipes(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            apply(player, recipes);
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onPackDisable(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<RecipeEntry<?>> recipes = findCurrentRecipes(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!recipes.isEmpty()) {
                player.lockRecipes(recipes);
            }
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onReload(MinecraftServer server) {
        if (server != null) {
            syncPlayers(server);
        }
    }

    public static void onPlayerJoin(MinecraftServer server, ServerPlayerEntity player) {
        if (server != null && player != null) {
            apply(player, findCurrentRecipes(server));
        }
    }

}
