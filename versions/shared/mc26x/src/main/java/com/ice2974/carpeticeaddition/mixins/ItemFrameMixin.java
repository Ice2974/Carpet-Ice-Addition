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
import net.minecraft.world.damagesource.DamageSource;
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
    @Unique private boolean carpetIceAddition$invisibleFrameRefunded;
    @Unique private boolean carpetIceAddition$fixedFrameRefunded;
    @Unique private DamageSource carpetIceAddition$currentDamageSource;

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
                CarpetIceAdditionSettings.itemFrameInvisible,
                CarpetIceAdditionSettings.itemFrameFixed,
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
                this.carpetIceAddition$invisibleFrameRefunded = false;
                this.carpetIceAddition$invisibleFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logInvisibleApplied(frame.getUUID(), true, player.isCreative(), paidBefore, this.carpetIceAddition$invisibleFramePaid);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            case APPLY_FIXED -> {
                boolean paidBefore = this.carpetIceAddition$fixedFramePaid;
                this.fixed = true;
                this.carpetIceAddition$fixedFrameRefunded = false;
                this.carpetIceAddition$fixedFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logFixedApplied(frame.getUUID(), true, player.isCreative(), paidBefore, this.carpetIceAddition$fixedFramePaid);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            case CLEAR_FIXED -> {
                boolean creativeMode = player.isCreative();
                boolean refundTriggered = CarpetIceAdditionSettings.itemFrameFixed && !creativeMode && !this.carpetIceAddition$fixedFrameRefunded;
                ItemFrameInteractionHelper.logFixedClearAttempt(frame.getUUID(), this.fixed, creativeMode, refundTriggered);
                if (refundTriggered) {
                    carpetIceAddition$spawnRefundItem((ServerLevel) frame.level(), new ItemStack(Items.GLASS_PANE));
                }
                this.carpetIceAddition$fixedFrameRefunded = true;
                this.carpetIceAddition$fixedFramePaid = false;
                this.fixed = false;
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            default -> {
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void carpetIceAddition$captureDamageSource(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$currentDamageSource = source;
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void carpetIceAddition$clearDamageSource(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$currentDamageSource = null;
    }

    @Inject(method = "dropItem", at = @At("HEAD"))
    private void carpetIceAddition$refundCustomizationMaterials(ServerLevel level, Entity breaker, CallbackInfo ci) {
        ItemFrame frame = (ItemFrame) (Object) this;
        boolean creativeDestroyer = carpetIceAddition$isCreativeDestroyer(breaker);

        if (CarpetIceAdditionSettings.itemFrameInvisible && frame.isInvisible() && !this.carpetIceAddition$invisibleFrameRefunded) {
            boolean refundTriggered = !creativeDestroyer;
            ItemFrameInteractionHelper.logInvisibleRefundAttempt(frame.getUUID(), true, creativeDestroyer, refundTriggered);
            if (refundTriggered) {
                carpetIceAddition$spawnRefundItem(level, new ItemStack(Items.PHANTOM_MEMBRANE));
            }
            this.carpetIceAddition$invisibleFrameRefunded = true;
            this.carpetIceAddition$invisibleFramePaid = false;
        }

        if (CarpetIceAdditionSettings.itemFrameFixed && this.fixed && !this.carpetIceAddition$fixedFrameRefunded) {
            boolean refundTriggered = !creativeDestroyer;
            ItemFrameInteractionHelper.logFixedRefundAttempt(frame.getUUID(), true, creativeDestroyer, refundTriggered);
            if (refundTriggered) {
                carpetIceAddition$spawnRefundItem(level, new ItemStack(Items.GLASS_PANE));
            }
            this.carpetIceAddition$fixedFrameRefunded = true;
            this.carpetIceAddition$fixedFramePaid = false;
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
    private boolean carpetIceAddition$isCreativeDestroyer(Entity breaker) {
        if (this.carpetIceAddition$currentDamageSource != null && this.carpetIceAddition$currentDamageSource.isCreativePlayer()) {
            return true;
        }
        return breaker instanceof Player player && player.isCreative();
    }

    @Unique
    private void carpetIceAddition$spawnRefundItem(ServerLevel level, ItemStack stack) {
        ItemFrame frame = (ItemFrame) (Object) this;
        ItemEntity itemEntity = new ItemEntity(level, frame.getX(), frame.getY(), frame.getZ(), stack);
        level.addFreshEntity(itemEntity);
    }
}
