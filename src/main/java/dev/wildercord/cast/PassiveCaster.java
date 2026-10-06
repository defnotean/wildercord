package dev.wildercord.cast;

import dev.wildercord.content.CordTier;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Passives;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps passives running. Every second each passive that's on costs its upkeep; if the mana
 * isn't there it falters (stops renewing) until it is. There's no cooldown: a Self passive is
 * renewed every two seconds, an Orbit whenever its orbs run out. After the first
 * cast renewals are quiet, so a passive buff doesn't sparkle every two seconds.
 */
public final class PassiveCaster {
	private PassiveCaster() {}

	private static final class State {
		String key;
		long nextCast;
		boolean shown;
		boolean faltering;
		/** Counts the passive's casts: only the latest one's Orbit may fly (see {@link #running}). */
		int casts;
		/** The player and world it was cast in: after a dimension change the passive is cast afresh. */
		ServerPlayer caster;
		net.minecraft.world.level.Level level;
	}

	private static final Map<UUID, State[]> STATES = new HashMap<>();

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
	}

	/** The runes of a passive that will actually run: learned, held by the Cord, and inside its sockets. */
	public static List<RuneDef> activeRunes(List<String> ids, Spellbook book, CordTier tier) {
		List<RuneDef> runes = new ArrayList<>();
		if (tier == null || dev.wildercord.spell.RelayRules.containsIds(ids) || dev.wildercord.spell.ReweaveRules.containsIds(ids)) {
			return runes;
		}
		for (int i = 0; i < Math.min(ids.size(), sockets(tier)); i++) {
			Optional<RuneDef> rune = Runes.get(ids.get(i));
			if (rune.isPresent() && book.knows(rune.get().id()) && tier.holds(rune.get().tier()) && Passives.allowed(rune.get())) {
				runes.add(rune.get());
			}
		}
		return runes;
	}

	public static int sockets(CordTier tier) {
		return Math.min(Passives.SOCKETS, tier.sockets);
	}

	/** Every 5 ticks, from SpellCaster. */
	public static void tick(ServerPlayer player, int tickCount) {
		State[] states = STATES.computeIfAbsent(player.getUUID(), k -> {
			State[] fresh = new State[Passives.MAX];
			for (int i = 0; i < fresh.length; i++) {
				fresh[i] = new State();
			}
			return fresh;
		});
		CordTier tier = Spellbooks.tier(player);
		Spellbook book = Spellbooks.get(player);
		int slots = Passives.slots(Heart.active(player));
		long now = player.level().getGameTime();
		boolean second = tickCount % 20 == 0;
		for (int slot = 0; slot < Passives.MAX; slot++) {
			State state = states[slot];
			List<RuneDef> runes = slot < slots && tier != null && book.passiveOn(slot) && player.isAlive() && !player.isSpectator()
				? activeRunes(book.passives().get(slot), book, tier) : List.of();
			SpellCompiler.Compiled compiled = runes.isEmpty() || Passives.problem(runes) != null ? null : SpellCompiler.compile(runes);
			if (compiled == null || compiled.isEmpty()) {
				state.key = null;
				continue;
			}
			String key = String.join(",", runes.stream().map(RuneDef::id).toList());
			boolean fresh = false;
			if (!key.equals(state.key) || state.caster != player || state.level != player.level()) {
				// A new passive, or its Orbit ended with the old world: cast it again now.
				state.key = key;
				state.nextCast = 0;
				state.shown = false;
				state.caster = player;
				state.level = player.level();
				fresh = true;
			}
			// A passive starting up pays its first second straight away: switched on and off between two
			// seconds, it would otherwise cast its buffs without ever paying (and with no mana at all).
			if ((second || fresh) && !player.isCreative()) {
				float upkeep = Heart.upkeep(player, compiled);
				float mana = Spellbooks.mana(player);
				state.faltering = mana < upkeep;
				if (!state.faltering) {
					Spellbooks.setMana(player, mana - upkeep);
				}
			}
			if (state.faltering || now < state.nextCast) {
				continue;
			}
			int index = slot;
			int count = ++state.casts;
			Runnable cast = () -> CastEngine.cast(player, compiled.root(), 1, Heart.bonuses(player), true, () -> running(player, index, key, count));
			if (state.shown) {
				Fx.quietly(cast);
			} else {
				cast.run();
				dev.wildercord.advancement.Advancements.moment(player, dev.wildercord.advancement.Advancements.PASSIVE);
			}
			state.shown = true;
			state.nextCast = now + Passives.interval(compiled.root());
		}
	}

	/**
	 * Whether this cast of a passive is still the one running in its slot: its Orbit ends the moment it isn't. Only the
	 * latest cast counts, so switching a passive off and on again (or changing it and back) starts a fresh ring of orbs
	 * instead of bringing the old one back beside it, which stacked a ring for every switch.
	 */
	public static boolean running(ServerPlayer player, int slot, String key, int cast) {
		State[] states = STATES.get(player.getUUID());
		return states != null && key.equals(states[slot].key) && !states[slot].faltering && states[slot].casts == cast;
	}

	public static void forget(UUID player) {
		STATES.remove(player);
	}
}
