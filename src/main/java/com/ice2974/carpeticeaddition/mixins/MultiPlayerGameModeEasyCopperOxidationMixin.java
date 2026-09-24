package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.EasyCopperOxidationHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeEasyCopperOxidationMixin {
    @Inject(method = "performUseItemOn", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$predictCopperOxidation(LocalPlayer player, InteractionHand hand,
                                                           BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        Level level = player.level();
        if (!CarpetIceAdditionSettings.easyCopperOxidation || !EasyCopperOxidationHelper.isWaterBottle(stack)
                || player.isSpectator()
                || !level.getBlockState(hit.getBlockPos()).getBlock().isEnabled(level.enabledFeatures())) {
            return;
        }

//#if MC>=12103
        if (player.getCooldowns().isOnCooldown(stack)) {
//#else
//$$    if (player.getCooldowns().isOnCooldown(stack.getItem())) {
//#endif
            return;
        }

        if (EasyCopperOxidationHelper.eligibleNextState(level, hit.getBlockPos()).isPresent()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
