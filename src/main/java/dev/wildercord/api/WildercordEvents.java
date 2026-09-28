package dev.wildercord.api;

import dev.wildercord.spell.RuneDef;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Events add-ons can listen to. All fire on the server thread. Register listeners in
 * {@link WildercordAddon#onWildercordInit} (or any time after), e.g.
 * {@code WildercordEvents.AFTER_CAST.register((player, spell, runes, spent) -> ...)}.
 *
 * @since 1.0
 */
public final class WildercordEvents {
	private WildercordEvents() {}

	/** An add-on listener that throws is logged (once) and skipped, so one bad add-on can't kick a player or stop the server's tick. */
	private static final java.util.Set<Class<?>> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private static void guard(Object listener, Runnable call) {
		try {
			call.run();
		} catch (RuntimeException e) {
			if (FAILED.add(listener.getClass())) {
				dev.wildercord.Wildercord.LOGGER.error("A Wildercord add-on listener ({}) threw; it's being skipped", listener.getClass().getName(), e);
			}
		}
	}

	private static boolean guard(Object listener, java.util.function.BooleanSupplier call, boolean otherwise) {
		try {
			return call.getAsBoolean();
		} catch (RuntimeException e) {
			if (FAILED.add(listener.getClass())) {
				dev.wildercord.Wildercord.LOGGER.error("A Wildercord add-on listener ({}) threw; it's being skipped", listener.getClass().getName(), e);
			}
			return otherwise;
		}
	}

	/**
	 * A player is about to cast one of their spells: every check has passed (Cord, cooldown, mana) but
	 * nothing is spent yet. Return false to stop it (tell the player why yourself); nothing is charged.
	 */
	public static final Event<BeforeCast> BEFORE_CAST = EventFactory.createArrayBacked(BeforeCast.class, listeners -> (player, spell, runes, cost) -> {
		for (BeforeCast listener : listeners) {
			if (!guard(listener, () -> listener.allow(player, spell, runes, cost), true)) {
				return false;
			}
		}
		return true;
	});

	/** A player's spell was just cast (paid for, and on its way). */
	public static final Event<AfterCast> AFTER_CAST = EventFactory.createArrayBacked(AfterCast.class, listeners -> (player, spell, runes, spent) -> {
		for (AfterCast listener : listeners) {
			guard(listener, () -> listener.afterCast(player, spell, runes, spent));
		}
	});

	/** A spell's shape hit something (creatures, a block or just a point), just before its effects apply. */
	public static final Event<SpellHit> SPELL_HIT = EventFactory.createArrayBacked(SpellHit.class, listeners -> (caster, targets, point, effects) -> {
		for (SpellHit listener : listeners) {
			guard(listener, () -> listener.onHit(caster, targets, point, effects));
		}
	});

	/** A Shield stopped a spell (a parry, which turns it back, counts too). */
	public static final Event<SpellBlocked> SPELL_BLOCKED = EventFactory.createArrayBacked(SpellBlocked.class, listeners -> (caster, shielded, weight, shield) -> {
		for (SpellBlocked listener : listeners) {
			guard(listener, () -> listener.onBlocked(caster, shielded, weight, shield));
		}
	});

	/** A spell stored by Imbue was released (by a weapon, a tool, armour, an arrow, a glyph...). */
	public static final Event<ImbueReleased> IMBUE_RELEASED = EventFactory.createArrayBacked(ImbueReleased.class, listeners -> (owner, runes, at, target) -> {
		for (ImbueReleased listener : listeners) {
			guard(listener, () -> listener.onRelease(owner, runes, at, target));
		}
	});

	@FunctionalInterface
	public interface BeforeCast {
		/**
		 * @param spell the spell slot (0-3 the Cord's, 4 the Tome of the Fifth Page's)
		 * @param runes the runes that will fire
		 * @param cost  the mana it's about to cost (0 for one paid in health or a wild surge's free recast;
		 *              an overcast's full price, asked once, after its confirming second press)
		 * @return false to stop the cast
		 */
		boolean allow(ServerPlayer player, int spell, List<RuneDef> runes, int cost);
	}

	@FunctionalInterface
	public interface AfterCast {
		/** @param spent the mana spent (or health, times 5, for Blood Price) */
		void afterCast(ServerPlayer player, int spell, List<RuneDef> runes, int spent);
	}

	@FunctionalInterface
	public interface SpellHit {
		/**
		 * @param caster  who cast it (a player or a monster)
		 * @param targets the creatures hit (maybe none)
		 * @param effects the effects about to apply
		 */
		void onHit(LivingEntity caster, List<Entity> targets, Vec3 point, List<RuneDef> effects);
	}

	@FunctionalInterface
	public interface SpellBlocked {
		/**
		 * @param shielded who the Shield guarded
		 * @param weight   how much mana the stopped spell asked
		 * @param shield   the Shield's strength (it stops spells up to this weight)
		 */
		void onBlocked(LivingEntity caster, LivingEntity shielded, double weight, float shield);
	}

	@FunctionalInterface
	public interface ImbueReleased {
		/**
		 * @param owner  who imbued it
		 * @param runes  the stored runes
		 * @param at     where it goes off
		 * @param target what set it off, or null
		 */
		void onRelease(ServerPlayer owner, List<RuneDef> runes, Vec3 at, Entity target);
	}
}
