package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuToughAnvilMixin {

    @WrapOperation(
            method = "onTake",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/ContainerLevelAccess;execute(Ljava/util/function/BiConsumer;)V"
            ),
            require = 1
    )
    private void carpetIceAddition$wrapTakeAccessExecute(
            ContainerLevelAccess access,
            BiConsumer<Level, BlockPos> consumer,
            Operation<Void> original
    ) {
        if (CarpetIceAdditionSettings.preventsAnvilUseDamage()) {
            original.call(access, (BiConsumer<Level, BlockPos>) (level, pos) ->
                    level.levelEvent(LevelEvent.SOUND_ANVIL_USED, pos, 0));
        } else {
            original.call(access, consumer);
        }
    }
}
