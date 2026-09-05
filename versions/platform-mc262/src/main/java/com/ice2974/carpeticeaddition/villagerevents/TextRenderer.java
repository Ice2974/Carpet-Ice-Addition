package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TextRenderer {
    private static final Pattern FORMAT = Pattern.compile("%(?:(\\d+)\\$)?([%s])");
    private TextRenderer() { }
    static Component renderDeath(Component source, Component identity, Map<String, String> language) {
        if (!(source.getContents() instanceof TranslatableContents root) || root.getArgs().length == 0 || !(root.getArgs()[0] instanceof Component)) return null;
        return render(source, language, identity, true, 0);
    }
    static Component renderLiteralTree(Component source, Map<String, String> language) { return render(source, language, null, false, 0); }
    private static Component render(Component input, Map<String, String> language, Component victim, boolean root, int depth) {
        if (depth > 64) return null;
        ComponentContents content = input.getContents();
        MutableComponent result;
        if (content instanceof PlainTextContents plain) result = Component.literal(plain.text());
        else if (content instanceof TranslatableContents translatable) {
            String format = language.get(translatable.getKey());
            if (format == null) return null;
            result = format(translatable, format, language, root ? victim : null, depth + 1);
            if (result == null) return null;
        } else return null;
        result.setStyle(input.getStyle().withHoverEvent(null).withClickEvent(null));
        for (Component sibling : input.getSiblings()) {
            Component rendered = render(sibling, language, null, false, depth + 1);
            if (rendered == null) return null;
            result.append(rendered);
        }
        return result;
    }
    private static MutableComponent format(TranslatableContents content, String format, Map<String, String> language, Component victim, int depth) {
        if (!VanillaFormatString.isSupported(format)) return null;
        MutableComponent result = Component.empty(); Object[] args = content.getArgs(); Matcher matcher = FORMAT.matcher(format); int at = 0; int implicit = 0;
        while (matcher.find()) {
            String literal = format.substring(at, matcher.start());
            if (literal.indexOf('%') >= 0) return null;
            result.append(literal); at = matcher.end();
            if ("%".equals(matcher.group(2))) { result.append("%"); continue; }
            int index = matcher.group(1) == null ? implicit++ : Integer.parseInt(matcher.group(1)) - 1;
            if (index < 0 || index >= args.length) return null;
            Object value = victim != null && index == 0 ? victim : args[index];
            if (value instanceof Component component) { Component rendered = render(component, language, null, false, depth + 1); if (rendered == null) return null; result.append(rendered); }
            else if (value instanceof String || value instanceof Number || value instanceof Boolean) result.append(String.valueOf(value));
            else return null;
        }
        String trailing = format.substring(at);
        if (trailing.indexOf('%') >= 0) return null;
        return result.append(trailing);
    }
}
