package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.LevelEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedJukeboxStartEventManagerTest {
    private static final BlockPos POS = new BlockPos(1, 64, 2);
    private boolean previousRule;
    private RecordingLevel world;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        previousRule = CarpetIceAdditionSettings.recordWorldEventFix;
        CarpetIceAdditionSettings.recordWorldEventFix = true;
        DelayedJukeboxStartEventManager.clearAll();
        world = recordingLevel();
    }

    @AfterEach
    void tearDown() {
        DelayedJukeboxStartEventManager.clearAll();
        CarpetIceAdditionSettings.recordWorldEventFix = previousRule;
    }

    @Test
    void stopThenStartQueuesTheNewSongUntilTickEnd() {
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.queueStart(world, POS, 42);
        assertTrue(world.events.isEmpty());

        DelayedJukeboxStartEventManager.tick(world);

        assertEquals(List.of(start(POS, 42)), world.events);
    }

    @Test
    void startThenStopCancelsThePendingSong() {
        DelayedJukeboxStartEventManager.queueStart(world, POS, 42);
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.tick(world);
        assertTrue(world.events.isEmpty());
    }

    @Test
    void repeatedReplacementKeepsOnlyTheLastStart() {
        DelayedJukeboxStartEventManager.queueStart(world, POS, 10);
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.queueStart(world, POS, 20);
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.queueStart(world, POS, 30);
        DelayedJukeboxStartEventManager.queueStart(world, POS, 40);
        DelayedJukeboxStartEventManager.tick(world);
        assertEquals(List.of(start(POS, 40)), world.events);
    }

    @Test
    void finalStopCancelsAllEarlierReplacements() {
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.queueStart(world, POS, 10);
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.queueStart(world, POS, 20);
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.tick(world);
        assertTrue(world.events.isEmpty());
    }

    @Test
    void positionsAreIndependentAndMutableInputsAreSnapshotted() {
        BlockPos.MutableBlockPos mutable = POS.mutable();
        DelayedJukeboxStartEventManager.queueStart(world, mutable, 10);
        mutable.move(1, 0, 0);
        BlockPos other = mutable.immutable();
        DelayedJukeboxStartEventManager.queueStart(world, mutable, 20);
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        mutable.move(1, 0, 0);

        DelayedJukeboxStartEventManager.tick(world);

        assertEquals(List.of(start(other, 20)), world.events);
    }

    @Test
    void worldsKeepIndependentQueuesAndStops() throws ReflectiveOperationException {
        RecordingLevel otherWorld = recordingLevel();
        DelayedJukeboxStartEventManager.queueStart(world, POS, 10);
        DelayedJukeboxStartEventManager.queueStart(otherWorld, POS, 20);
        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.tick(world);
        assertTrue(world.events.isEmpty());
        assertTrue(otherWorld.events.isEmpty());

        DelayedJukeboxStartEventManager.tick(otherWorld);

        assertEquals(List.of(start(POS, 20)), otherWorld.events);
    }

    @Test
    void tickFlushesOnceAndAStopDoesNotSuppressNextTicksStart() {
        DelayedJukeboxStartEventManager.queueStart(world, POS, 10);
        DelayedJukeboxStartEventManager.tick(world);
        DelayedJukeboxStartEventManager.tick(world);
        assertEquals(List.of(start(POS, 10)), world.events);

        DelayedJukeboxStartEventManager.recordStop(world, POS);
        DelayedJukeboxStartEventManager.tick(world);
        DelayedJukeboxStartEventManager.queueStart(world, POS, 20);
        DelayedJukeboxStartEventManager.tick(world);
        assertEquals(List.of(start(POS, 10), start(POS, 20)), world.events);
    }

    @Test
    void disablingTheRuleDropsPendingStartsWithoutReplayingThem() {
        DelayedJukeboxStartEventManager.queueStart(world, POS, 10);
        CarpetIceAdditionSettings.recordWorldEventFix = false;
        DelayedJukeboxStartEventManager.tick(world);
        CarpetIceAdditionSettings.recordWorldEventFix = true;
        DelayedJukeboxStartEventManager.tick(world);
        assertTrue(world.events.isEmpty());

        DelayedJukeboxStartEventManager.queueStart(world, POS, 20);
        DelayedJukeboxStartEventManager.tick(world);
        assertEquals(List.of(start(POS, 20)), world.events);
    }

    @Test
    void serverCloseClearsEveryWorldsPendingStarts() throws ReflectiveOperationException {
        RecordingLevel otherWorld = recordingLevel();
        DelayedJukeboxStartEventManager.queueStart(world, POS, 10);
        DelayedJukeboxStartEventManager.queueStart(otherWorld, POS, 20);
        DelayedJukeboxStartEventManager.clearAll();
        DelayedJukeboxStartEventManager.tick(world);
        DelayedJukeboxStartEventManager.tick(otherWorld);
        assertTrue(world.events.isEmpty());
        assertTrue(otherWorld.events.isEmpty());

        DelayedJukeboxStartEventManager.queueStart(world, POS, 30);
        DelayedJukeboxStartEventManager.tick(world);
        assertEquals(List.of(start(POS, 30)), world.events);
    }

    private static SentEvent start(BlockPos pos, int songId) {
        return new SentEvent(LevelEvent.SOUND_PLAY_JUKEBOX_SONG, pos, songId);
    }

    private static RecordingLevel recordingLevel() throws ReflectiveOperationException {
        // Only world identity and the overridden event sink are used by this manager.
        // Bypass ServerLevel construction to avoid starting a server or accessing saves.
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        RecordingLevel level = (RecordingLevel) ((Unsafe) field.get(null)).allocateInstance(RecordingLevel.class);
        level.events = new ArrayList<>();
        return level;
    }

    private record SentEvent(int id, BlockPos pos, int data) {
    }

    private static final class RecordingLevel extends ServerLevel {
        private List<SentEvent> events;

        private RecordingLevel() {
            super(null, null, null, null, null, null, false, 0L, List.of(), false, null);
            throw new AssertionError("This event sink must not construct a Minecraft world");
        }

        @Override
        public void levelEvent(Entity source, int id, BlockPos pos, int data) {
            events.add(new SentEvent(id, pos.immutable(), data));
        }
    }
}
