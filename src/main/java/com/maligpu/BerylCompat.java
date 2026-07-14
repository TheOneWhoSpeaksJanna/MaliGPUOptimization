package com.maligpu;

import net.fabricmc.loader.api.FabricLoader;

public final class BerylCompat {
    private static final String[] CANDIDATE_IDS = {"beryl", "berylshaders", "vulkan-beryl", "berylshader", "beryl-shaders"};

    public static String detectModId() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            for (String id : CANDIDATE_IDS) {
                if (loader.isModLoaded(id)) {
                    return id;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean isPresent() {
        return detectModId() != null;
    }

    public static void applyAutoTune() {
        MaliGPUConfig cfg = MaliGPUConfig.INSTANCE;
        if (!cfg.autoTuneForBeryl) return;
        // Occlusion culling hooks the OpenGL entity-render path, which VulkanMod replaces.
        // Never enable it under Vulkan - MaliGPUMod.onInitialize force-disables it there anyway,
        // but we must not re-enable it (and persist it) from auto-tune.
        boolean vulkan = FabricLoader.getInstance().isModLoaded("vulkanmod");
        if (!vulkan) cfg.asyncOcclusionCulling = true;
        cfg.particleLimit = true;
        if (cfg.maxParticles > 250) cfg.maxParticles = 250;
        cfg.capAnimations = true;
        cfg.disableWeatherParticles = true;
        cfg.skipFarParticles = true;
        cfg.save();
        MaliGPUMod.LOGGER.info("[MaliGPUOptimization] Beryl auto-tune applied: particle cap={}, animations/weather capped{}",
                cfg.maxParticles, vulkan ? " (occlusion left off - VulkanMod present)" : "");
    }
}
