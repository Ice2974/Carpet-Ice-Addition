package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityNeutralPhantomsMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void carpetIceAddition$rememberNeutralPhantomsAttacker(
            ServerLevel serverLevel,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CarpetIceAdditionSettings.neutralPhantoms || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!((Object) this instanceof Phantom phantom)) {
            return;
        }
        try {
            ServerPlayer player = carpetIceAddition$getAttackingPlayer(source);
            if (player != null && phantom instanceof NeutralPhantomsRetaliationTracker tracker) {
                tracker.carpetIceAddition$recordNeutralPhantomsRetaliationTarget(player);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("neutralPhantoms", throwable);
        }
    }

    @Unique
    private ServerPlayer carpetIceAddition$getAttackingPlayer(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            return player;
        }

        Entity directSource = source.getDirectEntity();
        if (directSource instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }
}
