package dev.wildercord.cast;

import dev.wildercord.api.SpellMasteryApi;
import dev.wildercord.config.Config;
import dev.wildercord.spell.Attunements;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.Resonance;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneQuirks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Where the world's own magic, spells that grow and the marks magic leaves meet, through the hooks each offers:
 * <ul>
 * <li>A spell's <b>Lingering Mark</b> (a mastery trait) leaves a real residue of its element where it lands (see
 *     {@link Residues}), small, and at most one every {@link #MARK_REST} ticks for each caster.</li>
 * <li>Casting a spell one of whose runes has a quirk in this world, while that quirk holds where you stand, is a
 *     circumstance mastery counts ({@link #QUIRK}); a harmful spell used that way often enough may be offered
 *     <b>World-Tuned</b>, striking harder while the quirk holds.</li>
 * <li>About one in four of a world's harmonies ({@link WorldResonances}) first wakes only at a ley crossing (see
 *     {@link PowerPlaces}): cast elsewhere, its runes stir and say what they're missing. Once found it answers anywhere.</li>
 * </ul>
 */
public final class WorldBonds {
	private WorldBonds() {}

	/** The circumstance of casting a quirked rune while its quirk holds where the caster stands. */
	public static final String QUIRK = "world_quirk";
	/** World-Tuned: a quirked spell's hits, while the quirk holds. Weighed against the catalogue's own traits. */
	public static final MasteryTraits.Trait WORLD_TUNED = new MasteryTraits.Trait("wildercord:world_tuned", "World-Tuned",
		"Strikes 15% harder while this world's quirk on one of its runes holds where you cast it.", MasteryTraits.Hook.DAMAGE, 1.15, QUIRK,
		MasteryTraits.Kind.HARMFUL, Set.of(), Set.of(), QUIRK);
	/** How likely World-Tuned is beside a catalogue trait (1 is as likely). */
	static final double WORLD_TUNED_WEIGHT = 2.0;
	/** Ticks between two Lingering Mark residues from one caster. */
	static final int MARK_REST = 160;
	/** One harmony in this many first wakes only at a ley crossing. */
	static final int CROSSING_SHARE = 4;
	/** Ticks between two "the runes stir" hints to one player. */
	static final int HINT_REST = 200;

	private static final Map<UUID, Long> MARKED = new HashMap<>();
	private static final Map<UUID, Long> HINTED = new HashMap<>();

	public static void init() {
		SpellMasteryApi.setResidueSink(WorldBonds::lingeringMark);
		SpellMasteryApi.addCircumstance(QUIRK, (caster, spell) -> quirkHolds(caster, spell));
		SpellMasteryApi.registerTrait(WORLD_TUNED);
		SpellMasteryApi.addOfferSource((caster, key, spell, rank) -> quirked(caster.level().getServer(), spell)
			? List.of(new MasteryTraits.Weighted(WORLD_TUNED.id(), WORLD_TUNED_WEIGHT)) : List.of());
		WorldResonances.addWakeCondition("wildercord:ley_crossing", WorldBonds::wakesHere);
	}

	/** Lingering Mark: a small residue of the spell's element where it lands, or a glimmer when none can be left. */
	private static void lingeringMark(ServerLevel level, Vec3 at, ServerPlayer caster, List<RuneDef> spell, String element, MasteryTraits.Trait trait) {
		long now = level.getGameTime();
		Long last = MARKED.get(caster.getUUID());
		boolean rested = last == null || now - last >= MARK_REST || now < last;
		if (rested && !element.isEmpty() && Residues.leave(level, element, at, 1.0, caster) > 0) {
			MARKED.put(caster.getUUID(), now);
			return;
		}
		Motes.glows(level, at, 6, 0.4, element.isEmpty() ? 0xE8C46A : RuneColors.element(element), 0.1, 40, Vec3.ZERO, 0.01);
	}

	/** Whether one of {@code spell}'s runes has a quirk in this world. */
	private static boolean quirked(MinecraftServer server, List<RuneDef> spell) {
		if (server == null) {
			return false;
		}
		for (RuneQuirks.Quirk quirk : WorldResonances.quirks(server)) {
			for (RuneDef rune : spell) {
				if (rune.is(quirk.rune())) {
					return true;
				}
			}
		}
		return false;
	}

	/** Whether one of {@code spell}'s runes has a quirk in this world that holds where {@code caster} stands. */
	private static boolean quirkHolds(ServerPlayer caster, List<RuneDef> spell) {
		MinecraftServer server = caster.level().getServer();
		if (server == null) {
			return false;
		}
		Attunements.Place here = null;
		for (RuneQuirks.Quirk quirk : WorldResonances.quirks(server)) {
			for (RuneDef rune : spell) {
				if (rune.is(quirk.rune())) {
					if (here == null) {
						here = WorldQuirks.placeAt(caster.level(), caster.position());
					}
					if (quirk.holds(here)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Whether {@code resonance} is one of this world's that first wakes only at a ley crossing. */
	public static boolean crossingBound(Resonance resonance) {
		// Its name is the world's own, so which ones are bound differs from world to world.
		return Math.floorMod(resonance.name().hashCode(), CROSSING_SHARE) == 0;
	}

	private static boolean wakesHere(ServerPlayer caster, Resonance resonance) {
		boolean crossings = Config.get().elementalClimate() && Config.get().power().leyCrossings();
		if (!crossings || !crossingBound(resonance) || WorldResonances.discovered(caster, resonance.id())
			|| PowerPlaces.isPlaceOfPower(caster.level(), caster.blockPosition())) {
			return true;
		}
		long now = caster.level().getGameTime();
		Long last = HINTED.get(caster.getUUID());
		if (last == null || now - last >= HINT_REST || now < last) {
			HINTED.put(caster.getUUID(), now);
			caster.sendOverlayMessage(Component.translatable("message.wildercord.harmony_needs_crossing")
				.withStyle(ChatFormatting.ITALIC).withColor(resonance.color()));
		}
		return false;
	}
}
