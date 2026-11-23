package dev.wildercord.ocean;

import java.util.List;
import java.util.Optional;

/**
 * Pure domain rules, combat math, gear parameters, Trench Anvil recipes, and structure constants
 * for the Wildercord Deep Ocean Expansion.
 *
 * <p>Kept strictly free of Minecraft engine imports so every formula can be unit-tested offline.
 */
public final class OceanRules {
	private OceanRules() {}

	// ---- Entity & Boss Stats ----
	public static final double BARNACLE_HULK_HP = 110.0;
	public static final double BARNACLE_HULK_ARMOR = 10.0;
	public static final double BARNACLE_HULK_DAMAGE = 12.0;
	public static final int BARNACLE_HULK_SLAM_COOLDOWN = 70;
	public static final double BARNACLE_HULK_SLAM_RADIUS = 3.2;

	public static final double SIREN_JELLY_HP = 44.0;
	public static final double SIREN_JELLY_DAMAGE = 6.0;
	public static final double SIREN_JELLY_PULSE_RADIUS = 5.5;
	public static final int SIREN_JELLY_PULSE_COOLDOWN = 80;

	public static final double CORAL_MAKO_HP = 56.0;
	public static final double CORAL_MAKO_DAMAGE = 9.0;
	public static final double CORAL_MAKO_LUNGE_SPEED = 1.35;
	public static final int CORAL_MAKO_LUNGE_COOLDOWN = 55;

	public static final double DREAD_NAUTILUS_HP = 85.0;
	public static final double DREAD_NAUTILUS_ARMOR = 14.0;
	public static final float DREAD_NAUTILUS_LASER_DAMAGE = 7.0f;
	public static final int DREAD_NAUTILUS_CHARGE_TICKS = 45;
	public static final int DREAD_NAUTILUS_FIRE_TICKS = 35;

	public static final double THALASSOR_MAX_HP = 2400.0;
	public static final double THALASSOR_ARMOR = 12.0;
	public static final double THALASSOR_MELEE_DAMAGE = 18.0;
	public static final float THALASSOR_BEAM_DAMAGE = 14.0f;
	public static final double THALASSOR_VORTEX_RADIUS = 18.0;
	public static final int THALASSOR_SEGMENT_COUNT = 14;
	public static final int THALASSOR_RESPAWN_TICKS = 12_000;

	public static final double LUMI_SEAHORSE_HP = 50.0;
	public static final double LUMI_SEAHORSE_SWIM_SPEED = 0.34;
	public static final double LUMI_SEAHORSE_DASH_IMPULSE = 1.45;
	public static final int LUMI_SEAHORSE_DASH_COOLDOWN = 45;

	public static final double PEARL_PUP_WILD_HP = 28.0;
	public static final double PEARL_PUP_TAMED_HP = 56.0;
	public static final int PEARL_PUP_SNIFF_INTERVAL = 200;
	public static final int PEARL_PUP_SNIFF_RADIUS = 36;

	public static final double MOON_WHALE_HP = 220.0;
	public static final double MOON_WHALE_SONG_RADIUS = 28.0;
	public static final int MOON_WHALE_SONG_INTERVAL = 320;
	public static final int MOON_WHALE_AMBERGRIS_INTERVAL = 2400;

	// ---- Brine-Merchant Professions ----
	public enum MerchantProfession {
		PEARL_JEWELER(0, "pearl_jeweler"),
		ABYSSAL_CARTOGRAPHER(1, "abyssal_cartographer"),
		KELP_ALCHEMIST(2, "kelp_alchemist");

		public final int id;
		public final String key;

		MerchantProfession(int id, String key) {
			this.id = id;
			this.key = key;
		}

		public static MerchantProfession fromId(int id) {
			for (MerchantProfession p : values()) {
				if (p.id == id) return p;
			}
			return PEARL_JEWELER;
		}
	}

	// ---- Abyssal Diving Suit ----
	public static final int SUIT_DURABILITY_MULT = 40;
	public static final int SUIT_ENCHANTABILITY = 18;
	public static final int HELM_DEFENSE = 4;
	public static final int CHEST_DEFENSE = 9;
	public static final int LEGS_DEFENSE = 7;
	public static final int FEET_DEFENSE = 4;
	public static final float SUIT_TOUGHNESS = 3.0f;
	public static final float SUIT_KNOCKBACK_RES = 0.1f;
	public static final double SUIT_GREAVES_WATER_EFFICIENCY = 0.75;
	public static final double SUIT_BOOTS_MINING_SPEED = 0.50;
	public static final int CHEST_OXYGEN_MAX = 2400;
	public static final int CHEST_OXYGEN_REFILL_RATE = 15;
	public static final float FULL_SUIT_ABYSSAL_DAMAGE_REDUCTION = 0.25f;

