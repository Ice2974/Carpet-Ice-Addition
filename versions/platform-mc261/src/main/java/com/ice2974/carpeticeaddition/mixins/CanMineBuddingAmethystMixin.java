package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class CanMineBuddingAmethystMixin {

    @Inject(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void carpetIceAddition$dropBuddingAmethystWithSilkTouch(
            BlockState state,
            Level world,
            BlockPos pos,
            @Nullable BlockEntity blockEntity,
            @Nullable Entity entity,
            ItemStack tool,
            CallbackInfo ci
    ) {
        if (!CarpetIceAdditionSettings.silkTouchBuddingAmethyst
               ) {
            return;
        }

        try {
            if (!(world instanceof ServerLevel serverWorld) || !state.is(Blocks.BUDDING_AMETHYST)) {
                return;
            }
            if (entity instanceof Player player && player.isCreative()) {
                return;
            }
            if (!tool.isCorrectToolForDrops(state)) {
                return;
            }

            boolean hasSilkTouch = EnchantmentHelper.getEnchantmentsForCrafting(tool)
                    .entrySet()
                    .stream()
                    .anyMatch(entry -> entry.getKey().is(Enchantments.SILK_TOUCH) && entry.getIntValue() > 0);
            if (!hasSilkTouch) {
                return;
            }

            Block.popResource(world, pos, new ItemStack(Blocks.BUDDING_AMETHYST));
            state.spawnAfterBreak(serverWorld, pos, tool, true);
            ci.cancel();
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("silkTouchBuddingAmethyst", throwable);
        }
    }
}