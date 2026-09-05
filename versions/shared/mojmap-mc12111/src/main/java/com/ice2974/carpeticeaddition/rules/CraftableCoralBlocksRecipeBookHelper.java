package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
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

    private static List<RecipeHolder<?>> findCurrentRecipes(MinecraftServer server) {
        Map<String, RecipeHolder<?>> byId = new HashMap<>();
        RecipeManager manager = server.getRecipeManager();
        for (RecipeHolder<?> entry : manager.getRecipes()) {
            Identifier id = entry.id().identifier();
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
                byId.put(id.toString(), entry);
            }
        }
        List<RecipeHolder<?>> result = new ArrayList<>();
        for (String path : CraftableCoralBlocksRecipes.RECIPE_PATHS) {
            RecipeHolder<?> entry = byId.get(CraftableCoralBlocksRecipes.NAMESPACE + ":" + path);
            if (entry != null) {
                result.add(entry);
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
            player.resetRecipes(recipes);
        }
    }

    private static void syncPlayers(MinecraftServer server) {
        List<RecipeHolder<?>> recipes = findCurrentRecipes(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            apply(player, recipes);
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onPackDisable(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<RecipeHolder<?>> recipes = findCurrentRecipes(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!recipes.isEmpty()) {
                player.resetRecipes(recipes);
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

}
