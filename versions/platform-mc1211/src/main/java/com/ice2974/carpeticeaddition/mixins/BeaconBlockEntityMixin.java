package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;getOpacity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)I"
            ),
            require = 1
    )
    private static int carpetIceAddition$beaconIgnoresObstruction(BlockState state, BlockView world, BlockPos pos, Operation<Integer> original) {
        if (CarpetIceAdditionSettings.beaconIgnoresObstruction) {
            return 0;
        }
        return original.call(state, world, pos);
    }
}
