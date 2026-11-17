package com.ice2974.carpeticeaddition.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.DelayedJukeboxStartEventManager;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxManagerRecordWorldEventMixin {

    @Shadow @Final private BlockPos blockPos;

    @WrapOperation(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V"
            )
    )
    private void carpetIceAddition$delayJukeboxStartWorldEvent(
            LevelAccessor world, Entity entity, int eventId, BlockPos pos, int data, Operation<Void> original
    ) {
        if (CarpetIceAdditionSettings.recordWorldEventFix && world instanceof ServerLevel serverWorld) {
            try {
                DelayedJukeboxStartEventManager.queueStart(serverWorld, pos, data);
                return;
            } catch (Throwable throwable) {
                CarpetIceAdditionMod.reportFeatureCompatibilityIssue("recordWorldEventFix", throwable);
            }
        }
        original.call(world, entity, eventId, pos, data);
    }

    @Inject(
            method = "stop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;levelEvent(ILnet/minecraft/core/BlockPos;I)V"
            )
    )
    private void carpetIceAddition$recordStopBeforeImmediateWorldEvent(LevelAccessor world, BlockState state, CallbackInfo ci) {
        if (CarpetIceAdditionSettings.recordWorldEventFix && world instanceof ServerLevel serverWorld) {
            try {
                DelayedJukeboxStartEventManager.recordStop(serverWorld, this.blockPos);
            } catch (Throwable throwable) {
                CarpetIceAdditionMod.reportFeatureCompatibilityIssue("recordWorldEventFix", throwable);
            }
        }
    }
}
