package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;

/**
 * A spell after it has been read: segments of groups, joined by links.
 *
 * <pre>
 * Bolt · Fire · Split · On Hit · Burst · Explode
 * └─ segment: [group Bolt(+Split) → Fire]  link On Hit ─→ segment: [group Burst → Explode]
 * </pre>
 *
 * <p>A modifier that needs {@link Trait#COOLDOWN} (Rapid, Vow, Blood Price) sits among a group's
 * {@link Group#shapeMods} but changes the whole spell, so it's priced on the whole spell
 * (see {@link SpellCompiler#cost}).</p>
 */
public final class SpellPlan {
	private SpellPlan() {}

	/** Groups that fire together, plus an optional link to what fires later. */
	public static final class Segment {
		public final List<Group> groups = new ArrayList<>();
		/** The shape used when an effect appears before any shape in this segment. */
		public final RuneDef implicitShape;
		public Link link;

		Segment(RuneDef implicitShape) {
			this.implicitShape = implicitShape;
		}

		public boolean isEmpty() {
			return groups.isEmpty() && link == null;
		}
	}

	/** One shape and the effects it delivers. */
	public static final class Group {
		public final RuneDef shape;
		/** True when no shape rune was written and {@link Segment#implicitShape} stands in. */
		public final boolean implicit;
		public final List<RuneDef> shapeMods = new ArrayList<>();
		public final List<EffectNode> effects = new ArrayList<>();
		/** The share of its shape's mana it costs: less than 1 when the shape was tied into a Knot. */
		public double factor = 1.0;

		Group(RuneDef shape, boolean implicit) {
			this.shape = shape;
			this.implicit = implicit;
		}

		public int count(RuneDef modifier) {
			return SpellPlan.count(shapeMods, modifier);
		}
	}

	/** One effect and the modifiers attached to it. */
	public static final class EffectNode {
		public final RuneDef effect;
		public final List<RuneDef> mods = new ArrayList<>();
		/** The share of its mana it costs: less than 1 inside a Knot. */
		public double factor = 1.0;
		/** The caster's rank for this effect (see {@link Ranks}), for the readout; 1 when nobody's ranks were given. */
		public int rank = 1;

		EffectNode(RuneDef effect) {
			this.effect = effect;
		}

		public int count(RuneDef modifier) {
			return SpellPlan.count(mods, modifier);
		}
	}

	/** A link rune: when {@link #next} fires. */
	public static final class Link {
		public final RuneDef link;
		public final List<RuneDef> mods = new ArrayList<>();
		/** The group whose hits or kills trigger this link; null for links that don't watch a group. */
		public final Group anchor;
		public Segment next;
		/** Echo only: everything before the Echo, fired again. */
		public Segment echoPrefix;
		/**
		 * An Echo or a Pulse after an On Hit or On Kill. What follows those fires at every creature hit
		 * or killed, but a repeat is paid for once, so it goes off only the first time: once for each
		 * time the part holding it was paid for (the cast, or one of an earlier Pulse's runs).
		 */
		public boolean firstOnly;
		/** The share of its mana it costs: less than 1 inside a Knot. */
		public double factor = 1.0;

		Link(RuneDef link, Group anchor) {
			this.link = link;
			this.anchor = anchor;
		}

		public int count(RuneDef modifier) {
			return SpellPlan.count(mods, modifier);
		}
	}

	static int count(List<RuneDef> mods, RuneDef modifier) {
		int n = 0;
		for (RuneDef mod : mods) {
			if (mod.is(modifier.id())) {
				n++;
			}
		}
		return n;
	}
}
