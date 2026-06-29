package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 26.1 (mojmap) 珊瑚块配方书同步 Helper。
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
    public static List<RecipeHolder<?>> findCoralRecipes(RecipeManager manager) {
        List<RecipeHolder<?>> result = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Identifier id = holder.id().identifier();
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
                result.add(holder);
            }
        }
        return result;
    }

    public static void onRuleChanged(MinecraftServer server) {
        if (server == null) {
            return;
        }
        List<RecipeHolder<?>> coral = findCoralRecipes(server.getRecipeManager());
        if (coral.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyRecipeBookState(player, coral);
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onPlayerJoin(MinecraftServer server, ServerPlayer player) {
        applyRecipeBookState(player, findCoralRecipes(server.getRecipeManager()));
    }

    private static void applyRecipeBookState(ServerPlayer player, Collection<RecipeHolder<?>> coral) {
        if (coral.isEmpty()) {
            return;
        }
        try {
            ServerRecipeBook book = player.getRecipeBook();
            if (CarpetIceAdditionSettings.craftableCoralBlocks) {
                player.awardRecipes(coral);
            } else {
                book.removeRecipes(coral, player);
            }
        } catch (Throwable ignored) {
            // 规则相关配方同步失败不应阻断其他逻辑
        }
    }
}
