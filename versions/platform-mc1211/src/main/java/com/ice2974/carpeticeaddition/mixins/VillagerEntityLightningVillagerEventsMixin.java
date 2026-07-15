package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventConversionScope121;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 1.21.1 creates a Witch manually instead of using MobEntity.convertTo. */
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityLightningVillagerEventsMixin {
    @Redirect(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"))
    private void carpetIceAddition$observeWitchSpawn(ServerWorld world, Entity witch) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        VillagerEventState state = (VillagerEventState) self;
        VillagerEventSnapshot121 snapshot = VillagerEventsRuntime121.snapshot(self, null);
        try { state.carpetIceAddition$beginConversion(snapshot); } catch (Throwable error) { VillagerEventsCompatibility.report(error); }
        VillagerEventConversionScope121.push(state);
        try { world.spawnEntityAndPassengers(witch); }
        finally { VillagerEventConversionScope121.pop(state); }
    }

    @Redirect(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/VillagerEntity;discard()V"))
    private void carpetIceAddition$observeVillagerDiscard(VillagerEntity villager) {
        villager.discard();
        VillagerEventState state = (VillagerEventState) villager;
        try {
            state.carpetIceAddition$recordConversionDiscard();
            VillagerEventSnapshot121 snapshot = state.carpetIceAddition$conversionSnapshot();
            boolean success = state.carpetIceAddition$finishConversion(true);
            state.carpetIceAddition$clearVillagerEventState();
            if (success && CarpetServer.minecraft_server != null) VillagerEventsRuntime121.conversion(CarpetServer.minecraft_server, "witch", snapshot);
        } catch (Throwable error) { VillagerEventsCompatibility.report(error); }
    }
}
