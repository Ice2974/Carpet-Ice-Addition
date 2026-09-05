package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class VillagerEventsRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static volatile Session session;
    private VillagerEventsRuntime() { }
    public static void onServerLoaded(MinecraftServer server) { VillagerEventsCompatibility.beginServerSession(); replace(server); }
    public static void onServerClosed(MinecraftServer server) { Session current = session; if (current != null && current.server == server) { current.close(); session = null; } VillagerEventsCompatibility.endServerSession(); }
    private static synchronized void replace(MinecraftServer server) { if (session != null) session.close(); session = new Session(server); }
    private static Session current(MinecraftServer server) {
        Session value = session; String locale = VanillaLanguageService.normalizeLocale(CarpetSettings.language);
        if (value == null || value.server != server || !value.locale.equals(locale)) { replace(server); value = session; }
        return value;
    }
    public static VillagerEventSnapshot captureDeath(Villager villager, DamageSource source) { return VillagerEventsLogger.active() ? snapshot(villager, source.getLocalizedDeathMessage(villager)) : null; }
    public static VillagerEventSnapshot snapshot(Villager villager, Component death) {
        VillagerIdentity.Identity identity = VillagerIdentity.create(villager);
        return new VillagerEventSnapshot(SEQUENCE.incrementAndGet(), villager.level().dimension().identifier().toString(), villager.blockPosition().getX(), villager.blockPosition().getY(), villager.blockPosition().getZ(), identity.translated(), identity.fallback(), death);
    }
    public static void death(MinecraftServer server, VillagerEventSnapshot snapshot) {
        if (snapshot == null || !VillagerEventsLogger.active()) return;
        Session current = current(server);
        if (current.language.state() == VanillaLanguageService.State.LOADING) return;
        if (current.language.state() == VanillaLanguageService.State.FAILED) { current.warnOnce("death output paused because vanilla language loading failed"); return; }
        sendDeath(current, snapshot);
    }
    private static void sendDeath(Session session, VillagerEventSnapshot snapshot) {
        Component rendered = TextRenderer.renderDeath(snapshot.deathMessage(), snapshot.identity(), session.language.translations());
        if (rendered == null) rendered = TextRenderer.renderDeath(snapshot.deathMessage(), snapshot.fallbackIdentity(), session.language.translations());
        if (rendered == null) { session.warnOnce("suppressed death message with unresolved vanilla component"); return; }
        VillagerEventsLogger.send("death", message(rendered, snapshot));
    }
    public static void conversion(MinecraftServer server, String event, VillagerEventSnapshot snapshot) {
        if (snapshot == null || !VillagerEventsLogger.active()) return;
        Session current = current(server);
        Component identity = current.language.state() == VanillaLanguageService.State.READY ? TextRenderer.renderLiteralTree(snapshot.identity(), current.language.translations()) : null;
        if (identity == null) identity = TextRenderer.renderLiteralTree(snapshot.fallbackIdentity(), java.util.Map.of());
        if (identity == null) { current.warnOnce("suppressed conversion message with unresolved component"); return; }
        String template = "zombified".equals(event) ? "logger.carpet-ice-addition.villager_events.zombified" : "logger.carpet-ice-addition.villager_events.witch";
        String action = TranslationFormatUtil.translate(template);
        int marker = action.indexOf("%s");
        Component detail = marker < 0 ? Component.literal(action) : Component.literal(action.substring(0, marker)).append(identity).append(action.substring(marker + 2));
        VillagerEventsLogger.send(event, message(detail, snapshot));
    }
    private static Component message(Component detail, VillagerEventSnapshot snapshot) {
        String dimension = switch (snapshot.dimensionId()) { case "minecraft:overworld" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.overworld"); case "minecraft:the_nether" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.nether"); case "minecraft:the_end" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.end"); default -> snapshot.dimensionId(); };
        return Component.literal("[VillagerEvents] ").append(detail).append(" | ").append(dimension).append(" | ").append(snapshot.x() + ", " + snapshot.y() + ", " + snapshot.z());
    }
    private static final class Session implements AutoCloseable {
        final MinecraftServer server; final String locale; final VanillaLanguageService language; final AtomicBoolean warned = new AtomicBoolean(); volatile boolean closed;
        Session(MinecraftServer server) {
            this.server = server; this.locale = VanillaLanguageService.normalizeLocale(CarpetSettings.language);
            Path root = FabricLoader.getInstance().getConfigDir().resolve("carpet-ice-addition").resolve("vanilla-language");
            String version = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
            this.language = new VanillaLanguageService(version, locale, root, VillagerEventsRuntime.class.getClassLoader(), LOGGER);
            language.start(ignored -> server.execute(() -> { if (closed || session != this || language.state() != VanillaLanguageService.State.READY) return; }));
        }
        void warnOnce(String message) { if (warned.compareAndSet(false, true)) LOGGER.warn("[VillagerEvents] {}", message); }
        public void close() { closed = true; language.close(); }
    }
}
