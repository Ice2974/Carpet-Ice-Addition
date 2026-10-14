package com.ice2974.carpeticeaddition.command;

import carpet.utils.CommandHelper;
import com.ice2974.carpeticeaddition.command.WeakloadRegistry.TicketApplyException;
import com.ice2974.carpeticeaddition.command.WeakloadRegistry.WriteProtectedException;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /weakload：语法与反馈镜像原版 /forceload 的弱加载版本。弱加载区块持续执行计划刻
 * 与方块实体刻，不执行实体刻与随机刻；玩家、/forceload 等更强加载来源自然覆盖实际
 * 加载等级，强来源消失后自动回落弱加载。命令返回表示登记完成，不等待区块异步加载。
 */
public final class WeakloadCommand {
    private static final int MAX_CHUNK_LIMIT = 256;
    private static final int WORLD_BOUND = 30000000;

    private static final Dynamic2CommandExceptionType ERROR_TOO_MANY_CHUNKS = new Dynamic2CommandExceptionType(
            (max, amount) -> tr("command.carpet-ice-addition.weakload.error.toobig", max, amount)
    );
    private static final DynamicCommandExceptionType ERROR_OUT_OF_WORLD = new DynamicCommandExceptionType(
            value -> tr("command.carpet-ice-addition.weakload.error.out_of_world", value)
    );
    private static final Dynamic2CommandExceptionType ERROR_NOT_WEAK_LOADED = new Dynamic2CommandExceptionType(
            (chunk, dimension) -> tr("command.carpet-ice-addition.weakload.query.failure", chunk, dimension)
    );
    private static final SimpleCommandExceptionType ERROR_ALL_ADDED = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.weakload.added.failure")
    );
    private static final SimpleCommandExceptionType ERROR_NONE_REMOVED = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.weakload.removed.failure")
    );
    private static final SimpleCommandExceptionType CONFIG_SAVE_FAILED = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.weakload.error.config_save_failed")
    );
    private static final SimpleCommandExceptionType CONFIG_PROTECTED = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.weakload.error.config_protected")
    );
    private static final SimpleCommandExceptionType TICKET_APPLY_FAILED = new SimpleCommandExceptionType(
            tr("command.carpet-ice-addition.weakload.error.ticket_apply_failed")
    );

    private WeakloadCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("weakload")
                .requires(WeakloadCommand::canUseWeakload)
                .then(literal("add")
                        .then(argument("from", ColumnPosArgument.columnPos())
                                .executes(context -> changeWeakLoad(
                                        context.getSource(),
                                        ColumnPosArgument.getColumnPos(context, "from"),
                                        ColumnPosArgument.getColumnPos(context, "from"),
                                        true))
                                .then(argument("to", ColumnPosArgument.columnPos())
                                        .executes(context -> changeWeakLoad(
                                                context.getSource(),
                                                ColumnPosArgument.getColumnPos(context, "from"),
                                                ColumnPosArgument.getColumnPos(context, "to"),
                                                true)))))
                .then(literal("remove")
                        .then(argument("from", ColumnPosArgument.columnPos())
                                .executes(context -> changeWeakLoad(
                                        context.getSource(),
                                        ColumnPosArgument.getColumnPos(context, "from"),
                                        ColumnPosArgument.getColumnPos(context, "from"),
                                        false))
                                .then(argument("to", ColumnPosArgument.columnPos())
                                        .executes(context -> changeWeakLoad(
                                                context.getSource(),
                                                ColumnPosArgument.getColumnPos(context, "from"),
                                                ColumnPosArgument.getColumnPos(context, "to"),
                                                false))))
                        .then(literal("all").executes(context -> removeAll(context.getSource()))))
                .then(literal("query")
                        .executes(context -> listWeakLoad(context.getSource()))
                        .then(argument("pos", ColumnPosArgument.columnPos())
                                .executes(context -> queryWeakLoad(
                                        context.getSource(),
                                        ColumnPosArgument.getColumnPos(context, "pos"))))));
    }

    private static int changeWeakLoad(CommandSourceStack source, ColumnPos from, ColumnPos to, boolean add)
            throws CommandSyntaxException {
        int minX = Math.min(from.x(), to.x());
        int minZ = Math.min(from.z(), to.z());
        int maxX = Math.max(from.x(), to.x());
        int maxZ = Math.max(from.z(), to.z());
        if (minX >= -WORLD_BOUND && minZ >= -WORLD_BOUND && maxX < WORLD_BOUND && maxZ < WORLD_BOUND) {
            int minChunkX = SectionPos.blockToSectionCoord(minX);
            int minChunkZ = SectionPos.blockToSectionCoord(minZ);
            int maxChunkX = SectionPos.blockToSectionCoord(maxX);
            int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
            long chunkCount = (maxChunkX - minChunkX + 1L) * (maxChunkZ - minChunkZ + 1L);
            if (chunkCount > MAX_CHUNK_LIMIT) {
                throw ERROR_TOO_MANY_CHUNKS.create(MAX_CHUNK_LIMIT, chunkCount);
            }

            ServerLevel level = source.getLevel();
            String dimension = level.dimension().identifier().toString();
            long[] packedChunks = new long[(int) chunkCount];
            int index = 0;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    packedChunks[index++] = WeakloadRegistry.pack(chunkX, chunkZ);
                }
            }

            long[] changedChunks;
            try {
                changedChunks = add
                        ? WeakloadManager.addChunks(level, packedChunks)
                        : WeakloadManager.removeChunks(level, packedChunks);
            } catch (IOException exception) {
                throw storageError(exception);
            }
            if (changedChunks.length == 0) {
                throw add ? ERROR_ALL_ADDED.create() : ERROR_NONE_REMOVED.create();
            }
            String minChunk = "[" + minChunkX + ", " + minChunkZ + "]";
            String maxChunk = "[" + maxChunkX + ", " + maxChunkZ + "]";
            if (changedChunks.length == 1) {
                // 范围内只有一个区块实际变化时反馈该区块本身，而不是范围最小角
                long packed = changedChunks[0];
                String chunk = "[" + WeakloadRegistry.unpackX(packed) + ", " + WeakloadRegistry.unpackZ(packed) + "]";
                String key = add
                        ? "command.carpet-ice-addition.weakload.added.single"
                        : "command.carpet-ice-addition.weakload.removed.single";
                source.sendSuccess(() -> tr(key, chunk, dimension), true);
            } else {
                String key = add
                        ? "command.carpet-ice-addition.weakload.added.multiple"
                        : "command.carpet-ice-addition.weakload.removed.multiple";
                source.sendSuccess(() -> tr(key, changedChunks.length, minChunk, maxChunk, dimension), true);
            }
            return changedChunks.length;
        }
        throw ERROR_OUT_OF_WORLD.create("(" + minX + ", " + minZ + ")");
    }

    private static int removeAll(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        String dimension = level.dimension().identifier().toString();
        try {
            WeakloadManager.removeAllChunks(level);
        } catch (IOException exception) {
            throw storageError(exception);
        }
        source.sendSuccess(
                () -> tr("command.carpet-ice-addition.weakload.removed.all", dimension), true);
        return 0;
    }

    private static int queryWeakLoad(CommandSourceStack source, ColumnPos pos) throws CommandSyntaxException {
        int chunkX = SectionPos.blockToSectionCoord(pos.x());
        int chunkZ = SectionPos.blockToSectionCoord(pos.z());
        ServerLevel level = source.getLevel();
        String dimension = level.dimension().identifier().toString();
        String chunk = "[" + chunkX + ", " + chunkZ + "]";
        if (WeakloadManager.isWeakloaded(level, WeakloadRegistry.pack(chunkX, chunkZ))) {
            source.sendSuccess(() -> tr(
                    "command.carpet-ice-addition.weakload.query.success", chunk, dimension), false);
            return 1;
        }
        throw ERROR_NOT_WEAK_LOADED.create(chunk, dimension);
    }

    private static int listWeakLoad(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        String dimension = level.dimension().identifier().toString();
        long[] chunks = WeakloadManager.chunksIn(level);
        int chunkCount = chunks.length;
        if (chunkCount > 0) {
            StringBuilder chunkList = new StringBuilder();
            for (int index = 0; index < chunkCount; index++) {
                if (index > 0) {
                    chunkList.append(", ");
                }
                chunkList.append('[')
                        .append(WeakloadRegistry.unpackX(chunks[index]))
                        .append(", ")
                        .append(WeakloadRegistry.unpackZ(chunks[index]))
                        .append(']');
            }
            String joined = chunkList.toString();
            if (chunkCount == 1) {
                source.sendSuccess(() -> tr(
                        "command.carpet-ice-addition.weakload.list.single", dimension, joined), false);
            } else {
                source.sendSuccess(() -> tr(
                        "command.carpet-ice-addition.weakload.list.multiple", chunkCount, dimension, joined), false);
            }
        } else {
            source.sendFailure(tr(
                    "command.carpet-ice-addition.weakload.added.none", dimension));
        }
        return chunkCount;
    }

    private static CommandSyntaxException storageError(IOException exception) {
        if (exception instanceof WriteProtectedException) {
            return CONFIG_PROTECTED.create();
        }
        if (exception instanceof TicketApplyException) {
            return TICKET_APPLY_FAILED.create();
        }
        return CONFIG_SAVE_FAILED.create();
    }

    private static MutableComponent tr(String key, Object... args) {
        return Component.literal(TranslationFormatUtil.translate(key, args));
    }

    private static boolean canUseWeakload(CommandSourceStack source) {
        return CommandHelper.canUseCommand(source, CarpetIceAdditionSettings.commandWeakload);
    }
}
