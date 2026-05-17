package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.LegacyPvpRuleHelper;
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

            LivingEntity target = (LivingEntity) (Object) this;
            if (!this.carpetIceAddition$isTamedTarget(target)) {
                return;
            }

            if (!LegacyPvpRuleHelper.isPvpEnabled(world.getServer()) || this.carpetIceAddition$isOwnedByAttacker(target, attacker)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("disablePlayerAttackingTamedMobs", throwable);
        }
    }

    private boolean carpetIceAddition$isTamedTarget(LivingEntity target) {
        if (target instanceof TameableEntity tameableEntity) {
            return tameableEntity.isTamed();
        }
        return target instanceof AbstractHorseEntity horseEntity && horseEntity.isTame();
    }

    private boolean carpetIceAddition$isOwnedByAttacker(LivingEntity target, PlayerEntity attacker) {
        UUID ownerUuid = this.carpetIceAddition$getOwnerUuid(target);
        return ownerUuid != null && ownerUuid.equals(attacker.getUuid());
    }

    private UUID carpetIceAddition$getOwnerUuid(LivingEntity target) {
        if (target instanceof TameableEntity tameableEntity) {
            return tameableEntity.getOwnerUuid();
        }
        if (target instanceof AbstractHorseEntity horseEntity) {
            return horseEntity.getOwnerUuid();
        }
        return null;
    }
}
