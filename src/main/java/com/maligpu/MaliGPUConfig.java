package com.maligpu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MaliGPUConfig {
    private static final Path FILE = Path.of("config", "maligpu.properties");

    public static final MaliGPUConfig INSTANCE = new MaliGPUConfig();

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
