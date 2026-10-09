package dev.wildercord.client;

import net.minecraft.client.gui.screens.Screen;

/** Explicit retained editor ancestry. Unrelated or closed screens cannot receive an old edit reply. */
public interface CordEditorParent {
    Screen cordEditorParent();
}
