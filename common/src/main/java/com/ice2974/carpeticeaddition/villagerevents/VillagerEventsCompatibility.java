package com.ice2974.carpeticeaddition.villagerevents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/** Logger observation must never be able to abort vanilla entity processing. */
public final class VillagerEventsCompatibility {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final AtomicBoolean REPORTED = new AtomicBoolean();
    private VillagerEventsCompatibility() { }
    public static void report(Throwable error) {
        if (REPORTED.compareAndSet(false, true)) LOGGER.warn("[VillagerEvents] Observation was disabled for one event after a compatibility error", error);
    }
}
