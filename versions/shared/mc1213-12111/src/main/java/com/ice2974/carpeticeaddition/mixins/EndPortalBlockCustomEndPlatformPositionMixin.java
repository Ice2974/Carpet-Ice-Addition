package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CustomEndPlatformPositionHelper;
import com.ice2974.carpeticeaddition.rules.CustomEndPlatformPositionHelper.IntPosition;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionEndPlatformSettings;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.EndPlatformFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockCustomEndPlatformPositionMixin {
    private static final float END_PLATFORM_YAW = 270.0F;

    @Inject(method = "createTeleportTarget", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$customEndPlatformPosition(ServerWorld world, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTarget> cir) {
        try {
            if (world.getRegistryKey() == World.END) {
                return;
            }

            Optional<IntPosition> configuredPosition = CustomEndPlatformPositionHelper.parse(CarpetIceAdditionEndPlatformSettings.customEndPlatformPosition);
            if (configuredPosition.isEmpty()) {
                return;
            }

            ServerWorld endWorld = world.getServer().getWorld(World.END);
            if (endWorld == null) {
                cir.setReturnValue(null);
                return;
            }

            BlockPos platformCenter = toBlockPos(configuredPosition.get());
            if (!carpetIceAddition$isPlatformAreaInBounds(endWorld, platformCenter)) {
                CustomEndPlatformPositionHelper.reportOutOfBoundsPosition(configuredPosition.get().asRuleString());
                return;
            }

            Vec3d arrivalPos = platformCenter.up().toBottomCenterPos();
            EndPlatformFeature.generate(endWorld, platformCenter, true);
            Set<PositionFlag> relatives = PositionFlag.combine(PositionFlag.DELTA, Set.of(PositionFlag.X_ROT));
            if (entity instanceof ServerPlayerEntity) {
                arrivalPos = arrivalPos.subtract(0.0, 1.0, 0.0);
            }

            cir.setReturnValue(new TeleportTarget(
                    endWorld,
                    arrivalPos,
                    Vec3d.ZERO,
                    END_PLATFORM_YAW,
                    0.0F,
                    relatives,
                    TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET)
            ));
        } catch (Throwable throwable) {
            CustomEndPlatformPositionHelper.reportCompatibilityIssue(throwable);
        }
    }

    private static BlockPos toBlockPos(IntPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    private static boolean carpetIceAddition$isPlatformAreaInBounds(ServerWorld world, BlockPos platformCenter) {
        return world.isInBuildLimit(platformCenter.down()) && world.isInBuildLimit(platformCenter.up(2));
    }
}
