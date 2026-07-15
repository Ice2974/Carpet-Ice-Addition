package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Villager.class)
public abstract class VillagerLightningVillagerEventsMixin {
    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob carpetIceAddition$observeConversion(Villager villager, EntityType<? extends Mob> type, ConversionParams params, ConversionParams.AfterConversion<?> finalizer) {
        VillagerEventState26 state = null; VillagerEventSnapshot26 snapshot = null; boolean observing = false;
        try {
            state = (VillagerEventState26) villager;
            snapshot = VillagerEventsRuntime26.snapshot(villager, null);
            state.carpetIceAddition$abortConversion(); state.carpetIceAddition$beginConversion(snapshot); observing = true;
        } catch (Throwable error) {
            VillagerEventsCompatibility.report("witch_conversion", error);
            if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); }
        }
        Mob result = null; boolean returned = false;
        try { result = villager.convertTo(type, params, (ConversionParams.AfterConversion) finalizer); returned = true; return result; }
        finally {
            if (returned && observing) try { if (state.carpetIceAddition$finishConversion(result instanceof Witch) && CarpetServer.minecraft_server != null) VillagerEventsRuntime26.conversion(CarpetServer.minecraft_server, "witch", snapshot); }
            catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
            finally { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); } }
            else if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
        }
    }
}
