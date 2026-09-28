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
		return compile(runes, Runes.SELF);
	}

	/**
	 * Reads what an Imbue stored: the runes after it, read as they were after the link, so an effect
	 * with no shape of its own lands on whatever set it off.
	 */
	public static Compiled compileStored(List<RuneDef> runes) {
		return compile(runes, Runes.TRIGGER);
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

	private static Compiled compile(List<RuneDef> runes, RuneDef implicitShape) {
		Reader reader = new Reader(runes, true, implicitShape);
		SpellPlan.Segment root = reader.segment(0, implicitShape, List.of());
		double cost = cost(root);
		List<String> lines = new ArrayList<>();
		describe(root, "", lines);
		checkEmptyGroups(root, reader.warnings);
		int rapid = countShapeMods(root, Runes.RAPID_MOD);
		int vows = countShapeMods(root, Runes.VOW_MOD);
		int healthCost = countShapeMods(root, Runes.BLOOD_PRICE_MOD) > 0 ? SpellNumbers.healthCost(cost) : 0;
		if (healthCost > 0) {
			lines.add("Costs " + healthCost + " health instead of mana.");
		}
		return new Compiled(root, cost, SpellNumbers.cooldownTicks(cost, rapid, vows), List.copyOf(lines), List.copyOf(reader.warnings), reader.attachedTo,
			healthCost);
	}

	// ------------------------------------------------------------------ reading

	private record Target(int index, RuneDef def, List<RuneDef> mods) {}

	private static final class Reader {
		final List<RuneDef> runes;
		final boolean record;
		final int[] attachedTo;
		final List<String> warnings = new ArrayList<>();
		int echoes;
		/**
		 * Where the spell an Echo repeats starts, and what an effect with no shape lands on there: the
		 * whole spell from you, or (inside an Imbue) only what was stored, at whatever set it off.
		 */
		int base;
		RuneDef baseShape;

		Reader(List<RuneDef> runes, boolean record, RuneDef rootShape) {
			this.runes = runes;
			this.record = record;
			this.attachedTo = new int[runes.size()];
			Arrays.fill(attachedTo, NOT_A_MODIFIER);
			this.baseShape = rootShape;
		}

		/** Whether this is part of a stored (imbued) spell, released later from an item or a glyph. */
		boolean stored() {
			return baseShape.is(Runes.TRIGGER.id());
		}

		SpellPlan.Segment segment(int from, RuneDef implicitShape, List<Target> inherited) {
			SpellPlan.Segment seg = new SpellPlan.Segment(implicitShape);
			List<Target> targets = new ArrayList<>(inherited);
			SpellPlan.Group group = null;
			for (int i = from; i < runes.size(); i++) {
				RuneDef rune = runes.get(i);
				switch (rune.family()) {
					case SHAPE -> {
						group = new SpellPlan.Group(rune, false);
						seg.groups.add(group);
						targets.add(new Target(i, rune, group.shapeMods));
					}
					case EFFECT -> {
						if (group == null) {
							group = new SpellPlan.Group(implicitShape, true);
							seg.groups.add(group);
						}
						SpellPlan.EffectNode node = new SpellPlan.EffectNode(rune);
						group.effects.add(node);
						targets.add(new Target(i, rune, node.mods));
					}
					case MODIFIER -> {
						Target target = null;
						for (int t = targets.size() - 1; t >= 0; t--) {
							if (targets.get(t).def().has(rune.needs())) {
								target = targets.get(t);
								break;
							}
						}
						if (target == null) {
							warn(rune.name() + " does nothing here: nothing on its left that it can change.");
							mark(i, UNATTACHED);
						} else {
							target.mods().add(rune);
							mark(i, target.index());
						}
					}
					case LINK -> {
						boolean watchesGroup = rune.is(Runes.ON_HIT.id()) || rune.is(Runes.ON_KILL.id()) || rune.is(Runes.IMBUE.id());
						if (watchesGroup && group == null) {
							warn(rune.name() + " needs a shape before it to watch.");
						}
						SpellPlan.Link link = new SpellPlan.Link(rune, watchesGroup ? group : null);
						if (rune.is(Runes.ECHO.id())) {
							echoes++;
							if (echoes > SpellNumbers.MAX_ECHOES) {
								warn("Only " + SpellNumbers.MAX_ECHOES + " Echoes count; the rest are ignored.");
							} else {
								link.echoPrefix = new Reader(runes.subList(base, i), false, baseShape).segment(0, baseShape, List.of());
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
						link.next = segment(i + 1, nextShape, List.of(new Target(i, rune, link.mods)));
						base = outerBase;
						baseShape = outerShape;
						if (link.next.isEmpty() && !rune.is(Runes.ECHO.id())) {
							warn(rune.name() + " has nothing after it.");
						}
						if (rune.is(Runes.IMBUE.id()) && runes.subList(i + 1, runes.size()).stream().anyMatch(r -> r.is(Runes.IMBUE.id()))) {
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

	private static int countShapeMods(SpellPlan.Segment seg, RuneDef modifier) {
		if (seg == null) {
			return 0;
		}
		int n = 0;
		for (SpellPlan.Group g : seg.groups) {
			n += g.count(modifier);
		}
		return n + (seg.link == null ? 0 : countShapeMods(seg.link.next, modifier));
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
		}
		if (seg.link != null) {
			checkEmptyGroups(seg.link.next, warnings);
		}
	}

	// ------------------------------------------------------------------ cost

	public static double cost(SpellPlan.Segment seg) {
		if (seg == null) {
			return 0;
		}
		double total = 0;
		for (SpellPlan.Group g : seg.groups) {
			total += groupCost(g);
		}
		if (seg.link != null) {
			SpellPlan.Link link = seg.link;
			double rest = cost(link.next) * (link.link.is(Runes.PULSE.id()) ? SpellNumbers.PULSES : link.link.is(Runes.IMBUE.id()) ? SpellNumbers.IMBUE_CHARGES : 1);
			total += link.link.cost() * product(link.mods) + rest + cost(link.echoPrefix);
		}
		return total;
	}

	private static double groupCost(SpellPlan.Group g) {
		double effects = 0;
		for (SpellPlan.EffectNode e : g.effects) {
			effects += e.effect.cost() * product(e.mods);
		}
		return (g.shape.cost() + effects * g.shape.multiplier()) * product(g.shapeMods);
	}

	private static double product(List<RuneDef> mods) {
		double p = 1;
		for (RuneDef mod : mods) {
			p *= mod.multiplier();
		}
		return p;
	}

	// ------------------------------------------------------------------ readout

	private static void describe(SpellPlan.Segment seg, String indent, List<String> lines) {
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
		if (id.equals(Runes.ECHO.id())) {
			if (link.echoPrefix != null) {
				lines.add(indent + "0.5s later, everything before this fires again.");
			}
			describe(link.next, indent, lines);
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
			header = SpellNumbers.PULSES + " times, every " + seconds(SpellNumbers.pulseInterval(link)) + ":";
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
		} else {
			header = link.link.name() + ":";
		}
		lines.add(indent + header);
		describe(link.next, indent + "  ", lines);
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
			return (copies > 1 ? copies + " fields" : "A field") + " (" + blocks(SpellNumbers.zoneRadius(g)) + ", " + SpellNumbers.zoneSeconds(g) + "s, every " + seconds(SpellNumbers.zoneInterval(g)) + ")";
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
			return "A " + blocks(SpellNumbers.wallWidth(g)).replace(" blocks", "-block") + " wall (" + SpellNumbers.wallSeconds(g) + "s)";
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
			return "A totem (" + blocks(SpellNumbers.totemRadius(g)) + ", " + SpellNumbers.totemSeconds(g) + "s, every " + seconds(SpellNumbers.totemInterval(g)) + ")";
		}
		if (id.equals(Runes.DOMAIN.id())) {
			return "Your domain (" + blocks(SpellNumbers.domainRadius(g)) + ", " + SpellNumbers.domainSeconds(g) + "s, every " + seconds(SpellNumbers.domainInterval(g)) + ")";
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
			if (amp > 0) mods.add("+" + Math.round((SpellNumbers.power(e) - 1) * 100) + "% power");
			if (ext > 0) mods.add(trim(SpellNumbers.duration(e)) + "x duration");
			if (wid > 0) mods.add("+" + Math.round((SpellNumbers.effectRadius(e) - 1) * 100) + "% radius");
			if (e.count(Runes.FRUGAL_MOD) > 0) mods.add("frugal");
			if (e.count(Runes.OVERCHARGE_MOD) > 0) mods.add("overcharged");
			if (e.count(Runes.FOCUS_MOD) > 0) mods.add("focused");
			if (e.count(Runes.EXECUTE_MOD) > 0) mods.add("x" + trim(SpellNumbers.executeBonus(e)) + " under half health");
			if (e.count(Runes.TRIAL_KEY) > 0) mods.add("x" + trim(SpellNumbers.trialKeyBonus(e)) + " at full health");
			if (e.count(Runes.KINDLED) > 0) mods.add("sets alight " + SpellNumbers.kindledSeconds(e) + "s");
			if (e.count(Runes.UNSTABLE) > 0) mods.add("unstable: 50-200% power");
			if (SpellNumbers.lingerHits(e) > 0) mods.add("+" + SpellNumbers.lingerHits(e) + " hits");
			if (e.effect.is(Runes.SHIELD.id())) mods.add(seconds(SpellNumbers.shieldTicks(e)));
			joiner.add(e.effect.name() + mods);
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
