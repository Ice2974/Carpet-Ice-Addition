package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;

@Mixin(TrialSpawnerStateData.class)
public abstract class TrialSpawnerDataMixin {

    @Redirect(
            method = "tryDetectPlayers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/trialspawner/PlayerDetector;detect(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/trialspawner/PlayerDetector$EntitySelector;Lnet/minecraft/core/BlockPos;DZ)Ljava/util/List;"
            )
    )
    private List<UUID> carpetIceAddition$filterInvisiblePlayersFromTrialSpawnerDetection(
            PlayerDetector detector,
            ServerLevel world,
            PlayerDetector.EntitySelector selector,
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
                Player player = world.getPlayerByUUID(uuid);
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
