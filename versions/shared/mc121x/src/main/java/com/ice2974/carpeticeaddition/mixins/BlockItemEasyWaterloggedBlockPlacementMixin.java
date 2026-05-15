package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.world.World;
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
    @Unique
    private static final ThreadLocal<Boolean> CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK = ThreadLocal.withInitial(() -> false);

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("HEAD"))
    private void carpetIceAddition$cachePlacementContext(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.set(context);
        CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.set(false);
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
        if (!CarpetIceAdditionSettings.easyWaterloggedBlockPlacement) {
            return state;
        }

        try {
            ItemPlacementContext context = CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.get();
            if (context == null) {
                return state;
            }

            PlayerEntity player = context.getPlayer();
            if (player == null || !player.getOffHandStack().isOf(Items.WATER_BUCKET) || !state.contains(Properties.WATERLOGGED)) {
                return state;
            }

            BlockState waterloggedState = state.with(Properties.WATERLOGGED, true);
            if (waterloggedState != state) {
                CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.set(true);
            }
            return waterloggedState;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("easyWaterloggedBlockPlacement", throwable);
            return state;
        }
    }

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z", at = @At("RETURN"))
    private void carpetIceAddition$scheduleWaterTick(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValueZ() || !CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.get()) {
                return;
            }

            World world = context.getWorld();
            world.scheduleFluidTick(context.getBlockPos(), Fluids.WATER, Fluids.WATER.getTickRate(world));
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("easyWaterloggedBlockPlacement", throwable);
        } finally {
            CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.remove();
            CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.remove();
        }
    }
}
