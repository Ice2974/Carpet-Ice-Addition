package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.passive.WanderingTraderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraderEntityMixin {

    @Inject(method = "tickDespawnDelay", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$namedWanderingTraderPersistence(CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.namedWanderingTraderPersistence) {
            return;
        }

        // 仅在原版即将 1→0→discard 的临界 tick 取消，使 despawnDelay 停在 1：
        // TraderLlamaEntity 每刻按商人 despawnDelay-1 同步自身计时，提前冻结会连带延长羊驼生命周期。
        WanderingTraderEntity trader = (WanderingTraderEntity) (Object) this;
        if (trader.getDespawnDelay() == 1 && !trader.hasCustomer() && trader.hasCustomName()) {
            ci.cancel();
        }
    }
}
