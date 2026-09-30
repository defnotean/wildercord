package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;

/**
 * A stable visual fingerprint for one rune. Its own illustrated emblem is the centre of the animation;
 * these values choose the surrounding stroke, rhythm, rotation and accent. The full 64-bit fingerprint
 * is retained so two built-in runes cannot silently acquire the same choreography.
 */
public record AnimationSignature(long fingerprint, int motif, int arms, int beat, int turn, int accent) {
	public static AnimationSignature of(RuneDef rune) {
		AnimationSignature base = of(rune.id());
		if (rune.family() != RuneFamily.EFFECT) {
			return base;
		}
		// Keep the stroke's motion related to what the effect does. Its emblem, rotation, rhythm,
		// number of strokes and accent still belong to this one rune alone.
		int[] grammar = switch (rune.category()) {
			case "control" -> new int[] {2, 4, 7, 1};
			case "support" -> new int[] {0, 2, 3, 7};
			case "movement" -> new int[] {3, 1, 0, 6};
			case "world" -> new int[] {0, 2, 6, 7};
			case "summon" -> new int[] {3, 0, 2, 7};
			case "time" -> new int[] {7, 2, 3, 0};
			default -> rune.kind() == EffectKind.HELPFUL ? new int[] {0, 2, 3, 7} : new int[] {1, 4, 5, 6};
		};
		return new AnimationSignature(base.fingerprint(), grammar[(int) (base.fingerprint() & 3)], base.arms(), base.beat(),
			base.turn(), base.accent());
	}

	public static AnimationSignature of(String id) {
		long hash = 0xcbf29ce484222325L;
		for (byte b : id.getBytes(StandardCharsets.UTF_8)) {
			hash ^= b & 0xffL;
			hash *= 0x100000001b3L;
		}
		// Avalanche the short rune paths before taking individual choices from the bits.
		hash ^= hash >>> 33;
		hash *= 0xff51afd7ed558ccdL;
		hash ^= hash >>> 33;
		hash *= 0xc4ceb9fe1a85ec53L;
		hash ^= hash >>> 33;
		return new AnimationSignature(hash, (int) (hash & 7), 3 + (int) ((hash >>> 3) & 3),
			2 + (int) ((hash >>> 6) & 3), (int) ((hash >>> 9) & 15), (int) ((hash >>> 13) & 15));
	}
}
