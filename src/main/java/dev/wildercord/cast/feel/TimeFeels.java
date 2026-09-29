package dev.wildercord.cast.feel;

/**
 * The signatures of the time runes. Like the void ones, each gets an early cue at the hand ({@code time_cue}: a dry tick and a
 * tiny glass ping, at a pitch of its own on the scale) so the cast is heard the moment it starts, and a scale that says how big
 * its spells read. What happens after (the upright faces, the sand, the sounds at the target) lives where it is drawn:
 * {@code TimeFx}, {@code TechniqueVfx}, {@code FusedVoidVfx}, {@code SignatureVfx}; the kit is {@code tools/feel/time.py}.
 */
final class TimeFeels {
	private TimeFeels() {}

	private static Signature rune(String id, int step, double scale) {
		Signature s = Signature.of(id).sound(Phase.CUE, "time_cue", 0.9F, Feels.step(step));
		if (scale != 1.0) {
			s.scale(scale);
		}
		return s;
	}

	static void register() {
		rune("borrowed_time", 0, 1.0).register();
		rune("countdown", 1, 0.9).register();
		rune("accelerate", 4, 1.0).register();
		rune("chronoshift", 3, 1.0).register();
		rune("foresight", 2, 1.0).register();
		rune("prolong", 1, 1.0).register();
		rune("reckoning", 0, 1.0).register();
		rune("riposte", 3, 1.0).register();
		rune("time_skip", 5, 1.0).register();
		rune("timesteal", 2, 1.0).register();
		rune("doomclock", 0, 1.2).register();
		rune("rewind", 0, 1.2).register();
		rune("stasis", 0, 1.3).register();
	}
}
