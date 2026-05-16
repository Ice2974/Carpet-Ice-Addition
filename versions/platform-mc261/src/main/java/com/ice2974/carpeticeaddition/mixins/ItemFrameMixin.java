package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper;
import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper.FrameCustomizationAction;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Unique private boolean carpetIceAddition$invisibleFramePaid;
    @Unique private boolean carpetIceAddition$fixedFramePaid;

    @Shadow
    private boolean fixed;

    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$handleCustomizationTool(Player player, InteractionHand hand, Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        ItemFrame frame = (ItemFrame) (Object) this;
        ItemStack heldStack = player.getItemInHand(hand);
        FrameCustomizationAction action = ItemFrameInteractionHelper.resolveAction(
                player.isSpectator(),
                !this.getItem().isEmpty(),
                CarpetIceAdditionSettings.invisibleItemFrames,
                CarpetIceAdditionSettings.fixedItemFrames,
                frame.isInvisible(),
                this.fixed,
                heldStack.is(Items.PHANTOM_MEMBRANE),
                heldStack.is(Items.GLASS_PANE),
                heldStack.is(ItemTags.AXES)
        );
        if (action == FrameCustomizationAction.NONE) {
            return;
        }

        if (frame.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        switch (action) {
            case CONSUME_INTERACTION -> cir.setReturnValue(InteractionResult.SUCCESS);
            case APPLY_INVISIBLE -> {
                boolean paidBefore = this.carpetIceAddition$invisibleFramePaid;
                frame.setInvisible(true);
                this.carpetIceAddition$invisibleFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logInvisibleApplied(frame.getUUID(), true, player.isCreative(), paidBefore, this.carpetIceAddition$invisibleFramePaid);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            case APPLY_FIXED -> {
                boolean paidBefore = this.carpetIceAddition$fixedFramePaid;
                this.fixed = true;
                this.carpetIceAddition$fixedFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logFixedApplied(frame.getUUID(), true, player.isCreative(), paidBefore, this.carpetIceAddition$fixedFramePaid);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            case CLEAR_FIXED -> {
                boolean paidBefore = this.carpetIceAddition$fixedFramePaid;
                ItemFrameInteractionHelper.logFixedClearAttempt(frame.getUUID(), this.fixed, paidBefore, paidBefore);
                if (paidBefore) {
                    carpetIceAddition$spawnRefundItem((ServerLevel) frame.level(), new ItemStack(Items.GLASS_PANE));
                    this.carpetIceAddition$fixedFramePaid = false;
                }
                this.fixed = false;
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            default -> {
            }
        }
    }

    @Inject(method = "dropItem", at = @At("HEAD"))
    private void carpetIceAddition$refundPaidInvisibleMaterial(ServerLevel level, Entity breaker, CallbackInfo ci) {
        ItemFrame frame = (ItemFrame) (Object) this;
        boolean paidBefore = this.carpetIceAddition$invisibleFramePaid;
        boolean refundTriggered = paidBefore;
        ItemFrameInteractionHelper.logInvisibleRefundAttempt(frame.getUUID(), frame.isInvisible(), paidBefore, refundTriggered);
        if (refundTriggered) {
            carpetIceAddition$spawnRefundItem(level, new ItemStack(Items.PHANTOM_MEMBRANE));
            this.carpetIceAddition$invisibleFramePaid = false;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$writePaidFlags(ValueOutput output, CallbackInfo ci) {
        output.putBoolean(ItemFrameInteractionHelper.INVISIBLE_FRAME_PAID_KEY, this.carpetIceAddition$invisibleFramePaid);
        output.putBoolean(ItemFrameInteractionHelper.FIXED_FRAME_PAID_KEY, this.carpetIceAddition$fixedFramePaid);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$readPaidFlags(ValueInput input, CallbackInfo ci) {
        this.carpetIceAddition$invisibleFramePaid = input.getBooleanOr(ItemFrameInteractionHelper.INVISIBLE_FRAME_PAID_KEY, false);
        this.carpetIceAddition$fixedFramePaid = input.getBooleanOr(ItemFrameInteractionHelper.FIXED_FRAME_PAID_KEY, false);
    }

    private static boolean carpetIceAddition$consumeSingleItem(ItemStack stack, Player player) {
        if (player.isCreative()) {
            return false;
        }
        stack.shrink(1);
        return true;
    }

    @Unique
    private void carpetIceAddition$spawnRefundItem(ServerLevel level, ItemStack stack) {
        ItemFrame frame = (ItemFrame) (Object) this;
        ItemEntity itemEntity = new ItemEntity(level, frame.getX(), frame.getY(), frame.getZ(), stack);
        level.addFreshEntity(itemEntity);
    }
}
