package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class VillagerEventsRuntime26 {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static volatile Session session;
    private VillagerEventsRuntime26() { }
    public static void onServerLoaded(MinecraftServer server) { replace(server); }
    public static void onServerClosed(MinecraftServer server) { Session current = session; if (current != null && current.server == server) { current.close(); session = null; } }
    private static synchronized void replace(MinecraftServer server) { if (session != null) session.close(); session = new Session(server); }
    private static Session current(MinecraftServer server) {
        Session value = session; String locale = VanillaLanguageService.normalizeLocale(CarpetSettings.language);
        if (value == null || value.server != server || !value.locale.equals(locale)) { replace(server); value = session; }
        return value;
    }
    public static VillagerEventSnapshot26 captureDeath(Villager villager, DamageSource source) { return VillagerEventsLogger26.active() ? snapshot(villager, source.getLocalizedDeathMessage(villager)) : null; }
    public static VillagerEventSnapshot26 snapshot(Villager villager, Component death) {
        Component identity = villager.hasCustomName() ? Component.literal("“").append(villager.getCustomName().copy()).append("”（村民）") : Component.literal("村民");
        return new VillagerEventSnapshot26(SEQUENCE.incrementAndGet(), villager.level().dimension().identifier().getPath(), villager.blockPosition().getX(), villager.blockPosition().getY(), villager.blockPosition().getZ(), identity, death);
    }
    public static void death(MinecraftServer server, VillagerEventSnapshot26 snapshot) {
        if (snapshot == null || !VillagerEventsLogger26.active()) return;
        Session current = current(server);
        if (current.language.state() == VanillaLanguageService.State.LOADING) { current.enqueue(snapshot); return; }
        if (current.language.state() == VanillaLanguageService.State.FAILED) { current.warnOnce("death output paused because vanilla language loading failed"); return; }
        sendDeath(current, snapshot);
    }
    private static void sendDeath(Session session, VillagerEventSnapshot26 snapshot) {
        Component rendered = TextRenderer26.renderDeath(snapshot.deathMessage(), snapshot.identity(), session.language.translations());
        if (rendered == null) { session.warnOnce("suppressed death message with unresolved vanilla component"); return; }
        VillagerEventsLogger26.send("death", message(rendered, snapshot));
    }
    public static void conversion(MinecraftServer server, String event, VillagerEventSnapshot26 snapshot) {
        if (snapshot == null || !VillagerEventsLogger26.active()) return;
        String action = "zombified".equals(event) ? TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.zombified", snapshot.identity().getString()) : TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.witch", snapshot.identity().getString());
        VillagerEventsLogger26.send(event, message(Component.literal(action), snapshot));
    }
    private static Component message(Component detail, VillagerEventSnapshot26 snapshot) {
        String dimension = switch (snapshot.dimensionId()) { case "overworld" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.overworld"); case "the_nether" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.nether"); case "the_end" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.end"); default -> snapshot.dimensionId().replace('_', ' '); };
        return Component.literal("[VillagerEvents] ").append(detail).append(" | ").append(dimension).append(" | ").append(snapshot.x() + ", " + snapshot.y() + ", " + snapshot.z());
    }
    private static final class Session implements AutoCloseable {
        final MinecraftServer server; final String locale; final VanillaLanguageService language; final ArrayDeque<VillagerEventSnapshot26> pending = new ArrayDeque<>(); final AtomicBoolean warned = new AtomicBoolean(); volatile boolean closed;
        Session(MinecraftServer server) {
            this.server = server; this.locale = VanillaLanguageService.normalizeLocale(CarpetSettings.language);
            Path root = FabricLoader.getInstance().getConfigDir().resolve("carpet-ice-addition").resolve("vanilla-language");
            String version = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
            this.language = new VanillaLanguageService(version, locale, root, VillagerEventsRuntime26.class.getClassLoader(), LOGGER);
            language.start(ignored -> server.execute(() -> { if (closed || session != this || language.state() != VanillaLanguageService.State.READY) return; while (!pending.isEmpty()) sendDeath(this, pending.removeFirst()); }));
        }
        void enqueue(VillagerEventSnapshot26 event) { if (pending.size() >= 128) { warnOnce("death queue is full; newest death message was skipped"); return; } pending.addLast(event); }
        void warnOnce(String message) { if (warned.compareAndSet(false, true)) LOGGER.warn("[VillagerEvents] {}", message); }
        public void close() { closed = true; pending.clear(); language.close(); }
    }
}
