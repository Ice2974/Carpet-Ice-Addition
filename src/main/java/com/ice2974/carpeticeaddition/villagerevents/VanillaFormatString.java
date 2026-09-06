package com.ice2974.carpeticeaddition.villagerevents;

/** Validates the small, vanilla-compatible placeholder subset rendered by villagerEvents. */
public final class VanillaFormatString {
    private VanillaFormatString() { }
    public static boolean isSupported(String value) {
        if (value == null) return false;
        boolean explicit = false;
        boolean implicit = false;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != '%') continue;
            if (++index >= value.length()) return false;
            if (value.charAt(index) == '%') continue;
            int start = index;
            while (index < value.length() && Character.isDigit(value.charAt(index))) index++;
            if (index > start) {
                if (index >= value.length() || value.charAt(index++) != '$') return false;
                if (value.charAt(start) == '0') return false;
                try { Integer.parseInt(value.substring(start, index - 1)); } catch (NumberFormatException exception) { return false; }
                explicit = true;
            } else implicit = true;
            if (index >= value.length() || value.charAt(index) != 's') return false;
        }
        return !(explicit && implicit);
    }
}
