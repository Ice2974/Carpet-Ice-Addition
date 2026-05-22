package com.ice2974.carpeticeaddition.command;

public final class CommandStringParsingUtil {
    private CommandStringParsingUtil() {
    }

    public static int skipWhitespace(String input, int start) {
        int index = Math.max(0, start);
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
        return index;
    }

    public static ParsedToken parseNextToken(String input, int start) {
        int index = skipWhitespace(input, start);
        if (index >= input.length()) {
            return null;
        }

        if (input.charAt(index) != '"') {
            int end = index;
            while (end < input.length() && !Character.isWhitespace(input.charAt(end))) {
                end++;
            }
            return new ParsedToken(input.substring(index, end), end);
        }

        StringBuilder builder = new StringBuilder();
        int cursor = index + 1;
        while (cursor < input.length()) {
            char current = input.charAt(cursor);
            if (current == '\\') {
                if (cursor + 1 >= input.length()) {
                    return null;
                }
                char escaped = input.charAt(cursor + 1);
                if (escaped != '\\' && escaped != '"') {
                    return null;
                }
                builder.append(escaped);
                cursor += 2;
                continue;
            }
            if (current == '"') {
                return new ParsedToken(builder.toString(), cursor + 1);
            }
            builder.append(current);
            cursor++;
        }
        return null;
    }

    public record ParsedToken(String value, int nextIndex) {
    }
}
