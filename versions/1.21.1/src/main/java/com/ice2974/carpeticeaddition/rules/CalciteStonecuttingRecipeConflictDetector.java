package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * {@code calciteStonecuttingRecipe} 的冲突检测器（1.21.1 平台 override）。
 *
 * <p>与 root 版语义一致，仅替换 1.21.1 的 API 差异：
 * <ul>
 *   <li>标识符类型为 {@link ResourceLocation}，{@code RecipeHolder.id()} 直接返回该类型；</li>
 *   <li>1.21.1 尚无配方 display API，产物通过 {@code Recipe.getResultItem(HolderLookup.Provider)} 读取；</li>
 *   <li>1.21.1 的 {@code SingleItemRecipe.ingredient} 是 {@code protected}（跨包不可读），
 *       改用公共的 {@code Recipe.getIngredients().get(0)}（该方法自 1.21.3 起已被移除）。</li>
 * </ul>
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
        RecipeManager manager = server.getRecipeManager();
        HolderLookup.Provider lookup = server.registryAccess();
        RecipeConflictDefinition definition = CalciteStonecuttingRecipeData.CONFLICT;
        Set<String> targets = definition.targets();

        boolean[] ownPresent = {false};
        boolean[] ownAcceptable = {false};
        boolean conflict = RecipeConflictScanner.scan(manager.getRecipes(), definition,
                entry -> true, entry -> entry.id().toString(), entry -> {
                    ownPresent[0] = true;
                    ownAcceptable[0] = isOwnRecipeAcceptable(entry.value(), lookup);
                }, entry -> hasConflictingResult(entry.value(), entry.id().toString(), lookup, targets));
        if (conflict) return true;

        if (ownPresent[0] && !ownAcceptable[0]) {
            LOGGER.warn("[Carpet Ice Addition] Builtin calcite stonecutting recipe id is present but its content "
                    + "does not match the rule contract; treating it as a conflict");
            return true;
        }
        String packId = RecipeDatapackRegistry.CALCITE_STONECUTTING_PACK.toString();
        boolean packSelected = RecipePackCoordinator.isBuiltinPackSelected(server, packId);
        if (ownPresent[0] && !packSelected) {
            LOGGER.warn("[Carpet Ice Addition] Calcite stonecutting recipe id is provided while our builtin pack "
                    + "is not selected; treating it as a conflict");
            return true;
        }
        if (!ownPresent[0] && packSelected) {
            LOGGER.warn("[Carpet Ice Addition] Calcite stonecutting builtin pack is selected but its recipe is "
                    + "absent from the recipe manager; treating absence as no conflict");
        }
        return false;
    }

    /**
     * 异 id 配方是否产出目标物品（包私有以供单测直接驱动）。
     *
     * <p>fail-closed：切石配方的产物解析抛异常时计为冲突——「无法判定」不得等同于「无冲突」，
     * 与 root 版语义一致。产物为 null / 空不计为冲突（合法场景）。
     */
    static boolean hasConflictingResult(Recipe<?> recipe, String recipeId, HolderLookup.Provider lookup, Set<String> targets) {
        return StonecuttingRecipeConflictScanner.hasConflictingResult(recipe, recipeId, lookup, CalciteStonecuttingRecipeData.CONFLICT, targets);
    }

    private static boolean isOwnRecipeAcceptable(Recipe<?> recipe, HolderLookup.Provider lookup) {
        boolean stonecuttingType = recipe.getType() == RecipeType.STONECUTTING;
        ItemStack result = StonecuttingRecipeConflictScanner.resolveResult(recipe, lookup).stack();
        if (result == null || result.isEmpty()) {
            return false;
        }
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        return CalciteStonecuttingRecipeData.ownRecipeAcceptable(
                stonecuttingType,
                resultId == null ? null : resultId.toString(),
                result.getCount(),
                acceptsDripstone(recipe));
    }

    private static boolean acceptsDripstone(Recipe<?> recipe) {
        try {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            if (ingredients.isEmpty()) {
                return false;
            }
            Ingredient ingredient = ingredients.get(0);
            return ingredient != null && ingredient.test(new ItemStack(Items.DRIPSTONE_BLOCK));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 撤销前判定：该 holder 的内容是否仍是本规则承诺（1.21.1 平台 override 版）。
     *
     * <p>与 root 版语义一致，仅把上下文来源换成 1.21.1 的 {@code HolderLookup.Provider}；无法判定
     * 一律返回 false ⇒ 调用方不得撤销。
     */
    static boolean matchesRuleContract(MinecraftServer server, RecipeHolder<?> holder) {
        if (server == null || holder == null) {
            return false;
        }
        try {
            return isOwnRecipeAcceptable(holder.value(), server.registryAccess());
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 重新计算冲突锁定状态，并在状态迁移时广播提示与写日志。 */
    public static void recomputeAndNotify(MinecraftServer server) {
        RecipeConflictLock.Transition transition = CalciteStonecuttingRecipeState.LOCK.apply(
                detectConflict(server) ? RecipeConflictLock.Detection.CONFLICT : RecipeConflictLock.Detection.NO_CONFLICT,
                () -> CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe, value -> CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe = value);
        if (transition == RecipeConflictLock.Transition.LOCKED) {
            broadcast(server, "carpet.rule.calciteStonecuttingRecipe.conflict.locked");
        } else if (transition == RecipeConflictLock.Transition.UNLOCKED) {
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
