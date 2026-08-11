package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The datapack controller for the craftableCoralBlocks rule only.
 *
 * <p>All state transitions are server-thread owned.  The resource reload
 * callbacks are deliberately coalesced here so a rule observer, an external
 * reload, and conflict detection cannot recursively start the same reload.
 */
public final class CraftableCoralBlocksDataPackController {
    public static final Identifier PACK_ID = Identifier.tryParse("carpet-ice-addition:craftable_coral_blocks");

    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static boolean initialized;
    private static MinecraftServer server;
    private static boolean reloadInFlight;
    private static boolean ownReloadStart;
    private static boolean worldsLoaded;
    private static boolean ready;
    private static boolean degraded;
    private static boolean retryUsed;
    private static Boolean pendingTarget;
    private static long generation;

    private CraftableCoralBlocksDataPackController() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        var container = FabricLoader.getInstance()
                .getModContainer("carpet-ice-addition")
                .orElseThrow(() -> new IllegalStateException("Missing carpet-ice-addition mod container"));
        if (!ResourceManagerHelper.registerBuiltinResourcePack(PACK_ID, container, ResourcePackActivationType.NORMAL)) {
            LOGGER.warn("[Carpet Ice Addition] Failed to register craftableCoralBlocks builtin datapack");
        }
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((minecraftServer, ignored) -> onReloadStart(minecraftServer));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((minecraftServer, ignored, success) ->
                minecraftServer.execute(() -> onReloadEnd(minecraftServer, success)));
    }

    public static void onServerLoadedWorlds(MinecraftServer minecraftServer) {
        bind(minecraftServer);
        worldsLoaded = true;
        ready = false;
        retryUsed = false;
        degraded = false;
        try {
            CraftableCoralBlocksConflictDetector.recomputeAndNotify(minecraftServer);
            requestReconcile(minecraftServer, false);
        } catch (Throwable throwable) {
            LOGGER.warn("[Carpet Ice Addition] craftableCoralBlocks startup reconciliation failed", throwable);
        }
    }

    public static void onRuleChanged(MinecraftServer minecraftServer) {
        if (minecraftServer == null) {
            return;
        }
        bind(minecraftServer);
        if (!worldsLoaded) {
            return;
        }
        retryUsed = false;
        degraded = false;
        requestReconcile(minecraftServer, false);
    }

    public static void onPlayerJoin(MinecraftServer minecraftServer, ServerPlayerEntity player) {
        if (minecraftServer == null) {
            return;
        }
        if (!ready) {
            return;
        }
        CraftableCoralBlocksRecipeBookHelper.onPlayerJoin(minecraftServer, player);
    }

    public static void onServerClosed(MinecraftServer minecraftServer) {
        if (server == minecraftServer) {
            generation++;
            server = null;
            reloadInFlight = false;
            ownReloadStart = false;
            ready = false;
            worldsLoaded = false;
            degraded = false;
            retryUsed = false;
            pendingTarget = null;
        }
    }

    private static void bind(MinecraftServer minecraftServer) {
        if (server != minecraftServer) {
            server = minecraftServer;
            generation++;
            reloadInFlight = false;
            ownReloadStart = false;
            ready = false;
            worldsLoaded = false;
            degraded = false;
            retryUsed = false;
            pendingTarget = null;
        }
    }

    private static void onReloadStart(MinecraftServer minecraftServer) {
        bind(minecraftServer);
        if (ownReloadStart) {
            ownReloadStart = false;
            return;
        }
        reloadInFlight = true;
        generation++;
        retryUsed = false;
        degraded = false;
    }

    private static void onReloadEnd(MinecraftServer minecraftServer, boolean success) {
        if (server != minecraftServer || !reloadInFlight) {
            return;
        }
        reloadInFlight = false;
        if (!worldsLoaded) {
            return;
        }
        if (!success) {
            handleReloadFailure(minecraftServer, new IllegalStateException("Minecraft datapack reload returned success=false"));
            return;
        }
        try {
            CraftableCoralBlocksConflictDetector.recomputeAndNotify(minecraftServer);
            CraftableCoralBlocksRecipeBookHelper.onReload(minecraftServer);
            requestReconcile(minecraftServer, false);
        } catch (Throwable throwable) {
            handleReloadFailure(minecraftServer, throwable);
        }
    }

    private static void requestReconcile(MinecraftServer minecraftServer, boolean forceRetry) {
        if (server != minecraftServer) {
            return;
        }
        boolean target = CraftableCoralBlocksSettings.effective();
        boolean actual = isPackEnabled(minecraftServer);
        pendingTarget = target;
        if (reloadInFlight) {
            return;
        }
        if (target == actual) {
            pendingTarget = null;
            if (!ready) {
                CraftableCoralBlocksRecipeBookHelper.onReload(minecraftServer);
                ready = true;
            }
            return;
        }
        if (degraded && !forceRetry) {
            return;
        }
        startReload(minecraftServer, target);
    }

    private static void startReload(MinecraftServer minecraftServer, boolean target) {
        Collection<String> selected = minecraftServer.getDataPackManager().getEnabledIds();
        Set<String> next = new LinkedHashSet<>(selected);
        if (target) {
            next.add(PACK_ID.toString());
        } else {
            next.remove(PACK_ID.toString());
            CraftableCoralBlocksRecipeBookHelper.onPackDisable(minecraftServer);
        }
        reloadInFlight = true;
        ownReloadStart = true;
        long requestGeneration = ++generation;
        try {
            CompletableFuture<Void> future = minecraftServer.reloadResources(next);
            future.whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    minecraftServer.execute(() -> {
                        if (server == minecraftServer && reloadInFlight && generation == requestGeneration) {
                            reloadInFlight = false;
                            handleReloadFailure(minecraftServer, throwable);
                        }
                    });
                }
            });
        } catch (Throwable throwable) {
            reloadInFlight = false;
            handleReloadFailure(minecraftServer, throwable);
        }
    }

    private static boolean isPackEnabled(MinecraftServer minecraftServer) {
        return minecraftServer.getDataPackManager().getEnabledIds().contains(PACK_ID.toString());
    }

    private static void handleReloadFailure(MinecraftServer minecraftServer, Throwable throwable) {
        if (!retryUsed) {
            retryUsed = true;
            minecraftServer.execute(() -> requestReconcile(minecraftServer, true));
            LOGGER.warn("[Carpet Ice Addition] craftableCoralBlocks datapack reload failed; scheduling one retry", throwable);
            return;
        }
        degraded = true;
        ready = false;
        LOGGER.warn("[Carpet Ice Addition] craftableCoralBlocks datapack state is degraded; automatic retries stopped", throwable);
    }
}
