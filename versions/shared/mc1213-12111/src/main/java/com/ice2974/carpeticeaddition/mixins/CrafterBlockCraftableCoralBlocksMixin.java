package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksConflictDetector;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksRecipes;
import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.minecraft.block.CrafterBlock;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * B3 兜底：Crafter 的 {@code RecipeCache} 命中后不再经过 {@code ServerRecipeManager.getFirstMatch}，
 * 因此 B2 兜不住 Crafter 的重复合成。此处注入 {@link CrafterBlock#getCraftingRecipe} 的 RETURN，
 * effective=false 且原返回命中本模组 coral recipe 时，改返回一个对当前 input 真实 matches 的外部
 * crafting recipe；无外部真实匹配才返回 empty。绝不因外部 recipe 输出产物相同但输入不同而返回它。
 */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockCraftableCoralBlocksMixin {
    @Inject(method = "getCraftingRecipe(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/recipe/input/CraftingRecipeInput;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private static void carpetIceAddition$filterCoral(ServerWorld world, CraftingRecipeInput input, CallbackInfoReturnable<Optional<RecipeEntry<CraftingRecipe>>> cir) {
        if (CraftableCoralBlocksSettings.effective()) {
            return;
        }
        Optional<RecipeEntry<CraftingRecipe>> ret = cir.getReturnValue();
        if (ret.isEmpty()) {
            return;
        }
        if (!CraftableCoralBlocksRecipes.isCoralRecipeId(ret.get().id().getValue().toString())) {
            return;
        }
        cir.setReturnValue(CraftableCoralBlocksConflictDetector.findExternalCrafterMatch(world, input));
    }
}
