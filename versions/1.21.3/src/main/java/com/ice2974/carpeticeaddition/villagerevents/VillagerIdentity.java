package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

final class VillagerIdentity {
    private VillagerIdentity() { }
    static Component create(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        Component identity = villager.isBaby() ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby"))
                : profession == VillagerProfession.NITWIT ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit"))
                : profession == VillagerProfession.NONE ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed"))
                : professionName(id);
        if (!villager.hasCustomName()) return identity;
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return named(villager.getCustomName().copy(), identity, chinese);
    }
    private static Component professionName(ResourceLocation id) {
        if (id == null) { VillagerEventsCompatibility.report("identity", new IllegalStateException("Villager profession has no registry id")); return Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")); }
        if ("minecraft".equals(id.getNamespace())) {
            String key = "entity.minecraft.villager." + id.getPath();
            return Component.translatableWithFallback(key, Language.getInstance().getOrDefault(key, id.toString()));
        }
        return Component.literal(id.toString());
    }
    private static Component named(Component name, Component value, boolean chinese) {
        return chinese ? Component.literal("“").append(name).append("”（").append(value).append("）") : Component.literal("\"").append(name).append("\" (").append(value).append(")");
    }
}
