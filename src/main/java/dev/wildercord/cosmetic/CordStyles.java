package dev.wildercord.cosmetic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * How a Cord can look, and how each look is earned, with no Minecraft in it. A style has three
 * parts: the beads' material, their glow (the spell's own colours, or one fixed colour) and the
 * trail a cast leaves. Some parts come with play (Heart Circles, a boss, a feat), the rest are
 * bought once with materials in the Cord screen's Cosmetics page.
 *
 * <p>Options are keyed {@code group:id}, e.g. {@code material:gold}, {@code glow:red},
 * {@code trail:embers}.</p>
 */
public final class CordStyles {
	private CordStyles() {}

	public static final String MATERIAL = "material";
	public static final String GLOW = "glow";
	public static final String TRAIL = "trail";

	/** How an option is earned. */
	public enum Kind { FREE, CIRCLE, FEAT, BOSS, CRAFT }

	/**
	 * What unlocks an option: nothing ({@code FREE}), a Heart Circle ({@code amount}), a feat
	 * ({@code what} is its id), helping slay a boss, or materials bought in the Cosmetics page
	 * ({@code amount} of the item {@code what}).
	 */
	public record Unlock(Kind kind, String what, int amount) {
		static Unlock free() {
			return new Unlock(Kind.FREE, "", 0);
		}

		static Unlock circle(int n) {
			return new Unlock(Kind.CIRCLE, "", n);
		}

		static Unlock feat(String id) {
			return new Unlock(Kind.FEAT, id, 0);
		}

		static Unlock boss() {
			return new Unlock(Kind.BOSS, "", 0);
		}

		static Unlock craft(String item, int count) {
			return new Unlock(Kind.CRAFT, item, count);
		}
	}

	/** One choice: its group and id, a colour for its swatch (and, for glows, the glow itself), and its unlock. */
	public record Option(String group, String id, int color, Unlock unlock) {
		public String key() {
			return group + ":" + id;
		}
	}

	public static final List<Option> MATERIALS = List.of(
		new Option(MATERIAL, "glass", 0xE8F4FF, Unlock.free()),
		new Option(MATERIAL, "gold", 0xF2C84B, Unlock.craft("minecraft:gold_ingot", 4)),
		new Option(MATERIAL, "obsidian", 0x2A1A40, Unlock.circle(4)),
		new Option(MATERIAL, "amethyst", 0xB07CEB, Unlock.craft("minecraft:amethyst_shard", 4)),
		new Option(MATERIAL, "bone", 0xE8E0C8, Unlock.feat("runebound")),
		new Option(MATERIAL, "prismarine", 0x6FC8B4, Unlock.craft("minecraft:prismarine_crystals", 4)));

	public static final List<Option> TRAILS = List.of(
		new Option(TRAIL, "none", 0x8A84A0, Unlock.free()),
		new Option(TRAIL, "sparks", 0xFFE070, Unlock.circle(1)),
		new Option(TRAIL, "petals", 0xFFA8D8, Unlock.craft("minecraft:pink_petals", 4)),
		new Option(TRAIL, "snow", 0xE6FAFF, Unlock.craft("minecraft:snowball", 8)),
		new Option(TRAIL, "embers", 0xFF8A3A, Unlock.craft("minecraft:blaze_powder", 4)),
		new Option(TRAIL, "stars", 0xC8B8FF, Unlock.boss()));

	/** The sixteen dye colours as glows (brightened a little, since they're light), after "spell". */
	private static final String[] DYES = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
		"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
	private static final int[] DYE_GLOWS = {0xF8F8FF, 0xFF9A3C, 0xF070E0, 0x70C8FF, 0xFFE850, 0x90F050, 0xFF9CC8, 0x9098A8,
		0xC8CCD8, 0x40E0E0, 0xB070F0, 0x5070FF, 0xC08850, 0x58C048, 0xFF4848, 0x483858};

	public static final List<Option> GLOWS;

	static {
		List<Option> glows = new ArrayList<>();
		glows.add(new Option(GLOW, "spell", 0xFFFFFF, Unlock.free()));
		for (int i = 0; i < DYES.length; i++) {
			glows.add(new Option(GLOW, DYES[i], DYE_GLOWS[i], Unlock.craft("minecraft:" + DYES[i] + "_dye", 1)));
		}
		GLOWS = List.copyOf(glows);
	}

	/** A whole look: material, glow ("spell" or a dye) and trail. */
	public record Style(String material, String glow, String trail) {
		public static final Style DEFAULT = new Style("glass", "spell", "none");

		public Style withMaterial(String id) {
			return new Style(id, glow, trail);
		}

		public Style withGlow(String id) {
			return new Style(material, id, trail);
		}

		public Style withTrail(String id) {
			return new Style(material, glow, id);
		}

		/** The same style with one option (any group) swapped in. */
		public Style with(Option option) {
			return switch (option.group()) {
				case MATERIAL -> withMaterial(option.id());
				case GLOW -> withGlow(option.id());
				default -> withTrail(option.id());
			};
		}

		public String get(String group) {
			return switch (group) {
				case MATERIAL -> material;
				case GLOW -> glow;
				default -> trail;
			};
		}
	}

	/** What a player has done that unlocks styles, and what they've bought. */
	public record Progress(int circles, Collection<String> grimoire, boolean bossSlain, Collection<String> bought) {}

	public static List<Option> group(String group) {
		return switch (group) {
			case MATERIAL -> MATERIALS;
			case GLOW -> GLOWS;
			default -> TRAILS;
		};
	}

	/** The option with this key, or null. */
	public static Option option(String key) {
		int colon = key.indexOf(':');
		if (colon < 0) {
			return null;
		}
		return find(key.substring(0, colon), key.substring(colon + 1));
	}

	public static Option find(String group, String id) {
		for (Option option : group(group)) {
			if (option.group().equals(group) && option.id().equals(id)) {
				return option;
			}
		}
		return null;
	}

	public static boolean unlocked(Option option, Progress progress) {
		Unlock unlock = option.unlock();
		return switch (unlock.kind()) {
			case FREE -> true;
			case CIRCLE -> progress.circles() >= unlock.amount();
			case FEAT -> progress.grimoire().contains("feat:" + unlock.what());
			case BOSS -> progress.bossSlain();
			case CRAFT -> progress.bought().contains(option.key());
		};
	}

	/** Whether every part of {@code style} is a real option the player has unlocked. */
	public static boolean allowed(Style style, Progress progress) {
		for (String group : List.of(MATERIAL, GLOW, TRAIL)) {
			Option option = find(group, style.get(group));
			if (option == null || !unlocked(option, progress)) {
				return false;
			}
		}
		return true;
	}

	/** The style with any unknown or locked part put back to the default (a style saved before an option was taken away, say). */
	public static Style sanitize(Style style, Progress progress) {
		Style out = style;
		for (String group : List.of(MATERIAL, GLOW, TRAIL)) {
			Option option = find(group, out.get(group));
			if (option == null || !unlocked(option, progress)) {
				out = out.with(find(group, Style.DEFAULT.get(group)));
			}
		}
		return out;
	}

	/** The fixed glow colour for {@code glow}, or -1 to follow each rune's own colour. */
	public static int glowColor(String glow) {
		if (glow.equals("spell")) {
			return -1;
		}
		Option option = find(GLOW, glow);
		return option == null ? -1 : option.color();
	}

	/** Whether this option can be bought in the Cosmetics page (rather than earned by play). */
	public static boolean buyable(Option option) {
		return option.unlock().kind() == Kind.CRAFT;
	}
}
