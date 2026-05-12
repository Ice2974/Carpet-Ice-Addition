package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin {
    @Shadow
    private boolean fixed;

    @Shadow
    public abstract ItemStack getHeldItemStack();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$itemFrameFeatures(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.isSpectator() || this.getHeldItemStack().isEmpty()) {
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        boolean phantomMembrane = CarpetIceAdditionSettings.invisibleItemFrames && stack.isOf(Items.PHANTOM_MEMBRANE);
        boolean glassPane = CarpetIceAdditionSettings.fixedItemFrames && stack.isOf(Items.GLASS_PANE);
        boolean axe = CarpetIceAdditionSettings.fixedItemFrames && stack.isIn(ItemTags.AXES);
        if (!phantomMembrane && !glassPane && !axe) {
            return;
        }

        ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
        if (frame.getWorld().isClient()) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        if (phantomMembrane) {
            if (!frame.isInvisible()) {
                frame.setInvisible(true);
                decrementUnlessCreative(stack, player);
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        } else if (glassPane) {
            if (!this.fixed) {
                this.fixed = true;
                decrementUnlessCreative(stack, player);
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        } else if (this.fixed) {
            this.fixed = false;
            ((AbstractDecorationEntity) (Object) this).dropStack((ServerWorld) frame.getWorld(), new ItemStack(Items.GLASS_PANE), 0.0F);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "onBreak", at = @At("HEAD"))
    private void carpetIceAddition$dropPhantomMembrane(ServerWorld world, Entity breaker, CallbackInfo ci) {
        ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
        if (CarpetIceAdditionSettings.invisibleItemFrames && frame.isInvisible()) {
            ((AbstractDecorationEntity) (Object) this).dropStack(world, new ItemStack(Items.PHANTOM_MEMBRANE), 0.0F);
        }
    }

    private static void decrementUnlessCreative(ItemStack stack, PlayerEntity player) {
        if (!player.isInCreativeMode()) {
            stack.decrement(1);
        }
    }
}
