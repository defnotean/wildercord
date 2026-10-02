package dev.wildercord.client;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.StringReader;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.config.Config;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sword strings on the client: the player's swings read as they're made ({@code mixin.MinecraftStringsMixin} hands each one
 * here) by a {@link StringReader} against the arts the player has ({@link AuraApi#stringsOf}); a string played asks the server
 * for its art ({@link SwordStrings.Perform}), and the server's word comes back only when it refuses. The indicator
 * ({@link StringHud}) and the sounds follow along.
 *
 * <p>What counts as a swing toward a string: one with an aura weapon, aura on and a method learned, not charging a spell, not a
 * spectator, no screen open, at a creature under the crosshair (anything living), or at nothing while in a fight (a blow given
 * or taken in the last few seconds). A swing at a block is digging. What the swing was (full, low, leaping, running) is read as
 * the attack begins, before vanilla's own swing resets the blade and ends a sprint.</p>
 *
 * <p>Before asking, the client checks what it knows itself (the art's rest, the aura it costs, its condition): a string whose
 * art can't go falls through to another art the same swings spell, or is refused here with the reason above the hotbar.</p>
 */
public final class SwordStringsClient {
	private SwordStringsClient() {}

	private static final StringReader<AuraApi.StringArt> READER = new StringReader<>(StringRules.WINDOW);

	/**
	 * What the player was doing as an attack began.
	 *
	 * @param strength how full the swing was (vanilla's attack strength)
	 * @param delay    the weapon's full charge (ticks)
	 * @param marks    low, leaping and running (and full) marks
	 * @param living   whether a living creature was under the crosshair
	 * @param block    whether a block was (digging, not fighting)
	 */
	private record Pending(float strength, float delay, int marks, boolean living, boolean block) {}

	private static Pending pending;
	/** When the player last struck a creature or was struck (game time): a whiff counts toward a string for a while after. */
	private static long engagedAt = Long.MIN_VALUE / 4;
	/** Arts asked for and not yet answered, by id: when. A refusal is matched to its own request. */
	private static final Map<String, Long> ASKED = new HashMap<>();
	/** Rests the client is sure of before the server's arrive (an art it just asked for is resting), by id: until when. */
	private static final Map<String, Long> PREDICTED = new HashMap<>();
	/** When each art's refusal was last said above the hotbar: in a long fight a resting art is refused often, and said once a while. */
	private static final Map<String, Long> TOLD = new HashMap<>();
	/** How long before the same art's refusal is said again (ticks); the marks and the sound say it every time. */
	private static final int TELL_AGAIN = 100;
	private static ClientLevel level;

	/** What the game tests read: the last art asked for, the last fumble's and refusal's art, and how many of each. */
	private static String lastAsked = "";
	private static String lastFumbled = "";
	private static String lastRefused = "";
	private static int asked;
	private static int fumbles;
	private static int refusals;
	private static int strokes;
	/** When the last string was played to its end (game time): its last swing's trail cuts brighter. */
	private static long completedAt = Long.MIN_VALUE / 4;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(SwordStrings.Cue.TYPE, (payload, context) -> cue(payload));
		ClientPlayNetworking.registerGlobalReceiver(SwordStrings.Refused.TYPE, (payload, context) -> refused(payload));
		ClientTickEvents.END_CLIENT_TICK.register(SwordStringsClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
		StringHud.init();
	}

	/** Whether the player's swings are read toward strings now. */
	static boolean reading(Minecraft mc, LocalPlayer player) {
		return player != null && player.isAlive() && !player.isSpectator() && mc.gui.screen() == null && Aura.enabled(player) && Config.strings(player)
			&& Aura.stage(player) >= AuraRules.GLOW && Aura.holdsWeapon(player) && !player.isUsingItem()
			&& player.getAttached(WildercordAttachments.CHARGE) == null;
	}

	/** As an attack begins: notes what the player is doing, before the swing changes it. */
	public static void attackBegins(Minecraft mc) {
		pending = null;
		LocalPlayer player = mc.player;
		if (!reading(mc, player) || mc.level == null) {
			return;
		}
		HitResult hit = mc.hitResult;
		boolean living = hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target && target.isAlive();
		boolean block = hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK && !mc.level.getBlockState(b.getBlockPos()).isAir();
		float strength = player.getAttackStrengthScale(0.5F);
		int marks = SwordString.Token.SWING.bit();
		if (strength >= AuraRules.FULL_SWING - 1.0E-4) {
			marks |= SwordString.Token.FULL.bit();
		}
		if (player.isShiftKeyDown()) {
			marks |= SwordString.Token.LOW.bit();
		}
		if (leaping(player)) {
			marks |= SwordString.Token.LEAP.bit();
		}
		if (player.isSprinting()) {
			marks |= SwordString.Token.RUN.bit();
		}
		pending = new Pending(strength, player.getCurrentItemAttackStrengthDelay(), marks, living, block);
	}

	/** Whether a swing now is a leaping one: in the air, and not swimming, climbing, riding, flying or gliding. */
	static boolean leaping(LocalPlayer player) {
		return !player.onGround() && !player.isInWater() && !player.isInLava() && !player.onClimbable() && !player.isPassenger()
			&& !player.getAbilities().flying && !player.isFallFlying() && !player.isSwimming();
	}

	/** A swing just went (its packets sent): read toward a string. */
	public static void swung(Minecraft mc, boolean thrust) {
		Pending p = pending;
		pending = null;
		LocalPlayer player = mc.player;
		if (p == null || player == null || mc.level == null || (p.block() && !thrust)) {
			return;
		}
		long now = mc.level.getGameTime();
		if (p.living()) {
			engagedAt = now;
		} else if (now - engagedAt > StringRules.ENGAGED_TICKS) {
			// A swing at nothing, in peace: no string.
			return;
		}
		READER.window(Config.stringWindow(player));
		strokes++;
		handle(mc, player, READER.stroke(now, p.marks(), StringRules.recover(p.delay()), AuraApi.stringsOf(player), art -> usable(player, art)), now);
	}

	/** Whether an art can go now, as far as the client knows: rested, its price there, its condition met. */
	static boolean usable(LocalPlayer player, AuraApi.StringArt art) {
		return why(player, art) == null;
	}

	/** Why an art can't go now, as far as the client knows, or null if it can. */
	static SwordStrings.Refusal why(LocalPlayer player, AuraApi.StringArt art) {
		long now = player.level().getGameTime();
		if (now < Math.max(SwordStrings.readyAt(player, art.id()), PREDICTED.getOrDefault(art.id(), Long.MIN_VALUE))) {
			return SwordStrings.Refusal.NOT_READY;
		}
		if (!art.condition().met(player)) {
			return SwordStrings.Refusal.CONDITION;
		}
		if (Aura.aura(player) < SwordStrings.price(player, art) - 1.0E-4) {
			return SwordStrings.Refusal.NO_AURA;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static void handle(Minecraft mc, LocalPlayer player, StringReader.Event event, long now) {
		switch (event) {
			case null -> {
			}
			case StringReader.Landed landed -> {
				List<StringReader.Stroke> chain = READER.chain();
				StringHud.live(chain, READER.deadline(), READER.nextWindow());
				if (MagicQuality.stringIndicator != MagicQuality.StringIndicator.HIDDEN) {
					sound(mc, "aura_string_tick", 0.3F, Feels.step(chain.size() - 1));
				}
			}
			case StringReader.Completed<?> done -> {
				AuraApi.StringArt art = (AuraApi.StringArt) done.art();
				List<Integer> marks = new ArrayList<>();
				for (StringReader.Stroke s : done.strokes()) {
					marks.add(s.marks());
				}
				if (ClientPlayNetworking.canSend(SwordStrings.Perform.TYPE)) {
					ClientPlayNetworking.send(new SwordStrings.Perform(art.id(), marks));
				}
				ASKED.put(art.id(), now);
				if (art.cooldownTicks() > 0) {
					PREDICTED.put(art.id(), now + SwordStrings.rest(player, art));
				}
				lastAsked = art.id();
				asked++;
				completedAt = now;
				StringHud.completed(done.strokes());
				sound(mc, "aura_string_complete", 0.7F, 1.0F);
			}
			case StringReader.Refused<?> no -> {
				AuraApi.StringArt art = (AuraApi.StringArt) no.art();
				SwordStrings.Refusal why = why(player, art);
				Component line = SwordStrings.refusal(player, art, why == null ? SwordStrings.Refusal.CLOSED : why);
				Long told = TOLD.get(art.id());
				if (line != null && (told == null || now - told >= TELL_AGAIN || now < told)) {
					player.sendOverlayMessage(line);
					TOLD.put(art.id(), now);
				}
				lastRefused = art.id();
				refusals++;
				StringHud.refused(no.strokes());
				sound(mc, "aura_string_fumble", 0.45F, 1.2F);
			}
			case StringReader.Fumbled<?> late -> {
				lastFumbled = ((AuraApi.StringArt) late.art()).id();
				fumbles++;
				StringHud.fumbled(late.strokes());
				sound(mc, "aura_string_fumble", 0.6F, 1.0F);
			}
			case StringReader.Lapsed lapsed -> StringHud.lapsed();
		}
	}

	/** A perfect guard or an Aura Step, from the server: the next swing within its moment is a counter or a step cut. */
	static void cue(SwordStrings.Cue payload) {
		Minecraft mc = Minecraft.getInstance();
		StringReader.Cue[] cues = StringReader.Cue.values();
		if (mc.level == null || payload.cue() < 0 || payload.cue() >= cues.length) {
			return;
		}
		long now = mc.level.getGameTime();
		READER.cue(cues[payload.cue()], now);
		// A guard or a step is a move in the fight: a swing at nothing straight after it counts.
		engagedAt = now;
		if (!READER.chain().isEmpty()) {
			StringHud.live(READER.chain(), READER.deadline(), READER.nextWindow());
		}
		StringHud.cued(cues[payload.cue()], now);
	}

	/** The server didn't let an art go: the indicator's completion turns to a fumble. */
	static void refused(SwordStrings.Refused payload) {
		Minecraft mc = Minecraft.getInstance();
		Long at = ASKED.remove(payload.art());
		PREDICTED.remove(payload.art());
		if (mc.level == null || at == null || mc.level.getGameTime() - at > 60) {
			return;
		}
		lastRefused = payload.art();
		refusals++;
		StringHud.refusedByServer();
		sound(mc, "aura_string_fumble", 0.45F, 1.2F);
	}

	static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			if (level != null) {
				reset();
			}
			return;
		}
		if (mc.level != level) {
			// A new world or dimension: nothing carries over.
			reset();
			level = mc.level;
		}
		long now = mc.level.getGameTime();
		if (player.hurtTime > 0) {
			engagedAt = now;
		}
		if (!reading(mc, player)) {
			if (!READER.chain().isEmpty()) {
				READER.reset();
				StringHud.lapsed();
			}
		} else {
			handle(mc, player, READER.tick(now), now);
		}
		ASKED.values().removeIf(at -> now - at > 100 || at > now + 100);
		PREDICTED.values().removeIf(until -> until < now);
		StringHud.tick(now);
	}

	static void reset() {
		READER.reset();
		pending = null;
		engagedAt = Long.MIN_VALUE / 4;
		ASKED.clear();
		PREDICTED.clear();
		TOLD.clear();
		level = null;
		StringHud.reset();
	}

	private static void sound(Minecraft mc, String kit, float volume, float pitch) {
		SoundEvent event = dev.wildercord.content.WildercordSounds.kit(kit);
		LocalPlayer player = mc.player;
		if (event == null || player == null || mc.level == null) {
			return;
		}
		mc.level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), event, SoundSource.PLAYERS, volume, pitch, false);
	}

	/** The counter and step-cut marks a swing struck at {@code now} would carry (without using them up). */
	public static int cueMarks(long now) {
		return READER.cueMarks(now);
	}

	/** When the last string was played to its end (game time). */
	public static long completedAt() {
		return completedAt;
	}

	// ------------------------------------------------------------------ for the game tests

	/** The swings of the string being played, oldest first. */
	public static List<StringReader.Stroke> chain() {
		return READER.chain();
	}

	/** The id of the last art asked for. */
	public static String lastAsked() {
		return lastAsked;
	}

	public static String lastFumbled() {
		return lastFumbled;
	}

	public static String lastRefused() {
		return lastRefused;
	}

	/** How many arts were asked for, strings fumbled and refused, and swings read, since the game started. */
	public static int[] counts() {
		return new int[] {asked, fumbles, refusals, strokes};
	}
}
