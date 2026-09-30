package dev.wildercord.cast;

import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.player.WildercordAttachments.Soaring;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Soar: real flight for a while, as in creative (double-tap jump to take off, jump and sneak to rise and
 * sink), only slower. A player it lifts gets {@code mayfly} and a gentler flying speed; three seconds before
 * the end the wind starts to fade (a sound, wisps peeling off the wings and a line over the hotbar), and when
 * it ends it sets them down as Feather Fall does: slow falling, a drift the way they look, and no fall damage
 * until they touch the ground. Running out of flight never kills.
 *
 * <p>It's a burst of flight, not a way to live in the sky: casting it on someone already soaring is refused
 * (and gives its mana back), so a flight lasts its duration and no longer, and when a flight ends for any
 * reason but death the wings rest for 30 seconds ({@link WildercordAttachments#SOAR_REST}, saved) before it
 * lifts that player again.</p>
 *
 * <p>The rules that keep it from ever leaving a player flying who shouldn't be:</p>
 * <ul>
 *   <li>Creative and spectator players are never touched, and nor is a player who can already fly some
 *       other way: Soar only lifts those who can't, and only ever takes back a flight it gave. The saved
 *       {@link WildercordAttachments#SOARING} is the note that it gave one.</li>
 *   <li>Logging out takes the flight away before the player is saved and keeps the note, so the saved
 *       player can't fly; logging back in gives back whatever time is left (hovering, if they're in the
 *       air), or lets them down gently if it ran out. A crash that saved them mid-flight is tidied the same
 *       way, and so is a server stopping.</li>
 *   <li>Death ends it outright; a new world (a portal, a teleport) ends it gently.</li>
 *   <li>A server with flight turned off never kicks a soaring player: the server only counts a player as
 *       floating when they may not fly, and they may; the fall afterwards is fast enough not to count.</li>
 *   <li>The dungeons' warded arenas and vaults still the wind: it won't lift anyone inside, and a flier
 *       who comes in is set down.</li>
 *   <li>Pulls (anything that leaves a creature pulled: Pull, Gravity Well, a vortex...) and the winds that
 *       ground fliers (Weigh, Downdraft) tear it away, and the rest follows.</li>
 * </ul>
 * Only players fly; any other ally the wind touches falls slowly for as long instead.
 */
public final class Soar {
	private Soar() {}

	/** The wind's text colour, and the ward's violet for the ward's own message. */
	private static final int WIND_TEXT = 0xBFE8D8;
	private static final int WARD_TEXT = 0xB48CFF;

	/** Everyone Soar is looking after (flying or coming down), by id: fake players too, which aren't in the player list. */
	private static final Map<UUID, Flier> FLIERS = new HashMap<>();

	/** What Soar remembers of a flier between ticks (the rest is in their saved note). */
	private static final class Flier {
		final ServerPlayer player;
		boolean wasFlying;
		boolean warned;
		long nextGust;
		int drift;

		Flier(ServerPlayer player) {
			this.player = player;
			this.wasFlying = player.getAbilities().flying;
		}
	}

	/** How a flight ended, for what the flier is told and shown. */
	private enum Ending { RAN_OUT, GROUNDED, WARDED, TRAVELLED, TAKEN }

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Soar::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> login(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> leave(handler.player));
		// Before the players are saved for the last time: none of them is saved able to fly.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Flier flier : List.copyOf(FLIERS.values())) {
				leave(flier.player);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> FLIERS.clear());
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				stop(player);
			}
		});
		// The body a player comes back in (after dying, or leaving the End) starts with the flight its game mode
		// gives it: a note carried over with the rest of their things is of a flight that's gone. After the whole
		// respawn, so whatever order the carrying over happens in, it's done. Leaving the End mid-flight ends the
		// flight as any new world does, and the wings rest.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			Soaring note = oldPlayer.getAttached(WildercordAttachments.SOARING);
			FLIERS.remove(oldPlayer.getUUID());
			newPlayer.removeAttached(WildercordAttachments.SOARING);
			if (alive && note != null && !note.falling()) {
				rest(newPlayer);
			}
		});
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> end(player, Ending.TRAVELLED));
		// Coming down, a landing never hurts. (While flying, a player who may fly takes no fall damage anyway.)
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(entity instanceof ServerPlayer player && source.is(DamageTypeTags.IS_FALL) && player.hasAttached(WildercordAttachments.SOARING)));
	}

	// ------------------------------------------------------------------ casting

	/**
	 * Soar landing on its allies: players are given flight, anything else falls slowly for as long. Whoever
	 * can't be lifted (already soaring, resting, in a ward...) is refused, and the caster told why; a Soar that
	 * lifted nobody at all gives back what it cost.
	 */
	static void lift(Cast cast, SpellPlan.EffectNode node, List<LivingEntity> helped, int ticks) {
		// Only its first landing in a cast may give its mana back (a Pulse or a Zone lands it again, on those it just lifted).
		boolean first = cast.once("soar:" + System.identityHashCode(node));
		boolean lifted = false;
		for (LivingEntity t : helped) {
			if (!(t instanceof ServerPlayer player)) {
				t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, true));
				t.resetFallDistance();
				SoarVfx.lift(cast.level, t);
				lifted = true;
				continue;
			}
			boolean self = player == cast.caster;
			switch (start(player, ticks)) {
				case LIFT -> {
					SoarVfx.lift(cast.level, player);
					lifted = true;
				}
				case ALREADY_SOARING -> refuse(cast, self
					? Component.translatableWithFallback("message.wildercord.soar_soaring", "Already soaring")
					: Component.translatableWithFallback("message.wildercord.soar_soaring_ally", "%s is already soaring", player.getDisplayName()));
				case RESTING -> {
					int left = SoarRules.restSecondsLeft(player.level().getGameTime(), restingUntil(player));
					refuse(cast, self
						? Component.translatableWithFallback("message.wildercord.soar_resting", "Your wings need rest: %s s", left)
						: Component.translatableWithFallback("message.wildercord.soar_resting_ally", "%s's wings need rest: %s s", player.getDisplayName(), left));
				}
				case WARDED -> {
					SoarVfx.stilled(cast.level, player);
					tell(player, "soar_warded", "The ward stills the wind here", WARD_TEXT);
				}
				case ALREADY_FLIES -> {
					if (self) {
						tell(player, "soar_already", "You can already fly", WIND_TEXT);
					}
				}
				// Creative and spectator players are left alone, even by the particles.
				case CREATIVE -> { }
			}
		}
		if (first && !lifted) {
			refund(cast, node);
		}
	}

	/** Tells a player who cast Soar why it didn't lift someone. */
	private static void refuse(Cast cast, Component why) {
		if (cast.caster instanceof ServerPlayer caster) {
			tell(caster, why, WIND_TEXT);
		}
	}

	/**
	 * A Soar that lifted nobody gives back its share of what the spell cost, as a condition that doesn't hold gives
	 * back its branch's (see {@code CastEngine.refund}): only for a spell cast from a Cord and paid in mana, once.
	 */
	private static void refund(Cast cast, SpellPlan.EffectNode node) {
		if (!(cast.caster instanceof ServerPlayer player) || cast.passive || cast.origin() != null || player.isCreative()
				|| cast.info.root() == null || cast.info.spell().isEmpty()) {
			return;
		}
		SpellCompiler.Compiled compiled = SpellCompiler.compile(cast.info.spell());
		if (compiled.paysInHealth() || compiled.cost() <= 0) {
			return;
		}
		double share = SpellCompiler.effectShare(cast.info.root(), node);
		int paid = dev.wildercord.player.Heart.manaCost(player, compiled, 1.0);
		float back = (float) Math.min(paid, paid * share);
		if (back > 0) {
			dev.wildercord.player.Spellbooks.setMana(player, Math.min(dev.wildercord.player.Mana.max(player), dev.wildercord.player.Spellbooks.mana(player) + back));
		}
	}

	/**
	 * Gives {@code player} a flight of {@code ticks}. Returns whether it did, and if not, why (see
	 * {@link SoarRules#lift}): a flight is never renewed, and never given while the wings rest.
	 */
	public static SoarRules.Lift start(ServerPlayer player, int ticks) {
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		boolean soaring = note != null && !note.falling();
		long now = player.level().getGameTime();
		Abilities abilities = player.getAbilities();
		SoarRules.Lift lift = SoarRules.lift(player.isCreative() || player.isSpectator(), abilities.mayfly, soaring,
			DungeonWards.warded(player.level(), player.blockPosition()), SoarRules.resting(now, restingUntil(player)));
		if (lift != SoarRules.Lift.LIFT) {
			return lift;
		}
		player.removeAttached(WildercordAttachments.SOAR_REST);
		player.setAttached(WildercordAttachments.SOARING, new Soaring(now + ticks, SoarRules.priorSpeed(abilities.getFlyingSpeed()), false));
		abilities.mayfly = true;
		abilities.setFlyingSpeed(SoarRules.FLY_SPEED);
		// Cast while falling (off a cliff, thrown up by something), the wind catches you at once.
		if (airborne(player)) {
			abilities.flying = true;
		}
		player.onUpdateAbilities();
		player.resetFallDistance();
		Flier flier = new Flier(player);
		flier.nextGust = now + SoarRules.GUST_TICKS;
		FLIERS.put(player.getUUID(), flier);
		return lift;
	}

	/** When {@code player}'s wings have rested (0 if they aren't resting, or never flew). */
	public static long restingUntil(ServerPlayer player) {
		Long until = player.getAttached(WildercordAttachments.SOAR_REST);
		return until == null ? 0L : until;
	}

	/** A flight just ended (any way but death): the wings rest before the wind lifts them again. */
	private static void rest(ServerPlayer player) {
		long now = player.level().getGameTime();
		player.setAttached(WildercordAttachments.SOAR_REST, SoarRules.restUntil(now, restingUntil(player)));
	}

	/**
	 * A grounding hit on {@code target}: a pull, Weigh's weight or Downdraft's slam. A soaring player loses the
	 * flight (and comes down gently, unless the hit itself takes that away too), and the wings rest as after any
	 * flight.
	 */
	public static void ground(LivingEntity target) {
		if (!(target instanceof ServerPlayer player)) {
			return;
		}
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		if (note == null || note.falling()) {
			return;
		}
		end(player, Ending.GROUNDED);
	}

	/** Whether {@code player} is soaring right now (not yet coming down). */
	public static boolean soaring(ServerPlayer player) {
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		return note != null && !note.falling();
	}

	/** Whether {@code player} is coming down gently after a flight. */
	public static boolean descending(ServerPlayer player) {
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		return note != null && note.falling();
	}

	/** Whether {@code player}'s flight has started to fade (its warning has been given). */
	public static boolean fading(ServerPlayer player) {
		Flier flier = FLIERS.get(player.getUUID());
		return flier != null && flier.player == player && flier.warned && soaring(player);
	}

	// ------------------------------------------------------------------ the flight, tick by tick

	private static void tick(MinecraftServer server) {
		if (FLIERS.isEmpty()) {
			return;
		}
		for (Flier flier : List.copyOf(FLIERS.values())) {
			ServerPlayer player = flier.player;
			Soaring note = player.getAttached(WildercordAttachments.SOARING);
			if (player.isRemoved() || !player.isAlive() || note == null) {
				FLIERS.remove(player.getUUID(), flier);
				continue;
			}
			if (player.isCreative() || player.isSpectator()) {
				forget(player, note);
				continue;
			}
			long now = player.level().getGameTime();
			if (note.falling()) {
				descend(flier, note, now);
			} else {
				fly(flier, note, now);
			}
		}
	}

	private static void fly(Flier flier, Soaring note, long now) {
		ServerPlayer player = flier.player;
		Abilities abilities = player.getAbilities();
		if (!abilities.mayfly || SoarRules.stale(now, note.until())) {
			// Something else took the flight away (or it can't be trusted): it's over, gently.
			end(player, Ending.TAKEN);
			return;
		}
		if (now >= note.until()) {
			end(player, Ending.RAN_OUT);
			return;
		}
		if ((now + player.getId()) % SoarRules.WARD_CHECK_TICKS == 0 && DungeonWards.warded(player.level(), player.blockPosition())) {
			end(player, Ending.WARDED);
			return;
		}
		if (SoarRules.warnNow(now, note.until(), flier.warned)) {
			flier.warned = true;
			SoarVfx.fading(player.level(), player);
			tell(player, "soar_fading", "The wind beneath you is fading...", WIND_TEXT);
		}
		boolean flying = abilities.flying;
		if (flying && !flier.wasFlying) {
			SoarVfx.takeOff(player.level(), player);
			flier.nextGust = now + SoarRules.GUST_TICKS;
		}
		flier.wasFlying = flying;
		if (flying && now >= flier.nextGust) {
			Vec3 moved = player.getKnownSpeed();
			dev.wildercord.cast.feel.Feels.sound(player.level(), player.position().add(0, 1, 0), "wind_soar_gust", SoarRules.gustVolume(moved.length()), 1.0F);
			int spread = SoarRules.GUST_TICKS / 3;
			flier.nextGust = now + SoarRules.GUST_TICKS - spread + player.getRandom().nextInt(2 * spread + 1);
		}
	}

	/** Coming down: slow falling, Feather Fall's drift, and no fall damage, until they're down (or the guard runs out). */
	private static void descend(Flier flier, Soaring note, long now) {
		ServerPlayer player = flier.player;
		if (landed(player) || now >= note.until() || SoarRules.stale(now, note.until())) {
			player.removeAttached(WildercordAttachments.SOARING);
			FLIERS.remove(player.getUUID(), flier);
			return;
		}
		cushion(player);
		if (++flier.drift % 2 == 0) {
			Effects.glide(player);
		}
		if (flier.drift % 12 == 0) {
			SoarVfx.drifting(player.level(), player);
		}
	}

	/** Down: on the ground, in water or lava, on a ladder, riding, gliding, asleep, or flying some other way. */
	private static boolean landed(ServerPlayer player) {
		return player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger() || player.isFallFlying()
			|| player.isSleeping() || player.getAbilities().flying;
	}

	private static boolean airborne(ServerPlayer player) {
		return !player.onGround() && !player.isInWater() && !player.isInLava() && !player.onClimbable() && !player.isPassenger() && !player.isFallFlying();
	}

	/** Slow falling kept topped up (quietly: no swirl of particles), and no fall counted. */
	private static void cushion(ServerPlayer player) {
		MobEffectInstance slow = player.getEffect(MobEffects.SLOW_FALLING);
		if (slow == null || slow.getDuration() < 10) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
		}
		player.resetFallDistance();
	}

	/**
	 * A flight ends: the flight it gave is taken back, the flying speed put back, and the gentle descent begins
	 * (nothing happens if it was already over).
	 */
	private static void end(ServerPlayer player, Ending ending) {
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		if (note == null || note.falling()) {
			return;
		}
		if (player.isCreative() || player.isSpectator()) {
			forget(player, note);
			return;
		}
		takeBack(player, note);
		player.onUpdateAbilities();
		if (player.connection != null) {
			player.connection.resetFlyingTicks();
		}
		rest(player);
		long now = player.level().getGameTime();
		player.setAttached(WildercordAttachments.SOARING, new Soaring(now + SoarRules.DESCENT_TICKS, note.speed(), true));
		FLIERS.computeIfAbsent(player.getUUID(), id -> new Flier(player));
		cushion(player);
		switch (ending) {
			case RAN_OUT -> {
				SoarVfx.setDown(player.level(), player);
				tell(player, "soar_ended", "The wind sets you down gently", WIND_TEXT);
			}
			case GROUNDED -> {
				SoarVfx.grounded(player.level(), player);
				tell(player, "soar_grounded", "Grounded! The wind is torn out from under you", WIND_TEXT);
			}
			case WARDED -> {
				SoarVfx.stilled(player.level(), player);
				tell(player, "soar_warded", "The ward stills the wind here", WARD_TEXT);
			}
			case TRAVELLED -> {
				SoarVfx.setDown(player.level(), player);
				tell(player, "soar_travelled", "The wind doesn't follow you between worlds", WIND_TEXT);
			}
			case TAKEN -> SoarVfx.setDown(player.level(), player);
		}
	}

	/** The flight Soar gave taken back from the player's abilities (they're sent by whoever calls this). */
	private static void takeBack(ServerPlayer player, Soaring note) {
		Abilities abilities = player.getAbilities();
		abilities.mayfly = false;
		abilities.flying = false;
		abilities.setFlyingSpeed(SoarRules.restoredSpeed(abilities.getFlyingSpeed(), note.speed()));
	}

	/**
	 * A player who turned creative or spectator: their flight is their game mode's now. Only the speed Soar set goes
	 * back (and a flight cut short this way rests the wings, as any does).
	 */
	private static void forget(ServerPlayer player, Soaring note) {
		if (!note.falling()) {
			rest(player);
		}
		Abilities abilities = player.getAbilities();
		float speed = SoarRules.restoredSpeed(abilities.getFlyingSpeed(), note.speed());
		if (speed != abilities.getFlyingSpeed()) {
			abilities.setFlyingSpeed(speed);
			player.onUpdateAbilities();
		}
		player.removeAttached(WildercordAttachments.SOARING);
		FLIERS.remove(player.getUUID());
	}

	/** Death: everything goes, at once. */
	public static void stop(ServerPlayer player) {
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		FLIERS.remove(player.getUUID());
		if (note == null) {
			return;
		}
		if (!note.falling() && !player.isCreative() && !player.isSpectator()) {
			takeBack(player, note);
			player.onUpdateAbilities();
		}
		player.removeAttached(WildercordAttachments.SOARING);
	}

	// ------------------------------------------------------------------ logging out and in

	/**
	 * A player leaving (or the server stopping): the flight is taken away before they're saved, so the saved
	 * player can't fly whatever happens next, but the note stays, so logging back in gives back what's left.
	 * No packets: the connection is closing.
	 */
	static void leave(ServerPlayer player) {
		FLIERS.remove(player.getUUID());
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		if (note != null && !note.falling() && !player.isCreative() && !player.isSpectator()) {
			takeBack(player, note);
		}
	}

	/**
	 * A player logging in with a note of a flight: whatever time it has left is given back (hovering, if they're
	 * in the air), or, if it ran out while they were away (or they were saved in the middle of a crash), what's
	 * left of it is taken away and they're let down gently. Creative and spectator players just lose the note.
	 */
	public static void login(ServerPlayer player) {
		// A rest that's over (or from another world's clock) is only clutter in the saved player.
		if (player.hasAttached(WildercordAttachments.SOAR_REST) && !SoarRules.resting(player.level().getGameTime(), restingUntil(player))) {
			player.removeAttached(WildercordAttachments.SOAR_REST);
		}
		Soaring note = player.getAttached(WildercordAttachments.SOARING);
		if (note == null) {
			return;
		}
		if (player.isCreative() || player.isSpectator()) {
			forget(player, note);
			return;
		}
		long now = player.level().getGameTime();
		Abilities abilities = player.getAbilities();
		switch (SoarRules.onLogin(now, note.until(), note.falling())) {
			case RESUME -> {
				abilities.mayfly = true;
				abilities.setFlyingSpeed(SoarRules.FLY_SPEED);
				if (airborne(player)) {
					abilities.flying = true;
				}
				player.onUpdateAbilities();
				Flier flier = new Flier(player);
				flier.nextGust = now + SoarRules.GUST_TICKS;
				FLIERS.put(player.getUUID(), flier);
			}
			case DESCEND -> {
				if (!note.falling()) {
					takeBack(player, note);
					player.onUpdateAbilities();
					rest(player);
				}
				player.setAttached(WildercordAttachments.SOARING, new Soaring(now + SoarRules.DESCENT_TICKS, note.speed(), true));
				FLIERS.put(player.getUUID(), new Flier(player));
				cushion(player);
			}
		}
	}

	private static void tell(ServerPlayer player, String key, String fallback, int color) {
		tell(player, Component.translatableWithFallback("message.wildercord." + key, fallback), color);
	}

	private static void tell(ServerPlayer player, Component text, int color) {
		player.sendOverlayMessage(text.copy().withColor(color));
	}
}
