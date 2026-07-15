package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

public final class VillagerDeathSide121 {
    private VillagerDeathSide121() {
    }

    public static ServerWorld serverWorld(VillagerEntity villager) {
        return villager.getEntityWorld() instanceof ServerWorld world ? world : null;
    }
}
