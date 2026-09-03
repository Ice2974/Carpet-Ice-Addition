package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NameTagItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public abstract class NameTagItemDuplicateNamingFixMixin {

    @Inject(
            method = "useOnEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setCustomName(Lnet/minecraft/text/Text;)V"
            ),
            cancellable = true
    )
    private void carpetIceAddition$preventDuplicateNaming(
            ItemStack stack,
            PlayerEntity user,
            LivingEntity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!CarpetIceAdditionSettings.nameTagDuplicateNamingFix) {
            return;
        }

        Text tagCustomName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (tagCustomName != null && entity.hasCustomName()) {
            Text currentCustomName = entity.getCustomName();
            if (tagCustomName.equals(currentCustomName)) {
                // 原版 1.21.1 useOnEntity 成功时返回 ActionResult.success(isClient)，
                // 服务端侧即 ActionResult.CONSUME；取消重复命名时保持与服务端原版语义一致，
                // 避免 ServerPlayNetworkHandler 因 SUCCESS 的 shouldSwingHand() 补播挥手。
                // 1.21.2+ 原版改为恒返回 SUCCESS，故 shared 档（mc1213-12111 / mc26x）返回 SUCCESS，勿跨版本统一。
                cir.setReturnValue(ActionResult.CONSUME);
            }
        }
    }
}
