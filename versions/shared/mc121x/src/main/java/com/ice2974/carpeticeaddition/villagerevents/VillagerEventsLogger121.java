package com.ice2974.carpeticeaddition.villagerevents;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import net.minecraft.text.Text;

public final class VillagerEventsLogger121 {
    public static boolean __villagerEvents;
    private static final String[] OPTIONS = {"all", "death", "zombified", "witch"};
    private static Logger logger;

    private VillagerEventsLogger121() { }

    public static void register() {
        if (logger != null) return;
        try {
            logger = new Logger(VillagerEventsLogger121.class.getField("__villagerEvents"), "villagerEvents", "all", OPTIONS, true);
            LoggerRegistry.registerLogger("villagerEvents", logger);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register villagerEvents logger", exception);
        }
    }

    public static boolean active() { return __villagerEvents && logger != null; }

    public static void send(String event, Text message) {
        if (!active()) return;
        logger.log(option -> ("all".equals(option) || event.equals(option)) ? new Text[]{message} : null);
    }
}
