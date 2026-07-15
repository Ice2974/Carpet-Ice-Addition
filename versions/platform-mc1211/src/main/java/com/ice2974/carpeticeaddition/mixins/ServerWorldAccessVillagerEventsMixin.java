package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventConversionScope121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.entity.Entity;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Observes the boolean that the 1.21.1 default passenger-spawn helper discards. */
@Mixin(ServerWorldAccess.class)
public interface ServerWorldAccessVillagerEventsMixin {
    @Redirect(method = "spawnEntityAndPassengers", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ServerWorldAccess;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean carpetIceAddition$observePassengerSpawn(ServerWorldAccess world, Entity entity) {
        boolean accepted = world.spawnEntity(entity);
        try { VillagerEventConversionScope121.recordSpawn(entity, accepted); }
        catch (Throwable error) { VillagerEventsCompatibility.report("conversion_spawn", error); }
        return accepted;
    }
}
