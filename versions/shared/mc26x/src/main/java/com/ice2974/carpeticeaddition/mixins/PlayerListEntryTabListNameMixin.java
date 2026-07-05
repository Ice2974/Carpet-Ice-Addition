package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.BotTabListNameHelper;
import com.ice2974.carpeticeaddition.rules.RealPlayerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PlayerListEntryTabListNameMixin {
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void carpetIceAddition$modifyBotTabListName(CallbackInfoReturnable<Component> cir) {
        if (!BotTabListNameHelper.shouldApply()) {
            return;
        }

        try {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (!RealPlayerHelper.isFakePlayer(player)) {
                return;
            }

            cir.setReturnValue(BotTabListNameHelper.buildDisplayName(player, cir.getReturnValue()));
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("botTabListName", throwable);
        }
    }
}