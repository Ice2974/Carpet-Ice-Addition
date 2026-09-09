//#if MC<260000
package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

final class VillagerIdentity {
    private VillagerIdentity() { }
    static Component create(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        Identifier id = profession.unwrapKey().map(key -> key.identifier()).orElse(null);
        String identifier = id == null ? null : id.toString();
        Component identity = villager.isBaby() ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby"))
                : "minecraft:nitwit".equals(identifier) ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit"))
                : "minecraft:none".equals(identifier) ? Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed"))
                : professionName(id);
        if (!villager.hasCustomName()) return identity;
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return named(villager.getCustomName().copy(), identity, chinese);
    }
    private static Component professionName(Identifier id) {
        if (id == null) { VillagerEventsCompatibility.report("identity", new IllegalStateException("Villager profession has no registry key")); return Component.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unknown_profession")); }
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
//#endif
