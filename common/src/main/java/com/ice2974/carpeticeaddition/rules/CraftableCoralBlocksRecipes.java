package com.ice2974.carpeticeaddition.rules;

import java.util.List;

/**
 * 纯字符串数据：珊瑚块配方 id 表与判定。不依赖任何 Minecraft 类，
 * 供各平台 Mixin（B2/B3 过滤）与配方书同步 Helper 复用。
 *
 * 配方 namespace 固定为 {@code carpet-ice-addition}（与 mod id 一致），
 * path 覆盖 10 种珊瑚扇（活 + 死）。
 */
public final class CraftableCoralBlocksRecipes {
    public static final String NAMESPACE = "carpet-ice-addition";

    public static final List<String> RECIPE_PATHS = List.of(
            "coral_block_from_tube_coral_fan",
            "coral_block_from_brain_coral_fan",
            "coral_block_from_bubble_coral_fan",
            "coral_block_from_fire_coral_fan",
            "coral_block_from_horn_coral_fan",
            "coral_block_from_dead_tube_coral_fan",
            "coral_block_from_dead_brain_coral_fan",
            "coral_block_from_dead_bubble_coral_fan",
            "coral_block_from_dead_fire_coral_fan",
            "coral_block_from_dead_horn_coral_fan"
    );

    /**
     * 10 条珊瑚块配方对应的输出产物 id（{@code minecraft:...}）。
     *
     * <p>作为冲突检测的权威目标集合：不依赖运行期 RecipeManager 中本模组 recipe 是否成功注册，
     * 避免本模组 recipe 缺失 / 被覆盖 / 加载异常时冲突检测失效。平台层据此解析为 Item 后与外部
     * recipe 的输出产物比较。
     */
    public static final List<String> RESULT_ITEM_IDS = List.of(
            "minecraft:tube_coral_block",
            "minecraft:brain_coral_block",
            "minecraft:bubble_coral_block",
            "minecraft:fire_coral_block",
            "minecraft:horn_coral_block",
            "minecraft:dead_tube_coral_block",
            "minecraft:dead_brain_coral_block",
            "minecraft:dead_bubble_coral_block",
            "minecraft:dead_fire_coral_block",
            "minecraft:dead_horn_coral_block"
    );

    private CraftableCoralBlocksRecipes() {
    }

    /**
     * 判定某个 recipe id 字符串（形如 {@code namespace:path}）是否为本规则注册的珊瑚块配方。
     * 只认 {@code carpet-ice-addition} namespace 下 10 个固定 path，避免误伤其他 datapack / Mod。
     */
    public static boolean isCoralRecipeId(String id) {
        if (id == null) {
            return false;
        }
        int separator = id.indexOf(':');
        if (separator <= 0 || separator >= id.length() - 1) {
            return false;
        }
        return isCoralRecipe(id.substring(0, separator), id.substring(separator + 1));
    }

    public static boolean isCoralRecipe(String namespace, String path) {
        return NAMESPACE.equals(namespace) && RECIPE_PATHS.contains(path);
    }
}
