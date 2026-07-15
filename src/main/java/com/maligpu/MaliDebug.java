package com.maligpu;

import net.minecraft.client.Minecraft;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Deep debug instrumentation for the whole game + our mod. Gated by MaliGPUConfig.debugLogging
 * so it stays silent unless the user opts in. Engine-side only (no Vulkan/render-path access).
 *
 * What it can tell you that vanilla logs can't:
 *  - exactly how long each resource reload took (and how many times) -> the "29877 caches cleared" storm
 *  - JVM GC pause totals (the biggest hidden FPS-killer on a 1.5GB Android device)
 *  - sound spawn rate per second (audio-thread stall detector)
 *  - loaded-chunk delta per second (memory + bandwidth pressure from chunk churn)
 */
public final class MaliDebug {
    private static final org.slf4j.Logger LOG = com.maligpu.MaliGPUMod.LOGGER;
    private static final boolean ON = MaliGPUConfig.INSTANCE.debugLogging;

    private static long reloadCount = 0;
    private static long lastReloadStart = 0;
    private static long worstReloadMs = 0;

    private static long lastGcTime = 0;
    private static long lastGcCount = 0;

    private static long soundWindowStart = 0;
    private static long soundCount = 0;

    private static int lastLoadedChunks = -1;
    private static long lastChunkSample = 0;

    public static void reloadStart() {
        if (!ON) return;
        reloadCount++;
        lastReloadStart = System.nanoTime();
    }

    public static void reloadEnd() {
        if (!ON) return;
        if (lastReloadStart == 0) return;
        long ms = (System.nanoTime() - lastReloadStart) / 1_000_000;
        worstReloadMs = Math.max(worstReloadMs, ms);
        LOG.info("[MaliGPUOptimization/DEBUG] resource reload #{} took {} ms (worst so far {} ms). " +
                        "Frequent/long reloads => full cache wipe + atlas re-upload (bandwidth tax on Mali).",
                reloadCount, ms, worstReloadMs);
        lastReloadStart = 0;
    }

    public static void soundPlayed() {
        if (!ON) return;
        long now = System.currentTimeMillis();
        if (soundWindowStart == 0) {
            soundWindowStart = now;
            soundCount = 0;
        }
        soundCount++;
        long elapsed = now - soundWindowStart;
        if (elapsed >= 1000) {
            LOG.info("[MaliGPUOptimization/DEBUG] sound spawn rate: {} sounds/sec (audio-thread stall risk above ~120/sec on Mali).",
                    soundCount);
            soundWindowStart = now;
            soundCount = 0;
        }
    }

    public static void sampleChunks() {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        int now = mc.level.getChunkSource().getLoadedChunksCount();
        long t = System.currentTimeMillis();
        if (lastLoadedChunks >= 0 && lastChunkSample > 0) {
            long dt = Math.max(1, t - lastChunkSample);
            int delta = now - lastLoadedChunks;
            if (delta != 0) {
                double perSec = delta * 1000.0 / dt;
                LOG.info("[MaliGPUOptimization/DEBUG] loaded chunks {} (delta {} in {} ms = {}/sec). " +
                                "Positive churn = mesh rebuild + vertex-upload bandwidth.", now, delta, dt, String.format("%.1f", perSec));
            }
        }
        lastLoadedChunks = now;
        lastChunkSample = t;
    }

    public static void gcSnapshot(String when) {
        if (!ON) return;
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        long time = 0, count = 0;
        for (var b : beans) {
            time += b.getCollectionTime();
            count += b.getCollectionCount();
        }
        long dTime = time - lastGcTime;
        long dCount = count - lastGcCount;
        if (lastGcTime == 0) {
            LOG.info("[MaliGPUOptimization/DEBUG] GC baseline @ {}: total {} ms over {} collections ({}).",
                    when, time, count, beans.size());
        } else if (dCount > 0) {
            LOG.info("[MaliGPUOptimization/DEBUG] GC since {} : +{} ms over +{} collections (avg {} ms/collection).",
                    when, dTime, dCount, String.format("%.1f", dTime * 1.0 / Math.max(1, dCount)));
        }
        lastGcTime = time;
        lastGcCount = count;
    }

    public static void feature(String name, String detail) {
        if (!ON) return;
        LOG.info("[MaliGPUOptimization/DEBUG] feature {}: {}", name, detail);
    }

    public static boolean enabled() {
        return ON;
    }
}
