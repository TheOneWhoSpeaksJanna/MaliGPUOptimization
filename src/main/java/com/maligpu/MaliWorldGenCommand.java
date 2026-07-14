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
    // How many full chunks to force-generate per server tick.
    // Higher = faster world-gen but bigger per-tick hitch. Tune to taste.
    private static final int BATCH = 32;

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
                    .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "chunks"))))));

        ServerTickEvents.START_SERVER_TICK.register(server -> tick());
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, int count) {
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

        src.sendSuccess(() -> Component.literal("[MaliGPUOptimization] Queued " + total + " chunks for fast pre-generation around you. Green bar shows progress + ETA."), false);
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

        int n = Math.min(BATCH, queue.size());
        for (int i = 0; i < n; i++) {
            ChunkPos p = queue.poll();
            if (p == null) break;
            try {
                level.getChunkSource().getChunk(p.x(), p.z(), ChunkStatus.FULL, true);
                done++;
            } catch (Exception ignored) {
                done++;
            }
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
            bar.setName(Component.literal("[MaliGPUOptimization] " + pct + "%  " + done + "/" + total + "  ETA " + eta));
        }

        if (queue.isEmpty()) {
            running = false;
            if (bar != null) {
                bar.setName(Component.literal("[MaliGPUOptimization] Done! " + done + "/" + total + " chunks"));
                bar.setProgress(1.0f);
                bar.removeAllPlayers();
            }
            if (feedback != null) {
                feedback.sendSuccess(() -> Component.literal("[MaliGPUOptimization] Pre-generation complete: " + done + "/" + total + " chunks generated and saved."), false);
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
