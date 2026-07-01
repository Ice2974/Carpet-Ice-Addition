package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksConflictDetector;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksRecipes;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksState;
import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerCraftableCoralBlocksMixin {
    /**
     * 重载 A（普通查询，用于"找一个能合成的配方"）：effective=false 且原返回命中本模组 coral recipe 时，
     * 改返回一个 namespace 外部的真实匹配，使外部同产物配方继续生效。无外部匹配才返回 empty。
     */
    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoral(RecipeType<T> type, I input, World world, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        if (CraftableCoralBlocksSettings.effective()) {
            return;
        }
        Optional<RecipeEntry<T>> ret = cir.getReturnValue();
        if (ret.isEmpty()) {
            return;
        }
        Identifier id = ret.get().id();
        if (!CraftableCoralBlocksRecipes.isCoralRecipeId(id.toString())) {
            return;
        }
        // 原返回命中本模组 coral recipe 且 effective=false：尝试找外部替代
        RecipeManager self = (RecipeManager) (Object) this;
        cir.setReturnValue(CraftableCoralBlocksConflictDetector.findExternalMatch(self, type, input, world));
    }

    /**
     * 重载 B（带指定 id）：指定 id 查询语义是校验该 id 配方是否匹配，不应改返回别的 id。
     * 若目标是本模组 coral recipe 且 effective=false，返回 empty。
     */
    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;Lnet/minecraft/util/Identifier;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoralWithId(RecipeType<T> type, I input, World world, Identifier id, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$filterCoralNoSubstitute(cir);
    }

    /**
     * 重载 C（带指定 entry）：同重载 B，不找替代，命中本模组 coral recipe 且 effective=false 时返回 empty。
     */
    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;Lnet/minecraft/recipe/RecipeEntry;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoralWithEntry(RecipeType<T> type, I input, World world, RecipeEntry<T> recipe, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$filterCoralNoSubstitute(cir);
    }

    private static <T extends Recipe<?>> void carpetIceAddition$filterCoralNoSubstitute(CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        if (CraftableCoralBlocksSettings.effective()) {
            return;
        }
        cir.getReturnValue().ifPresent(entry -> {
            if (CraftableCoralBlocksRecipes.isCoralRecipeId(entry.id().toString())) {
                cir.setReturnValue(Optional.empty());
            }
        });
    }
}
