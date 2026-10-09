package dev.wildercord.cast.feel;

/** A shape's gesture family: how the spell leaves the hand and moves. Drives poses, cue sounds and travel decoration. */
public enum Motion {
	/** A small quick flick: Spark, Ray, Touch. */
	FLICK,
	/** Something thrown: Bolt, Arc, Wisp, Ricochet, Cluster, Comet, Orb. */
	HURL,
	/** A line of light from the hand: Beam, Lance, Prism, Stream, Latch, Sweep. */
	BEAM,
	/** An arm sweeping: Cone, Crescent, Glaive, Barrage, Wave, Blitz. */
	SLASH,
	/** Radiating from a point: Burst, Nova, Ring, Pillar. */
	BLAST,
	/** A seal on the ground that lingers: Zone, Totem, Wall, Vortex, Domain, Imprint, Mine, Snare, Trail, Relay, Reweave. */
	SEAL,
	/** Called from above: Rain, Constellation. */
	CALL,
	/** Around the caster: Self, Orbit. */
	AURA;

	/** The motion of a shape id or path (add-on shapes are {@link #HURL}). */
	public static Motion of(String shape) {
		String path = shape.substring(shape.indexOf(':') + 1);
		return switch (path) {
			case "spark", "ray", "touch" -> FLICK;
			case "beam", "lance", "prism", "stream", "latch", "sweep" -> BEAM;
			case "cone", "crescent", "glaive", "barrage", "wave", "blitz" -> SLASH;
			case "burst", "nova", "ring", "pillar" -> BLAST;
			case "zone", "totem", "wall", "vortex", "domain", "imprint", "mine", "snare", "trail", "relay", "reweave" -> SEAL;
			case "rain", "constellation" -> CALL;
			case "self", "orbit", "trigger" -> AURA;
			// ---- shapes pack
			case "furrow", "seam", "hedgerow", "fissure", "fan", "stepstones", "causeway", "corridor", "stairwell" -> SLASH;
			case "plot", "seedbed", "lattice", "perimeter", "rosette", "spiral", "crossway", "lamplit", "shoreline" -> SEAL;
			case "shaft", "spire", "pit", "collapse", "vault", "dome", "facade", "canopy", "lodeseek" -> BLAST;
			case "footing", "aureole", "saddle", "fellowship", "packbond" -> AURA;
			case "herd", "nursery", "shoal", "flock", "sentinel", "grudge", "rearguard" -> CALL;
			case "tether" -> BEAM;
			case "bobber" -> FLICK;
			default -> HURL;
		};
	}
}
