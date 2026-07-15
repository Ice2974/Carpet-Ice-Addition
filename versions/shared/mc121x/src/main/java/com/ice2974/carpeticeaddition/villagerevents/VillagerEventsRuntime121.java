package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.ice2974.carpeticeaddition.villagerevents.VanillaLanguageService.State;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Server-session holder. It is replaced and closed when a server closes. */
public final class VillagerEventsRuntime121 {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static volatile Session session;

    private VillagerEventsRuntime121() { }

    public static void onServerLoaded(MinecraftServer server) { replace(server); }
    public static void onServerClosed(MinecraftServer server) {
        Session current = session;
        if (current != null && current.server == server) { current.close(); session = null; }
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

    public static VillagerEventSnapshot121 captureDeath(VillagerEntity villager, DamageSource source) {
        if (!VillagerEventsLogger121.active()) return null;
        return snapshot(villager, source.getDeathMessage(villager));
    }

    public static VillagerEventSnapshot121 snapshot(VillagerEntity villager, Text death) {
        BlockPos pos = villager.getBlockPos();
        Text identity = villager.hasCustomName() ? Text.literal("“").append(villager.getCustomName().copy()).append("”（村民）") : Text.literal("村民");
        return new VillagerEventSnapshot121(SEQUENCE.incrementAndGet(), "unknown", pos.getX(), pos.getY(), pos.getZ(), identity, death);
    }

    public static void death(MinecraftServer server, VillagerEventSnapshot121 snapshot) {
        if (snapshot == null || !VillagerEventsLogger121.active()) return;
        Session current = current(server);
        if (current.language.state() == State.LOADING) { current.enqueue(snapshot); return; }
        if (current.language.state() == State.FAILED) { current.warnOnce("death output paused because vanilla language loading failed"); return; }
        sendDeath(current, snapshot);
    }

    public static void conversion(MinecraftServer server, String event, VillagerEventSnapshot121 snapshot) {
        if (snapshot == null || !VillagerEventsLogger121.active()) return;
        String action = "zombified".equals(event)
                ? TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.zombified", snapshot.identity().getString())
                : TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.witch", snapshot.identity().getString());
        VillagerEventsLogger121.send(event, message(Text.literal(action), snapshot));
    }

    private static void sendDeath(Session current, VillagerEventSnapshot121 snapshot) {
        Text rendered = TextRenderer121.renderDeath(snapshot.deathMessage(), snapshot.identity(), current.language.translations());
        if (rendered == null) { current.warnOnce("suppressed death message with unresolved vanilla component"); return; }
        VillagerEventsLogger121.send("death", message(rendered, snapshot));
    }

    private static Text message(Text detail, VillagerEventSnapshot121 snapshot) {
        String dimension = switch (snapshot.dimensionId()) {
            case "overworld" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.overworld");
            case "the_nether" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.nether");
            case "the_end" -> TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.dimension.end");
            default -> snapshot.dimensionId().replace('_', ' ');
        };
        return Text.literal("[VillagerEvents] ").append(detail).append(" | ").append(dimension).append(" | ")
                .append(snapshot.x() + ", " + snapshot.y() + ", " + snapshot.z());
    }

    private static final class Session implements AutoCloseable {
        private final MinecraftServer server;
        private final String locale;
        private final VanillaLanguageService language;
        private final ArrayDeque<VillagerEventSnapshot121> pending = new ArrayDeque<>();
        private final AtomicBoolean warned = new AtomicBoolean();
        private volatile boolean closed;

        private Session(MinecraftServer server) {
            this.server = server;
            this.locale = VanillaLanguageService.normalizeLocale(CarpetSettings.language);
            Path root = FabricLoader.getInstance().getConfigDir().resolve("carpet-ice-addition").resolve("vanilla-language");
            String minecraftVersion = FabricLoader.getInstance().getModContainer("minecraft")
                    .orElseThrow(() -> new IllegalStateException("Minecraft metadata unavailable"))
                    .getMetadata().getVersion().getFriendlyString();
            this.language = new VanillaLanguageService(minecraftVersion, locale, root, VillagerEventsRuntime121.class.getClassLoader(), LOGGER);
            this.language.start(ignored -> server.execute(() -> {
                if (closed || session != this || language.state() != State.READY) return;
                while (!pending.isEmpty()) sendDeath(this, pending.removeFirst());
            }));
        }

        private void enqueue(VillagerEventSnapshot121 snapshot) {
            // Conservative bounded policy until a future configuration surface is introduced.
            if (pending.size() >= 128) { warnOnce("death queue is full; newest death message was skipped"); return; }
            pending.addLast(snapshot);
        }

        private void warnOnce(String message) { if (warned.compareAndSet(false, true)) LOGGER.warn("[VillagerEvents] {}", message); }
        @Override public void close() { closed = true; pending.clear(); language.close(); }
    }
}
