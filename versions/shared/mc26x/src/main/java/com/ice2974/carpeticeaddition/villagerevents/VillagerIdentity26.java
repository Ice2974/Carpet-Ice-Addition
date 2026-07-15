package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/** Produces only literal identity components, so logger output never asks a client to translate it. */
final class VillagerIdentity26 {
    record Identity(Component translated, Component fallback) { }
    private VillagerIdentity26() { }

    static Identity create(Villager villager) {
        String identity;
        if (villager.isBaby()) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby");
        else {
            var profession = villager.getVillagerData().profession();
            Identifier id = profession.unwrapKey().map(ResourceKey::identifier).orElse(null);
            String identifier = String.valueOf(id);
            if ("minecraft:nitwit".equals(identifier)) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit");
            else if ("minecraft:none".equals(identifier)) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed");
            else return named(villager, professionName(id), Component.literal(identifier));
        }
        Component value = Component.literal(identity);
        return named(villager, value, value.copy());
    }
    private static Component professionName(Identifier id) {
        if (id != null && "minecraft".equals(id.getNamespace())) return Component.translatable("entity.minecraft.villager." + id.getPath());
        return Component.literal(String.valueOf(id));
    }
    private static Identity named(Villager villager, Component result, Component fallback) {
        if (!villager.hasCustomName()) return new Identity(result, fallback);
        Component name = villager.getCustomName().copy();
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        Component translated = chinese ? Component.literal("“").append(name).append("”（").append(result).append("）")
                : Component.literal("\"").append(name).append("\" (").append(result).append(")");
        Component fallbackNamed = chinese ? Component.literal("“").append(villager.getCustomName().copy()).append("”（").append(fallback).append("）")
                : Component.literal("\"").append(villager.getCustomName().copy()).append("\" (").append(fallback).append(")");
        return new Identity(translated, fallbackNamed);
    }
}
