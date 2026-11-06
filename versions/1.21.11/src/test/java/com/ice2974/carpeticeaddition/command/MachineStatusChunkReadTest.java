package com.ice2974.carpeticeaddition.command;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MachineStatusChunkReadTest {
    private static final BlockPos POSITION = new BlockPos(-17, 64, 32);

    @TempDir
    Path saveRoot;
    private MinecraftServer server;
    private ProbeChunkCache chunks;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() throws Exception {
        // These fixtures never start a server, open a world, or run Minecraft constructors.
        chunks = allocate(ProbeChunkCache.class);
        chunks.available = new CompletableFuture<>();
        ProbeLevel level = allocate(ProbeLevel.class);
        level.chunks = chunks;
        server = allocate(DedicatedServer.class);
        Field levels = MinecraftServer.class.getDeclaredField("levels");
        levels.setAccessible(true);
        levels.set(server, Map.of(Level.OVERWORLD, level));
        MachineStatusConfigManager.initialize(saveRoot);
        MachineStatusConfigManager.addMachine("machine", "minecraft:overworld",
                POSITION.getX(), POSITION.getY(), POSITION.getZ(),
                "minecraft:lever[face=wall,facing=north,powered=false]");
    }

    @AfterEach
    void shutdown() {
        MachineStatusConfigManager.shutdown();
    }

    @Test
    void fullTicketWithIncompleteChunkIsUnloadedWithoutWaiting() {
        assertStatus(MachineStatusKind.UNLOADED);
        assertFalse(chunks.available.isDone());
    }

    @Test
    void completedChunkIsReadDirectlyAndMatchesSavedState() throws Exception {
        completeChunk(Blocks.LEVER.defaultBlockState());
        assertStatus(MachineStatusKind.STOPPED);
    }

    @Test
    void completedChunkWithChangedPropertiesIsRunning() throws Exception {
        completeChunk(Blocks.LEVER.defaultBlockState().setValue(LeverBlock.POWERED, true));
        assertStatus(MachineStatusKind.RUNNING);
    }

    @Test
    void completedChunkWithChangedBlockTypeIsInvalid() throws Exception {
        completeChunk(Blocks.STONE.defaultBlockState());
        assertStatus(MachineStatusKind.INVALID);
    }

    private void assertStatus(MachineStatusKind kind) {
        assertEquals(List.of("machine"), MachineStatusCommand.getMachineRecordsByStatus(server, kind)
                .stream().map(MachineStatusConfigManager.MachineRecord::name).toList());
        assertEquals(1, chunks.reads);
    }

    private void completeChunk(BlockState state) throws Exception {
        ProbeChunk chunk = allocate(ProbeChunk.class);
        chunk.state = state;
        chunks.available.complete(chunk);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field instance = unsafeType.getDeclaredField("theUnsafe");
        instance.setAccessible(true);
        return type.cast(unsafeType.getMethod("allocateInstance", Class.class).invoke(instance.get(null), type));
    }

    private static final class ProbeLevel extends ServerLevel {
        private ProbeChunkCache chunks;

        private ProbeLevel() {
            super(null, Runnable::run, null, null, Level.OVERWORLD, null, false, 0L, List.of(), false, null);
        }

        @Override
        public ServerChunkCache getChunkSource() {
            return chunks;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            throw new AssertionError("Query must not re-enter the world's potentially blocking chunk lookup");
        }
    }

    private static final class ProbeChunkCache extends ServerChunkCache {
        private CompletableFuture<LevelChunk> available;
        private int reads;

        private ProbeChunkCache() {
            super(null, null, null, null, Runnable::run, null, 0, 0, false, null, null);
        }

        @Override
        public boolean hasChunk(int x, int z) {
            return true; // A FULL ticket may exist even while the chunk is not ready.
        }

        @Override
        public LevelChunk getChunkNow(int x, int z) {
            assertEquals(-2, x);
            assertEquals(2, z);
            reads++;
            return available.getNow(null);
        }
    }

    private static final class ProbeChunk extends LevelChunk {
        private BlockState state;

        private ProbeChunk() {
            super(null, new ChunkPos(-2, 2));
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            assertEquals(POSITION, pos);
            return state;
        }
    }
}
