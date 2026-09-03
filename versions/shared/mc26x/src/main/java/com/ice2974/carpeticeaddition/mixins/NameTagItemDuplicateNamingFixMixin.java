package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public abstract class NameTagItemDuplicateNamingFixMixin {

    @Inject(
            method = "interactLivingEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setCustomName(Lnet/minecraft/network/chat/Component;)V"
            ),
            cancellable = true
    )
    private void carpetIceAddition$preventDuplicateNaming(
            ItemStack stack,
            Player user,
            LivingEntity entity,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!CarpetIceAdditionSettings.nameTagDuplicateNamingFix) {
            return;
        }

        Component tagCustomName = stack.get(DataComponents.CUSTOM_NAME);
        if (tagCustomName != null && entity.hasCustomName()) {
            Component currentCustomName = entity.getCustomName();
            if (tagCustomName.equals(currentCustomName)) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}
