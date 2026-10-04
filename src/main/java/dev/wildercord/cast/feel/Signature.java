package dev.wildercord.cast.feel;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * One rune's own feel: what it adds to (or takes over from) the shared layers. Build one in your element's
 * {@code register()} and call {@link #register()}. Every setter is optional; a rune with no signature keeps the default
 * feel of its shape, element and role.
 *
 * <pre>{@code
 * Signature.of("meteor")
 *     .sound(Phase.CUE, "fire_flick")                       // played at the hand when it is cast
 *     .sound(Phase.IMPACT, "fire_whump", 1.0F, 1.0F)          // played at each impact (name, volume, pitch)
 *     .replace(Phase.IMPACT)                                  // ...instead of the element's impact sound
 *     .hook(Phase.AFTERMATH, ctx -> MyFx.crater(ctx))         // your own particles, given a FeelCtx
 *     .accent(0xFFB040)                                       // the theme's second colour for this rune's spells
 *     .register();
 * }</pre>
 *
 * A signature on an <b>effect</b> rune applies to every group whose first effect is that rune (cue, travel, impact,
 * aftermath) and, in {@link Phase#HIT}, to each creature that effect touches. A signature on a <b>shape</b> id works
 * the same for groups with that shape (an effect's signature wins over the shape's).
 */
public final class Signature {
	/** A sound of a signature: a kit name (see tools/feel), a volume and a pitch (1.0 = as authored). */
	public record Cue(String name, float volume, float pitch) {}

	final String id;
	final Map<Phase, Cue> sounds = new EnumMap<>(Phase.class);
	final Map<Phase, Hook> hooks = new EnumMap<>(Phase.class);
	final EnumSet<Phase> replaced = EnumSet.noneOf(Phase.class);
	Motion motion;
	Integer accent;
	double scale = 1.0;
	private boolean authoredOutcome;

	private Signature(String id) {
		this.id = id;
	}

	/** @param rune a rune id ({@code "wildercord:meteor"}) or its path ({@code "meteor"}) */
	public static Signature of(String rune) {
		return new Signature(rune.contains(":") ? rune : "wildercord:" + rune);
	}

	/** A sound from the kit at this phase (played in addition to the default unless you {@link #replace} the phase). */
	public Signature sound(Phase phase, String kitName) {
		return sound(phase, kitName, 1.0F, 1.0F);
	}

	public Signature sound(Phase phase, String kitName, float volume, float pitch) {
		sounds.put(phase, new Cue(kitName, volume, pitch));
		return this;
	}

	/** Your own particles or logic at this phase (runs in addition to the default unless you {@link #replace} the phase). */
	public Signature hook(Phase phase, Hook hook) {
		hooks.put(phase, hook);
		return this;
	}

	/** The default look and sound of these phases is left out: yours stands alone. */
	public Signature replace(Phase... phases) {
		replaced.addAll(java.util.List.of(phases));
		return this;
	}

	/** Opt in only after actual owner observations replace this rune's collision/aftermath body.
	 * This is independent of replace(IMPACT): existing signatures retain their old semantics. */
	public Signature authoredOutcome() { authoredOutcome = true; return this; }
	public boolean ownsOutcomeBody() { return authoredOutcome; }

	/** Treat this rune's spells as another motion (pose, cue): a Meteor is a CALL, not a HURL. */
	public Signature motion(Motion motion) {
		this.motion = motion;
		return this;
	}

	/** The theme's second colour (highlights, cores, secondary rings) for spells led by this rune, as 0xRRGGBB. */
	public Signature accent(int rgb) {
		this.accent = rgb;
		return this;
	}

	/** Multiplies the feel's scale (a rune that should always read bigger or smaller than its cost says). */
	public Signature scale(double factor) {
		this.scale = factor;
		return this;
	}

	public Signature register() {
		Signatures.put(this);
		return this;
	}

	public boolean replaces(Phase phase) {
		return replaced.contains(phase);
	}

	public Cue soundOf(Phase phase) {
		return sounds.get(phase);
	}

	public Hook hookOf(Phase phase) {
		return hooks.get(phase);
	}
}
