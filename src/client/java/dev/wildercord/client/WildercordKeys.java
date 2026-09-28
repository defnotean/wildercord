package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * R casts (tap) or charges (hold, then let go); V moves to the next spell (tap) or opens the spell
 * wheel (hold); K opens the Cord screen. Casting spells 1-4 directly is unbound by default.
 */
public final class WildercordKeys {
	private WildercordKeys() {}

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Wildercord.id("wildercord"));
	/** Held this many ticks, a key press becomes a hold (a charge, or the wheel). */
	private static final int HOLD = 5;

	private static KeyMapping cast;
	private static KeyMapping next;
	private static KeyMapping open;
	private static final KeyMapping[] CAST_N = new KeyMapping[4];

	private static int castHeld = -1;
	private static boolean charging;
	private static int nextHeld = -1;

	/** Current key names, for help text that stays right after rebinding. */
	public static net.minecraft.network.chat.Component castKey() {
		return cast.getTranslatedKeyMessage();
	}

	public static net.minecraft.network.chat.Component nextKey() {
		return next.getTranslatedKeyMessage();
	}

	/** The switch-spell key, which the spell wheel watches to know when it's let go. */
	public static KeyMapping nextMapping() {
		return next;
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
		ClientTickEvents.END_CLIENT_TICK.register(WildercordKeys::tick);
	}

	private static void tick(Minecraft client) {
		boolean playing = client.player != null && client.gui.screen() == null;
		// The cast key: a tap casts at once; held, the spell charges until it's let go.
		while (cast.consumeClick()) {
			if (castHeld < 0 && playing) {
				castHeld = 0;
			}
		}
		if (castHeld >= 0) {
			if (cast.isDown() && playing) {
				castHeld++;
				if (castHeld == HOLD && !charging) {
					charging = true;
					ClientPlayNetworking.send(new WildercordNetworking.ChargeSpell(-1, true));
				}
			} else {
				if (client.player != null) {
					if (charging) {
						ClientPlayNetworking.send(new WildercordNetworking.ChargeSpell(-1, false));
					} else {
						ClientPlayNetworking.send(new WildercordNetworking.CastSpell(-1));
					}
				}
				castHeld = -1;
				charging = false;
			}
		}
		// The switch key: a tap moves on; held, the spell wheel opens.
		while (next.consumeClick()) {
			if (nextHeld < 0 && playing) {
				nextHeld = 0;
			}
		}
		if (nextHeld >= 0) {
			CordTier tier = client.player == null ? null : Spellbooks.tier(client.player);
			if (next.isDown() && playing) {
				nextHeld++;
				if (nextHeld == HOLD && tier != null && tier.spells > 1) {
					client.gui.setScreen(new SpellWheelScreen(next));
					nextHeld = -1;
				}
			} else {
				if (client.player != null && nextHeld < HOLD) {
					ClientPlayNetworking.send(new WildercordNetworking.SelectSpell(Spellbooks.get(client.player).selected() + 1));
				}
				nextHeld = -1;
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
	}
}
