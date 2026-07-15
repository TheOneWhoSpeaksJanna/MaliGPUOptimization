package com.maligpu;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.util.ArrayDeque;

/**
 * Client-side performance manager (engine-side, Vulkan-safe).
 *
 * Two responsibilities, both gap-fillers the current modpack does NOT cover:
 *  1) Dynamic FPS  - throttle the client frame rate when the window is unfocused / hidden,
 *     conserving CPU/GPU/battery. FPSDisplay only reads fps; nothing in the pack throttles it.
 *  2) Adaptive distance - sample average client fps and step render + simulation distance down
 *     when fps is too low, restore when headroom returns. All distances in the pack are static.
 *
 * Both work purely through the public Options API (framerateLimit / renderDistance /
 * simulationDistance) and the Window focus state. No renderer / Vulkan path is touched.
 */
public final class ClientPerfManager {

    public static void tick(Minecraft mc) {
        if (mc.options == null || mc.getWindow() == null) return;

        if (MaliGPUConfig.INSTANCE.dynamicFps) {
            applyDynamicFps(mc);
        }
        if (MaliGPUConfig.INSTANCE.adaptiveDistance) {
            applyAdaptiveDistance(mc);
        }
    }

    // ---- Dynamic FPS -------------------------------------------------------

    private static void applyDynamicFps(Minecraft mc) {
        Window w = mc.getWindow();
        Options opts = mc.options;
        int userLimit = opts.framerateLimit().get();

        // Respect an explicit user cap: don't push above it. UNLIMITED_FRAMERATE_CUTOFF (0) means
        // the user wants unlimited, so we treat the ceiling as the display refresh.
        int ceiling = (userLimit <= 0) ? Math.max(30, w.getRefreshRate()) : userLimit;

        int target;
        if (w.isIconified() || w.isMinimized()) {
            target = MaliGPUConfig.INSTANCE.dynamicFpsInvisible; // 0 = pause rendering while hidden
        } else if (!w.isFocused()) {
            target = Math.min(ceiling, Math.max(1, MaliGPUConfig.INSTANCE.dynamicFpsUnfocused));
        } else {
            target = ceiling; // focused: use the user's setting / refresh
        }

        if (target != currentFpsCap) {
            opts.framerateLimit().set(target);
            currentFpsCap = target;
        }
    }

    private static int currentFpsCap = -1;

    // ---- Adaptive distance -------------------------------------------------

    private static final ArrayDeque<Double> samples = new ArrayDeque<>();
    private static double sampleAccum = 0.0;
    private static int sampleCount = 0;
    private static long lastSampleMs = System.currentTimeMillis();

    private static void applyAdaptiveDistance(Minecraft mc) {
        long now = System.currentTimeMillis();
        long window = Math.max(1, (long) MaliGPUConfig.INSTANCE.adaptiveSampleSeconds) * 1000L;
        if (now - lastSampleMs >= window) {
            if (sampleCount > 0) {
                double avg = sampleAccum / sampleCount;
                samples.addLast(avg);
                if (samples.size() > 6) samples.removeFirst(); // keep ~30s of history
                sampleAccum = 0;
                sampleCount = 0;
                lastSampleMs = now;
                adapt(mc, average(samples));
            }
        }
        sampleAccum += mc.getFps();
        sampleCount++;
    }

    private static void adapt(Minecraft mc, double avgFps) {
        Options opts = mc.options;
        int rd = opts.renderDistance().get();
        int sd = opts.simulationDistance().get();
        int min = Math.max(2, MaliGPUConfig.INSTANCE.adaptiveMinDistance);

        if (avgFps < MaliGPUConfig.INSTANCE.adaptiveFloorFps) {
            int nrd = Math.max(min, rd - 1);
            int nsd = Math.max(min, sd - 1);
            if (nrd != rd) opts.renderDistance().set(nrd);
            if (nsd != sd) opts.simulationDistance().set(nsd);
            if (nrd != rd || nsd != sd) {
                MaliGPUMod.LOGGER.info("[MaliGPUOptimization] Adaptive distance: avg fps {} < {} -> render {} / sim {}",
                        String.format("%.1f", avgFps), MaliGPUConfig.INSTANCE.adaptiveFloorFps, nrd, nsd);
            }
        } else if (avgFps > MaliGPUConfig.INSTANCE.adaptiveCeilFps) {
            // Restore only if the user originally had headroom; we never exceed their set distance.
            int maxRd = userMaxRender;
            int maxSd = userMaxSim;
            int nrd = Math.min(maxRd, rd + 1);
            int nsd = Math.min(maxSd, sd + 1);
            if (nrd != rd) opts.renderDistance().set(nrd);
            if (nsd != sd) opts.simulationDistance().set(nsd);
        }
    }

    // Capture the user's starting distances as the adaptive ceiling so we never clamp them tighter
    // than they chose, only relieve pressure when fps is low.
    private static int userMaxRender = 32;
    private static int userMaxSim = 32;

    public static void captureUserBaseline(Minecraft mc) {
        if (mc.options == null) return;
        userMaxRender = Math.max(userMaxRender, mc.options.renderDistance().get());
        userMaxSim = Math.max(userMaxSim, mc.options.simulationDistance().get());
    }

    private static double average(ArrayDeque<Double> q) {
        if (q.isEmpty()) return 0;
        double s = 0;
        for (double v : q) s += v;
        return s / q.size();
    }
}
