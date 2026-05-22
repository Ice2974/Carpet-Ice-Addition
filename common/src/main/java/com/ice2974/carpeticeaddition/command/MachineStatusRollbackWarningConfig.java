package com.ice2974.carpeticeaddition.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MachineStatusRollbackWarningConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MachineStatusRollbackWarningConfig.class);
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final String MOD_DIRECTORY = "carpet-ice-addition";
    private static final String FILE_NAME = "machine_status_rollback_warning.json";
    private static final List<String> DEFAULT_PATTERNS = List.of(
            "^/?qb\\s+(back|restore)\\b.*",
            "^/?quickbackupmulti\\s+(back|restore)\\b.*",
            "^!!qb\\s+back\\b.*",
            "^!!pb\\s+back\\b.*",
            "^!!cb\\s+back\\b.*"
    );

    private static boolean initialized;
    private static Path configPath;
    private static State state = State.DEFAULT;

    private MachineStatusRollbackWarningConfig() {
    }

    public static Snapshot snapshot() {
        ensureInitialized();
        synchronized (MachineStatusRollbackWarningConfig.class) {
            return new Snapshot(state.rollbackCommandPatterns, state.compiledRollbackCommandPatterns);
        }
    }

    public static Path resolvePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_DIRECTORY).resolve(FILE_NAME);
    }

    private static void ensureInitialized() {
        synchronized (MachineStatusRollbackWarningConfig.class) {
            if (initialized) {
                return;
            }

            configPath = resolvePath();
            state = load(configPath);
            initialized = true;
        }
    }

    private static State load(Path path) {
        if (!Files.exists(path)) {
            State defaultState = createState(DEFAULT_PATTERNS, path);
            saveQuietly(defaultState);
            return defaultState;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            ConfigFileData data = GSON.fromJson(reader, ConfigFileData.class);
            if (data == null || data.rollbackCommandPatterns == null) {
                return State.DEFAULT;
            }
            return createState(data.rollbackCommandPatterns, path);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read machine status rollback warning config from {}. Using default patterns.", path, exception);
            return State.DEFAULT;
        }
    }

    private static State createState(List<String> configuredPatterns, Path sourcePath) {
        LinkedHashSet<String> normalizedPatterns = new LinkedHashSet<>();
        List<Pattern> compiledPatterns = new ArrayList<>();
        for (String configuredPattern : configuredPatterns) {
            if (configuredPattern == null) {
                continue;
            }

            String normalizedPattern = configuredPattern.trim();
            if (normalizedPattern.isEmpty() || !normalizedPatterns.add(normalizedPattern)) {
                continue;
            }

            try {
                compiledPatterns.add(Pattern.compile(normalizedPattern));
            } catch (PatternSyntaxException exception) {
                LOGGER.warn(
                        "Skipping invalid rollback command pattern '{}' in {}: {}",
                        normalizedPattern,
                        sourcePath,
                        exception.getMessage()
                );
            }
        }

        return new State(List.copyOf(normalizedPatterns), List.copyOf(compiledPatterns));
    }

    private static void saveQuietly(State updatedState) {
        try {
            save(updatedState);
        } catch (IOException exception) {
            LOGGER.warn("Failed to save default machine status rollback warning config to {}", configPath, exception);
        }
    }

    private static void save(State updatedState) throws IOException {
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
    }

    public record Snapshot(List<String> rollbackCommandPatterns, List<Pattern> compiledRollbackCommandPatterns) {
    }

    private static final class State {
        private static final State DEFAULT = new State(
                DEFAULT_PATTERNS,
                DEFAULT_PATTERNS.stream().map(Pattern::compile).toList()
        );

        private final List<String> rollbackCommandPatterns;
        private final List<Pattern> compiledRollbackCommandPatterns;

        private State(List<String> rollbackCommandPatterns, List<Pattern> compiledRollbackCommandPatterns) {
            this.rollbackCommandPatterns = rollbackCommandPatterns;
            this.compiledRollbackCommandPatterns = compiledRollbackCommandPatterns;
        }
    }

    private static final class ConfigFileData {
        private List<String> rollbackCommandPatterns = List.of();

        private ConfigFileData() {
        }

        private ConfigFileData(State state) {
            this.rollbackCommandPatterns = state.rollbackCommandPatterns;
        }
    }
}
