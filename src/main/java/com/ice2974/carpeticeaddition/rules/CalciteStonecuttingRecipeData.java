package com.ice2974.carpeticeaddition.rules;

import java.util.List;

/**
 * {@code calciteStonecuttingRecipe} 的纯字符串数据与内容判定。
 *
 * <p>不依赖任何 Minecraft 类，供各平台冲突检测器与单测复用。
 *
 * <p>规则承诺：使用切石机把 1 个滴水石块加工为 1 个方解石。因此「本模组配方 id 存在」并不足以证明
 * 规则能力成立——外部数据包可以用同一路径覆盖成别的配方。{@link #ownRecipeAcceptable} 把该判定
 * 下成纯函数，覆盖「类型被换」「产物被换」「数量被改」「原料被改（含原料不再接受滴水石块）」。
 */
public final class CalciteStonecuttingRecipeData {
    /** 配方 namespace，固定为 mod id。 */
    public static final String NAMESPACE = "carpet-ice-addition";

    /** 切石配方 path；与内置数据包内的文件名、以及 build.gradle 的校验清单一致。 */
    public static final String RECIPE_PATH = "calcite_from_dripstone_block_stonecutting";

    /** 本规则只注册 1 条配方。 */
    public static final List<String> RECIPE_PATHS = List.of(RECIPE_PATH);

    /** 产物：方解石。 */
    public static final String RESULT_ITEM_ID = "minecraft:calcite";

    /** 产物数量：1（规则文档承诺 1:1）。 */
    public static final int RESULT_COUNT = 1;

    /** 原料：滴水石块。 */
    public static final String INGREDIENT_ITEM_ID = "minecraft:dripstone_block";

    /**
     * 冲突检测的权威目标集合：不依赖运行期本模组配方是否注册成功，
     * 避免配方缺失 / 被覆盖 / 加载异常时冲突检测失效。
     */
    public static final List<String> RESULT_ITEM_IDS = List.of(RESULT_ITEM_ID);

    private CalciteStonecuttingRecipeData() {
    }

    /** 判定某个 recipe id 字符串（形如 {@code namespace:path}）是否为本规则注册的配方。 */
    public static boolean isOwnRecipeId(String id) {
        if (id == null) {
            return false;
        }
        int separator = id.indexOf(':');
        if (separator <= 0 || separator >= id.length() - 1) {
            return false;
        }
        return isOwnRecipe(id.substring(0, separator), id.substring(separator + 1));
    }

    public static boolean isOwnRecipe(String namespace, String path) {
        if (!NAMESPACE.equals(namespace)) {
            return false;
        }
        // RECIPE_PATHS 是不可变列表，contains(null) 会抛 NPE：显式守卫，使该判定对 null 全定义。
        return path != null && RECIPE_PATHS.contains(path);
    }

    /**
     * 纯决策：本模组配方 id 所处的配方内容是否符合本规则承诺。
     *
     * @param stonecuttingType          配方类型是否为 {@code minecraft:stonecutting}
     * @param resultItemId              解析出的产物物品 id
     * @param resultCount               解析出的产物数量
     * @param ingredientAcceptsDripstone 原料是否接受 {@code minecraft:dripstone_block}
     * @return true 表示内容与本规则承诺一致；false 表示同 id 被覆盖或内容不符（应判为冲突）
     */
    public static boolean ownRecipeAcceptable(
            boolean stonecuttingType, String resultItemId, int resultCount, boolean ingredientAcceptsDripstone) {
        return stonecuttingType
                && RESULT_ITEM_ID.equals(resultItemId)
                && resultCount == RESULT_COUNT
                && ingredientAcceptsDripstone;
    }
}
