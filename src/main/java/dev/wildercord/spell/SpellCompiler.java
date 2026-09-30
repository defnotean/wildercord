package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Reads a row of runes into a {@link SpellPlan}, prices it, and explains it in plain
 * English. The rules:
 * <ul>
 *   <li>A shape starts a group; effects before any shape use the segment's implicit shape
 *       (Self at the start, the trigger after a link).</li>
 *   <li>A modifier attaches to the closest rune on its left that has the trait it needs,
 *       never crossing back past a link (the link rune itself is still reachable).</li>
 *   <li>A link ends the segment; everything after it fires later.</li>
 * </ul>
 */
public final class SpellCompiler {
	private SpellCompiler() {}

	/** Marks a non-modifier rune in {@link Compiled#attachedTo()}. */
	public static final int NOT_A_MODIFIER = -2;
	/** Marks a modifier that found nothing to attach to. */
	public static final int UNATTACHED = -1;

	/**
	 * @param attachedTo for each input rune: the index it attached to (modifiers),
	 *                   {@link #UNATTACHED}, or {@link #NOT_A_MODIFIER}
	 * @param healthCost Blood Price: health paid instead of mana, or 0 when the spell costs mana
	 */
	public record Compiled(SpellPlan.Segment root, double cost, int cooldownTicks, List<String> lines, List<String> warnings, int[] attachedTo,
			int healthCost) {
		public boolean isEmpty() {
			return root.isEmpty();
		}

		public boolean paysInHealth() {
			return healthCost > 0;
		}

		/** Mana as the player sees it: rounded up. */
		public int manaCost() {
			return (int) Math.ceil(cost - 1e-9);
		}
	}

	public static Compiled compile(List<RuneDef> runes) {
		return compile(runes, Runes.SELF, Ranks.Lookup.NONE);
	}

	/** Reads a spell for one caster: the readout names each effect at the rank they know it (e.g. "Fire II"). */
	public static Compiled compile(List<RuneDef> runes, Ranks.Lookup ranks) {
		return compile(runes, Runes.SELF, ranks);
	}

	/**
	 * Reads what an Imbue stored: the runes after it, read as they were after the link, so an effect
	 * with no shape of its own lands on whatever set it off.
	 */
	public static Compiled compileStored(List<RuneDef> runes) {
		return compile(runes, Runes.TRIGGER, Ranks.Lookup.NONE);
	}

	/** The runes an Imbue in {@code spell} would store: everything after the first Imbue (empty if there's none). */
	public static List<RuneDef> stored(List<RuneDef> spell) {
		for (int i = 0; i < spell.size(); i++) {
			if (spell.get(i).is(Runes.IMBUE.id())) {
				return List.copyOf(spell.subList(i + 1, spell.size()));
			}
		}
		return List.of();
	}

	private static Compiled compile(List<RuneDef> runes, RuneDef implicitShape, Ranks.Lookup ranks) {
		Reader reader = new Reader(expand(runes), runes.size(), true, implicitShape, ranks);
		SpellPlan.Segment root = reader.segment(0, implicitShape, List.of(), false);
		double cost = cost(root);
		List<String> lines = new ArrayList<>();
		describe(root, "", "", lines);
		for (RuneDef rune : runes) {
			if (Knots.isKnot(rune)) {
				lines.add(rune.name() + " is a Knot: " + Knots.flatten(List.of(rune)).size() + " runes in one socket, "
					+ Math.round((1 - Knots.DISCOUNT) * 100) + "% less mana.");
			}
		}
		checkEmptyGroups(root, reader.warnings);
		List<RuneDef> whole = wholeSpellMods(root);
		int rapid = SpellPlan.count(whole, Runes.RAPID_MOD);
		int vows = SpellPlan.count(whole, Runes.VOW_MOD);
		int healthCost = SpellPlan.count(whole, Runes.BLOOD_PRICE_MOD) > 0 ? SpellNumbers.healthCost(cost) : 0;
		if (healthCost > 0) {
			lines.add("Costs " + healthCost + " health instead of mana.");
		}
		return new Compiled(root, cost, SpellNumbers.cooldownTicks(cost, rapid, vows), List.copyOf(lines), List.copyOf(reader.warnings), reader.attachedTo,
			healthCost);
	}

	// ------------------------------------------------------------------ reading

	/**
	 * One rune as the reader sees it, once Knots are untied into their runes.
	 *
	 * @param outer  its socket in the spell as written (a Knot's runes all share the Knot's)
	 * @param scope  0 for a rune written in the spell; each Knot's runes share a number of their own
	 * @param factor the share of its mana it costs: {@link Knots#DISCOUNT} for each Knot around it
	 */
	private record Entry(RuneDef rune, int outer, int scope, double factor, String rankId) {}

