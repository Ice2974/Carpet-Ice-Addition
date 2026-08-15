package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.entity.SculkCatalystBlockEntity$CatalystListener")
public abstract class SculkCatalystListenerMixin {
    @Shadow
    @Final
    private PositionSource positionSource;

    // 仅在同一次 handleGameEvent 调用内有效：每次 HEAD 无条件重置；HEAD 到 addCursors
    // 调用之间只有纯查询语句，不存在同步重入本监听器的路径，无跨调用残留。
    @Unique
    private boolean carpetIceAddition$woolAboveCatalyst;

    @Inject(method = "handleGameEvent", at = @At("HEAD"))
    private void carpetIceAddition$checkWoolCover(ServerLevel level, Holder<GameEvent> event,
            GameEvent.Context context, Vec3 pos, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$woolAboveCatalyst = false;
        if (!CarpetIceAdditionSettings.woolSuppressesSculkSpread) {
            return;
        }
        this.carpetIceAddition$woolAboveCatalyst = this.positionSource.getPosition(level)
                .map(BlockPos::containing)
                .map(BlockPos::above)
                .map(level::getBlockState)
                .map(state -> state.is(BlockTags.WOOL))
                .orElse(false);
    }

    // 只跳过新 charge cursor 的创建：handleGameEvent 中后续的 skipDropExperience()
    // （经验抑制）与 bloom() 均不依赖这次调用，照常执行。
    @Redirect(method = "handleGameEvent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/SculkSpreader;addCursors(Lnet/minecraft/core/BlockPos;I)V"))
    private void carpetIceAddition$skipSpreadIfWoolCovered(SculkSpreader spreader, BlockPos pos, int charge) {
        if (CarpetIceAdditionSettings.woolSuppressesSculkSpread && this.carpetIceAddition$woolAboveCatalyst) {
            return;
        }
        spreader.addCursors(pos, charge);
    }
}
