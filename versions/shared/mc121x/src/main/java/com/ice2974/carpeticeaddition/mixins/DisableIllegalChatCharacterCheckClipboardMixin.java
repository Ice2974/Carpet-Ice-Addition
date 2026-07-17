package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SelectionManager.class)
public abstract class DisableIllegalChatCharacterCheckClipboardMixin {

    @Redirect(
            method = "getClipboard(Lnet/minecraft/client/MinecraftClient;)Ljava/lang/String;",
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
            method = "getClipboard(Lnet/minecraft/client/MinecraftClient;)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Formatting;strip(Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private static String carpetIceAddition$keepIllegalClipboardCharacters(String clipboard) {
        if (CarpetIceAdditionSettings.disableIllegalChatCharacterCheck) {
            return clipboard;
        }
        return Formatting.strip(clipboard);
    }
}
