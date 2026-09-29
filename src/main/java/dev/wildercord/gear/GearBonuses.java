package dev.wildercord.gear;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.SpellPlan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What someone's casting gear does to their spells. Pure, so it's unit-tested and the readout, the
 * HUD and the server agree.
 *
 * <p>Gear counts from its slot in the inventory ({@link GearSlot}) or, while that slot is empty,
 * from the hands: a piece in its slot <em>takes the place of</em> held pieces of the same kind (a
 * staff in the staff slot and staffs in the hands: only the slotted one counts), and a kind whose
 * slot is empty works held exactly as before (staffs in either hand, foci and the tome from the
 * off-hand). The slots are separate, so a tome and a focus, or a slotted staff and a held focus, all
 * apply together.</p>
 *
 * <p>The rules: the same piece twice counts once. A staff's power goes to effects of its element
 * only; its discount goes to a whole spell with at least one effect of its element, and two staffs
 * never discount the same spell twice (the better one counts). Foci and the tome apply to every
 * spell.</p>
 */
public record GearBonuses(List<GearDef> pieces) {
	public static final GearBonuses NONE = new GearBonuses(List.of());

	public GearBonuses {
		pieces = List.copyOf(new LinkedHashSet<>(pieces));
	}

	/** The gear in these hands (either may be null), with nothing in the slots: each piece counts only from a hand it works in. */
	public static GearBonuses of(GearDef mainHand, GearDef offHand) {
		return of(Map.of(), mainHand, offHand);
	}

	/**
	 * The gear someone has: what's in its slots, plus the pieces in the hands whose slot is empty (each
	 * from a hand it works in). {@code slotted} may hold a piece only under a slot that accepts it; anything
	 * else is ignored.
	 */
	public static GearBonuses of(Map<GearSlot, GearDef> slotted, GearDef mainHand, GearDef offHand) {
		List<GearDef> pieces = new ArrayList<>(4);
		for (GearSlot slot : GearSlot.all()) {
			GearDef piece = slotted.get(slot);
			if (piece != null && slot.accepts(piece)) {
				pieces.add(piece);
			}
		}
		if (mainHand != null && mainHand.worksIn(true) && !filled(slotted, mainHand)) {
			pieces.add(mainHand);
		}
		if (offHand != null && offHand.worksIn(false) && !filled(slotted, offHand)) {
			pieces.add(offHand);
		}
		return pieces.isEmpty() ? NONE : new GearBonuses(pieces);
	}

	/** Whether the slot this held piece would go in has a piece of its own, which then takes its place. */
	private static boolean filled(Map<GearSlot, GearDef> slotted, GearDef held) {
		GearSlot slot = GearSlot.of(held);
		if (slot == null) {
			return false;
		}
		GearDef piece = slotted.get(slot);
		return piece != null && slot.accepts(piece);
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

	/** Whether the Tome of the Fifth Page counts (in its slot, or held in the off-hand with the slot empty). */
	public boolean fifthSpell() {
		for (GearDef piece : pieces) {
			if (piece.fifthSpell()) {
				return true;
			}
		}
		return false;
	}

	/** The staffs that count and favour one of these elements (for the charged-cast flourish and the readout). */
	public List<GearDef> staffsFor(Collection<String> elements) {
		List<GearDef> staffs = new ArrayList<>();
		for (GearDef piece : pieces) {
			if (piece.kind().staff() && elements.contains(piece.element())) {
				staffs.add(piece);
			}
		}
		return staffs;
	}

	/** Whether any gear that counts changes a spell with these elements (so the readout mentions it). */
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
