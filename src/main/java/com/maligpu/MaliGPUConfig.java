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
    // NOTE: under VulkanMod (Vulkan renderer) the entity-render hook this system fed is replaced,
    // so occlusion culling is a passive profiler only. Off by default on Vulkan to avoid a
    // useless background thread. Re-enable only on the OpenGL/Krypton path where the hook applies.
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

    // === v1.2.0: All-in-one gaps the rest of the modpack does not cover ===

    // Audio engine: remove the vanilla 247-simultaneous-sound hard cap that stalls the
    // OpenAL audio thread under heavy sound load (e.g. rain + many entities). Engine-side,
    // Vulkan-safe.
    public boolean liftAudioSoundCap = true;

    // Dynamic FPS: when the game window is unfocused / not visible, throttle the client to a
    // near-idle frame rate so background CPU/GPU/battery are conserved. FPSDisplay only READS
    // fps; nothing in the pack actually throttles it.
    public boolean dynamicFps = true;
    public int dynamicFpsUnfocused = 1;   // fps cap applied while not focused
    public int dynamicFpsInvisible = 0;   // fps cap applied while window hidden (0 = pause render)

    // Adaptive distance scaling: sample average client FPS and step render/simulation distance
    // down when fps is too low, restore when there is headroom. No mod in the pack does this
    // dynamically (all distances are static).
    public boolean adaptiveDistance = true;
    public double adaptiveFloorFps = 28.0;     // drop a distance step below this average
    public double adaptiveCeilFps = 50.0;      // restore a distance step above this average
    public int adaptiveMinDistance = 4;        // never go below this (render & sim)
    public int adaptiveSampleSeconds = 5;      // averaging window

    // Graphics auto-preset applied on first run (then user may override in settings).
    public boolean graphicsAutoPreset = true;
    public boolean graphicsPresetApplied = false; // internal flag, becomes true after first apply

    // Vanilla memory-leak patches (Debugify-style safe subset). These target known vanilla
    // resource leaks; they do not overlap with FerriteCore (which optimizes data structures,
    // not leaks). New in 1.2.0.
    public boolean patchMemoryLeaks = true;

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
            adaptiveDistance = bool(p, "adaptiveDistance", adaptiveDistance);
            adaptiveFloorFps = Double.parseDouble(p.getProperty("adaptiveFloorFps", Double.toString(adaptiveFloorFps)));
            adaptiveCeilFps = Double.parseDouble(p.getProperty("adaptiveCeilFps", Double.toString(adaptiveCeilFps)));
            adaptiveMinDistance = intv(p, "adaptiveMinDistance", adaptiveMinDistance);
            adaptiveSampleSeconds = intv(p, "adaptiveSampleSeconds", adaptiveSampleSeconds);
            graphicsAutoPreset = bool(p, "graphicsAutoPreset", graphicsAutoPreset);
            graphicsPresetApplied = bool(p, "graphicsPresetApplied", graphicsPresetApplied);
            patchMemoryLeaks = bool(p, "patchMemoryLeaks", patchMemoryLeaks);
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
            p.setProperty("adaptiveDistance", Boolean.toString(adaptiveDistance));
            p.setProperty("adaptiveFloorFps", Double.toString(adaptiveFloorFps));
            p.setProperty("adaptiveCeilFps", Double.toString(adaptiveCeilFps));
            p.setProperty("adaptiveMinDistance", Integer.toString(adaptiveMinDistance));
            p.setProperty("adaptiveSampleSeconds", Integer.toString(adaptiveSampleSeconds));
            p.setProperty("graphicsAutoPreset", Boolean.toString(graphicsAutoPreset));
            p.setProperty("graphicsPresetApplied", Boolean.toString(graphicsPresetApplied));
            p.setProperty("patchMemoryLeaks", Boolean.toString(patchMemoryLeaks));

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
}
