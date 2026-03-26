package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RuleMessageThrottle {
    private static final long MESSAGE_INTERVAL_TICKS = 20L;
    private static final Map<UUID, Long> LAST_MESSAGE_TICK = new HashMap<>();

    private RuleMessageThrottle() {
    }

    public static boolean shouldSendScaffoldingWarning(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        Long last = LAST_MESSAGE_TICK.get(player.getUuid());
        if (last != null && now - last < MESSAGE_INTERVAL_TICKS) {
            return false;
        }
        LAST_MESSAGE_TICK.put(player.getUuid(), now);
        return true;
    }
}