	/** The spell with every Knot replaced by the runes it holds (Knots in Knots too, up to {@link Knots#MAX_DEPTH}). */
	private static List<Entry> expand(List<RuneDef> runes) {
		List<Entry> out = new ArrayList<>();
		int[] scopes = {0};
		for (int i = 0; i < runes.size(); i++) {
			expand(runes.get(i), i, 0, 1.0, 0, null, out, scopes);
		}
		return out;
	}

	private static void expand(RuneDef rune, int outer, int scope, double factor, int depth, String rankId, List<Entry> out, int[] scopes) {
		if (WovenRunes.isWoven(rune)) {
			for (RuneDef held : WovenRunes.contents(rune)) {
				out.add(new Entry(held, outer, scope, factor, rune.id()));
			}
			return;
		}
		if (!Knots.isKnot(rune)) {
			out.add(new Entry(rune, outer, scope, factor, rankId));
			return;
		}
		if (depth >= Knots.MAX_DEPTH) {
			return;
		}
		int inner = ++scopes[0];
		for (RuneDef held : Knots.contents(rune)) {
			expand(held, outer, inner, factor * Knots.DISCOUNT, depth + 1, rankId, out, scopes);
		}
	}

	private record Target(int index, Entry entry, List<RuneDef> mods) {}

	private static final class Reader {
		final List<Entry> runes;
		final boolean record;
		final int[] attachedTo;
		final Ranks.Lookup ranks;
		final List<String> warnings = new ArrayList<>();
		int echoes;
		/**
		 * Where the spell an Echo repeats starts, and what an effect with no shape lands on there: the
		 * whole spell from you, or (inside an Imbue) only what was stored, at whatever set it off.
		 */
		int base;
		RuneDef baseShape;

		Reader(List<Entry> runes, int written, boolean record, RuneDef rootShape, Ranks.Lookup ranks) {
			this.runes = runes;
			this.record = record;
			this.attachedTo = new int[written];
			this.ranks = ranks;
			Arrays.fill(attachedTo, NOT_A_MODIFIER);
			this.baseShape = rootShape;
		}

		/** Whether this is part of a stored (imbued) spell, released later from an item or a glyph. */
		boolean stored() {
			return baseShape.is(Runes.TRIGGER.id());
		}

