package com.ice2974.carpeticeaddition.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.client.gui.font.TextFieldHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextFieldHelper.class)
public abstract class DisableIllegalChatCharacterCheckClipboardMixin {

    @WrapOperation(
            method = "getClipboardContents(Lnet/minecraft/client/Minecraft;)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/ChatFormatting;stripFormatting(Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private static String carpetIceAddition$keepIllegalClipboardCharacters(
            String clipboard,
            Operation<String> original
    ) {
        if (CarpetIceAdditionSettings.disableIllegalChatCharacterCheck) {
            return clipboard;
        }
        return original.call(clipboard);
    }
}