	// ---- Weapons, Relics & Cuisine ----
	public static final float HARPOON_MELEE_DAMAGE = 9.0f;
	public static final float HARPOON_ATTACK_SPEED = -2.2f;
	public static final float HARPOON_RANGED_DAMAGE = 12.0f;
	public static final float HARPOON_SUBMERGED_BONUS_DAMAGE = 16.0f;
	public static final int HARPOON_MAX_CHARGE_TICKS = 14;
	public static final double HARPOON_MAX_RANGE = 42.0;
	public static final double HARPOON_PULL_MAX_SPEED = 1.65;
	public static final double HARPOON_REEL_TARGET_SPEED = 1.35;

	public static final float GLAIVE_DAMAGE = 11.0f;
	public static final float GLAIVE_ATTACK_SPEED = -2.6f;
	public static final double GLAIVE_REACH_BONUS = 1.25;
	public static final double GLAIVE_SLASH_RANGE = 5.5;
	public static final double GLAIVE_SLASH_HALF_ANGLE_DEG = 37.5;
	public static final float GLAIVE_SLASH_DAMAGE = 14.0f;
	public static final float GLAIVE_SLASH_WET_DAMAGE = 18.0f;
	public static final int GLAIVE_COOLDOWN_TICKS = 40;

	public static final double LANTERN_REPEL_RADIUS = 10.0;

	public static final double CONCH_DOME_RADIUS = 4.5;
	public static final int CONCH_DOME_DURATION_TICKS = 240;
	public static final int CONCH_COOLDOWN_TICKS = 160;

	public static final int SASHIMI_NUTRITION = 7;
	public static final float SASHIMI_SATURATION = 0.8f;
	public static final int RAMEN_NUTRITION = 10;
	public static final float RAMEN_SATURATION = 1.0f;

	// ---- Structure Footprint Contract Bounds ----
	public static final int SANCTUARY_SIZE_XZ = 49;
	public static final int SANCTUARY_HEIGHT = 18;
	public static final int GALLEON_LENGTH = 46;
	public static final int GALLEON_HEIGHT = 28;
	public static final int GALLEON_WIDTH = 15;
	public static final int WATCHTOWER_SIZE_XZ = 15;
	public static final int WATCHTOWER_MIN_HEIGHT = 28;
	public static final int FORGE_SIZE_XZ = 27;
	public static final int FORGE_HEIGHT = 20;
	public static final int CITADEL_SIZE_XZ = 65;
	public static final int CITADEL_HEIGHT = 28;
	public static final int CITADEL_OBELISK_COUNT = 8;
	public static final int CITADEL_OBELISK_HEIGHT = 8;

	// ---- Trench Anvil Forging Recipes ----
	public record TrenchForgeRecipe(
		String outputItemId,
		String baseItemId,
		String primaryMaterialId,
		int primaryCount,
		String secondaryMaterialId,
		int secondaryCount
	) {}

	public static final List<TrenchForgeRecipe> TRENCH_RECIPES = List.of(
		new TrenchForgeRecipe("wildercord:abyssal_diving_helm", "minecraft:diamond_helmet", "wildercord:abyssal_iron_plate", 4, "wildercord:abyssal_pearl", 2),
		new TrenchForgeRecipe("wildercord:bathysphere_chestplate", "minecraft:diamond_chestplate", "wildercord:abyssal_iron_plate", 6, "wildercord:abyssal_pearl", 2),
		new TrenchForgeRecipe("wildercord:kelp_weave_greaves", "minecraft:diamond_leggings", "wildercord:abyssal_iron_plate", 5, "wildercord:abyssal_pearl", 1),
		new TrenchForgeRecipe("wildercord:trench_boots", "minecraft:diamond_boots", "wildercord:abyssal_iron_plate", 4, "wildercord:abyssal_pearl", 1),
		new TrenchForgeRecipe("wildercord:tide_cleaver_harpoon", "minecraft:trident", "wildercord:abyssal_iron_plate", 3, "wildercord:coral_edge_fang", 2),
		new TrenchForgeRecipe("wildercord:leviathan_glaive", "minecraft:netherite_ingot", "wildercord:coral_edge_fang", 2, "wildercord:leviathan_heart_pearl", 1),
		new TrenchForgeRecipe("wildercord:siren_pearl_lantern", "minecraft:lantern", "wildercord:voltaic_jelly_sac", 1, "wildercord:abyssal_pearl", 1),
		new TrenchForgeRecipe("wildercord:tidecaller_conch", "minecraft:nautilus_shell", "wildercord:star_barnacle_ambergris", 1, "wildercord:abyssal_pearl", 2)
	);

	public static Optional<TrenchForgeRecipe> matchTrenchRecipe(
		String baseItemId,
		String matAId,
		int countA,
		String matBId,
		int countB
	) {
		if (baseItemId == null) return Optional.empty();
		for (TrenchForgeRecipe r : TRENCH_RECIPES) {
			if (!r.baseItemId().equals(baseItemId)) continue;
			boolean direct = r.primaryMaterialId().equals(matAId) && countA >= r.primaryCount()
				&& r.secondaryMaterialId().equals(matBId) && countB >= r.secondaryCount();
			boolean swapped = r.primaryMaterialId().equals(matBId) && countB >= r.primaryCount()
				&& r.secondaryMaterialId().equals(matAId) && countA >= r.secondaryCount();
			if (direct || swapped) {
				return Optional.of(r);
			}
		}
		return Optional.empty();
	}