		/**
		 * @param afterHits read after an On Hit or On Kill (with no Pulse since): this part fires at
		 *                  every creature hit or killed, but is paid for once
		 */
		SpellPlan.Segment segment(int from, RuneDef implicitShape, List<Target> inherited, boolean afterHits) {
			SpellPlan.Segment seg = new SpellPlan.Segment(implicitShape);
			List<Target> targets = new ArrayList<>(inherited);
			SpellPlan.Group group = null;
			for (int i = from; i < runes.size(); i++) {
				Entry entry = runes.get(i);
				RuneDef rune = entry.rune();
				switch (rune.family()) {
					case SHAPE -> {
						group = new SpellPlan.Group(rune, false);
						group.factor = entry.factor();
						seg.groups.add(group);
						targets.add(new Target(i, entry, group.shapeMods));
					}
					case EFFECT -> {
						if (group == null) {
							group = new SpellPlan.Group(implicitShape, true);
							seg.groups.add(group);
						}
						SpellPlan.EffectNode node = new SpellPlan.EffectNode(rune);
						node.factor = entry.factor();
						node.rank = Ranks.clamp(ranks.rank(entry.rankId() == null ? rune.id() : entry.rankId()));
						group.effects.add(node);
						targets.add(new Target(i, entry, node.mods));
					}
					case MODIFIER -> {
						// A Knot is sealed: a modifier inside one only changes the Knot's own runes, and one
						// outside never reaches in.
						Target target = null;
						for (int t = targets.size() - 1; t >= 0; t--) {
							Target candidate = targets.get(t);
							if (candidate.entry().scope() == entry.scope() && candidate.entry().rune().has(rune.needs())) {
								target = candidate;
								break;
							}
						}
						if (target == null) {
							warn(rune.name() + " does nothing here: nothing on its left that it can change.");
							if (entry.scope() == 0) {
								mark(entry.outer(), UNATTACHED);
							}
						} else {
							target.mods().add(rune);
							if (entry.scope() == 0) {
								mark(entry.outer(), target.entry().outer());
							}
						}
					}
					case KNOT -> {
						// Only a Knot too deep to untie is still a Knot here: it does nothing.
						warn(rune.name() + " holds Knots too deep to untie (" + Knots.MAX_DEPTH + " at most).");
					}
					case LINK -> {
						boolean watchesGroup = firesAtHits(rune) || rune.is(Runes.IMBUE.id());
						if (watchesGroup && group == null) {
							warn(rune.name() + " needs a shape before it to watch.");
						}
						SpellPlan.Link link = new SpellPlan.Link(rune, watchesGroup ? group : null);
						link.factor = entry.factor();
						// A repeat is paid for once: after On Hit or On Kill it goes off for the first hit or kill only.
						link.firstOnly = afterHits && (rune.is(Runes.ECHO.id()) || rune.is(Runes.PULSE.id()));
						if (rune.is(Runes.ECHO.id())) {
							echoes++;
							if (echoes > SpellNumbers.MAX_ECHOES) {
								warn("Only " + SpellNumbers.MAX_ECHOES + " Echoes count; the rest are ignored.");
							} else {
								link.echoPrefix = new Reader(runes.subList(base, i), 0, false, baseShape, ranks).segment(0, baseShape, List.of(), false);
							}
						}
						RuneDef nextShape;
						if (rune.is(Runes.DELAY.id()) || rune.is(Runes.PULSE.id())) {
							nextShape = Runes.SELF;
						} else if (rune.is(Runes.ECHO.id()) || rune.is(Runes.IF_SNEAKING.id()) || rune.is(Runes.IF_AIRBORNE.id())
								|| rune.is(Runes.COMBO.id()) || rune.is(Runes.IF_WOUNDED.id()) || rune.is(Runes.IF_OUTNUMBERED.id()) || rune.is(Runes.IF_WET.id())) {
							nextShape = implicitShape;
						} else {
							nextShape = Runes.TRIGGER;
						}
						if (rune.is(Runes.COMBO.id()) && stored()) {
							warn("Combo never fires in an imbued spell: every release counts as a first cast.");
						}
						boolean imbue = rune.is(Runes.IMBUE.id());
						int outerBase = base;
						RuneDef outerShape = baseShape;
						if (imbue) {
							// What follows is stored and released on its own: an Echo in it repeats only that.
							base = i + 1;
							baseShape = Runes.TRIGGER;
						}
						// On Hit and On Kill fire what follows at every creature; each of a Pulse's runs (and a stored
						// spell's release) is paid for on its own.
						boolean nextAfterHits = firesAtHits(rune) || afterHits && !rune.is(Runes.PULSE.id()) && !imbue;
						link.next = segment(i + 1, nextShape, List.of(new Target(i, entry, link.mods)), nextAfterHits);
						base = outerBase;
						baseShape = outerShape;
						if (link.next.isEmpty() && !rune.is(Runes.ECHO.id())) {
							warn(rune.name() + " has nothing after it.");
						}
						if (rune.is(Runes.IMBUE.id()) && runes.subList(i + 1, runes.size()).stream().anyMatch(r -> r.rune().is(Runes.IMBUE.id()))) {
							warn("An Imbue can't store another Imbue.");
						}
						seg.link = link;
						return seg;
					}
				}
			}
			return seg;
		}

		private void warn(String message) {
			if (record && !warnings.contains(message)) {
				warnings.add(message);
			}
		}

		private void mark(int index, int value) {
			if (record) {
				attachedTo[index] = value;
			}
		}
	}

	/**
	 * Whether {@code link} watches the group before it and fires the rest at the creatures that group
	 * hit: On Hit, On Kill, and (new runes, batch 2) On Reaction and On Weakness, which fire at those a
	 * reaction went off on or a weakness was struck on. What follows is paid for once however many it
	 * fires at, so a repeat after one goes off for the first only.
	 */
	public static boolean firesAtHits(RuneDef link) {
		return link.is(Runes.ON_HIT.id()) || link.is(Runes.ON_KILL.id()) || link.is(Runes.ON_REACTION.id()) || link.is(Runes.ON_WEAKNESS.id());
	}

	/**
	 * Whether {@code modifier} changes the whole spell rather than the shape it sits on: one that needs
	 * {@link Trait#COOLDOWN} (Rapid, Vow, Blood Price). It's priced on the whole spell, so where it
	 * sits makes no difference to the price.
	 */
	public static boolean wholeSpell(RuneDef modifier) {
		return Trait.COOLDOWN.equals(modifier.needs());
	}

	/** Every modifier in the spell that changes the whole spell (see {@link #wholeSpell}), links included; an Echo's repeat counts once. */
	private static List<RuneDef> wholeSpellMods(SpellPlan.Segment root) {
		List<RuneDef> out = new ArrayList<>();
		for (SpellPlan.Segment seg = root; seg != null; seg = seg.link == null ? null : seg.link.next) {
			for (SpellPlan.Group g : seg.groups) {
				for (RuneDef mod : g.shapeMods) {
					if (wholeSpell(mod)) {
						out.add(mod);
					}
				}
			}
		}
		return out;
	}

