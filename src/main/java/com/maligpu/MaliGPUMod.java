package com.maligpu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
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

        // Authoritative: VulkanMod replaces the OpenGL entity-render path, so the occlusion
        // culling hook can never apply. Force it off AFTER Beryl auto-tune (which must not re-enable it).
        boolean vulkanMod = FabricLoader.getInstance().isModLoaded("vulkanmod");
        if (vulkanMod && MaliGPUConfig.INSTANCE.asyncOcclusionCulling) {
            MaliGPUConfig.INSTANCE.asyncOcclusionCulling = false;
            LOGGER.info("[MaliGPUOptimization] VulkanMod detected - forcing asyncOcclusionCulling off (entity hook is not on the Vulkan render path; leaving it on would waste a background thread).");
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

        MaliWorldGenCommand.register();

        // v1.3.0: apply-at-boot + debug instrumentation. Boot mixin applies engine tunables at
        // Minecraft constructor; debug system instruments the whole game when enabled.
        MaliGPUMod.LOGGER.info("[MaliGPUOptimization] v1.3.0 features: dynamicFps={} liftAudioSoundCap={} patchMemoryLeaks={} applyAtBoot={} debugLogging={} worldGenTickBudgetMs={} reloadDebounceMs={}",
                MaliGPUConfig.INSTANCE.dynamicFps,
                MaliGPUConfig.INSTANCE.liftAudioSoundCap,
                MaliGPUConfig.INSTANCE.patchMemoryLeaks,
                MaliGPUConfig.INSTANCE.applyAtBoot,
                MaliGPUConfig.INSTANCE.debugLogging,
                MaliGPUConfig.INSTANCE.worldGenTickBudgetMs,
                MaliGPUConfig.INSTANCE.reloadDebounceMs);

        if (MaliGPUConfig.INSTANCE.debugLogging) {
            MaliDebug.feature("startup", "debug logging enabled - whole-MC instrumentation active (reload timing, GC pauses, sound rate, chunk delta)");
        }
    }
}
