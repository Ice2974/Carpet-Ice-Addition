package com.ice2974.carpeticeaddition.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuntimeCompatibility {
    private static final String LOADER_MIN = "0.18.4";
    private static final String FABRIC_API_MIN = "0.129.0+1.21.7";
    private static final String CARPET_MIN = "1.4.177";

    private final CheckResult loader;
    private final CheckResult fabricApi;
    private final CheckResult carpet;

    private RuntimeCompatibility(CheckResult loader, CheckResult fabricApi, CheckResult carpet) {
        this.loader = loader;
        this.fabricApi = fabricApi;
        this.carpet = carpet;
    }

    public static RuntimeCompatibility detect(Logger logger) {
        CheckResult loader = checkLoaderVersion();
        CheckResult fabricApi = checkModVersion("fabric-api", FABRIC_API_MIN);
        CheckResult carpet = checkModVersion("carpet", CARPET_MIN);

        logResult(logger, "fabric-loader", LOADER_MIN, loader);
        logResult(logger, "fabric-api", FABRIC_API_MIN, fabricApi);
        logResult(logger, "carpet", CARPET_MIN, carpet);

        if (!loader.compatible || !fabricApi.compatible || !carpet.compatible) {
            logger.warn("[Carpet Ice Addition] Running in compatibility fallback mode due to version mismatch.");
        }

        return new RuntimeCompatibility(loader, fabricApi, carpet);
    }

    public boolean shouldEnableSafeScaffoldingBreak() {
        return loader.compatible && carpet.compatible && fabricApi.compatible;
    }

    public boolean shouldEnableCrafterOutputBlockRule() {
        return loader.compatible && carpet.compatible && fabricApi.compatible;
    }

    public boolean shouldEnableRecordWorldEventFix() {
        return loader.compatible && carpet.compatible && fabricApi.compatible;
    }

    public boolean shouldEnableDisableParticlesPackets() {
        return loader.compatible && carpet.compatible && fabricApi.compatible;
    }

    public boolean isInFallbackMode() {
        return !loader.compatible || !fabricApi.compatible || !carpet.compatible;
    }

    private static CheckResult checkLoaderVersion() {
        try {
            String version = FabricLoader.getInstance().getModContainer("fabricloader")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse(null);
            if (version == null) {
                return CheckResult.missing();
            }
            return CheckResult.of(version, compareNormalized(normalize("fabricloader", version), normalize("fabricloader", LOADER_MIN)) >= 0);
        } catch (Throwable ignored) {
            return CheckResult.unknown();
        }
    }

    private static CheckResult checkModVersion(String modId, String minimumVersion) {
        try {
            Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(modId);
            if (container.isEmpty()) {
                if ("fabric-api".equals(modId) && hasFabricApiModules()) {
                    return CheckResult.of("module-split", true);
                }
                return CheckResult.missing();
            }
            String version = container.get().getMetadata().getVersion().getFriendlyString();
            boolean compatible = compareNormalized(normalize(modId, version), normalize(modId, minimumVersion)) >= 0;
            return CheckResult.of(version, compatible);
        } catch (Throwable ignored) {
            return CheckResult.unknown();
        }
    }

    private static String normalize(String modId, String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        if ("carpet".equals(modId) && value.contains("-")) {
            value = value.substring(value.indexOf('-') + 1);
        }
        int plusIndex = value.indexOf('+');
        if (plusIndex > -1) {
            value = value.substring(0, plusIndex);
        }
        return value;
    }

    private static int compareNormalized(String current, String minimum) {
        int[] currentParts = toNumericParts(current);
        int[] minParts = toNumericParts(minimum);
        int max = Math.max(currentParts.length, minParts.length);
        for (int i = 0; i < max; i++) {
            int c = i < currentParts.length ? currentParts[i] : 0;
            int m = i < minParts.length ? minParts[i] : 0;
            if (c != m) {
                return Integer.compare(c, m);
            }
        }
        return 0;
    }

    private static int[] toNumericParts(String version) {
        Matcher matcher = Pattern.compile("\\d+").matcher(version);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        int[] parts = new int[count];
        matcher.reset();
        int index = 0;
        while (matcher.find()) {
            parts[index++] = Integer.parseInt(matcher.group());
        }
        return parts;
    }

    private static boolean hasFabricApiModules() {
        return FabricLoader.getInstance()
                .getAllMods()
                .stream()
                .map(mod -> mod.getMetadata().getId())
                .anyMatch(id -> id.startsWith("fabric-") && !"fabricloader".equals(id) && !"fabric-api".equals(id));
    }

    private static void logResult(Logger logger, String name, String minimum, CheckResult result) {
        if (result.missing) {
            logger.warn("[Carpet Ice Addition] {} not found at runtime. Minimum expected: {}", name, minimum);
            return;
        }
        if (result.unknown) {
            logger.warn("[Carpet Ice Addition] {} version could not be detected. Proceeding in fallback mode.", name);
            return;
        }
        if (result.compatible) {
            logger.info("[Carpet Ice Addition] {} version {} (minimum {}).", name, result.detectedVersion, minimum);
        } else {
            logger.warn("[Carpet Ice Addition] {} version {} is below minimum {}. Fallback mode enabled.", name, result.detectedVersion, minimum);
        }
    }

    private static final class CheckResult {
        private final String detectedVersion;
        private final boolean compatible;
        private final boolean missing;
        private final boolean unknown;

        private CheckResult(String detectedVersion, boolean compatible, boolean missing, boolean unknown) {
            this.detectedVersion = detectedVersion;
            this.compatible = compatible;
            this.missing = missing;
            this.unknown = unknown;
        }

        private static CheckResult of(String detectedVersion, boolean compatible) {
            return new CheckResult(detectedVersion, compatible, false, false);
        }

        private static CheckResult missing() {
            return new CheckResult("missing", false, true, false);
        }

        private static CheckResult unknown() {
            return new CheckResult("unknown", false, false, true);
        }
    }
}
