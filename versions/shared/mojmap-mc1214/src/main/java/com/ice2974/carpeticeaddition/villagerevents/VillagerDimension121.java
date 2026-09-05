package com.ice2974.carpeticeaddition.villagerevents;
import net.minecraft.world.entity.npc.Villager;
final class VillagerDimension121 {
    private VillagerDimension121() { }
    static String id(Villager villager) { return villager.level().dimension().location().toString(); }
}
