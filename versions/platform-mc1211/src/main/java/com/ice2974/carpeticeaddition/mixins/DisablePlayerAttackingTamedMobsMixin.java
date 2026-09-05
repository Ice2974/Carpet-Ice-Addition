package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.LegacyPvpRuleHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

@Mixin(Entity.class)
public abstract class DisablePlayerAttackingTamedMobsMixin {

    @Inject(
            method = "isInvulnerableTo(Lnet/minecraft/world/damagesource/DamageSource;)Z",
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
            if (!(damageSource.getEntity() instanceof Player attacker)) {
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
        if (target instanceof TamableAnimal tameableEntity) {
            return tameableEntity.isTame();
        }
        return target instanceof AbstractHorse horseEntity && horseEntity.isTamed();
    }

    private boolean carpetIceAddition$isOwnedByAttacker(Entity target, Player attacker) {
        UUID ownerUuid = this.carpetIceAddition$getOwnerUuid(target);
        return ownerUuid != null && ownerUuid.equals(attacker.getUUID());
    }

    private UUID carpetIceAddition$getOwnerUuid(Entity target) {
        if (target instanceof TamableAnimal tameableEntity) {
            return tameableEntity.getOwnerUUID();
        }
        if (target instanceof AbstractHorse horseEntity) {
            return horseEntity.getOwnerUUID();
        }
        return null;
    }
}
