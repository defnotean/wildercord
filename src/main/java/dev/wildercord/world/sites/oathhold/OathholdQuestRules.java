package dev.wildercord.world.sites.oathhold;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Pure Adventurer's Guild progression rules, rank tiers, and the 30 curated Oathhold job requests
 * (cleaning chores, herb gathering, civic craft shifts, courier deliveries, pest/monster hunts,
 * and sanctioned Colosseum Arena bouts).
 */
public final class OathholdQuestRules {
	private OathholdQuestRules() {}

	public enum Rank {
		F(0, "Copper", 0),
		E(50, "Iron", 0),
		D(150, "Bronze", 1),
		C(350, "Silver", 1),
		B(700, "Gold", 2),
		A(1200, "Mythril", 2),
		S(2000, "Orichalcum", 3);

		public final int minPoints;
		public final String sealMaterial;
		public final int tradeDiscount;

		Rank(int minPoints, String sealMaterial, int tradeDiscount) {
			this.minPoints = minPoints;
			this.sealMaterial = sealMaterial;
			this.tradeDiscount = tradeDiscount;
		}

		public static Rank ofPoints(int points) {
			Rank current = F;
			for (Rank r : values()) {
				if (points >= r.minPoints) {
					current = r;
				}
			}
			return current;
		}

		public Rank next() {
			Rank[] all = values();
			int idx = ordinal() + 1;
			return idx < all.length ? all[idx] : null;
		}

		public int tradeDiscountEmeralds(int basePrice) {
			if (basePrice <= 1) return basePrice;
			return Math.max(1, basePrice - tradeDiscount);
		}
	}

	public enum JobCategory {
		CLEANING("cleaning", "Street & Hall Cleaning"),
		HERB_GATHERING("herb_gathering", "Herb & Reagent Foraging"),
		WORK_SHIFT("work_shift", "Artisan Work Shift"),
		CARGO_DELIVERY("cargo_delivery", "District Courier Delivery"),
		MONSTER_HUNT("monster_hunt", "Monster Subjugation"),
		ARENA_BOUT("arena_bout", "Colosseum Arena Bout");

		public final String id;
		public final String label;

		JobCategory(String id, String label) {
			this.id = id;
			this.label = label;
		}

		public static JobCategory of(String id) {
			for (JobCategory c : values()) {
				if (c.id.equals(id)) return c;
			}
			return CLEANING;
		}
	}

	public record JobSpec(
		String id,
		String title,
		String client,
		String description,
		JobCategory category,
		Rank minRank,
		String targetKey,
		int needed,
		int emeralds,
		int rankPoints,
		String bonusItem
	) {}

