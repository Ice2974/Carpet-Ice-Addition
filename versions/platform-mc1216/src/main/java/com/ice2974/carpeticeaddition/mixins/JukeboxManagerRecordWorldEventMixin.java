package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.DelayedJukeboxStartEventManager;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.jukebox.JukeboxManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(JukeboxManager.class)
public abstract class JukeboxManagerRecordWorldEventMixin {

    @Redirect(
            method = "startPlaying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldAccess;syncWorldEvent(Lnet/minecraft/entity/Entity;ILnet/minecraft/util/math/BlockPos;I)V"
            )
    )
    private void carpetIceAddition$delayJukeboxStartWorldEvent(WorldAccess world, Entity entity, int eventId, BlockPos pos, int data) {
        if (!CarpetIceAdditionSettings.recordWorldEventFix || !CarpetIceAdditionMod.shouldEnableRecordWorldEventFix()) {
            world.syncWorldEvent(entity, eventId, pos, data);
            return;
        }

        try {
            if (world instanceof ServerWorld serverWorld) {
                DelayedJukeboxStartEventManager.queueStart(serverWorld, pos, data);
            } else {
                world.syncWorldEvent(entity, eventId, pos, data);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("recordWorldEventFix", throwable);
            world.syncWorldEvent(entity, eventId, pos, data);
        }
    }

    @Redirect(
            method = "stopPlaying",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldAccess;syncWorldEvent(ILnet/minecraft/util/math/BlockPos;I)V"
            )
    )
    private void carpetIceAddition$recordStopBeforeImmediateWorldEvent(WorldAccess world, int eventId, BlockPos pos, int data) {
        if (CarpetIceAdditionSettings.recordWorldEventFix && CarpetIceAdditionMod.shouldEnableRecordWorldEventFix()) {
            try {
                if (world instanceof ServerWorld serverWorld) {
                    DelayedJukeboxStartEventManager.recordStop(serverWorld, pos);
                }
            } catch (Throwable throwable) {
                CarpetIceAdditionMod.reportFeatureCompatibilityIssue("recordWorldEventFix", throwable);
            }
        }
        world.syncWorldEvent(eventId, pos, data);
    }
}
