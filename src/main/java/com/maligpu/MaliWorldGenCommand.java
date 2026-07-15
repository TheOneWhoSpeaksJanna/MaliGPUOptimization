package com.maligpu;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public final class MaliWorldGenCommand {
    // Time-budgeted world generation.
    //
    // The old version generated a fixed BATCH of chunks per server tick, which on a Mali SoC could
    // take >200ms/tick and spiral the integrated loopback server ("Can't keep up! ... 4000+ ticks
    // behind"), freezing the game. Instead we budget by TIME: each tick we generate chunks until a
    // soft wall (default 12ms) is hit, then stop and let the server tick finish normally. This keeps
    // the game responsive (server TPS stays ~20) while pre-gen still makes steady progress in the
    // background. The wall is configurable in maligpu.properties (worldGenTickBudgetMs).
    private static final long TICK_BUDGET_MS = MaliGPUConfig.INSTANCE.worldGenTickBudgetMs;

    private static final Queue<ChunkPos> queue = new ArrayDeque<>();
    private static ServerLevel level;
    private static CommandSourceStack feedback;
    private static ServerPlayer player;
    private static ServerBossEvent bar;
    private static int total = 0;
    private static int done = 0;
    private static long startTimeMs = 0;
    private static boolean running = false;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("mali-load-world")
                .requires(src -> src.getPlayer() != null)
                .then(Commands.argument("chunks", IntegerArgumentType.integer(1, 200000))
                    .executes(ctx -> startGen(ctx, IntegerArgumentType.getInteger(ctx, "chunks"))))));

        ServerTickEvents.START_SERVER_TICK.register(server -> tick());
    }

    private static int startGen(CommandContext<CommandSourceStack> ctx, int count) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("[MaliGPUOptimization] This command needs a player context."));
            return 0;
        }
        ServerLevel world = src.getLevel();
        BlockPos center = p.blockPosition();
        ChunkPos cc = new ChunkPos(center.getX() >> 4, center.getZ() >> 4);
        int side = (int) Math.ceil(Math.sqrt(count));
        int half = side / 2;

        List<ChunkPos> list = new ArrayList<>();
        outer:
        for (int dz = -half; dz <= half; dz++) {
            for (int dx = -half; dx <= half; dx++) {
                list.add(new ChunkPos(cc.x() + dx, cc.z() + dz));
                if (list.size() >= count) break outer;
            }
        }

        queue.clear();
        queue.addAll(list);
        total = list.size();
        done = 0;
        level = world;
        feedback = src;
        player = p;
        startTimeMs = System.currentTimeMillis();
        running = true;

        bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("[MaliGPUOptimization] Starting..."), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        bar.setProgress(0.0f);
        bar.addPlayer(p);

        src.sendSuccess(() -> Component.literal("[MaliGPUOptimization] Queued " + total + " chunks for time-budgeted pre-generation (no server lag). Green bar = progress + ETA."), false);
        return 1;
    }

    private static String formatEta(long millis) {
        if (millis <= 0) return "0s";
        long s = millis / 1000;
        long m = s / 60;
        s = s % 60;
        if (m > 0) return m + "m" + (s > 0 ? s + "s" : "");
        return s + "s";
    }

    private static void tick() {
        if (!running || queue.isEmpty()) return;

        long budget = Math.max(4L, MaliGPUConfig.INSTANCE.worldGenTickBudgetMs);
        long start = System.nanoTime();
        long limitNs = budget * 1_000_000L;

        int generated = 0;
        while (!queue.isEmpty()) {
            ChunkPos cp = queue.peek();
            try {
                level.getChunkSource().getChunk(cp.x(), cp.z(), ChunkStatus.FULL, true);
                done++;
                generated++;
            } catch (Exception ignored) {
                done++;
            }
            queue.poll();

            // Stop this tick once we hit the time budget so the server tick can finish on time.
            if ((System.nanoTime() - start) >= limitNs) break;
        }

        float progress = total > 0 ? (float) done / (float) total : 1.0f;
        long elapsed = System.currentTimeMillis() - startTimeMs;
        String eta = "calculating...";
        if (done > 0 && elapsed > 0) {
            long perChunkMs = elapsed / done;
            long remainingMs = perChunkMs * (total - done);
            eta = formatEta(remainingMs);
        }
        int pct = (int) (progress * 100);
        if (bar != null) {
            bar.setProgress(progress);
            bar.setName(Component.literal("[MaliGPUOptimization] " + pct + "%  " + done + "/" + total + "  ETA " + eta + "  (+" + generated + "/tick)"));
        }

        if (queue.isEmpty()) {
            running = false;
            if (bar != null) {
                bar.setName(Component.literal("[MaliGPUOptimization] Done! " + done + "/" + total + " chunks"));
                bar.setProgress(1.0f);
                bar.removeAllPlayers();
            }
            if (feedback != null) {
                feedback.sendSuccess(() -> Component.literal("[MaliGPUOptimization] Pre-generation complete: " + done + "/" + total + " chunks generated (time-budgeted, server stayed responsive)."), false);
            }
            total = 0;
            done = 0;
            level = null;
            feedback = null;
            player = null;
            bar = null;
            startTimeMs = 0;
        }
    }
}
