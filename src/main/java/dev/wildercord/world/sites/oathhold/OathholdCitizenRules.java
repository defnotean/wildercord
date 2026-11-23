package dev.wildercord.world.sites.oathhold;

import java.util.List;

/**
 * Pure naming, role metadata, and contextual dialogue rules for the 750+ named {@link OathholdCitizen}
 * inhabitants of Oathhold.
 */
public final class OathholdCitizenRules {
	private OathholdCitizenRules() {}

	public enum Archetype {
		GUILD_RECEPTIONIST("guild_receptionist", "Guild Receptionist", false, true),
		GUILD_MASTER("guild_master", "Guild Master", false, true),
		BLACKSMITH("blacksmith", "Master Blacksmith", true, false),
		ALCHEMIST_HERBALIST("alchemist", "Apothecary Herbalist", true, false),
		BAKER_COOK("baker", "Master Baker", true, false),
		CLOTHIER_TAILOR("clothier", "Royal Clothier", true, false),
		FISHMONGER_SAILOR("fishmonger", "Harbor Fishmonger", true, false),
		SCHOLAR_SCRIBE("scholar", "Arcanum Scholar", true, false),
		JEWELER_BANKER("banker", "Exchange Bullionist", true, false),
		ARENA_PROMOTER("arena_promoter", "Colosseum Promoter", false, true),
		CARPENTER_MASON("artisan", "Master Builder", true, false),
		PRIEST_CLERIC("cleric", "Sanctuary Prelate", true, true),
		CITY_WATCHMAN("watchman", "Oathhold Watchman", false, false),
		STREET_SWEEPER("sweeper", "Street Sweeper", false, false),
		TOWNSFOLK("townsfolk", "Citizen", false, false),
		NOBLE_PATRON("noble", "Court Patrician", false, false);

		public final String id;
		public final String title;
		public final boolean merchant;
		public final boolean essential;

		Archetype(String id, String title, boolean merchant, boolean essential) {
			this.id = id;
			this.title = title;
			this.merchant = merchant;
			this.essential = essential;
		}

		public static Archetype ofId(String id) {
			for (Archetype a : values()) {
				if (a.id.equals(id)) return a;
			}
			return TOWNSFOLK;
		}

		public static Archetype byIndex(int idx) {
			Archetype[] all = values();
			return all[Math.floorMod(idx, all.length)];
		}
	}

	public record Identity(String fullName, String givenName, String title, Archetype archetype, int skinTone) {}

	private static final List<String> GIVEN_NAMES = List.of(
		"Aldric", "Bram", "Cedric", "Dorian", "Edric", "Faelan", "Garrick", "Halvor",
		"Ivor", "Jasper", "Kaelen", "Leoric", "Magnus", "Nolan", "Orin", "Percival",
		"Quentin", "Rowan", "Soren", "Theron", "Ulric", "Vance", "Warren", "Xander",
		"Yorick", "Zane", "Aeliana", "Brynna", "Clara", "Delia", "Elena", "Freya",
		"Giselle", "Helena", "Isolde", "Jocelyn", "Kira", "Lyra", "Mira", "Nadia",
		"Ophelia", "Petra", "Quinn", "Rosalind", "Sylvie", "Talia", "Una", "Valeria",
		"Wren", "Yvette", "Zora", "Alaric", "Cassian", "Corin", "Darius", "Evander",
		"Florian", "Gideon", "Hadrian", "Julian", "Lucian", "Matthias", "Ronan", "Tobias"
	);

	private static final List<String> SURNAMES = List.of(
		"Ashvale", "Blackwood", "Brightstone", "Copperfield", "Dawnforge", "Eldermere", "Fairwind", "Goldweaver",
		"Greymantle", "Hartford", "Ironbrow", "Keepwatch", "Larkspur", "Meadowbrook", "Nightingale", "Oakenshield",
		"Pendleton", "Quarryman", "Redcloak", "Silverton", "Stormharbor", "Thornwall", "Underhill", "Valemont",
		"Waverider", "Whitlock", "Ambercrest", "Barleycorn", "Briggs", "Caelum", "Dewdrop", "Emberfall",
		"Fletcher", "Galewatch", "Hearthstone", "Inkbinder", "Javelin", "Kestrel", "Lancaster", "Mariner",
		"Northgate", "Oakhaven", "Pebblebrook", "Quickstep", "Rosenwald", "Stonehelm", "Tidewater", "Windsong"
	);

