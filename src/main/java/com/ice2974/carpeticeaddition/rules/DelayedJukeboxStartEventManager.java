package com.ice2974.carpeticeaddition.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LevelEvent;

public final class DelayedJukeboxStartEventManager {
    private static final Map<ServerLevel, PendingWorldEvents> PENDING_EVENTS = new HashMap<>();

    private DelayedJukeboxStartEventManager() {
    }

    public static void queueStart(ServerLevel world, BlockPos pos, int songRawId) {
        PendingWorldEvents state = PENDING_EVENTS.computeIfAbsent(world, ignored -> new PendingWorldEvents());
        BlockPos key = pos.immutable();
        if (state.stoppedThisTick.contains(key)) {
            return;
        }
        state.startEvents.put(key, songRawId);
    }

    public static void recordStop(ServerLevel world, BlockPos pos) {
        PendingWorldEvents state = PENDING_EVENTS.computeIfAbsent(world, ignored -> new PendingWorldEvents());
        BlockPos key = pos.immutable();
        state.stoppedThisTick.add(key);
        state.startEvents.remove(key);
    }

    public static void tick(ServerLevel world) {
        PendingWorldEvents state = PENDING_EVENTS.get(world);
        if (state == null) {
            return;
        }

        for (Map.Entry<BlockPos, Integer> entry : state.startEvents.entrySet()) {
            if (!state.stoppedThisTick.contains(entry.getKey())) {
                world.levelEvent(null, LevelEvent.SOUND_PLAY_JUKEBOX_SONG, entry.getKey(), entry.getValue());
            }
        }

        PENDING_EVENTS.remove(world);
    }

    private static final class PendingWorldEvents {
        private final Map<BlockPos, Integer> startEvents = new HashMap<>();
        private final Set<BlockPos> stoppedThisTick = new HashSet<>();
    }
}
