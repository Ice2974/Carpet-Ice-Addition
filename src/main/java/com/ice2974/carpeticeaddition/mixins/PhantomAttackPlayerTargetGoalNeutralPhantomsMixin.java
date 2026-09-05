//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomAttackPlayerTargetGoal")
public abstract class PhantomAttackPlayerTargetGoalNeutralPhantomsMixin {
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$disableNeutralPhantomsActivePlayerTargeting(CallbackInfoReturnable<Boolean> cir) {
        if (CarpetIceAdditionSettings.neutralPhantoms) {
            cir.setReturnValue(false);
        }
    }
}
//#endif
