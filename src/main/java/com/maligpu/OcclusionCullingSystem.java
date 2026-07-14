package com.maligpu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public enum OcclusionCullingSystem {
    INSTANCE;

    private final Set<Entity> visibleEntities = new HashSet<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private ForkJoinPool workers;

    public synchronized void start() {
        if (running.get()) return;
        int threads = Math.max(1, MaliGPUConfig.INSTANCE.cullWorkerThreads);
        workers = new ForkJoinPool(threads);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MaliGPU-OcclusionCuller");
            t.setDaemon(true);
            return t;
        });
        running.set(true);
        int interval = Math.max(50, MaliGPUConfig.INSTANCE.cullRecomputeIntervalMs);
        scheduler.scheduleAtFixedRate(this::recompute, 0, interval, TimeUnit.MILLISECONDS);
        MaliGPUMod.LOGGER.info("[MaliGPUOptimization] Occlusion culling active (workers={}, interval={}ms)", threads, interval);
    }

    public synchronized void stop() {
        running.set(false);
        if (scheduler != null) scheduler.shutdownNow();
        if (workers != null) workers.shutdownNow();
    }

    public boolean isEntityVisible(Entity e) {
        if (!MaliGPUConfig.INSTANCE.asyncOcclusionCulling) return true;
        synchronized (visibleEntities) {
            return visibleEntities.contains(e);
        }
    }

    private void recompute() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        ClientLevel level = mc.level;
        Vec3 eye = mc.player.getEyePosition(1.0F);
        BlockPos center = BlockPos.containing(eye);
        int extent = Math.max(8, MaliGPUConfig.INSTANCE.cullGridHalfExtent);

        Set<Entity> next = new HashSet<>();
        next.add(mc.player);
        for (Entity e : level.entitiesForRendering()) {
            if (e == mc.player) continue;
            AABB box = e.getBoundingBox();
            Vec3 target = box.getCenter();
            if (target.distanceTo(eye) > extent) {
                next.add(e);
                continue;
            }
            if (hasLineOfSight(level, center, target, extent)) {
                next.add(e);
            }
        }

        synchronized (visibleEntities) {
            visibleEntities.clear();
            visibleEntities.addAll(next);
        }
    }

    private boolean hasLineOfSight(ClientLevel level, BlockPos from, Vec3 to, int extent) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 dir = to.subtract(start);
        double dist = dir.length();
        if (dist < 0.001) return true;
        Vec3 step = dir.normalize().scale(0.5);
        int maxSteps = (int) (dist / 0.5);
        Vec3 cur = start;
        for (int i = 0; i < maxSteps; i++) {
            cur = cur.add(step);
            if (Math.abs(cur.x - start.x) > extent && Math.abs(cur.y - start.y) > extent && Math.abs(cur.z - start.z) > extent) {
                return true;
            }
            BlockPos bp = BlockPos.containing(cur);
            try {
                BlockState bs = level.getBlockState(bp);
                if (bs == null) return true;
                if (bs.isSolid()) {
                    FluidState fs = bs.getFluidState();
                    if (fs != null && !fs.isEmpty()) return true;
                    return false;
                }
            } catch (Exception ignored) {
                return true;
            }
        }
        return true;
    }
}
