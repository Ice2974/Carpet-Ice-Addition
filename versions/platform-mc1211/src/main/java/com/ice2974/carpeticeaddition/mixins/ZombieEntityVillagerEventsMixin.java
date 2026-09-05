package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.world.entity.monster.Zombie.class)
public abstract class ZombieEntityVillagerEventsMixin {
    @Redirect(method = "killedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;"))
    private Mob carpetIceAddition$observeInfection(Villager villager, EntityType<? extends Mob> type, boolean keepEquipment) {
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
        Mob result = null; boolean returned = false;
        try { result = villager.convertTo(type, keepEquipment); returned = true; return result; }
        finally {
            if (returned && observing) try { if (state.carpetIceAddition$finishConversion(result instanceof ZombieVillager) && CarpetServer.minecraft_server != null) VillagerEventsRuntime121.conversion(CarpetServer.minecraft_server, "zombified", snapshot); }
            catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
            finally { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("zombified_conversion", cleanup); } }
            else if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable error) { VillagerEventsCompatibility.report("zombified_conversion", error); }
        }
    }
}
