package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CustomEndPlatformPositionHelper;
import com.ice2974.carpeticeaddition.rules.CustomEndPlatformPositionHelper.IntPosition;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionEndPlatformSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockCustomEndPlatformPositionMixin {

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$customEndPlatformPosition(ServerLevel world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTransition> cir) {
        try {
            if (world.dimension() == Level.END) {
                return;
            }

            Optional<IntPosition> configuredPosition = CustomEndPlatformPositionHelper.parse(CarpetIceAdditionEndPlatformSettings.customEndPlatformPosition);
            if (configuredPosition.isEmpty()) {
                return;
            }

            ServerLevel endWorld = world.getServer().getLevel(Level.END);
            if (endWorld == null) {
                cir.setReturnValue(null);
                return;
            }

            BlockPos platformCenter = toBlockPos(configuredPosition.get());
            if (!carpetIceAddition$isPlatformAreaInBounds(endWorld, platformCenter)) {
                CustomEndPlatformPositionHelper.reportOutOfBoundsPosition(configuredPosition.get().asRuleString());
                return;
            }

            Vec3 arrivalPos = Vec3.atBottomCenterOf(platformCenter.above());
            EndPlatformFeature.createEndPlatform(endWorld, platformCenter, true);
            float yaw = Direction.WEST.toYRot();
            float pitch = 0.0F;
            Set<Relative> relatives = Relative.union(Relative.DELTA, Set.of(Relative.X_ROT));
            if (entity instanceof ServerPlayer) {
                arrivalPos = arrivalPos.subtract(0.0, 1.0, 0.0);
            }

            cir.setReturnValue(new TeleportTransition(
                    endWorld,
                    arrivalPos,
                    Vec3.ZERO,
                    yaw,
                    pitch,
                    relatives,
                    TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)
            ));
        } catch (Throwable throwable) {
            CustomEndPlatformPositionHelper.reportCompatibilityIssue(throwable);
        }
    }

    private static BlockPos toBlockPos(IntPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    private static boolean carpetIceAddition$isPlatformAreaInBounds(ServerLevel world, BlockPos platformCenter) {
        return world.isInWorldBounds(platformCenter.below()) && world.isInWorldBounds(platformCenter.above(2));
    }
}
