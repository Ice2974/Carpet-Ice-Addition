package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.DelayedJukeboxStartEventManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldRecordWorldEventMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void carpetIceAddition$tickDelayedRecordWorldEvents(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        try {
            DelayedJukeboxStartEventManager.tick((ServerWorld) (Object) this);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("recordWorldEventFix", throwable);
        }
    }
}
