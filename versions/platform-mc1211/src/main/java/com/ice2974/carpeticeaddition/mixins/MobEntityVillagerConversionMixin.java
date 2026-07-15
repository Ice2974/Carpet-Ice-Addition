package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEntity.class)
public abstract class MobEntityVillagerConversionMixin {
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean carpetIceAddition$observeSpawn(World world, Entity target) {
        boolean accepted = world.spawnEntity(target);
        try {
            Object self = this;
            if (self instanceof VillagerEntity && self instanceof VillagerEventState state && state.carpetIceAddition$conversionActive() && (target instanceof ZombieVillagerEntity || target instanceof WitchEntity)) state.carpetIceAddition$recordConversionSpawn(accepted);
        } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_spawn", error); }
        return accepted;
    }
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;discard()V"))
    private void carpetIceAddition$observeDiscard(MobEntity source) {
        source.discard();
        try {
            Object self = this;
            if (self instanceof VillagerEntity && self instanceof VillagerEventState state && state.carpetIceAddition$conversionActive()) state.carpetIceAddition$recordConversionDiscard();
        } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_discard", error); }
    }
}
