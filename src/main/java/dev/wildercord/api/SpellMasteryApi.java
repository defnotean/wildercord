package dev.wildercord.api;

import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.RuneDef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hooks into spell mastery (spells that grow with their caster: see {@code cast.Mastery}), for the rest of the mod and
 * for add-ons. All of them run on the server thread; register them while the mod initialises (or any time after).
 *
 * <ul>
 *   <li>{@link #addCircumstance}: a new circumstance a spell's casts are counted in (a world's rune quirk, standing in a
 *       place of power). Traits can then ask for it, as Undying Flame asks for rain.</li>
 *   <li>{@link #registerTrait} and {@link #addOfferSource}: traits from outside the catalogue, and when to offer them (a
 *       quirk of this world's runes, a residue a spell soaked up), alongside the catalogue's own at each rank.</li>
 *   <li>{@link #setResidueSink}: what a trait with the {@link MasteryTraits.Hook#RESIDUE} hook leaves where its spell lands.
 *       Until something sets one, a faint glimmer is all it leaves.</li>
 * </ul>
 *
 * @since 0.8
 */
public final class SpellMasteryApi {
	private SpellMasteryApi() {}

	/** Whether a player casting {@code spell} right now is in a circumstance. */
	@FunctionalInterface
	public interface Circumstance {
		boolean holds(ServerPlayer caster, List<RuneDef> spell);
	}

	/**
	 * Traits to offer, from outside the catalogue, when a spell reaches {@code rank}: each must already be registered
	 * ({@link #registerTrait}) and fit the spell, and is weighed against the catalogue's own (1 is as likely as any).
	 */
	@FunctionalInterface
	public interface OfferSource {
		List<MasteryTraits.Weighted> offer(ServerPlayer caster, String spellKey, List<RuneDef> spell, int rank);
	}

	/** Where a residue trait's magic is left: {@code at}, by {@code caster}'s {@code spell}, of {@code element} ("" for none). */
	@FunctionalInterface
	public interface ResidueSink {
		void leave(ServerLevel level, Vec3 at, ServerPlayer caster, List<RuneDef> spell, String element, MasteryTraits.Trait trait);
	}

	private static final Map<String, Circumstance> CIRCUMSTANCES = new LinkedHashMap<>();
	private static final List<OfferSource> SOURCES = new ArrayList<>();
	/** Until another is set: a faint glimmer in the spell's colour, gone in a moment. */
	private static final ResidueSink GLIMMER = (level, at, caster, spell, element, trait) -> dev.wildercord.cast.Motes.glows(level, at, 6, 0.4,
		element.isEmpty() ? 0xE8C46A : dev.wildercord.spell.RuneColors.element(element), 0.1, 40, Vec3.ZERO, 0.01);
	private static volatile ResidueSink residue = GLIMMER;

	/**
	 * Counts a spell's casts in a new circumstance. Its id must not be one of {@link MasteryTraits#CIRCUMSTANCES}.
	 *
	 * @throws IllegalArgumentException for an id already in use
	 */
	public static synchronized void addCircumstance(String id, Circumstance test) {
		if (MasteryTraits.CIRCUMSTANCES.contains(id) || CIRCUMSTANCES.containsKey(id)) {
			throw new IllegalArgumentException("Mastery circumstance already exists: " + id);
		}
		CIRCUMSTANCES.put(id, test);
	}

	/** Adds a trait from outside the catalogue (its id namespaced: {@code mymod:echoing}). See {@link MasteryTraits#custom}. */
	public static void registerTrait(MasteryTraits.Trait trait) {
		MasteryTraits.register(trait);
	}

	/** Offers traits from outside the catalogue at each rank. */
	public static synchronized void addOfferSource(OfferSource source) {
		SOURCES.add(source);
	}

	/** Sets what residue traits leave where their spells land (one sink: the residue system's). */
	public static void setResidueSink(ResidueSink sink) {
		residue = sink == null ? GLIMMER : sink;
	}

	// ------------------------------------------------------------------ for Wildercord's own use

	/** Every circumstance added from outside, by id. */
	public static synchronized Map<String, Circumstance> circumstances() {
		return Map.copyOf(CIRCUMSTANCES);
	}

	/** Every trait offered from outside for this spell and rank (a source that throws is skipped). */
	public static List<MasteryTraits.Weighted> offers(ServerPlayer caster, String spellKey, List<RuneDef> spell, int rank) {
		List<OfferSource> sources;
		synchronized (SpellMasteryApi.class) {
			sources = List.copyOf(SOURCES);
		}
		List<MasteryTraits.Weighted> out = new ArrayList<>();
		for (OfferSource source : sources) {
			try {
				out.addAll(source.offer(caster, spellKey, List.copyOf(spell), rank));
			} catch (RuntimeException e) {
				dev.wildercord.Wildercord.LOGGER.warn("A mastery offer source ({}) threw; skipping it", source.getClass().getName(), e);
			}
		}
		return out;
	}

	/** Leaves a residue trait's magic at {@code at}. */
	public static void leaveResidue(ServerLevel level, Vec3 at, ServerPlayer caster, List<RuneDef> spell, String element, MasteryTraits.Trait trait) {
		try {
			residue.leave(level, at, caster, spell, element, trait);
		} catch (RuntimeException e) {
			dev.wildercord.Wildercord.LOGGER.warn("The mastery residue sink threw", e);
		}
	}
}
