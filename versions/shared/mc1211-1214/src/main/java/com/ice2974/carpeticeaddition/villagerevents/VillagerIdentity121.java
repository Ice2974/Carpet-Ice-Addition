package com.ice2974.carpeticeaddition.villagerevents;

import carpet.CarpetSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;

final class VillagerIdentity121 {
    private VillagerIdentity121() { }
    static Text create(VillagerEntity villager) { return compose(villager, String.valueOf(villager.getVillagerData().getProfession())); }
    static Text compose(VillagerEntity villager, String profession) {
        String lower = profession.toLowerCase(java.util.Locale.ROOT);
        String identity = villager.isBaby() ? TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.baby")
                : lower.contains("nitwit") ? TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.nitwit")
                : lower.equals("none") || lower.endsWith(":none") || lower.contains("unemployed") ? TranslationFormatUtil.translate("logger.carpet-ice-addition.villager_events.unemployed") : profession;
        Text result = Text.literal(identity);
        if (!villager.hasCustomName()) return result;
        boolean chinese = CarpetSettings.language != null && CarpetSettings.language.toLowerCase(java.util.Locale.ROOT).startsWith("zh");
        return chinese ? Text.literal("“").append(villager.getCustomName().copy()).append("”（").append(result).append("）") : Text.literal("\"").append(villager.getCustomName().copy()).append("\" (").append(result).append(")");
    }
}
