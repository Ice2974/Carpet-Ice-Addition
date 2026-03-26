package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DelayedJukeboxStartEventManager {
    private static final Map<ServerWorld, PendingWorldEvents> PENDING_EVENTS = new HashMap<>();

    private DelayedJukeboxStartEventManager() {
    }

    public static void queueStart(ServerWorld world, BlockPos pos, int songRawId) {
        PendingWorldEvents state = PENDING_EVENTS.computeIfAbsent(world, ignored -> new PendingWorldEvents());
        BlockPos key = pos.toImmutable();
        if (state.stoppedThisTick.contains(key)) {
            return;
        }
        state.startEvents.put(key, songRawId);
    }

    public static void recordStop(ServerWorld world, BlockPos pos) {
        PendingWorldEvents state = PENDING_EVENTS.computeIfAbsent(world, ignored -> new PendingWorldEvents());
        BlockPos key = pos.toImmutable();
        state.stoppedThisTick.add(key);
        state.startEvents.remove(key);
    }

    public static void tick(ServerWorld world) {
        PendingWorldEvents state = PENDING_EVENTS.get(world);
        if (state == null) {
            return;
        }

        for (Map.Entry<BlockPos, Integer> entry : state.startEvents.entrySet()) {
            if (!state.stoppedThisTick.contains(entry.getKey())) {
                world.syncWorldEvent(null, WorldEvents.JUKEBOX_STARTS_PLAYING, entry.getKey(), entry.getValue());
            }
        }

        PENDING_EVENTS.remove(world);
    }

    private static final class PendingWorldEvents {
        private final Map<BlockPos, Integer> startEvents = new HashMap<>();
        private final Set<BlockPos> stoppedThisTick = new HashSet<>();
    }
}
