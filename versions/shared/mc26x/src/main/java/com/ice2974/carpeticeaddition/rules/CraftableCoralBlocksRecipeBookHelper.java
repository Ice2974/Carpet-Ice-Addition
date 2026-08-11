package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Synchronizes only the craftableCoralBlocks recipe-book state.
 *
 * <p>Recipe IDs are the authoritative identity.  RecipeHolder instances are
 * resolved from the current RecipeManager for each synchronization and are
 * never retained across a datapack reload.
 */
public final class CraftableCoralBlocksRecipeBookHelper {
    private CraftableCoralBlocksRecipeBookHelper() {
    }

    private static List<RecipeHolder<?>> findCurrentRecipes(MinecraftServer server) {
        Map<String, RecipeHolder<?>> byId = new HashMap<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Identifier id = holder.id().identifier();
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
                byId.put(id.toString(), holder);
            }
        }
        List<RecipeHolder<?>> result = new ArrayList<>();
        for (String path : CraftableCoralBlocksRecipes.RECIPE_PATHS) {
            RecipeHolder<?> holder = byId.get(CraftableCoralBlocksRecipes.NAMESPACE + ":" + path);
            if (holder != null) {
                result.add(holder);
            }
        }
        return result;
    }

    private static void apply(ServerPlayer player, List<RecipeHolder<?>> recipes) {
        if (recipes.isEmpty()) {
            return;
        }
        if (CraftableCoralBlocksSettings.effective()) {
            player.awardRecipes(recipes);
        } else {
            player.getRecipeBook().removeRecipes(recipes, player);
        }
    }

    private static void syncPlayers(MinecraftServer server) {
        List<RecipeHolder<?>> recipes = findCurrentRecipes(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            apply(player, recipes);
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onRuleChanged(MinecraftServer server) {
        if (server != null) {
            syncPlayers(server);
        }
    }

    public static void onPackDisable(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<RecipeHolder<?>> recipes = findCurrentRecipes(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!recipes.isEmpty()) {
                player.getRecipeBook().removeRecipes(recipes, player);
            }
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onReload(MinecraftServer server) {
        if (server != null) {
            syncPlayers(server);
        }
    }

    public static void onPlayerJoin(MinecraftServer server, ServerPlayer player) {
        if (server != null && player != null) {
            apply(player, findCurrentRecipes(server));
        }
    }

    public static void onServerClosed(MinecraftServer server) {
        // No recipe objects or server-specific cache survives a server instance.
    }
}
