package com.ice2974.carpeticeaddition.command;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class MachineStatusStateUtil {
    private static final String DEFAULT_NAMESPACE = "minecraft";
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+");

    private MachineStatusStateUtil() {
    }

    public static ParsedState parse(String rawState) {
        if (rawState == null) {
            return null;
        }

        String normalized = rawState.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        int bracketIndex = normalized.indexOf('[');
        String blockIdPart;
        String propertiesPart = null;
        if (bracketIndex < 0) {
            if (normalized.indexOf(']') >= 0) {
                return null;
            }
            blockIdPart = normalized;
        } else {
            if (!normalized.endsWith("]")) {
                return null;
            }
            if (normalized.indexOf(']') != normalized.length() - 1) {
                return null;
            }
            blockIdPart = normalized.substring(0, bracketIndex).trim();
            propertiesPart = normalized.substring(bracketIndex + 1, normalized.length() - 1).trim();
        }

        if (!IDENTIFIER_PATTERN.matcher(blockIdPart).matches()) {
            return null;
        }

        LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        if (propertiesPart != null && !propertiesPart.isEmpty()) {
            for (String entry : propertiesPart.split(",")) {
                String token = entry.trim();
                int equalsIndex = token.indexOf('=');
                if (equalsIndex <= 0 || equalsIndex >= token.length() - 1) {
                    return null;
                }

                String key = token.substring(0, equalsIndex).trim();
                String value = token.substring(equalsIndex + 1).trim();
                if (key.isEmpty() || value.isEmpty() || properties.putIfAbsent(key, value) != null) {
                    return null;
                }
            }
        }

        return new ParsedState(
                normalizeIdentifier(blockIdPart),
                Collections.unmodifiableMap(new LinkedHashMap<>(properties))
        );
    }

    public static String normalizeIdentifier(String rawIdentifier) {
        String normalized = rawIdentifier == null ? "" : rawIdentifier.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        if (normalized.indexOf(':') < 0) {
            return DEFAULT_NAMESPACE + ":" + normalized;
        }
        return normalized;
    }

    public record ParsedState(String blockId, Map<String, String> properties) {
    }
}
