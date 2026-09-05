//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerDeathSide121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerEntityVillagerEventsMixin implements VillagerEventState {
    @Unique private VillagerEventSnapshot121 carpetIceAddition$deathSnapshot;
    @Unique private VillagerEventSnapshot121 carpetIceAddition$conversionSnapshot;
    @Unique private boolean carpetIceAddition$conversionActive;
    @Unique private boolean carpetIceAddition$conversionSpawned;
    @Unique private boolean carpetIceAddition$conversionDiscarded;
    @Unique private boolean carpetIceAddition$convertedDuringDeath;

    @Inject(method = "die", at = @At("HEAD"))
    private void carpetIceAddition$captureDeath(DamageSource source, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        try {
            carpetIceAddition$clearVillagerEventState();
            if (VillagerDeathSide121.serverWorld(self) == null) return;
            carpetIceAddition$beginDeath(VillagerEventsRuntime121.captureDeath(self, source));
        }
        catch (Throwable error) { carpetIceAddition$clearVillagerEventState(); VillagerEventsCompatibility.report("death_capture", error); }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void carpetIceAddition$reportDeath(DamageSource source, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        try {
            ServerLevel world = VillagerDeathSide121.serverWorld(self);
            VillagerEventSnapshot121 snapshot = carpetIceAddition$deathSnapshot;
            boolean converted = carpetIceAddition$convertedDuringDeath;
            if (world != null && !converted && snapshot != null) {
                VillagerEventsRuntime121.death(world.getServer(), snapshot);
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
//#endif
