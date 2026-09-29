package dev.wildercord.client;

import dev.wildercord.spell.RuneColors;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Map;

/**
 * Tiny pixel marks for the ten elements (a flame, a snowflake, a bolt...), 7 pixels square, drawn
 * with fills like the Shield's hexagon so they stay crisp at every GUI scale. The HUD uses them for
 * the elemental climate: each element where you stand favours (a green up-chevron) or hinders (a red
 * down-chevron).
 */
public final class ElementGlyphs {
	private ElementGlyphs() {}

	public static final int SIZE = 7;
	/** One climate mark: the glyph, a gap, the chevron, and space before the next. */
	private static final int ENTRY = SIZE + 1 + 5 + 3;
	private static final int UP = 0xFF7FE08C;
	private static final int DOWN = 0xFFE06060;

	private static String[] shape(String element) {
		return switch (element) {
			case "fire" -> new String[] {
				"...#...",
				"...##..",
				"..###..",
				".#####.",
				".##.##.",
				".#...#.",
				"..###.."};
			case "frost" -> new String[] {
				"...#...",
				".#.#.#.",
				"..###..",
				"#######",
				"..###..",
				".#.#.#.",
				"...#..."};
			case "storm" -> new String[] {
				"....##.",
				"...##..",
				"..####.",
				".####..",
				"...##..",
				"..##...",
				"..#...."};
			case "wind" -> new String[] {
				".####..",
				"#....#.",
				".....#.",
				"#####..",
				".......",
				"####...",
				"....#.."};
			case "earth" -> new String[] {
				".......",
				"...#...",
				"..###..",
				".##.##.",
				".#####.",
				"#######",
				"#######"};
			case "life" -> new String[] {
				"....###",
				"..#####",
				".###.##",
				".##.###",
				".#.####",
				".#####.",
				"#......"};
			case "void" -> new String[] {
				"..###..",
				".##....",
				"##.....",
				"##.....",
				"##.....",
				".##....",
				"..###.."};
			case "arcane" -> new String[] {
				"...#...",
				"..#.#..",
				".#...#.",
				"#..#..#",
				".#...#.",
				"..#.#..",
				"...#..."};
			case "time" -> new String[] {
				"#######",
				".#...#.",
				"..#.#..",
				"...#...",
				"..#.#..",
				".#.#.#.",
				"#######"};
			case "blood" -> new String[] {
				"...#...",
				"...#...",
				"..###..",
				".#####.",
				".#####.",
				".#####.",
				"..###.."};
			default -> new String[] {
				".......",
				"..###..",
				".#####.",
				".#####.",
				".#####.",
				"..###..",
				"......."};
		};
	}

	/** The element's mark at {@code x}, {@code y} (its top left), in its colour over a soft shadow. */
	public static void draw(GuiGraphicsExtractor g, String element, int x, int y) {
		int color = 0xFF000000 | RuneColors.element(element);
		String[] rows = shape(element);
		for (int pass = 0; pass < 2; pass++) {
			int offset = pass == 0 ? 1 : 0;
			int c = pass == 0 ? 0x70000000 : color;
			for (int row = 0; row < rows.length; row++) {
				String line = rows[row];
				int run = -1;
				for (int col = 0; col <= line.length(); col++) {
					boolean on = col < line.length() && line.charAt(col) == '#';
					if (on && run < 0) {
						run = col;
					} else if (!on && run >= 0) {
						g.fill(x + run + offset, y + row + offset, x + col + offset, y + row + 1 + offset, c);
						run = -1;
					}
				}
			}
		}
	}

	/** A chevron 5 pixels wide and 3 tall, pointing up (favoured) or down (hindered). */
	private static void chevron(GuiGraphicsExtractor g, int x, int y, boolean up) {
		int color = up ? UP : DOWN;
		for (int i = 0; i < 3; i++) {
			int row = up ? i : 2 - i;
			g.fill(x + 2 - i, y + row, x + 3 + i, y + row + 1, color);
		}
	}

	/** How wide a row of {@code count} climate marks is (0 for none). */
	public static int rowWidth(int count) {
		return count <= 0 ? 0 : count * ENTRY - 3;
	}

	/** A row of climate marks from {@code x}: each element and whether it's favoured (factor above 1) or hindered. */
	public static void row(GuiGraphicsExtractor g, Map<String, Double> factors, int x, int y) {
		int at = x;
		for (Map.Entry<String, Double> entry : factors.entrySet()) {
			draw(g, entry.getKey(), at, y);
			chevron(g, at + SIZE + 1, y + 2, entry.getValue() > 1.0);
			at += ENTRY;
		}
	}
}
