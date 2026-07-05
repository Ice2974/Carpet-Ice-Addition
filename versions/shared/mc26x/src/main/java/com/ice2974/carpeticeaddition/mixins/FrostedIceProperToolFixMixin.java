package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class FrostedIceProperToolFixMixin {

    @Inject(
            method = "getDestroySpeed(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)F",
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
            if (state.getBlock() == Blocks.FROSTED_ICE && stack.is(ItemTags.PICKAXES)) {
                cir.setReturnValue(((Item) (Object) this).getDestroySpeed(stack, Blocks.ICE.defaultBlockState()));
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("frostedIceProperToolFix", throwable);
        }
    }
}
