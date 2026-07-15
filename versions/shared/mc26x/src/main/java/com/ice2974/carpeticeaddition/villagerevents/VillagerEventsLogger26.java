package com.ice2974.carpeticeaddition.villagerevents;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import net.minecraft.network.chat.Component;

public final class VillagerEventsLogger26 {
    public static boolean __villagerEvents;
    private static Logger logger;
    private VillagerEventsLogger26() { }
    public static void register() {
        try {
            logger = new Logger(VillagerEventsLogger26.class.getField("__villagerEvents"), "villagerEvents", "all", new String[]{"all", "death", "zombified", "witch"}, true);
            LoggerRegistry.registerLogger("villagerEvents", logger);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }
    public static boolean active() { return __villagerEvents && logger != null; }
    public static void send(String event, Component message) {
        if (active()) logger.log(option -> ("all".equals(option) || event.equals(option)) ? new Component[]{message} : null);
    }
}
