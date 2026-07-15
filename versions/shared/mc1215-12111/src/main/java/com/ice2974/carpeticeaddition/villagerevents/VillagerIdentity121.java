package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;

final class VillagerIdentity121 {
    record Identity(Text translated, Text fallback) { }
    private VillagerIdentity121() { }
    static Identity create(VillagerEntity villager) {
        RegistryEntry<VillagerProfession> profession = villager.getVillagerData().profession();
        Identifier id = profession.getKey().map(key -> key.getValue()).orElse(null);
        String identifier = id == null ? null : id.toString();
        Text result = villager.isBaby() ? Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby"))
                : "minecraft:nitwit".equals(identifier) ? Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit"))
                : "minecraft:none".equals(identifier) ? Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed"))
                : professionName(id);
        Text fallback = villager.isBaby() || "minecraft:nitwit".equals(identifier) || "minecraft:none".equals(identifier) ? result.copy() : fallbackName(id);
        if (!villager.hasCustomName()) return new Identity(result, fallback);
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return new Identity(named(villager.getCustomName().copy(), result, chinese), fallbackNamed(villager, fallback, chinese));
    }
    private static Text professionName(Identifier id) {
        if (id == null) { VillagerEventsCompatibility.report("identity", new IllegalStateException("Villager profession has no registry key")); return Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")); }
        if (id != null && "minecraft".equals(id.getNamespace())) return Text.translatable("entity.minecraft.villager." + id.getPath());
        return Text.literal(id.toString());
    }
    private static Text fallbackName(Identifier id) { return id == null ? Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")) : Text.literal(id.toString()); }
    private static Text fallbackNamed(VillagerEntity villager, Text value, boolean chinese) {
        Text name = TextRenderer121.renderLiteralTree(villager.getCustomName(), java.util.Map.of());
        return name == null ? value : named(name, value, chinese);
    }
    private static Text named(Text name, Text value, boolean chinese) {
        return chinese ? Text.literal("“").append(name).append("”（").append(value).append("）") : Text.literal("\"").append(name).append("\" (").append(value).append(")");
    }
}
