package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionHighVersionSettings;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinEntity.class)
public abstract class PiglinEntityMobsSpawnWithoutSpearsMixin {
    @Inject(
            method = "initialize(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/world/LocalDifficulty;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/entity/EntityData;)Lnet/minecraft/entity/EntityData;",
            at = @At("RETURN")
    )
    private void carpetIceAddition$removeNaturalPiglinSpears(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            EntityData entityData,
            CallbackInfoReturnable<EntityData> cir
    ) {
        if (!CarpetIceAdditionHighVersionSettings.mobsSpawnWithoutSpears
                || !carpetIceAddition$isNaturalSpawn(spawnReason)) {
            return;
        }

        PiglinEntity piglin = (PiglinEntity) (Object) this;
        ItemStack mainHandStack = piglin.getEquippedStack(EquipmentSlot.MAINHAND);
        if (mainHandStack.isOf(Items.GOLDEN_SPEAR)) {
            piglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }
    }

    @Unique
    private static boolean carpetIceAddition$isNaturalSpawn(SpawnReason spawnReason) {
        return spawnReason == SpawnReason.NATURAL
                || spawnReason == SpawnReason.CHUNK_GENERATION
                || spawnReason == SpawnReason.STRUCTURE;
    }
}
