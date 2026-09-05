package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.CrafterOutputBlockHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
    @Shadow @Final private static EnumProperty<FrontAndTop> ORIENTATION;

    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$cancelCraftIfOutputBlocked(BlockState state, ServerLevel world, BlockPos pos, CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.crafterStopsWhenOutputBlocked) {
            return;
        }

        try {
            if (!(world.getBlockEntity(pos) instanceof CrafterBlockEntity crafter)) {
                return;
            }

            CraftingInput input = crafter.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> recipe = CrafterBlock.getPotentialResults(world, input);
            if (recipe.isEmpty()) {
                return;
            }

            ItemStack result = recipe.get().value().assemble(input, world.registryAccess());
            if (result.isEmpty()) {
                return;
            }

            Direction outputDirection = state.getValue(ORIENTATION).front();
            Container target = HopperBlockEntity.getContainerAt(world, pos.relative(outputDirection));
            if (target == null) {
                return;
            }

            if (!CrafterOutputBlockHelper.canFullyInsertAll(target, List.of(result), outputDirection.getOpposite())) {
                ci.cancel();
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("crafterStopsWhenOutputBlocked", throwable);
        }
    }
}
