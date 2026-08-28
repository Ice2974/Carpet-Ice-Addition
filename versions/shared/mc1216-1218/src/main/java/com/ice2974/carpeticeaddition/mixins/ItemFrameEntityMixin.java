package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper;
import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper.FrameCustomizationAction;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
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
    @Unique private boolean carpetIceAddition$invisibleFrameRefunded;
    @Unique private boolean carpetIceAddition$fixedFrameRefunded;
    @Unique private DamageSource carpetIceAddition$currentDamageSource;

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
                CarpetIceAdditionSettings.itemFrameInvisible,
                CarpetIceAdditionSettings.itemFrameFixed,
                frame.isInvisible(),
                this.fixed,
                heldStack.isOf(Items.PHANTOM_MEMBRANE),
                heldStack.isOf(Items.GLASS_PANE),
                heldStack.isIn(ItemTags.AXES)
        );
        if (action == FrameCustomizationAction.NONE) {
            return;
        }

        if (frame.getWorld().isClient()) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        switch (action) {
            case CONSUME_INTERACTION -> cir.setReturnValue(ActionResult.SUCCESS);
            case APPLY_INVISIBLE -> {
                boolean paidBefore = this.carpetIceAddition$invisibleFramePaid;
                frame.setInvisible(true);
                this.carpetIceAddition$invisibleFrameRefunded = false;
                this.carpetIceAddition$invisibleFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logInvisibleApplied(frame.getUuid(), true, player.isInCreativeMode(), paidBefore, this.carpetIceAddition$invisibleFramePaid);
                cir.setReturnValue(ActionResult.SUCCESS);
            }
            case APPLY_FIXED -> {
                boolean paidBefore = this.carpetIceAddition$fixedFramePaid;
                this.fixed = true;
                this.carpetIceAddition$fixedFrameRefunded = false;
                this.carpetIceAddition$fixedFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logFixedApplied(frame.getUuid(), true, player.isInCreativeMode(), paidBefore, this.carpetIceAddition$fixedFramePaid);
                cir.setReturnValue(ActionResult.SUCCESS);
            }
            case CLEAR_FIXED -> {
                boolean creativeMode = player.isInCreativeMode();
                boolean refundTriggered = CarpetIceAdditionSettings.itemFrameFixed && !creativeMode && !this.carpetIceAddition$fixedFrameRefunded;
                ItemFrameInteractionHelper.logFixedClearAttempt(frame.getUuid(), this.fixed, creativeMode, refundTriggered);
                if (refundTriggered) {
                    carpetIceAddition$spawnRefundItem((ServerWorld) frame.getWorld(), new ItemStack(Items.GLASS_PANE));
                }
                this.carpetIceAddition$fixedFrameRefunded = true;
                this.carpetIceAddition$fixedFramePaid = false;
                this.fixed = false;
                cir.setReturnValue(ActionResult.SUCCESS);
            }
            default -> {
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void carpetIceAddition$captureDamageSource(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$currentDamageSource = source;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void carpetIceAddition$clearDamageSource(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$currentDamageSource = null;
    }

    @Inject(method = "onBreak", at = @At("HEAD"))
    private void carpetIceAddition$refundCustomizationMaterials(ServerWorld world, Entity breaker, CallbackInfo ci) {
        ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
        boolean creativeDestroyer = carpetIceAddition$isCreativeDestroyer(breaker);

        if (CarpetIceAdditionSettings.itemFrameInvisible && frame.isInvisible() && !this.carpetIceAddition$invisibleFrameRefunded) {
            boolean refundTriggered = !creativeDestroyer;
            ItemFrameInteractionHelper.logInvisibleRefundAttempt(frame.getUuid(), true, creativeDestroyer, refundTriggered);
            if (refundTriggered) {
                carpetIceAddition$spawnRefundItem(world, new ItemStack(Items.PHANTOM_MEMBRANE));
            }
            this.carpetIceAddition$invisibleFrameRefunded = true;
            this.carpetIceAddition$invisibleFramePaid = false;
        }

        if (CarpetIceAdditionSettings.itemFrameFixed && this.fixed && !this.carpetIceAddition$fixedFrameRefunded) {
            boolean refundTriggered = !creativeDestroyer;
            ItemFrameInteractionHelper.logFixedRefundAttempt(frame.getUuid(), true, creativeDestroyer, refundTriggered);
            if (refundTriggered) {
                carpetIceAddition$spawnRefundItem(world, new ItemStack(Items.GLASS_PANE));
            }
            this.carpetIceAddition$fixedFrameRefunded = true;
            this.carpetIceAddition$fixedFramePaid = false;
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
    private boolean carpetIceAddition$isCreativeDestroyer(Entity breaker) {
        if (this.carpetIceAddition$currentDamageSource != null && this.carpetIceAddition$currentDamageSource.isSourceCreativePlayer()) {
            return true;
        }
        return breaker instanceof PlayerEntity player && player.isInCreativeMode();
    }

    @Unique
    private void carpetIceAddition$spawnRefundItem(ServerWorld world, ItemStack stack) {
        ItemFrameEntity frame = (ItemFrameEntity) (Object) this;
        ItemEntity itemEntity = new ItemEntity(world, frame.getX(), frame.getY(), frame.getZ(), stack);
        world.spawnEntity(itemEntity);
    }
}
