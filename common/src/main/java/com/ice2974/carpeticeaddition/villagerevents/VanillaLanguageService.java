package com.ice2974.carpeticeaddition.villagerevents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Per-server vanilla-language loader. It deliberately owns no global cache or executor: callers
 * create one instance for one server session and must close it with that session.
 */
public final class VanillaLanguageService implements AutoCloseable {
    public enum State { LOADING, READY, FAILED }

    private static final URI VERSION_MANIFEST = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
    private static final int MAX_METADATA_BYTES = 8 * 1024 * 1024;
    private static final int MAX_LANGUAGE_BYTES = 4 * 1024 * 1024;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final String minecraftVersion;
    private final String locale;
    private final Path cacheFile;
    private final ClassLoader minecraftClassLoader;
    private final Logger logger;
    private final ExecutorService executor;
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile State state = State.LOADING;
    private volatile Map<String, String> translations = Map.of();
    private volatile Future<?> task;

    public VanillaLanguageService(String minecraftVersion, String carpetLanguage, Path cacheRoot,
                                  ClassLoader minecraftClassLoader, Logger logger) {
        this.minecraftVersion = requireVersion(minecraftVersion);
        this.locale = normalizeLocale(carpetLanguage);
        this.cacheFile = cacheRoot.resolve(this.minecraftVersion).resolve(this.locale + ".json");
        this.minecraftClassLoader = Objects.requireNonNull(minecraftClassLoader, "minecraftClassLoader");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CarpetIceAddition-VanillaLanguage");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static String normalizeLocale(String carpetLanguage) {
        String value = carpetLanguage == null ? "" : carpetLanguage.toLowerCase(java.util.Locale.ROOT);
        if (value.matches("zh(?:_[a-z0-9]{2,8})?")) return "zh_cn";
        return "en_us";
    }

    public State state() { return state; }
    public String locale() { return locale; }
    public Map<String, String> translations() { return translations; }

    public void start(Consumer<VanillaLanguageService> completion) {
        if (closed.get()) return;
        task = executor.submit(() -> load(completion));
    }

    private void load(Consumer<VanillaLanguageService> completion) {
        try {
            Map<String, String> loaded = loadCache();
            if (loaded == null && "en_us".equals(locale)) loaded = loadClasspathEnglish();
            if (loaded == null && "zh_cn".equals(locale)) loaded = downloadLanguage();
            if (loaded == null || loaded.isEmpty()) throw new IOException("No usable vanilla language resource");
            translations = Collections.unmodifiableMap(loaded);
            state = State.READY;
        } catch (Throwable throwable) {
            state = State.FAILED;
            if (!closed.get()) logger.warn("[VillagerEvents] Vanilla {} language resource is unavailable: {}", locale, throwable.toString());
        }
        if (!closed.get()) completion.accept(this);
    }

    private Map<String, String> loadCache() throws IOException {
        if (!Files.isRegularFile(cacheFile)) return null;
        byte[] bytes = Files.readAllBytes(cacheFile);
        if (bytes.length == 0 || bytes.length > MAX_LANGUAGE_BYTES) return null;
        return parseLanguage(bytes);
    }

    private Map<String, String> loadClasspathEnglish() throws IOException {
        try (InputStream stream = minecraftClassLoader.getResourceAsStream("assets/minecraft/lang/en_us.json")) {
            if (stream == null) return null;
            return parseLanguage(readLimited(stream, MAX_LANGUAGE_BYTES));
        }
    }

    private Map<String, String> downloadLanguage() throws Exception {
        JsonObject manifest = jsonObject(get(VERSION_MANIFEST, MAX_METADATA_BYTES));
        JsonObject version = null;
        for (JsonElement entry : manifest.getAsJsonArray("versions")) {
            JsonObject candidate = entry.getAsJsonObject();
            if (minecraftVersion.equals(candidate.get("id").getAsString())) {
                version = jsonObject(get(officialUri(candidate.get("url").getAsString()), MAX_METADATA_BYTES));
                break;
            }
        }
        if (version == null) throw new IOException("Minecraft version not in Mojang manifest: " + minecraftVersion);
        JsonObject assetIndex = version.getAsJsonObject("assetIndex");
        byte[] indexBytes = get(officialUri(assetIndex.get("url").getAsString()), MAX_METADATA_BYTES);
        verifySha1(indexBytes, assetIndex.get("sha1").getAsString());
        JsonObject objects = jsonObject(indexBytes).getAsJsonObject("objects");
        JsonObject object = objects.getAsJsonObject("minecraft/lang/" + locale + ".json");
        if (object == null) throw new IOException("Language object not present in asset index: " + locale);
        String hash = object.get("hash").getAsString();
        int size = object.get("size").getAsInt();
        if (!hash.matches("[0-9a-f]{40}") || size <= 0 || size > MAX_LANGUAGE_BYTES) throw new IOException("Invalid language asset metadata");
        URI assetUri = officialUri("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash);
        byte[] language = get(assetUri, MAX_LANGUAGE_BYTES);
        if (language.length != size) throw new IOException("Language object size mismatch");
        verifySha1(language, hash);
        Map<String, String> parsed = parseLanguage(language);
        writeCache(language);
        return parsed;
    }

    private byte[] get(URI uri, int limit) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).header("User-Agent", "Carpet-Ice-Addition/villagerEvents").GET().build();
        IOException failure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (closed.get()) throw new IOException("Language service closed");
            try {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
                try (InputStream body = response.body()) { return readLimited(body, limit); }
            } catch (IOException exception) {
                failure = exception;
                if (attempt < 2) Thread.sleep(attempt == 0 ? 1000L : 3000L);
            }
        }
        throw failure == null ? new IOException("Download failed") : failure;
    }

    private static URI officialUri(String value) throws IOException {
        URI uri = URI.create(value);
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || !(host.equals("piston-meta.mojang.com") || host.equals("resources.download.minecraft.net"))) {
            throw new IOException("Rejected non-official language URL");
        }
        return uri;
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        byte[] bytes = input.readAllBytes();
        if (bytes.length == 0 || bytes.length > limit) throw new IOException("Downloaded object exceeds limit");
        return bytes;
    }

    private static JsonObject jsonObject(byte[] bytes) throws IOException {
        try { return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (RuntimeException exception) { throw new IOException("Invalid JSON", exception); }
    }

    private static Map<String, String> parseLanguage(byte[] bytes) throws IOException {
        JsonObject object = jsonObject(bytes);
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) result.put(entry.getKey(), entry.getValue().getAsString());
        }
        if (result.isEmpty()) throw new IOException("Language JSON contains no strings");
        return result;
    }

    private void writeCache(byte[] bytes) throws IOException {
        Files.createDirectories(cacheFile.getParent());
        Path temporary = Files.createTempFile(cacheFile.getParent(), locale + ".", ".part");
        try {
            Files.write(temporary, bytes);
            try { Files.move(temporary, cacheFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static void verifySha1(byte[] bytes, String expected) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-1").digest(bytes)) builder.append(String.format("%02x", value));
        if (!builder.toString().equals(expected)) throw new IOException("SHA-1 mismatch");
    }

    private static String requireVersion(String value) {
        if (value == null || !value.matches("(?:1\\.21(?:\\.\\d+)?|26\\.[12](?:\\.\\d+)?)")) throw new IllegalArgumentException("Unsupported Minecraft version: " + value);
        return value;
    }

    @Override public void close() {
        if (closed.compareAndSet(false, true)) {
            Future<?> running = task;
            if (running != null) running.cancel(true);
            executor.shutdownNow();
            translations = Map.of();
        }
    }
}
