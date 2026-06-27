package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityNeutralPhantomsMixin {
    @Inject(method = "damage", at = @At("RETURN"))
    private void carpetIceAddition$rememberNeutralPhantomsAttacker(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CarpetIceAdditionSettings.neutralPhantoms || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!((Object) this instanceof PhantomEntity phantom)) {
            return;
        }
        try {
            ServerPlayerEntity player = carpetIceAddition$getAttackingPlayer(source);
            if (player != null && phantom instanceof NeutralPhantomsRetaliationTracker tracker) {
                tracker.carpetIceAddition$recordNeutralPhantomsRetaliationTarget(player);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("neutralPhantoms", throwable);
        }
    }

    @Unique
    private ServerPlayerEntity carpetIceAddition$getAttackingPlayer(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity player) {
            return player;
        }

        Entity directSource = source.getSource();
        if (directSource instanceof ServerPlayerEntity player) {
            return player;
        }
        return null;
    }
}
