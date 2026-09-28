package dev.wildercord.gear;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.SpellPlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What the casting gear in someone's hands does to their spells. Pure, so it's unit-tested and the
 * readout, the HUD and the server agree.
 *
 * <p>The rules: the same piece in both hands counts once. A staff's power goes to effects of its
 * element only; its discount goes to a whole spell with at least one effect of its element, and two
 * staffs never discount the same spell twice (the better one counts). Foci and the tome work only
 * from the off-hand, and apply to every spell.</p>
 */
public record GearBonuses(List<GearDef> pieces) {
	public static final GearBonuses NONE = new GearBonuses(List.of());

	public GearBonuses {
		pieces = List.copyOf(new LinkedHashSet<>(pieces));
	}

	/** The gear in these hands (either may be null): each piece counts only from a hand it works in. */
	public static GearBonuses of(GearDef mainHand, GearDef offHand) {
		List<GearDef> held = new ArrayList<>(2);
		if (mainHand != null && mainHand.worksIn(true)) {
			held.add(mainHand);
		}
		if (offHand != null && offHand.worksIn(false)) {
			held.add(offHand);
		}
		return held.isEmpty() ? NONE : new GearBonuses(held);
	}

	public boolean isEmpty() {
		return pieces.isEmpty();
	}

	/** Power multiplier on an effect of {@code element} ("" for an effect without one). */
	public double power(String element) {
		double power = 1;
		double staff = 1;
		for (GearDef piece : pieces) {
			power *= piece.power();
			if (piece.kind().staff() && !element.isEmpty() && piece.element().equals(element)) {
				staff = Math.max(staff, piece.elementPower());
			}
		}
		return power * staff;
	}

	/** Cost multiplier on a spell whose effects have these elements. */
	public double cost(Collection<String> elements) {
		double cost = 1;
		double staff = 1;
		for (GearDef piece : pieces) {
			cost *= piece.cost();
			if (piece.kind().staff() && elements.contains(piece.element())) {
				staff = Math.min(staff, piece.elementCost());
			}
		}
		return cost * staff;
	}

	/** How much faster a charged cast fills. */
	public double chargeSpeed() {
		double speed = 1;
		for (GearDef piece : pieces) {
			speed *= piece.chargeSpeed();
		}
		return speed;
	}

	/** Extra max mana. */
	public int mana() {
		int mana = 0;
		for (GearDef piece : pieces) {
			mana += piece.mana();
		}
		return mana;
	}

	/** Chance that a spell echoes (goes off again a moment later). */
	public double echo() {
		double miss = 1;
		for (GearDef piece : pieces) {
			miss *= 1 - piece.echo();
		}
		return 1 - miss;
	}

	/** Whether the Tome of the Fifth Page is in the off-hand. */
	public boolean fifthSpell() {
		for (GearDef piece : pieces) {
			if (piece.fifthSpell()) {
				return true;
			}
		}
		return false;
	}

	/** The staffs held that favour one of these elements (for the charged-cast flourish and the readout). */
	public List<GearDef> staffsFor(Collection<String> elements) {
		List<GearDef> staffs = new ArrayList<>();
		for (GearDef piece : pieces) {
			if (piece.kind().staff() && elements.contains(piece.element())) {
				staffs.add(piece);
			}
		}
		return staffs;
	}

	/** Whether anything held changes a spell with these elements (so the readout mentions it). */
	public boolean changes(Collection<String> elements) {
		return Math.abs(cost(elements) - 1) > 1e-9 || !staffsFor(elements).isEmpty() || pieces.stream().anyMatch(p -> Math.abs(p.power() - 1) > 1e-9);
	}

	// ------------------------------------------------------------------ what a spell is made of

	/** The elements of every effect in a row of runes, a Knot's runes included (for leaning and contracts, what a cast counts toward). */
	public static Set<String> elements(List<RuneDef> runes) {
		Set<String> elements = new LinkedHashSet<>();
		for (RuneDef rune : dev.wildercord.spell.Knots.flatten(runes)) {
			if (rune.family() == RuneFamily.EFFECT && !rune.element().isEmpty()) {
				elements.add(rune.element());
			}
		}
		return elements;
	}

	/** The elements of every effect in a compiled spell, links and echoes included. */
	public static Set<String> elements(SpellPlan.Segment root) {
		Set<String> elements = new LinkedHashSet<>();
		collect(root, elements, 0);
		return elements;
	}

	private static void collect(SpellPlan.Segment seg, Set<String> into, int depth) {
		if (seg == null || depth > 32) {
			return;
		}
		for (SpellPlan.Group g : seg.groups) {
			for (SpellPlan.EffectNode e : g.effects) {
				if (!e.effect.element().isEmpty()) {
					into.add(e.effect.element());
				}
			}
		}
		if (seg.link != null) {
			collect(seg.link.next, into, depth + 1);
			collect(seg.link.echoPrefix, into, depth + 1);
		}
	}
}
