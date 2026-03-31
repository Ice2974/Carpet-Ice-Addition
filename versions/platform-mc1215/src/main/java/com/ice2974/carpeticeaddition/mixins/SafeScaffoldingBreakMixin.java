package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.RealPlayerHelper;
import com.ice2974.carpeticeaddition.rules.RuleMessageThrottle;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class SafeScaffoldingBreakMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$guardScaffoldingBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!CarpetIceAdditionSettings.safeScaffoldingBreak || !CarpetIceAdditionMod.shouldEnableSafeScaffoldingBreak()) {
            return;
        }

        try {
            if (RealPlayerHelper.isFakePlayer(this.player)) {
                return;
            }

            BlockState state = this.player.getWorld().getBlockState(pos);
            if (!state.isOf(Blocks.SCAFFOLDING)) {
                return;
            }

            ItemStack mainHand = this.player.getMainHandStack();
            if (mainHand.isEmpty() || mainHand.isOf(Items.SCAFFOLDING)) {
                return;
            }

            if (RuleMessageThrottle.shouldSendScaffoldingWarning(this.player)) {
                this.player.sendMessage(Text.translatable("message.carpet-ice-addition.safe_scaffolding_break"), true);
            }
            cir.setReturnValue(false);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("safeScaffoldingBreak", throwable);
        }
    }
}
