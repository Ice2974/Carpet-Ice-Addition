package com.ice2974.carpeticeaddition.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CustomEndPlatformPositionHelper {
    public static final String VANILLA = "vanilla";

    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");
    private static final Pattern PATTERN = Pattern.compile("^(-?\\d+),(-?\\d+),(-?\\d+)$");
    private static final AtomicBoolean COMPATIBILITY_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean OUT_OF_BOUNDS_REPORTED = new AtomicBoolean(false);

    private CustomEndPlatformPositionHelper() {
    }

    public static Optional<IntPosition> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String normalized = value.trim();
        if (VANILLA.equals(normalized)) {
            return Optional.empty();
        }

        Matcher matcher = PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new IntPosition(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static void reportCompatibilityIssue(Throwable throwable) {
        if (COMPATIBILITY_ERROR_REPORTED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[Carpet Ice Addition] Compatibility issue in feature customEndPlatformPosition. Feature will be safely skipped. Cause: {}",
                    throwable.toString()
            );
        }
    }

    public static void reportOutOfBoundsPosition(String configuredValue) {
        if (OUT_OF_BOUNDS_REPORTED.compareAndSet(false, true)) {
            LOGGER.warn(
                    "[Carpet Ice Addition] customEndPlatformPosition={} is outside the target world's usable bounds. Falling back to vanilla End portal behavior.",
                    configuredValue
            );
        }
    }

    public record IntPosition(int x, int y, int z) {
        public String asRuleString() {
            return this.x + "," + this.y + "," + this.z;
        }
    }
}
