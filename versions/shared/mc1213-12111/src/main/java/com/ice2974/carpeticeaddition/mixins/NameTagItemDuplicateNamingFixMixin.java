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
                // 原版 1.21.2+ useOnEntity 成功恒返回 SUCCESS（ActionResult 重构后 SwingSource.CLIENT，服务端不补挥手）；
                // 1.21.1 因原版服务端成功为 CONSUME 而由 platform-mc1211 单独实现，勿跨版本统一返回值。
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
}
