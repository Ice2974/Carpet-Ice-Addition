package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mob.class)
public abstract class MobEntityVillagerConversionMixin {
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean carpetIceAddition$observeSpawn(Level world, Entity target) {
        boolean accepted = world.addFreshEntity(target);
        try {
            Object self = this;
            if (self instanceof Villager && self instanceof VillagerEventState state && state.carpetIceAddition$conversionActive() && (target instanceof ZombieVillager || target instanceof Witch)) state.carpetIceAddition$recordConversionSpawn(accepted);
        } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_spawn", error); }
        return accepted;
    }
    @Redirect(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private void carpetIceAddition$observeDiscard(Mob source) {
        source.discard();
        try {
            Object self = this;
            if (self instanceof Villager && self instanceof VillagerEventState state && state.carpetIceAddition$conversionActive()) state.carpetIceAddition$recordConversionDiscard();
        } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_discard", error); }
    }
}
