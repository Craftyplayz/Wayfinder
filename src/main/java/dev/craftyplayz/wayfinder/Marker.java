package dev.craftyplayz.wayfinder;

import java.util.Objects;

public record Marker(String name, double pitch, double yaw, boolean enabled) {
    public static final int MAX_NAME_LENGTH = 64;

    public Marker {
        Objects.requireNonNull(name, "name");
        if (name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Marker names must not contain control characters");
        }
        name = name.strip();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Marker names must contain 1–64 characters");
        }
        if (!Double.isFinite(pitch) || pitch < -90 || pitch > 90) {
            throw new IllegalArgumentException("Pitch must be finite and within ±90 degrees");
        }
        if (!Double.isFinite(yaw)) {
            throw new IllegalArgumentException("Yaw must be finite");
        }
        yaw %= 360;
        if (yaw >= 180) yaw -= 360;
        if (yaw < -180) yaw += 360;
    }
}
