package dev.wildercord.wildlife;

import java.util.Locale;

/**
 * Pure domain rules for the four real Shrine Cats ({@code Yuki}, {@code Odie}, {@code Freya}, {@code Kuro}):
 * floor assignments inside the Cat Sanctuary dungeon, non-lethal guardian yield thresholds, taming rules,
 * sanctuary floor perks, and unique companion powers.
 */
public final class ShrineCatRules {
	private ShrineCatRules() {}

	public static final int ROLE_VILLAGE = 0;
	public static final int ROLE_GUARDIAN = 1;
	public static final int ROLE_CLEARED = 2;
	public static final int ROLE_TAMED = 3;

	public static final float YIELD_FRACTION = 0.22F;
	public static final double SANCTUARY_BUFF_RANGE = 42.0;

	public enum Variant {
		YUKI(
			0,
			"yuki",
			"Yuki",
			"Yuki, the Whispering Tabby",
			1,
			140.0F,
			6.5F,
			"Whisker Ward (Resistance I & steadied footing)",
			"Chilling gust swipes that slow attackers and steady her owner",
			false,
			true,
			false
		),
		ODIE(
			1,
			"odie",
			"Odie",
			"Odie, the Sunbeam Tabby",
			2,
			180.0F,
			7.5F,
			"Sunbeam Loaf (Regeneration I & Fire Resistance)",
			"Warm hearth purr that heals his owner and lobs solar sparks",
			false,
			true,
			false
		),
		FREYA(
			2,
			"freya",
			"Freya",
			"Freya, the Silver-Bell Sentinel",
			3,
			220.0F,
			8.5F,
			"Silver-Bell Chime (Absorption I & Haste I)",
			"Ringing pink-collar silver bell that grants Absorption and repels foes",
			true,
			false,
			false
		),
		KURO(
			3,
			"kuro",
			"Kuro",
			"Kuro, the Notched-Ear Shadow",
			4,
			280.0F,
			10.0F,
			"Night-Stalker Grace (Speed II, Strength I & Night Vision)",
			"Shadow-step pounce and agility cloak for his owner",
			false,
			false,
			true
		);

		public final int id;
		public final String key;
		public final String displayName;
		public final String title;
		public final int sanctuaryFloor;
		public final float trialHealth;
		public final float baseDamage;
		public final String dungeonPerk;
		public final String tamedPower;
		public final boolean bellCollar;
		public final boolean fluffyCoat;
		public final boolean notchedLeftEar;

		Variant(int id, String key, String displayName, String title, int sanctuaryFloor, float trialHealth,
				float baseDamage, String dungeonPerk, String tamedPower, boolean bellCollar,
				boolean fluffyCoat, boolean notchedLeftEar) {
			this.id = id;
			this.key = key;
			this.displayName = displayName;
			this.title = title;
			this.sanctuaryFloor = sanctuaryFloor;
			this.trialHealth = trialHealth;
			this.baseDamage = baseDamage;
			this.dungeonPerk = dungeonPerk;
			this.tamedPower = tamedPower;
			this.bellCollar = bellCollar;
			this.fluffyCoat = fluffyCoat;
			this.notchedLeftEar = notchedLeftEar;
		}

		public static Variant byId(int id) {
			Variant[] values = values();
			return values[Math.floorMod(id, values.length)];
		}

		public static Variant byKey(String key) {
			String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
			for (Variant v : values()) {
				if (v.key.equals(normalized)) {
					return v;
				}
			}
			return YUKI;
		}
	}

	/** Whether a guardian cat yields in its playful floor trial without dying. */
	public static boolean shouldYieldInTrial(float healthAfterHit, float maxHealth) {
		return maxHealth > 0 && healthAfterHit <= Math.max(4.0F, maxHealth * YIELD_FRACTION);
	}

	/** Whether {@code role} actively grants its floor buff to nearby sanctuary delvers. */
	public static boolean grantsDungeonPerk(int role) {
		return role == ROLE_CLEARED || role == ROLE_TAMED;
	}

	/** Whether a cat in {@code role} may be tamed right now. */
	public static boolean canTame(int role, boolean sanctuaryFinished, boolean hasCharm) {
		if (role == ROLE_TAMED || role == ROLE_GUARDIAN) {
			return false;
		}
		if (role == ROLE_CLEARED) {
			return sanctuaryFinished;
		}
		// Peaceful Sakura Village cats ask for either finishing a sanctuary or offering a Suzu Cat-Bell Charm.
		return sanctuaryFinished || hasCharm;
	}

	/** Trial combat damage tuned for guardian encounters. */
	public static float chillTrialDamage(Variant variant, boolean specialAttack) {
		return specialAttack ? variant.baseDamage + 2.5F : variant.baseDamage;
	}
}
