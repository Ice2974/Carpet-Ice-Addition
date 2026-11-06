package com.ice2974.carpeticeaddition.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineStatusRollbackWarningConfigTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"rollbackCommandPatterns\":null}", "null"})
    void missingOrNullPatternsKeepDefaultRollbackMatching(String json) throws Exception {
        List<Pattern> patterns = loadPatterns(json);
        assertEquals(5, patterns.size());
        for (String command : List.of("qb back", "/qb restore latest", "quickbackupmulti back",
                "!!qb back", "!!pb back", "!!cb back")) {
            assertTrue(matches(patterns, command), command);
        }
        assertFalse(matches(patterns, "qb make"));
    }

    @Test
    void explicitEmptyArrayDisablesAllMatching() throws Exception {
        List<Pattern> patterns = loadPatterns("{\"rollbackCommandPatterns\":[]}");
        assertTrue(patterns.isEmpty());
        assertFalse(matches(patterns, "qb back"));
    }

    @Test
    void explicitPatternsReplaceDefaults() throws Exception {
        List<Pattern> patterns = loadPatterns("{\"rollbackCommandPatterns\":[\"^custom restore$\"]}");
        assertEquals(1, patterns.size());
        assertTrue(matches(patterns, "custom restore"));
        assertFalse(matches(patterns, "qb back"));
    }

    @SuppressWarnings("unchecked")
    private List<Pattern> loadPatterns(String json) throws Exception {
        Path file = directory.resolve("warning.json");
        Files.writeString(file, json);
        // Exercise the production loader without initializing FabricLoader or changing global config.
        Method load = MachineStatusRollbackWarningConfig.class.getDeclaredMethod("load", Path.class);
        load.setAccessible(true);
        Object state = load.invoke(null, file);
        Field compiled = state.getClass().getDeclaredField("compiledRollbackCommandPatterns");
        compiled.setAccessible(true);
        assertEquals(json, Files.readString(file));
        return (List<Pattern>) compiled.get(state);
    }

    private static boolean matches(List<Pattern> patterns, String command) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(command).matches());
    }
}
