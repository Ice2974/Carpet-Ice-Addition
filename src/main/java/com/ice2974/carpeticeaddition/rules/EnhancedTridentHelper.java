package com.ice2974.carpeticeaddition.rules;

/**
 * Pure-Java ordering and parameter primitives for the {@code enhancedTrident} Carpet rule.
 *
 * <p>This class is intentionally free of Minecraft types so it can be unit-tested by the
 * core-platform test suite ({@code :1.21.11:test}). The Minecraft-facing sweep lives in
 * {@link EnhancedTridentSweeper}, which delegates the per-candidate geometry to vanilla
 * {@code AABB} methods (bytecode-verified identical across all supported versions):
 * group 0 (segment start inside the margin-inflated candidate box) via
 * {@code AABB#contains} semantics — implemented here as {@link #boxContains} — and
 * group 1 via a direct {@code AABB#clip(start, end)} call in the Sweeper. Group 0 is an
 * intentional rule extension over vanilla flight (where {@code AABB#clip} returns empty
 * for a start inside the box, so such candidates are never hit), required so a piston can
 * re-hit an entity the trident never left. Zero-length segments are rejected up front,
 * so a stationary trident never establishes an attack round.
 *
 * <p>Orderings must stay deterministic regardless of broadphase iteration order because
 * they decide which target becomes the head (and therefore which target's vanilla
 * self-motion response is kept).
 */
public final class EnhancedTridentHelper {

    /**
     * Segments whose squared length does not exceed this value squared count as zero
     * displacement and never establish an attack round.
     */
    public static final double EPSILON = 1.0E-7;

    private EnhancedTridentHelper() {
    }

    public static boolean isSignificantSegment(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz > EPSILON * EPSILON;
    }

    /**
     * 与 vanilla {@code AABB#contains(double, double, double)} 严格等价的半开区间判定
     * （min 面含、max 面不含：{@code min <= v && v < max}），已对全部受支持版本字节码
     * 核实一致。作为 group-0「start inside」的判定基础；不扩大 vanilla 的边界范围。
     */
    public static boolean boxContains(
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double px, double py, double pz) {
        return px >= minX && px < maxX && py >= minY && py < maxY && pz >= minZ && pz < maxZ;
    }

    /**
     * 候选盒中心相对段起点沿段向量 {@code d} 的投影（未归一化：同一次扫掠内所有
     * 候选共享同一 {@code d}，按正数 |d| 缩放不改变全序）。group-0 的组内排序键：
     * 按投影值升序排列，投影值相同按 entity ID 升序。
     */
    public static double centerProjection(
            double sx, double sy, double sz, double dx, double dy, double dz,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double cx = (minX + maxX) * 0.5D;
        double cy = (minY + maxY) * 0.5D;
        double cz = (minZ + maxZ) * 0.5D;
        return (cx - sx) * dx + (cy - sy) * dy + (cz - sz) * dz;
    }

    /**
     * 点 {@code hit} 沿段 {@code [p, p + d]} 的参数，钳制到 {@code [0, 1]}。用于从
     * vanilla {@code AABB#clip} 返回的命中点反推 group-1 的排序参数 t（vanilla 严格
     * 保证 d ∈ (0, 1)，钳制仅为吸收重投影的浮点过冲），不实现任何 clip 几何。
     */
    public static double paramAlong(
            double px, double py, double pz, double dx, double dy, double dz,
            double hx, double hy, double hz) {
        double lengthSqr = dx * dx + dy * dy + dz * dz;
        if (lengthSqr <= 0.0D) {
            return 0.0D;
        }
        double t = ((hx - px) * dx + (hy - py) * dy + (hz - pz) * dz) / lengthSqr;
        return Math.max(0.0D, Math.min(1.0D, t));
    }

    /**
     * Total order over sweep hits: group 0 (segment start inside the candidate box) first,
     * then ascending within-group sort key (group 0: candidate-box center projected onto
     * the movement direction; group 1: segment entry parameter), ties broken by ascending
     * entity id. Deterministic regardless of broadphase iteration order.
     */
    public static int compareHits(int groupA, double keyA, int idA, int groupB, double keyB, int idB) {
        if (groupA != groupB) {
            return Integer.compare(groupA, groupB);
        }
        int byKey = Double.compare(keyA, keyB);
        if (byKey != 0) {
            return byKey;
        }
        return Integer.compare(idA, idB);
    }
}
