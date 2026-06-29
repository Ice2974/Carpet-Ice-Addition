package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksRecipes;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerRecipeManager.class)
public abstract class ServerRecipeManagerCraftableCoralBlocksMixin {
    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoral(RecipeType<T> type, I input, World world, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$filterCoral(cir);
    }

    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;Lnet/minecraft/registry/RegistryKey;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoralWithId(RecipeType<T> type, I input, World world, RegistryKey<Recipe<?>> id, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$filterCoral(cir);
    }

    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;Lnet/minecraft/recipe/RecipeEntry;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoralWithEntry(RecipeType<T> type, I input, World world, RecipeEntry<T> recipe, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        carpetIceAddition$filterCoral(cir);
    }

    private static <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoral(CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        if (CarpetIceAdditionSettings.craftableCoralBlocks) {
            return;
        }
        cir.getReturnValue().ifPresent(entry -> {
            if (CraftableCoralBlocksRecipes.isCoralRecipeId(entry.id().getValue().toString())) {
                cir.setReturnValue(Optional.empty());
            }
        });
    }
}
