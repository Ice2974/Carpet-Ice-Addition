package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import carpet.CarpetServer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityLightningVillagerEventsMixin {
    @Redirect(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/VillagerEntity;convertTo(Lnet/minecraft/entity/EntityType;Lnet/minecraft/entity/conversion/EntityConversionContext;Lnet/minecraft/entity/conversion/EntityConversionContext$Finalizer;)Lnet/minecraft/entity/mob/MobEntity;"))
    private MobEntity carpetIceAddition$observeWitch(VillagerEntity villager, EntityType<? extends MobEntity> type,
                                                     EntityConversionContext context, EntityConversionContext.Finalizer<?> finalizer) {
        VillagerEventState state = (VillagerEventState) villager;
        VillagerEventSnapshot121 snapshot = VillagerEventsRuntime121.snapshot(villager, null);
        try { state.carpetIceAddition$beginConversion(snapshot); } catch (Throwable error) { VillagerEventsCompatibility.report(error); }
        MobEntity result = null; boolean returned = false;
        try { result = villager.convertTo(type, context, (EntityConversionContext.Finalizer) finalizer); returned = true; return result; }
        finally {
            if (returned) try {
                if (state.carpetIceAddition$finishConversion(result instanceof WitchEntity) && CarpetServer.minecraft_server != null) VillagerEventsRuntime121.conversion(CarpetServer.minecraft_server, "witch", snapshot);
            } catch (Throwable error) { VillagerEventsCompatibility.report(error); }
            else try { state.carpetIceAddition$finishConversion(false); } catch (Throwable error) { VillagerEventsCompatibility.report(error); }
        }
    }
}
