package dev.wildercord.api;

/**
 * The entrypoint an add-on implements to extend Wildercord. List the class under the
 * {@code "wildercord"} entrypoint key in your {@code fabric.mod.json}:
 *
 * <pre>{@code
 * "entrypoints": {
 *   "wildercord": ["com.example.MyAddon"]
 * }
 * }</pre>
 *
 * <p>Wildercord calls {@link #onWildercordInit} once, on both the client and the server, at the end
 * of its own initialisation: register runes, categories and reactions there (the client needs the
 * same runes to show them), and subscribe to {@link WildercordEvents}. Everything registered must be
 * the same on both sides.</p>
 *
 * @since 1.0
 */
@FunctionalInterface
public interface WildercordAddon {
	/** Register runes, categories, reactions and event listeners. */
	void onWildercordInit(WildercordApi api);
}
