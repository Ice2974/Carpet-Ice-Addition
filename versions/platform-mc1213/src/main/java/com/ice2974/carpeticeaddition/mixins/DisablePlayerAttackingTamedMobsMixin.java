package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class DisablePlayerAttackingTamedMobsMixin {

    @Inject(
            method = "isInvulnerableTo(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void carpetIceAddition$disablePlayerAttackingTamedMobs(
            ServerWorld world,
            DamageSource damageSource,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CarpetIceAdditionSettings.disablePlayerAttackingTamedMobs) {
            return;
        }

        try {
            if (!(damageSource.getAttacker() instanceof PlayerEntity attacker)) {
                return;
            }
            if (this.carpetIceAddition$isTamedByAttacker((LivingEntity) (Object) this, attacker)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("disablePlayerAttackingTamedMobs", throwable);
        }
    }

    private boolean carpetIceAddition$isTamedByAttacker(LivingEntity target, PlayerEntity attacker) {
        UUID ownerUuid;
        if (target instanceof TameableEntity tameableEntity) {
            if (!tameableEntity.isTamed()) {
                return false;
            }
            ownerUuid = tameableEntity.getOwnerUuid();
        } else if (target instanceof AbstractHorseEntity horseEntity) {
            if (!horseEntity.isTame()) {
                return false;
            }
            ownerUuid = horseEntity.getOwnerUuid();
        } else {
            return false;
        }

        return ownerUuid != null && ownerUuid.equals(attacker.getUuid());
    }
}
