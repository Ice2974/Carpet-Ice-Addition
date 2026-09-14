package com.ice2974.carpeticeaddition.rules;

/**
 * betterTridentDespawnCondition 规则的静止计时纯逻辑（无 Minecraft 类型，由 core
 * 平台 {@code :1.21.11:test} 单测覆盖）；Mixin 侧的采样 / 重置入口见
 * {@link BetterTridentDespawnState} duck 接口。
 *
 * <p>「移动」判定为坐标数值精确相等/不等（{@code ==} / {@code !=}，不使用 epsilon；
 * +0.0 与 -0.0 数值相等，不视为移动）：已对全部受支持版本核实，插地且无外力时
 * 三叉戟整条 tick 链（含 1.21.1 的 baseTick 路径）不写位置坐标，任何真实坐标数值
 * 变化都是实际位移。anchor 只在建立 / 位移 / 代际失效时改写为新坐标，连续静止
 * 采样期间数值恒定不变，不存在「逐采样前移基准」导致的累积微位移漏检。
 *
 * <p>计数语义：采样点 = 服务端插地 tick 的 {@code tickDespawn} 调用。anchor 建立、
 * 位移检出、代际失效后的<strong>首次有效静止采样计 1</strong>（与落地 tick 的
 * vanilla {@code life} 计数对齐：无扰动时静止阈值与 {@code life >= 1200} 在同一
 * tick 达成），此后每次数值相等的采样 +1，在
 * {@link #STATIONARY_TICKS_REQUIRED} 处饱和（该计数仅用于 ≥ 阈值判定）。
 */
public final class BetterTridentDespawnTracker {

    /** 允许消失所需的连续有效静止采样数。 */
    public static final int STATIONARY_TICKS_REQUIRED = 1200;

    /**
     * vanilla {@code AbstractArrow#tickDespawn} 的 despawn 阈值。与该方法体结构
     * （{@code life++; if (life >= 1200) discard();}，方法内唯一一处 discard 调用
     * 与唯一一处 1200 常量）同为对当前全部受支持版本逐一字节码核实的不变式；
     * <strong>未来新增或移植 Minecraft 版本时必须重新核实两者，不得假设继续成立</strong>。
     */
    public static final int VANILLA_DESPAWN_THRESHOLD = 1200;

    private BetterTridentDespawnTracker() {
    }

    /** 坐标数值精确相等（+0.0 与 -0.0 相等；无 epsilon）。 */
    public static boolean samePosition(double ax, double ay, double az, double x, double y, double z) {
        return x == ax && y == ay && z == az;
    }

    /**
     * 以当前 {@code life} 值原样执行 vanilla tickDespawn 是否不会触发 discard（life++ 后仍低于阈值）。
     * 写作 {@code life < 阈值 - 1} 而非 {@code life + 1 < 阈值}：后者在 {@code life} 接近
     * {@code Integer.MAX_VALUE} 时自增溢出为负、错误判定直通 vanilla，恰好重开本规则要关闭的
     * 溢出链；{@code 阈值 - 1} 为编译期常量，任何 int 输入都无溢出路径。
     */
    public static boolean vanillaWouldNotDiscard(int life) {
        return life < VANILLA_DESPAWN_THRESHOLD - 1;
    }

    /**
     * 扣留 tick 的 life 推进：饱和于 {@link #VANILLA_DESPAWN_THRESHOLD}。vanilla 生命期内
     * {@code life} 达到阈值即 discard、永不自增超过 1200，无界增长只能来自本规则的扣留复刻——
     * 不饱和会把 vanilla 以 {@code (short)} 序列化的该字段推出 NBT 表示范围（超过 32767 后
     * 保存/重载回绕为负，重载后需约 32768+ tick 才恢复 despawn 资格）。先判断后自增：
     * {@code Integer.MAX_VALUE} 不执行 {@code + 1} 运算，无 int 溢出。
     */
    public static int detainedLifeAdvance(int life) {
        return life >= VANILLA_DESPAWN_THRESHOLD ? VANILLA_DESPAWN_THRESHOLD : life + 1;
    }

    public static boolean isDespawnPermitted(int stationaryTicks) {
        return stationaryTicks >= STATIONARY_TICKS_REQUIRED;
    }

    /**
     * 静止采样状态机。分支优先级：代际失效 / anchor 未建立 / 数值不等（位移检出）
     * 均以当前采样建立新 anchor 并计 1；数值相等（且代际一致、anchor 有效）才延续
     * 计数。{@link #onDisplacement()}（move() 外力位移观测与 startFalling 离地入口
     * 共用）使 anchor 失效、计数清零，后续首次有效静止采样重新从 1 计数——与落地
     * 语义完全对称。
     */
    public static final class State {

        boolean hasAnchor;
        double anchorX;
        double anchorY;
        double anchorZ;
        int stationaryTicks;
        int epochGeneration = -1;

        public void onGroundedSample(double x, double y, double z, int currentEpoch) {
            boolean continuesStationaryRun = this.hasAnchor
                    && this.epochGeneration == currentEpoch
                    && BetterTridentDespawnTracker.samePosition(this.anchorX, this.anchorY, this.anchorZ, x, y, z);
            this.anchorX = x;
            this.anchorY = y;
            this.anchorZ = z;
            this.hasAnchor = true;
            this.epochGeneration = currentEpoch;
            this.stationaryTicks = continuesStationaryRun
                    ? Math.min(this.stationaryTicks + 1, STATIONARY_TICKS_REQUIRED)
                    : 1;
        }

        public void onDisplacement() {
            this.hasAnchor = false;
            this.stationaryTicks = 0;
        }

        public int stationaryTicks() {
            return this.stationaryTicks;
        }

        public boolean isDespawnPermitted() {
            return this.stationaryTicks >= STATIONARY_TICKS_REQUIRED;
        }
    }
}
