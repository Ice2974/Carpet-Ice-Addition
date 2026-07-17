package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.font.TextFieldHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextFieldHelper.class)
public abstract class DisableIllegalChatCharacterCheckClipboardMixin {

    @Redirect(
            method = "getClipboardContents(Lnet/minecraft/client/Minecraft;)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private static String carpetIceAddition$keepCarriageReturns(
            String clipboard,
            String regex,
            String replacement
    ) {
        if (CarpetIceAdditionSettings.disableIllegalChatCharacterCheck) {
            return clipboard;
        }
        return clipboard.replaceAll(regex, replacement);
    }

    @Redirect(
            method = "getClipboardContents(Lnet/minecraft/client/Minecraft;)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/ChatFormatting;stripFormatting(Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private static String carpetIceAddition$keepIllegalClipboardCharacters(String clipboard) {
        if (CarpetIceAdditionSettings.disableIllegalChatCharacterCheck) {
            return clipboard;
        }
        return ChatFormatting.stripFormatting(clipboard);
    }
}
