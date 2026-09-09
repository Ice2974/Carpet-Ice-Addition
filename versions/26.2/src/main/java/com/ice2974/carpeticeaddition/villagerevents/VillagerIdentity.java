package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

/** Builds the villager identity component; vanilla keys are left for each client to translate. */
final class VillagerIdentity {
    private VillagerIdentity() { }

    static Component create(Villager villager) {
        String identity;
        if (villager.isBaby()) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby");
        else {
            var profession = villager.getVillagerData().profession();
            Identifier id = profession.unwrapKey().map(ResourceKey::identifier).orElse(null);
            String identifier = id == null ? null : id.toString();
            if ("minecraft:nitwit".equals(identifier)) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit");
            else if ("minecraft:none".equals(identifier)) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed");
            else return named(villager, professionName(id));
        }
        return named(villager, Component.literal(identity));
    }
    private static Component professionName(Identifier id) {
        if (id == null) { VillagerEventsCompatibility.report("identity", new IllegalStateException("Villager profession has no registry key")); return Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")); }
        if ("minecraft".equals(id.getNamespace())) {
            String key = "entity.minecraft.villager." + id.getPath();
            return Component.translatableWithFallback(key, Language.getInstance().getOrDefault(key, id.toString()));
        }
        return Component.literal(id.toString());
    }
    private static Component named(Villager villager, Component result) {
        if (!villager.hasCustomName()) return result;
        Component name = villager.getCustomName().copy();
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return chinese ? Component.literal("“").append(name).append("”（").append(result).append("）")
                : Component.literal("\"").append(name).append("\" (").append(result).append(")");
    }
}
