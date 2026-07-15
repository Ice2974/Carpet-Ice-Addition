package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Observes only an active Villager conversion; every other Mob conversion is passed through unchanged. */
@Mixin(MobEntity.class)
public abstract class MobEntityVillagerConversionMixin {
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean carpetIceAddition$observeVillagerSpawn(ServerWorld world, Entity target) {
        boolean accepted = world.spawnEntity(target);
        Object self = this;
        if (self instanceof VillagerEntity && self instanceof VillagerEventState state && (target instanceof ZombieVillagerEntity || target instanceof WitchEntity)) {
            state.carpetIceAddition$recordConversionSpawn(accepted);
        }
        return accepted;
    }

    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;discard()V"))
    private void carpetIceAddition$observeVillagerDiscard(MobEntity source) {
        Object self = this;
        if (self instanceof VillagerEntity && self instanceof VillagerEventState state) state.carpetIceAddition$recordConversionDiscard();
        source.discard();
    }
}
