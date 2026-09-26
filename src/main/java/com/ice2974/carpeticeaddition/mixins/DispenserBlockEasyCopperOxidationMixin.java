package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.EasyCopperOxidationHelper;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockEasyCopperOxidationMixin {
    @Inject(method = "getDispenseMethod", at = @At("RETURN"), cancellable = true)
    private void carpetIceAddition$wrapWaterBottleBehavior(Level level, ItemStack stack,
                                                             CallbackInfoReturnable<DispenseItemBehavior> cir) {
        if (!CarpetIceAdditionSettings.easyCopperOxidation || !EasyCopperOxidationHelper.isWaterBottle(stack)) {
            return;
        }

        DispenseItemBehavior original = cir.getReturnValue();
        cir.setReturnValue((source, selectedStack) -> {
            if (CarpetIceAdditionSettings.easyCopperOxidation
                    && EasyCopperOxidationHelper.isWaterBottle(selectedStack)) {
                BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                if (EasyCopperOxidationHelper.tryOxidize(source.level(), target)) {
                    return EasyCopperOxidationHelper.BottleRemainderBehavior.INSTANCE.dispense(source, selectedStack);
                }
            }
            return original.dispense(source, selectedStack);
        });
    }
}
