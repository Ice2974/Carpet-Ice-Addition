package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState26;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mob.class)
public abstract class MobVillagerConversionMixin {
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean carpetIceAddition$observeSpawn(ServerLevel level, Entity target) {
        boolean accepted = level.addFreshEntity(target);
        Object self = this;
        if (self instanceof Villager && self instanceof VillagerEventState26 state && state.carpetIceAddition$conversionActive() && (target instanceof ZombieVillager || target instanceof Witch)) state.carpetIceAddition$recordConversionSpawn(accepted);
        return accepted;
    }
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private void carpetIceAddition$observeDiscard(Mob source) {
        Object self = this;
        source.discard();
        if (self instanceof Villager && self instanceof VillagerEventState26 state && state.carpetIceAddition$conversionActive()) state.carpetIceAddition$recordConversionDiscard();
    }
}
