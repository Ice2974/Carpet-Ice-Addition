package com.ice2974.carpeticeaddition.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SelectionManager.class)
public abstract class DisableIllegalTextCharacterCheckClipboardMixin {

    @WrapOperation(
            method = "getClipboard(Lnet/minecraft/client/MinecraftClient;)Ljava/lang/String;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Formatting;strip(Ljava/lang/String;)Ljava/lang/String;"
            )
    )
    private static String carpetIceAddition$keepIllegalClipboardCharacters(
            String clipboard,
            Operation<String> original
    ) {
        if (CarpetIceAdditionSettings.disableIllegalTextCharacterCheck) {
            return clipboard;
        }
        return original.call(clipboard);
    }
}
