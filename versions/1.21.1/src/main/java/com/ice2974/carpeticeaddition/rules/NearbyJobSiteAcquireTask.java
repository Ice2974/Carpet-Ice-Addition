package com.ice2974.carpeticeaddition.rules;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.function.Predicate;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 近距离工作站认领与 POTENTIAL_JOB_SITE 清理任务（MC 1.21 ~ 1.21.1，旧任务名 + VillagerProfession 参数）。
 * villagerTradingOptimization 的无寻路职业链入口，双分支结构，不以 POTENTIAL_JOB_SITE 缺失作为任务级门控：
 * - POTENTIAL_JOB_SITE 缺失且 JOB_SITE 缺失（村民失站或失业）时，按原版 FindPointOfInterestTask 的
 *   20~40 tick 随机周期，在补货判定距离（调用与 VillagerWorkTask 相同的 BlockPos.isWithinDistance(Vec3d, double)，
 *   约 1.73 格）内搜索有空闲票据的工作站，经 PointOfInterestStorage.getPosition（含占票）确认后写入
 *   POTENTIAL_JOB_SITE，交由原版 GoToWorkTask 完成 POTENTIAL_JOB_SITE → JOB_SITE 转换与职业赋值；
 *   职业谓词沿用原版 acquirableWorkstation：失业村民匹配全部可获取工作站，就业村民只匹配本职业工作站，
 *   傻瓜村民（NITWIT）谓词恒不匹配；
 * - POTENTIAL_JOB_SITE 存在但已超出原版转换距离（2.0 格）或跨维度时（村民被水流、活塞、载具等移离），
 *   先经原版 releaseTicketFor 释放票据，再清除记忆，避免票据与 POTENTIAL_JOB_SITE 永久卡住
 *   （原版依赖已被本规则删除的 WalkTowardJobSiteTask 超时释放）。
 * 已绑定 JOB_SITE 的健康村民每次仅做两次记忆存在性检查即短路，不产生 POI 查询；
 * 节流状态仅保存在任务实例字段中，随 Brain 重建重置，不写 NBT 或其他持久化数据。
 */
public class NearbyJobSiteAcquireTask extends Behavior<Villager> {
    /**
     * 与 VillagerWorkTask.MAX_DISTANCE（私有常量，值 1.73）同一数值与距离方法，
     * 保证能认领的工作站必在补货判定距离内。
     */
    private static final double WORK_DISTANCE = 1.73D;
    /** 与原版 GoToWorkTask 的 POTENTIAL_JOB_SITE → JOB_SITE 转换距离阈值一致。 */
    private static final double CONVERSION_DISTANCE = 2.0D;
    /** 与原版 FindPointOfInterestTask 相同的 20~40 tick 随机重试周期。 */
    private static final long RETRY_INTERVAL = 20L;
    private static final int RETRY_INTERVAL_JITTER = 20;

    private final Predicate<Holder<PoiType>> workstationPredicate;
    private long nextCheckTime;

    public NearbyJobSiteAcquireTask(VillagerProfession profession) {
        super(ImmutableMap.of());
        this.workstationPredicate = profession.acquirableJobSite();
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Villager entity) {
        if (entity.getBrain().hasMemoryValue(MemoryModuleType.POTENTIAL_JOB_SITE)) {
            this.cleanUpDisplacedPotentialSite(world, entity);
            return false;
        }
        if (entity.isBaby() || entity.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE)) {
            return false;
        }
        if (this.nextCheckTime == 0L) {
            this.nextCheckTime = world.getGameTime() + world.getRandom().nextInt(RETRY_INTERVAL_JITTER);
            return false;
        }
        return world.getGameTime() >= this.nextCheckTime;
    }

    @Override
    protected void start(ServerLevel world, Villager entity, long time) {
        this.nextCheckTime = time + RETRY_INTERVAL + world.getRandom().nextInt(RETRY_INTERVAL_JITTER);
        PoiManager poiStorage = world.getPoiManager();
        poiStorage.findAllClosestFirstWithType(
                        this.workstationPredicate, pos -> true, entity.blockPosition(), 2, PoiManager.Occupancy.HAS_SPACE)
                .map(Pair::getSecond)
                .filter(pos -> pos.closerToCenterThan(entity.position(), WORK_DISTANCE))
                .filter(pos -> poiStorage.take(this.workstationPredicate, (entry, blockPos) -> blockPos.equals(pos), pos, 1).isPresent())
                .findFirst()
                .ifPresent(pos -> entity.getBrain()
                        .setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(world.dimension(), pos)));
    }

    private void cleanUpDisplacedPotentialSite(ServerLevel world, Villager entity) {
        entity.getBrain()
                .getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                .filter(pos -> pos.dimension() != world.dimension()
                        || (!pos.pos().closerToCenterThan(entity.position(), CONVERSION_DISTANCE) && !entity.assignProfessionWhenSpawned()))
                .ifPresent(pos -> {
                    entity.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
                    entity.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
                });
    }
}
