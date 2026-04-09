package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.BotTabListNameHelper;
import com.ice2974.carpeticeaddition.rules.RealPlayerHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerListEntryTabListNameMixin {
    @Inject(method = "getPlayerListName", at = @At("RETURN"), cancellable = true)
    private void carpetIceAddition$modifyBotTabListName(CallbackInfoReturnable<Text> cir) {
        if (!BotTabListNameHelper.shouldApply()) {
            return;
        }

        try {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            if (!RealPlayerHelper.isFakePlayer(player)) {
                return;
            }

            cir.setReturnValue(BotTabListNameHelper.buildDisplayName(player, cir.getReturnValue()));
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("botTabListName", throwable);
        }
    }
}