package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemEasyWaterloggedBlockPlacementMixin {
    @Unique
    private static final ThreadLocal<ItemPlacementContext> CARPET_ICE_ADDITION$PLACEMENT_CONTEXT = new ThreadLocal<>();

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("HEAD"))
    private void carpetIceAddition$cachePlacementContext(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.set(context);
    }

    @ModifyArg(
            method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"
            ),
            index = 1
    )
    private BlockState carpetIceAddition$enableWaterlogging(BlockState state) {
        try {
            if (!CarpetIceAdditionSettings.easyWaterloggedBlockPlacement) {
                return state;
            }

            ItemPlacementContext context = CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.get();
            if (context == null) {
                return state;
            }

            PlayerEntity player = context.getPlayer();
            if (player == null || !player.getOffHandStack().isOf(Items.WATER_BUCKET) || !state.contains(Properties.WATERLOGGED)) {
                return state;
            }

            return state.with(Properties.WATERLOGGED, true);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("easyWaterloggedBlockPlacement", throwable);
            return state;
        } finally {
            CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.remove();
        }
    }

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("RETURN"))
    private void carpetIceAddition$clearPlacementContext(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.remove();
    }
}
