/**
 * Wildercord's public API for add-on mods.
 *
 * <p>An add-on implements {@link dev.wildercord.api.WildercordAddon} and lists it under the
 * {@code "wildercord"} entrypoint in its {@code fabric.mod.json}. Through
 * {@link dev.wildercord.api.WildercordApi} it can:</p>
 * <ul>
 *   <li>register runes of every family (effects, shapes, modifiers, links) with their traits and
 *       behaviour ({@link dev.wildercord.api.RuneBuilder});</li>
 *   <li>register Codex categories and element reactions ({@link dev.wildercord.api.ElementReaction});</li>
 *   <li>listen to casts, hits, Shields blocking spells and imbued spells being released
 *       ({@link dev.wildercord.api.WildercordEvents});</li>
 *   <li>read a player's mana, learned runes and spells.</li>
 * </ul>
 *
 * <p>Within 1.x everything public in this package is only ever added to, never removed or renamed.
 * Rune definitions ({@link dev.wildercord.spell.RuneDef}), {@link dev.wildercord.spell.Trait},
 * {@link dev.wildercord.spell.EffectKind} and {@link dev.wildercord.spell.RuneFamily} are part of the
 * API too. Everything else in Wildercord is internal and may change. See {@code docs/API.md}.</p>
 */
package dev.wildercord.api;
