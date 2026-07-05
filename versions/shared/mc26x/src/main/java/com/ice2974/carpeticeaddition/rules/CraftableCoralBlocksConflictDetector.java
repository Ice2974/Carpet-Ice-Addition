package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * craftableCoralBlocks 外部配方冲突检测与外部替代 recipe 查找（26.x mojmap）。
 *
 * <p>冲突判定：RecipeManager 中存在 namespace ≠ {@code carpet-ice-addition} 的 crafting 配方，
 * 其输出产物 Item 与本模组自带 10 条 coral recipe 的某个目标产物一致。目标产物集合以
 * {@link CraftableCoralBlocksRecipes#RESULT_ITEM_IDS} 为权威来源。
 *
 * <p>检测范围限定 {@link RecipeType#CRAFTING}：stonecutting / smelting / custom recipe type 等不触发锁定。
 *
 * <p>26.x 无 {@code getAllMatches}，外部替代查找通过遍历 {@link RecipeManager#getRecipes()} 并调用
 * {@link Recipe#matches(RecipeInput, Level)} 实现真实匹配。result 提取走 RecipeDisplay / SlotDisplay（mojmap 命名）。
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
        ServerLevel level = server.overworld();
        if (level == null) {
            return false;
        }
        RecipeManager manager = server.getRecipeManager();
        ContextMap ctx = SlotDisplayContext.fromLevel(level);
        Set<String> targets = new HashSet<>(CraftableCoralBlocksRecipes.RESULT_ITEM_IDS);
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.getType() != RecipeType.CRAFTING) {
                continue;
            }
            Identifier id = holder.id().identifier();
            if (CraftableCoralBlocksRecipes.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            try {
                java.util.List<RecipeDisplay> displays = recipe.display();
                if (displays == null || displays.isEmpty()) {
                    continue;
                }
                SlotDisplay resultDisplay = displays.get(0).result();
                if (resultDisplay == null) {
                    continue;
                }
                ItemStack result = resultDisplay.resolveForFirstStack(ctx);
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
                // 跳过无法提取产物的 recipe
            }
        }
        return false;
    }

    /**
     * 重新计算冲突锁定状态，并在状态迁移 / reload 仍锁定时广播提示与写日志。
     * 必须在 recipe book 同步与菜单刷新之前调用。
     *
     * <p>字段压 false / 恢复 desiredValue 均通过直接静态字段写完成，不经 SettingsManager，
     * 因此不触发 observer / validator / 配置保存，不修改 {@code carpet.conf}。Carpet
     * {@code /carpet} 查询走 {@code ParsedRule.value()} 实时反射读字段，故显示与字段一致。
     * 调用方（{@code onReload}）需在之后显式调用 {@link CraftableCoralBlocksRecipeBookHelper#onReload}
     * 完成配方书同步——直接字段写不触发 observer，不能依赖 observer 同步。
     *
     * <p>异常安全：{@link #detectConflict} 内部已 catch 单条 recipe 异常；若其本身抛出（不应发生），
     * 由调用方 try/catch 兜底，本方法不修改 conflictLocked / desiredValue / 字段，避免错误恢复或清空。
     */
    public static void recomputeAndNotify(MinecraftServer server) {
        boolean conflict = detectConflict(server);
        boolean wasLocked = CraftableCoralBlocksState.isConflictLocked();

        if (conflict) {
            if (!wasLocked) {
                // 新锁定：保存冲突前字段期望值，供解除后恢复
                CraftableCoralBlocksState.setDesiredValue(CraftableCoralBlocksSettings.craftableCoralBlocks);
            }
            // 直接字段写压 false（不触发 observer / 不保存 carpet.conf）
            CraftableCoralBlocksSettings.craftableCoralBlocks = false;
            CraftableCoralBlocksState.setConflictLocked(true);
            broadcast(server, "carpet.rule.craftableCoralBlocks.conflict.locked");
        } else if (wasLocked) {
            // 冲突解除：按 desiredValue 恢复字段，立即清空 desiredValue
            Boolean desired = CraftableCoralBlocksState.getDesiredValue();
            if (desired != null) {
                CraftableCoralBlocksSettings.craftableCoralBlocks = desired;
            }
            CraftableCoralBlocksState.setDesiredValue(null);
            CraftableCoralBlocksState.setConflictLocked(false);
            broadcast(server, "carpet.rule.craftableCoralBlocks.conflict.resolved");
        }
    }

    private static void broadcast(MinecraftServer server, String key) {
        String text = TranslationFormatUtil.translate(key);
        LOGGER.warn("[Carpet Ice Addition] {}", text);
        if (server.getPlayerList() != null) {
            Component msg = Component.literal(text);
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    /**
     * B2 重载 A：effective=false 且原返回命中本模组 coral recipe 时，遍历 {@link RecipeManager#getRecipes()}
     * 取一个 namespace 外部、对当前 input 真实 matches 的 recipe 返回。无则返回 empty。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> findExternalMatch(
            RecipeManager manager, RecipeType<T> type, I input, Level level) {
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.getType() != type) {
                continue;
            }
            Identifier id = holder.id().identifier();
            if (CraftableCoralBlocksRecipes.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            try {
                if (((Recipe) recipe).matches(input, level)) {
                    return Optional.of((RecipeHolder<T>) holder);
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
    public static Optional<RecipeHolder<CraftingRecipe>> findExternalCrafterMatch(ServerLevel level, CraftingInput input) {
        if (level == null) {
            return Optional.empty();
        }
        RecipeManager manager = (RecipeManager) level.recipeAccess();
        if (manager == null) {
            return Optional.empty();
        }
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.getType() != RecipeType.CRAFTING) {
                continue;
            }
            Identifier id = holder.id().identifier();
            if (CraftableCoralBlocksRecipes.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            try {
                if (((Recipe) recipe).matches(input, level)) {
                    return Optional.of((RecipeHolder<CraftingRecipe>) holder);
                }
            } catch (Throwable ignored) {
                // 跳过匹配异常的 recipe
            }
        }
        return Optional.empty();
    }
}
