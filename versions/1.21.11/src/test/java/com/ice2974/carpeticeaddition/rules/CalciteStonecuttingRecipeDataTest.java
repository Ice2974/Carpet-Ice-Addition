package com.ice2974.carpeticeaddition.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CalciteStonecuttingRecipeData} 的配方内容判定、recipe id 边界与打包数据一致性测试。
 *
 * <p>该纯类是冲突检测器信任的「规则承诺」来源：id 存在并不等于规则能力成立，外部数据包可以用同一
 * 路径覆盖成别的内容。这里同时验证 (1) 内容判定真值表、(2) id 解析边界（含 null 全定义）、
 * (3) 实际打包进 jar 的配方 JSON 与常量一致——否则冲突检测器会把本模组配方自身判为冲突。
 *
 * <p>打包数据读取依赖测试运行期 classpath 上的 {@code main} 资源（{@code processResources} 产物）。
 * 资源缺失时直接失败（不静默跳过），以免覆盖被伪装成通过。
 */
class CalciteStonecuttingRecipeDataTest {
    private static final String OWN_RECIPE_ID =
            CalciteStonecuttingRecipeData.NAMESPACE + ":" + CalciteStonecuttingRecipeData.RECIPE_PATH;
    private static final String RECIPE_RESOURCE =
            "resourcepacks/calcite_stonecutting/data/" + CalciteStonecuttingRecipeData.NAMESPACE
                    + "/recipe/" + CalciteStonecuttingRecipeData.RECIPE_PATH + ".json";
    private static final String PACK_MCMETA_RESOURCE = "resourcepacks/calcite_stonecutting/pack.mcmeta";

    // ---------------------------------------------------------------- 常量契约

    @Test
    void constantsMatchRuleContract() {
        assertEquals("carpet-ice-addition", CalciteStonecuttingRecipeData.NAMESPACE);
        assertEquals("calcite_from_dripstone_block_stonecutting", CalciteStonecuttingRecipeData.RECIPE_PATH);
        assertEquals(List.of(CalciteStonecuttingRecipeData.RECIPE_PATH), CalciteStonecuttingRecipeData.RECIPE_PATHS,
                "本规则只注册 1 条配方");
        assertEquals("minecraft:calcite", CalciteStonecuttingRecipeData.RESULT_ITEM_ID);
        assertEquals(1, CalciteStonecuttingRecipeData.RESULT_COUNT, "规则承诺 1:1");
        assertEquals("minecraft:dripstone_block", CalciteStonecuttingRecipeData.INGREDIENT_ITEM_ID);
        assertEquals(List.of(CalciteStonecuttingRecipeData.RESULT_ITEM_ID), CalciteStonecuttingRecipeData.RESULT_ITEM_IDS,
                "冲突检测的权威目标集合必须与产物 id 一致");
    }

    // ---------------------------------------------------------------- 内容判定

    @Test
    void ownRecipeAcceptableRequiresAllFourConditions() {
        assertTrue(CalciteStonecuttingRecipeData.ownRecipeAcceptable(true, "minecraft:calcite", 1, true));

        assertFalse(CalciteStonecuttingRecipeData.ownRecipeAcceptable(false, "minecraft:calcite", 1, true),
                "类型不是切石 ⇒ 不可接受");
        assertFalse(CalciteStonecuttingRecipeData.ownRecipeAcceptable(true, "minecraft:quartz", 1, true),
                "产物被换 ⇒ 不可接受");
        assertFalse(CalciteStonecuttingRecipeData.ownRecipeAcceptable(true, null, 1, true),
                "产物无法解析 ⇒ 不可接受");
        assertFalse(CalciteStonecuttingRecipeData.ownRecipeAcceptable(true, "minecraft:calcite", 0, true),
                "数量 0 ⇒ 不可接受");
        assertFalse(CalciteStonecuttingRecipeData.ownRecipeAcceptable(true, "minecraft:calcite", 2, true),
                "数量被改 ⇒ 不可接受");
        assertFalse(CalciteStonecuttingRecipeData.ownRecipeAcceptable(true, "minecraft:calcite", -1, true),
                "负数数量 ⇒ 不可接受");
        assertFalse(CalciteStonecuttingRecipeData.ownRecipeAcceptable(true, "minecraft:calcite", 1, false),
                "原料不再接受滴水石块 ⇒ 不可接受");
    }

    // ---------------------------------------------------------------- recipe id 边界

    @Test
    void ownRecipeIdAcceptsExactId() {
        assertTrue(CalciteStonecuttingRecipeData.isOwnRecipeId(OWN_RECIPE_ID));
        assertTrue(CalciteStonecuttingRecipeData.isOwnRecipe(
                CalciteStonecuttingRecipeData.NAMESPACE, CalciteStonecuttingRecipeData.RECIPE_PATH));
    }

