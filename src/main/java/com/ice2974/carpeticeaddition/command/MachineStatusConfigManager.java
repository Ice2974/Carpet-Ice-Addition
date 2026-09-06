package com.ice2974.carpeticeaddition.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class MachineStatusConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineStatusConfigManager.class);
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final String MOD_DIRECTORY = "carpet-ice-addition";
    private static final String FILE_NAME = "machine_status.json";

    private static Path configPath;
    private static State state = State.DEFAULT;

    private MachineStatusConfigManager() {
    }

    public static synchronized void initialize(Path saveRoot) {
        configPath = resolvePath(saveRoot);
        state = load(configPath);
    }

    public static synchronized void shutdown() {
        configPath = null;
        state = State.DEFAULT;
    }

    public static synchronized List<MachineRecord> snapshot() {
        ensureInitialized();
        return List.copyOf(state.machines.values());
    }

    public static synchronized MachineRecord getMachine(String name) {
        ensureInitialized();
        return state.machines.get(name);
    }

    public static synchronized boolean containsMachine(String name) {
        ensureInitialized();
        return state.machines.containsKey(name);
    }

    public static synchronized void addMachine(
            String name,
            String dimension,
            int x,
            int y,
            int z,
            String shutdownBlockState
    ) throws IOException {
        ensureInitialized();
        State updated = state.copy();
        updated.machines.put(name, new MachineRecord(name, dimension, x, y, z, shutdownBlockState));
        save(updated);
        state = updated;
    }

    public static synchronized boolean removeMachine(String name) throws IOException {
        ensureInitialized();
        if (!state.machines.containsKey(name)) {
            return false;
        }
        State updated = state.copy();
        updated.machines.remove(name);
        save(updated);
        state = updated;
        return true;
    }

    public static synchronized MachineRecord renameMachine(String name, String newName) throws IOException {
        ensureInitialized();
        MachineRecord existing = state.machines.get(name);
        if (existing == null) {
            return null;
        }

        State updated = state.copy();
        updated.machines.remove(name);
        MachineRecord renamed = new MachineRecord(newName, existing.dimension(), existing.x(), existing.y(), existing.z(), existing.shutdownBlockState());
        updated.machines.put(newName, renamed);
        save(updated);
        state = updated;
        return renamed;
    }

    public static synchronized MachineRecord updateMachineState(String name, String shutdownBlockState) throws IOException {
        ensureInitialized();
        MachineRecord existing = state.machines.get(name);
        if (existing == null) {
            return null;
        }

        State updated = state.copy();
        MachineRecord replacement = new MachineRecord(
                existing.name(),
                existing.dimension(),
                existing.x(),
                existing.y(),
                existing.z(),
                shutdownBlockState
        );
        updated.machines.put(name, replacement);
        save(updated);
        state = updated;
        return replacement;
    }

    public static synchronized MachineRecord moveMachine(
            String name,
            String dimension,
            int x,
            int y,
            int z,
            String shutdownBlockState
    ) throws IOException {
        ensureInitialized();
        MachineRecord existing = state.machines.get(name);
        if (existing == null) {
            return null;
        }

        State updated = state.copy();
        MachineRecord replacement = new MachineRecord(existing.name(), dimension, x, y, z, shutdownBlockState);
        updated.machines.put(name, replacement);
        save(updated);
        state = updated;
        return replacement;
    }

    public static Path resolvePath(Path saveRoot) {
        return saveRoot.resolve(MOD_DIRECTORY).resolve(FILE_NAME);
    }

    private static void ensureInitialized() {
        if (configPath == null) {
            throw new IllegalStateException("Machine status config manager has not been initialized");
        }
    }

    private static State load(Path path) {
        if (!Files.exists(path)) {
            return State.DEFAULT.copy();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ConfigFileData data = GSON.fromJson(reader, ConfigFileData.class);
            State loaded = State.DEFAULT.copy();
            if (data != null && data.machines != null) {
                for (MachineRecordData recordData : data.machines) {
                    if (recordData == null || recordData.name == null || recordData.name.isBlank()) {
                        continue;
                    }
                    if (loaded.machines.containsKey(recordData.name)) {
                        LOGGER.warn("Duplicate machine status entry '{}' found in {}. Keeping the first one.", recordData.name, path);
                        continue;
                    }
                    loaded.machines.put(recordData.name, new MachineRecord(
                            recordData.name,
                            recordData.dimension == null ? "" : recordData.dimension,
                            recordData.x,
                            recordData.y,
                            recordData.z,
                            recordData.shutdownBlockState == null ? "" : recordData.shutdownBlockState
                    ));
                }
            }
            return loaded;
        } catch (Exception exception) {
            LOGGER.error("Failed to read machine status config from {}. Using defaults.", path, exception);
            return State.DEFAULT.copy();
        }
    }

    private static void save(State updatedState) throws IOException {
        if (configPath == null) {
            throw new IOException("Machine status config path has not been initialized");
        }

        try {
            Files.createDirectories(configPath.getParent());
            Path tempFile = configPath.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(
                    tempFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                GSON.toJson(new ConfigFileData(updatedState), writer);
            }
            try {
                Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveException) {
                Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to save machine status config to {}", configPath, exception);
            throw exception;
        }
    }

    public record MachineRecord(String name, String dimension, int x, int y, int z, String shutdownBlockState) {
    }

    private static final class State {
        private static final State DEFAULT = new State(new LinkedHashMap<>());

        private final LinkedHashMap<String, MachineRecord> machines;

        private State(LinkedHashMap<String, MachineRecord> machines) {
            this.machines = machines;
        }

        private State copy() {
            return new State(new LinkedHashMap<>(this.machines));
        }
    }

    private static final class ConfigFileData {
        private List<MachineRecordData> machines = List.of();

        private ConfigFileData() {
        }

        private ConfigFileData(State state) {
            this.machines = new ArrayList<>(state.machines.size());
            for (MachineRecord record : state.machines.values()) {
                this.machines.add(new MachineRecordData(record));
            }
        }
    }

    private static final class MachineRecordData {
        private String name;
        private String dimension;
        private int x;
        private int y;
        private int z;
        private String shutdownBlockState;

        private MachineRecordData() {
        }

        private MachineRecordData(MachineRecord record) {
            this.name = record.name();
            this.dimension = record.dimension();
            this.x = record.x();
            this.y = record.y();
            this.z = record.z();
            this.shutdownBlockState = record.shutdownBlockState();
        }
    }
}
