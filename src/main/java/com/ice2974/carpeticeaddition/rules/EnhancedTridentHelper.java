package com.ice2974.carpeticeaddition.rules;

/**
 * Pure-Java geometry and ordering primitives for the {@code enhancedTrident} Carpet rule.
 *
 * <p>This class is intentionally free of Minecraft types so it can be unit-tested by the
 * core-platform test suite ({@code :1.21.11:test}). The Minecraft-facing sweep that feeds
 * these primitives lives in {@link EnhancedTridentSweeper}.
 *
 * <p>The per-candidate test mirrors vanilla {@code ProjectileUtil#getEntityHitResult}
 * (the 7-arg overload used by {@code AbstractArrow#findHitEntity}, bytecode-verified
 * identical across all supported versions): candidates are tested against their
 * margin-inflated AABB via segment entry. Candidates already containing the segment
 * start form group 0 and take precedence — an intentional rule extension over vanilla
 * flight (where {@code AABB#clip} returns empty for a start inside the box, so such
 * candidates are never hit), required so a piston can re-hit an entity the trident
 * never left. Zero-length segments are rejected up front, so a stationary trident
 * never establishes an attack round.
 *
 * <p>Within group 0, hits are ordered by the candidate-box center projected onto the
 * movement direction; within group 1, by the segment entry parameter. Both groups fall
 * back to ascending entity id. These orderings decide which target becomes the head
 * (and therefore which target's vanilla self-motion response is kept), so they must
 * stay deterministic.
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
     * Entry parameter {@code t} in {@code [0, 1]} of segment {@code [s, s + d]} into the
     * AABB, or {@link Double#NaN} when the segment misses the box. A start inside the box
     * yields {@code 0}.
     */
    public static double segmentBoxEntryT(
            double sx, double sy, double sz, double dx, double dy, double dz,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double tMin = 0.0D;
        double tMax = 1.0D;

        if (dx == 0.0D) {
            if (sx < minX || sx > maxX) {
                return Double.NaN;
            }
        } else {
            double t1 = (minX - sx) / dx;
            double t2 = (maxX - sx) / dx;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Double.NaN;
            }
        }

        if (dy == 0.0D) {
            if (sy < minY || sy > maxY) {
                return Double.NaN;
            }
        } else {
            double t1 = (minY - sy) / dy;
            double t2 = (maxY - sy) / dy;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Double.NaN;
            }
        }

        if (dz == 0.0D) {
            if (sz < minZ || sz > maxZ) {
                return Double.NaN;
            }
        } else {
            double t1 = (minZ - sz) / dz;
            double t2 = (maxZ - sz) / dz;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return Double.NaN;
            }
        }

        return tMin;
    }

    public static boolean boxContains(
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double px, double py, double pz) {
        return px >= minX && px <= maxX && py >= minY && py <= maxY && pz >= minZ && pz <= maxZ;
    }

    /**
     * 候选盒中心相对段起点沿段向量 {@code d} 的投影（未归一化：同一次扫掠内所有
     * 候选共享同一 {@code d}，按正数 |d| 缩放不改变全序）。组 0 的组内排序键：
     * 中心更靠移动方向前方的目标排在前。
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
