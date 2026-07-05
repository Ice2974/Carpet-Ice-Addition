package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.PhantomSpawnWarningHelper;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerWorldPhantomSpawnWarningMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void carpetIceAddition$phantomSpawnWarning(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        try {
            PhantomSpawnWarningHelper.tick((ServerLevel) (Object) this);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("phantomSpawnWarning", throwable);
        }
    }
}
