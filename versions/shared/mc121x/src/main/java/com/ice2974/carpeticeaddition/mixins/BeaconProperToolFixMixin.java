package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class BeaconProperToolFixMixin {

    @ModifyReturnValue(
            method = "getMiningSpeedMultiplier",
            at = @At("RETURN")
    )
    private float carpetIceAddition$beaconProperToolFix(float original, BlockState state) {
        if (!CarpetIceAdditionSettings.beaconProperToolFix) {
            return original;
        }

        try {
            ItemStack self = (ItemStack) (Object) this;
            if (state.isOf(Blocks.BEACON) && self.isIn(ItemTags.PICKAXES)) {
                float stoneSpeed = self.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState());
                return Math.max(original, stoneSpeed);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("beaconProperToolFix", throwable);
        }
        return original;
    }
}
