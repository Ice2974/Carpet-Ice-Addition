package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableList;
import com.ice2974.carpeticeaddition.mixins.PlayerBreakIceLikeMagmaBlocksMixin;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 直接调用生产 Mixin 回调及认领任务，保留真实 ItemStack、Brain、职业谓词与距离计算。
 * 测试替身只隔离服务器/POI IO；Unsafe 仅用于绕过这些测试替身的真实服务器构造器。
 * 不替代 Mixin transformation 或游戏内测试。
 */
class MagmaAndJobSiteRegressionTest {
    private static final BlockPos BROKEN = new BlockPos(0, 1, 0);
    private static final BlockPos OLD_SITE = new BlockPos(64, 0, 0);
    private static final BlockPos NEW_SITE = BlockPos.ZERO;
    private static Holder<Enchantment> silkTouch;
    private FakeLevel level;
    private FakePlayer player;
    private FakeVillager villager;
    private FakePoiManager pois;
    private NearbyJobSiteAcquireTask task;
    private BreakHooks hooks;
    private boolean previousRule;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        silkTouch = VanillaRegistries.createLookup().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
    }

    @BeforeEach
    void setUp() throws Exception {
        previousRule = CarpetIceAdditionSettings.iceLikeMagmaBlocks;
        CarpetIceAdditionSettings.iceLikeMagmaBlocks = true;
        pois = allocate(FakePoiManager.class);
        pois.sites = new HashMap<>();
        level = allocate(FakeLevel.class);
        level.states = new HashMap<>();
        level.pois = pois;
        level.random = RandomSource.create(42L);
        level.time = 100L;
        level.states.put(BROKEN, Blocks.MAGMA_BLOCK.defaultBlockState());
        level.states.put(BROKEN.below(), Blocks.STONE.defaultBlockState());
        player = allocate(FakePlayer.class);
        player.world = level;
        player.tool = new ItemStack(Items.DIAMOND_PICKAXE);
        hooks = new BreakHooks(player);
        villager = allocate(FakeVillager.class);
        villager.memory = new Brain<>(List.of(MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE,
                MemoryModuleType.WALK_TARGET), List.of(), ImmutableList.of(), () -> null);
        villager.released = new ArrayList<>();
        Field position = Entity.class.getDeclaredField("position");
        position.setAccessible(true);
        position.set(villager, new Vec3(0.5D, 0.5D, 0.5D));
        task = new NearbyJobSiteAcquireTask(BuiltInRegistries.VILLAGER_PROFESSION
                .getOrThrow(VillagerProfession.ARMORER));
    }

    @AfterEach
    void restoreRule() {
        CarpetIceAdditionSettings.iceLikeMagmaBlocks = previousRule;
    }

    @Test
    void brokenSilkTouchToolDoesNotCreateLava() throws Exception {
        player.tool.enchant(silkTouch, 1);
        player.tool.setDamageValue(player.tool.getMaxDamage() - 1);
        capture();
        // 最后一次耐久消耗的结果：同一主手堆叠变空，附魔读取也随之变空。
        player.tool.shrink(1);
        finish(true);
        assertEquals(0, level.lavaWrites);
    }

    @Test
    void brokenOrdinaryToolStillCreatesLava() throws Exception {
        capture();
        player.tool.shrink(1);
        finish(true);
        assertEquals(1, level.lavaWrites);
    }

    @Test
    void handReplacementCannotChangeCapturedEnchantment() throws Exception {
        capture();
        player.tool = new ItemStack(Items.DIAMOND_PICKAXE);
        player.tool.enchant(silkTouch, 1);
        finish(true);
        assertEquals(1, level.lavaWrites);
    }

    @Test
    void failedBreakClearsSnapshotBeforeNextBreak() throws Exception {
        player.tool.enchant(silkTouch, 1);
        capture();
        finish(false);
        assertEquals(0, level.lavaWrites);
        player.tool = new ItemStack(Items.DIAMOND_PICKAXE);
        capture();
        finish(true);
        assertEquals(1, level.lavaWrites);
        finish(true);
        assertEquals(1, level.lavaWrites, "RETURN 后必须清除待处理快照");
    }

    @Test
    void disabledRuleCreativeAndNonMagmaBreaksDoNotLeaveSnapshot() throws Exception {
        CarpetIceAdditionSettings.iceLikeMagmaBlocks = false;
        capture();
        CarpetIceAdditionSettings.iceLikeMagmaBlocks = true;
        finish(true);
        player.creative = true;
        capture();
        player.creative = false;
        finish(true);
        level.states.put(BROKEN, Blocks.STONE.defaultBlockState());
        capture();
        finish(true);
        assertEquals(0, level.lavaWrites);
    }

    @Test
    void missingSupportPreventsLava() throws Exception {
        level.states.remove(BROKEN.below());
        capture();
        finish(true);
        assertEquals(0, level.lavaWrites);
    }

    @Test
    void removedDistantJobSiteReleasesBeforeReacquiringNearbySite() {
        bind(OLD_SITE, Level.OVERWORLD);
        pois.sites.put(NEW_SITE, armorerSite());
        runAfterInitialDelay();
        assertEquals(List.of(GlobalPos.of(Level.OVERWORLD, OLD_SITE)), villager.released);
        assertFalse(villager.memory.hasMemoryValue(MemoryModuleType.JOB_SITE));
        assertEquals(GlobalPos.of(Level.OVERWORLD, NEW_SITE),
                villager.memory.getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).orElseThrow());
        assertEquals(1, pois.taken);
        assertFalse(villager.memory.hasMemoryValue(MemoryModuleType.WALK_TARGET));
    }

    @Test
    void validDistantJobSiteIsRetainedWithoutClaimingAnotherTicket() {
        bind(OLD_SITE, Level.OVERWORLD);
        pois.sites.put(OLD_SITE, armorerSite());
        pois.sites.put(NEW_SITE, armorerSite());
        runAfterInitialDelay();
        assertEquals(GlobalPos.of(Level.OVERWORLD, OLD_SITE),
                villager.memory.getMemory(MemoryModuleType.JOB_SITE).orElseThrow());
        assertTrue(villager.released.isEmpty());
        assertEquals(0, pois.searches);
        assertEquals(0, pois.taken);
        assertFalse(task.checkExtraStartConditions(level, villager), "有效绑定也必须遵守重试节流");
        assertEquals(1, pois.validations);
    }

    @Test
    void wrongProfessionAtOldPositionDoesNotKeepBinding() {
        bind(OLD_SITE, Level.OVERWORLD);
        pois.sites.put(OLD_SITE, BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.FARMER));
        pois.sites.put(NEW_SITE, armorerSite());
        runAfterInitialDelay();
        assertEquals(1, villager.released.size());
        assertEquals(1, pois.taken);
    }

    @Test
    void crossDimensionBindingReleasesOriginalGlobalPosition() {
        bind(OLD_SITE, Level.NETHER);
        pois.sites.put(OLD_SITE, armorerSite());
        pois.sites.put(NEW_SITE, armorerSite());
        runAfterInitialDelay();
        assertEquals(List.of(GlobalPos.of(Level.NETHER, OLD_SITE)), villager.released);
        assertEquals(0, pois.validations, "不同维度不能使用当前世界的 POI 判定旧绑定");
        assertEquals(1, pois.taken);
    }

    @Test
    void reacquisitionStillRejectsDistantOrWrongProfessionCandidates() {
        bind(OLD_SITE, Level.OVERWORLD);
        pois.sites.put(new BlockPos(2, 0, 0), armorerSite());
        pois.sites.put(NEW_SITE, BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.FARMER));
        runAfterInitialDelay();
        assertEquals(1, villager.released.size());
        assertEquals(0, pois.taken);
        assertFalse(villager.memory.hasMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE));
        assertFalse(villager.memory.hasMemoryValue(MemoryModuleType.WALK_TARGET));
    }

    @Test
    void displacedPotentialSiteStillReleasesBeforeErasing() {
        villager.memory.setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(Level.OVERWORLD, OLD_SITE));
        assertFalse(task.checkExtraStartConditions(level, villager));
        assertEquals(List.of(GlobalPos.of(Level.OVERWORLD, OLD_SITE)), villager.released);
        assertFalse(villager.memory.hasMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE));
    }

    private void bind(BlockPos pos, ResourceKey<Level> dimension) {
        villager.memory.setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(dimension, pos));
    }

    private void runAfterInitialDelay() {
        assertFalse(task.checkExtraStartConditions(level, villager));
        assertEquals(0, pois.validations);
        level.time += 20L;
        assertTrue(task.checkExtraStartConditions(level, villager));
        task.start(level, villager, level.time);
    }

    private static Holder<PoiType> armorerSite() {
        return BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.ARMORER);
    }

    private void capture() throws Exception {
        invokeHook("carpetIceAddition$captureMagmaBlockBreak", false);
    }

    private void finish(boolean successful) throws Exception {
        invokeHook("carpetIceAddition$createLavaSourceAfterBreak", successful);
    }

    private void invokeHook(String name, boolean result) throws Exception {
        Method method = PlayerBreakIceLikeMagmaBlocksMixin.class.getDeclaredMethod(
                name, BlockPos.class, CallbackInfoReturnable.class);
        method.setAccessible(true);
        method.invoke(hooks, BROKEN, new CallbackInfoReturnable<Boolean>("destroyBlock", false, result));
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static final class BreakHooks extends PlayerBreakIceLikeMagmaBlocksMixin {
        BreakHooks(ServerPlayer player) { this.player = player; }
    }

    private static final class FakeLevel extends ServerLevel {
        Map<BlockPos, BlockState> states;
        FakePoiManager pois;
        RandomSource random;
        long time;
        int lavaWrites;

        private FakeLevel() { super(null, null, null, null, null, null, false, 0L, List.of(), false, null); }
        @Override public ResourceKey<Level> dimension() { return Level.OVERWORLD; }
        @Override public RandomSource getRandom() { return random; }
        @Override public long getGameTime() { return time; }
        @Override public PoiManager getPoiManager() { return pois; }
        @Override public BlockState getBlockState(BlockPos pos) { return states.getOrDefault(pos, Blocks.AIR.defaultBlockState()); }
        @Override public boolean setBlockAndUpdate(BlockPos pos, BlockState state) {
            assertEquals(BROKEN, pos);
            assertTrue(state.is(Blocks.LAVA));
            lavaWrites++;
            return true;
        }
    }

    private static final class FakePlayer extends ServerPlayer {
        FakeLevel world;
        ItemStack tool;
        boolean creative;

        private FakePlayer() { super(null, null, null, null); }
        @Override public ServerLevel level() { return world; }
        @Override public ItemStack getMainHandItem() { return tool; }
        @Override public boolean isCreative() { return creative; }
    }

    private static final class FakeVillager extends Villager {
        Brain<Villager> memory;
        List<GlobalPos> released;

        private FakeVillager() { super(null, null); }
        @Override public Brain<Villager> getBrain() { return memory; }
        @Override public boolean isBaby() { return false; }
        @Override public BlockPos blockPosition() { return NEW_SITE; }
        @Override public boolean assignProfessionWhenSpawned() { return false; }
        @Override public void releasePoi(MemoryModuleType<GlobalPos> type) {
            // 若生产代码先 erase 再 release，本断言会失败。
            released.add(memory.getMemory(type).orElseThrow());
        }
    }

    private static final class FakePoiManager extends PoiManager {
        Map<BlockPos, Holder<PoiType>> sites;
        int validations;
        int searches;
        int taken;

        private FakePoiManager() { super(null, null, null, false, null, null, null); }
        @Override public boolean exists(BlockPos pos, Predicate<Holder<PoiType>> predicate) {
            validations++;
            return sites.containsKey(pos) && predicate.test(sites.get(pos));
        }
        @Override public Stream<Pair<Holder<PoiType>, BlockPos>> findAllClosestFirstWithType(
                Predicate<Holder<PoiType>> type, Predicate<BlockPos> position, BlockPos center, int radius, Occupancy occupancy) {
            searches++;
            assertEquals(Occupancy.HAS_SPACE, occupancy);
            return sites.entrySet().stream().filter(entry -> type.test(entry.getValue()) && position.test(entry.getKey()))
                    .map(entry -> Pair.of(entry.getValue(), entry.getKey()));
        }
        @Override public Optional<BlockPos> take(Predicate<Holder<PoiType>> type,
                BiPredicate<Holder<PoiType>, BlockPos> position, BlockPos center, int radius) {
            assertTrue(type.test(sites.get(center)) && position.test(sites.get(center), center));
            taken++;
            return Optional.of(center);
        }
    }
}
