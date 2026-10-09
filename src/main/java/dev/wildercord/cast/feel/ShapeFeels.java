package dev.wildercord.cast.feel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * The shapes' feel: each shape's gesture as it leaves the hand, its motion's sound pitched to the shape's own note of the
 * scale (so a Spark and a Ray, both flicks, never sound alike), and any shape signatures. Owned by the shapes work.
 */
final class ShapeFeels {
	private ShapeFeels() {}

	/** A shape's gesture: which note of its motion's sound, and how loud (before the band). */
	private record Gesture(float pitch, float volume) {}

	private static final float O = 0.5F;

	private static final Map<String, Gesture> GESTURES = Map.ofEntries(
		// FLICK
		Map.entry("touch", new Gesture(1.0F, 0.45F)), Map.entry("ray", new Gesture(1.26F, 0.55F)), Map.entry("spark", new Gesture(1.498F, 0.5F)),
		// HURL
		Map.entry("bolt", new Gesture(1.0F, 0.55F)), Map.entry("arc", new Gesture(1.682F * O, 0.55F)), Map.entry("wisp", new Gesture(1.498F, 0.45F)),
		Map.entry("ricochet", new Gesture(1.26F, 0.5F)), Map.entry("cluster", new Gesture(1.122F, 0.55F)), Map.entry("comet", new Gesture(1.498F * O, 0.65F)),
		Map.entry("orb", new Gesture(1.26F * O, 0.65F)),
		// BEAM
		Map.entry("beam", new Gesture(1.0F, 0.5F)), Map.entry("lance", new Gesture(1.498F * O, 0.6F)), Map.entry("prism", new Gesture(1.498F, 0.5F)),
		Map.entry("stream", new Gesture(1.26F, 0.45F)), Map.entry("latch", new Gesture(1.682F, 0.45F)), Map.entry("sweep", new Gesture(1.122F, 0.5F)),
		// SLASH
		Map.entry("cone", new Gesture(1.26F, 0.55F)), Map.entry("crescent", new Gesture(1.0F, 0.6F)), Map.entry("glaive", new Gesture(1.498F, 0.5F)),
		Map.entry("barrage", new Gesture(1.682F, 0.45F)), Map.entry("wave", new Gesture(1.498F * O, 0.6F)), Map.entry("blitz", new Gesture(1.122F, 0.6F)),
		// BLAST
		Map.entry("burst", new Gesture(1.0F, 0.65F)), Map.entry("nova", new Gesture(1.26F, 0.55F)), Map.entry("ring", new Gesture(1.498F, 0.55F)),
		Map.entry("pillar", new Gesture(1.498F * O, 0.65F)),
		// SEAL
		Map.entry("zone", new Gesture(1.0F, 0.55F)), Map.entry("totem", new Gesture(1.122F, 0.55F)), Map.entry("wall", new Gesture(1.498F * O, 0.6F)),
		Map.entry("vortex", new Gesture(1.682F * O, 0.6F)), Map.entry("domain", new Gesture(0.5F, 0.85F)), Map.entry("imprint", new Gesture(1.26F, 0.45F)),
		Map.entry("mine", new Gesture(1.498F, 0.3F)), Map.entry("snare", new Gesture(1.682F, 0.3F)), Map.entry("trail", new Gesture(1.26F, 0.35F)),
		Map.entry("relay", new Gesture(1.122F, 0.4F)), Map.entry("reweave", new Gesture(1.26F * O, 0.5F)),
		// CALL
		Map.entry("rain", new Gesture(1.0F, 0.6F)), Map.entry("constellation", new Gesture(1.498F, 0.55F)),
		// AURA
		Map.entry("self", new Gesture(1.0F, 0.4F)), Map.entry("orbit", new Gesture(1.26F, 0.5F)),
		// ---- shapes pack
		Map.entry("furrow", new Gesture(1.0F, 0.35F)), Map.entry("plot", new Gesture(1.122F, 0.4F)), Map.entry("seedbed", new Gesture(1.26F, 0.45F)),
		Map.entry("shaft", new Gesture(1.498F, 0.5F)), Map.entry("stairwell", new Gesture(1.682F, 0.35F)), Map.entry("corridor", new Gesture(1.0F * O, 0.4F)),
		Map.entry("seam", new Gesture(1.122F * O, 0.45F)), Map.entry("facade", new Gesture(1.26F * O, 0.5F)), Map.entry("dome", new Gesture(1.498F * O, 0.35F)),
		Map.entry("footing", new Gesture(1.682F * O, 0.4F)), Map.entry("canopy", new Gesture(1.0F, 0.45F)), Map.entry("shoreline", new Gesture(1.122F, 0.5F)),
		Map.entry("perimeter", new Gesture(1.26F, 0.35F)), Map.entry("spire", new Gesture(1.498F, 0.4F)), Map.entry("pit", new Gesture(1.682F, 0.45F)),
		Map.entry("crossway", new Gesture(1.0F * O, 0.5F)), Map.entry("lodeseek", new Gesture(1.122F * O, 0.35F)), Map.entry("vault", new Gesture(1.26F * O, 0.4F)),
		Map.entry("lamplit", new Gesture(1.498F * O, 0.45F)), Map.entry("fissure", new Gesture(1.682F * O, 0.5F)), Map.entry("spiral", new Gesture(1.0F, 0.35F)),
		Map.entry("rosette", new Gesture(1.122F, 0.4F)), Map.entry("stepstones", new Gesture(1.26F, 0.45F)), Map.entry("causeway", new Gesture(1.498F, 0.5F)),
		Map.entry("hedgerow", new Gesture(1.682F, 0.35F)), Map.entry("lattice", new Gesture(1.0F * O, 0.4F)), Map.entry("collapse", new Gesture(1.122F * O, 0.45F)),
		Map.entry("fan", new Gesture(1.26F * O, 0.5F)), Map.entry("bobber", new Gesture(1.498F * O, 0.35F)), Map.entry("herd", new Gesture(1.682F * O, 0.4F)),
		Map.entry("fellowship", new Gesture(1.0F, 0.45F)), Map.entry("saddle", new Gesture(1.122F, 0.5F)), Map.entry("packbond", new Gesture(1.26F, 0.35F)),
		Map.entry("nursery", new Gesture(1.498F, 0.4F)), Map.entry("shoal", new Gesture(1.682F, 0.45F)), Map.entry("rearguard", new Gesture(1.0F * O, 0.5F)),
		Map.entry("grudge", new Gesture(1.122F * O, 0.35F)), Map.entry("sentinel", new Gesture(1.26F * O, 0.4F)), Map.entry("aureole", new Gesture(1.498F * O, 0.45F)),
		Map.entry("tether", new Gesture(1.682F * O, 0.5F)), Map.entry("flock", new Gesture(1.0F, 0.35F)));

	/** Whether a shape (by path) has its own gesture; built-in shapes all do (see FeelTest). */
	static boolean hasGesture(String path) {
		return GESTURES.containsKey(path);
	}

	/** The kit sound of a motion's gesture. */
	static String gestureSound(Motion motion) {
		return "gesture_" + motion.name().toLowerCase(java.util.Locale.ROOT);
	}

	/** Plays the shape's gesture at the hand: its motion's sound, at its note, by band. Quickened spells snap a step higher. */
	static void gesture(ServerLevel level, Vec3 hand, Feel feel) {
		String path = feel.shapeId().substring(feel.shapeId().indexOf(':') + 1);
		Gesture g = GESTURES.getOrDefault(path, new Gesture(1.0F, 0.5F));
		float pitch = g.pitch() * (feel.has("quicken") ? 1.122F : 1.0F) * (feel.has("frugal") ? 1.122F : 1.0F);
		float volume = g.volume() * (feel.has("frugal") ? 0.6F : 1.0F);
		Feels.sound(level, hand, feel, gestureSound(feel.motion()), volume, Math.min(2.0F, pitch));
	}

	static void register() {
	}
}