	private static void checkEmptyGroups(SpellPlan.Segment seg, List<String> warnings) {
		if (seg == null) {
			return;
		}
		for (SpellPlan.Group g : seg.groups) {
			boolean anchorsLink = seg.link != null && seg.link.anchor == g;
			if (g.effects.isEmpty() && !anchorsLink) {
				String message = g.shape.name() + " has no effect after it.";
				if (!warnings.contains(message)) {
					warnings.add(message);
				}
			}
			if (g.effects.isEmpty() && g.count(Runes.VOW_MOD) > 0) {
				String message = "Vow strengthens only " + g.shape.name() + "'s effects, and it has none: the cooldown is 5x longer for nothing.";
				if (!warnings.contains(message)) {
					warnings.add(message);
				}
			}
		}
		if (seg.link != null) {
			checkEmptyGroups(seg.link.next, warnings);
		}
	}

	// ------------------------------------------------------------------ cost

	/**
	 * A whole spell's mana, before anyone's discounts: every part of it, then each modifier that
	 * changes the whole spell (Rapid) on the whole of it, wherever it sits. Rapid on an empty Self
	 * costs what Rapid on the spell's biggest group does.
	 */
	public static double cost(SpellPlan.Segment root) {
		return partCost(root) * product(wholeSpellMods(root));
	}

	/** What a segment of a plan costs by itself (the mana of what a link fires), before the whole-spell modifiers. */
	public static double segmentCost(SpellPlan.Segment seg) {
		return partCost(seg);
	}

	private static double partCost(SpellPlan.Segment seg) {
		if (seg == null) {
			return 0;
		}
		double total = 0;
		for (SpellPlan.Group g : seg.groups) {
			total += groupCost(g);
		}
		if (seg.link != null) {
			SpellPlan.Link link = seg.link;
			double rest = partCost(link.next) * (link.link.is(Runes.PULSE.id()) ? SpellNumbers.PULSES : link.link.is(Runes.IMBUE.id()) ? SpellNumbers.IMBUE_CHARGES : 1);
			total += link.link.cost() * product(link.mods) * link.factor + rest + partCost(link.echoPrefix);
		}
		return total;
	}

	private static double groupCost(SpellPlan.Group g) {
		double effects = 0;
		for (SpellPlan.EffectNode e : g.effects) {
			effects += e.effect.cost() * product(e.mods) * e.factor;
		}
		double mods = 1;
		for (RuneDef mod : g.shapeMods) {
			if (!wholeSpell(mod)) {
				mods *= mod.multiplier();
			}
		}
		return (g.shape.cost() * g.factor + effects * g.shape.multiplier()) * mods;
	}

	private static double product(List<RuneDef> mods) {
		double p = 1;
		for (RuneDef mod : mods) {
			p *= mod.multiplier();
		}
		return p;
	}

	/**
	 * How a spell's mana divides between the elements of its effects, each as a share of what all its
	 * effects cost as {@link #cost} prices them (a Pulse's runs and an Imbue's charges counted, a shape's
	 * multiplier and its modifiers on its own effects). The shape and links carry whatever they deliver,
	 * so a spell of fire alone is all fire; an effect with no element is nobody's share. Empty for a spell
	 * with no effects. What a player's affinity makes cheaper, and what casting it counts toward.
	 */
	public static java.util.Map<String, Double> elementShares(SpellPlan.Segment root) {
		java.util.Map<String, Double> costs = new java.util.LinkedHashMap<>();
		double total = effectCosts(root, 1.0, costs, 0);
		java.util.Map<String, Double> shares = new java.util.LinkedHashMap<>();
		if (total <= 0) {
			return shares;
		}
		for (java.util.Map.Entry<String, Double> entry : costs.entrySet()) {
			if (!entry.getKey().isEmpty()) {
				shares.put(entry.getKey(), entry.getValue() / total);
			}
		}
		return shares;
	}

	/**
	 * The share of a spell's effect mana that one of its effects costs, priced as {@link #elementShares} prices
	 * them (the shape's multiplier and modifiers on it, a Pulse's runs and an Imbue's charges counted): what a
	 * spell gives back when that effect finds nothing it can do (Soar on someone already soaring). The effect is
	 * {@code node} itself if it's in {@code root}, or else the first effect of the same rune with the same
	 * modifiers (a chorus recasts a spell as a plan of its own). 0 when there's no such effect.
	 */
	public static double effectShare(SpellPlan.Segment root, SpellPlan.EffectNode node) {
		double total = effectCosts(root, 1.0, new java.util.HashMap<>(), 0);
		if (total <= 0) {
			return 0;
		}
		double exact = nodeCost(root, 1.0, node, true, 0);
		return (exact > 0 ? exact : nodeCost(root, 1.0, node, false, 0)) / total;
	}

