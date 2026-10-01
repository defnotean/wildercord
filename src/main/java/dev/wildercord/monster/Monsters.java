package dev.wildercord.monster;

import dev.wildercord.cast.Cast;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * The magical monsters of the wilds: Bramblewalkers, Gloomstalkers, Thunderwing Harpies, Geode Crawlers, Bog Witch-Frogs
 * and Mana Oozes. This registers them (see {@link MonsterContent}, {@link MonsterSpawns}) and answers the spells that land
 * on them (their harm is answered as it lands, in each creature's own {@code hurtServer}; this is for what a spell does
 * beyond harm). Every one can also roll Runebound, carrying a spell that suits it ({@link RuneboundKin}).
 */
public final class Monsters {
	private Monsters() {}

	/** How long an earth spell holds a Thunderwing Harpy on the ground. */
	static final int EARTH_GROUNDS = 60;

	public static void init() {
		MonsterContent.init();
		MonsterSpawns.init();
	}

	/**
	 * Every effect of every spell, where it landed: light shows a Gloomstalker (even a spell that does no harm, such as
	 * Light or Reveal), and earth drags a Thunderwing Harpy out of the sky. Cheap when nothing here was hit.
	 */
	public static void onSpell(Cast cast, Cast.Hit hit, RuneDef effect) {
		if (hit.entities().isEmpty() || !(cast.level instanceof ServerLevel level)) {
			return;
		}
		String element = effect.element();
		boolean light = MonsterRules.revealing(element) || effect == Runes.LIGHT || effect == Runes.REVEAL || effect == Runes.GLIMMER;
		boolean earth = element.equals("earth");
		if (!light && !earth) {
			return;
		}
		for (Entity creature : hit.entities()) {
			if (light && creature instanceof Gloomstalker stalker) {
				stalker.reveal(level, MonsterRules.SPELL_REVEAL, cast.caster);
			} else if (earth && creature instanceof ThunderwingHarpy harpy && harpy.isAlive()) {
				harpy.ground(level, EARTH_GROUNDS);
			}
		}
	}
}
