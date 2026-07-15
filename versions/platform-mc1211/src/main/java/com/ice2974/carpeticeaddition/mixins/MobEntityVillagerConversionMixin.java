package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
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
        Object self = this;
        if (self instanceof VillagerEntity && self instanceof VillagerEventState state && (target instanceof ZombieVillagerEntity || target instanceof WitchEntity)) state.carpetIceAddition$recordConversionSpawn(accepted);
        return accepted;
    }
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;discard()V"))
    private void carpetIceAddition$observeDiscard(MobEntity source) {
        Object self = this;
        if (self instanceof VillagerEntity && self instanceof VillagerEventState state) state.carpetIceAddition$recordConversionDiscard();
        source.discard();
    }
}