	/** What {@code node} (or, not {@code exact}, the first effect like it) costs in {@code seg}, times {@code scale}; 0 if absent. */
	private static double nodeCost(SpellPlan.Segment seg, double scale, SpellPlan.EffectNode node, boolean exact, int depth) {
		if (seg == null || depth > 32) {
			return 0;
		}
		for (SpellPlan.Group g : seg.groups) {
			double mods = 1;
			for (RuneDef mod : g.shapeMods) {
				if (!wholeSpell(mod)) {
					mods *= mod.multiplier();
				}
			}
			for (SpellPlan.EffectNode e : g.effects) {
				if (exact ? e == node : e.effect.equals(node.effect) && e.mods.equals(node.mods)) {
					return e.effect.cost() * product(e.mods) * e.factor * g.shape.multiplier() * mods * scale;
				}
			}
		}
		if (seg.link != null) {
			SpellPlan.Link link = seg.link;
			double runs = link.link.is(Runes.PULSE.id()) ? SpellNumbers.PULSES : link.link.is(Runes.IMBUE.id()) ? SpellNumbers.IMBUE_CHARGES : 1;
			double found = nodeCost(link.next, scale * runs, node, exact, depth + 1);
			return found > 0 ? found : nodeCost(link.echoPrefix, scale, node, exact, depth + 1);
		}
		return 0;
	}

	/** Adds each element's effect costs in {@code seg} (times {@code scale}) into {@code into}; returns all of them together. */
	private static double effectCosts(SpellPlan.Segment seg, double scale, java.util.Map<String, Double> into, int depth) {
		if (seg == null || depth > 32) {
			return 0;
		}
		double total = 0;
		for (SpellPlan.Group g : seg.groups) {
			double mods = 1;
			for (RuneDef mod : g.shapeMods) {
				if (!wholeSpell(mod)) {
					mods *= mod.multiplier();
				}
			}
			for (SpellPlan.EffectNode e : g.effects) {
				double cost = e.effect.cost() * product(e.mods) * e.factor * g.shape.multiplier() * mods * scale;
				into.merge(e.effect.element(), cost, Double::sum);
				total += cost;
			}
		}
		if (seg.link != null) {
			SpellPlan.Link link = seg.link;
			double runs = link.link.is(Runes.PULSE.id()) ? SpellNumbers.PULSES : link.link.is(Runes.IMBUE.id()) ? SpellNumbers.IMBUE_CHARGES : 1;
			total += effectCosts(link.next, scale * runs, into, depth + 1);
			total += effectCosts(link.echoPrefix, scale, into, depth + 1);
		}
		return total;
	}

	// ------------------------------------------------------------------ readout

	/** @param after "hit" or "kill" under an On Hit or On Kill (the nearest), otherwise "" */
	private static void describe(SpellPlan.Segment seg, String indent, String after, List<String> lines) {
		if (seg == null) {
			return;
		}
		for (SpellPlan.Group g : seg.groups) {
			if (g.effects.isEmpty() && seg.link != null && seg.link.anchor == g && seg.link.link.is(Runes.IMBUE.id())) {
				// Only there to say where the Imbue goes: the header says it.
				continue;
			}
			String vow = g.count(Runes.VOW_MOD) > 0 ? " (vowed: x" + trim(Math.pow(2.0, g.count(Runes.VOW_MOD))) + " power)" : "";
			lines.add(indent + shapePhrase(g) + vow + ": " + effectsPhrase(g));
		}
		if (seg.link == null) {
			return;
		}
		SpellPlan.Link link = seg.link;
		String id = link.link.id();
		// A repeat after On Hit or On Kill goes off for the first hit or kill only (see SpellPlan.Link#firstOnly).
		String firstOnly = link.firstOnly && !after.isEmpty() ? " (first " + after + " only)" : "";
		if (id.equals(Runes.ECHO.id())) {
			if (link.echoPrefix != null) {
				lines.add(indent + "0.5s later, everything before this fires again" + firstOnly + ".");
			}
			describe(link.next, indent, after, lines);
			return;
		}
		String header;
		if (id.equals(Runes.ON_HIT.id())) {
			header = "On hit:";
		} else if (id.equals(Runes.ON_KILL.id())) {
			header = "On kill:";
		} else if (id.equals(Runes.ON_LAND.id())) {
			header = "When you land:";
		} else if (id.equals(Runes.DELAY.id())) {
			header = "After " + seconds(SpellNumbers.delayTicks(link)) + ":";
		} else if (id.equals(Runes.PULSE.id())) {
			header = SpellNumbers.PULSES + " times, every " + seconds(SpellNumbers.pulseInterval(link)) + firstOnly + ":";
		} else if (id.equals(Runes.ON_HURT.id())) {
			header = "When something hurts you:";
		} else if (id.equals(Runes.IF_SNEAKING.id())) {
			header = "If you're sneaking:";
		} else if (id.equals(Runes.ON_LOW_HEALTH.id())) {
			header = "When your health drops below 30%:";
		} else if (id.equals(Runes.IF_AIRBORNE.id())) {
			header = "If you're in the air:";
		} else if (id.equals(Runes.COMBO.id())) {
			header = "Every 3rd cast:";
		} else if (id.equals(Runes.IF_WOUNDED.id())) {
			header = "If you're below half health:";
		} else if (id.equals(Runes.IF_OUTNUMBERED.id())) {
			header = "If 3 or more enemies are within 8 blocks:";
		} else if (id.equals(Runes.IF_WET.id())) {
			header = "If you're in water or rain:";
		} else if (id.equals(Runes.IMBUE.id())) {
			boolean self = link.anchor != null && link.anchor.shape.is(Runes.SELF.id());
			header = "Stored in " + (self ? "the item in your hand" : "the block it touches") + " (" + SpellNumbers.IMBUE_CHARGES + " charges), then:";
		} else if (id.equals(Runes.ON_REACTION.id())) {
			header = "On a reaction:";
		} else if (id.equals(Runes.ON_WEAKNESS.id())) {
			header = "On a weakness struck:";
		} else {
			header = link.link.name() + ":";
		}
		lines.add(indent + header);
		String next = id.equals(Runes.ON_HIT.id()) || id.equals(Runes.ON_WEAKNESS.id()) ? "hit" : id.equals(Runes.ON_KILL.id()) ? "kill"
			: id.equals(Runes.ON_REACTION.id()) ? "reaction" : after;
		describe(link.next, indent + "  ", next, lines);
	}

