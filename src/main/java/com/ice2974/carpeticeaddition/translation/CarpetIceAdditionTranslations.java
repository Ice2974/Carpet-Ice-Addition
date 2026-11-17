package com.ice2974.carpeticeaddition.translation;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class CarpetIceAdditionTranslations {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarpetIceAdditionTranslations.class);
    private static final String LANG_PATH = "/assets/carpet-ice-addition/lang/";

    private final Map<String, String> chinese;
    private final Map<String, String> english;

    private static class Bundled {
        private static final CarpetIceAdditionTranslations DICTIONARIES =
                new CarpetIceAdditionTranslations(CarpetIceAdditionTranslations.class::getResourceAsStream);
    }

    CarpetIceAdditionTranslations(Function<String, InputStream> openResource) {
        chinese = load("zh_cn", openResource);
        english = load("en_us", openResource);
    }

    public static Map<String, String> get(String lang) {
        return Bundled.DICTIONARIES.select(lang);
    }

    Map<String, String> select(String lang) {
        return lang != null && lang.toLowerCase(Locale.ROOT).startsWith("zh") && !chinese.isEmpty()
                ? chinese : english;
    }

    private static Map<String, String> load(String lang, Function<String, InputStream> openResource) {
        String path = LANG_PATH + lang + ".json";
        InputStream stream = openResource.apply(path);
        if (stream == null) {
            LOGGER.warn("Cannot load translation resource {}: file is missing", path);
            return Map.of();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8.newDecoder()))) {
            reader.setStrictness(Strictness.STRICT);
            Map<String, String> translations = new HashMap<>();
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (reader.peek() != JsonToken.STRING) {
                    throw new IOException("Expected a string for translation key " + key);
                }
                if (translations.putIfAbsent(key, reader.nextString()) != null) {
                    throw new IOException("Duplicate translation key " + key);
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT || translations.isEmpty()) {
                throw new IOException("Expected a single non-empty translation object");
            }
            return Map.copyOf(translations);
        } catch (IOException | IllegalStateException e) {
            LOGGER.warn("Cannot load translation resource {}: {}", path, e.getMessage());
            return Map.of();
        }
    }
}
