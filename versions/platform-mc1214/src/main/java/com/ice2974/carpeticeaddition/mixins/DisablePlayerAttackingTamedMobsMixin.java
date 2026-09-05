package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.LegacyPvpRuleHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

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

            if (!LegacyPvpRuleHelper.isPvpEnabled(world.getServer()) || this.carpetIceAddition$isOwnedByAttacker(target, attacker)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("disablePlayerAttackingTamedMobs", throwable);
        }
    }

    private boolean carpetIceAddition$isTamedTarget(LivingEntity target) {
        if (target instanceof TamableAnimal tameableEntity) {
            return tameableEntity.isTame();
        }
        return target instanceof AbstractHorse horseEntity && horseEntity.isTamed();
    }

    private boolean carpetIceAddition$isOwnedByAttacker(LivingEntity target, Player attacker) {
        UUID ownerUuid = this.carpetIceAddition$getOwnerUuid(target);
        return ownerUuid != null && ownerUuid.equals(attacker.getUUID());
    }

    private UUID carpetIceAddition$getOwnerUuid(LivingEntity target) {
        if (target instanceof TamableAnimal tameableEntity) {
            return tameableEntity.getOwnerUUID();
        }
        if (target instanceof AbstractHorse horseEntity) {
            return horseEntity.getOwnerUUID();
        }
        return null;
    }
}