	private static String shapePhrase(SpellPlan.Group g) {
		int copies = SpellNumbers.copies(g);
		String id = g.shape.id();
		if (id.equals(Runes.SELF.id())) {
			return "You";
		}
		if (id.equals(Runes.TRIGGER.id())) {
			return "The target";
		}
		if (id.equals(Runes.TOUCH.id())) {
			return SpellNumbers.chainJumps(g) > 0 ? "What you touch (chains " + SpellNumbers.chainJumps(g) + ")" : "What you touch";
		}
		if (id.equals(Runes.BOLT.id()) || id.equals(Runes.BEAM.id())) {
			StringJoiner adj = new StringJoiner(", ", " (", ")").setEmptyValue("");
			if (SpellNumbers.homing(g)) adj.add("homing");
			if (SpellNumbers.pierce(g) > 0) adj.add("pierces " + SpellNumbers.pierce(g));
			if (SpellNumbers.bounces(g) > 0) adj.add("bounces " + SpellNumbers.bounces(g));
			if (SpellNumbers.chainJumps(g) > 0) adj.add("chains " + SpellNumbers.chainJumps(g));
			if (g.count(Runes.QUICKEN) > 0) adj.add("fast");
			String noun = id.equals(Runes.BOLT.id()) ? "bolt" : "beam";
			int shots = SpellNumbers.volleyShots(g);
			return (copies > 1 ? copies + " " + noun + "s" : "A " + noun) + (shots > 1 ? " x" + shots : "") + adj;
		}
		if (id.equals(Runes.BURST.id())) {
			return (copies > 1 ? copies + " bursts" : "Everything") + " within " + blocks(SpellNumbers.burstRadius(g));
		}
		if (id.equals(Runes.ZONE.id())) {
			return (copies > 1 ? copies + " fields" : "A field") + " (" + blocks(SpellNumbers.zoneRadius(g)) + ", " + trim(SpellNumbers.zoneLife(g)) + "s, every " + seconds(SpellNumbers.zoneInterval(g)) + ")";
		}
		if (id.equals(Runes.RAIN.id())) {
			return (5 * copies) + " strikes from the sky (" + blocks(SpellNumbers.rainRadius(g)) + ")";
		}
		int volley = SpellNumbers.volleyShots(g);
		String times = volley > 1 ? " x" + volley : "";
		if (id.equals(Runes.ARC.id())) {
			return (copies > 1 ? copies + " lobbed bolts" : "A lobbed bolt") + times + (SpellNumbers.bounces(g) > 0 ? " (bounces " + SpellNumbers.bounces(g) + ")" : "");
		}
		if (id.equals(Runes.CONE.id())) {
			return "A " + blocks(SpellNumbers.coneLength(g)).replace(" blocks", "-block") + " cone";
		}
		if (id.equals(Runes.TRAIL.id())) {
			return "Your trail (" + SpellNumbers.trailSeconds(g) + "s)";
		}
		if (id.equals(Runes.WALL.id())) {
			return "A " + blocks(SpellNumbers.wallWidth(g)).replace(" blocks", "-block") + " wall (" + seconds(SpellNumbers.wallTicks(g)) + ", every " + seconds(SpellNumbers.wallInterval(g)) + ")";
		}
		if (id.equals(Runes.ORBIT.id())) {
			return SpellNumbers.orbs(g) + " orbiting orbs (" + SpellNumbers.orbitSeconds(g) + "s)";
		}
		if (id.equals(Runes.RING.id())) {
			return "A ring out to " + blocks(SpellNumbers.ringRadius(g));
		}
		if (id.equals(Runes.PILLAR.id())) {
			return (copies > 1 ? copies + " pillars" : "A pillar") + " (" + blocks(SpellNumbers.pillarRadius(g)) + ")";
		}
		if (id.equals(Runes.WAVE.id())) {
			return "A " + blocks(SpellNumbers.waveWidth(g)).replace(" blocks", "-block") + " wave";
		}
		if (id.equals(Runes.MINE.id())) {
			return (copies > 1 ? copies + " hidden mines" : "A hidden mine") + " (" + blocks(SpellNumbers.mineRadius(g)) + ")";
		}
		if (id.equals(Runes.TOTEM.id())) {
			return "A totem (" + blocks(SpellNumbers.totemRadius(g)) + ", " + seconds(SpellNumbers.totemTicks(g)) + ", every " + seconds(SpellNumbers.totemInterval(g)) + ")";
		}
		if (id.equals(Runes.DOMAIN.id())) {
			return "Your domain (" + blocks(SpellNumbers.domainRadius(g)) + ", " + seconds(SpellNumbers.domainTicks(g)) + ", every " + seconds(SpellNumbers.domainInterval(g)) + ")";
		}
		if (id.equals(Runes.CRESCENT.id())) {
			return (copies > 1 ? copies + " crescents" : "A crescent") + times + " (" + blocks(SpellNumbers.crescentWidth(g)).replace(" blocks", "-block") + " wide)";
		}
		if (id.equals(Runes.BARRAGE.id())) {
			return "A barrage of " + SpellNumbers.barrageBlows(g) + " blows (35% power each)";
		}
		if (id.equals(Runes.ORB.id())) {
			return (copies > 1 ? copies + " orbs" : "A drifting orb") + " (" + blocks(SpellNumbers.orbRadius(g)) + ")";
		}
		if (id.equals(Runes.BLITZ.id())) {
			return "Everything you flash through";
		}
		if (id.equals(Runes.SPARK.id())) {
			return (copies > 1 ? copies + " sparks" : "A spark") + times + " (75% power)";
		}
		if (id.equals(Runes.RAY.id())) {
			StringJoiner adj = new StringJoiner(", ", ", ", "").setEmptyValue("");
			if (SpellNumbers.pierce(g) > 0) adj.add("pierces " + SpellNumbers.pierce(g));
			if (SpellNumbers.chainJumps(g) > 0) adj.add("chains " + SpellNumbers.chainJumps(g));
			return "A ray (" + blocks(SpellNumbers.RAY_RANGE) + adj + ")";
		}
		if (id.equals(Runes.NOVA.id())) {
			return "A nova (" + blocks(SpellNumbers.novaRadius(g)) + ")";
		}
		if (id.equals(Runes.WISP.id())) {
			return copies > 1 ? copies + " seeking wisps" : "A seeking wisp";
		}
		if (id.equals(Runes.COMET.id())) {
			return (copies > 1 ? copies + " comets" : "A comet") + times + " (bursts " + blocks(SpellNumbers.cometRadius(g)) + ")";
		}
		if (id.equals(Runes.RICOCHET.id())) {
			return (copies > 1 ? copies + " ricocheting orbs" : "A ricocheting orb") + " (" + SpellNumbers.ricochetBounces(g) + " bounces)";
		}
		if (id.equals(Runes.CLUSTER.id())) {
			return (copies > 1 ? copies + " clusters" : "A cluster") + " (" + SpellNumbers.CLUSTER_SHARDS + " shards, " + blocks(SpellNumbers.clusterRadius(g)) + ")";
		}
		if (id.equals(Runes.LANCE.id())) {
			return (copies > 1 ? copies + " lances" : "A lance") + " through everything in " + blocks(SpellNumbers.LANCE_RANGE);
		}
		if (id.equals(Runes.SWEEP.id())) {
			return "A " + blocks(SpellNumbers.sweepLength(g)).replace(" blocks", "-block") + " sweeping beam";
		}
		if (id.equals(Runes.PRISM.id())) {
			return (copies > 1 ? copies + " prism beams" : "A prism beam") + " (splits in 3)";
		}
		if (id.equals(Runes.STREAM.id())) {
			return "A stream of " + SpellNumbers.streamStrikes(g) + " strikes (35% power each)";
		}
		if (id.equals(Runes.VORTEX.id())) {
			return "A vortex's eye (" + blocks(SpellNumbers.vortexEye(g)) + ", drags in from " + blocks(SpellNumbers.vortexRadius(g)) + ", "
				+ SpellNumbers.vortexSeconds(g) + "s)";
		}
		if (id.equals(Runes.SNARE.id())) {
			return "A tripwire (up to " + blocks(SpellNumbers.SNARE_LENGTH) + ", springs " + blocks(SpellNumbers.snareRadius(g)) + ")";
		}
		if (id.equals(Runes.CONSTELLATION.id())) {
			return "Up to " + SpellNumbers.CONSTELLATION_STARS + " enemies within " + blocks(SpellNumbers.constellationRange(g));
		}
		// New runes (batch 2).
		if (id.equals(Runes.GLAIVE.id())) {
			return (copies > 1 ? copies + " glaives" : "A glaive") + " (out " + blocks(SpellNumbers.GLAIVE_RANGE) + " and back)";
		}
		if (id.equals(Runes.IMPRINT.id())) {
			return (copies > 1 ? copies + " imprints" : "An imprint") + " (" + blocks(SpellNumbers.imprintRadius(g)) + ", erupts after "
				+ seconds(SpellNumbers.imprintDelay(g)) + ")";
		}
		if (id.equals(Runes.LATCH.id())) {
			return "A latch (" + SpellNumbers.latchStrikes(g) + " strikes, every " + seconds(SpellNumbers.latchInterval(g)) + ", "
				+ Math.round(SpellNumbers.LATCH_STRENGTH * 100) + "% power each)";
		}
		return g.shape.name();
	}

