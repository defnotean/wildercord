package dev.wildercord.compat;

/**
 * Brewing mixes are loaded from {@code data/wildercord/brewing/*.json} in Minecraft 26.3; this compat placeholder
 * keeps the {@code compat/} surface aligned with 26.2 (where {@code PotionBrewingMixin} calls {@code register}).
 */
public final class WildercordBrewing {
	private WildercordBrewing() {}
}
