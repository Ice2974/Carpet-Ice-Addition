package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.LegacyPvpRuleHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class DisablePlayerAttackingTamedMobsMixin {

    @Inject(
            method = "isInvulnerableTo(Lnet/minecraft/entity/damage/DamageSource;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void carpetIceAddition$disablePlayerAttackingTamedMobs(
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

            Entity target = (Entity) (Object) this;
            if (!this.carpetIceAddition$isTamedTarget(target)) {
                return;
            }

            if (!LegacyPvpRuleHelper.isPvpEnabled(target.getServer()) || this.carpetIceAddition$isOwnedByAttacker(target, attacker)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("disablePlayerAttackingTamedMobs", throwable);
        }
    }

    private boolean carpetIceAddition$isTamedTarget(Entity target) {
        if (target instanceof TameableEntity tameableEntity) {
            return tameableEntity.isTamed();
        }
        return target instanceof AbstractHorseEntity horseEntity && horseEntity.isTame();
    }

    private boolean carpetIceAddition$isOwnedByAttacker(Entity target, PlayerEntity attacker) {
        UUID ownerUuid = this.carpetIceAddition$getOwnerUuid(target);
        return ownerUuid != null && ownerUuid.equals(attacker.getUuid());
    }

    private UUID carpetIceAddition$getOwnerUuid(Entity target) {
        if (target instanceof TameableEntity tameableEntity) {
            return tameableEntity.getOwnerUuid();
        }
        if (target instanceof AbstractHorseEntity horseEntity) {
            return horseEntity.getOwnerUuid();
        }
        return null;
    }
}
