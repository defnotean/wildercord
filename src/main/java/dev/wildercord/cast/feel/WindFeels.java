package dev.wildercord.cast.feel;

/**
 * The signatures of the wind runes. Each has an accent by family (gust is mint, sky is pale blue for the verticals, heavy
 * is grey-green for slams and whirls, sand for the dust devil, warm gold for Zephyr and Recoil), a scale that matches its
 * power, a quiet cast tag in the rune's own voice, and its own impact: the shape's flare and rings stay, the element's
 * shared wind pop goes (fourteen wind runes used to end in the same vanilla pop), and a generic touched glow is dropped
 * where the rune already draws its own burst.
 *
 * <p>Families: displace (Push, Windcut, Repel, Disarm, Recoil), vertical (Launch, Levitate, Updraft, Downdraft, Summit
 * Wind, Skyglyph, Cushion, Feather Fall), whirl (Cyclone, Dust Devil, Razorgale), buff and ward (Swift, Leap, Dash, Gale
 * Mantle, Deflect, Zephyr) and Prune.</p>
 */
final class WindFeels {
	private WindFeels() {}

	private static final int GUST = 0xC8F0DC;
	private static final int SKY = 0xBFE3FF;
	private static final int HEAVY = 0x9FB8B0;
	private static final int SAND = 0xD8C08A;

	private static Signature wind(String rune, double scale, int accent, String tag, float volume, float pitch) {
		return Signature.of(rune).scale(scale).accent(accent).sound(Phase.CUE, tag, volume, pitch).replace(Phase.IMPACT, Phase.HIT);
	}

	static void register() {
		// Displace: slabs, slash lines, domes; outward and straight.
		wind("push", 0.9, GUST, "wind_thump", 0.3F, 1.0F).hook(Phase.AFTERMATH, FrostWindFx::settle).register();
		wind("windcut", 0.9, 0xFFFFFF, "wind_slash", 0.3F, 1.0F).hook(Phase.AFTERMATH, FrostWindFx::settle).register();
		wind("repel", 1.05, GUST, "wind_thump", 0.35F, 0.7F).motion(Motion.BLAST).hook(Phase.AFTERMATH, FrostWindFx::settle).register();
		wind("disarm", 0.95, GUST, "wind_snatch", 0.3F, 1.0F).register();
		wind("recoil", 1.05, 0xFFD87A, "wind_rewind", 0.3F, 1.0F).register();

		// Vertical: columns, rising rings, falling crescents.
		wind("launch", 1.0, SKY, "wind_rise", 0.3F, 1.0F).hook(Phase.AFTERMATH, FrostWindFx::settle).register();
		wind("levitate", 0.95, SKY, "wind_rise", 0.25F, 0.7F).register();
		wind("updraft", 1.15, SKY, "wind_rise", 0.35F, 1.0F).register();
		wind("downdraft", 1.15, HEAVY, "wind_crash", 0.35F, 1.0F).motion(Motion.CALL).register();
		wind("summit_wind", 1.15, 0xE8F4FF, "wind_rise", 0.35F, 0.6F).register();
		wind("skyglyph", 1.0, SKY, "wind_glyph", 0.3F, 1.0F).motion(Motion.SEAL).register();
		wind("cushion", 0.85, SKY, "wind_feather", 0.3F, 0.7F).register();
		wind("feather_fall", 0.8, SKY, "wind_feather", 0.3F, 1.3F).register();

		// Whirl: funnels and spiralling crescents.
		wind("cyclone", 1.1, HEAVY, "wind_whirl", 0.3F, 1.0F).motion(Motion.SEAL).register();
		wind("dust_devil", 1.3, SAND, "wind_whirl", 0.35F, 0.7F).register();
		wind("razorgale", 1.1, 0xE05060, "wind_slash", 0.35F, 1.2F).register();

		// Buff and ward: streaks, rings at the feet, orbiting gusts.
		wind("swift", 0.8, GUST, "wind_dash", 0.25F, 1.5F).register();
		wind("leap", 0.8, GUST, "wind_feather", 0.3F, 1.1F).register();
		wind("dash", 0.95, GUST, "wind_dash", 0.35F, 1.0F).register();
		wind("gale_mantle", 1.0, GUST, "wind_gale", 0.3F, 1.0F).register();
		wind("deflect", 0.95, GUST, "wind_deflect", 0.3F, 0.9F).register();
		wind("zephyr", 1.05, 0xFFE6A8, "wind_feather", 0.3F, 0.9F).register();
		wind("prune", 0.7, GUST, "wind_slash", 0.3F, 1.4F).register();
	}
}
