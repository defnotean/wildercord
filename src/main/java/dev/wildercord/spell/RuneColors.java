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
