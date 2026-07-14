package com.maligpu;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaliGPUMod implements ModInitializer {
    public static final String MOD_ID = "maligpu";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[MaliGPUOptimization] Initializing for Mali TBDR (Helio G100 / G57 MC2) on Minecraft 26.1.2");

        String beryl = BerylCompat.detectModId();
        if (beryl != null) {
            LOGGER.info("[MaliGPUOptimization] Beryl detected (modid={}). Enabling Beryl compatibility auto-tune to reclaim CPU/bandwidth headroom.", beryl);
            BerylCompat.applyAutoTune();
        }

        LOGGER.info("[MaliGPUOptimization] asyncOcclusionCulling={} (off under VulkanMod - entity hook not on Vulkan path) particleLimit={} aggressiveTickThrottle={} autoTuneForBeryl={}",
                MaliGPUConfig.INSTANCE.asyncOcclusionCulling,
                MaliGPUConfig.INSTANCE.particleLimit,
                MaliGPUConfig.INSTANCE.aggressiveTickThrottle,
                MaliGPUConfig.INSTANCE.autoTuneForBeryl);

        if (MaliGPUConfig.INSTANCE.asyncOcclusionCulling) {
            OcclusionCullingSystem.INSTANCE.start();
        } else {
            LOGGER.info("[MaliGPUOptimization] Occlusion culling disabled - safe under VulkanMod; using particle cap + tick throttle only.");
        }
    }
}
