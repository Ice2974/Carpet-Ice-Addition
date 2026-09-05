package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper;
import com.ice2974.carpeticeaddition.rules.ItemFrameInteractionHelper.FrameCustomizationAction;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameEntityMixin {
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
    private void carpetIceAddition$handleCustomizationTool(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
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

        if (frame.getCommandSenderWorld().isClientSide()) {
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
                ItemFrameInteractionHelper.logInvisibleApplied(frame.getUUID(), true, player.hasInfiniteMaterials(), paidBefore, this.carpetIceAddition$invisibleFramePaid);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            case APPLY_FIXED -> {
                boolean paidBefore = this.carpetIceAddition$fixedFramePaid;
                this.fixed = true;
                this.carpetIceAddition$fixedFrameRefunded = false;
                this.carpetIceAddition$fixedFramePaid = carpetIceAddition$consumeSingleItem(heldStack, player);
                ItemFrameInteractionHelper.logFixedApplied(frame.getUUID(), true, player.hasInfiniteMaterials(), paidBefore, this.carpetIceAddition$fixedFramePaid);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            case CLEAR_FIXED -> {
                boolean creativeMode = player.hasInfiniteMaterials();
                boolean refundTriggered = CarpetIceAdditionSettings.itemFrameFixed && !creativeMode && !this.carpetIceAddition$fixedFrameRefunded;
                ItemFrameInteractionHelper.logFixedClearAttempt(frame.getUUID(), this.fixed, creativeMode, refundTriggered);
                if (refundTriggered) {
                    carpetIceAddition$spawnRefundItem((ServerLevel) frame.getCommandSenderWorld(), new ItemStack(Items.GLASS_PANE));
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

    @Inject(method = "hurt", at = @At("HEAD"))
    private void carpetIceAddition$captureDamageSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$currentDamageSource = source;
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void carpetIceAddition$clearDamageSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.carpetIceAddition$currentDamageSource = null;
    }

    @Inject(method = "dropItem(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void carpetIceAddition$refundCustomizationMaterials(Entity breaker, CallbackInfo ci) {
        ItemFrame frame = (ItemFrame) (Object) this;
        if (frame.getCommandSenderWorld().isClientSide()) {
            return;
        }

        boolean creativeDestroyer = carpetIceAddition$isCreativeDestroyer(breaker);
        ServerLevel world = (ServerLevel) frame.getCommandSenderWorld();

        if (CarpetIceAdditionSettings.itemFrameInvisible && frame.isInvisible() && !this.carpetIceAddition$invisibleFrameRefunded) {
            boolean refundTriggered = !creativeDestroyer;
            ItemFrameInteractionHelper.logInvisibleRefundAttempt(frame.getUUID(), true, creativeDestroyer, refundTriggered);
            if (refundTriggered) {
                carpetIceAddition$spawnRefundItem(world, new ItemStack(Items.PHANTOM_MEMBRANE));
            }
            this.carpetIceAddition$invisibleFrameRefunded = true;
            this.carpetIceAddition$invisibleFramePaid = false;
        }

        if (CarpetIceAdditionSettings.itemFrameFixed && this.fixed && !this.carpetIceAddition$fixedFrameRefunded) {
            boolean refundTriggered = !creativeDestroyer;
            ItemFrameInteractionHelper.logFixedRefundAttempt(frame.getUUID(), true, creativeDestroyer, refundTriggered);
            if (refundTriggered) {
                carpetIceAddition$spawnRefundItem(world, new ItemStack(Items.GLASS_PANE));
            }
            this.carpetIceAddition$fixedFrameRefunded = true;
            this.carpetIceAddition$fixedFramePaid = false;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$writePaidFlags(CompoundTag nbt, CallbackInfo ci) {
        nbt.putBoolean(ItemFrameInteractionHelper.INVISIBLE_FRAME_PAID_KEY, this.carpetIceAddition$invisibleFramePaid);
        nbt.putBoolean(ItemFrameInteractionHelper.FIXED_FRAME_PAID_KEY, this.carpetIceAddition$fixedFramePaid);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$readPaidFlags(CompoundTag nbt, CallbackInfo ci) {
        this.carpetIceAddition$invisibleFramePaid = nbt.getBoolean(ItemFrameInteractionHelper.INVISIBLE_FRAME_PAID_KEY);
        this.carpetIceAddition$fixedFramePaid = nbt.getBoolean(ItemFrameInteractionHelper.FIXED_FRAME_PAID_KEY);
    }

    private static boolean carpetIceAddition$consumeSingleItem(ItemStack stack, Player player) {
        if (player.hasInfiniteMaterials()) {
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
        return breaker instanceof Player player && player.hasInfiniteMaterials();
    }

    @Unique
    private void carpetIceAddition$spawnRefundItem(ServerLevel world, ItemStack stack) {
        ItemFrame frame = (ItemFrame) (Object) this;
        ItemEntity itemEntity = new ItemEntity(world, frame.getX(), frame.getY(), frame.getZ(), stack);
        world.addFreshEntity(itemEntity);
    }
}
