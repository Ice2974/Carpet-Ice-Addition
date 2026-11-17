package com.ice2974.carpeticeaddition.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.1 creates a Witch manually instead of using MobEntity.convertTo. */
@Mixin(Villager.class)
public abstract class VillagerLightningVillagerEventsMixin {
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
    @WrapOperation(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void carpetIceAddition$observeWitchSpawn(ServerLevel world, Entity witch, Operation<Void> original) {
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
        try { original.call(world, witch); }
        catch (Throwable failure) { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); } throw failure; }
        finally { if (pushed) try { VillagerEventConversionScope121.pop(state); } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_scope", error); } }
    }

    @WrapOperation(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;discard()V"))
    private void carpetIceAddition$observeVillagerDiscard(Villager villager, Operation<Void> original) {
        VillagerEventState state = null;
        try { original.call(villager); }
        catch (Throwable failure) {
            try { ((VillagerEventState) villager).carpetIceAddition$abortConversion(); }
            catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); }
            throw failure;
        }
        try {
            state = (VillagerEventState) villager;
            if (!state.carpetIceAddition$conversionActive() || !villager.isRemoved()) return;
            state.carpetIceAddition$recordConversionDiscard();
            VillagerEventSnapshot snapshot = state.carpetIceAddition$conversionSnapshot();
            boolean success = state.carpetIceAddition$finishConversion(true);
            state.carpetIceAddition$clearVillagerEventState();
            if (success && CarpetServer.minecraft_server != null) VillagerEventsRuntime.conversion(CarpetServer.minecraft_server, "witch", snapshot);
        } catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
        finally { if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); } }
    }

    @WrapOperation(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;releaseAllPois()V"))
    private void carpetIceAddition$observeReleaseTickets(Villager villager, Operation<Void> original) {
        try { original.call(villager); }
        catch (Throwable failure) {
            try { ((VillagerEventState) villager).carpetIceAddition$abortConversion(); }
            catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); }
            throw failure;
        }
    }
}
