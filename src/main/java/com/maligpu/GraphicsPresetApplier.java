package com.maligpu;

import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

/**
 * Graphics auto-preset applied once on first run (then user may override in video settings).
 *
 * Mali TBDR GPUs (Helio G100 / G57 MC2) stall on bandwidth-heavy states: fancy clouds, FANCY
 * graphics preset, smooth lighting, and mipmap generation all push extra fragment/vertex work
 * for little gameplay value on a phone SoC. We snap to the Mali-friendly baseline on first launch
 * only, leaving the user free to change anything afterward. Engine-side, Vulkan-safe.
 */
public final class GraphicsPresetApplier {

    public static void applyOnFirstRun(Minecraft mc) {
        if (mc.options == null) return;
        if (!MaliGPUConfig.INSTANCE.graphicsAutoPreset) return;
        if (MaliGPUConfig.INSTANCE.graphicsPresetApplied) return;

        Options opts = mc.options;
        opts.graphicsPreset().set(GraphicsPreset.FAST);
        opts.cloudStatus().set(CloudStatus.OFF);
        opts.mipmapLevels().set(0);
        // Keep smooth lighting off for bandwidth; FAST preset already does this but be explicit.
        // (smoothLighting is part of GraphicsPreset; no separate public OptionInstance in 26.1.2.)

        MaliGPUConfig.INSTANCE.graphicsPresetApplied = true;
        MaliGPUConfig.INSTANCE.save();

        MaliGPUMod.LOGGER.info("[MaliGPUOptimization] Graphics auto-preset applied (FAST graphics, clouds OFF, mipmap 0) for Mali TBDR.");
    }
}
