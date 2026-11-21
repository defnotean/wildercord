package dev.wildercord.client.lore;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyBinding;
import net.minecraft.client.Minecraft;

/** The lore journal's key (H by default, rebindable under "Wildercord: Lore"). */
public final class LoreJournalClient {
	private LoreJournalClient() {}

	private static KeyBinding key;

	public static void init() {
		key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.wildercord.lore_journal", InputConstants.KEY_H,
			KeyBinding.Category.register(Wildercord.id("lore"))));
		ClientTickEvents.END_CLIENT_TICK.register(LoreJournalClient::tick);
	}

	public static KeyBinding mapping() {
		return key;
	}

	private static void tick(Minecraft client) {
		while (key.consumeClick()) {
			if (client.player != null && client.screen == null) client.setScreen(new LoreJournalScreen());
		}
	}
}
