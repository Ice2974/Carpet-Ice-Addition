package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseSpawner.class)
public abstract class MobSpawnerLogicMixin {

    @Redirect(
            method = "isNearPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;hasNearbyAlivePlayer(DDDD)Z"
            )
    )
    private boolean carpetIceAddition$ignoreInvisiblePlayersForSpawnerActivation(
            Level world,
            double x,
            double y,
            double z,
            double range
    ) {
        if (!CarpetIceAdditionSettings.spawnersIgnoreInvisiblePlayers
               ) {
            return world.hasNearbyAlivePlayer(x, y, z, range);
        }

        try {
            for (Player player : world.players()) {
                if (player.isInvisible()) {
                    continue;
                }
                if (!EntitySelector.NO_SPECTATORS.test(player)) {
                    continue;
                }
                if (!EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)) {
                    continue;
                }

                double distanceSquared = player.distanceToSqr(x, y, z);
                if (range < 0.0D || distanceSquared < range * range) {
                    return true;
                }
            }
            return false;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("spawnersIgnoreInvisiblePlayers", throwable);
            return world.hasNearbyAlivePlayer(x, y, z, range);
        }
    }
}
