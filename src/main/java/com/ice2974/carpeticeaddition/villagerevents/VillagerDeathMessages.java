package com.ice2974.carpeticeaddition.villagerevents;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Rebuilds a vanilla death message with the villager identity as its first argument, leaving translation to the client. */
final class VillagerDeathMessages {
    private VillagerDeathMessages() { }

    static Component withIdentity(Component source, Component identity) {
        if (!(source.getContents() instanceof TranslatableContents root) || root.getArgs().length == 0 || !(root.getArgs()[0] instanceof Component)) return source;
        Object[] args = root.getArgs().clone();
        args[0] = identity;
        String fallback = Language.getInstance().getOrDefault(root.getKey(), root.getKey());
        MutableComponent output = Component.translatableWithFallback(root.getKey(), fallback, args);
        output.setStyle(source.getStyle());
        for (Component sibling : source.getSiblings()) output.append(sibling);
        return output;
    }
}
