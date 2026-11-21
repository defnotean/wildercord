package dev.wildercord.cast;

import dev.wildercord.Wildercord;

/**
 * Pure presentation rules for rendering burning {@code wildercord} custom mobs with crisp perimeter
 * embers and smoke instead of vanilla's oversized planar fire billboard that hides detailed models.
 */
public final class CustomMobFireRules {
	private CustomMobFireRules() {}

	/** Whether an entity type namespace belongs to Wildercord custom entities. */
	public static boolean isCustomMobNamespace(String namespace) {
		return Wildercord.MOD_ID.equals(namespace);
	}

	/** Number of perimeter flame wisps per burst based on the mob's bounding volume. */
	public static int emberCount(double bbWidth, double bbHeight) {
		double volume = Math.max(0.1, bbWidth) * Math.max(0.5, bbHeight);
		return Math.clamp((int) Math.round(volume * 0.45), 1, 3);
	}

	/** Horizontal ring radius keeping fire embers around the mob's silhouette rather than across its face. */
	public static double perimeterRadius(double bbWidth) {
		return Math.max(0.35, bbWidth * 0.52 + 0.08);
	}
}
