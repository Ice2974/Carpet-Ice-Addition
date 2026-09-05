//#if MC<260000
package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

public final class VillagerDeathSide121 {
    private VillagerDeathSide121() {
    }

    public static ServerLevel serverWorld(Villager villager) {
        return villager.level() instanceof ServerLevel world ? world : null;
    }
}
//#endif
