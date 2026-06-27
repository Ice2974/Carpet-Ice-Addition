package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.PvpRuleHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class DisablePlayerAttackingTamedMobsMixin {

    @Inject(
            method = "isInvulnerableTo(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carpetIceAddition$disablePlayerAttackingTamedMobs(
            ServerLevel world,
            DamageSource damageSource,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CarpetIceAdditionSettings.disablePlayerAttackingTamedMobs) {
            return;
        }

        try {
            if (!(damageSource.getEntity() instanceof Player attacker)) {
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
        if (target instanceof TamableAnimal tamableAnimal) {
            return tamableAnimal.isTame();
        }
        return target instanceof AbstractHorse horse && horse.isTamed();
    }

    private boolean carpetIceAddition$isOwnedByAttacker(LivingEntity target, Player attacker) {
        EntityReference<LivingEntity> ownerReference = this.carpetIceAddition$getOwnerReference(target);
        return ownerReference != null && ownerReference.getUUID() != null && ownerReference.getUUID().equals(attacker.getUUID());
    }

    private EntityReference<LivingEntity> carpetIceAddition$getOwnerReference(LivingEntity target) {
        if (target instanceof TamableAnimal tamableAnimal) {
            return tamableAnimal.getOwnerReference();
        }
        if (target instanceof AbstractHorse horse) {
            return horse.getOwnerReference();
        }
        return null;
    }
}
