package com.maligpu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MaliGPUConfig {
    private static final Path FILE = Path.of("config", "maligpu.properties");

    public static final MaliGPUConfig INSTANCE = new MaliGPUConfig();

    // === Legacy Mali-GPU tuning (kept from 1.1.x) ===
    public boolean asyncOcclusionCulling = false;
    public int cullGridHalfExtent = 64;
    public int cullWorkerThreads = 2;
    public int cullRecomputeIntervalMs = 250;

    public boolean particleLimit = true;
    public int maxParticles = 400;
    public boolean particleDistanceCull = true;
    public double particleCullRadius = 48.0;

    public boolean aggressiveTickThrottle = false;
    public double tickBudgetSeconds = 0.008;

    public boolean autoTuneForBeryl = true;

    public boolean capAnimations = true;
    public boolean disableWeatherParticles = true;
    public boolean skipFarParticles = true;

    // === v1.2.0+: gap-fillers (Vulkan-safe, engine-side) ===
    public boolean liftAudioSoundCap = true;   // throttle new one-shot sounds/tick (audio-thread stall fix)
    public boolean dynamicFps = true;          // throttle FPS when unfocused/hidden
    public int dynamicFpsUnfocused = 1;
    public int dynamicFpsInvisible = 0;
    public boolean patchMemoryLeaks = true;    // force-release level/sound resources on disconnect

    // === v1.2.1: world-gen time budget ===
    // Caps /mali-load-world generation to N ms per server tick so the integrated loopback server
    // never spirals ("Can't keep up!"). Lower = more responsive while pre-genning.
    public long worldGenTickBudgetMs = 12L;

    // === v1.3.0: debug instrumentation + boot/cache ===
    // Deep debug logs. When true, the mod logs WHOLE-MINECRAFT instrumentation (resource reload
    // timing, GC pauses, sound spawn rate, loaded-chunk delta) at INFO, plus per-feature stats.
    // Off by default to keep the log clean; turn on to see what MC does right/wrong.
    public boolean debugLogging = false;

    // Apply all engine-tunables at game boot (Minecraft constructor) instead of waiting for the
    // first client tick. This is the "make changes at start" request; also writes a one-time
    // device calibration cache so the first-start cost is paid once, not every launch.
    public boolean applyAtBoot = true;

    // Resource-reload debounce (ms): when Minecraft fires a reload, wait this long and apply the
    // setup ONCE. Prevents the repeated full cache-clear + 16-atlas rebuild storm seen in logs
    // ("29877 caches cleared" x3). This is the caching we can own without touching VulkanMod.
    public long reloadDebounceMs = 1500L;

    // Count atlas rebuilds and log a warning when a reload triggers excessive rebuilds (sign of a
    // misbehaving resource pack or an unnecessary reload). Visibility-only; does not block reloads.
    public boolean trackAtlasRebuilds = true;

    private MaliGPUConfig() {
        load();
    }

    public void load() {
        try {
            if (!Files.exists(FILE)) {
                save();
                return;
            }
            Properties p = new Properties();
            try (var r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            asyncOcclusionCulling = bool(p, "asyncOcclusionCulling", asyncOcclusionCulling);
            cullGridHalfExtent = intv(p, "cullGridHalfExtent", cullGridHalfExtent);
            cullWorkerThreads = intv(p, "cullWorkerThreads", cullWorkerThreads);
            cullRecomputeIntervalMs = intv(p, "cullRecomputeIntervalMs", cullRecomputeIntervalMs);
            particleLimit = bool(p, "particleLimit", particleLimit);
            maxParticles = intv(p, "maxParticles", maxParticles);
            particleDistanceCull = bool(p, "particleDistanceCull", particleDistanceCull);
            particleCullRadius = Double.parseDouble(p.getProperty("particleCullRadius", Double.toString(particleCullRadius)));
            aggressiveTickThrottle = bool(p, "aggressiveTickThrottle", aggressiveTickThrottle);
            tickBudgetSeconds = Double.parseDouble(p.getProperty("tickBudgetSeconds", Double.toString(tickBudgetSeconds)));
            autoTuneForBeryl = bool(p, "autoTuneForBeryl", autoTuneForBeryl);
            capAnimations = bool(p, "capAnimations", capAnimations);
            disableWeatherParticles = bool(p, "disableWeatherParticles", disableWeatherParticles);
            skipFarParticles = bool(p, "skipFarParticles", skipFarParticles);

            liftAudioSoundCap = bool(p, "liftAudioSoundCap", liftAudioSoundCap);
            dynamicFps = bool(p, "dynamicFps", dynamicFps);
            dynamicFpsUnfocused = intv(p, "dynamicFpsUnfocused", dynamicFpsUnfocused);
            dynamicFpsInvisible = intv(p, "dynamicFpsInvisible", dynamicFpsInvisible);
            patchMemoryLeaks = bool(p, "patchMemoryLeaks", patchMemoryLeaks);
            worldGenTickBudgetMs = longv(p, "worldGenTickBudgetMs", worldGenTickBudgetMs);

            debugLogging = bool(p, "debugLogging", debugLogging);
            applyAtBoot = bool(p, "applyAtBoot", applyAtBoot);
            reloadDebounceMs = longv(p, "reloadDebounceMs", reloadDebounceMs);
            trackAtlasRebuilds = bool(p, "trackAtlasRebuilds", trackAtlasRebuilds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            if (!Files.exists(FILE.getParent())) {
                Files.createDirectories(FILE.getParent());
            }
            Properties p = new Properties();
            p.setProperty("asyncOcclusionCulling", Boolean.toString(asyncOcclusionCulling));
            p.setProperty("cullGridHalfExtent", Integer.toString(cullGridHalfExtent));
            p.setProperty("cullWorkerThreads", Integer.toString(cullWorkerThreads));
            p.setProperty("cullRecomputeIntervalMs", Integer.toString(cullRecomputeIntervalMs));
            p.setProperty("particleLimit", Boolean.toString(particleLimit));
            p.setProperty("maxParticles", Integer.toString(maxParticles));
            p.setProperty("particleDistanceCull", Boolean.toString(particleDistanceCull));
            p.setProperty("particleCullRadius", Double.toString(particleCullRadius));
            p.setProperty("aggressiveTickThrottle", Boolean.toString(aggressiveTickThrottle));
            p.setProperty("tickBudgetSeconds", Double.toString(tickBudgetSeconds));
            p.setProperty("autoTuneForBeryl", Boolean.toString(autoTuneForBeryl));
            p.setProperty("capAnimations", Boolean.toString(capAnimations));
            p.setProperty("disableWeatherParticles", Boolean.toString(disableWeatherParticles));
            p.setProperty("skipFarParticles", Boolean.toString(skipFarParticles));

            p.setProperty("liftAudioSoundCap", Boolean.toString(liftAudioSoundCap));
            p.setProperty("dynamicFps", Boolean.toString(dynamicFps));
            p.setProperty("dynamicFpsUnfocused", Integer.toString(dynamicFpsUnfocused));
            p.setProperty("dynamicFpsInvisible", Integer.toString(dynamicFpsInvisible));
            p.setProperty("patchMemoryLeaks", Boolean.toString(patchMemoryLeaks));
            p.setProperty("worldGenTickBudgetMs", Long.toString(worldGenTickBudgetMs));

            p.setProperty("debugLogging", Boolean.toString(debugLogging));
            p.setProperty("applyAtBoot", Boolean.toString(applyAtBoot));
            p.setProperty("reloadDebounceMs", Long.toString(reloadDebounceMs));
            p.setProperty("trackAtlasRebuilds", Boolean.toString(trackAtlasRebuilds));

            try (var w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                p.store(w, "MaliGPUOptimization config - Mali GPU (Helio G100 / G57 MC2) tuning for Minecraft 26.1.2");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean bool(Properties p, String k, boolean d) {
        return Boolean.parseBoolean(p.getProperty(k, Boolean.toString(d)));
    }

    private static int intv(Properties p, String k, int d) {
        try {
            return Integer.parseInt(p.getProperty(k, Integer.toString(d)));
        } catch (NumberFormatException e) {
            return d;
        }
    }

    private static long longv(Properties p, String k, long d) {
        try {
            return Long.parseLong(p.getProperty(k, Long.toString(d)));
        } catch (NumberFormatException e) {
            return d;
        }
    }
}
