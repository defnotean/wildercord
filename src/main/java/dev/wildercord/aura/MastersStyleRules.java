package dev.wildercord.aura;

import java.util.List;

/** Authored fixed-release style forms with a paid server windup before their existing effect choreography. */
public final class MastersStyleRules {
	private MastersStyleRules() {}

	/** How this art chooses its target or ground anchor at its fixed active frame. Every new profile must decide explicitly. */
	public enum TargetPolicy {
		/** Re-query the committed cone at impact; an old last-swing victim cannot bypass its direction or reach. */
		ACTIVE_CONE,
		/** Keep the server-observed last-swing victim for a target/counter performer to validate itself. */
		STRING_TARGET,
		/** Capture Hailfall's observed cloud anchor and independent direct-priority right before its windup. */
		HAILFALL_RECEIPT,
		/** Capture Skyfall's observed-victim, nearest-cone or explicit-ground route before its windup. */
		SKYFALL_RECEIPT,
		/** Plant an untargeted field from release-time feet and accepted facing; never promote the observed victim. */
		GROUND_AHEAD
	}

	/**
	 * These profiles release once at {@code windup}; later physical cuts and independently released effects keep
	 * their performer's own scheduling. Landing-driven, travelling and held/channelled arts need their actual
	 * phase callbacks and must not be added here merely to give them a cosmetic pose.
	 */
	public record Style(int animation, String art, int windup, int recovery, TargetPolicy targets) {
		public Style { java.util.Objects.requireNonNull(targets, "An art must declare its target policy"); }
	}
	public static final List<Style> STYLES = List.of(
		new Style(3, "kindling_draw", 6, 12, TargetPolicy.ACTIVE_CONE),
		new Style(4, "frostbite", 6, 12, TargetPolicy.ACTIVE_CONE),
		new Style(5, "crackle", 4, 12, TargetPolicy.ACTIVE_CONE),
		new Style(6, "cutting_breeze", 4, 10, TargetPolicy.ACTIVE_CONE),
		new Style(7, "rockbreaker", 10, 18, TargetPolicy.ACTIVE_CONE),
		new Style(8, "thorn_lash", 6, 12, TargetPolicy.ACTIVE_CONE),
		new Style(9, "void_cut", 6, 12, TargetPolicy.ACTIVE_CONE),
		new Style(10, "star_needle", 4, 10, TargetPolicy.ACTIVE_CONE),
		new Style(11, "echo_cut", 6, 20, TargetPolicy.ACTIVE_CONE),
		new Style(12, "bloodletting", 6, 12, TargetPolicy.ACTIVE_CONE),
		new Style(13, "rising_cinders", 8, 16, TargetPolicy.ACTIVE_CONE),
		new Style(14, "blossom_fall", 8, 18, TargetPolicy.ACTIVE_CONE),
		new Style(15, "hailfall", 8, 16, TargetPolicy.HAILFALL_RECEIPT),
		new Style(16, "skyfall", 6, 16, TargetPolicy.SKYFALL_RECEIPT),
		new Style(17, "collapse", 8, 18, TargetPolicy.GROUND_AHEAD),
		new Style(18, "red_rain", 8, 16, TargetPolicy.GROUND_AHEAD),
		new Style(19, "crimson_moon", 10, 20, TargetPolicy.ACTIVE_CONE)
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
