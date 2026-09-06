package com.ice2974.carpeticeaddition.command;

import java.util.regex.Pattern;

public final class MachineStatusRollbackCommandMatcher {
    private MachineStatusRollbackCommandMatcher() {
    }

    public static boolean matches(String rawInput) {
        String normalized = rawInput == null ? "" : rawInput.trim();
        if (normalized.isEmpty()) {
            return false;
        }

        for (Pattern pattern : MachineStatusRollbackWarningConfig.snapshot().compiledRollbackCommandPatterns()) {
            if (pattern.matcher(normalized).matches()) {
                return true;
            }
        }
        return false;
    }
}
