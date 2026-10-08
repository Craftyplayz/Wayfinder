package dev.craftyplayz.wayfinder;

import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Standalone runner: no Minecraft runtime or external test framework required. */
public final class CoreTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Logger.getLogger(MarkerManager.class.getName()).setLevel(Level.OFF);
        validation();
        projection();
        Files.createDirectories(Path.of("build"));
        Path directory = Files.createTempDirectory(Path.of("build"), "core-tests-");
        try {
            persistence(directory);
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        System.out.println("CoreTests: " + checks + " checks passed");
    }

    private static Marker marker(String name) {
        return new Marker(name, 1.25, -64, 30_000_000, true, "minecraft:overworld");
    }

    private static void validation() {
        check(marker("  Home  ").name().equals("Home"), "Names are trimmed");
        marker("a".repeat(64));
        for (String name : List.of("", " ", "a".repeat(65), "bad\nname", "\tname", "bad\u007fname")) {
            rejects(() -> marker(name));
        }
        rejects(() -> marker(null));
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                30_000_001, -30_000_001}) {
            rejects(() -> new Marker("X", value, 0, 0, true, "minecraft:overworld"));
            rejects(() -> new Marker("X", 0, value, 0, true, "minecraft:overworld"));
            rejects(() -> new Marker("X", 0, 0, value, true, "minecraft:overworld"));
        }
        new Marker("Boundary", -30_000_000, 30_000_000, -30_000_000, false, "my-mod:folder/dim_2");
        for (String dimension : List.of("", "overworld", ":overworld", "minecraft:", "Minecraft:overworld",
                "minecraft:Upper", "minecraft:has space", " minecraft:overworld", "a:b:c", "a:\nb")) {
            rejects(() -> new Marker("X", 0, 0, 0, true, dimension));
        }
        rejects(() -> new Marker("X", 0, 0, 0, true, null));
        check(HudSettings.DEFAULTS.showDistance() && HudSettings.DEFAULTS.showLabels()
                && HudSettings.DEFAULTS.showOffscreen(), "Visible defaults");
        check(HudSettings.DEFAULTS.maxDistance() >= 1_250, "Example village visible by default");
        for (double distance : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY, 60_000_001}) {
            rejects(() -> new HudSettings(true, true, true, distance));
        }
        new HudSettings(false, false, false, 60_000_000);
    }

    private static MarkerProjection.Point project(double x, double y, double z) {
        return MarkerProjection.project(x, y, z, 90, 200, 100, 10);
    }

    private static void projection() {
        var ahead = project(0, 0, -10);
        near(ahead.x(), 100);
        near(ahead.y(), 50);
        near(ahead.distance(), 10);
        check(!ahead.offscreen(), "Ahead is visible");
        near(project(1, 0, -10).x(), 105);
        near(project(-1, 0, -10).x(), 95);
        near(project(0, 1, -10).y(), 45);
        near(project(0, -1, -10).y(), 55);
        near(MarkerProjection.project(1, 0, -10, 90, 400, 100, 10).x(), 205);
        near(MarkerProjection.project(1, 0, -10, 60, 200, 100, 10).x(), 108.66025403784439);
        var behind = project(0, 0, 10);
        check(behind.offscreen(), "Behind is offscreen");
        near(behind.x(), 190);
        near(behind.y(), 50);
        near(behind.angle(), 0);
        near(project(1, 0, 10).x(), 190);
        near(project(-1, 0, 10).x(), 10);
        near(project(0, 1, 10).y(), 10);
        near(project(0, -1, 10).y(), 90);
        for (double z : new double[]{-1, 0, 1}) {
            var right = project(100, 0, z);
            check(right.offscreen(), "Right outside");
            near(right.x(), 190);
            near(right.y(), 50);
            near(project(-100, 0, z).x(), 10);
            near(project(0, 100, z).y(), 10);
            near(project(0, -100, z).y(), 90);
            var diagonal = project(100, 100, z);
            near(diagonal.x(), 140);
            near(diagonal.y(), 10);
            near(diagonal.angle(), -Math.PI / 4);
        }
        var far = project(0, 0, -30_000_000);
        check(!far.offscreen(), "Far ahead remains centered");
        near(far.distance(), 30_000_000);
        var zero = project(0, 0, 0);
        check(!zero.offscreen(), "Zero displacement stable");
        near(zero.x(), 100);
        near(zero.y(), 50);
        near(zero.distance(), 0);
        var nearPlane = project(1, 0, -Double.MIN_VALUE);
        check(nearPlane.offscreen(), "Tiny depth is handled without NaN");
        near(nearPlane.x(), 190);

        for (double degrees : new double[]{-720, -181, -180, -179, -90, 0, 90, 179, 180, 181, 720}) {
            double angle = Math.toRadians(degrees);
            double s = Math.sin(angle / 2);
            double c = Math.cos(angle / 2);
            var rotated = MarkerProjection.project(-Math.sin(angle) * 10, 0, -Math.cos(angle) * 10,
                    0, s, 0, c, 90, 200, 100, 10);
            check(!rotated.offscreen(), "Yaw orientation " + degrees);
            near(rotated.x(), 100);
            near(rotated.y(), 50);
            var scaled = MarkerProjection.project(-Math.sin(angle) * 10, 0, -Math.cos(angle) * 10,
                    0, s * 7, 0, c * 7, 90, 200, 100, 10);
            near(scaled.x(), rotated.x());
            near(scaled.y(), rotated.y());
        }
        for (double degrees : new double[]{-90, -45, 0, 45, 90}) {
            double angle = Math.toRadians(degrees);
            var rotated = MarkerProjection.project(0, Math.sin(angle) * 10, -Math.cos(angle) * 10,
                    Math.sin(angle / 2), 0, 0, Math.cos(angle / 2), 90, 200, 100, 10);
            check(!rotated.offscreen(), "Pitch orientation " + degrees);
            near(rotated.x(), 100);
            near(rotated.y(), 50);
        }
        var roll = MarkerProjection.project(0, 1, -10, 0, 0, Math.sqrt(0.5), Math.sqrt(0.5),
                90, 200, 100, 10);
        near(roll.x(), 105);
        near(roll.y(), 50);
        Random random = new Random(42);
        for (int i = 0; i < 250; i++) {
            double qx = random.nextDouble() - 0.5;
            double qy = random.nextDouble() - 0.5;
            double qz = random.nextDouble() - 0.5;
            double qw = random.nextDouble() - 0.5;
            double norm = Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
            qx /= norm;
            qy /= norm;
            qz /= norm;
            qw /= norm;
            double x = random.nextDouble() * 200 - 100;
            double y = random.nextDouble() * 200 - 100;
            double z = random.nextDouble() * 200 - 100;
            // Independent forward rotation using cross products, then exercise the inverse.
            double tx = 2 * (qy * z - qz * y);
            double ty = 2 * (qz * x - qx * z);
            double tz = 2 * (qx * y - qy * x);
            double wx = x + qw * tx + qy * tz - qz * ty;
            double wy = y + qw * ty + qz * tx - qx * tz;
            double wz = z + qw * tz + qx * ty - qy * tx;
            var expected = project(x, y, z);
            var actual = MarkerProjection.project(wx, wy, wz, qx, qy, qz, qw, 90, 200, 100, 10);
            near(actual.x(), expected.x());
            near(actual.y(), expected.y());
            near(actual.distance(), expected.distance());
            near(actual.angle(), expected.angle());
            check(actual.offscreen() == expected.offscreen(), "Arbitrary quaternion classification");
        }
        rejects(() -> MarkerProjection.project(0, 0, -1, 0, 0, 0, 0, 90, 200, 100, 10));
        rejects(() -> MarkerProjection.project(0, 0, -1, Double.NaN, 0, 0, 1, 90, 200, 100, 10));
        for (double fov : new double[]{0, 180, -1, Double.NaN}) {
            rejects(() -> MarkerProjection.project(0, 0, -1, fov, 200, 100, 10));
        }
        rejects(() -> MarkerProjection.project(0, 0, -1, 90, 0, 100, 10));
        rejects(() -> MarkerProjection.project(0, 0, -1, 90, 200, 100, 50));
        rejects(() -> MarkerProjection.project(0, 0, -1, 90, 200, 100, -1));
        rejects(() -> project(Double.NaN, 0, -1));
        rejects(() -> project(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
    }

    private static void persistence(Path directory) throws IOException {
        Path file = directory.resolve("nested/markers.json");
        MarkerManager manager = new MarkerManager(file);
        check(manager.lastError() == null && manager.markers().isEmpty(), "Missing file starts empty");
        check(!Files.exists(file), "Load does not create missing file");
        check(manager.add(marker("Home")), "Add saves");
        List<Marker> snapshot = manager.markers();
        rejects(() -> snapshot.add(marker("Forbidden")));
        Marker original = snapshot.getFirst();
        Marker canceledEdit = new Marker("Canceled", 2, 3, 4, false, original.dimension());
        check(manager.markers().getFirst().equals(original) && !canceledEdit.equals(original), "Canceled edit immutable");
        check(manager.add(marker("Second")), "Second add");
        check(snapshot.size() == 1, "Snapshots do not change");
        check(manager.toggle(0) && !manager.markers().getFirst().enabled(), "Toggle");
        check(manager.update(1, marker("Edited")), "Update");
        check(manager.remove(0), "Remove");
        HudSettings settings = new HudSettings(false, true, false, 123.5);
        check(manager.updateSettings(settings), "Settings save");
        check(Files.readString(file).contains("\n  \"markers\""), "Pretty JSON");
        JsonParser.parseString(Files.readString(file));
        MarkerManager reloaded = new MarkerManager(file);
        check(reloaded.markers().equals(manager.markers()), "Round trip markers");
        check(reloaded.settings().equals(settings), "Round trip settings");
        String saved = Files.readString(file);
        for (int index : new int[]{-1, 1, Integer.MAX_VALUE}) {
            check(!manager.remove(index) && !manager.update(index, marker("Bad")) && !manager.toggle(index),
                    "Invalid index rejected");
        }
        check(!manager.add(null) && !manager.update(0, null) && !manager.updateSettings(null), "Null mutations rejected");
        check(Files.readString(file).equals(saved), "Invalid edits do not write");
        check(manager.save() && manager.lastError() == null, "Successful save clears error");

        Files.writeString(file, "{broken");
        check(!manager.load() && manager.lastError() != null, "Malformed load reported");
        check(manager.markers().equals(reloaded.markers()), "Failed reload retains snapshot");
        check(!manager.add(marker("Do not overwrite")), "Malformed file blocks implicit save");
        check(Files.readString(file).equals("{broken"), "Malformed original preserved");
        check(manager.save(), "Explicit recovery permitted");
        try (var backups = Files.list(file.getParent())) {
            List<Path> recovered = backups.filter(path -> path.getFileName().toString().contains(".corrupt-")).toList();
            check(recovered.size() == 1 && Files.readString(recovered.getFirst()).equals("{broken"), "Corrupt backup");
        }
        String validEntry = """
                {"name":"Valid","x":0,"y":0,"z":0,"enabled":true,"dimension":"minecraft:overworld"}
                """;
        String partial = "{\"markers\":[" + validEntry + ",{\"name\":\"Invalid\"}]}";
        Files.writeString(file, partial);
        MarkerManager partialManager = new MarkerManager(file);
        check(partialManager.markers().size() == 1 && partialManager.lastError() != null, "Invalid entry skipped visibly");
        check(!partialManager.toggle(0), "Partial load blocks accidental data loss");
        check(Files.readString(file).equals(partial), "Invalid entries stay on disk");
        check(partialManager.save() && partialManager.toggle(0), "Explicit partial recovery unlocks edits");
        for (String bad : List.of("", "null", "[]", "{}", "{\"markers\":{}}", "{\"markers\":[]} trailing",
                "{\"markers\":[ " + validEntry.replace("\"enabled\":true", "\"enabled\":\"true\"") + "]}",
                "{\"markers\":[],\"settings\":{\"showDistance\":true}}",
                "{\"markers\":[" + validEntry.replace("\"x\":0", "\"x\":1e1000") + "]}",
                "{\"markers\":[" + validEntry.replace("\"y\":0", "\"y\":\"0\"") + "]}")) {
            Files.writeString(file, bad);
            MarkerManager invalid = new MarkerManager(file);
            check(invalid.lastError() != null, "Invalid JSON or schema diagnosed");
            check(!invalid.add(marker("No")) && Files.readString(file).equals(bad), "Bad data never silently replaced");
        }

        Path blocker = directory.resolve("blocked");
        MarkerManager blocked = new MarkerManager(blocker.resolve("markers.json"));
        Files.writeString(blocker, "not a directory");
        check(!blocked.add(marker("Unwritten")), "I/O failure returned");
        check(blocked.markers().isEmpty() && blocked.lastError() != null, "I/O failure is transactional");
        check(!blocked.updateSettings(settings) && blocked.settings().equals(HudSettings.DEFAULTS),
                "Settings I/O failure transactional");
        Files.delete(blocker);
        check(blocked.add(marker("Now writable")) && blocked.lastError() == null, "Retry after I/O failure");
        Path failedMove = directory.resolve("move.json");
        MarkerManager move = new MarkerManager(failedMove);
        Files.createDirectory(failedMove);
        Files.writeString(failedMove.resolve("child"), "keep");
        check(!move.add(marker("Not saved")) && move.markers().isEmpty(), "Failed replacement is transactional");
        check(Files.readString(failedMove.resolve("child")).equals("keep"), "Failed replacement retains original");
        try (var files = Files.list(directory)) {
            check(files.noneMatch(path -> path.getFileName().toString().endsWith(".new")), "Temporary files cleaned");
        }
    }

    private static void near(double actual, double expected) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) < 1e-7,
                "Expected " + expected + ", got " + actual);
    }

    private static void rejects(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException | NullPointerException | UnsupportedOperationException expected) {
            checks++;
            return;
        }
        throw new AssertionError("Expected validation to reject input");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
