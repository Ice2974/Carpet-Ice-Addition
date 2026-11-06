package com.ice2974.carpeticeaddition.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillItemConfigManagerTest {
    @TempDir
    Path saveRoot;

    @AfterEach
    void shutdown() {
        KillItemConfigManager.shutdown();
    }

    @Test
    void missingFileCanBeCreatedAndReloaded() throws IOException {
        KillItemConfigManager.initialize(saveRoot);
        assertTrue(KillItemConfigManager.addBlacklistItem("diamond"));
        assertTrue(KillItemConfigManager.setClearNamedItems(true));
        KillItemConfigManager.shutdown();
        KillItemConfigManager.initialize(saveRoot);

        assertEquals(new KillItemConfigManager.Snapshot(Set.of("minecraft:diamond"), true),
                KillItemConfigManager.snapshot());
    }

    @Test
    void ioFailureProtectsRestoredBlacklistUntilSuccessfulInitialization() throws IOException {
        KillItemConfigManager.initialize(saveRoot);
        KillItemConfigManager.addBlacklistItem("minecraft:diamond");
        KillItemConfigManager.shutdown();
        Path path = KillItemConfigManager.resolvePath(saveRoot);
        Path backup = path.resolveSibling("original.json");
        Files.move(path, backup);
        byte[] original = Files.readAllBytes(backup);

        Files.createDirectory(path);
        KillItemConfigManager.initialize(saveRoot);
        assertEquals(new KillItemConfigManager.Snapshot(Set.of(), false), KillItemConfigManager.snapshot());
        Files.delete(path);
        Files.move(backup, path);

        assertAllMutationsRejected();
        assertArrayEquals(original, Files.readAllBytes(path));
        assertEquals(new KillItemConfigManager.Snapshot(Set.of(), false), KillItemConfigManager.snapshot());
        assertFalse(Files.exists(path.resolveSibling("killitem.json.tmp")));

        KillItemConfigManager.initialize(saveRoot);
        assertTrue(KillItemConfigManager.setClearNamedItems(true));
        KillItemConfigManager.shutdown();
        KillItemConfigManager.initialize(saveRoot);
        assertEquals(new KillItemConfigManager.Snapshot(Set.of("minecraft:diamond"), true),
                KillItemConfigManager.snapshot());
    }

    @Test
    void malformedFileIsKeptUntouchedAndRepeatedInitializationStaysProtected() throws IOException {
        Path path = KillItemConfigManager.resolvePath(saveRoot);
        Files.createDirectories(path.getParent());
        String malformed = "{\"blacklist\":[";
        Files.writeString(path, malformed);

        for (int attempt = 0; attempt < 2; attempt++) {
            KillItemConfigManager.initialize(saveRoot);
            assertAllMutationsRejected();
            assertEquals(malformed, Files.readString(path));
        }

        KillItemConfigManager.shutdown();
        Path anotherWorld = saveRoot.resolve("another-world");
        KillItemConfigManager.initialize(anotherWorld);
        assertTrue(KillItemConfigManager.addBlacklistItem("minecraft:gold_ingot"));
        assertTrue(Files.exists(KillItemConfigManager.resolvePath(anotherWorld)));
        assertEquals(malformed, Files.readString(path));
    }

    @Test
    void failedSaveDoesNotChangeSnapshotOrExistingFile() throws IOException {
        KillItemConfigManager.initialize(saveRoot);
        KillItemConfigManager.addBlacklistItem("minecraft:diamond");
        Path path = KillItemConfigManager.resolvePath(saveRoot);
        String original = Files.readString(path);
        Files.createDirectory(path.resolveSibling("killitem.json.tmp"));

        assertThrows(IOException.class, () -> KillItemConfigManager.setClearNamedItems(true));
        assertEquals(original, Files.readString(path));
        assertEquals(new KillItemConfigManager.Snapshot(Set.of("minecraft:diamond"), false),
                KillItemConfigManager.snapshot());
    }

    private static void assertAllMutationsRejected() {
        assertThrows(IOException.class, () -> KillItemConfigManager.addBlacklistItem("minecraft:gold_ingot"));
        assertThrows(IOException.class, () -> KillItemConfigManager.removeBlacklistItem("minecraft:diamond"));
        assertThrows(IOException.class, KillItemConfigManager::clearBlacklist);
        assertThrows(IOException.class, () -> KillItemConfigManager.setClearNamedItems(true));
        // A no-op against the fallback must not report success while the real value is unknown.
        assertThrows(IOException.class, () -> KillItemConfigManager.setClearNamedItems(false));
    }
}
