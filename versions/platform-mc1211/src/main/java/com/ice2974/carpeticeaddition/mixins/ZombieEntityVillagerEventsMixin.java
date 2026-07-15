package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
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
        VillagerEventState state = null; VillagerEventSnapshot121 snapshot = null; boolean observing = false;
        try {
            state = (VillagerEventState) villager;
            snapshot = state.carpetIceAddition$deathSnapshot();
            if (snapshot == null) snapshot = VillagerEventsRuntime121.snapshot(villager, null);
            state.carpetIceAddition$abortConversion(); state.carpetIceAddition$beginConversion(snapshot); observing = true;
        } catch (Throwable error) {
            VillagerEventsCompatibility.report("zombified_conversion", error);
            if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("zombified_conversion", cleanup); }
        }
        MobEntity result = null; boolean returned = false;
        try { result = villager.convertTo(type, keepEquipment); returned = true; return result; }
        finally {
            if (returned && observing) try { if (state.carpetIceAddition$finishConversion(result instanceof ZombieVillagerEntity) && CarpetServer.minecraft_server != null) VillagerEventsRuntime121.conversion(CarpetServer.minecraft_server, "zombified", snapshot); }
            catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
            finally { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("zombified_conversion", cleanup); } }
            else if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
        }
    }
}
