package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksRecipe;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
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
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$craftableCoralBlocks(RecipeType<T> type, I input, Level level, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        carpetIceAddition$tryCraftableCoralBlocks(type, input, cir);
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$craftableCoralBlocksWithId(RecipeType<T> type, I input, Level level, ResourceKey<Recipe<?>> id, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        carpetIceAddition$tryCraftableCoralBlocks(type, input, cir);
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$craftableCoralBlocksWithEntry(RecipeType<T> type, I input, Level level, RecipeHolder<T> recipe, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        carpetIceAddition$tryCraftableCoralBlocks(type, input, cir);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <I extends RecipeInput, T extends Recipe<I>> void carpetIceAddition$tryCraftableCoralBlocks(RecipeType<T> type, I input, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        if (cir.getReturnValue().isPresent() || type != RecipeType.CRAFTING || !(input instanceof CraftingInput craftingInput)) {
            return;
        }

        Optional<RecipeHolder<CraftingRecipe>> recipe = CraftableCoralBlocksRecipe.match(craftingInput);
        recipe.ifPresent(entry -> cir.setReturnValue(Optional.of((RecipeHolder<T>) (RecipeHolder) entry)));
    }
}
