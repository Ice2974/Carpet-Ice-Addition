package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.village.VillagerProfession;

final class VillagerIdentity121 {
    record Identity(Text translated, Text fallback) { }
    private VillagerIdentity121() { }
    static Identity create(VillagerEntity villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        Identifier id = Registries.VILLAGER_PROFESSION.getId(profession);
        Text result = villager.isBaby() ? Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby"))
                : profession == VillagerProfession.NITWIT ? Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit"))
                : profession == VillagerProfession.NONE ? Text.literal(TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed"))
                : professionName(id);
        Text fallback = villager.isBaby() || profession == VillagerProfession.NITWIT || profession == VillagerProfession.NONE ? result.copy() : Text.literal(String.valueOf(id));
        if (!villager.hasCustomName()) return new Identity(result, fallback);
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return new Identity(named(villager, result, chinese), named(villager, fallback, chinese));
    }
    private static Text professionName(Identifier id) {
        if (id != null && "minecraft".equals(id.getNamespace())) return Text.translatable("entity.minecraft.villager." + id.getPath());
        return Text.literal(String.valueOf(id));
    }
    private static Text named(VillagerEntity villager, Text value, boolean chinese) {
        return chinese ? Text.literal("“").append(villager.getCustomName().copy()).append("”（").append(value).append("）") : Text.literal("\"").append(villager.getCustomName().copy()).append("\" (").append(value).append(")");
    }
}
