package dev.wildercord.spell;

/** Family and element colours (0xRRGGBB), shared by items, particles and the UI. */
public final class RuneColors {
	private RuneColors() {}

	public static final int SHAPE = 0x40C8BE;
	public static final int MODIFIER = 0xF0C440;
	public static final int LINK = 0xA064F0;
	public static final int KNOT = 0xF08CB4;
	public static final int NEUTRAL = 0xC8C8D2;

	public static int element(String element) {
		return switch (element) {
			case "fire" -> 0xF06E32;
			case "frost" -> 0x8CDCFF;
			case "storm" -> 0xFFE650;
			case "wind" -> 0xC8F0DC;
			case "earth" -> 0xB48C5A;
			case "life" -> 0x6EDC64;
			case "void" -> 0xB45AF0;
			case "arcane" -> 0xE678DC;
			case "time" -> 0xF2D98A;
			case "blood" -> 0xD2283C;
			default -> NEUTRAL;
		};
	}

	/**
	 * A fused rune's second colour, for the half of its ring and emblem that shows its partner element
	 * (see tools/circle_art.py): that element's colour, or for a fusion of one element with itself, its
	 * own lightened. -1 for any other rune.
	 */
	public static int second(RuneDef rune) {
		return Fusions.recipeFor(rune).map(recipe -> {
			String own = recipe.first().equals(rune.element()) || !recipe.second().equals(rune.element()) ? recipe.first() : recipe.second();
			String partner = own.equals(recipe.first()) ? recipe.second() : recipe.first();
			return own.equals(partner) ? lighten(element(partner), 0.45F) : element(partner);
		}).orElse(-1);
	}

	private static int lighten(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return Math.round(r + (255 - r) * t) << 16 | Math.round(g + (255 - g) * t) << 8 | Math.round(b + (255 - b) * t);
	}

	public static int of(RuneDef rune) {
		return switch (rune.family()) {
			case SHAPE -> SHAPE;
			case EFFECT -> element(rune.element());
			case MODIFIER -> MODIFIER;
			case LINK -> LINK;
			case KNOT -> KNOT;
		};
	}
}
