package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.network.chat.Component;

public record VillagerEventSnapshot26(long sequence, String dimensionId, int x, int y, int z, Component identity, Component deathMessage) {
    public VillagerEventSnapshot26 { identity = identity.copy(); deathMessage = deathMessage == null ? null : deathMessage.copy(); }
}
