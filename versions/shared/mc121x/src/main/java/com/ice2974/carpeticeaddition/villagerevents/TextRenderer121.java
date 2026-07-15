package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TextRenderer121 {
    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([%s])");

    private TextRenderer121() { }

    static Text renderDeath(Text source, Text identity, Map<String, String> language) {
        if (!(source.getContent() instanceof TranslatableTextContent root) || root.getArgs().length == 0 || !(root.getArgs()[0] instanceof Text)) return null;
        return render(source, language, identity, true, 0);
    }
    static Text renderLiteralTree(Text source, Map<String, String> language) { return render(source, language, null, false, 0); }

    private static Text render(Text input, Map<String, String> language, Text rootVictim, boolean root, int depth) {
        if (depth > 64) return null;
        TextContent content = input.getContent();
        MutableText output;
        if (content instanceof PlainTextContent plain) {
            output = Text.literal(plain.string());
        } else if (content instanceof TranslatableTextContent translatable) {
            String format = language.get(translatable.getKey());
            if (format == null) return null;
            output = format(translatable, format, language, root ? rootVictim : null, depth + 1);
            if (output == null) return null;
        } else {
            return null;
        }
        // Hover events are intentionally stripped: SHOW_ITEM/SHOW_ENTITY can trigger client-local text.
        Style style = input.getStyle().withHoverEvent(null);
        output.setStyle(style);
        for (Text sibling : input.getSiblings()) {
            Text rendered = render(sibling, language, null, false, depth + 1);
            if (rendered == null) return null;
            output.append(rendered);
        }
        return output;
    }

    private static MutableText format(TranslatableTextContent content, String format, Map<String, String> language, Text rootVictim, int depth) {
        Object[] args = content.getArgs();
        MutableText result = Text.empty();
        Matcher matcher = FORMAT.matcher(format);
        int index = 0;
        int nextImplicit = 0;
        while (matcher.find()) {
            result.append(format.substring(index, matcher.start()));
            index = matcher.end();
            if ("%".equals(matcher.group(2))) { result.append("%"); continue; }
            int argument = matcher.group(1) == null ? nextImplicit++ : Integer.parseInt(matcher.group(1)) - 1;
            if (argument < 0 || argument >= args.length) return null;
            Object value = rootVictim != null && argument == 0 ? rootVictim : args[argument];
            if (value instanceof Text text) {
                Text rendered = render(text, language, null, false, depth + 1);
                if (rendered == null) return null;
                result.append(rendered);
            } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                result.append(String.valueOf(value));
            } else {
                return null;
            }
        }
        result.append(format.substring(index));
        return result;
    }
}
