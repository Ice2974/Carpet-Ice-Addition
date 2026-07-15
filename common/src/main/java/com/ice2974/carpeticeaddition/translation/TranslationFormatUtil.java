package com.ice2974.carpeticeaddition.translation;

import carpet.CarpetSettings;
import carpet.utils.Translations;

import java.util.IllegalFormatException;
import java.util.Locale;

public final class TranslationFormatUtil {
    private TranslationFormatUtil() {
    }

    public static String translate(String key, Object... args) {
        String template = Translations.tr(key);
        if (key.equals(template)) {
            String loggerTemplate = CarpetIceAdditionTranslations.villagerEvents(key, CarpetSettings.language);
            if (loggerTemplate != null) {
                template = loggerTemplate;
            }
        }
        if (args.length == 0) {
            return template;
        }

        Object[] normalizedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            normalizedArgs[i] = stringify(args[i]);
        }

        try {
            return String.format(Locale.ROOT, template, normalizedArgs);
        } catch (IllegalFormatException ignored) {
            return template;
        }
    }

    private static String stringify(Object value) {
        return value == null ? "null" : value.toString();
    }
}
