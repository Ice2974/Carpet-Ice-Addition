package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.network.chat.Component;

public record VillagerEventSnapshot(long sequence, String dimensionId, int x, int y, int z, Component identity, Component deathMessage) {
    public VillagerEventSnapshot { identity = identity.copy(); deathMessage = deathMessage == null ? null : deathMessage.copy(); }
}
