package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenDisableIllegalTextCharacterCheckMixin {

    @Shadow @Final private TextFieldHelper titleEdit;
    @Shadow private boolean isModified;

    @Invoker("updateButtonVisibility")
    protected abstract void carpetIceAddition$invokeUpdateButtons();

    @Inject(method = "titleKeyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$pasteIllegalBookTitleCharacters(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CarpetIceAdditionSettings.disableIllegalTextCharacterCheck || !Screen.isPaste(keyCode)) {
            return;
        }

        this.titleEdit.paste();
        this.carpetIceAddition$invokeUpdateButtons();
        this.isModified = true;
        cir.setReturnValue(true);
    }
}
