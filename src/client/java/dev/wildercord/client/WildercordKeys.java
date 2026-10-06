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
 * wheel (hold); K opens the Cord screen; B opens the backpack worn in the Backpack slot (in the inventory
 * too, and closes an open backpack); Z is the Aura key (a tap, a press while sneaking, a double tap, a
 * hold, and a tap then a held press each go to the server, which picks the technique: see {@code api.AuraApi}).
 * Casting spells 1-4 directly, and loading the next loadout, are unbound by default.
 */
public final class WildercordKeys {
	private WildercordKeys() {}

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Wildercord.id("wildercord"));
	/** Held this many ticks, a key press becomes a hold (a charge, or the wheel). */
	private static final int HOLD = 5;

	private static KeyMapping cast;
	private static KeyMapping next;
	private static KeyMapping open;
	/** Opens the worn backpack (the server opens it, or says there's none). */
	private static KeyMapping backpack;
	/** Loads the next saved loadout (the server checks it may, and names it above the hotbar). */
	private static KeyMapping nextLoadout;
	private static KeyMapping magicSettings;
	/** "Cast spell N": the Cord's four, and the tome's fifth. */
	private static final KeyMapping[] CAST_N = new KeyMapping[dev.wildercord.gear.SpellSlots.ALL];

	/** The Aura key: its ways of being pressed go to the server as {@code aura.Aura.Key}. */
	private static KeyMapping aura;
	/** Two taps of the Aura key this close together (ticks) are a double tap. */
	private static final int DOUBLE_TAP = 8;
	private static int auraHeld = -1;
	/** Whether the Aura key's press already went (sneaking, or held into a hold): letting it go then sends nothing more. */
	private static boolean auraSent;
	private static long auraTapAt = Long.MIN_VALUE / 2;
	/**
	 * Whether the press held now came straight after a tap (within the double tap's moment), for a player with a tap-and-hold technique
	 * (awakening, from Edge): let go quickly it's a double tap; held {@link dev.wildercord.aura.AwakeningRules#HOLD_TICKS} it's the
	 * tap-and-hold; let go between, nothing.
	 */
	private static boolean auraSecond;
	/** Whether the tap before that second press was held back waiting for it (so it goes with the pair, never on its own). */
	private static boolean auraFirstWaited;
	/** How long the second press of a tap-and-hold has been held (ticks), for its charge by the crosshair; 0 when none is. */
	private static int auraCharge;
	/** When the last tap-and-hold went (game time), for its ring snapping out by the crosshair. */
	private static long auraTapHoldAt = Long.MIN_VALUE / 4;

	private static int castHeld = -1;
	private static boolean charging;
	private static int nextHeld = -1;

	/** Current key names, for help text that stays right after rebinding. */
	public static net.minecraft.network.chat.Component castKey() {
		return cast.getTranslatedKeyMessage();
	}

	/** The cast key itself (tests hold it to charge, as a player would). */
	public static KeyMapping castMapping() {
		return cast;
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

	/** The backpack key, which the backpack screen and the inventory watch too. */
	public static KeyMapping backpackMapping() {
		return backpack;
	}

	public static net.minecraft.network.chat.Component backpackKey() {
		return backpack.getTranslatedKeyMessage();
	}

	/** Asks the server to open the worn backpack. */
	public static void openBackpack() {
		ClientPlayNetworking.send(new WildercordNetworking.OpenBackpack());
	}

	/** The Aura key itself (tests press it as a player would). */
	public static KeyMapping auraMapping() {
		return aura;
	}

	public static net.minecraft.network.chat.Component auraKey() {
		return aura.getTranslatedKeyMessage();
	}

	private static void sendAura(dev.wildercord.api.AuraApi.Trigger trigger) {
		ClientPlayNetworking.send(new dev.wildercord.aura.Aura.Key(trigger.ordinal()));
	}

	/**
	 * How far a tap-and-hold of the Aura key has come toward awakening (0 to 1; 0 while none is being held), with the partial tick, for
	 * its charge by the crosshair.
	 */
	public static float awakeningCharge(float partial) {
		return auraCharge <= 0 ? 0 : dev.wildercord.aura.AwakeningRules.charge(auraCharge + partial);
	}

	/** When the last tap-and-hold of the Aura key went (game time). */
	public static long tapHoldSentAt() {
		return auraTapHoldAt;
	}

	public static net.minecraft.network.chat.Component nextLoadoutKey() {
		return nextLoadout.getTranslatedKeyMessage();
	}

	public static void init() {
		magicSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.magic_settings",
			InputConstants.UNKNOWN.getType(), InputConstants.UNKNOWN.getValue(), CATEGORY));
		cast = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.cast", InputConstants.KEY_R, CATEGORY));
		next = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.next_spell", InputConstants.KEY_V, CATEGORY));
		open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.open_cord", InputConstants.KEY_K, CATEGORY));
		backpack = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.open_backpack", InputConstants.KEY_B, CATEGORY));
		// Z, not V: V is already the spell key (tap to switch, hold for the wheel).
		aura = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.aura", InputConstants.KEY_Z, CATEGORY));
		for (int i = 0; i < CAST_N.length; i++) {
			CAST_N[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.cast_" + (i + 1),
				InputConstants.UNKNOWN.getType(), InputConstants.UNKNOWN.getValue(), CATEGORY));
		}
		nextLoadout = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildercord.next_loadout",
			InputConstants.UNKNOWN.getType(), InputConstants.UNKNOWN.getValue(), CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(WildercordKeys::tick);
	}

	/** A lone tap held back for the double tap's moment (see {@link #auraKey}), or false. */
	private static boolean auraTapWaiting;

	/**
	 * Whether a lone tap waits out the double tap's moment before it goes: only when the player has a double-tap technique
	 * (Aura Step, from Form), so a double tap is a step alone and never looses a slash first.
	 */
	private static boolean tapsWait(Minecraft client) {
		if (client.player == null) {
			return false;
		}
		if (has(client, dev.wildercord.api.AuraApi.Trigger.DOUBLE_TAP)) {
			return true;
		}
		// From Edge, while an awakening is ready, a lone tap waits too: a tap then a held press must awaken with a full pool, never
		// loose a slash first (which would spend it).
		return has(client, dev.wildercord.api.AuraApi.Trigger.TAP_HOLD) && dev.wildercord.aura.Awakening.ready(client.player);
	}

	/** Whether the player has a technique for {@code trigger} at their stage. */
	private static boolean has(Minecraft client, dev.wildercord.api.AuraApi.Trigger trigger) {
		return client.player != null && dev.wildercord.api.AuraApi.techniqueFor(dev.wildercord.aura.Aura.stage(client.player), trigger).isPresent();
	}

	/**
	 * The Aura key: pressed while sneaking it goes at once (a guard can't wait for the key to come up); otherwise a tap goes
	 * when it's let go, and held it goes once as the hold begins. A second tap soon after is a double tap: with a double-tap
	 * technique (from Form) a lone tap waits out that moment first and the pair goes as the double tap alone; without one the
	 * first tap goes at once and the second goes as a tap and a double tap.
	 *
	 * <p>From Edge (a tap-and-hold technique: awakening) a second press straight after a tap that's held instead
	 * ({@link dev.wildercord.aura.AwakeningRules#HOLD_TICKS}) goes as the tap-and-hold, the waiting tap with it, and never as a hold
	 * (Dominion) too; let go before it completes (and after it was clearly held) it sends nothing. A lone tap waits for that second
	 * press while an awakening is ready (always from Form, for the step).</p>
	 */
	private static void auraKey(Minecraft client, boolean playing) {
		long gameTime = client.level == null ? 0 : client.level.getGameTime();
		if (auraTapWaiting && (!playing || gameTime - auraTapAt > DOUBLE_TAP)) {
			// No second tap came: the first goes as a tap after all.
			auraTapWaiting = false;
			if (playing) {
				sendAura(dev.wildercord.api.AuraApi.Trigger.TAP);
			}
		}
		while (aura.consumeClick()) {
			if (auraHeld < 0 && playing) {
				auraHeld = 0;
				auraSent = client.player.isShiftKeyDown();
				auraSecond = false;
				if (auraSent) {
					sendAura(dev.wildercord.api.AuraApi.Trigger.SNEAK_TAP);
				} else if (gameTime - auraTapAt <= DOUBLE_TAP && has(client, dev.wildercord.api.AuraApi.Trigger.TAP_HOLD)) {
					// Straight after a tap: a double tap, or held, a tap-and-hold. A tap held back goes with the pair either way.
					auraSecond = true;
					auraFirstWaited = auraTapWaiting;
					auraTapWaiting = false;
				}
			}
		}
		if (auraHeld < 0 || !playing) {
			auraCharge = 0;
			if (!playing) {
				auraSecond = false;
			}
			return;
		}
		if (aura.isDown()) {
			auraHeld++;
			if (auraSecond) {
				auraCharge = auraSent ? 0 : auraHeld;
				if (!auraSent && auraHeld >= dev.wildercord.aura.AwakeningRules.HOLD_TICKS) {
					auraSent = true;
					auraCharge = 0;
					auraTapAt = Long.MIN_VALUE / 2;
					auraTapHoldAt = gameTime;
					sendAura(dev.wildercord.api.AuraApi.Trigger.TAP_HOLD);
				}
				return;
			}
			if (!auraSent && auraHeld == HOLD) {
				auraSent = true;
				sendAura(dev.wildercord.api.AuraApi.Trigger.HOLD);
			}
			return;
		}
		auraCharge = 0;
		if (auraSecond) {
			auraSecond = false;
			if (!auraSent && auraHeld < HOLD) {
				// Let go quickly: a double tap after all. A tap held back for it goes with it as the slash it was where there's no step
				// (Edge); one that went already goes again, as before.
				if (!auraFirstWaited || !has(client, dev.wildercord.api.AuraApi.Trigger.DOUBLE_TAP)) {
					sendAura(dev.wildercord.api.AuraApi.Trigger.TAP);
				}
				sendAura(dev.wildercord.api.AuraApi.Trigger.DOUBLE_TAP);
			}
			// Let go after it was clearly held but before it completed: nothing at all (an awakening called off).
			auraTapAt = Long.MIN_VALUE / 2;
			auraHeld = -1;
			return;
		}
		if (!auraSent) {
			long now = gameTime;
			boolean second = now - auraTapAt <= DOUBLE_TAP;
			if (tapsWait(client)) {
				if (second && auraTapWaiting) {
					auraTapWaiting = false;
					sendAura(dev.wildercord.api.AuraApi.Trigger.DOUBLE_TAP);
					auraTapAt = Long.MIN_VALUE / 2;
				} else {
					auraTapWaiting = true;
					auraTapAt = now;
				}
			} else {
				sendAura(dev.wildercord.api.AuraApi.Trigger.TAP);
				if (second) {
					sendAura(dev.wildercord.api.AuraApi.Trigger.DOUBLE_TAP);
					auraTapAt = Long.MIN_VALUE / 2;
				} else {
					auraTapAt = now;
				}
			}
		}
		auraHeld = -1;
	}

	private static void tick(Minecraft client) {
		while (magicSettings.consumeClick()) if (client.player != null) client.gui.setScreen(new MagicSettingsScreen());
		if (client.player == null || !client.player.isAlive()) {
			// Dead or out of the world: whatever was held is dropped, and nothing is cast.
			castHeld = -1;
			charging = false;
			nextHeld = -1;
			auraHeld = -1;
			auraSecond = false;
			auraCharge = 0;
			while (cast.consumeClick()) {
				// Discarded.
			}
			while (aura.consumeClick()) {
				// Discarded.
			}
		}
		boolean playing = client.player != null && client.player.isAlive() && client.gui.screen() == null;
		RelayClient.beginTick(client);
		if (!RelayClient.key(client, cast, -1) || charging) {
		// The cast key: a tap casts at once; held, the spell charges until it's let go.
		while (cast.consumeClick()) {
			if (castHeld < 0 && playing) {
				castHeld = 0;
			}
		}
		// While a screen is open (the spell wheel, chat, the pause menu) a held cast just waits, like a
		// drawn bow: opening one lets go of every key, which mustn't count as letting go of the spell.
		if (castHeld >= 0 && playing) {
			if (cast.isDown()) {
				castHeld++;
				if (castHeld == HOLD && !charging) {
					charging = true;
					ClientPlayNetworking.send(new WildercordNetworking.ChargeSpell(-1, true));
				}
			} else {
				if (client.player != null) {
					if (charging) {
						// How well its glyph was traced goes first, so it's there when the release arrives.
						SigilTrace.release();
						ClientPlayNetworking.send(new WildercordNetworking.ChargeSpell(-1, false));
					} else {
						ClientPlayNetworking.send(new WildercordNetworking.CastSpell(-1));
					}
				}
				castHeld = -1;
				charging = false;
			}
		}
		} else {
			while (cast.consumeClick()) {}
			castHeld = -1; charging = false;
		}
		// The switch key: a tap moves on; held, the spell wheel opens.
		while (next.consumeClick()) {
			RelayClient.cancel();
			if (nextHeld < 0 && playing) {
				nextHeld = 0;
			}
		}
		if (nextHeld >= 0 && playing) {
			CordTier tier = Spellbooks.tier(client.player);
			if (next.isDown()) {
				nextHeld++;
				// The wheel needs two spells to choose between: the Cord's, and the tome's while it's held.
				if (nextHeld == HOLD && tier != null
						&& dev.wildercord.gear.SpellSlots.open(tier.spells, dev.wildercord.gear.Gear.tome(client.player)).size() > 1) {
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
		auraKey(client, playing);
		while (open.consumeClick()) {
			if (client.player != null && client.gui.screen() == null) {
				client.gui.setScreen(new CordScreen());
			}
		}
		while (backpack.consumeClick()) {
			if (playing) {
				openBackpack();
			}
		}
		while (nextLoadout.consumeClick()) {
			if (playing) {
				ClientPlayNetworking.send(new WildercordNetworking.LoadoutRequest(dev.wildercord.loadout.Loadouts.NEXT, -1, ""));
			}
		}
		for (int i = 0; i < CAST_N.length; i++) {
			boolean relay = RelayClient.key(client, CAST_N[i], i);
			while (CAST_N[i].consumeClick()) {
				if (client.player != null) {
					if (!relay && playing) ClientPlayNetworking.send(new WildercordNetworking.CastSpell(i));
				}
			}
		}
	}
}
