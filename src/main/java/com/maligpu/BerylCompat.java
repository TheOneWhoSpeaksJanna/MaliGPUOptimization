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
        cfg.asyncOcclusionCulling = true;
        cfg.particleLimit = true;
        if (cfg.maxParticles > 250) cfg.maxParticles = 250;
        MaliGPUMod.LOGGER.info("[MaliGPUOptimization] Beryl auto-tune applied: occlusion culling forced on, particle cap tightened to {}.", cfg.maxParticles);
    }
}