	/** Deterministically derives a persistent unique identity for the citizen placed at {@code (lx, ly, lz)}. */
	public static Identity identityFor(int lx, int ly, int lz, Archetype archetype) {
		int h1 = mix(lx * 73856093 ^ ly * 19349663 ^ lz * 83492791 ^ (archetype.ordinal() * 265443576));
		int h2 = mix(lz * 73856093 ^ lx * 19349663 ^ (ly + 17) * 83492791 ^ (archetype.ordinal() * 1103515245));
		String given = GIVEN_NAMES.get(Math.floorMod(h1, GIVEN_NAMES.size()));
		String surname = SURNAMES.get(Math.floorMod(h2, SURNAMES.size()));
		String combined = given + " " + surname;
		int skinTone = Math.floorMod(h1 >>> 4, 4);
		return new Identity(combined + " — " + archetype.title, combined, archetype.title, archetype, skinTone);
	}

	public static String dialogue(Archetype archetype, OathholdQuestRules.Rank rank, boolean wanted, int lineIndex) {
		if (wanted) {
			return switch (archetype) {
				case CITY_WATCHMAN -> "Halt, lawbreaker! Sheathe your steel and settle your bounty in Emeralds, or the High Knights will detain you!";
				case PRIEST_CLERIC -> "Peace, wanderer. Sneak-speak to me with Emeralds in hand to pay restitution and clear your name with the Order.";
				default -> "Stay back! The Watch has posted a bounty notice on you across the square!";
			};
		}
		boolean highRank = rank.ordinal() >= OathholdQuestRules.Rank.B.ordinal();
		return switch (archetype) {
			case GUILD_RECEPTIONIST -> highRank
				? "Welcome back, Rank-" + rank.name() + " veteran! We have urgent A and S-tier commissions waiting behind the desk."
				: "Welcome to the Oathhold Adventurers' Guild! Check the Request Board for cleaning shifts, herb foraging, or Arena bouts.";
			case GUILD_MASTER -> "Rise from Copper Rank F through Orichalcum Rank S — every honest job strengthens the capital!";
			case ARENA_PROMOTER -> "Step onto the Colosseum sands in the West Ward! Accept an Arena Bout from the board, then speak to me to summon your foe.";
			case BLACKSMITH -> highRank
				? "An honor to hammer steel for a Rank-" + rank.name() + " hero! My finest ingots are discounted for Guild veterans."
				: "Need horseshoes hammered or steel traded? We keep the Great Forge blazing day and night.";
			case ALCHEMIST_HERBALIST -> "Dandelions, poppies, pink petals, and glow berries keep our infirmaries stocked — check the Guild for foraging jobs.";
			case BAKER_COOK -> "Fresh honey bread, hearty stew, and warm tarts straight from the ovens!";
			case CLOTHIER_TAILOR -> "Emerald surcoats and crisp linen tunics sewn right here off King's Way.";
			case FISHMONGER_SAILOR -> "Six galleons in East Harbor today! Fresh salmon and cod straight off the finger piers.";
			case SCHOLAR_SCRIBE -> "The Arcanum Dome holds star-charts older than the Outer Wall itself.";
			case JEWELER_BANKER -> "Every emerald and gold ingot in King's Exchange is weighed true against Royal Standards.";
			case CARPENTER_MASON -> "Oathhold stands two thousand blocks across, stone upon stone cleanly laid.";
			case PRIEST_CLERIC -> "May the Bell-Spire watch over your steps. Pay restitution here if you ever run afoul of city law.";
			case CITY_WATCHMAN -> highRank
				? "Hail, champion of Rank " + rank.name() + "! Keep the peace and the High Knights stand as your allies."
				: "The Oathbound High Knights patrol in peace — break no laws, strike no citizens, and their blades stay sheathed.";
			case STREET_SWEEPER -> "Sweep, scrub, rinse! Even the cleanest boulevard needs an apprentice broom when festival wagons roll through.";
			case NOBLE_PATRON -> (lineIndex & 1) == 0
				? "Nothing rivals a twilight stroll beside the Central Canal and Acropolis gardens!"
				: "Rumour in King's Square says the next S-Rank Colosseum bout will draw the whole Citadel.";
			case TOWNSFOLK -> switch (Math.floorMod(lineIndex, 3)) {
				case 0 -> "My cousin started at Copper Rank F sweeping cobwebs on the Canal Quay — now he duels in the Colosseum!";
				case 1 -> "Try sneaking up to talk shop or ask the Guild Receptionists for fresh daily requests.";
				default -> "Good day, traveller! Watch the wagons along the Imperial Causeway.";
			};
		};
	}

	private static int mix(int x) {
		x ^= x >>> 16;
		x *= 0x7feb352d;
		x ^= x >>> 15;
		x *= 0x846ca68b;
		x ^= x >>> 16;
		return x;
	}
}
