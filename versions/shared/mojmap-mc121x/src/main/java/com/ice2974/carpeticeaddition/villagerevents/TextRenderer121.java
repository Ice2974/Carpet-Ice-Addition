package com.ice2974.carpeticeaddition.villagerevents;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

final class TextRenderer121 {
    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([%s])");

    private TextRenderer121() { }

    static Component renderDeath(Component source, Component identity, Map<String, String> language) {
        if (!(source.getContents() instanceof TranslatableContents root) || root.getArgs().length == 0 || !(root.getArgs()[0] instanceof Component)) return null;
        return render(source, language, identity, true, 0);
    }
    static Component renderLiteralTree(Component source, Map<String, String> language) { return render(source, language, null, false, 0); }

    private static Component render(Component input, Map<String, String> language, Component rootVictim, boolean root, int depth) {
        if (depth > 64) return null;
        ComponentContents content = input.getContents();
        MutableComponent output;
        if (content instanceof PlainTextContents plain) {
            output = Component.literal(plain.text());
        } else if (content instanceof TranslatableContents translatable) {
            String format = language.get(translatable.getKey());
            if (format == null) return null;
            output = format(translatable, format, language, root ? rootVictim : null, depth + 1);
            if (output == null) return null;
        } else {
            return null;
        }
        // Hover events are intentionally stripped: SHOW_ITEM/SHOW_ENTITY can trigger client-local text.
        Style style = input.getStyle().withHoverEvent(null).withClickEvent(null);
        output.setStyle(style);
        for (Component sibling : input.getSiblings()) {
            Component rendered = render(sibling, language, null, false, depth + 1);
            if (rendered == null) return null;
            output.append(rendered);
        }
        return output;
    }

    private static MutableComponent format(TranslatableContents content, String format, Map<String, String> language, Component rootVictim, int depth) {
        if (!VanillaFormatString.isSupported(format)) return null;
        Object[] args = content.getArgs();
        MutableComponent result = Component.empty();
        Matcher matcher = FORMAT.matcher(format);
        int index = 0;
        int nextImplicit = 0;
        while (matcher.find()) {
            String literal = format.substring(index, matcher.start());
            if (literal.indexOf('%') >= 0) return null;
            result.append(literal);
            index = matcher.end();
            if ("%".equals(matcher.group(2))) { result.append("%"); continue; }
            int argument = matcher.group(1) == null ? nextImplicit++ : Integer.parseInt(matcher.group(1)) - 1;
            if (argument < 0 || argument >= args.length) return null;
            Object value = rootVictim != null && argument == 0 ? rootVictim : args[argument];
            if (value instanceof Component text) {
                Component rendered = render(text, language, null, false, depth + 1);
                if (rendered == null) return null;
                result.append(rendered);
            } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                result.append(String.valueOf(value));
            } else {
                return null;
            }
        }
        String trailing = format.substring(index);
        if (trailing.indexOf('%') >= 0) return null;
        result.append(trailing);
        return result;
    }
}
