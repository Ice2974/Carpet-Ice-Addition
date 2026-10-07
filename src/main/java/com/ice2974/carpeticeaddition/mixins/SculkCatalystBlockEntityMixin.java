package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkCatalystBlockEntity.class)
public abstract class SculkCatalystBlockEntityMixin {
    // woolSuppressesSculkSpread 开启时，催发体上方有羊毛则永久取消其已有的 charge cursor。
    // 每个 block entity 持有 1:1 的 SculkSpreader，clear 只影响该催发体；cursor 为空时
    // 短路返回，不查询方块也不标脏。清空后 updateCursors 对空列表天然 no-op，无需取消原调用。
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void carpetIceAddition$cancelOngoingSpreadIfWoolCovered(Level level, BlockPos pos,
            BlockState state, SculkCatalystBlockEntity blockEntity, CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.woolSuppressesSculkSpread) {
            return;
        }
        SculkSpreader spreader = blockEntity.getListener().getSculkSpreader();
        if (spreader.getCursors().isEmpty()) {
            return;
        }
        if (level.getBlockState(pos.above()).is(BlockTags.WOOL)) {
            spreader.clear();
            // clear() 只清内存列表、不标记 block entity 脏位；若不 setChanged()，
            // 取消后的空列表不会被持久化，世界保存 / 重启会从旧 NBT 恢复已取消的 cursor。
            blockEntity.setChanged();
        }
    }
}
