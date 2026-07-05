package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.RealPlayerHelper;
import com.ice2974.carpeticeaddition.rules.RuleMessageThrottle;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class SafeScaffoldingBreakMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$guardScaffoldingBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CarpetIceAdditionSettings.safeScaffoldingBreak) {
            return;
        }

        try {
            if (RealPlayerHelper.isFakePlayer(this.player)) {
                return;
            }

            BlockState state = this.player.level().getBlockState(pos);
            if (!state.is(Blocks.SCAFFOLDING)) {
                return;
            }

            ItemStack mainHand = this.player.getMainHandItem();
            if (mainHand.isEmpty() || mainHand.is(Items.SCAFFOLDING)) {
                return;
            }

            if (RuleMessageThrottle.shouldSendScaffoldingWarning(this.player)) {
                this.player.sendSystemMessage(Component.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.safe_scaffolding_break")), true);
            }
            cir.setReturnValue(false);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("safeScaffoldingBreak", throwable);
        }
    }
}
