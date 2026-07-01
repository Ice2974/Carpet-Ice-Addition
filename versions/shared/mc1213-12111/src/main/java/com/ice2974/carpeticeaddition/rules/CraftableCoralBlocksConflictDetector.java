package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * craftableCoralBlocks 外部配方冲突检测与外部替代 recipe 查找（1.21.3 ~ 1.21.11 yarn，shared）。
 *
 * <p>冲突判定：RecipeManager 中存在 namespace ≠ {@code carpet-ice-addition} 的 crafting 配方，
 * 其输出产物 Item 与本模组自带 10 条 coral recipe 的某个目标产物一致。目标产物集合以
 * {@link CraftableCoralBlocksRecipes#RESULT_ITEM_IDS} 为权威来源。
 *
 * <p>检测范围限定 {@link RecipeType#CRAFTING}：stonecutting / smelting / custom recipe type 等不触发锁定。
 *
 * <p>1.21.3+ 无 {@code getAllMatches}，外部替代查找通过遍历 {@link ServerRecipeManager#values()} 并调用
 * {@link Recipe#matches(RecipeInput, World)} 实现真实匹配。result 提取走 RecipeDisplay / SlotDisplay。
 */
public final class CraftableCoralBlocksConflictDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private CraftableCoralBlocksConflictDetector() {
    }

    /**
     * 扫描 RecipeManager 判定是否存在外部 crafting 配方冲突。
     */
    public static boolean detectConflict(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        ServerWorld world = server.getOverworld();
        if (world == null) {
            return false;
        }
        ServerRecipeManager manager = server.getRecipeManager();
        ContextParameterMap ctx = SlotDisplayContexts.createParameters(world);
        Set<String> targets = new HashSet<>(CraftableCoralBlocksRecipes.RESULT_ITEM_IDS);
        for (RecipeEntry<?> entry : manager.values()) {
            Recipe<?> recipe = entry.value();
            if (recipe.getType() != RecipeType.CRAFTING) {
                continue;
            }
            Identifier id = entry.id().getValue();
            if (CraftableCoralBlocksRecipes.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            try {
                List<RecipeDisplay> displays = recipe.getDisplays();
                if (displays == null || displays.isEmpty()) {
                    continue;
                }
                SlotDisplay resultDisplay = displays.get(0).result();
                if (resultDisplay == null) {
                    continue;
                }
                ItemStack result = resultDisplay.getFirst(ctx);
                if (result == null || result.isEmpty()) {
                    continue;
                }
                Item item = result.getItem();
                if (item == null) {
                    continue;
                }
                Identifier resultId = Registries.ITEM.getId(item);
                if (targets.contains(resultId.toString())) {
                    return true;
                }
            } catch (Throwable ignored) {
                // 跳过无法提取产物的 recipe
            }
        }
        return false;
    }

    /**
     * 重新计算冲突锁定状态，并在状态迁移 / reload 仍锁定时广播提示与写日志。
     * 必须在 recipe book 同步与菜单刷新之前调用。
     */
    public static void recomputeAndNotify(MinecraftServer server) {
        boolean conflict = detectConflict(server);
        boolean wasLocked = CraftableCoralBlocksState.isConflictLocked();
        CraftableCoralBlocksState.setConflictLocked(conflict);

        if (conflict) {
            broadcast(server, "carpet.rule.craftableCoralBlocks.conflict.locked");
        } else if (wasLocked) {
            broadcast(server, "carpet.rule.craftableCoralBlocks.conflict.resolved");
        }
    }

    private static void broadcast(MinecraftServer server, String key) {
        String text = TranslationFormatUtil.translate(key);
        LOGGER.warn("[Carpet Ice Addition] {}", text);
        if (server.getPlayerManager() != null) {
            Text msg = Text.literal(text);
            server.getPlayerManager().broadcast(msg, false);
        }
    }

    /**
     * B2 重载 A：effective=false 且原返回命中本模组 coral recipe 时，遍历 {@link ServerRecipeManager#values()}
     * 取一个 namespace 外部、对当前 input 真实 matches 的 recipe 返回。无则返回 empty。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> findExternalMatch(
            ServerRecipeManager manager, RecipeType<T> type, I input, World world) {
        for (RecipeEntry<?> entry : manager.values()) {
            Recipe<?> recipe = entry.value();
            if (recipe.getType() != type) {
                continue;
            }
            Identifier id = entry.id().getValue();
            if (CraftableCoralBlocksRecipes.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            try {
                if (((Recipe) recipe).matches(input, world)) {
                    return Optional.of((RecipeEntry<T>) entry);
                }
            } catch (Throwable ignored) {
                // 跳过匹配异常的 recipe
            }
        }
        return Optional.empty();
    }

    /**
     * B3 Crafter：effective=false 且原返回命中本模组 coral recipe 时，取一个 namespace 外部、对当前
     * input 真实 matches 的 crafting recipe 返回。无则返回 empty。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RecipeEntry<CraftingRecipe>> findExternalCrafterMatch(World world, CraftingRecipeInput input) {
        ServerRecipeManager manager;
        if (world instanceof ServerWorld sw) {
            manager = sw.getRecipeManager();
        } else if (world != null) {
            manager = (ServerRecipeManager) world.getRecipeManager();
        } else {
            return Optional.empty();
        }
        for (RecipeEntry<?> entry : manager.values()) {
            Recipe<?> recipe = entry.value();
            if (recipe.getType() != RecipeType.CRAFTING) {
                continue;
            }
            Identifier id = entry.id().getValue();
            if (CraftableCoralBlocksRecipes.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            try {
                if (((Recipe) recipe).matches(input, world)) {
                    return Optional.of((RecipeEntry<CraftingRecipe>) entry);
                }
            } catch (Throwable ignored) {
                // 跳过匹配异常的 recipe
            }
        }
        return Optional.empty();
    }
}
