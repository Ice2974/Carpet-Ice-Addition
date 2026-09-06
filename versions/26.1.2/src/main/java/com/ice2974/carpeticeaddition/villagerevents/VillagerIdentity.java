package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

/** Produces only literal identity components, so logger output never asks a client to translate it. */
final class VillagerIdentity {
    record Identity(Component translated, Component fallback) { }
    private VillagerIdentity() { }

    static Identity create(Villager villager) {
        String identity;
        if (villager.isBaby()) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby");
        else {
            var profession = villager.getVillagerData().profession();
            Identifier id = profession.unwrapKey().map(ResourceKey::identifier).orElse(null);
            String identifier = id == null ? null : id.toString();
            if ("minecraft:nitwit".equals(identifier)) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit");
            else if ("minecraft:none".equals(identifier)) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed");
            else return named(villager, professionName(id), fallbackName(id));
        }
        Component value = Component.literal(identity);
        return named(villager, value, value.copy());
    }
    private static Component professionName(Identifier id) {
        if (id == null) { VillagerEventsCompatibility.report("identity", new IllegalStateException("Villager profession has no registry key")); return Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")); }
        if (id != null && "minecraft".equals(id.getNamespace())) return Component.translatable("entity.minecraft.villager." + id.getPath());
        return Component.literal(id.toString());
    }
    private static Component fallbackName(Identifier id) { return id == null ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")) : Component.literal(id.toString()); }
    private static Identity named(Villager villager, Component result, Component fallback) {
        if (!villager.hasCustomName()) return new Identity(result, fallback);
        Component name = villager.getCustomName().copy();
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        Component translated = named(name, result, chinese);
        Component safeName = TextRenderer.renderLiteralTree(villager.getCustomName(), java.util.Map.of());
        Component fallbackNamed = safeName == null ? fallback : named(safeName, fallback, chinese);
        return new Identity(translated, fallbackNamed);
    }
    private static Component named(Component name, Component value, boolean chinese) {
        return chinese ? Component.literal("“").append(name).append("”（").append(value).append("）")
                : Component.literal("\"").append(name).append("\" (").append(value).append(")");
    }
}
