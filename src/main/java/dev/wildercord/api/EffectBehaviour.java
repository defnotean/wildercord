package dev.wildercord.api;

/**
 * What an add-on effect rune does when its shape hits something.
 *
 * <p>Called on the server for each hit of each group the effect is in. Targets are already sorted
 * into {@link EffectContext#harmed()} (fair game: never the caster or their allies, and past any
 * Shield) and {@link EffectContext#helped()} (the caster and allies). Deal damage through
 * {@link EffectContext#hurt}, and check {@link EffectContext#mayEdit} before changing a block.</p>
 *
 * @since 1.0
 */
@FunctionalInterface
public interface EffectBehaviour {
	void apply(EffectContext context);
}
