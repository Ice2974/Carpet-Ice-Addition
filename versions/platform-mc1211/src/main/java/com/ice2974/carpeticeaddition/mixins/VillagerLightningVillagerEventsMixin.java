package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.npc.Villager;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventConversionScope121;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.1 creates a Witch manually instead of using MobEntity.convertTo. */
@Mixin(Villager.class)
public abstract class VillagerLightningVillagerEventsMixin {
    @Invoker("releaseAllPois")
    abstract void carpetIceAddition$invokeReleaseAllTickets();
    @Inject(method = "thunderHit", at = @At("HEAD"))
    private void carpetIceAddition$resetLightningState(ServerLevel world, LightningBolt lightning, CallbackInfo ci) {
        try { ((VillagerEventState) (Object) this).carpetIceAddition$abortConversion(); }
        catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
    }
    @Inject(method = "thunderHit", at = @At("RETURN"))
    private void carpetIceAddition$clearIncompleteLightningState(ServerLevel world, LightningBolt lightning, CallbackInfo ci) {
        try { ((VillagerEventState) (Object) this).carpetIceAddition$abortConversion(); }
        catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
    }
    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void carpetIceAddition$observeWitchSpawn(ServerLevel world, Entity witch) {
        VillagerEventState state = null;
        boolean observing = false;
        boolean pushed = false;
        try {
            Villager self = (Villager) (Object) this;
            state = (VillagerEventState) self;
            VillagerEventSnapshot snapshot = VillagerEventsRuntime.snapshot(self, null);
            state.carpetIceAddition$abortConversion();
            state.carpetIceAddition$beginConversion(snapshot);
            observing = true;
            VillagerEventConversionScope121.push(state, witch);
            pushed = true;
        } catch (Throwable error) {
            VillagerEventsCompatibility.report("witch_conversion", error);
            if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); }
        }
        try { world.addFreshEntityWithPassengers(witch); }
        catch (Throwable original) { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); } throw original; }
        finally { if (pushed) try { VillagerEventConversionScope121.pop(state); } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_scope", error); } }
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;discard()V"))
    private void carpetIceAddition$observeVillagerDiscard(Villager villager) {
        VillagerEventState state = null;
        try { villager.discard(); }
        catch (Throwable original) {
            try { ((VillagerEventState) villager).carpetIceAddition$abortConversion(); }
            catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); }
            throw original;
        }
        try {
            state = (VillagerEventState) villager;
            if (!state.carpetIceAddition$conversionActive()) return;
            state.carpetIceAddition$recordConversionDiscard();
            VillagerEventSnapshot snapshot = state.carpetIceAddition$conversionSnapshot();
            boolean success = state.carpetIceAddition$finishConversion(true);
            state.carpetIceAddition$clearVillagerEventState();
            if (success && CarpetServer.minecraft_server != null) VillagerEventsRuntime.conversion(CarpetServer.minecraft_server, "witch", snapshot);
        } catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
        finally { if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); } }
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;releaseAllPois()V"))
    private void carpetIceAddition$observeReleaseTickets(Villager villager) {
        try { carpetIceAddition$invokeReleaseAllTickets(); }
        catch (Throwable original) {
            try { ((VillagerEventState) villager).carpetIceAddition$abortConversion(); }
            catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); }
            throw original;
        }
    }
}
