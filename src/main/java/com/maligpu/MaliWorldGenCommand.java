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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class MaliWorldGenCommand {
    // How many full chunks to force-generate per server tick.
    // Higher = faster world-gen but bigger per-tick hitch. Tune to taste.
    private static final int BATCH = 32;

    private static final Queue<ChunkPos> queue = new ArrayDeque<>();
    private static ServerLevel level;
    private static CommandSourceStack feedback;
    private static int total = 0;
    private static int done = 0;
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
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("[MaliGPUOptimization] This command needs a player context."));
            return 0;
        }
        ServerLevel world = src.getLevel();
        BlockPos center = player.blockPosition();
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
        running = true;

        src.sendSuccess(() -> Component.literal("[MaliGPUOptimization] Queued " + total + " chunks for fast pre-generation around you. Stand by..."), false);
        return 1;
    }

    private static void tick() {
        if (!running || queue.isEmpty()) return;

        int n = Math.min(BATCH, queue.size());
        for (int i = 0; i < n; i++) {
            ChunkPos p = queue.poll();
            if (p == null) break;
            try {
                // Force-generate the chunk to FULL status on the server thread (saves to disk).
                level.getChunkSource().getChunk(p.x(), p.z(), ChunkStatus.FULL, true);
                done++;
            } catch (Exception ignored) {
                done++;
            }
        }

        if (queue.isEmpty()) {
            running = false;
            final int d = done, t = total;
            if (feedback != null) {
                feedback.sendSuccess(() -> Component.literal("[MaliGPUOptimization] Pre-generation complete: " + d + "/" + t + " chunks generated and saved."), false);
            }
            total = 0;
            done = 0;
            level = null;
            feedback = null;
        } else if (done > 0 && done % 500 == 0) {
            // Lightweight progress ping every 500 chunks so the user can see it working.
            final int d = done, t = total;
            if (feedback != null) {
                feedback.sendSuccess(() -> Component.literal("[MaliGPUOptimization] Pre-generating... " + d + "/" + t + " chunks done."), false);
            }
        }
    }
}
