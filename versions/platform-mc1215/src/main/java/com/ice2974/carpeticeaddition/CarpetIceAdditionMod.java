package com.ice2974.carpeticeaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.compat.RuntimeCompatibility;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.CarpetIceAdditionTranslations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CarpetIceAdditionMod implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "carpet-ice-addition";
    public static final String MOD_NAME = "Carpet Ice Addition";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    private static final CarpetIceAdditionMod INSTANCE = new CarpetIceAdditionMod();
    private static final AtomicBoolean SAFE_SCAFFOLDING_BREAK_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CRAFTER_OUTPUT_RULE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean RECORD_WORLD_EVENT_FIX_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean SPAWNERS_IGNORE_INVISIBLE_PLAYERS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_KELP_NATURAL_GROWTH_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED = new AtomicBoolean(false);

    private static String version;
    private static RuntimeCompatibility compatibility;

    @Override
    public void onInitialize() {
        version = FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .orElseThrow(RuntimeException::new)
                .getMetadata()
                .getVersion()
                .getFriendlyString();
        compatibility = RuntimeCompatibility.detect(LOGGER);
        CarpetServer.manageExtension(INSTANCE);
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(CarpetIceAdditionSettings.class);
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return CarpetIceAdditionTranslations.get(lang);
    }

    public static boolean isCompatibilityFallbackMode() {
        return compatibility != null && compatibility.isInFallbackMode();
    }

    public static boolean shouldEnableSafeScaffoldingBreak() {
        return compatibility == null || compatibility.shouldEnableSafeScaffoldingBreak();
    }

    public static boolean shouldEnableCrafterOutputBlockRule() {
        return compatibility == null || compatibility.shouldEnableCrafterOutputBlockRule();
    }

    public static boolean shouldEnableRecordWorldEventFix() {
        return compatibility == null || compatibility.shouldEnableRecordWorldEventFix();
    }

    public static boolean shouldEnableSpawnersIgnoreInvisiblePlayers() {
        return compatibility == null || compatibility.shouldEnableSpawnersIgnoreInvisiblePlayers();
    }

    public static boolean shouldEnableDisableKelpNaturalGrowth() {
        return compatibility == null || compatibility.shouldEnableDisableKelpNaturalGrowth();
    }

    public static boolean shouldEnableCanMineBuddingAmethyst() {
        return compatibility == null || compatibility.shouldEnableCanMineBuddingAmethyst();
    }

    public static void reportFeatureCompatibilityIssue(String featureName, Throwable throwable) {
        AtomicBoolean flag;
        if ("safeScaffoldingBreak".equals(featureName)) {
            flag = SAFE_SCAFFOLDING_BREAK_ERROR_REPORTED;
        } else if ("crafterStopsWhenOutputBlocked".equals(featureName)) {
            flag = CRAFTER_OUTPUT_RULE_ERROR_REPORTED;
        } else if ("recordWorldEventFix".equals(featureName)) {
            flag = RECORD_WORLD_EVENT_FIX_ERROR_REPORTED;
        } else if ("spawnersIgnoreInvisiblePlayers".equals(featureName)) {
            flag = SPAWNERS_IGNORE_INVISIBLE_PLAYERS_ERROR_REPORTED;
        } else if ("disableKelpNaturalGrowth".equals(featureName)) {
            flag = DISABLE_KELP_NATURAL_GROWTH_ERROR_REPORTED;
        } else if ("canMineBuddingAmethyst".equals(featureName)) {
            flag = CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED;
        } else {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}: {}", featureName, throwable.toString());
            return;
        }

        if (flag.compareAndSet(false, true)) {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}. Feature will be safely skipped. Cause: {}",
                    featureName, throwable.toString());
        }
    }
}
