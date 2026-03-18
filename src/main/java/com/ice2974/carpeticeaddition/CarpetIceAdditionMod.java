package com.ice2974.carpeticeaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.compat.RuntimeCompatibility;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
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

    public static boolean shouldEnableSafeScaffoldingBreak() {
        return compatibility == null || compatibility.shouldEnableSafeScaffoldingBreak();
    }

    public static boolean shouldEnableCrafterOutputBlockRule() {
        return compatibility == null || compatibility.shouldEnableCrafterOutputBlockRule();
    }

    public static boolean shouldEnableRecordWorldEventFix() {
        return compatibility == null || compatibility.shouldEnableRecordWorldEventFix();
    }

    public static void reportFeatureCompatibilityIssue(String featureName, Throwable throwable) {
        AtomicBoolean flag;
        if ("safeScaffoldingBreak".equals(featureName)) {
            flag = SAFE_SCAFFOLDING_BREAK_ERROR_REPORTED;
        } else if ("crafterStopsWhenOutputBlocked".equals(featureName)) {
            flag = CRAFTER_OUTPUT_RULE_ERROR_REPORTED;
        } else if ("recordWorldEventFix".equals(featureName)) {
            flag = RECORD_WORLD_EVENT_FIX_ERROR_REPORTED;
        } else {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}: {}", featureName, throwable.toString());
            return;
        }

        if (flag.compareAndSet(false, true)) {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}. Feature will be safely skipped. Cause: {}",
                    featureName, throwable.toString());
        }
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        if (lang != null && lang.toLowerCase().startsWith("zh")) {
            return Map.of(
                    "carpet.category.carpetIceAddition",
                    "Ice",
                    "carpet.rule.safeScaffoldingBreak.name",
                    "\u811a\u624b\u67b6\u9632\u8bef\u89e6",
                    "carpet.rule.safeScaffoldingBreak.desc",
                    "\u53ea\u6709\u4e3b\u624b\u6301\u811a\u624b\u67b6\u6216\u4e3b\u624b\u4e3a\u7a7a\u65f6\uff0c\u73a9\u5bb6\u624d\u80fd\u7834\u574f\u811a\u624b\u67b6\uff0c\u9632\u6b62\u8bef\u62c6\u3002",
                    "carpet.rule.crafterStopsWhenOutputBlocked.name",
                    "\u5408\u6210\u5668\u8f93\u51fa\u963b\u585e\u65f6\u505c\u6b62\u5408\u6210",
                    "carpet.rule.crafterStopsWhenOutputBlocked.desc",
                    "\u5f53\u5408\u6210\u5668\u524d\u65b9\u662f\u53ef\u81ea\u52a8\u63a5\u6536\u4ea7\u7269\u7684\u6709\u6548\u5bb9\u5668\uff0c\u4e14\u65e0\u6cd5\u5b8c\u6574\u63a5\u6536\u672c\u6b21\u4e3b\u4ea7\u7269\u4e0e\u914d\u65b9\u4f59\u7559\u7269\u65f6\uff0c\u53d6\u6d88\u672c\u6b21\u5408\u6210\uff0c\u907f\u514d\u4ea7\u7269\u55b7\u51fa\u3002",
                    "carpet.rule.recordWorldEventFix.name",
                    "\u5531\u7247\u4e16\u754c\u4e8b\u4ef6\u65f6\u5e8f\u4fee\u590d",
                    "carpet.rule.recordWorldEventFix.desc",
                    "\u4fee\u590d\u4e86\u5c06\u5531\u7247\u5feb\u901f\u653e\u5165\u5531\u7247\u673a\u540e\u53c8\u8fc5\u901f\u53d6\u51fa\u65f6\uff0c\u97f3\u4e50\u4ecd\u53ef\u80fd\u7ee7\u7eed\u64ad\u653e\uff0c\u4e14\u591a\u4e2a\u5531\u7247\u97f3\u9891\u53ef\u80fd\u91cd\u53e0\u7684\u95ee\u9898\uff0c\u8be6\u89c1 MC-112245\u3002",
                    "message.carpet-ice-addition.safe_scaffolding_break",
                    "\u4f60\u5fc5\u987b\u624b\u6301\u811a\u624b\u67b6\u6216\u7a7a\u624b\u624d\u80fd\u7834\u574f\u811a\u624b\u67b6"
            );
        }
        return Map.of(
                "carpet.category.carpetIceAddition",
                "Ice",
                "carpet.rule.safeScaffoldingBreak.name",
                "\u811a\u624b\u67b6\u9632\u8bef\u89e6",
                "carpet.rule.safeScaffoldingBreak.desc",
                "Require holding scaffolding or an empty main hand to break scaffolding.",
                "carpet.rule.crafterStopsWhenOutputBlocked.name",
                "\u5408\u6210\u5668\u8f93\u51fa\u963b\u585e\u65f6\u505c\u6b62\u5408\u6210",
                "carpet.rule.crafterStopsWhenOutputBlocked.desc",
                "Prevent the crafter from crafting when its output points to a valid auto-accepting container that cannot fully accept the crafted result and recipe remainders.",
                "carpet.rule.recordWorldEventFix.name",
                "\u5531\u7247\u4e16\u754c\u4e8b\u4ef6\u65f6\u5e8f\u4fee\u590d",
                "carpet.rule.recordWorldEventFix.desc",
                "Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, which may also cause overlapping disc audio. See MC-112245.",
                "message.carpet-ice-addition.safe_scaffolding_break",
                "Hold scaffolding or empty your main hand to break scaffolding."
        );
    }
}
