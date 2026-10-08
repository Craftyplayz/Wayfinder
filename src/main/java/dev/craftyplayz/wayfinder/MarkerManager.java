package dev.craftyplayz.wayfinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MarkerManager {
    private static final Logger LOGGER = Logger.getLogger(MarkerManager.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .setStrictness(Strictness.STRICT).create();
    private final Path file;
    private List<Marker> markers = List.of();
    private HudSettings settings = HudSettings.DEFAULTS;
    private String lastError;
    private boolean needsRecovery;

    /** The path is a JSON file, not a directory. Loading never modifies that file. */
    public MarkerManager(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        load();
    }

    public synchronized List<Marker> markers() { return markers; }
    public synchronized HudSettings settings() { return settings; }
    /** Null after success; otherwise a user-displayable validation or I/O error. */
    public synchronized String lastError() { return lastError; }

    public synchronized boolean load() {
        try {
            if (Files.notExists(file)) {
                markers = List.of();
                settings = HudSettings.DEFAULTS;
                needsRecovery = false;
                lastError = null;
                return true;
            }
            JsonElement parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonElement.class);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IllegalArgumentException("Expected a JSON object");
            }
            JsonObject root = parsed.getAsJsonObject();
            JsonElement entries = root.get("markers");
            if (entries == null || !entries.isJsonArray()) {
                throw new IllegalArgumentException("Expected a markers array");
            }
            List<Marker> loaded = new ArrayList<>();
            int invalid = 0;
            for (JsonElement entry : entries.getAsJsonArray()) {
                try {
                    JsonObject object = entry.getAsJsonObject();
                    loaded.add(new Marker(string(object, "name"), number(object, "x"),
                            number(object, "y"), number(object, "z"), bool(object, "enabled"),
                            string(object, "dimension")));
                } catch (RuntimeException ex) {
                    invalid++;
                    LOGGER.log(Level.WARNING, "Skipping invalid marker entry " + loaded.size(), ex);
                }
            }
            HudSettings loadedSettings = HudSettings.DEFAULTS;
            if (root.has("settings")) {
                try {
                    JsonObject object = root.getAsJsonObject("settings");
                    loadedSettings = new HudSettings(bool(object, "showDistance"), bool(object, "showLabels"),
                            bool(object, "showOffscreen"), number(object, "maxDistance"));
                } catch (RuntimeException ex) {
                    invalid++;
                    LOGGER.log(Level.WARNING, "Invalid HUD settings; using defaults", ex);
                }
            }
            markers = List.copyOf(loaded);
            settings = loadedSettings;
            needsRecovery = invalid > 0;
            if (needsRecovery) {
                return fail("Skipped " + invalid + " invalid entries. Explicitly save to back up and recover the file.", null);
            }
            lastError = null;
            return true;
        } catch (IOException | RuntimeException ex) {
            needsRecovery = true;
            return fail("Could not load markers: " + ex.getMessage(), ex);
        }
    }

    /** Explicit recovery consent: preserves an unreadable/partially invalid original in a backup. */
    public synchronized boolean save() {
        return persist(markers, settings, true);
    }

    public synchronized boolean add(Marker marker) {
        if (marker == null) return fail("Marker must not be null", null);
        List<Marker> next = new ArrayList<>(markers);
        next.add(marker);
        return persist(next, settings, false);
    }

    public synchronized boolean update(int index, Marker marker) {
        if (!validIndex(index)) return false;
        if (marker == null) return fail("Marker must not be null", null);
        List<Marker> next = new ArrayList<>(markers);
        next.set(index, marker);
        return persist(next, settings, false);
    }

    public synchronized boolean remove(int index) {
        if (!validIndex(index)) return false;
        List<Marker> next = new ArrayList<>(markers);
        next.remove(index);
        return persist(next, settings, false);
    }

    public synchronized boolean toggle(int index) {
        if (!validIndex(index)) return false;
        Marker m = markers.get(index);
        return update(index, new Marker(m.name(), m.x(), m.y(), m.z(), !m.enabled(), m.dimension()));
    }

    public synchronized boolean updateSettings(HudSettings value) {
        if (value == null) return fail("Settings must not be null", null);
        return persist(markers, value, false);
    }

    private boolean validIndex(int index) {
        return index >= 0 && index < markers.size() || fail("Marker index is out of range", null);
    }

    private boolean persist(List<Marker> next, HudSettings nextSettings, boolean explicitRecovery) {
        if (needsRecovery && !explicitRecovery) {
            return fail("File contains invalid data. Explicitly save to back up and recover it before editing.", null);
        }
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            if (needsRecovery && Files.exists(file)) {
                Path backup = Files.createTempFile(file.getParent(), file.getFileName() + ".corrupt-", ".json");
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            JsonObject root = new JsonObject();
            root.add("markers", GSON.toJsonTree(next));
            root.add("settings", GSON.toJsonTree(nextSettings));
            temporary = Files.createTempFile(file.getParent(), file.getFileName() + ".", ".new");
            Files.writeString(temporary, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            markers = List.copyOf(next);
            settings = nextSettings;
            needsRecovery = false;
            lastError = null;
            return true;
        } catch (IOException | RuntimeException ex) {
            return fail("Could not save markers: " + ex.getMessage(), ex);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException ex) {
                    LOGGER.log(Level.WARNING, "Could not remove temporary marker file", ex);
                }
            }
        }
    }

    private boolean fail(String message, Exception cause) {
        lastError = message;
        if (cause != null) LOGGER.log(Level.WARNING, message, cause);
        return false;
    }

    private static JsonPrimitive primitive(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing or invalid " + key);
        }
        return value.getAsJsonPrimitive();
    }

    private static String string(JsonObject object, String key) {
        JsonPrimitive value = primitive(object, key);
        if (!value.isString()) throw new IllegalArgumentException(key + " must be a string");
        return value.getAsString();
    }

    private static double number(JsonObject object, String key) {
        JsonPrimitive value = primitive(object, key);
        if (!value.isNumber()) throw new IllegalArgumentException(key + " must be a number");
        return value.getAsDouble();
    }

    private static boolean bool(JsonObject object, String key) {
        JsonPrimitive value = primitive(object, key);
        if (!value.isBoolean()) throw new IllegalArgumentException(key + " must be a boolean");
        return value.getAsBoolean();
    }
}
