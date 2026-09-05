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

            //#disable-remap player.level() 存在多版本双重解析（<=1.21.5 解析 Entity.level=method_37908、>=1.21.6 解析 Player.level=method_51469），
            //# 保持原文本让各版本 javac 自行解析回基线方法（与 mojmap-unified 档行为一致）
            BlockState state = this.player.level().getBlockState(pos);
            //#enable-remap
            if (!state.is(Blocks.SCAFFOLDING)) {
                return;
            }

            ItemStack mainHand = this.player.getMainHandItem();
            if (mainHand.isEmpty() || mainHand.is(Items.SCAFFOLDING)) {
                return;
            }

            if (RuleMessageThrottle.shouldSendScaffoldingWarning(this.player)) {
                this.player.displayClientMessage(Component.literal(TranslationFormatUtil.translate("message.carpet-ice-addition.safe_scaffolding_break")), true);
            }
            cir.setReturnValue(false);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("safeScaffoldingBreak", throwable);
        }
    }
}
