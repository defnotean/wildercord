package dev.wildercord.cast.feel;

/**
 * The signatures of the fire runes. One owner per file, so nobody edits another element's registrations: add a
 * {@link Signature} per rune here (see docs/ADDING_RUNES.md, "Give it its own feel").
 *
 * <pre>{@code
 * Signature.of("ember").sound(Phase.HIT, "fire_flick").register();
 * }</pre>
 */
final class FireFeels {
	private FireFeels() {}

	static void register() {
	}
}
