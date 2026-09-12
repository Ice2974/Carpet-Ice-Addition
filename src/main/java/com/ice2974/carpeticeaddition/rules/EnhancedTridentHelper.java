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
     * Total order over sweep hits: group 0 (segment start inside the candidate box) first,
     * then ascending entry parameter, ties broken by ascending entity id. Deterministic
     regardless of broadphase iteration order.
     */
    public static int compareHits(int groupA, double tA, int idA, int groupB, double tB, int idB) {
        if (groupA != groupB) {
            return Integer.compare(groupA, groupB);
        }
        int byT = Double.compare(tA, tB);
        if (byT != 0) {
            return byT;
        }
        return Integer.compare(idA, idB);
    }

    /**
     * Parameter of point {@code hit} along segment {@code [p, p + d]}, clamped to
     * {@code [0, 1]}. Used to trim sweep candidates that lie behind the first block face
     * between the segment ends.
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
}
