package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.EasyCopperOxidationHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeEasyCopperOxidationMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$oxidizeCopper(ServerPlayer player, Level level, ItemStack stack,
                                                  InteractionHand hand, BlockHitResult hit,
                                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (!CarpetIceAdditionSettings.easyCopperOxidation || !EasyCopperOxidationHelper.isWaterBottle(stack)
                || player.isSpectator() || !(level instanceof ServerLevel serverLevel)
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

        if (!EasyCopperOxidationHelper.tryOxidize(serverLevel, hit.getBlockPos(), player)) {
            return;
        }

        if (!player.hasInfiniteMaterials()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
