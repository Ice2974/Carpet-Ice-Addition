//#if MC>=12111 && MC<260000

package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionHighVersionSettings;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Piglin.class)
public abstract class PiglinEntityMobsSpawnWithoutSpearsMixin {
    @Inject(
            method = "finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;",
            at = @At("RETURN")
    )
    private void carpetIceAddition$removeNaturalPiglinSpears(
            ServerLevelAccessor world,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData entityData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!CarpetIceAdditionHighVersionSettings.mobsSpawnWithoutSpears
                || !carpetIceAddition$isNaturalSpawn(spawnReason)) {
            return;
        }

        Piglin piglin = (Piglin) (Object) this;
        ItemStack mainHandStack = piglin.getItemBySlot(EquipmentSlot.MAINHAND);
        if (mainHandStack.is(Items.GOLDEN_SPEAR)) {
            piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }
    }

    @Unique
    private static boolean carpetIceAddition$isNaturalSpawn(EntitySpawnReason spawnReason) {
        return spawnReason == EntitySpawnReason.NATURAL
                || spawnReason == EntitySpawnReason.CHUNK_GENERATION
                || spawnReason == EntitySpawnReason.STRUCTURE;
    }
}

//#endif