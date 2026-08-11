package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects conflicting external crafting recipes for craftableCoralBlocks.
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
            if (CraftableCoralBlocksRecipes.isCoralRecipe(id.getNamespace(), id.getPath())) {
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

}
