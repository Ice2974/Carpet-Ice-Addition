package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.PvpRuleHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.LazyEntityReference;
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

@Mixin(LivingEntity.class)
public abstract class DisablePlayerAttackingTamedMobsMixin {

    @Inject(
            method = "isInvulnerableTo(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;)Z",
            at = @At("HEAD"),
            cancellable = true
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

            if (!PvpRuleHelper.isPvpEnabled(world) || this.carpetIceAddition$isOwnedByAttacker(target, attacker)) {
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
        if (target instanceof AbstractHorseEntity horseEntity) {
            return horseEntity.isTame();
        }
        return false;
    }

    private boolean carpetIceAddition$isOwnedByAttacker(LivingEntity target, PlayerEntity attacker) {
        LazyEntityReference<LivingEntity> ownerReference = this.carpetIceAddition$getOwnerReference(target);
        return ownerReference != null && ownerReference.getUuid() != null && ownerReference.getUuid().equals(attacker.getUuid());
    }

    private LazyEntityReference<LivingEntity> carpetIceAddition$getOwnerReference(LivingEntity target) {
        if (target instanceof TameableEntity tameableEntity) {
            return tameableEntity.getOwnerReference();
        }
        if (target instanceof AbstractHorseEntity horseEntity) {
            return horseEntity.getOwnerReference();
        }
        return null;
    }
}
