package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.CrafterOutputBlockHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.enums.Orientation;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
    @Shadow @Final private static EnumProperty<Orientation> ORIENTATION;

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$cancelCraftIfOutputBlocked(BlockState state, ServerWorld world, BlockPos pos, CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.crafterStopsWhenOutputBlocked || !CarpetIceAdditionMod.shouldEnableCrafterOutputBlockRule()) {
            return;
        }

        try {
            if (!(world.getBlockEntity(pos) instanceof CrafterBlockEntity crafter)) {
                return;
            }

            CraftingRecipeInput input = crafter.createRecipeInput();
            Optional<RecipeEntry<CraftingRecipe>> recipe = CrafterBlock.getCraftingRecipe(world, input);
            if (recipe.isEmpty()) {
                return;
            }

            ItemStack result = recipe.get().value().craft(input, world.getRegistryManager());
            if (result.isEmpty()) {
                return;
            }

            Direction outputDirection = state.get(ORIENTATION).getFacing();
            Inventory target = HopperBlockEntity.getInventoryAt(world, pos.offset(outputDirection));
            if (target == null) {
                return;
            }

            List<ItemStack> plannedOutputs = new ArrayList<>();
            plannedOutputs.add(result);
            DefaultedList<ItemStack> remainders = recipe.get().value().getRecipeRemainders(input);
            for (ItemStack remainder : remainders) {
                if (!remainder.isEmpty()) {
                    plannedOutputs.add(remainder);
                }
            }

            if (!CrafterOutputBlockHelper.canFullyInsertAll(target, plannedOutputs, outputDirection.getOpposite())) {
                ci.cancel();
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("crafterStopsWhenOutputBlocked", throwable);
        }
    }
}
