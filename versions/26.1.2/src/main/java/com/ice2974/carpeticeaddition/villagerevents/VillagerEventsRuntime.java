package com.ice2974.carpeticeaddition.villagerevents;

import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.concurrent.atomic.AtomicLong;

public final class VillagerEventsRuntime {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private VillagerEventsRuntime() { }
    public static void onServerLoaded(MinecraftServer server) { VillagerEventsCompatibility.beginServerSession(); }
    public static void onServerClosed(MinecraftServer server) { VillagerEventsCompatibility.endServerSession(); }
    public static VillagerEventSnapshot captureDeath(Villager villager, DamageSource source) { return VillagerEventsLogger.active() ? snapshot(villager, source.getLocalizedDeathMessage(villager)) : null; }
    public static VillagerEventSnapshot snapshot(Villager villager, Component death) {
        Component identity = VillagerIdentity.create(villager);
        return new VillagerEventSnapshot(SEQUENCE.incrementAndGet(), villager.level().dimension().identifier().toString(), villager.blockPosition().getX(), villager.blockPosition().getY(), villager.blockPosition().getZ(), identity, death);
    }
    public static void death(MinecraftServer server, VillagerEventSnapshot snapshot) {
        if (snapshot == null || !VillagerEventsLogger.active()) return;
        VillagerEventsLogger.send("death", message(VillagerDeathMessages.withIdentity(snapshot.deathMessage(), snapshot.identity()), snapshot));
    }
    public static void conversion(MinecraftServer server, String event, VillagerEventSnapshot snapshot) {
        if (snapshot == null || !VillagerEventsLogger.active()) return;
        String template = "zombified".equals(event) ? "logger.carpet-ice-addition.villager_events.zombified" : "logger.carpet-ice-addition.villager_events.witch";
        String action = TranslationFormatUtil.translate(template);
        int marker = action.indexOf("%s");
        Component detail = marker < 0 ? Component.literal(action) : Component.literal(action.substring(0, marker)).append(snapshot.identity()).append(action.substring(marker + 2));
        VillagerEventsLogger.send(event, message(detail, snapshot));
    }
    private static Component message(Component detail, VillagerEventSnapshot snapshot) {
        String dimension = switch (snapshot.dimensionId()) { case "minecraft:overworld" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.overworld"); case "minecraft:the_nether" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.nether"); case "minecraft:the_end" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.end"); default -> snapshot.dimensionId(); };
        return Component.literal("[VillagerEvents] ").append(detail).append(" | ").append(dimension).append(" | ").append(snapshot.x() + ", " + snapshot.y() + ", " + snapshot.z());
    }
}
