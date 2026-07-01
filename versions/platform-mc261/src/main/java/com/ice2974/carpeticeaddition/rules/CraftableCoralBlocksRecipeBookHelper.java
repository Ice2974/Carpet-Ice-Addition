package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 26.1 (mojmap) 珊瑚块配方书同步 Helper。
 *
 * <p>珊瑚块配方为真实注册的 vanilla shaped recipe（常驻 RecipeManager），规则只作 gate。
 * 本 Helper 负责在规则切换 / 玩家加入 / reload 后，对这 10 条 recipe 做最小 unlock/lock 同步，
 * 不全量同步所有配方，不触发 datapack reload。
 *
 * <p>双层优化：
 * <ul>
 *   <li>第一层：缓存 10 条珊瑚块 {@link RecipeHolder}，避免每次玩家进入都全量扫描 RecipeManager。</li>
 *   <li>第二层：{@code syncRevision} + {@code playerSyncedRevision} 记录每个玩家已同步到的 revision，
 *       同一玩家在规则未变化时反复进入跳过重复 unlock/lock。</li>
 * </ul>
 *
 * <p>关于 "empty 时玩家下次进入仍会重试"：指的是重试同步流程（再次走 applyRecipeBookState），
 * 而 <b>不是</b> 重新扫描 RecipeManager。若 {@code cachedCoralRecipes} 已缓存空列表，
 * 后续玩家进入会命中 empty 缓存直接返回空列表，不会重新扫描 RecipeManager，
 * 直到 {@code /reload}（{@link #onReload}）或 {@link #onServerClosed} 清空缓存。
 */
public final class CraftableCoralBlocksRecipeBookHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private CraftableCoralBlocksRecipeBookHelper() {
    }

    /** 触发同步的路径，用于一次性 warn 去重与日志上下文。 */
    private enum TriggerPath {
        JOIN,
        RULE_CHANGED,
        RELOAD
    }

    /** 缓存的 10 条珊瑚块 recipe holder；null 表示未缓存，空列表表示已缓存但 RecipeManager 中无匹配。 */
    private static List<RecipeHolder<?>> cachedCoralRecipes;

    /** 当前同步版本号；规则切换 / reload 时递增。 */
    private static int syncRevision = 0;

    /** 每个玩家已同步到的 revision；null / 旧值表示需要重新同步。 */
    private static final Map<UUID, Integer> playerSyncedRevision = new HashMap<>();

    /** 配方不完整（empty / partial）一次性 warn 去重；reload 时重置以重新诊断。 */
    private static final Set<TriggerPath> warnedIncomplete = EnumSet.noneOf(TriggerPath.class);

    /** 同步失败一次性 warn 去重；按进程 / path 去重，避免持续失败刷屏。 */
    private static final Set<TriggerPath> warnedApplyFail = EnumSet.noneOf(TriggerPath.class);

    /**
     * 从 RecipeManager 按 id 过滤出 10 条珊瑚块 recipe（reload 后对象是新实例，需重新查）。
     * 仅在缓存为空时由 {@link #getCoralRecipes} 调用。
     */
    private static List<RecipeHolder<?>> findCoralRecipes(RecipeManager manager) {
        List<RecipeHolder<?>> result = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Identifier id = holder.id().identifier();
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
                result.add(holder);
            }
        }
        return result;
    }

    /**
     * 获取珊瑚块 recipe，命中缓存时复用，避免反复全量扫描 RecipeManager。
     *
     * <p>无论结果完整 / partial / empty 都缓存：partial / empty 通常是服务器稳定状态
     *（recipe JSON 未注册 / 被 datapack 移除 / datapack 缺失部分），反复扫描不会改变结果，
     * 缓存后命中即可。{@code /reload} 后 {@link #invalidateCoralRecipeCache()} 会重新扫描，
     * 能感知到 datapack 修复后的新状态。
     */
    private static List<RecipeHolder<?>> getCoralRecipes(MinecraftServer server, TriggerPath path) {
        if (server == null) {
            return List.of();
        }
        if (cachedCoralRecipes != null) {
            return cachedCoralRecipes;
        }
        List<RecipeHolder<?>> result = findCoralRecipes(server.getRecipeManager());
        int expected = CraftableCoralBlocksRecipes.RECIPE_PATHS.size();
        if (result.size() != expected) {
            warnCoralRecipesIncompleteOnce(path, result.size(), expected);
        }
        cachedCoralRecipes = result;
        return result;
    }

    private static void invalidateCoralRecipeCache() {
        cachedCoralRecipes = null;
    }

    private static boolean isPlayerUpToDate(ServerPlayer player) {
        // 必须用 null 判断：新玩家 map 无记录时，即使 syncRevision 为任意值也必须视为未同步。
        // 不要使用 getOrDefault(uuid, syncRevision)，那样会让首次进入玩家被误判为已同步。
        Integer prev = playerSyncedRevision.get(player.getUUID());
        return prev != null && prev.intValue() == syncRevision;
    }

    private static void markPlayerSynced(ServerPlayer player) {
        playerSyncedRevision.put(player.getUUID(), syncRevision);
    }

    /**
     * 对玩家应用当前规则状态下的珊瑚块配方书同步。
     *
     * @return {@code true} 仅当 award/remove 成功完成；{@code false} 表示无可同步配方或同步失败（不标记已同步，下次进入重试）。
     */
    private static boolean applyRecipeBookState(ServerPlayer player, Collection<RecipeHolder<?>> coral, TriggerPath path) {
        if (coral.isEmpty()) {
            return false;
        }
        try {
            ServerRecipeBook book = player.getRecipeBook();
            if (CraftableCoralBlocksSettings.effective()) {
                player.awardRecipes(coral);
            } else {
                book.removeRecipes(coral, player);
            }
            return true;
        } catch (Throwable t) {
            warnApplyFailureOnce(path, t);
            return false;
        }
    }

    private static void warnCoralRecipesIncompleteOnce(TriggerPath path, int found, int expected) {
        if (warnedIncomplete.add(path)) {
            LOGGER.warn("[Carpet Ice Addition] craftableCoralBlocks: coral recipes incomplete (found={}, expected={}, trigger={})",
                    found, expected, path);
        }
    }

    private static void warnApplyFailureOnce(TriggerPath path, Throwable cause) {
        if (warnedApplyFail.add(path)) {
            LOGGER.warn("[Carpet Ice Addition] craftableCoralBlocks: recipe book sync failed (trigger={}, cause={})",
                    path, cause.toString());
        }
    }

    public static void onRuleChanged(MinecraftServer server) {
        if (server == null) {
            return;
        }
        syncRevision++;
        List<RecipeHolder<?>> coral = getCoralRecipes(server, TriggerPath.RULE_CHANGED);
        if (coral.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (applyRecipeBookState(player, coral, TriggerPath.RULE_CHANGED)) {
                markPlayerSynced(player);
            }
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onReload(MinecraftServer server) {
        if (server == null) {
            return;
        }
        // /reload 后 RecipeHolder 实例重建，必须清空缓存；并重置 incomplete / apply-fail warn，使本次 reload 能重新诊断。
        invalidateCoralRecipeCache();
        warnedIncomplete.clear();
        warnedApplyFail.clear();
        syncRevision++;
        List<RecipeHolder<?>> coral = getCoralRecipes(server, TriggerPath.RELOAD);
        if (coral.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (applyRecipeBookState(player, coral, TriggerPath.RELOAD)) {
                markPlayerSynced(player);
            }
            CraftingRefresherDispatcher.refreshOpenCraftingMenu(player);
        }
    }

    public static void onPlayerJoin(MinecraftServer server, ServerPlayer player) {
        if (server == null) {
            return;
        }
        if (isPlayerUpToDate(player)) {
            return;
        }
        List<RecipeHolder<?>> coral = getCoralRecipes(server, TriggerPath.JOIN);
        if (coral.isEmpty()) {
            return;
        }
        if (applyRecipeBookState(player, coral, TriggerPath.JOIN)) {
            markPlayerSynced(player);
        }
        // 不刷新 3×3 菜单：玩家刚进入无打开的合成菜单。
    }

    public static void onServerClosed(MinecraftServer server) {
        invalidateCoralRecipeCache();
        playerSyncedRevision.clear();
        warnedIncomplete.clear();
        warnedApplyFail.clear();
        syncRevision = 0;
    }
}
