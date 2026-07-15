package com.ice2974.carpeticeaddition.villagerevents;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class VanillaLanguageServiceTest {
    @Test void normalizesOnlySupportedLocaleFamilies() {
        assertEquals("zh_cn", VanillaLanguageService.normalizeLocale("zh_tw"));
        assertEquals("en_us", VanillaLanguageService.normalizeLocale("en_us"));
        assertEquals("en_us", VanillaLanguageService.normalizeLocale("../../zh_cn"));
    }
    @Test void readsWithHardLimit() throws Exception {
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), VanillaLanguageService.readLimited(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)), 3));
        assertThrows(IOException.class, () -> VanillaLanguageService.readLimited(new ByteArrayInputStream("abcd".getBytes(StandardCharsets.UTF_8)), 3));
    }
    @Test void parsesOnlyNonEmptyLanguageObjects() throws Exception {
        assertEquals("Fall", VanillaLanguageService.parseLanguage("{\"death.fell\":\"Fall\"}".getBytes(StandardCharsets.UTF_8)).get("death.fell"));
        assertThrows(IOException.class, () -> VanillaLanguageService.parseLanguage("{}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> VanillaLanguageService.parseLanguage("{".getBytes(StandardCharsets.UTF_8)));
    }
    @Test void validatesSha1() throws Exception {
        VanillaLanguageService.verifySha1("abc".getBytes(StandardCharsets.UTF_8), "a9993e364706816aba3e25717850c26c9cd0d89d");
        assertThrows(IOException.class, () -> VanillaLanguageService.verifySha1("abc".getBytes(StandardCharsets.UTF_8), "0000000000000000000000000000000000000000"));
    }
    @Test void acceptsOnlyRenderedVanillaPlaceholderSubset() {
        assertTrue(VanillaFormatString.isSupported("%s %1$s %%"));
        assertFalse(VanillaFormatString.isSupported("%d"));
        assertFalse(VanillaFormatString.isSupported("%1$d"));
        assertFalse(VanillaFormatString.isSupported("%"));
        assertFalse(VanillaFormatString.isSupported("%1s"));
    }
}
