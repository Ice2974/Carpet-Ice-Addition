package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 26.3 override：Block.playerDestroy 参数类型收窄为 (ServerLevel, ServerPlayer, ...)，
// 根 src 保留 ≤26.2 的 (Level, Player, ...) 描述符形态（描述符差异按仓库约定以 override 表达，不进宏）。
@Mixin(Block.class)
public abstract class IceLikeMagmaBlocksNoDropMixin {

    @Inject(
            method = "playerDestroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"
            ),
            cancellable = true
    )
    private void carpetIceAddition$suppressMagmaBlockDrop(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool,
            CallbackInfo ci
    ) {
        if (!CarpetIceAdditionSettings.iceLikeMagmaBlocks) {
            return;
        }

        try {
            if (!state.is(Blocks.MAGMA_BLOCK)) {
                return;
            }
            if (player.isCreative()) {
                return;
            }

            boolean hasSilkTouch = EnchantmentHelper.getEnchantmentsForCrafting(tool)
                    .entrySet()
                    .stream()
                    .anyMatch(entry -> entry.getKey().is(Enchantments.SILK_TOUCH) && entry.getIntValue() > 0);
            if (hasSilkTouch) {
                return;
            }

            ci.cancel();
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("iceLikeMagmaBlocks", throwable);
        }
    }
}
