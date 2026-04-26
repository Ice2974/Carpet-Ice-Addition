package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobSpawnerLogic.class)
public abstract class MobSpawnerLogicMixin {

    @Redirect(
            method = "isPlayerInRange",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;isPlayerInRange(DDDD)Z"
            )
    )
    private boolean carpetIceAddition$ignoreInvisiblePlayersForSpawnerActivation(
            World world,
            double x,
            double y,
            double z,
            double range
    ) {
        if (!CarpetIceAdditionSettings.spawnersIgnoreInvisiblePlayers
               ) {
            return world.isPlayerInRange(x, y, z, range);
        }

        try {
            for (PlayerEntity player : world.getPlayers()) {
                if (player.isInvisible()) {
                    continue;
                }
                if (!EntityPredicates.EXCEPT_SPECTATOR.test(player)) {
                    continue;
                }
                if (!EntityPredicates.VALID_LIVING_ENTITY.test(player)) {
                    continue;
                }

                double distanceSquared = player.squaredDistanceTo(x, y, z);
                if (range < 0.0D || distanceSquared < range * range) {
                    return true;
                }
            }
            return false;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("spawnersIgnoreInvisiblePlayers", throwable);
            return world.isPlayerInRange(x, y, z, range);
        }
    }
}
