package dev.wildercord.aura;

import java.util.List;

/** The first authored style forms that now have a paid server windup before their existing effect choreography. */
public final class MastersStyleRules {
	private MastersStyleRules() {}

	public record Style(int animation, String art, int windup, int recovery) {}
	public static final List<Style> STYLES = List.of(
		new Style(3, "kindling_draw", 6, 12),
		new Style(4, "frostbite", 6, 12),
		new Style(5, "crackle", 4, 12),
		new Style(6, "cutting_breeze", 4, 10),
		new Style(7, "rockbreaker", 10, 18),
		new Style(8, "thorn_lash", 6, 12),
		new Style(9, "void_cut", 6, 12),
		new Style(10, "star_needle", 4, 10),
		new Style(11, "echo_cut", 6, 20),
		new Style(12, "bloodletting", 6, 12)
	);

	public static Style of(String art) {
		return STYLES.stream().filter(style -> style.art().equals(art)).findFirst().orElse(null);
	}

	public static Style animation(int id) {
		return STYLES.stream().filter(style -> style.animation() == id).findFirst().orElse(null);
	}
	/** Only the two projectile first forms tilt their attack plane, retaining their original pitch scaling. */
	public static float attackPitch(int animation, float viewPitch) {
		if (!Float.isFinite(viewPitch)) return 0;
		double factor = animation == 6 ? .4 : animation == 10 ? .35 : 0;
		if (factor == 0) return 0;
		double radians = Math.toRadians(Math.max(-90, Math.min(90, viewPitch)));
		return (float) Math.toDegrees(Math.atan2(Math.sin(radians) * factor, Math.max(0, Math.cos(radians))));
	}

}
