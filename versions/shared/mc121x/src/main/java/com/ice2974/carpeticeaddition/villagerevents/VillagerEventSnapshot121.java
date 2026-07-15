package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.text.Text;

/** Immutable-at-capture event data. It intentionally has no entity, world, or DamageSource reference. */
public record VillagerEventSnapshot121(long sequence, String dimensionId, int x, int y, int z, Text identity, Text deathMessage) {
    public VillagerEventSnapshot121 {
        identity = identity.copy();
        deathMessage = deathMessage == null ? null : deathMessage.copy();
    }
}
