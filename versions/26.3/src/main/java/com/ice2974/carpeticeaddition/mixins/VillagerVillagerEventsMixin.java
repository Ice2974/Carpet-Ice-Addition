package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerVillagerEventsMixin implements VillagerEventState {
    @Unique private VillagerEventSnapshot carpetIceAddition$deathSnapshot;
    @Unique private boolean carpetIceAddition$conversionActive;
    @Unique private boolean carpetIceAddition$conversionSpawned;
    @Unique private boolean carpetIceAddition$conversionDiscarded;
    @Unique private boolean carpetIceAddition$convertedDuringDeath;
    @Inject(method = "die", at = @At("HEAD"))
    private void carpetIceAddition$captureDeath(DamageSource source, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        carpetIceAddition$clearVillagerEventState();
        try { if (self.level() instanceof ServerLevel) carpetIceAddition$deathSnapshot = VillagerEventsRuntime.captureDeath(self, source); }
        catch (Throwable error) { VillagerEventsCompatibility.report("death_capture", error); }
    }
    @Inject(method = "die", at = @At("TAIL"))
    private void carpetIceAddition$reportDeath(DamageSource source, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        try { if (!carpetIceAddition$convertedDuringDeath && carpetIceAddition$deathSnapshot != null && self.level() instanceof ServerLevel world) VillagerEventsRuntime.death(world.getServer(), carpetIceAddition$deathSnapshot); }
        catch (Throwable error) { VillagerEventsCompatibility.report("death_report", error); }
        finally { carpetIceAddition$clearVillagerEventState(); }
    }
    @Override public void carpetIceAddition$beginConversion(VillagerEventSnapshot snapshot) { carpetIceAddition$conversionActive = true; carpetIceAddition$conversionSpawned = false; carpetIceAddition$conversionDiscarded = false; }
    @Override public boolean carpetIceAddition$conversionActive() { return carpetIceAddition$conversionActive; }
    @Override public void carpetIceAddition$recordConversionSpawn(boolean accepted) { if (carpetIceAddition$conversionActive && accepted) carpetIceAddition$conversionSpawned = true; }
    @Override public void carpetIceAddition$recordConversionDiscard() { if (carpetIceAddition$conversionActive) carpetIceAddition$conversionDiscarded = true; }
    @Override public boolean carpetIceAddition$finishConversion(boolean returnedEntity) { boolean success = carpetIceAddition$conversionActive && returnedEntity && carpetIceAddition$conversionSpawned && carpetIceAddition$conversionDiscarded; carpetIceAddition$conversionActive = false; carpetIceAddition$conversionSpawned = false; carpetIceAddition$conversionDiscarded = false; if (success) carpetIceAddition$convertedDuringDeath = true; return success; }
    @Override public void carpetIceAddition$abortConversion() { carpetIceAddition$conversionActive = false; carpetIceAddition$conversionSpawned = false; carpetIceAddition$conversionDiscarded = false; }
    @Override public boolean carpetIceAddition$convertedDuringDeath() { return carpetIceAddition$convertedDuringDeath; }
    @Override public void carpetIceAddition$clearVillagerEventState() { carpetIceAddition$deathSnapshot = null; carpetIceAddition$conversionActive = false; carpetIceAddition$conversionSpawned = false; carpetIceAddition$conversionDiscarded = false; carpetIceAddition$convertedDuringDeath = false; }
}
