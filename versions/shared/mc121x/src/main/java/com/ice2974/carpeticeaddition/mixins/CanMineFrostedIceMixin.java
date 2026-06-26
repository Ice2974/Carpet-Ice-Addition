package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class CanMineFrostedIceMixin {

    @Inject(
            method = "dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void carpetIceAddition$dropFrostedIceWithSilkTouch(
            BlockState state,
            World world,
            BlockPos pos,
            @Nullable BlockEntity blockEntity,
            @Nullable Entity entity,
            ItemStack tool,
            CallbackInfo ci
    ) {
        if (!CarpetIceAdditionSettings.silkTouchFrostedIce) {
            return;
        }

        try {
            if (!(world instanceof ServerWorld serverWorld) || !state.isOf(Blocks.FROSTED_ICE)) {
                return;
            }
            if (entity instanceof PlayerEntity player && player.isCreative()) {
                return;
            }

            boolean hasSilkTouch = EnchantmentHelper.getEnchantments(tool)
                    .getEnchantmentEntries()
                    .stream()
                    .anyMatch(entry -> entry.getKey().matchesKey(Enchantments.SILK_TOUCH) && entry.getIntValue() > 0);
            if (!hasSilkTouch) {
                return;
            }

            Block.dropStack(world, pos, new ItemStack(Blocks.ICE));
            state.onStacksDropped(serverWorld, pos, tool, true);
            ci.cancel();
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("silkTouchFrostedIce", throwable);
        }
    }
}