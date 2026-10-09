package dev.wildercord.client.lore;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** The lore journal's key (H by default, rebindable under "Wildercord: Lore"). */
public final class LoreJournalClient {
	private LoreJournalClient() {}

	private static KeyMapping key;

	public static void init() {
		key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.lore_journal", InputConstants.KEY_H,
			KeyMapping.Category.register(Wildercord.id("lore"))));
		ClientTickEvents.END_CLIENT_TICK.register(LoreJournalClient::tick);
	}

	public static KeyMapping mapping() {
		return key;
	}

	private static void tick(Minecraft client) {
		while (key.consumeClick()) {
			if (client.player != null && client.gui.screen() == null) client.gui.setScreen(new LoreJournalScreen());
		}
	}
}
