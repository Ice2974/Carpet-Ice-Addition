package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenDisableIllegalTextCharacterCheckMixin {

    @Shadow @Final private SelectionManager bookTitleSelectionManager;
    @Shadow private boolean dirty;

    @Invoker("updateButtons")
    protected abstract void carpetIceAddition$invokeUpdateButtons();

    @Inject(method = "keyPressedSignMode(III)Z", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$pasteIllegalBookTitleCharacters(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CarpetIceAdditionSettings.disableIllegalTextCharacterCheck || !Screen.isPaste(keyCode)) {
            return;
        }

        this.bookTitleSelectionManager.paste();
        this.carpetIceAddition$invokeUpdateButtons();
        this.dirty = true;
        cir.setReturnValue(true);
    }
}
