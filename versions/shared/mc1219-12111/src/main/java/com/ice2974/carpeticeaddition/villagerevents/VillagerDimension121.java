package com.ice2974.carpeticeaddition.villagerevents;
import net.minecraft.entity.passive.VillagerEntity;
final class VillagerDimension121 {
    private VillagerDimension121() { }
    static String id(VillagerEntity villager) { return villager.getEntityWorld().getRegistryKey().getValue().toString(); }
}