	// ---- Pure Gameplay & Combat Math ----

	/** Step oxygen tank reserve for one server tick: refills in air, drains slowly underwater. */
	public static int stepTankOxygen(int currentReserve, boolean hasHelm, boolean hasChestplate, boolean submerged, boolean hasWaterBreathing, long worldTick) {
		if (!hasChestplate) return 0;
		if (!submerged || hasWaterBreathing) {
			return Math.min(CHEST_OXYGEN_MAX, currentReserve + CHEST_OXYGEN_REFILL_RATE);
		}
		int drainInterval = hasHelm ? 2 : 1;
		if (currentReserve > 0 && (worldTick % drainInterval == 0)) {
			return currentReserve - 1;
		}
		return currentReserve;
	}

	/** Incoming abyssal damage multiplier (0.75f when wearing all 4 pieces of the Abyssal Diving Suit). */
	public static float abyssalDamageMultiplier(int wornPieces) {
		return wornPieces >= 4 ? (1.0f - FULL_SUIT_ABYSSAL_DAMAGE_REDUCTION) : 1.0f;
	}

	/** Clamped harpoon pull vector from thrower toward block/anchor or from target back toward thrower. */
	public static double[] computePullVector(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double maxSpeed) {
		double dx = toX - fromX;
		double dy = toY - fromY;
		double dz = toZ - fromZ;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1.0e-5) {
			return new double[] {0.0, 0.0, 0.0};
		}
		double scale = Math.min(maxSpeed, Math.max(0.55, len * 0.22)) / len;
		return new double[] {dx * scale, dy * scale + 0.08, dz * scale};
	}

	/** Checks whether a target position lies inside the Leviathan Glaive's horizontal hydro-slash arc. */
	public static boolean isInHydroSlashArc(
		double originX, double originY, double originZ,
		double lookX, double lookZ,
		double targetX, double targetY, double targetZ
	) {
		double dx = targetX - originX;
		double dy = Math.abs(targetY - originY);
		double dz = targetZ - originZ;
		if (dy > 3.0) return false;
		double horizDistSq = dx * dx + dz * dz;
		if (horizDistSq > GLAIVE_SLASH_RANGE * GLAIVE_SLASH_RANGE) return false;
		if (horizDistSq < 1.0e-4) return true;

		double lookLen = Math.hypot(lookX, lookZ);
		if (lookLen < 1.0e-5) return false;
		double dot = (dx * lookX + dz * lookZ) / (Math.sqrt(horizDistSq) * lookLen);
		return dot >= Math.cos(Math.toRadians(GLAIVE_SLASH_HALF_ANGLE_DEG));
	}

	/** Checks whether a position is inside the active Tidecaller Conch breathable air dome. */
	public static boolean isInsideConchDome(double centerX, double centerY, double centerZ, double px, double py, double pz) {
		double dx = px - centerX;
		double dy = py - centerY;
		double dz = pz - centerZ;
		return (dx * dx + dy * dy + dz * dz) <= (CONCH_DOME_RADIUS * CONCH_DOME_RADIUS);
	}

	/** Checks whether a swarmer mob within range should be gently pushed back by Siren-Pearl Lantern. */
	public static boolean shouldRepelSwarmer(double distSq, boolean isEligibleSwarmer) {
		return isEligibleSwarmer && distSq > 1.0e-4 && distSq <= (LANTERN_REPEL_RADIUS * LANTERN_REPEL_RADIUS);
	}

	/** Lumi-Seahorse 3D pitch swim dash velocity from yaw/pitch degrees. */
	public static double[] seahorseDashVelocity(float yawDeg, float pitchDeg) {
		double yawRad = Math.toRadians(yawDeg);
		double pitchRad = Math.toRadians(pitchDeg);
		double cosPitch = Math.cos(pitchRad);
		double vx = -Math.sin(yawRad) * cosPitch * LUMI_SEAHORSE_DASH_IMPULSE;
		double vy = -Math.sin(pitchRad) * LUMI_SEAHORSE_DASH_IMPULSE;
		double vz = Math.cos(yawRad) * cosPitch * LUMI_SEAHORSE_DASH_IMPULSE;
		return new double[] {vx, vy, vz};
	}

	/** Returns Thalassor boss phase (1 = Patrol >60%, 2 = Maelstrom 25..60%, 3 = Fury <=25%). */
	public static int thalassorPhase(double hp, double maxHp) {
		if (maxHp <= 0.0) return 1;
		double ratio = hp / maxHp;
		if (ratio <= 0.25) return 3;
		if (ratio <= 0.60) return 2;
		return 1;
	}

	/** Damage multiplier received by Dread-Nautilus Sentinel when struck from behind its coiled shell. */
	public static float nautilusShellDamageScale(boolean hitFromFrontAperture) {
		return hitFromFrontAperture ? 1.0f : 0.45f;
	}
}
