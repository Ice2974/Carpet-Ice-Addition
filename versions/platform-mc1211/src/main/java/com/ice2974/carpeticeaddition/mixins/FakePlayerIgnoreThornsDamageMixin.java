package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.RealPlayerHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class FakePlayerIgnoreThornsDamageMixin {

    @Inject(
            target = @Desc(
                    value = "damage",
                    args = {DamageSource.class, float.class},
                    ret = boolean.class
            ),
            at = @At("HEAD"),
            cancellable = true
    )
    private void carpetIceAddition$ignoreThornsDamageForFakePlayerMc1210(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        this.carpetIceAddition$tryCancelFakePlayerThornsDamage(source, cir);
    }

    private void carpetIceAddition$tryCancelFakePlayerThornsDamage(
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CarpetIceAdditionSettings.fakePlayerIgnoreThornsDamage) {
            return;
        }

        try {
            if (!source.isOf(DamageTypes.THORNS)) {
                return;
            }
            if (!((Object) this instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }
            if (!RealPlayerHelper.isFakePlayer(serverPlayer)) {
                return;
            }

            cir.setReturnValue(false);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("fakePlayerIgnoreThornsDamage", throwable);
        }
    }
}
