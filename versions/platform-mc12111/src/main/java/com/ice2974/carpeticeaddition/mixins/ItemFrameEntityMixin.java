package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper;
import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper.FrameCustomizationAction;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin {
    @Unique private boolean carpetIceAddition$invisibleFramePaid;
    @Unique private boolean carpetIceAddition$fixedFramePaid;

    @Shadow
    private boolean fixed;

    @Shadow
    public abstract ItemStack getHeldItemStack();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$handleCustomizationTool(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
        ItemStack heldStack = player.getStackInHand(hand);
        FrameCustomizationAction action = ItemFrameInteractionHelper.resolveAction(
                player.isSpectator(),
                !this.getHeldItemStack().isEmpty(),
                CarpetIceAdditionSettings.invisibleItemFrames,
                CarpetIceAdditionSettings.fixedItemFrames,
                frame.isInvisible(),
                this.fixed,
                heldStack.isOf(Items.PHANTOM_MEMBRANE),
                heldStack.isOf(Items.GLASS_PANE),
                heldStack.isIn(ItemTags.AXES)
        );
        if (action == FrameCustomizationAction.NONE) {
            return;
        }

        if (frame.getEntityWorld().isClient()) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        switch (action) {
            case CONSUME_INTERACTION -> cir.setReturnValue(ActionResult.SUCCESS);
            case APPLY_INVISIBLE -> {
                boolean paidBefore = this.carpetIceAddition$invisibleFramePaid;
                frame.setInvisible(true);
                this.carpetIceAddition$invisibleFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logInvisibleApplied(frame.getUuid(), true, player.isInCreativeMode(), paidBefore, this.carpetIceAddition$invisibleFramePaid);
                cir.setReturnValue(ActionResult.SUCCESS);
            }
            case APPLY_FIXED -> {
                boolean paidBefore = this.carpetIceAddition$fixedFramePaid;
                this.fixed = true;
                this.carpetIceAddition$fixedFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logFixedApplied(frame.getUuid(), true, player.isInCreativeMode(), paidBefore, this.carpetIceAddition$fixedFramePaid);
                cir.setReturnValue(ActionResult.SUCCESS);
            }
            case CLEAR_FIXED -> {
                boolean paidBefore = this.carpetIceAddition$fixedFramePaid;
                ItemFrameInteractionHelper.logFixedClearAttempt(frame.getUuid(), this.fixed, paidBefore, paidBefore);
                if (paidBefore) {
                    carpetIceAddition$spawnRefundItem((ServerWorld) frame.getEntityWorld(), new ItemStack(Items.GLASS_PANE));
                    this.carpetIceAddition$fixedFramePaid = false;
                }
                this.fixed = false;
                cir.setReturnValue(ActionResult.SUCCESS);
            }
            default -> {
            }
        }
    }

    @Inject(method = "onBreak", at = @At("HEAD"))
    private void carpetIceAddition$refundPaidInvisibleMaterial(ServerWorld world, Entity breaker, CallbackInfo ci) {
        ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
        boolean paidBefore = this.carpetIceAddition$invisibleFramePaid;
        boolean refundTriggered = paidBefore;
        ItemFrameInteractionHelper.logInvisibleRefundAttempt(frame.getUuid(), frame.isInvisible(), paidBefore, refundTriggered);
        if (refundTriggered) {
            carpetIceAddition$spawnRefundItem(world, new ItemStack(Items.PHANTOM_MEMBRANE));
            this.carpetIceAddition$invisibleFramePaid = false;
        }
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void carpetIceAddition$writePaidFlags(WriteView view, CallbackInfo ci) {
        view.putBoolean(ItemFrameInteractionHelper.INVISIBLE_FRAME_PAID_KEY, this.carpetIceAddition$invisibleFramePaid);
        view.putBoolean(ItemFrameInteractionHelper.FIXED_FRAME_PAID_KEY, this.carpetIceAddition$fixedFramePaid);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void carpetIceAddition$readPaidFlags(ReadView view, CallbackInfo ci) {
        this.carpetIceAddition$invisibleFramePaid = view.getBoolean(ItemFrameInteractionHelper.INVISIBLE_FRAME_PAID_KEY, false);
        this.carpetIceAddition$fixedFramePaid = view.getBoolean(ItemFrameInteractionHelper.FIXED_FRAME_PAID_KEY, false);
    }

    private static boolean carpetIceAddition$consumeSingleItem(ItemStack stack, PlayerEntity player) {
        if (player.isInCreativeMode()) {
            return false;
        }
        stack.decrement(1);
        return true;
    }

    @Unique
    private void carpetIceAddition$spawnRefundItem(ServerWorld world, ItemStack stack) {
        ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
        ItemEntity itemEntity = new ItemEntity(world, frame.getX(), frame.getY(), frame.getZ(), stack);
        world.spawnEntity(itemEntity);
    }
}
