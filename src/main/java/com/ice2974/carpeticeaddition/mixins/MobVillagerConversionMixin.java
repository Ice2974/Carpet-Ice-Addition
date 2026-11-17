//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observes only an active Villager conversion; every other Mob conversion is passed through unchanged. */
@Mixin(Mob.class)
public abstract class MobVillagerConversionMixin {
    //#disable-remap
    @WrapOperation(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    //#enable-remap
    private boolean carpetIceAddition$observeVillagerSpawn(ServerLevel world, Entity target, Operation<Boolean> original) {
        boolean accepted = original.call(world, target);
        try {
            Object self = this;
            if (self instanceof Villager && self instanceof VillagerEventState state && state.carpetIceAddition$conversionActive() && (target instanceof ZombieVillager || target instanceof Witch)) state.carpetIceAddition$recordConversionSpawn(accepted);
        } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_spawn", error); }
        return accepted;
    }

    //#disable-remap
    @WrapOperation(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    //#enable-remap
    private void carpetIceAddition$observeVillagerDiscard(Mob source, Operation<Void> original) {
        original.call(source);
        try {
            Object self = this;
            if (self instanceof Villager && self instanceof VillagerEventState state && state.carpetIceAddition$conversionActive() && source.isRemoved()) state.carpetIceAddition$recordConversionDiscard();
        } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_discard", error); }
    }
}
//#endif
