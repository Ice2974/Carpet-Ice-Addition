package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionHighVersionSettings;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMobsSpawnWithoutSpearsMixin {
    @Inject(
            method = "finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;",
            at = @At("RETURN")
    )
    private void carpetIceAddition$removeNaturalMobSpears(
            ServerLevelAccessor world,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        if (!CarpetIceAdditionHighVersionSettings.mobsSpawnWithoutSpears
                || !carpetIceAddition$isNaturalSpawn(spawnReason)) {
            return;
        }

        Object self = this;
        if (self instanceof ZombifiedPiglin zombifiedPiglin) {
            ItemStack mainHandStack = zombifiedPiglin.getItemBySlot(EquipmentSlot.MAINHAND);
            if (mainHandStack.is(Items.GOLDEN_SPEAR)) {
                zombifiedPiglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            }
            return;
        }

        if (self.getClass() == Zombie.class
                || self instanceof ZombieVillager
                || self instanceof Husk) {
            Zombie zombie = (Zombie) self;
            ItemStack mainHandStack = zombie.getItemBySlot(EquipmentSlot.MAINHAND);
            if (mainHandStack.is(Items.IRON_SPEAR)) {
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
        }
    }

    @Unique
    private static boolean carpetIceAddition$isNaturalSpawn(EntitySpawnReason spawnReason) {
        return spawnReason == EntitySpawnReason.NATURAL
                || spawnReason == EntitySpawnReason.CHUNK_GENERATION
                || spawnReason == EntitySpawnReason.STRUCTURE;
    }
}
