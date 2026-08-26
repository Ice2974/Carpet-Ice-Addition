package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class BeaconProperToolFixMixin {

    @Inject(
            method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carpetIceAddition$beaconProperToolFix(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (!CarpetIceAdditionSettings.beaconProperToolFix) {
            return;
        }

        try {
            ItemStack self = (ItemStack) (Object) this;
            if (state.getBlock() == Blocks.BEACON && self.is(ItemTags.PICKAXES)) {
                cir.setReturnValue(self.getDestroySpeed(Blocks.STONE.defaultBlockState()));
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("beaconProperToolFix", throwable);
        }
    }
}
