package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

/** Produces only literal identity components, so logger output never asks a client to translate it. */
final class VillagerIdentity26 {
    private VillagerIdentity26() { }

    static Component create(Villager villager) {
        String identity;
        if (villager.isBaby()) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby");
        else {
            String profession = String.valueOf(villager.getVillagerData().profession());
            String lower = profession.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("nitwit")) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit");
            else if (lower.equals("none") || lower.endsWith(":none") || lower.contains("unemployed")) identity = TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed");
            else identity = profession;
        }
        Component result = Component.literal(identity);
        if (!villager.hasCustomName()) return result;
        Component name = villager.getCustomName().copy();
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return chinese ? Component.literal("“").append(name).append("”（").append(result).append("）")
                : Component.literal("\"").append(name).append("\" (").append(result).append(")");
    }
}
