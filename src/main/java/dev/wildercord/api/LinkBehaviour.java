package dev.wildercord.api;

/**
 * When the rest of the spell after an add-on link rune fires. Called on the server once the groups
 * before the link have gone off; call {@link LinkContext#fire()} or {@link LinkContext#fireAt} now,
 * later (with {@link LinkContext#later}), several times, or never.
 *
 * <p>What follows the link starts with the implicit shape "Target": effects with no shape of their
 * own land on the creature (or point) passed to {@link LinkContext#fireAt}.</p>
 *
 * @since 1.0
 */
@FunctionalInterface
public interface LinkBehaviour {
	void link(LinkContext context);
}
