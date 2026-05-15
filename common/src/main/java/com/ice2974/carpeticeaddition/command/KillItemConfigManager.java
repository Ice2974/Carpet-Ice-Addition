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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class KillItemConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KillItemConfigManager.class);
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final String MOD_DIRECTORY = "carpet-ice-addition";
    private static final String FILE_NAME = "killitem.json";

    private static Path configPath;
    private static State state = State.DEFAULT;

    private KillItemConfigManager() {
    }

    public static synchronized void initialize(Path saveRoot) {
        configPath = resolvePath(saveRoot);
        state = load(configPath);
    }

    public static synchronized void shutdown() {
        configPath = null;
        state = State.DEFAULT;
    }

    public static synchronized Snapshot snapshot() {
        ensureInitialized();
        return new Snapshot(
                Collections.unmodifiableSet(new LinkedHashSet<>(state.blacklist)),
                state.clearNamedItems
        );
    }

    public static synchronized boolean addBlacklistItem(String itemId) throws IOException {
        ensureInitialized();
        State updated = state.copy();
        boolean changed = updated.blacklist.add(normalizeItemId(itemId));
        if (!changed) {
            return false;
        }
        save(updated);
        state = updated;
        return true;
    }

    public static synchronized boolean removeBlacklistItem(String itemId) throws IOException {
        ensureInitialized();
        State updated = state.copy();
        boolean changed = updated.blacklist.remove(normalizeItemId(itemId));
        if (!changed) {
            return false;
        }
        save(updated);
        state = updated;
        return true;
    }

    public static synchronized boolean clearBlacklist() throws IOException {
        ensureInitialized();
        if (state.blacklist.isEmpty()) {
            return false;
        }
        State updated = state.copy();
        updated.blacklist.clear();
        save(updated);
        state = updated;
        return true;
    }

    public static synchronized boolean setClearNamedItems(boolean clearNamedItems) throws IOException {
        ensureInitialized();
        if (state.clearNamedItems == clearNamedItems) {
            return false;
        }
        State updated = state.copy();
        updated.clearNamedItems = clearNamedItems;
        save(updated);
        state = updated;
        return true;
    }

    public static Path resolvePath(Path saveRoot) {
        return saveRoot.resolve(MOD_DIRECTORY).resolve(FILE_NAME);
    }

    private static void ensureInitialized() {
        if (configPath == null) {
            throw new IllegalStateException("Killitem config manager has not been initialized");
        }
    }

    private static String normalizeItemId(String itemId) {
        return itemId.trim();
    }

    private static State load(Path path) {
        if (!Files.exists(path)) {
            return State.DEFAULT.copy();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ConfigFileData data = GSON.fromJson(reader, ConfigFileData.class);
            State loaded = State.DEFAULT.copy();
            if (data != null && data.blacklist != null) {
                for (String entry : data.blacklist) {
                    if (entry != null && !entry.isBlank()) {
                        loaded.blacklist.add(entry.trim());
                    }
                }
            }
            if (data != null) {
                loaded.clearNamedItems = data.clearNamedItems;
            }
            return loaded;
        } catch (Exception exception) {
            LOGGER.error("Failed to read killitem config from {}. Using defaults.", path, exception);
            return State.DEFAULT.copy();
        }
    }

    private static void save(State updatedState) throws IOException {
        if (configPath == null) {
            throw new IOException("Killitem config path has not been initialized");
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
            LOGGER.error("Failed to save killitem config to {}", configPath, exception);
            throw exception;
        }
    }

    public record Snapshot(Set<String> blacklist, boolean clearNamedItems) {
    }

    private static final class State {
        private static final State DEFAULT = new State(new LinkedHashSet<>(), false);

        private final LinkedHashSet<String> blacklist;
        private boolean clearNamedItems;

        private State(LinkedHashSet<String> blacklist, boolean clearNamedItems) {
            this.blacklist = blacklist;
            this.clearNamedItems = clearNamedItems;
        }

        private State copy() {
            return new State(new LinkedHashSet<>(this.blacklist), this.clearNamedItems);
        }
    }

    private static final class ConfigFileData {
        private List<String> blacklist = List.of();
        private boolean clearNamedItems;

        private ConfigFileData() {
        }

        private ConfigFileData(State state) {
            this.blacklist = List.copyOf(state.blacklist);
            this.clearNamedItems = state.clearNamedItems;
        }
    }
}
