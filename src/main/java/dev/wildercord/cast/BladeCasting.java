package dev.wildercord.cast;

import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Spellblade's bridge into the cast engine (see {@code aura.Spellblade}): a spell held on a blade lands on what the blade's
 * Aura Slash cuts, in place of its own shape. Everything else is the spell as written, through the same code as any cast:
 * each group's effects go through {@link CastEngine#onHit} (so Shields, the spell defences and the PvP cap in
 * {@code Effects.hurt}, mastery, reactions, On Hit and its kin all apply), a group that acts on the caster (Self) goes off on
 * the caster as usual, and the first segment's own link (Delay, a condition, Echo...) follows once the slash has flown.
 */
public final class BladeCasting {
	private BladeCasting() {}

	/** Whether a group acts on its caster (or on what set it off) rather than reaching out: such a group never rides a blade. */
	static boolean onCaster(SpellPlan.Group g) {
		return g.shape.is(Runes.SELF.id()) || g.shape.is(Runes.TRIGGER.id());
	}

	/** Whether a spell has anything for a blade to carry: a group in its first part that reaches out. */
	public static boolean rides(SpellPlan.Segment root) {
		if (root == null) {
			return false;
		}
		for (SpellPlan.Group g : root.groups) {
			if (!onCaster(g) && !g.effects.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** The colour the blade shows while it holds this spell: its first reaching group's element. */
	public static int color(SpellPlan.Segment root) {
		for (SpellPlan.Group g : root.groups) {
			if (!onCaster(g)) {
				return CastEngine.colorOf(g);
			}
		}
		return root.groups.isEmpty() ? 0xFFFFFF : CastEngine.colorOf(root.groups.getFirst());
	}

	private static SpellPlan.Link anchored(SpellPlan.Segment root, SpellPlan.Group g) {
		return root.link != null && root.link.anchor == g ? root.link : null;
	}

	/**
	 * The slash leaves: the spell's first part is taken from the cast's allowance once, and any group of it that acts on the
	 * caster goes off now, as it would have. Returns false when the cast can't go on (its caster gone, its allowance spent).
	 */
	public static boolean begin(Cast cast, SpellPlan.Segment root) {
		if (!cast.alive() || !cast.takeSegment()) {
			return false;
		}
		for (SpellPlan.Group g : root.groups) {
			if (onCaster(g)) {
				CastEngine.deliver(cast, g, Cast.Trigger.self(cast.caster), anchored(root, g));
			}
		}
		return true;
	}

	/** The slash cut {@code target}: every reaching group of the spell lands on it, at {@code power} (each further foe a little weaker). */
	public static void cut(Cast cast, SpellPlan.Segment root, LivingEntity target, Vec3 from, Vec3 dir, double power) {
		if (!cast.alive() || power <= 0) {
			return;
		}
		Vec3 at = target.getBoundingBox().getCenter();
		for (SpellPlan.Group g : root.groups) {
			if (!onCaster(g)) {
				cast.prepareCircle(g);
				CastEngine.onHit(cast, g, new Cast.Hit(List.<Entity>of(target), at, dir, from, null, null, false, power), anchored(root, g));
			}
		}
	}

	/** The slash broke (on a wall, or at the end of its flight) having cut nothing: the spell bursts there instead. */
	public static void broke(Cast cast, SpellPlan.Segment root, Vec3 at, Vec3 dir, Vec3 from, BlockPos block, Direction face) {
		if (!cast.alive()) {
			return;
		}
		for (SpellPlan.Group g : root.groups) {
			if (!onCaster(g)) {
				cast.prepareCircle(g);
				CastEngine.onHit(cast, g, new Cast.Hit(List.of(), at, dir, from, block, face, false), anchored(root, g));
			}
		}
	}

	/** Once the slash has flown: the first part's own link, from where it ended (the rest of the spell follows as written). */
	public static void follow(Cast cast, SpellPlan.Segment root, Vec3 at, Vec3 dir, Entity first) {
		if (cast.alive()) {
			CastEngine.runLink(cast, root, new Cast.Trigger(at, dir, first, null, null));
		}
	}
}
