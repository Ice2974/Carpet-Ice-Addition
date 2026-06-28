package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class FrostedIceProperToolFixMixin {

    @Inject(
            method = "getMiningSpeed(Lnet/minecraft/item/ItemStack;Lnet/minecraft/block/BlockState;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carpetIceAddition$treatFrostedIceLikeIce(
            ItemStack stack,
            BlockState state,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!CarpetIceAdditionSettings.frostedIceProperToolFix) {
            return;
        }

        try {
            if (state.isOf(Blocks.FROSTED_ICE) && stack.isIn(ItemTags.PICKAXES)) {
                cir.setReturnValue(((Item) (Object) this).getMiningSpeed(stack, Blocks.ICE.getDefaultState()));
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("frostedIceProperToolFix", throwable);
        }
    }
}
