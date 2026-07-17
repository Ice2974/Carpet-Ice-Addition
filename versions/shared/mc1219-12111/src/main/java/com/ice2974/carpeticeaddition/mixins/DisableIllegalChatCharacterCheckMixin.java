package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.util.StringHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringHelper.class)
public abstract class DisableIllegalChatCharacterCheckMixin {

    @Inject(method = "isValidChar(I)Z", at = @At("HEAD"), cancellable = true)
    private static void carpetIceAddition$disableIllegalChatCharacterCheck(
            int codePoint,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CarpetIceAdditionSettings.disableIllegalChatCharacterCheck) {
            cir.setReturnValue(true);
        }
    }

}
