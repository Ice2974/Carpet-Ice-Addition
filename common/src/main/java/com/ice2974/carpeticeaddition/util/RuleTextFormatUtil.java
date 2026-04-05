package com.ice2974.carpeticeaddition.util;

import java.util.regex.Pattern;

public final class RuleTextFormatUtil {
    private static final Pattern AMPERSAND_FORMAT_PATTERN = Pattern.compile("(?i)&([0-9A-FK-OR])");

    private RuleTextFormatUtil() {
    }

    public static String formatAmpersandCodes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return AMPERSAND_FORMAT_PATTERN.matcher(input).replaceAll("\u00a7$1");
    }
}
