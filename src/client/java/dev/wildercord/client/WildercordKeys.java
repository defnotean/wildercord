package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

/** R casts, V switches spell, K opens the Cord screen. Casting spells 1-4 directly is unbound by default. */
public final class WildercordKeys {
	private WildercordKeys() {}

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Wildercord.id("wildercord"));

	private static KeyMapping cast;
	private static KeyMapping next;
	private static KeyMapping open;
	private static final KeyMapping[] CAST_N = new KeyMapping[4];

	/** Current key names, for help text that stays right after rebinding. */
	public static net.minecraft.network.chat.Component castKey() {
		return cast.getTranslatedKeyMessage();
	}

	public static net.minecraft.network.chat.Component nextKey() {
		return next.getTranslatedKeyMessage();
	}

	public static net.minecraft.network.chat.Component openKey() {
		return open.getTranslatedKeyMessage();
	}

	public static void init() {
		cast = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.cast", InputConstants.KEY_R, CATEGORY));
		next = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.next_spell", InputConstants.KEY_V, CATEGORY));
		open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.open_cord", InputConstants.KEY_K, CATEGORY));
		for (int i = 0; i < CAST_N.length; i++) {
			CAST_N[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.cast_" + (i + 1),
				InputConstants.UNKNOWN.getType(), InputConstants.UNKNOWN.getValue(), CATEGORY));
		}
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (cast.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new WildercordNetworking.CastSpell(-1));
				}
			}
			while (next.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new WildercordNetworking.SelectSpell(Spellbooks.get(client.player).selected() + 1));
				}
			}
			while (open.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					client.gui.setScreen(new CordScreen());
				}
			}
			for (int i = 0; i < CAST_N.length; i++) {
				while (CAST_N[i].consumeClick()) {
					if (client.player != null) {
						ClientPlayNetworking.send(new WildercordNetworking.CastSpell(i));
					}
				}
			}
		});
	}
}
