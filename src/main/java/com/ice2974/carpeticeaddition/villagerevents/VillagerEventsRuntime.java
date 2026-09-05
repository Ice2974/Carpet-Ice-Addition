//#if MC<260000
package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.ice2974.carpeticeaddition.villagerevents.VanillaLanguageService.State;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Server-session holder. It is replaced and closed when a server closes. */
public final class VillagerEventsRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static volatile Session session;

    private VillagerEventsRuntime() { }

    public static void onServerLoaded(MinecraftServer server) { VillagerEventsCompatibility.beginServerSession(); replace(server); }
    public static void onServerClosed(MinecraftServer server) {
        Session current = session;
        if (current != null && current.server == server) { current.close(); session = null; } VillagerEventsCompatibility.endServerSession();
    }

    private static Session current(MinecraftServer server) {
        Session value = session;
        if (value == null || value.server != server || !value.locale.equals(VanillaLanguageService.normalizeLocale(CarpetSettings.language))) {
            replace(server);
            value = session;
        }
        return value;
    }

    private static synchronized void replace(MinecraftServer server) {
        Session old = session;
        if (old != null) old.close();
        session = new Session(server);
    }

    public static VillagerEventSnapshot captureDeath(Villager villager, DamageSource source) {
        if (!VillagerEventsLogger.active()) return null;
        return snapshot(villager, source.getLocalizedDeathMessage(villager));
    }

    public static VillagerEventSnapshot snapshot(Villager villager, Component death) {
        BlockPos pos = villager.blockPosition();
        VillagerIdentity.Identity identity = VillagerIdentity.create(villager);
        return new VillagerEventSnapshot(SEQUENCE.incrementAndGet(), VillagerDimension121.id(villager), pos.getX(), pos.getY(), pos.getZ(), identity.translated(), identity.fallback(), death);
    }

    public static void death(MinecraftServer server, VillagerEventSnapshot snapshot) {
        if (snapshot == null || !VillagerEventsLogger.active()) return;
        Session current = current(server);
        if (current.language.state() == State.LOADING) return;
        if (current.language.state() == State.FAILED) return;
        sendDeath(current, snapshot);
    }

    public static void conversion(MinecraftServer server, String event, VillagerEventSnapshot snapshot) {
        if (snapshot == null || !VillagerEventsLogger.active()) return;
        Session current = current(server);
        Component identity = current.language.state() == State.READY ? TextRenderer.renderLiteralTree(snapshot.identity(), current.language.translations()) : null;
        if (identity == null) identity = TextRenderer.renderLiteralTree(snapshot.fallbackIdentity(), java.util.Map.of());
        if (identity == null) { current.warnOnce("suppressed conversion message with unresolved component"); return; }
        String template = "zombified".equals(event) ? "logger.carpet-ice-addition.villager_events.zombified" : "logger.carpet-ice-addition.villager_events.witch";
        String action = TranslationFormatUtil.translate(template);
        int marker = action.indexOf("%s");
        Component detail = marker < 0 ? Component.literal(action) : Component.literal(action.substring(0, marker)).append(identity).append(action.substring(marker + 2));
        VillagerEventsLogger.send(event, message(detail, snapshot));
    }

    private static void sendDeath(Session current, VillagerEventSnapshot snapshot) {
        Component rendered = TextRenderer.renderDeath(snapshot.deathMessage(), snapshot.identity(), current.language.translations());
        if (rendered == null) rendered = TextRenderer.renderDeath(snapshot.deathMessage(), snapshot.fallbackIdentity(), current.language.translations());
        if (rendered == null) { current.warnOnce("suppressed death message with unresolved vanilla component"); return; }
        VillagerEventsLogger.send("death", message(rendered, snapshot));
    }

    private static Component message(Component detail, VillagerEventSnapshot snapshot) {
        String dimension = switch (snapshot.dimensionId()) {
            case "minecraft:overworld" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.overworld");
            case "minecraft:the_nether" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.nether");
            case "minecraft:the_end" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.end");
            default -> snapshot.dimensionId();
        };
        return Component.literal("[VillagerEvents] ").append(detail).append(" | ").append(dimension).append(" | ")
                .append(snapshot.x() + ", " + snapshot.y() + ", " + snapshot.z());
    }

    private static final class Session implements AutoCloseable {
        private final MinecraftServer server;
        private final String locale;
        private final VanillaLanguageService language;
        private final AtomicBoolean warned = new AtomicBoolean();
        private final AtomicBoolean languageFailureWarned = new AtomicBoolean();
        private volatile boolean closed;

        private Session(MinecraftServer server) {
            this.server = server;
            this.locale = VanillaLanguageService.normalizeLocale(CarpetSettings.language);
            Path root = FabricLoader.getInstance().getConfigDir().resolve("carpet-ice-addition").resolve("vanilla-language");
            String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
                    .orElseThrow(() -> new IllegalStateException("Minecraft metadata unavailable"))
                    .getMetadata().getVersion().getFriendlyString();
            this.language = new VanillaLanguageService(minecraftVersion, locale, root, VillagerEventsRuntime.class.getClassLoader(), LOGGER);
            this.language.start(ignored -> server.execute(() -> {
                if (closed || session != this) return;
                if (language.state() == State.FAILED) warnLanguageFailureOnce();
            }));
        }

        private void warnOnce(String message) { if (warned.compareAndSet(false, true)) LOGGER.warn("[VillagerEvents] {}", message); }
        private void warnLanguageFailureOnce() {
            if (languageFailureWarned.compareAndSet(false, true)) {
                LOGGER.warn("[VillagerEvents] Death output is unavailable for this server session because the vanilla {} language resource failed to load. Restore network access or provide a valid cache, then restart the server.", locale);
            }
        }
        @Override public void close() { closed = true; language.close(); }
    }
}
//#endif
