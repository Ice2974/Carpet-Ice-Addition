package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.Validator;
import com.ice2974.carpeticeaddition.rules.CustomEndPlatformPositionHelper;
import net.minecraft.commands.CommandSourceStack;

import static carpet.api.settings.RuleCategory.FEATURE;

@SuppressWarnings("unused")
public final class CarpetIceAdditionEndPlatformSettings {
    private CarpetIceAdditionEndPlatformSettings() {
    }

    @Rule(
            categories = {CarpetIceAdditionSettings.ICE, FEATURE},
            options = {"vanilla", "-100,49,0"},
            strict = false,
            validators = CustomEndPlatformPositionValidator.class
    )
    public static String customEndPlatformPosition = "vanilla";

    public static final class CustomEndPlatformPositionValidator extends Validator<String> {
        @Override
        public String validate(CommandSourceStack source, CarpetRule<String> changingRule, String newValue, String userInput) {
            if (newValue == null) {
                return null;
            }

            String normalized = newValue.trim();
            if (CustomEndPlatformPositionHelper.VANILLA.equals(normalized)) {
                return CustomEndPlatformPositionHelper.VANILLA;
            }

            return CustomEndPlatformPositionHelper.parse(normalized)
                    .map(CustomEndPlatformPositionHelper.IntPosition::asRuleString)
                    .orElse(null);
        }

        @Override
        public String description() {
            return "Must be 'vanilla' or three integers in x,y,z format";
        }
    }
}
