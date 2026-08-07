package com.ice2974.carpeticeaddition.rules;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;

/**
 * Validator for {@code waterFluidTickDelay} and {@code lavaFluidTickDelay}.
 *
 * <p>Accepts the literal {@code freeze} or an integer from 1 through
 * {@link FluidTickDelayUtil#MAX_FLUID_TICK_DELAY}; rejects zero, negatives,
 * decimals, non-numeric strings, empty strings and out-of-range values.
 * Delegates the actual parsing to the common
 * {@link FluidTickDelayUtil} so the logic is unit-tested there.
 *
 * <p>This validator is side-effect free: it only validates the input. Cache
 * refresh happens in the rule observer after a successful set.
 */
public final class FluidTickDelayValidator extends Validator<String> {
    @Override
    public @Nullable String validate(@Nullable ServerCommandSource source, CarpetRule<String> changingRule, String newValue, String userInput) {
        return FluidTickDelayUtil.isValidRuleValue(newValue) ? newValue : null;
    }
}
