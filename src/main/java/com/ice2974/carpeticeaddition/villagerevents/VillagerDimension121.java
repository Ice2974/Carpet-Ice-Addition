//#if MC<260000
package com.ice2974.carpeticeaddition.villagerevents;
import net.minecraft.world.entity.npc.villager.Villager;
final class VillagerDimension121 {
    private VillagerDimension121() { }
    static String id(Villager villager) { return villager.level().dimension().identifier().toString(); }
}
//#endif
