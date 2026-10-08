# Wayfinder

A client-only **Minecraft Java 26.1.2 / Fabric** mod for fixed pitch/yaw direction markers.
Hold **H** to see markers while continuing to look around, move, and play normally.
Release H to hide them. The HUD does not open a screen or capture the mouse.
While holding H, looking within 5° of a marker snaps the camera to it and holds
it there for one second. Movement still works. After the hold, look at least
10° away from nearby markers before snapping again, or release and hold H again.
Releasing H, opening a menu, hiding the HUD, or losing focus cancels the hold.

## Installation

1. Install **Java 25** and Fabric Loader **0.19.3 or newer** for Minecraft **26.1.2**.
2. Install Fabric API **0.154.0+26.1.2 or newer** for Minecraft **26.1.2**.
3. Put `wayfinder-1.0.0.jar` from `build/libs` into your instance's `mods` folder.
   Do not install the `-sources.jar`.
4. Launch Minecraft and enter a world.

Works in singleplayer and on multiplayer servers. Servers do not need this mod;
it sends no custom packets and requires no server plugin or commands.

## Controls and markers

| Default key | Action |
| --- | --- |
| **H**, held | Show Directional Markers |
| **M**, pressed | Manage Directional Markers |

Both bindings appear under **Wayfinder** in **Options → Controls → Key Binds**.
They use Minecraft's normal key-binding system and can be rebound independently,
including to mouse buttons. H and M do not conflict with standard vanilla controls.
The overlay is hidden in menus, when the window loses focus, and with F1's hidden HUD.

Press M to open marker management. Select a marker, then **Edit**, **Remove**, or
**Enable/Disable** it. Removal requires confirmation. Use the page buttons for
long lists. Buttons and text fields support normal mouse and Tab/Enter navigation.

**Add Marker** opens an editor for name, pitch, yaw, and enabled state.
**Current View** fills the angles from the player's current look direction; edit
them before saving if desired. **Cancel** or Escape leaves the original unchanged.
Invalid names, non-finite angles, and out-of-range pitch are rejected with an error.

Angles use Minecraft's degrees, with decimals supported:
- **Pitch:** -90° looks straight up, 0° is level, +90° looks straight down.
- **Yaw:** 0° faces south, 90° west, ±180° north, and -90° east.
  Any finite yaw is accepted and wrapped into [-180°, 180°).

Markers point to absolute look directions, **not positions or offsets from your
current view**. Walking or changing dimensions does not change the target direction.
Turn until the marker's dot meets the center of your view to match the saved direction.
While holding H, the camera snaps to nearby target directions as described above.

## HUD

Markers are projected as fixed direction vectors using the actual camera orientation and
world projection's dynamic field of view and screen aspect ratio. This includes
vertical direction, yaw wrapping, and third-person cameras—not just compass yaw.
Visible targets have a small cyan dot and a readable backed label.
Targets outside the view have directional edge arrows. There are no block distances
or distance limits because markers have no world position.
An exactly rearward target chooses a rightward turn; either horizontal turn
would reach it.

Nearby labels are offset with leader lines. If the viewport is saturated,
directional icons remain visible rather than layering unreadable labels.
View bobbing, hurt shake, and nausea distortion are deliberately not applied
to the navigation overlay.

## Configuration and persistence

Data is saved in **`config/wayfinder.json`**, relative to the Minecraft instance.
Markers are local to the client and shared across worlds, servers, and dimensions.
Disable irrelevant markers when
changing worlds. No example markers are installed automatically.

Close Minecraft before manually editing the JSON. The file is loaded once at
startup; successful marker edits are saved immediately. Writes use a temporary
file and atomic replacement where supported. Failed saves leave the previous
in-memory list intact and display an error. Missing files mean an empty list.
Malformed files are not silently overwritten: marker management provides
explicit backup/recovery, preserving the original before saving recovered data.
Old X/Y/Z coordinate markers cannot be converted without a reference position.
They are reported as legacy entries and left untouched on disk until you explicitly
choose **Recover/Back Up**. Recovery backs up the original and keeps only valid angle
markers; recreate old targets with the desired pitch/yaw. Old distance settings
are ignored, while label and offscreen settings are retained.

Example:

```json
{
  "markers": [
    {
      "name": "Look southwest",
      "pitch": -15,
      "yaw": 45,
      "enabled": true
    }
  ],
  "settings": {
    "showLabels": true,
    "showOffscreen": true
  }
}
```

HUD settings are optional and edited in JSON. Marker scale is fixed to keep the
HUD simple; Minecraft's GUI Scale controls its displayed size.

## Build and tests

Install JDK **25**, set `JAVA_HOME` to that JDK, and run:

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Outputs go to `build/libs`.
The checked-in Gradle wrapper is generated by Gradle, and Loom uses Minecraft
26.1.2's official unobfuscated names; no legacy Yarn mappings are configured.
Initial builds require access to Gradle, Fabric Maven, Maven Central, and
Minecraft's download hosts.

`./gradlew coreTest` runs the dependency-light Java test runner for projection,
validation, persistence, and recovery; it is also part of `build`.
`./gradlew runClient` starts a development client when dependencies are available.

### CI and releases

`.github/workflows/build.yml` runs `./gradlew build` on JDK 25 for every push and
pull request and uploads the mod JAR as the `Wayfinder-build` artifact on the
run's summary page. Publishing a GitHub Release triggers
`.github/workflows/release.yml`, which rebuilds the tag and attaches
`wayfinder-<version>.jar` (version from `mod_version`) to the release.

### Verification status and in-game checklist

The implementation environment cannot resolve Fabric Maven or Minecraft's
download hosts. The Gradle wrapper itself ran, but the full mod build was blocked
at Loom dependency resolution. **Client compilation, a usable JAR, and in-game
behaviour have not been verified in this environment.** The standalone core
test runner passed **1,774 checks** on Java 25 using Gson 2.13.2, independently
of Minecraft.

Before release, run the build and this checklist in a real 26.1.2 client:

- Add pitch/yaw targets ahead, behind, left, right, above, below, and diagonally.
- Hold H while walking and looking through 360°, including up/down, screen
  edges, and the ±180° yaw boundary; release H and confirm immediate hiding.
- Test several coincident markers, different FOVs/GUI scales, and third-person view.
- Approach a marker with the crosshair while holding H; confirm a one-second snap,
  continued movement, free looking afterward, and rearming after looking away.
  Check targets above/below, both third-person views, and cancellation by releasing
  H, opening a menu, switching dimensions, hiding the HUD, and losing focus.
- Use Current View, edit/cancel, toggle enabled state, and confirm/cancel deletion.
- Walk long distances and switch dimensions; confirm the target direction stays fixed.
- Restart Minecraft and confirm persistence.
- Rebind both keys in Controls and check held versus pressed behaviour.
- Repeat in singleplayer and on an unmodified multiplayer server.
