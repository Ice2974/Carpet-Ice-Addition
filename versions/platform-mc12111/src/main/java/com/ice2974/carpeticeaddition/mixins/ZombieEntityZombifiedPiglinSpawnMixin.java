package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionHighVersionSettings;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityZombifiedPiglinSpawnMixin {
    @Inject(
            method = "initialize(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/world/LocalDifficulty;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/entity/EntityData;)Lnet/minecraft/entity/EntityData;",
            at = @At("RETURN")
    )
    private void carpetIceAddition$removeNaturalZombifiedPiglinSpears(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            EntityData entityData,
            CallbackInfoReturnable<EntityData> cir
    ) {
        if (!CarpetIceAdditionHighVersionSettings.zombifiedPiglinsSpawnWithoutSpears
                || !carpetIceAddition$isNaturalSpawn(spawnReason)
                || !((Object) this instanceof ZombifiedPiglinEntity zombifiedPiglin)) {
            return;
        }

        ItemStack mainHandStack = zombifiedPiglin.getEquippedStack(EquipmentSlot.MAINHAND);
        if (mainHandStack.isOf(Items.GOLDEN_SPEAR)) {
            zombifiedPiglin.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }
    }

    @Unique
    private static boolean carpetIceAddition$isNaturalSpawn(SpawnReason spawnReason) {
        return spawnReason == SpawnReason.NATURAL
                || spawnReason == SpawnReason.CHUNK_GENERATION
                || spawnReason == SpawnReason.STRUCTURE;
    }
}
