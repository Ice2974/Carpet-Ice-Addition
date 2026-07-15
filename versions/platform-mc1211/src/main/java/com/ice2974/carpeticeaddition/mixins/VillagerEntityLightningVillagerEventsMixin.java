package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsCompatibility;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime121;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventConversionScope121;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.1 creates a Witch manually instead of using MobEntity.convertTo. */
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityLightningVillagerEventsMixin {
    @Inject(method = "onStruckByLightning", at = @At("HEAD"))
    private void carpetIceAddition$resetLightningState(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        try { ((VillagerEventState) (Object) this).carpetIceAddition$abortConversion(); }
        catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
    }
    @Inject(method = "onStruckByLightning", at = @At("RETURN"))
    private void carpetIceAddition$clearIncompleteLightningState(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        try { ((VillagerEventState) (Object) this).carpetIceAddition$abortConversion(); }
        catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
    }
    @Redirect(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"))
    private void carpetIceAddition$observeWitchSpawn(ServerWorld world, Entity witch) {
        VillagerEventState state = null;
        boolean observing = false;
        boolean pushed = false;
        try {
            VillagerEntity self = (VillagerEntity) (Object) this;
            state = (VillagerEventState) self;
            VillagerEventSnapshot121 snapshot = VillagerEventsRuntime121.snapshot(self, null);
            state.carpetIceAddition$abortConversion();
            state.carpetIceAddition$beginConversion(snapshot);
            observing = true;
            VillagerEventConversionScope121.push(state, witch);
            pushed = true;
        } catch (Throwable error) {
            VillagerEventsCompatibility.report("witch_conversion", error);
            if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); }
        }
        try { world.spawnEntityAndPassengers(witch); }
        catch (Throwable original) { if (observing) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); } throw original; }
        finally { if (pushed) try { VillagerEventConversionScope121.pop(state); } catch (Throwable error) { VillagerEventsCompatibility.report("conversion_scope", error); } }
    }

    @Redirect(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/VillagerEntity;discard()V"))
    private void carpetIceAddition$observeVillagerDiscard(VillagerEntity villager) {
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
            VillagerEventSnapshot121 snapshot = state.carpetIceAddition$conversionSnapshot();
            boolean success = state.carpetIceAddition$finishConversion(true);
            state.carpetIceAddition$clearVillagerEventState();
            if (success && CarpetServer.minecraft_server != null) VillagerEventsRuntime121.conversion(CarpetServer.minecraft_server, "witch", snapshot);
        } catch (Throwable error) { VillagerEventsCompatibility.report("witch_conversion", error); }
        finally { if (state != null) try { state.carpetIceAddition$abortConversion(); } catch (Throwable cleanup) { VillagerEventsCompatibility.report("witch_conversion", cleanup); } }
    }
}
