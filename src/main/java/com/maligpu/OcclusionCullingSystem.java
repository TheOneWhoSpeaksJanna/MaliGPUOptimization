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

import java.util.BitSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public enum OcclusionCullingSystem {
    INSTANCE;

    private final AtomicInteger visibleCount = new AtomicInteger(0);
    private final AtomicInteger checkedCount = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private ForkJoinPool workers;

    private Entity[] snapshot = new Entity[0];
    private final BitSet visible = new BitSet();
    private int snapshotHash;

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
        int id = e.getId();
        if (id < 0 || id >= snapshot.length) return true;
        return visible.get(id);
    }

    public int visibleCount() {
        return visibleCount.get();
    }

    public int checkedCount() {
        return checkedCount.get();
    }

    private void recompute() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        ClientLevel level = mc.level;
        Vec3 eye = mc.player.getEyePosition(1.0F);
        BlockPos center = BlockPos.containing(eye);
        int extent = Math.max(8, MaliGPUConfig.INSTANCE.cullGridHalfExtent);

        java.util.ArrayList<Entity> list = new java.util.ArrayList<>();
        for (Entity en : level.entitiesForRendering()) list.add(en);
        Entity[] ents = list.toArray(new Entity[0]);
        int localHash = 1;
        for (Entity en : ents) localHash = 31 * localHash + en.getId();
        if (localHash != snapshotHash || ents.length != snapshot.length) {
            snapshot = ents;
            snapshotHash = localHash;
            if (visible.size() < ents.length) visible.clear();
        }
        visible.clear();
        visible.set(mc.player.getId());

        Vec3 finalEye = eye;
        BlockPos finalCenter = center;
        int total = ents.length;
        int kept = 1;
        for (Entity en : ents) {
            if (en == mc.player) continue;
            AABB box = en.getBoundingBox();
            Vec3 target = box.getCenter();
            if (target.distanceTo(finalEye) > extent
                    || hasLineOfSight(level, finalCenter, target, extent)) {
                visible.set(en.getId());
                kept++;
            }
        }
        visibleCount.set(kept);
        checkedCount.set(total);
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
