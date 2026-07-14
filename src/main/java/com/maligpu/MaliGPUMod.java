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
        LOGGER.info("[MaliGPUOptimization] asyncOcclusionCulling={} particleLimit={} aggressiveTickThrottle={}",
                MaliGPUConfig.INSTANCE.asyncOcclusionCulling,
                MaliGPUConfig.INSTANCE.particleLimit,
                MaliGPUConfig.INSTANCE.aggressiveTickThrottle);
        if (MaliGPUConfig.INSTANCE.asyncOcclusionCulling) {
            OcclusionCullingSystem.INSTANCE.start();
        }
    }
}