	public static final List<JobSpec> JOBS = List.of(
		// --- Cleaning (5) ---
		new JobSpec("clean_canal_cobwebs", "Sweep Canal Promenade Cobwebs", "Harbor Master Vane",
			"Clear sticky cobwebs off stone bollards and shop eaves.", JobCategory.CLEANING, Rank.F,
			"minecraft:cobweb", 6, 6, 18, "hearty_stew"),
		new JobSpec("clean_bazaar_moss", "Scrub Bazaar Stall Mud & Moss", "Bazaar Guild Steward",
			"Brush wet moss patches from the busy market walkways.", JobCategory.CLEANING, Rank.F,
			"minecraft:moss_carpet", 6, 6, 18, "forager_soup"),
		new JobSpec("clean_forge_soot", "Clear Forge Alley Soot", "Foreman Torin",
			"Sweep ash webs and grime outside the Great Forge chimneys.", JobCategory.CLEANING, Rank.E,
			"minecraft:cobweb", 7, 8, 24, "wildercord:blank_rune"),
		new JobSpec("clean_acropolis_gravel", "Tidy Acropolis Garden Turf", "Court Gardener Mira",
			"Remove overgrown moss mats crowding the royal rose beds.", JobCategory.CLEANING, Rank.E,
			"minecraft:moss_carpet", 7, 8, 24, "minecraft:golden_carrot"),
		new JobSpec("clean_harbor_warehouse", "Purge Harbor Warehouse Grime", "Quartermaster Briggs",
			"Clear old cellar cobwebs behind the fishmonger quays.", JobCategory.CLEANING, Rank.D,
			"minecraft:cobweb", 8, 11, 32, "wildercord:blank_rune"),

		// --- Herb Gathering (6) ---
		new JobSpec("herb_dandelions", "Gather Dandelions for Salves", "Apothecary Lyra",
			"Deliver fresh yellow dandelions for field dressing poultices.", JobCategory.HERB_GATHERING, Rank.F,
			"minecraft:dandelion", 8, 6, 20, "hearty_stew"),
		new JobSpec("herb_poppies", "Collect Red Poppies", "Abbey Infirmarian",
			"Gather crimson poppies to brew restorative tonics.", JobCategory.HERB_GATHERING, Rank.F,
			"minecraft:poppy", 8, 6, 20, "minecraft:bread"),
		new JobSpec("herb_pink_petals", "Pick Acropolis Pink Petals", "Cherry Parterre Keeper",
			"Supply fragrant blossom petals for sanctuary incense.", JobCategory.HERB_GATHERING, Rank.E,
			"minecraft:pink_petals", 10, 9, 28, "wildercord:blank_rune"),
		new JobSpec("herb_sweet_berries", "Harvest Sweet Berries for the Bakery", "Master Baker Corin",
			"Fill pantry baskets with fresh tart berries.", JobCategory.HERB_GATHERING, Rank.E,
			"minecraft:sweet_berries", 12, 9, 28, "honeyed_ham"),
		new JobSpec("herb_glow_berries", "Forage Luminous Glow Berries", "Arcanum Alchemist",
			"Collect cave glow berries to distill lantern ink.", JobCategory.HERB_GATHERING, Rank.D,
			"minecraft:glow_berries", 10, 12, 36, "wildercord:mana_crystal"),
		new JobSpec("herb_moonreed_floss", "Gather Rare Moonreed Floss", "High Apothecary Orin",
			"Harvest delicate silver reed floss from the luminous wetlands.", JobCategory.HERB_GATHERING, Rank.C,
			"wildercord:moonreed_floss", 6, 18, 55, "wildercord:aura_shard"),

		// --- Work Shifts (5) ---
		new JobSpec("shift_horseshoes", "Hammer Horseshoes at the Forge", "Stablemaster Garrick",
			"Work a hammering shift at any city anvil.", JobCategory.WORK_SHIFT, Rank.F,
			"anvil", 6, 7, 22, "minecraft:iron_ingot"),
		new JobSpec("shift_tavern_hearths", "Stoke Tavern Kitchen Hearths", "Innkeeper Bram",
			"Tend roaring campfires, smokers, or blast furnaces for dinner rush.", JobCategory.WORK_SHIFT, Rank.F,
			"hearth", 6, 7, 22, "hearty_stew"),
		new JobSpec("shift_star_charts", "Copy Star Charts in the Arcanum", "Arch-Scribe Caelum",
			"Draft celestial margins at any lectern or cartography table.", JobCategory.WORK_SHIFT, Rank.E,
			"lectern", 6, 9, 28, "wildercord:blank_rune"),
		new JobSpec("shift_temper_blades", "Temper Guard Blades", "Ordnance Master",
			"Inspect and whet guard weapons at a smithing table.", JobCategory.WORK_SHIFT, Rank.D,
			"smithing", 8, 12, 38, "wildercord:mana_crystal"),
		new JobSpec("shift_liturgies", "Transcribe High Cathedral Liturgies", "Prelate Aldous",
			"Copy covenant folios onto lectern parchment in the sanctuary.", JobCategory.WORK_SHIFT, Rank.C,
			"lectern", 10, 16, 52, "wildercord:aura_shard"),

		// --- Cargo & Courier Deliveries (4) ---
		new JobSpec("deliver_harbor_ledger", "Deliver Guild Ledger to East Harbor", "Guild Bursar",
			"Carry sealed harbor manifests to East Harbor Quay (report to a citizen there).", JobCategory.CARGO_DELIVERY, Rank.E,
			"harbor", 1, 10, 30, "wildercord:blank_rune"),
		new JobSpec("deliver_colosseum_rations", "Carry Ration Manifest to West Colosseum", "Quartermaster",
			"Bring supply writs to the West Tournament Colosseum concourse.", JobCategory.CARGO_DELIVERY, Rank.E,
			"colosseum", 1, 10, 30, "hunters_skewer"),
		new JobSpec("deliver_exchange_bullion", "Courier Bullion Seal to South Exchange", "Vault Clerk",
			"Escort mint stamps south to King's Exchange Square.", JobCategory.CARGO_DELIVERY, Rank.D,
			"exchange", 1, 14, 42, "minecraft:gold_ingot"),
		new JobSpec("deliver_abbey_incense", "Bring Incense Writ to North Abbey", "Guild Master",
			"Deliver ceremonial decrees north to High Sovereign Abbey.", JobCategory.CARGO_DELIVERY, Rank.D,
			"abbey", 1, 14, 42, "wildercord:mana_crystal"),

		// --- Monster Subjugation (5) ---
		new JobSpec("hunt_cellar_silverfish", "Cull Cellar Silverfish", "Grain Cellar Warden",
			"Exterminate burrowing silverfish spotted near the district cellars.", JobCategory.MONSTER_HUNT, Rank.F,
			"minecraft:silverfish", 5, 8, 25, "hearty_stew"),
		new JobSpec("hunt_canal_drowned", "Clear Canal Drowned", "Canal Watch Captain",
			"Subjugate drowned prowlers slipping up through the storm drains.", JobCategory.MONSTER_HUNT, Rank.E,
			"minecraft:drowned", 4, 11, 35, "wildercord:blank_rune"),
		new JobSpec("hunt_night_zombies", "Subjugate Gatehouse Zombies", "Outer Wall Sergeant",
			"Drive back undead stragglers roaming outside the walls.", JobCategory.MONSTER_HUNT, Rank.D,
			"minecraft:zombie", 6, 14, 45, "duelists_broth"),
		new JobSpec("hunt_outskirts_skeletons", "Hunt Outskirts Skeletons", "Ranger Lieutenant",
			"Thin the skeletal archers gathering beyond the outer moat.", JobCategory.MONSTER_HUNT, Rank.C,
			"minecraft:skeleton", 8, 18, 60, "wildercord:mana_crystal"),
		new JobSpec("hunt_dungeon_guardian", "Slay a Dungeon Guardian", "Grand Guild Master",
			"Bring down any slumbering dungeon boss in the wilds.", JobCategory.MONSTER_HUNT, Rank.A,
			"dungeon_boss", 1, 32, 140, "wildercord:aura_shard"),

		// --- Colosseum Arena Bouts (5) ---
		new JobSpec("arena_fallen_squire", "Colosseum Trial: Fallen Squire", "Arena Promoter Cassian",
			"Enter the West Colosseum sand and defeat a summoned Fallen Knight challenger.", JobCategory.ARENA_BOUT, Rank.E,
			"fallen_knight", 1, 14, 45, "wildercord:blank_rune"),
		new JobSpec("arena_halberdier", "Colosseum Rank-C Bout: Oathbound Halberdier", "Arena Promoter Cassian",
			"Best a sanctioned Oathbound Halberdier before the cheering stands.", JobCategory.ARENA_BOUT, Rank.C,
			"oathbound_halberdier", 1, 24, 90, "wildercord:mana_crystal"),
		new JobSpec("arena_blade_champion", "Colosseum Rank-B Bout: Blade-Champion", "Arena Promoter Cassian",
			"Overcome an Oathbound Blade-Champion in single sanctioned combat.", JobCategory.ARENA_BOUT, Rank.B,
			"oathbound_blade_champion", 1, 32, 135, "wildercord:aura_shard"),
		new JobSpec("arena_bastion", "Colosseum Rank-A Bout: Oathbound Bastion", "Arena Promoter Cassian",
			"Shatter an Oathbound Bastion's Sun-Anvil guard before the crowd.", JobCategory.ARENA_BOUT, Rank.A,
			"oathbound_bastion", 1, 40, 180, "wildercord:aura_shard"),
		new JobSpec("arena_banner_marshal", "Grand Championship: Banner-Marshal", "Grand Sovereign Marshal",
			"Claim the S-Rank laurel by defeating an Oathbound Banner-Marshal in the Arena.", JobCategory.ARENA_BOUT, Rank.S,
			"oathbound_banner_marshal", 1, 64, 300, "wildercord:aura_shard")
	);

