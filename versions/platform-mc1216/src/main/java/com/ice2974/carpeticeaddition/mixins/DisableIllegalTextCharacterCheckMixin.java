package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringUtil.class)
public abstract class DisableIllegalTextCharacterCheckMixin {

    @Inject(method = "isAllowedChatCharacter(C)Z", at = @At("HEAD"), cancellable = true)
    private static void carpetIceAddition$disableIllegalTextCharacterCheck(
            char character,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Keep vanilla CR rejection so every clipboard path retains its CR cleanup.
        if (CarpetIceAdditionSettings.disableIllegalTextCharacterCheck && character != '\r') {
            cir.setReturnValue(true);
        }
    }

}
