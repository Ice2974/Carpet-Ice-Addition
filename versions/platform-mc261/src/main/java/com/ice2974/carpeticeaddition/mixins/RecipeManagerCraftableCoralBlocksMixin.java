package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksRecipes;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerCraftableCoralBlocksMixin {
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoral(RecipeType<T> type, I input, Level level, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        carpetIceAddition$filterCoral(cir);
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoralWithId(RecipeType<T> type, I input, Level level, ResourceKey<Recipe<?>> id, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        carpetIceAddition$filterCoral(cir);
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoralWithHolder(RecipeType<T> type, I input, Level level, RecipeHolder<T> recipe, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        carpetIceAddition$filterCoral(cir);
    }

    private static <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$filterCoral(CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        if (CarpetIceAdditionSettings.craftableCoralBlocks) {
            return;
        }
        cir.getReturnValue().ifPresent(holder -> {
            if (CraftableCoralBlocksRecipes.isCoralRecipeId(holder.id().identifier().toString())) {
                cir.setReturnValue(Optional.empty());
            }
        });
    }
}
