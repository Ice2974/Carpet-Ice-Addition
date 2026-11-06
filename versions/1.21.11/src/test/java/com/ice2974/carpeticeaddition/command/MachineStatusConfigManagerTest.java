package com.ice2974.carpeticeaddition.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineStatusConfigManagerTest {
    @TempDir
    Path saveRoot;

    @AfterEach
    void shutdown() {
        MachineStatusConfigManager.shutdown();
    }

    @Test
    void missingFileCanBeCreatedAndReloaded() throws IOException {
        MachineStatusConfigManager.initialize(saveRoot);
        addMachine("old");
        MachineStatusConfigManager.shutdown();
        MachineStatusConfigManager.initialize(saveRoot);

        assertEquals(List.of(new MachineStatusConfigManager.MachineRecord(
                "old", "minecraft:overworld", 1, 64, -2, "minecraft:stone"
        )), MachineStatusConfigManager.snapshot());
    }

    @Test
    void ioFailureProtectsRestoredFileUntilSuccessfulInitialization() throws IOException {
        MachineStatusConfigManager.initialize(saveRoot);
        addMachine("old");
        MachineStatusConfigManager.shutdown();
        Path path = MachineStatusConfigManager.resolvePath(saveRoot);
        Path backup = path.resolveSibling("original.json");
        Files.move(path, backup);
        byte[] original = Files.readAllBytes(backup);

        // A directory at the file path triggers a real, portable read failure.
        Files.createDirectory(path);
        MachineStatusConfigManager.initialize(saveRoot);
        assertTrue(MachineStatusConfigManager.snapshot().isEmpty());
        Files.delete(path);
        Files.move(backup, path);

        assertAllMutationsRejected();
        assertArrayEquals(original, Files.readAllBytes(path));
        assertTrue(MachineStatusConfigManager.snapshot().isEmpty());
        assertFalse(Files.exists(path.resolveSibling("machine_status.json.tmp")));

        MachineStatusConfigManager.initialize(saveRoot);
        addMachine("new");
        MachineStatusConfigManager.shutdown();
        MachineStatusConfigManager.initialize(saveRoot);
        assertEquals(List.of("old", "new"), MachineStatusConfigManager.snapshot().stream()
                .map(MachineStatusConfigManager.MachineRecord::name).toList());
    }

    @Test
    void malformedFileIsKeptUntouchedAndRepeatedInitializationStaysProtected() throws IOException {
        Path path = MachineStatusConfigManager.resolvePath(saveRoot);
        Files.createDirectories(path.getParent());
        String malformed = "{\"machines\":[";
        Files.writeString(path, malformed);

        for (int attempt = 0; attempt < 2; attempt++) {
            MachineStatusConfigManager.initialize(saveRoot);
            assertAllMutationsRejected();
            assertEquals(malformed, Files.readString(path));
        }

        MachineStatusConfigManager.shutdown();
        Path anotherWorld = saveRoot.resolve("another-world");
        MachineStatusConfigManager.initialize(anotherWorld);
        addMachine("new");
        assertTrue(Files.exists(MachineStatusConfigManager.resolvePath(anotherWorld)));
        assertEquals(malformed, Files.readString(path));
    }

    @Test
    void failedSaveDoesNotChangeSnapshotOrExistingFile() throws IOException {
        MachineStatusConfigManager.initialize(saveRoot);
        addMachine("old");
        Path path = MachineStatusConfigManager.resolvePath(saveRoot);
        String original = Files.readString(path);
        Files.createDirectory(path.resolveSibling("machine_status.json.tmp"));

        assertThrows(IOException.class, () -> addMachine("new"));
        assertEquals(original, Files.readString(path));
        assertEquals(List.of("old"), MachineStatusConfigManager.snapshot().stream()
                .map(MachineStatusConfigManager.MachineRecord::name).toList());
    }

    private static void assertAllMutationsRejected() {
        assertThrows(IOException.class, () -> addMachine("new"));
        assertThrows(IOException.class, () -> MachineStatusConfigManager.removeMachine("old"));
        assertThrows(IOException.class, () -> MachineStatusConfigManager.renameMachine("old", "renamed"));
        assertThrows(IOException.class, () -> MachineStatusConfigManager.updateMachineState("old", "minecraft:dirt"));
        assertThrows(IOException.class, () -> MachineStatusConfigManager.moveMachine(
                "old", "minecraft:the_nether", 2, 65, 3, "minecraft:dirt"));
    }

    private static void addMachine(String name) throws IOException {
        MachineStatusConfigManager.addMachine(name, "minecraft:overworld", 1, 64, -2, "minecraft:stone");
    }
}
