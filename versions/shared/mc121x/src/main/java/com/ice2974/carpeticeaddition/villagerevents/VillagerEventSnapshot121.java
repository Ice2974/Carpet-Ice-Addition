package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.text.Text;

/** Immutable-at-capture event data. It intentionally has no entity, world, or DamageSource reference. */
public record VillagerEventSnapshot121(long sequence, String dimensionId, int x, int y, int z, Text identity, Text fallbackIdentity, Text deathMessage) {
    public VillagerEventSnapshot121 {
        identity = identity.copy();
        fallbackIdentity = fallbackIdentity.copy();
        deathMessage = deathMessage == null ? null : deathMessage.copy();
    }
}
