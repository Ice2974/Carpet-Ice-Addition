package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.block.entity.SculkCatalystBlockEntity$Listener")
public abstract class SculkCatalystListenerMixin {
    @Shadow
    @Final
    private PositionSource positionSource;

    // 仅在同一次 listen 调用内有效：每次 HEAD 无条件重置；HEAD 到 spread 调用之间
    // 只有纯查询语句，不存在同步重入本监听器的路径，无跨调用残留。
    @Unique
    private boolean carpetIceAddition$woolAboveCatalyst;

    @Inject(method = "listen", at = @At("HEAD"))
    private void carpetIceAddition$checkWoolCover(ServerWorld world, RegistryEntry<GameEvent> event,
            GameEvent.Emitter emitter, Vec3d emitterPos, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$woolAboveCatalyst = false;
        if (!CarpetIceAdditionSettings.woolSuppressesSculkSpread || !event.matches(GameEvent.ENTITY_DIE)) {
            return;
        }
        this.carpetIceAddition$woolAboveCatalyst = this.positionSource.getPos(world)
                .map(BlockPos::ofFloored)
                .map(BlockPos::up)
                .map(world::getBlockState)
                .map(state -> state.isIn(BlockTags.WOOL))
                .orElse(false);
    }

    // 只跳过新 charge cursor 的创建：listen 中后续的 disableExperienceDropping()
    // （经验抑制）与 bloom() 均不依赖这次调用，照常执行。
    @Redirect(method = "listen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/entity/SculkSpreadManager;spread(Lnet/minecraft/util/math/BlockPos;I)V"))
    private void carpetIceAddition$skipSpreadIfWoolCovered(SculkSpreadManager spreadManager, BlockPos pos, int charge) {
        if (CarpetIceAdditionSettings.woolSuppressesSculkSpread && this.carpetIceAddition$woolAboveCatalyst) {
            return;
        }
        spreadManager.spread(pos, charge);
    }
}
