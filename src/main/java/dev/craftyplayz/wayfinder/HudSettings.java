package dev.craftyplayz.wayfinder;

public record HudSettings(boolean showDistance, boolean showLabels, boolean showOffscreen,
                          double maxDistance) {
    public static final double MAX_DISTANCE = 60_000_000;
    public static final HudSettings DEFAULTS = new HudSettings(true, true, true, 60_000_000);

    public HudSettings {
        if (!Double.isFinite(maxDistance) || maxDistance <= 0 || maxDistance > MAX_DISTANCE) {
            throw new IllegalArgumentException("Maximum distance must be positive and at most 60,000,000");
        }
    }
}
