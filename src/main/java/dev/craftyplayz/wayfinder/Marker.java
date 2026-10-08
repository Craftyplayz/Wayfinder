package dev.craftyplayz.wayfinder;

import java.util.Objects;
import java.util.regex.Pattern;

public record Marker(String name, double x, double y, double z, boolean enabled, String dimension) {
    public static final double MAX_COORDINATE = 30_000_000;
    public static final int MAX_NAME_LENGTH = 64;
    private static final Pattern DIMENSION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public Marker {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dimension, "dimension");
        if (name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Marker names must not contain control characters");
        }
        name = name.strip();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Marker names must contain 1–64 characters");
        }
        validateCoordinate(x);
        validateCoordinate(y);
        validateCoordinate(z);
        if (!DIMENSION.matcher(dimension).matches()) {
            throw new IllegalArgumentException("Dimension must be a lowercase namespace:path identifier");
        }
    }

    private static void validateCoordinate(double value) {
        if (!Double.isFinite(value) || Math.abs(value) > MAX_COORDINATE) {
            throw new IllegalArgumentException("Coordinates must be finite and within ±30,000,000");
        }
    }
}
