package com.ice2974.carpeticeaddition.villagerevents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Logger observation must never be able to abort vanilla entity processing. */
public final class VillagerEventsCompatibility {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private VillagerEventsCompatibility() { }
    public static void beginServerSession() { REPORTED.clear(); }
    public static void endServerSession() { REPORTED.clear(); }
    public static void report(String category, Throwable error) {
        if (REPORTED.add(category)) LOGGER.warn("[VillagerEvents] Compatibility observation failed: {}", category, error);
    }
    public static void report(Throwable error) { report("observation", error); }
}