	private static String effectsPhrase(SpellPlan.Group g) {
		if (g.effects.isEmpty()) {
			return "nothing yet";
		}
		StringJoiner joiner = new StringJoiner(", ");
		for (SpellPlan.EffectNode e : g.effects) {
			StringJoiner mods = new StringJoiner(", ", " (", ")").setEmptyValue("");
			int amp = e.count(Runes.AMPLIFY);
			int ext = e.count(Runes.EXTEND);
			int wid = e.count(Runes.WIDEN);
			int late = SpellNumbers.belatedTicks(e);
			if (amp > 0 || e.rank > 1 || late > 0) {
				long percent = Math.round((SpellNumbers.power(e) * Ranks.power(e.rank) - 1) * 100);
				mods.add((percent >= 0 ? "+" : "") + percent + "% power");
			}
			if (late > 0) mods.add("lands " + seconds(late) + " late");
			if (ext > 0) mods.add(trim(SpellNumbers.duration(e)) + "x duration");
			if (wid > 0) mods.add("+" + Math.round((SpellNumbers.effectRadius(e) - 1) * 100) + "% radius");
			if (e.count(Runes.FRUGAL_MOD) > 0) mods.add("frugal");
			if (e.count(Runes.OVERCHARGE_MOD) > 0) mods.add("overcharged");
			if (e.count(Runes.FOCUS_MOD) > 0) mods.add("focused");
			if (e.count(Runes.EXECUTE_MOD) > 0) mods.add("x" + trim(SpellNumbers.executeBonus(e)) + " under half health");
			if (e.count(Runes.TRIAL_KEY) > 0) mods.add("x" + trim(SpellNumbers.trialKeyBonus(e)) + " at full health");
			if (e.count(Runes.KINDLED) > 0) mods.add("sets alight " + SpellNumbers.kindledSeconds(e) + "s");
			if (e.count(Runes.UNSTABLE) > 0) mods.add("unstable: 50-200% power");
			if (e.count(Runes.KINDRED) > 0) mods.add("shared at " + Math.round(SpellNumbers.KINDRED_SHARE * 100) + "% power");
			if (e.count(Runes.THIRST) > 0) mods.add("heals you " + Math.round(SpellNumbers.thirstShare(e) * 100) + "% of its damage");
			if (SpellNumbers.lingerHits(e) > 0) mods.add("+" + SpellNumbers.lingerHits(e) + " hits");
			if (e.effect.is(Runes.SHIELD.id())) mods.add(seconds(SpellNumbers.shieldTicks(e)));
			joiner.add(e.effect.name() + Ranks.suffix(e.rank) + mods);
		}
		return joiner.toString();
	}

	private static String blocks(double radius) {
		return trim(radius) + " blocks";
	}

	private static String seconds(int ticks) {
		return trim(ticks / 20.0) + "s";
	}

	private static String trim(double v) {
		return Math.abs(v - Math.rint(v)) < 1e-6 ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
	}
}
