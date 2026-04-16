package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.DelayedJukeboxStartEventManager;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxManagerRecordWorldEventMixin {

    @Redirect(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V"
            )
    )
    private void carpetIceAddition$delayJukeboxStartWorldEvent(LevelAccessor world, Entity entity, int eventId, BlockPos pos, int data) {
        if (!CarpetIceAdditionSettings.recordWorldEventFix || !CarpetIceAdditionMod.shouldEnableRecordWorldEventFix()) {
            world.levelEvent(entity, eventId, pos, data);
            return;
        }

        try {
            if (world instanceof ServerLevel serverWorld) {
                DelayedJukeboxStartEventManager.queueStart(serverWorld, pos, data);
            } else {
                world.levelEvent(entity, eventId, pos, data);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("recordWorldEventFix", throwable);
            world.levelEvent(entity, eventId, pos, data);
        }
    }

    @Redirect(
            method = "stop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;levelEvent(ILnet/minecraft/core/BlockPos;I)V"
            )
    )
    private void carpetIceAddition$recordStopBeforeImmediateWorldEvent(LevelAccessor world, int eventId, BlockPos pos, int data) {
        if (CarpetIceAdditionSettings.recordWorldEventFix && CarpetIceAdditionMod.shouldEnableRecordWorldEventFix()) {
            try {
                if (world instanceof ServerLevel serverWorld) {
                    DelayedJukeboxStartEventManager.recordStop(serverWorld, pos);
                }
            } catch (Throwable throwable) {
                CarpetIceAdditionMod.reportFeatureCompatibilityIssue("recordWorldEventFix", throwable);
            }
        }
        world.levelEvent(eventId, pos, data);
    }
}
