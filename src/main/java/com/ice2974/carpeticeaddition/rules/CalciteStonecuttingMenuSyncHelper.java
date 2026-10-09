package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings;
//#if MC>=12103
import net.minecraft.resources.Identifier;
//#else
//$$import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code calciteStonecuttingRecipe} 的菜单同步器与解锁同步器。
 *
 * <p>本类承担两件事，二者都挂在共享协调器既有的 {@link RecipePackCoordinator.ManagedPack} 钩子上，
 * 不新增第二套 reload 管线：
 * <ol>
 *   <li><b>切石机菜单</b>：{@link #onReload(MinecraftServer)} 在静默点重建在线玩家已打开菜单的配方列表；
 *       {@link #onPackDisabled(MinecraftServer)} 在本包被取消选中的**瞬间**清空结果槽，掐断已算好的结果与
 *       pending 取出（时序与既有 {@code craftableCoralBlocks} 的 {@code onPackDisable} 一致）。</li>
 *   <li><b>配方解锁</b>：规则启用且内置包已加载时向在线玩家 {@code awardRecipes}，新登录玩家由
 *       {@link #onPlayerJoin(MinecraftServer, ServerPlayer)} 补齐；本包被取消选中时 {@code resetRecipes}
 *       撤销。切石配方**没有**独立配方书界面，但解锁通知走的是
 *       {@code ClientboundRecipeBookAddPacket}（服务端按 {@code Recipe#showNotification()} 生成），
 *       与配方书界面无关，因此在切石配方上同样会弹出原版「解锁新配方」提示。</li>
 * </ol>
 *
 * <p><b>授予的三个必要条件</b>（见 {@link CalciteStonecuttingRecipeData#unlockAllowed}）：规则生效、
 * 本模组内置包当前已选中、且配方能从**当前** {@code RecipeManager} 解析出 holder。第三条不可省：
 * {@code ServerRecipeBook.addRecipes} 先写入「已解锁」记录再解析显示条目，配方缺失时不发包却已记录，
 * 会导致既无提示、又让后续授予因「已解锁」永久静默。
 *
 * <p><b>两段独立尝试</b>：同一静默点内「配方书动作」与「菜单动作」分别经
 * {@link IsolatedHookRunner#runAll(java.util.List)} 执行，两段都必然被尝试——前一段抛异常不得跳过后一段；
 * 两段结果用 {@link IsolatedHookRunner.Result#plus} 合并成**一次**上报，交给协调器的钩子失败路径记账
 * （{@code safeRun} → 兼容性上报一次 → {@code RecipeReloadGate.onPassFinished(false)}：重新欠一次 pass
 * 且不得报告 ready）。配方书侧的解析本身也在隔离段内：解析失败⇒本次不授予也不撤销，菜单段照样执行。
 * 本类不写任何日志，避免刷屏。
 *
 * <p>不对称说明：珊瑚侧的 {@link CraftableCoralBlocksRecipeBookHelper} 保持既有实现（不查包选中、
 * 逐玩家动作不分段隔离），本类只服务 {@code calciteStonecuttingRecipe}。
 */
public final class CalciteStonecuttingMenuSyncHelper {
    private static final String SYNC_DESCRIPTION =
            "calciteStonecuttingRecipe: stonecutter menu sync and recipe unlock";
    private static final String CLEANUP_DESCRIPTION =
            "calciteStonecuttingRecipe: stonecutter cleanup and recipe revoke";

    private CalciteStonecuttingMenuSyncHelper() {
    }

    /** 静默点同步：先尝试授予解锁，再尝试刷新已打开的切石机菜单（两段各自独立执行）。 */
    public static void onReload(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        BookPlan plan = new BookPlan();
        IsolatedHookRunner.Result resolveResult = IsolatedHookRunner.runAll(List.of(() -> {
            List<RecipeHolder<?>> own = findOwnRecipes(server);
            plan.resolve(own, CalciteStonecuttingRecipeData.unlockAllowed(
                    CalciteStonecuttingRecipeSettings.effective(), isOwnPackSelected(server), !own.isEmpty()));
        }));
        List<RecipeHolder<?>> recipes = plan.recipes;
        boolean award = plan.apply;

        List<Runnable> unlocks = new ArrayList<>();
        List<Runnable> refreshes = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (award) {
                unlocks.add(() -> player.awardRecipes(recipes));
            }
            refreshes.add(() -> StonecutterRefresherDispatcher.refreshOpenStonecutterMenu(player));
        }
        throwIfFailed(resolveResult
                .plus(IsolatedHookRunner.runAll(unlocks))
                .plus(IsolatedHookRunner.runAll(refreshes)), SYNC_DESCRIPTION);
    }

    /**
     * 本包被取消选中前的即时清理：先尝试撤销解锁记录，再尝试清空结果槽（两段各自独立执行）。
     *
     * <p>撤销只针对「内容仍是本规则承诺」的本模组配方 id：同 id 已被外部数据包覆盖时不撤销，
     * 避免替外部内容做解锁操作。
     */
    public static void onPackDisabled(MinecraftServer server) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        BookPlan plan = new BookPlan();
        IsolatedHookRunner.Result resolveResult = IsolatedHookRunner.runAll(List.of(() -> {
            List<RecipeHolder<?>> revocable = new ArrayList<>();
            for (RecipeHolder<?> holder : findOwnRecipes(server)) {
                if (CalciteStonecuttingRecipeConflictDetector.matchesRuleContract(server, holder)) {
                    revocable.add(holder);
                }
            }
            plan.resolve(revocable, !revocable.isEmpty());
        }));
        List<RecipeHolder<?>> recipes = plan.recipes;
        boolean revoke = plan.apply;

        List<Runnable> revokes = new ArrayList<>();
        List<Runnable> cleanups = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (revoke) {
                revokes.add(() -> player.resetRecipes(recipes));
            }
            cleanups.add(() -> StonecutterRefresherDispatcher.clearOpenStonecutterResult(player));
        }
        throwIfFailed(resolveResult
                .plus(IsolatedHookRunner.runAll(revokes))
                .plus(IsolatedHookRunner.runAll(cleanups)), CLEANUP_DESCRIPTION);
    }

    /**
     * 玩家登录：补齐解锁状态。
     *
     * <p>已解锁玩家（含此前在切石机取出过结果的玩家）在此为原版空操作——{@code addRecipes} 对已在
     * 解锁集合内的 id 直接跳过且不发送任何包，因此不会重复弹窗。登录只有「授予」一步，异常直接交给
     * 协调器 {@code safeRun} 记账（与珊瑚登录钩子一致）。
     */
    public static void onPlayerJoin(MinecraftServer server, ServerPlayer player) {
        if (server == null || player == null) {
            return;
        }
        List<RecipeHolder<?>> own = findOwnRecipes(server);
        if (CalciteStonecuttingRecipeData.unlockAllowed(
                CalciteStonecuttingRecipeSettings.effective(), isOwnPackSelected(server), !own.isEmpty())) {
            player.awardRecipes(own);
        }
    }

    /**
     * 从**当前** {@code RecipeManager} 解析本规则自带的配方 holder（与珊瑚侧同构：绝不跨 reload 保留
     * holder，id 是唯一权威身份）。
     *
     * <p>1.21.1 的 {@code RecipeHolder.id()} 直接返回 {@code ResourceLocation}，1.21.3+ 返回
     * {@code ResourceKey} 且需要 {@code identifier()}——这是唯一需要跨版本分叉的一处小差异，用宏表达，
     * 不新增平台 override 文件。
     */
    private static List<RecipeHolder<?>> findOwnRecipes(MinecraftServer server) {
        Map<String, RecipeHolder<?>> byId = new HashMap<>();
        RecipeManager manager = server.getRecipeManager();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            //#if MC>=12103
            Identifier id = holder.id().identifier();
            //#else
            //$$ResourceLocation id = holder.id();
            //#endif
            if (CalciteStonecuttingRecipeData.isOwnRecipe(id.getNamespace(), id.getPath())) {
                byId.put(id.toString(), holder);
            }
        }
        List<RecipeHolder<?>> result = new ArrayList<>();
        for (String path : CalciteStonecuttingRecipeData.RECIPE_PATHS) {
            RecipeHolder<?> holder = byId.get(CalciteStonecuttingRecipeData.NAMESPACE + ":" + path);
            if (holder != null) {
                result.add(holder);
            }
        }
        return result;
    }

    /** 本模组内置包当前是否已选中（复用协调器的只读查询，不新增状态）。 */
    private static boolean isOwnPackSelected(MinecraftServer server) {
        return RecipePackCoordinator.isBuiltinPackSelected(
                server, RecipeDatapackRegistry.CALCITE_STONECUTTING_PACK.toString());
    }

    /** 单玩家异常已被隔离；只要存在失败就抛出聚合异常，交给协调器记账（不在此处写日志）。 */
    private static void throwIfFailed(IsolatedHookRunner.Result result, String description) {
        if (!result.ok()) {
            throw result.aggregatedException(description);
        }
    }

    /**
     * 单次同步的配方书侧计划：解析结果在隔离段内一次性写入，解析失败时保持
     * {@code apply=false, recipes=[]}——既不授予也不撤销（不会留下静默解锁记录），
     * 同时不影响后续菜单段执行。
     */
    private static final class BookPlan {
        private List<RecipeHolder<?>> recipes = List.of();
        private boolean apply;

        private void resolve(List<RecipeHolder<?>> resolved, boolean shouldApply) {
            recipes = List.copyOf(resolved);
            apply = shouldApply;
        }
    }
}
