package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState26;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerVillagerEventsMixin implements VillagerEventState26 {
    @Unique private VillagerEventSnapshot26 carpetIceAddition$deathSnapshot;
    @Unique private boolean carpetIceAddition$conversionActive;
    @Unique private boolean carpetIceAddition$conversionSpawned;
    @Unique private boolean carpetIceAddition$conversionDiscarded;
    @Unique private boolean carpetIceAddition$convertedDuringDeath;
    @Inject(method = "die", at = @At("HEAD"))
    private void carpetIceAddition$captureDeath(DamageSource source, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (self.level() instanceof ServerLevel) carpetIceAddition$deathSnapshot = VillagerEventsRuntime26.captureDeath(self, source);
        carpetIceAddition$convertedDuringDeath = false;
    }
    @Inject(method = "die", at = @At("TAIL"))
    private void carpetIceAddition$reportDeath(DamageSource source, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        try { if (!carpetIceAddition$convertedDuringDeath && carpetIceAddition$deathSnapshot != null && self.level() instanceof ServerLevel world) VillagerEventsRuntime26.death(world.getServer(), carpetIceAddition$deathSnapshot); }
        finally { carpetIceAddition$deathSnapshot = null; }
    }
    @Override public void carpetIceAddition$beginConversion(VillagerEventSnapshot26 snapshot) { carpetIceAddition$conversionActive = true; carpetIceAddition$conversionSpawned = false; carpetIceAddition$conversionDiscarded = false; }
    @Override public void carpetIceAddition$recordConversionSpawn(boolean accepted) { if (carpetIceAddition$conversionActive && accepted) carpetIceAddition$conversionSpawned = true; }
    @Override public void carpetIceAddition$recordConversionDiscard() { if (carpetIceAddition$conversionActive) carpetIceAddition$conversionDiscarded = true; }
    @Override public boolean carpetIceAddition$finishConversion(boolean returnedEntity) { boolean success = carpetIceAddition$conversionActive && returnedEntity && carpetIceAddition$conversionSpawned && carpetIceAddition$conversionDiscarded; carpetIceAddition$conversionActive = false; if (success) carpetIceAddition$convertedDuringDeath = true; return success; }
    @Override public boolean carpetIceAddition$convertedDuringDeath() { return carpetIceAddition$convertedDuringDeath; }
}
