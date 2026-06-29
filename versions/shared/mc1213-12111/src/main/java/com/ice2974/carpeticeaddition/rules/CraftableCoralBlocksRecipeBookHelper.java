package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.21.3 ~ 1.21.11 (yarn) 珊瑚块配方书同步 Helper。
 *
 * <p>珊瑚块配方为真实注册的 vanilla shaped recipe（常驻 RecipeManager），规则只作 gate。
 * 本 Helper 负责在规则切换 / 玩家加入 / reload 后，对这 10 条 recipe 做最小 unlock/lock 同步，
 * 不全量同步所有配方，不触发 datapack reload。
 */
public final class CraftableCoralBlocksRecipeBookHelper {
    private CraftableCoralBlocksRecipeBookHelper() {
    }

    /**
     * 从 RecipeManager 按 id 过滤出 10 条珊瑚块 recipe（reload 后对象是新实例，需重新查）。
     */
    public static List<RecipeEntry<?>> findCoralRecipes(ServerRecipeManager manager) {
        List<RecipeEntry<?>> result = new ArrayList<>();
        for (RecipeEntry<?> entry : manager.values()) {
            Identifier id = entry.id().getValue();
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
                result.add(entry);
            }
        }
        return result;
    }

    public static void onRuleChanged(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<RecipeEntry<?>> coral = findCoralRecipes(server.getRecipeManager());
        if (coral.isEmpty()) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            applyRecipeBookState(player, coral);
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onPlayerJoin(MinecraftServer server, ServerPlayerEntity player) {
        applyRecipeBookState(player, findCoralRecipes(server.getRecipeManager()));
    }

    private static void applyRecipeBookState(ServerPlayerEntity player, List<RecipeEntry<?>> coral) {
        if (coral.isEmpty()) {
            return;
        }
        try {
            if (CarpetIceAdditionSettings.craftableCoralBlocks) {
                player.unlockRecipes(coral);
            } else {
                player.lockRecipes(coral);
            }
        } catch (Throwable ignored) {
            // 规则相关配方同步失败不应阻断其他逻辑
        }
    }
}
