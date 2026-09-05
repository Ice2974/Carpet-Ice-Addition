//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemUtils.class)
public abstract class ItemUsagePortableInfiniteWaterMixin {
    @Inject(
            method = "createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void carpetIceAddition$keepVanillaWaterBucket(
            ItemStack inputStack,
            Player player,
            ItemStack replacementStack,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!CarpetIceAdditionSettings.portableInfiniteWater || player.hasInfiniteMaterials()) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();
        if (inputStack.getItem() == Items.WATER_BUCKET
                && replacementStack.getItem() == Items.BUCKET
                && mainHandStack.getItem() == Items.WATER_BUCKET
                && offHandStack.getItem() == Items.WATER_BUCKET
                && (inputStack == mainHandStack || inputStack == offHandStack)) {
            cir.setReturnValue(inputStack);
        }
    }
}
//#endif
