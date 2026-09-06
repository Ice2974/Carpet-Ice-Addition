package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CustomEndPlatformPositionHelper;
import com.ice2974.carpeticeaddition.rules.CustomEndPlatformPositionHelper.IntPosition;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionEndPlatformSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockCustomEndPlatformPositionMixin {
    private static final float END_PLATFORM_YAW = 270.0F;

    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$customEndPlatformPosition(ServerLevel world, Entity entity, BlockPos pos, CallbackInfoReturnable<DimensionTransition> cir) {
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

            Vec3 arrivalPos = platformCenter.above().getBottomCenter();
            EndPlatformFeature.createEndPlatform(endWorld, platformCenter, true);
            if (entity instanceof ServerPlayer) {
                arrivalPos = arrivalPos.subtract(0.0, 1.0, 0.0);
            }

            cir.setReturnValue(new DimensionTransition(
                    endWorld,
                    arrivalPos,
                    entity.getDeltaMovement(),
                    END_PLATFORM_YAW,
                    entity.getXRot(),
                    DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
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
