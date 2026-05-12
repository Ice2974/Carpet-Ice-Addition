package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Shadow
    private boolean fixed;

    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$itemFrameFeatures(Player player, InteractionHand hand, Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isSpectator() || this.getItem().isEmpty()) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        boolean phantomMembrane = CarpetIceAdditionSettings.invisibleItemFrames && stack.is(Items.PHANTOM_MEMBRANE);
        boolean glassPane = CarpetIceAdditionSettings.fixedItemFrames && stack.is(Items.GLASS_PANE);
        boolean axe = CarpetIceAdditionSettings.fixedItemFrames && stack.is(ItemTags.AXES);
        if (!phantomMembrane && !glassPane && !axe) {
            return;
        }

        ItemFrame frame = (ItemFrame) (Object) this;
        if (frame.level().isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (phantomMembrane) {
            if (!frame.isInvisible()) {
                frame.setInvisible(true);
                shrinkUnlessCreative(stack, player);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else if (glassPane) {
            if (!this.fixed) {
                this.fixed = true;
                shrinkUnlessCreative(stack, player);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else if (this.fixed) {
            this.fixed = false;
            ((Entity) (Object) this).spawnAtLocation((ServerLevel) frame.level(), new ItemStack(Items.GLASS_PANE), 0.0F);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "dropItem", at = @At("HEAD"))
    private void carpetIceAddition$dropPhantomMembrane(ServerLevel level, Entity breaker, CallbackInfo ci) {
        ItemFrame frame = (ItemFrame) (Object) this;
        if (CarpetIceAdditionSettings.invisibleItemFrames && frame.isInvisible()) {
            ((Entity) (Object) this).spawnAtLocation(level, new ItemStack(Items.PHANTOM_MEMBRANE), 0.0F);
        }
    }

    private static void shrinkUnlessCreative(ItemStack stack, Player player) {
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }
}
