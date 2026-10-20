package com.ice2974.carpeticeaddition.rules;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToughAnvilRuleTest {

    private String originalRuleValue;

    @BeforeEach
    void setUp() {
        originalRuleValue = CarpetIceAdditionSettings.toughAnvil;
    }

    @AfterEach
    void tearDown() {
        CarpetIceAdditionSettings.toughAnvil = originalRuleValue;
    }

    @Test
    void evaluatesAllValidOptionsCorrectly() {
        CarpetIceAdditionSettings.toughAnvil = "false";
        assertFalse(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertFalse(CarpetIceAdditionSettings.preventsAnvilFallDamage());

        CarpetIceAdditionSettings.toughAnvil = "true";
        assertTrue(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertTrue(CarpetIceAdditionSettings.preventsAnvilFallDamage());

        CarpetIceAdditionSettings.toughAnvil = "useOnly";
        assertTrue(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertFalse(CarpetIceAdditionSettings.preventsAnvilFallDamage());

        CarpetIceAdditionSettings.toughAnvil = "fallOnly";
        assertFalse(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertTrue(CarpetIceAdditionSettings.preventsAnvilFallDamage());
    }

    @Test
    void fallbacksSafelyOnInvalidValues() {
        List<String> invalidValues = Arrays.asList(
                null,
                "",
                " ",
                "True",
                "TRUE",
                "useonly",
                "USEONLY",
                "fallonly",
                "FALLONLY",
                "1",
                "0",
                "ops",
                "invalid"
        );

        for (String invalid : invalidValues) {
            CarpetIceAdditionSettings.toughAnvil = invalid;
            assertFalse(
                    CarpetIceAdditionSettings.preventsAnvilUseDamage(),
                    "Expected preventsAnvilUseDamage to be false for: " + invalid
            );
            assertFalse(
                    CarpetIceAdditionSettings.preventsAnvilFallDamage(),
                    "Expected preventsAnvilFallDamage to be false for: " + invalid
            );
        }
    }

    @Test
    void ruleMutationReflectsImmediately() {
        CarpetIceAdditionSettings.toughAnvil = "false";
        assertFalse(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertFalse(CarpetIceAdditionSettings.preventsAnvilFallDamage());

        CarpetIceAdditionSettings.toughAnvil = "useOnly";
        assertTrue(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertFalse(CarpetIceAdditionSettings.preventsAnvilFallDamage());

        CarpetIceAdditionSettings.toughAnvil = "fallOnly";
        assertFalse(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertTrue(CarpetIceAdditionSettings.preventsAnvilFallDamage());

        CarpetIceAdditionSettings.toughAnvil = "true";
        assertTrue(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertTrue(CarpetIceAdditionSettings.preventsAnvilFallDamage());

        CarpetIceAdditionSettings.toughAnvil = "false";
        assertFalse(CarpetIceAdditionSettings.preventsAnvilUseDamage());
        assertFalse(CarpetIceAdditionSettings.preventsAnvilFallDamage());
    }

    @Test
    void ruleMetadataMatchesRequirements() throws NoSuchFieldException {
        Field field = CarpetIceAdditionSettings.class.getField("toughAnvil");
        Rule rule = field.getAnnotation(Rule.class);

        assertNotNull(rule, "Field toughAnvil must be annotated with @Rule");
        assertArrayEquals(
                new String[]{"false", "true", "useOnly", "fallOnly"},
                rule.options(),
                "Rule options must match exact 4 choices"
        );

        List<String> categories = Arrays.asList(rule.categories());
        assertTrue(categories.contains(CarpetIceAdditionSettings.ICE), "Must include ICE category");
        assertTrue(categories.contains(RuleCategory.FEATURE), "Must include FEATURE category");
        assertTrue(rule.strict(), "Rule must be strict by default");
    }
}
