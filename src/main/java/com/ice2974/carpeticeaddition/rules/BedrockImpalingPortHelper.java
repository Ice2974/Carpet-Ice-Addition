package com.ice2974.carpeticeaddition.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
//#if MC>=260200
//$$import net.minecraft.advancements.predicates.entity.EntityPredicate;
//#elseif MC>=12111
import net.minecraft.advancements.criterion.EntityPredicate;
//#else
//$$import net.minecraft.advancements.critereon.EntityPredicate;
//#endif

import java.util.List;
import java.util.Optional;

/**
 * bedrockImpalingPort 规则的判定逻辑。
 *
 * <p>规则开启时，把 vanilla Impaling 的目标条件从 {@code #minecraft:sensitive_to_impaling}
 * 替换为「目标处于水中或正在被雨淋」，即 {@link Entity#isInWaterOrRain()}。这是对原版目标
 * 判定的替换而非追加：干燥的原版水生实体不再获得额外伤害，水中或雨中的任意实体（含非水生
 * 实体与玩家）获得额外伤害。
 *
 * <p>实现方式是替换 {@code Enchantment#modifyDamageFilteredValue} 内部
 * {@code Enchantment#getEffects(minecraft:damage)} 的返回值（见 BedrockImpalingPortMixin），
 * 三态语义见 {@link Decision}。伤害数值始终由原版 {@code applyEffects} /
 * {@code EnchantmentValueEffect} / {@code LevelBasedValue} 与原 {@code MutableFloat} 处理，
 * 本类不计算也不补减 {@code 2.5 × level}。
 *
 * <p>只有在严格识别门（{@link #decide}）全部通过时才介入；任何未知的数据包形状、
 * 额外 requirements、额外 predicate 字段、第二个 damage effect、编码失败或其它无法确认的
 * 情况一律 fail-open，返回原 effects list，完整保留原版与数据包行为。
 */
public final class BedrockImpalingPortHelper {

    /** 三态语义：动作只有「不介入 / 施加 / 跳过」，避免用布尔量混淆 wet/dry 与 cancel。 */
    public enum Decision {
        /** 不介入：返回原 effects list，原版与数据包逻辑完整执行。 */
        VANILLA,
        /** 严格识别成功且目标在水中或雨中：移除 requirements，保留原 effect 对象。 */
        APPLY,
        /** 严格识别成功且目标干燥：返回空 list，不施加该效果。 */
        SKIP
    }

    /**
     * vanilla Impaling 目标条件中「实体类型」字段在 EntityPredicate JSON 中的键名。
     * 1.21.1～26.1.2 为 {@code type}；26.2 改为 {@code minecraft:entity_type}。
     * 按版本精确匹配，不为猜测未来格式而放宽。
     */
//#if MC>=260200
//$$    private static final String IMPALING_ENTITY_TYPE_KEY = "minecraft:entity_type";
//#else
    private static final String IMPALING_ENTITY_TYPE_KEY = "type";
//#endif

    /** vanilla Impaling 目标条件中该字段的值，形如 {@code #minecraft:sensitive_to_impaling}。 */
    private static final String IMPALING_TAG_REFERENCE =
            "#" + EntityTypeTags.SENSITIVE_TO_IMPALING.location();

    private BedrockImpalingPortHelper() {
    }

    /**
     * 基于原版 {@code getEffects} 的返回值做识别与三态转换。
     *
     * <p>调用方必须已经以原参数调用过一次原 {@code getEffects}，并把结果作为
     * {@code originalEffects} 传入；本方法不会再触发任何原版调用。
     *
     * @return 交给后续 {@code applyEffects} 的 effects list；无法确认时返回 {@code originalEffects} 本身
     */
    public static List<?> resolve(
            List<?> originalEffects,
            Enchantment self,
            DataComponentType<?> componentType,
            ServerLevel level,
            Entity target) {
        Decision decision = decide(originalEffects, self, componentType, level, target);
        if (decision == Decision.VANILLA) {
            return originalEffects;
        }
        if (decision == Decision.APPLY) {
            ConditionalEffect<?> only = (ConditionalEffect<?>) originalEffects.get(0);
            return List.of(new ConditionalEffect<>(only.effect(), Optional.empty()));
        }
        return List.of();
    }

    /**
     * 严格识别门 + wet/dry 判定。只有在全部条件成立时才返回 {@link Decision#APPLY} /
     * {@link Decision#SKIP}，否则一律 {@link Decision#VANILLA}。
     */
    private static Decision decide(
            List<?> originalEffects,
            Enchantment self,
            DataComponentType<?> componentType,
            ServerLevel level,
            Entity target) {
        if (!CarpetIceAdditionSettings.bedrockImpalingPort) {
            return Decision.VANILLA;
        }
        if (componentType != EnchantmentEffectComponents.DAMAGE) {
            return Decision.VANILLA;
        }
        if (level == null) {
            return Decision.VANILLA;
        }

        try {
            if (self != resolveImpaling(level)) {
                return Decision.VANILLA;
            }
            if (originalEffects == null || originalEffects.size() != 1) {
                return Decision.VANILLA;
            }
            Object only = originalEffects.get(0);
            if (!(only instanceof ConditionalEffect<?> effect)) {
                return Decision.VANILLA;
            }
            if (!isVanillaImpalingTargetCondition(effect, level)) {
                return Decision.VANILLA;
            }
            if (target == null) {
                return Decision.VANILLA;
            }
            return target.isInWaterOrRain() ? Decision.APPLY : Decision.SKIP;
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("bedrockImpalingPort", throwable);
            return Decision.VANILLA;
        }
    }

    /** 取当前注册表中的 {@code minecraft:impaling}。 */
    private static Enchantment resolveImpaling(ServerLevel level) {
//#if MC<12103
//$$        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
//$$                .getHolderOrThrow(Enchantments.IMPALING).value();
//#else
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getValueOrThrow(Enchantments.IMPALING);
//#endif
    }

    /**
     * 严格识别：requirements 恰为一个指向「被打目标」的实体属性条件，且该条件只设置了实体类型，
     * 且该类型精确等于 vanilla 的 {@code #minecraft:sensitive_to_impaling}。
     *
     * <p>使用 registry-aware 序列化上下文（{@code RegistryOps}）编码 EntityPredicate：
     * 其 CODEC 基于 {@code RegistryCodecs.homogeneousList}，底层 RegistryFileCodec /
     * HolderSetCodec 要求 {@code RegistryOps}，裸 {@code JsonOps} 会直接编码失败。
     */
    private static boolean isVanillaImpalingTargetCondition(ConditionalEffect<?> effect, ServerLevel level) {
        Optional<LootItemCondition> requirements = effect.requirements();
        if (requirements.isEmpty()) {
            return false;
        }
        if (!(requirements.get() instanceof LootItemEntityPropertyCondition condition)) {
            return false;
        }
        if (condition.entityTarget() != LootContext.EntityTarget.THIS) {
            return false;
        }
        Optional<EntityPredicate> predicate = condition.predicate();
        if (predicate.isEmpty()) {
            return false;
        }

        RegistryOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonElement encoded = EntityPredicate.CODEC.encodeStart(ops, predicate.get()).result().orElse(null);
        if (encoded == null || !encoded.isJsonObject()) {
            return false;
        }
        JsonObject object = encoded.getAsJsonObject();
        if (object.size() != 1) {
            return false;
        }
        JsonElement value = object.get(IMPALING_ENTITY_TYPE_KEY);
        if (value == null || !value.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        return primitive.isString() && IMPALING_TAG_REFERENCE.equals(primitive.getAsString());
    }
}
