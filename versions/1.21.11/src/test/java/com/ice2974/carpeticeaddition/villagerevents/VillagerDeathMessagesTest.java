package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class VillagerDeathMessagesTest {

    @Test
    void replacesOnlyTheFirstArgument() {
        Component source = Component.translatable("death.attack.mob", Component.literal("victim"), Component.literal("attacker"));
        Component result = VillagerDeathMessages.withIdentity(source, Component.literal("Farmer"));
        TranslatableContents contents = (TranslatableContents) result.getContents();
        assertEquals("death.attack.mob", contents.getKey());
        assertEquals("Farmer", ((Component) contents.getArgs()[0]).getString());
        assertEquals("attacker", ((Component) contents.getArgs()[1]).getString());
        assertEquals(Language.getInstance().getOrDefault("death.attack.mob", "death.attack.mob"), contents.getFallback());
    }

    @Test
    void keepsStyleAndSiblings() {
        Component source = Component.translatable("death.attack.mob", Component.literal("victim"))
                .withStyle(ChatFormatting.RED).append(Component.literal("suffix"));
        Component result = VillagerDeathMessages.withIdentity(source, Component.literal("Farmer"));
        assertEquals(source.getStyle(), result.getStyle());
        assertEquals(1, result.getSiblings().size());
        assertEquals("suffix", result.getSiblings().get(0).getString());
    }

    @Test
    void passesThroughUnexpectedStructuresUnchanged() {
        Component literal = Component.literal("plain");
        assertSame(literal, VillagerDeathMessages.withIdentity(literal, Component.literal("Farmer")));
        Component withoutArguments = Component.translatable("death.attack.mob");
        assertSame(withoutArguments, VillagerDeathMessages.withIdentity(withoutArguments, Component.literal("Farmer")));
        Component textualArgument = Component.translatable("death.attack.mob", "plain text");
        assertSame(textualArgument, VillagerDeathMessages.withIdentity(textualArgument, Component.literal("Farmer")));
    }
}