    @Test
    void ownRecipeIdRejectsMalformedAndForeignIds() {
        for (String id : new String[] {
                "",
                ":",
                "carpet-ice-addition:",
                ":calcite_from_dripstone_block_stonecutting",
                "minecraft:calcite_from_dripstone_block_stonecutting",
                "carpet-ice-addition:other",
                "carpet-ice-addition:Calcite_From_Dripstone_Block_Stonecutting",
                "Carpet-Ice-Addition:calcite_from_dripstone_block_stonecutting",
                " carpet-ice-addition:calcite_from_dripstone_block_stonecutting",
                "carpet-ice-addition:calcite_from_dripstone_block_stonecutting ",
                "carpet-ice-addition:calcite_from_dripstone_block_stonecutting:extra",
                "carpet-ice-addition:calcite_from_dripstone_block_stonecutting.json"}) {
            assertFalse(CalciteStonecuttingRecipeData.isOwnRecipeId(id), "必须拒绝非本模组精确 id：" + id);
        }
    }

    @Test
    void ownRecipeIdIsTotalForNullInputs() {
        assertFalse(CalciteStonecuttingRecipeData.isOwnRecipeId(null));
        assertFalse(CalciteStonecuttingRecipeData.isOwnRecipe(null, null));
        assertFalse(CalciteStonecuttingRecipeData.isOwnRecipe(null, CalciteStonecuttingRecipeData.RECIPE_PATH));
        assertFalse(CalciteStonecuttingRecipeData.isOwnRecipe(CalciteStonecuttingRecipeData.NAMESPACE, null),
                "null path 必须是 false 而不是 NPE");
    }

    // ---------------------------------------------------------------- 打包数据一致性

    @Test
    void packagedRecipeMatchesRuleContract() {
        JsonObject recipe = readJson(RECIPE_RESOURCE);

        assertEquals("minecraft:stonecutting", recipe.get("type").getAsString(),
                "规则只承诺切石配方：" + RECIPE_RESOURCE);

        JsonElement ingredientElement = recipe.get("ingredient");
        String ingredientId = itemIdOf(ingredientElement);
        assertEquals(CalciteStonecuttingRecipeData.INGREDIENT_ITEM_ID, ingredientId,
                "打包配方的原料必须与常量一致（冲突检测器按常量判定内容）");

        JsonElement resultElement = recipe.get("result");
        String resultId = itemIdOf(resultElement);
        int resultCount = countOf(resultElement);
        assertEquals(CalciteStonecuttingRecipeData.RESULT_ITEM_ID, resultId,
                "打包配方的产物必须与常量一致");
        assertEquals(CalciteStonecuttingRecipeData.RESULT_COUNT, resultCount,
                "打包配方的产物数量必须与常量一致（规则承诺 1:1）");

        assertTrue(CalciteStonecuttingRecipeData.ownRecipeAcceptable(
                        true, resultId, resultCount, CalciteStonecuttingRecipeData.INGREDIENT_ITEM_ID.equals(ingredientId)),
                "打包配方必须满足 ownRecipeAcceptable，否则冲突检测器会把本模组配方自身判为冲突");
    }

    @Test
    void packagedPackMetaIsPresentAndWellFormed() {
        JsonObject meta = readJson(PACK_MCMETA_RESOURCE);

        assertTrue(meta.has("pack"), "内置数据包必须声明 pack 段：" + PACK_MCMETA_RESOURCE);
        JsonObject pack = meta.getAsJsonObject("pack");
        assertTrue(pack.has("pack_format"), "必须声明 pack_format：" + pack);
        assertTrue(pack.get("pack_format").getAsInt() > 0, "pack_format 必须是正整数：" + pack);
    }

    // ---------------------------------------------------------------- 辅助

    /** 读取打包资源为 JSON 对象；资源缺失直接失败（不得静默跳过）。 */
    private static JsonObject readJson(String resourcePath) {
        ClassLoader loader = CalciteStonecuttingRecipeDataTest.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "测试运行期 classpath 上缺少打包资源（processResources 产物）：" + resourcePath);
            JsonElement element = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertTrue(element.isJsonObject(), "资源必须是 JSON 对象：" + resourcePath);
            return element.getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("读取打包资源失败：" + resourcePath, exception);
        }
    }

    /**
     * 从「裸物品 id」「{@code item} 对象」「{@code id} 对象」三种写法中取出物品 id。
     *
     * <p>1.21.1 的 old schema 用 {@code item} 键、1.21.3+ 用裸字符串 / {@code id} 键；
     * 两者都要能解析，避免测试与文件形态耦合。
     */
    private static String itemIdOf(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("item")) {
            return itemIdOf(object.get("item"));
        }
        if (object.has("id")) {
            return itemIdOf(object.get("id"));
        }
        return null;
    }

    /** 产物数量：声明了 {@code count} 就取之，否则按 vanilla 语义默认 1。 */
    private static int countOf(JsonElement resultElement) {
        if (resultElement != null && resultElement.isJsonObject()) {
            JsonObject result = resultElement.getAsJsonObject();
            if (result.has("count")) {
                return result.get("count").getAsInt();
            }
        }
        return 1;
    }
}
