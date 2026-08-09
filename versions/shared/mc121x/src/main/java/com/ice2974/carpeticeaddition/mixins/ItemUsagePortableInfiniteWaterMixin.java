package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemUsage.class)
public abstract class ItemUsagePortableInfiniteWaterMixin {
    @Inject(
            method = "exchangeStack(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void carpetIceAddition$keepVanillaWaterBucket(
            ItemStack inputStack,
            PlayerEntity player,
            ItemStack replacementStack,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!CarpetIceAdditionSettings.portableInfiniteWater || player.isInCreativeMode()) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        if (inputStack.getItem() == Items.WATER_BUCKET
                && replacementStack.getItem() == Items.BUCKET
                && mainHandStack.getItem() == Items.WATER_BUCKET
                && offHandStack.getItem() == Items.WATER_BUCKET
                && (inputStack == mainHandStack || inputStack == offHandStack)) {
            cir.setReturnValue(inputStack);
        }
    }
}
