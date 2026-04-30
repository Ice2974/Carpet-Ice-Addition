package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksRecipe;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
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
    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$craftableCoralBlocks(RecipeType<T> type, I input, World world, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$tryCraftableCoralBlocks(type, input, cir);
    }

    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;Lnet/minecraft/util/Identifier;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$craftableCoralBlocksWithId(RecipeType<T> type, I input, World world, Identifier id, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$tryCraftableCoralBlocks(type, input, cir);
    }

    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;Lnet/minecraft/recipe/RecipeEntry;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$craftableCoralBlocksWithEntry(RecipeType<T> type, I input, World world, RecipeEntry<T> recipe, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$tryCraftableCoralBlocks(type, input, cir);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$tryCraftableCoralBlocks(RecipeType<T> type, I input, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        if (cir.getReturnValue().isPresent() || type != RecipeType.CRAFTING || !(input instanceof CraftingRecipeInput craftingInput)) {
            return;
        }

        Optional<RecipeEntry<CraftingRecipe>> recipe = CraftableCoralBlocksRecipe.match(craftingInput);
        recipe.ifPresent(entry -> cir.setReturnValue(Optional.of((RecipeEntry<T>) (RecipeEntry) entry)));
    }
}
