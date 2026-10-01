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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

@Mixin(EndGatewayBlock.class)
public abstract class EndGatewayBlockCustomEndPlatformPositionMixin {
    @Inject(method = "getPortalDestination", at = @At("RETURN"), cancellable = true)
    private void carpetIceAddition$customEndPlatformPosition(ServerLevel world, Entity entity, BlockPos pos, CallbackInfoReturnable<DimensionTransition> cir) {
        try {
            DimensionTransition originalTransition = cir.getReturnValue();
            if (originalTransition == null || world.dimension() != Level.END) {
                return;
            }

            if (!(world.getBlockEntity(pos) instanceof TheEndGatewayBlockEntity gatewayBlockEntity)) {
                return;
            }

            TheEndGatewayBlockEntityAccessor accessor = (TheEndGatewayBlockEntityAccessor) gatewayBlockEntity;
            BlockPos exitPortal = accessor.carpetIceAddition$getExitPortal();
            if (exitPortal == null || !exitPortal.equals(ServerLevel.END_SPAWN_POINT) || !accessor.carpetIceAddition$isExactTeleport()) {
                return;
            }

            Optional<IntPosition> configuredPosition = CustomEndPlatformPositionHelper.parse(CarpetIceAdditionEndPlatformSettings.customEndPlatformPosition);
            if (configuredPosition.isEmpty()) {
                return;
            }

            BlockPos platformCenter = toBlockPos(configuredPosition.get());
            if (!carpetIceAddition$isPlatformAreaInBounds(world, platformCenter)) {
                CustomEndPlatformPositionHelper.reportOutOfBoundsPosition(configuredPosition.get().asRuleString());
                return;
            }

            Vec3 arrivalPos = Vec3.atBottomCenterOf(platformCenter.above());
            EndPlatformFeature.createEndPlatform(world, platformCenter, true);
            cir.setReturnValue(new DimensionTransition(
                    originalTransition.newLevel(),
                    arrivalPos,
                    originalTransition.speed(),
                    originalTransition.yRot(),
                    originalTransition.xRot(),
                    originalTransition.missingRespawnBlock(),
                    originalTransition.postDimensionTransition()
            ));
        } catch (Throwable throwable) {
            CustomEndPlatformPositionHelper.reportCompatibilityIssue(throwable);
        }
    }

    private static BlockPos toBlockPos(IntPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    private static boolean carpetIceAddition$isPlatformAreaInBounds(ServerLevel world, BlockPos platformCenter) {
        return world.isInWorldBounds(platformCenter.below())
                && world.isInWorldBounds(platformCenter.above(2))
                && world.isInWorldBounds(platformCenter.offset(-2, -1, -2))
                && world.isInWorldBounds(platformCenter.offset(2, 2, 2));
    }
}
