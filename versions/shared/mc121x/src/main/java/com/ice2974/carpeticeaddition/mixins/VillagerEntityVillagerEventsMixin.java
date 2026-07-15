package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import carpet.CarpetServer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityVillagerEventsMixin implements VillagerEventState {
    @Shadow @Final private World world;
    @Unique private VillagerEventSnapshot121 carpetIceAddition$deathSnapshot;
    @Unique private VillagerEventSnapshot121 carpetIceAddition$conversionSnapshot;
    @Unique private boolean carpetIceAddition$conversionActive;
    @Unique private boolean carpetIceAddition$conversionSpawned;
    @Unique private boolean carpetIceAddition$conversionDiscarded;
    @Unique private boolean carpetIceAddition$convertedDuringDeath;

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void carpetIceAddition$captureDeath(DamageSource source, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        try { if (world instanceof ServerWorld) carpetIceAddition$beginDeath(VillagerEventsRuntime121.captureDeath(self, source)); }
        catch (Throwable error) { carpetIceAddition$clearVillagerEventState(); VillagerEventsCompatibility.report("death_capture", error); }
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void carpetIceAddition$reportDeath(DamageSource source, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        try {
            if (!carpetIceAddition$convertedDuringDeath && carpetIceAddition$deathSnapshot != null && CarpetServer.minecraft_server != null) {
                VillagerEventsRuntime121.death(CarpetServer.minecraft_server, carpetIceAddition$deathSnapshot);
            }
        }
        catch (Throwable error) { VillagerEventsCompatibility.report("death_report", error); }
        finally { carpetIceAddition$clearVillagerEventState(); }
    }

    @Override public void carpetIceAddition$beginDeath(VillagerEventSnapshot121 snapshot) {
        carpetIceAddition$deathSnapshot = snapshot;
        carpetIceAddition$conversionSnapshot = null;
        carpetIceAddition$conversionActive = false;
        carpetIceAddition$conversionSpawned = false;
        carpetIceAddition$conversionDiscarded = false;
        carpetIceAddition$convertedDuringDeath = false;
    }
    @Override public VillagerEventSnapshot121 carpetIceAddition$deathSnapshot() { return carpetIceAddition$deathSnapshot; }
    @Override public void carpetIceAddition$beginConversion(VillagerEventSnapshot121 snapshot) {
        carpetIceAddition$conversionSnapshot = snapshot;
        carpetIceAddition$conversionActive = true;
        carpetIceAddition$conversionSpawned = false;
        carpetIceAddition$conversionDiscarded = false;
    }
    @Override public VillagerEventSnapshot121 carpetIceAddition$conversionSnapshot() { return carpetIceAddition$conversionSnapshot; }
    @Override public boolean carpetIceAddition$conversionActive() { return carpetIceAddition$conversionActive; }
    @Override public void carpetIceAddition$recordConversionSpawn(boolean accepted) { if (carpetIceAddition$conversionActive && accepted) carpetIceAddition$conversionSpawned = true; }
    @Override public void carpetIceAddition$recordConversionDiscard() { if (carpetIceAddition$conversionActive) carpetIceAddition$conversionDiscarded = true; }
    @Override public boolean carpetIceAddition$finishConversion(boolean returnedEntity) {
        boolean success = carpetIceAddition$conversionActive && returnedEntity && carpetIceAddition$conversionSpawned && carpetIceAddition$conversionDiscarded;
        carpetIceAddition$conversionActive = false;
        carpetIceAddition$conversionSnapshot = null;
        carpetIceAddition$conversionSpawned = false;
        carpetIceAddition$conversionDiscarded = false;
        if (success) carpetIceAddition$convertedDuringDeath = true;
        return success;
    }
    @Override public void carpetIceAddition$abortConversion() {
        carpetIceAddition$conversionSnapshot = null;
        carpetIceAddition$conversionActive = false;
        carpetIceAddition$conversionSpawned = false;
        carpetIceAddition$conversionDiscarded = false;
    }
    @Override public void carpetIceAddition$clearVillagerEventState() {
        carpetIceAddition$deathSnapshot = null;
        carpetIceAddition$conversionSnapshot = null;
        carpetIceAddition$conversionActive = false;
        carpetIceAddition$conversionSpawned = false;
        carpetIceAddition$conversionDiscarded = false;
        carpetIceAddition$convertedDuringDeath = false;
    }
}
