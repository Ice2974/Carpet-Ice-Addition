package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventConversionScope121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code ServerWorldAccess.spawnEntityAndPassengers} delegates through an invokedynamic
 * lambda in 1.21.1, so it contains no redirectable spawn invocation. Observe the concrete
 * {@link ServerLevel#addFreshEntity(Entity)} result instead; the conversion scope filters by
 * exact root-Witch identity.
 */
@Mixin(ServerLevel.class)
public abstract class ServerWorldAccessVillagerEventsMixin {
    @Inject(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void carpetIceAddition$observePassengerSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        try { VillagerEventConversionScope121.recordSpawn(entity, cir.getReturnValue()); }
        catch (Throwable error) { VillagerEventsCompatibility.report("conversion_spawn", error); }
    }
}
