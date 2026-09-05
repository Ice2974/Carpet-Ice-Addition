//#if MC<260000
package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

final class VillagerIdentity {
    record Identity(Component translated, Component fallback) { }
    private VillagerIdentity() { }
    static Identity create(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        Identifier id = profession.unwrapKey().map(key -> key.identifier()).orElse(null);
        String identifier = id == null ? null : id.toString();
        Component result = villager.isBaby() ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby"))
                : "minecraft:nitwit".equals(identifier) ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit"))
                : "minecraft:none".equals(identifier) ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed"))
                : professionName(id);
        Component fallback = villager.isBaby() || "minecraft:nitwit".equals(identifier) || "minecraft:none".equals(identifier) ? result.copy() : fallbackName(id);
        if (!villager.hasCustomName()) return new Identity(result, fallback);
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return new Identity(named(villager.getCustomName().copy(), result, chinese), fallbackNamed(villager, fallback, chinese));
    }
    private static Component professionName(Identifier id) {
        if (id == null) { VillagerEventsCompatibility.report("identity", new IllegalStateException("Villager profession has no registry key")); return Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")); }
        if (id != null && "minecraft".equals(id.getNamespace())) return Component.translatable("entity.minecraft.villager." + id.getPath());
        return Component.literal(id.toString());
    }
    private static Component fallbackName(Identifier id) { return id == null ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")) : Component.literal(id.toString()); }
    private static Component fallbackNamed(Villager villager, Component value, boolean chinese) {
        Component name = TextRenderer.renderLiteralTree(villager.getCustomName(), java.util.Map.of());
        return name == null ? value : named(name, value, chinese);
    }
    private static Component named(Component name, Component value, boolean chinese) {
        return chinese ? Component.literal("“").append(name).append("”（").append(value).append("）") : Component.literal("\"").append(name).append("\" (").append(value).append(")");
    }
}
//#endif
