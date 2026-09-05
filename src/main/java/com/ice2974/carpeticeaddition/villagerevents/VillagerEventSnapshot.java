//#if MC<260000
package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.network.chat.Component;

/** Immutable-at-capture event data. It intentionally has no entity, world, or DamageSource reference. */
public record VillagerEventSnapshot(long sequence, String dimensionId, int x, int y, int z, Component identity, Component fallbackIdentity, Component deathMessage) {
    public VillagerEventSnapshot {
        identity = identity.copy();
        fallbackIdentity = fallbackIdentity.copy();
        deathMessage = deathMessage == null ? null : deathMessage.copy();
    }
}
//#endif
