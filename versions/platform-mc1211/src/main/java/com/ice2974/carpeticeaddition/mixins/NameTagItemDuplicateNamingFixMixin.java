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
                // 原版 1.21.1 useOnEntity 成功时返回 ActionResult.success(isClient)，
                // 服务端侧即 ActionResult.CONSUME；取消重复命名时保持与服务端原版语义一致，
                // 避免 ServerPlayNetworkHandler 因 SUCCESS 的 shouldSwingHand() 补播挥手。
                // 1.21.2+ 原版改为恒返回 SUCCESS，故 shared 档（mc1213-12111 / mc26x）返回 SUCCESS，勿跨版本统一。
                cir.setReturnValue(InteractionResult.CONSUME);
            }
        }
    }
}
