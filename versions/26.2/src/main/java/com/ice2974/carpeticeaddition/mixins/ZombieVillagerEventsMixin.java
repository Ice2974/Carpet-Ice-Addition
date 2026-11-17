package com.ice2974.carpeticeaddition.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.monster.zombie.Zombie.class)
public abstract class ZombieVillagerEventsMixin {
    @WrapOperation(method = "convertVillagerToZombieVillager", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> Mob carpetIceAddition$observeConversion(Villager villager, EntityType<T> type, ConversionParams params, ConversionParams.AfterConversion<T> finalizer, Operation<Mob> original) {
        VillagerEventState state = null; VillagerEventSnapshot snapshot = null; boolean observing = false;
        try {
            state = (VillagerEventState) villager;
            snapshot = VillagerEventsRuntime.snapshot(villager, null);
            state.carpetIceAddition$abortConversion(); state.carpetIceAddition$beginConversion(snapshot); observing = true;
        } catch (Throwable error) {
            VillagerEventsCompatibility.report("zombified_conversion", error);
            if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("zombified_conversion", cleanup); }
        }
        Mob result = null; boolean returned = false;
        try { result = original.call(villager, type, params, finalizer); returned = true; return result; }
        finally {
            if (returned && observing) try { if (state.carpetIceAddition$finishConversion(result instanceof ZombieVillager) && CarpetServer.minecraft_server != null) VillagerEventsRuntime.conversion(CarpetServer.minecraft_server, "zombified", snapshot); }
            catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
            finally { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("zombified_conversion", cleanup); } }
            else if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
        }
    }
}
