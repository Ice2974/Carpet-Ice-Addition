package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.entity.mob.ZombieEntity.class)
public abstract class ZombieEntityVillagerEventsMixin {
    @Redirect(method = "onKilledOther", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/VillagerEntity;convertTo(Lnet/minecraft/entity/EntityType;Z)Lnet/minecraft/entity/mob/MobEntity;"))
    private MobEntity carpetIceAddition$observeInfection(VillagerEntity villager, EntityType<? extends MobEntity> type, boolean keepEquipment) {
        VillagerEventState state = (VillagerEventState) villager;
        VillagerEventSnapshot121 snapshot = state.carpetIceAddition$deathSnapshot();
        if (snapshot == null) snapshot = VillagerEventsRuntime121.snapshot(villager, null);
        state.carpetIceAddition$beginConversion(snapshot);
        MobEntity result = villager.convertTo(type, keepEquipment);
        if (state.carpetIceAddition$finishConversion(result instanceof ZombieVillagerEntity) && CarpetServer.minecraft_server != null) VillagerEventsRuntime121.conversion(CarpetServer.minecraft_server, "zombified", snapshot);
        return result;
    }
}
