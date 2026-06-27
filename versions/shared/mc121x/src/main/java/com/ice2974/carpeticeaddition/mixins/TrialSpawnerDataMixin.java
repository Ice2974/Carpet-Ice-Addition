package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.spawner.EntityDetector;
import net.minecraft.block.spawner.TrialSpawnerData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(TrialSpawnerData.class)
public abstract class TrialSpawnerDataMixin {

    @Redirect(
            method = "updatePlayers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/spawner/EntityDetector;detect(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/block/spawner/EntityDetector$Selector;Lnet/minecraft/util/math/BlockPos;DZ)Ljava/util/List;"
            )
    )
    private List<UUID> carpetIceAddition$filterInvisiblePlayersFromTrialSpawnerDetection(
            EntityDetector detector,
            ServerWorld world,
            EntityDetector.Selector selector,
            BlockPos pos,
            double range,
            boolean requireLineOfSight
    ) {
        List<UUID> detectedPlayers = detector.detect(world, selector, pos, range, requireLineOfSight);
        if (!CarpetIceAdditionSettings.spawnersIgnoreInvisiblePlayers) {
            return detectedPlayers;
        }

        try {
            if (detectedPlayers.isEmpty()) {
                return detectedPlayers;
            }

            List<UUID> filteredPlayers = new ArrayList<>(detectedPlayers.size());
            for (UUID uuid : detectedPlayers) {
                PlayerEntity player = world.getPlayerByUuid(uuid);
                if (player == null || !player.isInvisible()) {
                    filteredPlayers.add(uuid);
                }
            }
            return filteredPlayers;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("spawnersIgnoreInvisiblePlayers", throwable);
            return detectedPlayers;
        }
    }
}
