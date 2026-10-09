package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code calciteStonecuttingRecipe} 的冲突检测器（1.21.3 ~ 26.3）。
 *
 * <p>判定四条（任一成立即冲突）：
 * <ol>
 *   <li><b>异 id 同产物</b>：存在 recipe id ≠ 本模组的 {@code minecraft:stonecutting} 配方，结果含
 *       {@code minecraft:calcite}；</li>
 *   <li><b>同 id 内容不符</b>：本模组配方 id 存在，但其内容不满足规则承诺（类型 / 产物 / 数量 / 原料），
 *       说明该 id 被外部数据包覆盖；</li>
 *   <li><b>同 id 由外部提供</b>：本模组配方 id 存在，而本模组内置包当前未被选中；</li>
 *   <li>本模组内置包已选中但配方 id 缺失 → <b>仅告警</b>，不改锁状态（可能是加载中或包载入异常，
 *       避免把「加载中」误判为冲突）。</li>
 * </ol>
 *
 * <p>仅在静默点（世界加载完成 / 资源 reload 成功的 END 之后）调用，此二时点「选中集合 ↔ RecipeManager」
 * 一致，判定 3 不会把「刚取消选中、管理器尚未切换」误判为冲突。
 *
 * <p>边界（刻意不做的复杂化）：原料为「超集」（同时接受滴水石块与其它物品）不算冲突，因为规则承诺的
 * 能力仍然成立；结果的数据组件不做逐组件深比较，只按物品 id + 数量判定。
 */
public final class CalciteStonecuttingRecipeConflictDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private CalciteStonecuttingRecipeConflictDetector() {
    }

    /** 扫描 RecipeManager 判定是否存在冲突。 */
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
        Set<String> targets = new HashSet<>(CalciteStonecuttingRecipeData.RESULT_ITEM_IDS);

        boolean ownPresent = false;
        boolean ownAcceptable = false;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            Identifier id = holder.id().identifier();
            if (CalciteStonecuttingRecipeData.isOwnRecipe(id.getNamespace(), id.getPath())) {
                ownPresent = true;
                ownAcceptable = isOwnRecipeAcceptable(recipe, ctx);
                continue;
            }
            if (hasConflictingResult(recipe, ctx, targets)) {
                return true;
            }
        }

        // 2) 同 id 内容不符：被外部数据包覆盖成别的内容
        if (ownPresent && !ownAcceptable) {
            LOGGER.warn("[Carpet Ice Addition] Builtin calcite stonecutting recipe id is present but its content "
                    + "does not match the rule contract; treating it as a conflict");
            return true;
        }
        // 3) 同 id 由外部提供：本模组内置包未选中，但该 id 仍存在
        String packId = RecipeDatapackRegistry.CALCITE_STONECUTTING_PACK.toString();
        boolean packSelected = RecipePackCoordinator.isBuiltinPackSelected(server, packId);
        if (ownPresent && !packSelected) {
            LOGGER.warn("[Carpet Ice Addition] Calcite stonecutting recipe id is provided while our builtin pack "
                    + "is not selected; treating it as a conflict");
            return true;
        }
        // 4) 内置包已选中但 id 缺失：仅告警
        if (!ownPresent && packSelected) {
            LOGGER.warn("[Carpet Ice Addition] Calcite stonecutting builtin pack is selected but its recipe is "
                    + "absent from the recipe manager; lock state left unchanged");
        }
        return false;
    }

    /** 异 id 配方是否产出目标物品。 */
    private static boolean hasConflictingResult(Recipe<?> recipe, ContextMap ctx, Set<String> targets) {
        if (recipe.getType() != RecipeType.STONECUTTING) {
            return false;
        }
        for (ItemStack result : resolveResultStacks(recipe, ctx)) {
            Identifier resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId != null && targets.contains(resultId.toString())) {
                return true;
            }
        }
        return false;
    }

    /** 本模组配方 id 的内容是否符合规则承诺：切石类型 + 恰好 1 个方解石 + 原料接受滴水石块。 */
    private static boolean isOwnRecipeAcceptable(Recipe<?> recipe, ContextMap ctx) {
        boolean stonecuttingType = recipe.getType() == RecipeType.STONECUTTING;
        java.util.List<ItemStack> results = resolveResultStacks(recipe, ctx);
        if (results.size() != 1) {
            return false;
        }
        ItemStack result = results.get(0);
        Identifier resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        return CalciteStonecuttingRecipeData.ownRecipeAcceptable(
                stonecuttingType,
                resultId == null ? null : resultId.toString(),
                result.getCount(),
                acceptsDripstone(recipe));
    }

    /** 原料是否接受滴水石块（覆盖「原料被改、类型与产物不变」的覆盖场景）。 */
    private static boolean acceptsDripstone(Recipe<?> recipe) {
        if (!(recipe instanceof SingleItemRecipe singleItemRecipe)) {
            return false;
        }
        try {
            Ingredient ingredient = singleItemRecipe.input();
            return ingredient != null && ingredient.test(new ItemStack(Items.DRIPSTONE_BLOCK));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 汇总配方全部 display 解析出的非空结果栈；单条 display 异常只跳过自身。 */
    private static java.util.List<ItemStack> resolveResultStacks(Recipe<?> recipe, ContextMap ctx) {
        java.util.List<ItemStack> results = new ArrayList<>();
        try {
            java.util.List<RecipeDisplay> displays = recipe.display();
            if (displays == null) {
                return results;
            }
            for (RecipeDisplay display : displays) {
                try {
                    SlotDisplay resultDisplay = display.result();
                    if (resultDisplay == null) {
                        continue;
                    }
                    java.util.List<ItemStack> resolved = resultDisplay.resolveForStacks(ctx);
                    if (resolved == null) {
                        continue;
                    }
                    for (ItemStack stack : resolved) {
                        if (stack != null && !stack.isEmpty()) {
                            results.add(stack);
                        }
                    }
                } catch (Throwable ignored) {
                    // 跳过无法解析的 display
                }
            }
        } catch (Throwable ignored) {
            // 跳过无法提取产物的 recipe
        }
        return results;
    }

    /**
     * 重新计算冲突锁定状态，并在状态迁移时广播提示与写日志。
     *
     * <p>字段压 false / 恢复 desiredValue 均通过直接静态字段写完成，不经 SettingsManager，
     * 因此不触发 observer / validator / 配置保存，不修改 {@code carpet.conf}。
     * 调用方（静默点）会在之后执行菜单同步——直接字段写不触发 observer，不能依赖 observer 同步。
     */
    public static void recomputeAndNotify(MinecraftServer server) {
        boolean conflict = detectConflict(server);
        boolean wasLocked = CalciteStonecuttingRecipeState.isConflictLocked();

        if (conflict) {
            if (!wasLocked) {
                // 新锁定：保存冲突前字段期望值，供解除后恢复
                CalciteStonecuttingRecipeState.setDesiredValue(CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe);
            }
            CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe = false;
            CalciteStonecuttingRecipeState.setConflictLocked(true);
            if (!wasLocked) {
                broadcast(server, "carpet.rule.calciteStonecuttingRecipe.conflict.locked");
            }
        } else if (wasLocked) {
            Boolean desired = CalciteStonecuttingRecipeState.getDesiredValue();
            if (desired != null) {
                CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe = desired;
            }
            CalciteStonecuttingRecipeState.setDesiredValue(null);
            CalciteStonecuttingRecipeState.setConflictLocked(false);
            broadcast(server, "carpet.rule.calciteStonecuttingRecipe.conflict.resolved");
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
}
