package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.IceLikeMagmaBlocksHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerBreakIceLikeMagmaBlocksMixin {
    @Shadow @Final protected ServerPlayer player;

    @Unique private @Nullable BlockPos carpetIceAddition$pendingMagmaBreakPos;
    @Unique private boolean carpetIceAddition$pendingMagmaBreak;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void carpetIceAddition$captureMagmaBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$pendingMagmaBreakPos = pos;
        this.carpetIceAddition$pendingMagmaBreak = this.player.level().getBlockState(pos).is(Blocks.MAGMA_BLOCK);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
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

            ItemStack tool = this.player.getMainHandItem();
            boolean hasSilkTouch = EnchantmentHelper.getEnchantmentsForCrafting(tool)
                    .entrySet()
                    .stream()
                    .anyMatch(entry -> entry.getKey().is(Enchantments.SILK_TOUCH) && entry.getIntValue() > 0);

            BlockState belowState = this.player.level().getBlockState(brokenPos.below());
            // blocksMotion() 无非弃用等价 API：语义 = 排除 COBWEB/BAMBOO_SAPLING + legacySolid 尺寸阈值判定，保留原调用以冻结该语义
            @SuppressWarnings("deprecation")
            boolean blocksMovement = belowState.blocksMotion();
            boolean validSolidSupport = IceLikeMagmaBlocksHelper.isValidSolidSupport(
                    blocksMovement,
                    belowState.is(Blocks.COBWEB),
                    belowState.is(Blocks.BAMBOO_SAPLING)
            );
            boolean liquidSupport = !belowState.getFluidState().isEmpty();

            if (!IceLikeMagmaBlocksHelper.shouldCreateLavaSource(
                    CarpetIceAdditionSettings.iceLikeMagmaBlocks,
                    hasSilkTouch,
                    IceLikeMagmaBlocksHelper.hasIceLikeSupport(validSolidSupport, liquidSupport)
            )) {
                return;
            }

            this.player.level().setBlockAndUpdate(brokenPos, Blocks.LAVA.defaultBlockState());
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("iceLikeMagmaBlocks", throwable);
        } finally {
            this.carpetIceAddition$pendingMagmaBreakPos = null;
            this.carpetIceAddition$pendingMagmaBreak = false;
        }
    }
}
