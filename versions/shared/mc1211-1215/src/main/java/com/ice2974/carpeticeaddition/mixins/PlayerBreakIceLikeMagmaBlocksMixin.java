package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.IceLikeMagmaBlocksHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class PlayerBreakIceLikeMagmaBlocksMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Unique private @Nullable BlockPos carpetIceAddition$pendingMagmaBreakPos;
    @Unique private boolean carpetIceAddition$pendingMagmaBreak;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"))
    private void carpetIceAddition$captureMagmaBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$pendingMagmaBreakPos = pos;
        this.carpetIceAddition$pendingMagmaBreak = this.player.getServerWorld().getBlockState(pos).isOf(Blocks.MAGMA_BLOCK);
    }

    @Inject(method = "tryBreakBlock", at = @At("RETURN"))
    private void carpetIceAddition$createLavaSourceAfterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!this.carpetIceAddition$pendingMagmaBreak
                    || !cir.getReturnValueZ()
                    || !CarpetIceAdditionSettings.iceLikeMagmaBlocks
                    || this.player.isCreative()) {
                return;
            }

            BlockPos brokenPos = this.carpetIceAddition$pendingMagmaBreakPos;
            if (brokenPos == null) {
                return;
            }

            ItemStack tool = this.player.getMainHandStack();
            boolean hasSilkTouch = EnchantmentHelper.getEnchantments(tool)
                    .getEnchantmentEntries()
                    .stream()
                    .anyMatch(entry -> entry.getKey().matchesKey(Enchantments.SILK_TOUCH) && entry.getIntValue() > 0);

            BlockState belowState = this.player.getServerWorld().getBlockState(brokenPos.down());
            boolean validSolidSupport = IceLikeMagmaBlocksHelper.isValidSolidSupport(
                    belowState.blocksMovement(),
                    belowState.isOf(Blocks.COBWEB),
                    belowState.isOf(Blocks.BAMBOO_SAPLING)
            );
            boolean liquidSupport = !belowState.getFluidState().isEmpty();

            if (!IceLikeMagmaBlocksHelper.shouldCreateLavaSource(
                    CarpetIceAdditionSettings.iceLikeMagmaBlocks,
                    hasSilkTouch,
                    IceLikeMagmaBlocksHelper.hasIceLikeSupport(validSolidSupport, liquidSupport)
            )) {
                return;
            }

            this.player.getServerWorld().setBlockState(brokenPos, Blocks.LAVA.getDefaultState());
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("iceLikeMagmaBlocks", throwable);
        } finally {
            this.carpetIceAddition$pendingMagmaBreakPos = null;
            this.carpetIceAddition$pendingMagmaBreak = false;
        }
    }
}
