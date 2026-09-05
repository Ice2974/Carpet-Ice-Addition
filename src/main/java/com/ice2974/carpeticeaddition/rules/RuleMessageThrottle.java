package com.ice2974.carpeticeaddition.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class RuleMessageThrottle {
    private static final long MESSAGE_INTERVAL_TICKS = 20L;
    private static final Map<UUID, Long> LAST_MESSAGE_TICK = new HashMap<>();

    private RuleMessageThrottle() {
    }

    public static boolean shouldSendScaffoldingWarning(ServerPlayer player) {
        //#disable-remap player.level() 多版本双重解析，保持原文本由各版本 javac 解析（见 SafeScaffoldingBreakMixin 同注）
        long now = player.level().getGameTime();
        //#enable-remap
        Long last = LAST_MESSAGE_TICK.get(player.getUUID());
        if (last != null && now - last < MESSAGE_INTERVAL_TICKS) {
            return false;
        }
        LAST_MESSAGE_TICK.put(player.getUUID(), now);
        return true;
    }
}
