//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import carpet.CarpetServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.world.entity.monster.zombie.Zombie.class)
public abstract class ZombieVillagerEventsMixin {
    @Redirect(method = "convertVillagerToZombieVillager", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> Mob carpetIceAddition$observeInfection(Villager villager, EntityType<T> type,
                                                                               ConversionParams context, ConversionParams.AfterConversion<T> finalizer) {
        VillagerEventState state = null;
        VillagerEventSnapshot121 snapshot = null;
        boolean observing = false;
        try {
            state = (VillagerEventState) villager;
            snapshot = state.carpetIceAddition$deathSnapshot();
            if (snapshot == null) snapshot = VillagerEventsRuntime121.snapshot(villager, null);
            state.carpetIceAddition$abortConversion();
            state.carpetIceAddition$beginConversion(snapshot);
            observing = true;
        } catch (Throwable error) {
            VillagerEventsCompatibility.report("zombified_conversion", error);
            if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("zombified_conversion", cleanup); }
        }
        Mob result = null;
        boolean returned = false;
        try { result = villager.convertTo(type, context, finalizer); returned = true; return result; }
        finally {
            if (returned && observing) try {
                if (state.carpetIceAddition$finishConversion(result instanceof ZombieVillager) && CarpetServer.minecraft_server != null) VillagerEventsRuntime121.conversion(CarpetServer.minecraft_server, "zombified", snapshot);
            } catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
            finally { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("zombified_conversion", cleanup); } }
            else if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
        }
    }
}
//#endif