	private static final Map<String, JobSpec> BY_ID = new LinkedHashMap<>();

	static {
		for (JobSpec job : JOBS) {
			BY_ID.put(job.id(), job);
		}
	}

	public static JobSpec byId(String id) {
		return BY_ID.get(id);
	}

	/**
	 * Deterministically selects 8 distinct Guild Requests for a player on {@code day} at {@code rank},
	 * always guaranteeing at least one Cleaning, Herb Gathering, Work Shift, Monster Hunt, and Arena Bout.
	 */
	public static List<JobSpec> boardSelection(UUID playerId, long day, Rank rank) {
		int maxOrdinal = Math.min(Rank.S.ordinal(), rank.ordinal() + 1);
		List<JobSpec> eligible = new ArrayList<>();
		for (JobSpec job : JOBS) {
			if (job.minRank().ordinal() <= maxOrdinal) {
				eligible.add(job);
			}
		}
		Random rng = new Random(playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits() ^ (day * 0x9E3779B97F4A7C15L) ^ (rank.ordinal() * 31L));
		List<JobSpec> chosen = new ArrayList<>(8);

		for (JobCategory required : List.of(
			JobCategory.CLEANING, JobCategory.HERB_GATHERING, JobCategory.WORK_SHIFT,
			JobCategory.MONSTER_HUNT, JobCategory.ARENA_BOUT
		)) {
			List<JobSpec> pool = new ArrayList<>();
			for (JobSpec job : eligible) {
				if (job.category() == required && !chosen.contains(job)) {
					pool.add(job);
				}
			}
			if (!pool.isEmpty()) {
				chosen.add(pool.get(rng.nextInt(pool.size())));
			}
		}

		List<JobSpec> remaining = new ArrayList<>();
		for (JobSpec job : eligible) {
			if (!chosen.contains(job)) {
				remaining.add(job);
			}
		}
		while (chosen.size() < 8 && !remaining.isEmpty()) {
			chosen.add(remaining.remove(rng.nextInt(remaining.size())));
		}
		return List.copyOf(chosen);
	}
}
