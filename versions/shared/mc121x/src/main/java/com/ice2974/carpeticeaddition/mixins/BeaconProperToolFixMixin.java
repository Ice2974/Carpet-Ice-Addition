package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class BeaconProperToolFixMixin {

    @Inject(
            method = "getMiningSpeedMultiplier(Lnet/minecraft/block/BlockState;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carpetIceAddition$beaconProperToolFix(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (!CarpetIceAdditionSettings.beaconProperToolFix) {
            return;
        }

        try {
            ItemStack self = (ItemStack) (Object) this;
            if (state.isOf(Blocks.BEACON) && self.isIn(ItemTags.PICKAXES)) {
                cir.setReturnValue(self.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState()));
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("beaconProperToolFix", throwable);
        }
    }
}
