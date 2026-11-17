package com.ice2974.carpeticeaddition.translation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarpetIceAdditionTranslationsTest {
    @Test
    void selectsBundledDictionariesOnEveryLanguageChange() {
        Map<String, String> chinese = CarpetIceAdditionTranslations.get("zh_cn");
        Map<String, String> english = CarpetIceAdditionTranslations.get("en_us");
        String key = "carpet.rule.backpressureCrafter.desc";
        assertNotEquals(chinese.get(key), english.get(key));
        assertEquals(chinese, CarpetIceAdditionTranslations.get("zh_tw"));
        assertEquals(english, CarpetIceAdditionTranslations.get("fr_fr"));
        assertEquals(chinese, CarpetIceAdditionTranslations.get("ZH_CN"));
        assertEquals(english, CarpetIceAdditionTranslations.get(null));
        assertEquals(english, CarpetIceAdditionTranslations.get(""));
        assertThrows(UnsupportedOperationException.class, () -> chinese.put("test", "value"));
    }

    @Test
    void fallsBackToEnglishIfChineseIsMissingOrInvalid() {
        Map<String, String> english = Map.of("test", "English");
        for (String invalid : new String[]{null, "{", "{\"test\":7}", "{\"test\":\"partial\",\"test\":\"duplicate\"}"}) {
            var translations = new CarpetIceAdditionTranslations(path ->
                    resource(path.endsWith("/zh_cn.json") ? invalid : "{\"test\":\"English\"}"));
            assertEquals(english, translations.select("zh_cn"));
            assertEquals(english, translations.select("en_us"));
        }
    }

    @Test
    void missingEnglishDoesNotAffectChinese() {
        var translations = new CarpetIceAdditionTranslations(path ->
                path.endsWith("/zh_cn.json") ? resource("{\"test\":\"\\u4e2d\\u6587\"}") : null);
        assertEquals(Map.of(), translations.select("en_us"));
        assertEquals(Map.of("test", "\u4e2d\u6587"), translations.select("zh_cn"));
    }

    private static InputStream resource(String json) {
        return json == null ? null : new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
