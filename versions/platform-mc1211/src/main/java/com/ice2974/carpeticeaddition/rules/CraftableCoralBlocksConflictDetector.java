package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * craftableCoralBlocks 外部配方冲突检测与外部替代 recipe 查找（1.21.1 yarn）。
 *
 * <p>冲突判定：RecipeManager 中存在非本模组 10 个内置 recipe ID 的 crafting 配方，
 * 其输出产物 Item 与本模组自带 10 条 coral recipe 的某个目标产物一致（仅比较输出产物，不比较
 * recipe id / 输入 / pattern）。目标产物集合以 {@link CraftableCoralBlocksRecipes#RESULT_ITEM_IDS}
 * 为权威来源，不依赖运行期本模组 recipe 是否成功注册。
 *
 * <p>检测范围限定 {@link RecipeType#CRAFTING}：stonecutting / smelting / custom recipe type 等不触发
 * 锁定（craftableCoralBlocks 仅控制 crafting table / Crafter 合成路径）。
 *
 * <p>外部替代查找：用于 B2 重载 A（普通 getFirstMatch）与 B3（Crafter）—— 在 effective=false 且原返回
 * 命中本模组 coral recipe 时，改返回一个 namespace 外部、且对当前 input 真实 matches 的 recipe；
 * 无外部真实匹配时返回 empty。
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
        RecipeManager manager = server.getRecipeManager();
        RegistryWrapper.WrapperLookup lookup = server.getRegistryManager();
        Set<String> targets = new HashSet<>(CraftableCoralBlocksRecipes.RESULT_ITEM_IDS);
        for (RecipeEntry<?> entry : manager.values()) {
            Recipe<?> recipe = entry.value();
            if (recipe.getType() != RecipeType.CRAFTING) {
                continue;
            }
            Identifier id = entry.id();
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
                continue;
            }
            try {
                ItemStack result = recipe.getResult(lookup);
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
                // 跳过无法提取产物的 recipe，避免单条异常导致整体检测失败
            }
        }
        return false;
    }

    /**
     * 重新计算冲突锁定状态，并在状态迁移 / reload 仍锁定时广播提示与写日志。
     *
     * <p>必须在 recipe book 同步与菜单刷新之前调用。字段压 false / 恢复 desiredValue 均通过
     * 直接静态字段写完成，不经 SettingsManager，因此不触发 observer / validator / 配置保存，
     * 不修改 {@code carpet.conf}。Carpet {@code /carpet} 查询走 {@code ParsedRule.value()}
     * 实时反射读字段，故显示与字段一致。调用方需在之后显式调用
     * {@link CraftableCoralBlocksRecipeBookHelper} 完成同步——直接字段写不触发 observer。
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
            if (!wasLocked) {
                broadcast(server, "carpet.rule.craftableCoralBlocks.conflict.locked");
            }
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
        if (server.getPlayerManager() != null) {
            Text msg = Text.literal(text);
            server.getPlayerManager().broadcast(msg, false);
        }
    }

    /**
     * B2 重载 A：在 effective=false 且原返回命中本模组 coral recipe 时，从 {@link RecipeManager#getAllMatches}
     * 结果中取一个 namespace 外部的真实匹配返回。无则返回 empty。
     */
    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> findExternalMatch(
            RecipeManager manager, RecipeType<T> type, I input, World world) {
        List<RecipeEntry<T>> all = manager.getAllMatches(type, input, world);
        for (RecipeEntry<T> entry : all) {
            if (!CraftableCoralBlocksRecipes.isCoralRecipe(entry.id().getNamespace(), entry.id().getPath())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * B3 Crafter：在 effective=false 且原返回命中本模组 coral recipe 时，取一个 namespace 外部、对当前
     * input 真实 matches 的 crafting recipe 返回。无则返回 empty。
     */
    public static Optional<RecipeEntry<net.minecraft.recipe.CraftingRecipe>> findExternalCrafterMatch(
            World world, CraftingRecipeInput input) {
        RecipeManager manager = world.getRecipeManager();
        List<RecipeEntry<net.minecraft.recipe.CraftingRecipe>> all =
                manager.getAllMatches(RecipeType.CRAFTING, input, world);
        for (RecipeEntry<net.minecraft.recipe.CraftingRecipe> entry : all) {
            if (!CraftableCoralBlocksRecipes.isCoralRecipe(entry.id().getNamespace(), entry.id().getPath())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
}
