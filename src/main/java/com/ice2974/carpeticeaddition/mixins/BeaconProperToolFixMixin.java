package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class BeaconProperToolFixMixin {

    @ModifyReturnValue(
            method = "getDestroySpeed",
            at = @At("RETURN")
    )
    private float carpetIceAddition$beaconProperToolFix(float original, BlockState state) {
        if (!CarpetIceAdditionSettings.beaconProperToolFix) {
            return original;
        }

        try {
            ItemStack self = (ItemStack) (Object) this;
//#if MC<260000
            if (state.is(Blocks.BEACON) && self.is(ItemTags.PICKAXES)) {
//#else
//$$            if (state.getBlock() == Blocks.BEACON && self.is(ItemTags.PICKAXES)) {
//#endif
                float stoneSpeed = self.getDestroySpeed(Blocks.STONE.defaultBlockState());
                return Math.max(original, stoneSpeed);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("beaconProperToolFix", throwable);
        }
        return original;
    }
}
