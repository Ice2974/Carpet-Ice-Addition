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

import java.util.HashSet;
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
        Set<String> targets = new HashSet<>(CalciteStonecuttingRecipeData.RESULT_ITEM_IDS);

        boolean ownPresent = false;
        boolean ownAcceptable = false;
        for (RecipeHolder<?> entry : manager.getRecipes()) {
            Recipe<?> recipe = entry.value();
            ResourceLocation id = entry.id();
            if (CalciteStonecuttingRecipeData.isOwnRecipe(id.getNamespace(), id.getPath())) {
                ownPresent = true;
                ownAcceptable = isOwnRecipeAcceptable(recipe, lookup);
                continue;
            }
            if (hasConflictingResult(recipe, lookup, targets)) {
                return true;
            }
        }

        if (ownPresent && !ownAcceptable) {
            LOGGER.warn("[Carpet Ice Addition] Builtin calcite stonecutting recipe id is present but its content "
                    + "does not match the rule contract; treating it as a conflict");
            return true;
        }
        String packId = RecipeDatapackRegistry.CALCITE_STONECUTTING_PACK.toString();
        boolean packSelected = RecipePackCoordinator.isBuiltinPackSelected(server, packId);
        if (ownPresent && !packSelected) {
            LOGGER.warn("[Carpet Ice Addition] Calcite stonecutting recipe id is provided while our builtin pack "
                    + "is not selected; treating it as a conflict");
            return true;
        }
        if (!ownPresent && packSelected) {
            LOGGER.warn("[Carpet Ice Addition] Calcite stonecutting builtin pack is selected but its recipe is "
                    + "absent from the recipe manager; lock state left unchanged");
        }
        return false;
    }

    private static boolean hasConflictingResult(Recipe<?> recipe, HolderLookup.Provider lookup, Set<String> targets) {
        if (recipe.getType() != RecipeType.STONECUTTING) {
            return false;
        }
        ItemStack result = resolveResultStack(recipe, lookup);
        if (result == null || result.isEmpty()) {
            return false;
        }
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        return resultId != null && targets.contains(resultId.toString());
    }

    private static boolean isOwnRecipeAcceptable(Recipe<?> recipe, HolderLookup.Provider lookup) {
        boolean stonecuttingType = recipe.getType() == RecipeType.STONECUTTING;
        ItemStack result = resolveResultStack(recipe, lookup);
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

    private static ItemStack resolveResultStack(Recipe<?> recipe, HolderLookup.Provider lookup) {
        try {
            return recipe.getResultItem(lookup);
        } catch (Throwable ignored) {
            return null;
        }
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
        boolean conflict = detectConflict(server);
        boolean wasLocked = CalciteStonecuttingRecipeState.isConflictLocked();

        if (conflict) {
            if (!wasLocked) {
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
