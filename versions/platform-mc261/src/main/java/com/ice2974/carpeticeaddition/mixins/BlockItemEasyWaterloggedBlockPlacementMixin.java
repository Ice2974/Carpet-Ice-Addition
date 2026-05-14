package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemEasyWaterloggedBlockPlacementMixin {
    @Unique
    private static final ThreadLocal<BlockPlaceContext> CARPET_ICE_ADDITION$PLACEMENT_CONTEXT = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<Boolean> CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK = ThreadLocal.withInitial(() -> false);

    @Inject(method = "placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"))
    private void carpetIceAddition$cachePlacementContext(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.set(context);
        CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.set(false);
    }

    @ModifyArg(
            method = "placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            index = 1
    )
    private BlockState carpetIceAddition$enableWaterlogging(BlockState state) {
        if (!CarpetIceAdditionSettings.easyWaterloggedBlockPlacement) {
            return state;
        }

        try {
            BlockPlaceContext context = CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.get();
            if (context == null) {
                return state;
            }

            Player player = context.getPlayer();
            if (player == null || player.getOffhandItem().getItem() != Items.WATER_BUCKET || !state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                return state;
            }

            BlockState waterloggedState = state.setValue(BlockStateProperties.WATERLOGGED, true);
            if (waterloggedState != state) {
                CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.set(true);
            }
            return waterloggedState;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("easyWaterloggedBlockPlacement", throwable);
            return state;
        }
    }

    @Inject(method = "placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("RETURN"))
    private void carpetIceAddition$scheduleWaterTick(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValueZ() || !CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.get()) {
                return;
            }

            Level world = context.getLevel();
            world.scheduleTick(context.getClickedPos(), Fluids.WATER, Fluids.WATER.getTickDelay(world));
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("easyWaterloggedBlockPlacement", throwable);
        } finally {
            CARPET_ICE_ADDITION$PLACEMENT_CONTEXT.remove();
            CARPET_ICE_ADDITION$SHOULD_SCHEDULE_WATER_TICK.remove();
        }
    }
}
