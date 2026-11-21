package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Populates the 2,048x2,048 Oathhold megacity with 750+ unique named {@link OathholdCitizen} inhabitants
 * across all 16 archetypes, 7 Adventurer's Guild Request Boards, and outer-ward Knight patrol squads.
 * Runs last in {@link OathholdSectorPiece} after all structures exist so every spawn is checked for wall clearance.
 */
public final class OathholdPopulation {
	private OathholdPopulation() {}

	public record PlannedSpawn(int lx, int ly, int lz, OathholdCitizenRules.Archetype archetype, int tether, float yaw) {}

	public static void build(OathholdCanvas c) {
		placeGuildBoards(c);
		emitCityCitizens(c.clipMinLx(), c.clipMaxLx(), c.clipMinLz(), c.clipMaxLz(),
			p -> c.spawnCitizen(p.lx(), p.ly(), p.lz(), p.archetype(), p.tether(), p.yaw()));
		spawnBoulevardKnightPatrols(c);
	}

	/** Dry-run count of all deterministic citizens planned across a full 2,048x2,048 city. */
	public static int countPlannedCitizens() {
		final int[] count = {0};
		emitCityCitizens(-960, 960, -960, 960, p -> count[0]++);
		return count[0];
	}

	/** Dry-run list of sampled/all planned citizens for unit contract verification. */
	public static List<PlannedSpawn> sampleAllPlannedCitizens() {
		List<PlannedSpawn> all = new ArrayList<>(1024);
		emitCityCitizens(-960, 960, -960, 960, all::add);
		return all;
	}

	private static void placeGuildBoards(OathholdCanvas c) {
		// Adventurers' Guild HQ (Copper Guildhall interior & plaza entrance)
		c.guildBoard(136, 1, 50, Direction.SOUTH);
		c.guildBoard(136, 1, 84, Direction.SOUTH);
		// Central Fountain Plaza
		c.guildBoard(-12, 2, 166, Direction.EAST);
		// Four Cardinal Outer Landmarks
		c.guildBoard(-544, 1, -6, Direction.EAST);
		c.guildBoard(537, 2, -2, Direction.EAST);
		c.guildBoard(0, 1, 540, Direction.SOUTH);
		c.guildBoard(0, 1, -538, Direction.NORTH);
	}

	private static void spawnBoulevardKnightPatrols(OathholdCanvas c) {
		for (int z : new int[]{-760, -360, 360, 760}) {
			c.spawnMob(-4, 1, z, AuraWorld.OATHBOUND_HALBERDIER, z > 0 ? 0.0F : 180.0F, true);
			c.spawnMob(4, 1, z + 8, AuraWorld.OATHBOUND_BASTION, z > 0 ? 180.0F : 0.0F, true);
		}
		for (int x : new int[]{-760, -360, 360, 760}) {
			c.spawnMob(x, 1, -4, AuraWorld.OATHBOUND_BLADE_CHAMPION, x > 0 ? -90.0F : 90.0F, true);
			c.spawnMob(x + 8, 1, 4, AuraWorld.OATHBOUND_HALBERDIER, x > 0 ? 90.0F : -90.0F, true);
		}
	}

	private static void emitCityCitizens(int minX, int maxX, int minZ, int maxZ, Consumer<PlannedSpawn> out) {
		int ay = OathholdOrigin.ACROPOLIS_Y;

		// 1. Fixed Inner Citadel, Guild HQ, Acropolis & Four Outer Landmark rosters (~150 named inhabitants)
		List<PlannedSpawn> landmarks = List.of(
			// Adventurers' Guild HQ (Copper Guildhall [118..154, 46..80])
			new PlannedSpawn(136, 1, 53, OathholdCitizenRules.Archetype.GUILD_MASTER, 8, 0.0F),
			new PlannedSpawn(126, 1, 52, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 4, 0.0F),
			new PlannedSpawn(136, 1, 52, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 4, 0.0F),
			new PlannedSpawn(146, 1, 52, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 4, 0.0F),
			new PlannedSpawn(126, 1, 74, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 4, 180.0F),
			new PlannedSpawn(146, 1, 74, OathholdCitizenRules.Archetype.BLACKSMITH, 5, 180.0F),
			new PlannedSpawn(136, 1, 74, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 5, 180.0F),
			new PlannedSpawn(130, 11, 49, OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE, 6, 0.0F),
			new PlannedSpawn(142, 11, 75, OathholdCitizenRules.Archetype.NOBLE_PATRON, 6, 180.0F),
			new PlannedSpawn(124, 1, 63, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 6, -90.0F),
			new PlannedSpawn(148, 1, 63, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, 90.0F),
			new PlannedSpawn(132, 1, 64, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, -45.0F),
			new PlannedSpawn(140, 1, 61, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, 45.0F),
			new PlannedSpawn(134, 1, 86, OathholdCitizenRules.Archetype.STREET_SWEEPER, 8, 0.0F),
			new PlannedSpawn(120, 1, 86, OathholdCitizenRules.Archetype.TOWNSFOLK, 10, 90.0F),
			new PlannedSpawn(148, 1, 86, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 10, -90.0F),

			// Fountain Plaza
			new PlannedSpawn(-6, 2, 176, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 6, 90.0F),
			new PlannedSpawn(-4, 1, 196, OathholdCitizenRules.Archetype.TOWNSFOLK, 10, 0.0F),
			new PlannedSpawn(4, 1, 164, OathholdCitizenRules.Archetype.TOWNSFOLK, 10, 180.0F),
			new PlannedSpawn(-10, 1, 172, OathholdCitizenRules.Archetype.STREET_SWEEPER, 10, 90.0F),
			new PlannedSpawn(10, 1, 182, OathholdCitizenRules.Archetype.NOBLE_PATRON, 10, -90.0F),
			new PlannedSpawn(-18, 1, 178, OathholdCitizenRules.Archetype.BAKER_COOK, 4, 90.0F),
			new PlannedSpawn(18, 1, 178, OathholdCitizenRules.Archetype.CLOTHIER_TAILOR, 4, -90.0F),

			// Central Canal Promenade & Bridges
			new PlannedSpawn(-14, 1, 70, OathholdCitizenRules.Archetype.TOWNSFOLK, 10, 90.0F),
			new PlannedSpawn(14, 1, 100, OathholdCitizenRules.Archetype.NOBLE_PATRON, 10, -90.0F),
			new PlannedSpawn(-14, 1, 36, OathholdCitizenRules.Archetype.STREET_SWEEPER, 10, 90.0F),
			new PlannedSpawn(14, 1, 14, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 10, -90.0F),
			new PlannedSpawn(-16, 1, 134, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 10, 0.0F),
			new PlannedSpawn(16, 1, 44, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 10, -90.0F),
			new PlannedSpawn(-8, 3, 56, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, 90.0F),
			new PlannedSpawn(8, 3, 116, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, -90.0F),

			// Grand Market Hall & Bazaar Square
			new PlannedSpawn(-102, 1, 60, OathholdCitizenRules.Archetype.BAKER_COOK, 6, 0.0F),
			new PlannedSpawn(-90, 1, 60, OathholdCitizenRules.Archetype.CLOTHIER_TAILOR, 6, 0.0F),
			new PlannedSpawn(-78, 1, 60, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 6, 0.0F),
			new PlannedSpawn(-66, 1, 60, OathholdCitizenRules.Archetype.CARPENTER_MASON, 6, 0.0F),
			new PlannedSpawn(-106, 1, 100, OathholdCitizenRules.Archetype.BAKER_COOK, 5, 180.0F),
			new PlannedSpawn(-88, 1, 100, OathholdCitizenRules.Archetype.CLOTHIER_TAILOR, 5, 180.0F),
			new PlannedSpawn(-70, 1, 100, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 5, 180.0F),
			new PlannedSpawn(-54, 1, 100, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 5, 180.0F),
			new PlannedSpawn(-106, 1, 128, OathholdCitizenRules.Archetype.JEWELER_BANKER, 5, 180.0F),
			new PlannedSpawn(-88, 1, 128, OathholdCitizenRules.Archetype.CARPENTER_MASON, 5, 180.0F),
			new PlannedSpawn(-70, 1, 128, OathholdCitizenRules.Archetype.BAKER_COOK, 5, 180.0F),
			new PlannedSpawn(-54, 1, 128, OathholdCitizenRules.Archetype.CLOTHIER_TAILOR, 5, 180.0F),
			new PlannedSpawn(-98, 1, 114, OathholdCitizenRules.Archetype.STREET_SWEEPER, 10, 0.0F),
			new PlannedSpawn(-76, 1, 114, OathholdCitizenRules.Archetype.TOWNSFOLK, 10, 45.0F),
			new PlannedSpawn(-60, 1, 114, OathholdCitizenRules.Archetype.NOBLE_PATRON, 10, -45.0F),
			new PlannedSpawn(-46, 1, 122, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 10, -90.0F),

			// Great Forge & Blacksmiths' Yard
			new PlannedSpawn(-160, 1, 72, OathholdCitizenRules.Archetype.BLACKSMITH, 6, -90.0F),
			new PlannedSpawn(-148, 1, 68, OathholdCitizenRules.Archetype.BLACKSMITH, 6, 90.0F),
			new PlannedSpawn(-154, 1, 40, OathholdCitizenRules.Archetype.BLACKSMITH, 6, 0.0F),
			new PlannedSpawn(-144, 1, 40, OathholdCitizenRules.Archetype.CARPENTER_MASON, 6, 180.0F),
			new PlannedSpawn(-132, 1, 72, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 8, -90.0F),
			new PlannedSpawn(-166, 1, 48, OathholdCitizenRules.Archetype.STREET_SWEEPER, 8, 90.0F),

			// Grand Inn & Beer Garden
			new PlannedSpawn(60, 1, 120, OathholdCitizenRules.Archetype.BAKER_COOK, 6, 0.0F),
			new PlannedSpawn(56, 1, 108, OathholdCitizenRules.Archetype.TOWNSFOLK, 6, 180.0F),
			new PlannedSpawn(52, 1, 141, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, 0.0F),
			new PlannedSpawn(60, 1, 142, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, 180.0F),
			new PlannedSpawn(46, 6, 112, OathholdCitizenRules.Archetype.NOBLE_PATRON, 6, 0.0F),
			new PlannedSpawn(64, 6, 114, OathholdCitizenRules.Archetype.TOWNSFOLK, 6, 180.0F),

			// Upper Acropolis Great Hall, Esplanade, Barracks, Arcanum & Cathedral
			new PlannedSpawn(0, ay + 1, -68, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 8, 0.0F),
			new PlannedSpawn(-8, ay + 1, -58, OathholdCitizenRules.Archetype.STREET_SWEEPER, 8, 90.0F),
			new PlannedSpawn(8, ay + 1, -58, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, -90.0F),
			new PlannedSpawn(-34, ay + 1, -56, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 8, 0.0F),
			new PlannedSpawn(34, ay + 1, -56, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, 0.0F),
			new PlannedSpawn(76, ay + 1, -118, OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE, 8, 0.0F),
			new PlannedSpawn(84, ay + 1, -130, OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE, 8, 180.0F),
			new PlannedSpawn(74, ay + 1, -66, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 8, 90.0F),
			new PlannedSpawn(86, ay + 1, -60, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, -90.0F),
			new PlannedSpawn(146, ay + 1, -175, OathholdCitizenRules.Archetype.PRIEST_CLERIC, 6, 0.0F),
			new PlannedSpawn(154, ay + 1, -112, OathholdCitizenRules.Archetype.PRIEST_CLERIC, 6, 180.0F),
			new PlannedSpawn(148, ay + 1, -124, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, 180.0F),
			new PlannedSpawn(-118, ay + 1, -124, OathholdCitizenRules.Archetype.BLACKSMITH, 8, 0.0F),
			new PlannedSpawn(-142, ay + 1, -124, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 8, 90.0F),
			new PlannedSpawn(-84, ay + 1, -60, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 8, 0.0F),

			// West Colosseum & Concourse (note coords strictly outside wall radius rOut = 60 at [-545..-540] or inside sand [-595..-580]!)
			new PlannedSpawn(-545, 1, 0, OathholdCitizenRules.Archetype.ARENA_PROMOTER, 5, 90.0F),
			new PlannedSpawn(-584, 1, 0, OathholdCitizenRules.Archetype.ARENA_PROMOTER, 6, -90.0F),
			new PlannedSpawn(-550, 1, -24, OathholdCitizenRules.Archetype.BAKER_COOK, 5, 90.0F),
			new PlannedSpawn(-550, 1, 24, OathholdCitizenRules.Archetype.BLACKSMITH, 5, 90.0F),
			new PlannedSpawn(-542, 1, 14, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, 180.0F),
			new PlannedSpawn(-542, 1, -14, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, 0.0F),
			new PlannedSpawn(-664, 1, -46, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, 45.0F),
			new PlannedSpawn(-556, 1, -46, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 8, -45.0F),

			// East Harbor Quay & Piers
			new PlannedSpawn(539, 2, 0, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 6, 90.0F),
			new PlannedSpawn(545, 2, -92, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 5, 90.0F),
			new PlannedSpawn(545, 2, -62, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 5, 90.0F),
			new PlannedSpawn(545, 2, -32, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 5, 90.0F),
			new PlannedSpawn(545, 2, 32, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 5, 90.0F),
			new PlannedSpawn(545, 2, 62, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 5, 90.0F),
			new PlannedSpawn(545, 2, 92, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 5, 90.0F),
			new PlannedSpawn(538, 2, 14, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, 90.0F),
			new PlannedSpawn(538, 2, -14, OathholdCitizenRules.Archetype.TOWNSFOLK, 8, 90.0F),
			new PlannedSpawn(552, 2, 20, OathholdCitizenRules.Archetype.STREET_SWEEPER, 10, 0.0F),
			new PlannedSpawn(586, 2, 0, OathholdCitizenRules.Archetype.CARPENTER_MASON, 10, -90.0F),
			new PlannedSpawn(624, 2, 120, OathholdCitizenRules.Archetype.FISHMONGER_SAILOR, 10, 90.0F),
			new PlannedSpawn(624, 2, -120, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 10, 90.0F),

			// South King's Square & Imperial Exchange
			new PlannedSpawn(-24, 1, 556, OathholdCitizenRules.Archetype.JEWELER_BANKER, 6, 0.0F),
			new PlannedSpawn(0, 1, 570, OathholdCitizenRules.Archetype.JEWELER_BANKER, 6, 180.0F),
			new PlannedSpawn(24, 1, 556, OathholdCitizenRules.Archetype.JEWELER_BANKER, 6, 0.0F),
			new PlannedSpawn(-40, 1, 570, OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE, 6, 180.0F),
			new PlannedSpawn(-12, 1, 612, OathholdCitizenRules.Archetype.NOBLE_PATRON, 10, 45.0F),
			new PlannedSpawn(12, 1, 644, OathholdCitizenRules.Archetype.CLOTHIER_TAILOR, 10, -135.0F),
			new PlannedSpawn(-26, 1, 614, OathholdCitizenRules.Archetype.BAKER_COOK, 5, 0.0F),
			new PlannedSpawn(26, 1, 614, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 5, 0.0F),
			new PlannedSpawn(-26, 1, 650, OathholdCitizenRules.Archetype.CARPENTER_MASON, 5, 180.0F),
			new PlannedSpawn(26, 1, 650, OathholdCitizenRules.Archetype.STREET_SWEEPER, 8, 180.0F),
			new PlannedSpawn(0, 1, 594, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 8, 180.0F),

			// North Sovereign Abbey & Cloister Garth
			new PlannedSpawn(-4, 1, -654, OathholdCitizenRules.Archetype.PRIEST_CLERIC, 6, 0.0F),
			new PlannedSpawn(6, 1, -634, OathholdCitizenRules.Archetype.PRIEST_CLERIC, 6, 180.0F),
			new PlannedSpawn(-12, 1, -640, OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE, 6, 90.0F),
			new PlannedSpawn(-10, 1, -582, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 8, 90.0F),
			new PlannedSpawn(10, 1, -582, OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST, 8, -90.0F),
			new PlannedSpawn(0, 1, -544, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 8, 0.0F),
			new PlannedSpawn(12, 1, -568, OathholdCitizenRules.Archetype.NOBLE_PATRON, 8, 180.0F)
		);
		for (PlannedSpawn s : landmarks) {
			if (s.lx() >= minX && s.lx() <= maxX && s.lz() >= minZ && s.lz() <= maxZ) {
				out.accept(s);
			}
		}

		// 2. Lower City non-landmark Townhouse Blocks (~24 blocks -> ~48 citizens)
		for (int gx = -192; gx <= 160; gx += 32) {
			for (int gz = -32; gz <= 160; gz += 32) {
				int x0 = gx + 3, x1 = gx + 29, z0 = gz + 3, z1 = gz + 29;
				if (x1 < minX || x0 > maxX || z1 < minZ || z0 > maxZ) continue;
				if (OathholdInnerCore.overlapsInnerLandmark(x0, z0, x1, z1)) continue;
				int h = OathholdCanvas.hash(gx, 11, gz);
				OathholdCitizenRules.Archetype shopRole = switch (h % 5) {
					case 0 -> OathholdCitizenRules.Archetype.BAKER_COOK;
					case 1 -> OathholdCitizenRules.Archetype.CLOTHIER_TAILOR;
					case 2 -> OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST;
					case 3 -> OathholdCitizenRules.Archetype.BLACKSMITH;
					default -> OathholdCitizenRules.Archetype.CARPENTER_MASON;
				};
				out.accept(new PlannedSpawn(gx + 10, 1, gz + 16, shopRole, 8, 0.0F));
				out.accept(new PlannedSpawn(gx + 22, 1, gz + 15, (h & 1) == 0
					? OathholdCitizenRules.Archetype.TOWNSFOLK : OathholdCitizenRules.Archetype.STREET_SWEEPER, 10, 180.0F));
			}
		}

		// 3. Outer Imperial Causeway Sidewalks (N/S & E/W, step 64)
		for (int c = -896; c <= 896; c += 64) {
			int ac = Math.abs(c);
			if (ac < 268 || (ac >= 530 && ac <= 684)) continue;
			if (minX <= 12 && maxX >= -12 && c >= minZ && c <= maxZ) {
				int h = OathholdCanvas.hash(0, 31, c);
				out.accept(new PlannedSpawn(-8, 2, c, h % 3 == 0
					? OathholdCitizenRules.Archetype.STREET_SWEEPER : OathholdCitizenRules.Archetype.TOWNSFOLK, 12, 90.0F));
				out.accept(new PlannedSpawn(8, 2, c, h % 4 == 0
					? OathholdCitizenRules.Archetype.CITY_WATCHMAN : OathholdCitizenRules.Archetype.NOBLE_PATRON, 12, -90.0F));
			}
			if (minZ <= 12 && maxZ >= -12 && c >= minX && c <= maxX && c < 490) {
				int h = OathholdCanvas.hash(c, 33, 0);
				out.accept(new PlannedSpawn(c, 2, -8, h % 3 == 0
					? OathholdCitizenRules.Archetype.CITY_WATCHMAN : OathholdCitizenRules.Archetype.TOWNSFOLK, 12, 0.0F));
				out.accept(new PlannedSpawn(c, 2, 8, h % 4 == 0
					? OathholdCitizenRules.Archetype.STREET_SWEEPER : OathholdCitizenRules.Archetype.TOWNSFOLK, 12, 180.0F));
			}
		}

		// 4. Outer Ring Canal Promenades
		for (int p = -608; p <= 608; p += 96) {
			if (Math.abs(p) <= 76) continue;
			for (int s : new int[]{-641, 641}) {
				if (p >= minX && p <= maxX && s >= minZ && s <= maxZ && p < 496) {
					out.accept(new PlannedSpawn(p, 1, s, (Math.abs(p) & 64) == 0
						? OathholdCitizenRules.Archetype.FISHMONGER_SAILOR : OathholdCitizenRules.Archetype.TOWNSFOLK, 12, 0.0F));
				}
				if (s >= minX && s <= maxX && p >= minZ && p <= maxZ && (s < 0 || Math.abs(p) >= 240)) {
					out.accept(new PlannedSpawn(s, 1, p, OathholdCitizenRules.Archetype.NOBLE_PATRON, 12, 90.0F));
				}
			}
		}

		// 5. Outer Metropolitan Urban Blocks (48x48 city islands -> ~550+ distinct district NPCs)
		int startGx = Math.max(-912, Math.floorDiv(minX - 48, 48) * 48);
		int endGx = Math.min(864, Math.floorDiv(maxX + 48, 48) * 48);
		int startGz = Math.max(-912, Math.floorDiv(minZ - 48, 48) * 48);
		int endGz = Math.min(864, Math.floorDiv(maxZ + 48, 48) * 48);

		for (int gx = startGx; gx <= endGx; gx += 48) {
			for (int gz = startGz; gz <= endGz; gz += 48) {
				int x0 = gx + 4, x1 = gx + 44, z0 = gz + 4, z1 = gz + 44;
				if (x1 < minX || x0 > maxX || z1 < minZ || z0 > maxZ) continue;
				if (OathholdOuterWards.overlapsMajorFeature(x0, z0, x1, z1)) continue;

				int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
				int h = OathholdCanvas.hash(x0, 77, z0);
				boolean east = cx >= 0;
				boolean north = cz < 0;

				OathholdCitizenRules.Archetype merchantRole = quadrantMerchant(east, north, h);
				OathholdCitizenRules.Archetype secondaryRole = quadrantSecondary(east, north, h);

				if (h < 18) {
					// Quadrant Signature Plaza: 4 active NPCs + open plaza floor
					out.accept(new PlannedSpawn(cx - 4, 2, cz + 6, OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST, 8, 0.0F));
					out.accept(new PlannedSpawn(cx + 4, 2, cz + 6, merchantRole, 8, 0.0F));
					out.accept(new PlannedSpawn(cx - 4, 2, cz - 6, OathholdCitizenRules.Archetype.STREET_SWEEPER, 10, 180.0F));
					out.accept(new PlannedSpawn(cx + 4, 2, cz - 6, OathholdCitizenRules.Archetype.CITY_WATCHMAN, 10, 180.0F));
				} else {
					// Raised y=1 Urban Island: shopkeeper in alley/plaza front (y=2), sweeper, citizen & guard/patron
					out.accept(new PlannedSpawn(x0 + 8, 2, cz - 4, merchantRole, 8, 0.0F));
					out.accept(new PlannedSpawn(x1 - 8, 2, cz + 4, secondaryRole, 10, 180.0F));
					out.accept(new PlannedSpawn(cx - 2, 2, z1 + 1, OathholdCitizenRules.Archetype.TOWNSFOLK, 12, 0.0F));
					out.accept(new PlannedSpawn(cx + 5, 2, z0 - 1, (h & 3) == 0
						? OathholdCitizenRules.Archetype.CITY_WATCHMAN
						: ((h & 1) == 0 ? OathholdCitizenRules.Archetype.STREET_SWEEPER : OathholdCitizenRules.Archetype.NOBLE_PATRON),
						12, 180.0F));
				}
			}
		}
	}

	/** Verifies all 16 archetypes appear across a full generated Oathhold city. */
	public static Set<OathholdCitizenRules.Archetype> coveredArchetypes() {
		Set<OathholdCitizenRules.Archetype> seen = EnumSet.noneOf(OathholdCitizenRules.Archetype.class);
		for (PlannedSpawn s : sampleAllPlannedCitizens()) {
			seen.add(s.archetype());
		}
		return seen;
	}

	private static OathholdCitizenRules.Archetype quadrantMerchant(boolean east, boolean north, int h) {
		if (!east && north) {
			return (h & 1) == 0 ? OathholdCitizenRules.Archetype.BLACKSMITH : OathholdCitizenRules.Archetype.CARPENTER_MASON;
		}
		if (east && north) {
			return (h % 3 == 0) ? OathholdCitizenRules.Archetype.ALCHEMIST_HERBALIST : OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE;
		}
		if (!east) {
			return switch (h % 3) {
				case 0 -> OathholdCitizenRules.Archetype.BAKER_COOK;
				case 1 -> OathholdCitizenRules.Archetype.CLOTHIER_TAILOR;
				default -> OathholdCitizenRules.Archetype.JEWELER_BANKER;
			};
		}
		return (h % 3 == 0) ? OathholdCitizenRules.Archetype.CARPENTER_MASON : OathholdCitizenRules.Archetype.FISHMONGER_SAILOR;
	}

	private static OathholdCitizenRules.Archetype quadrantSecondary(boolean east, boolean north, int h) {
		if (!east && north) return OathholdCitizenRules.Archetype.BLACKSMITH;
		if (east && north) return OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE;
		if (!east) return OathholdCitizenRules.Archetype.BAKER_COOK;
		return (h & 1) == 0 ? OathholdCitizenRules.Archetype.TOWNSFOLK : OathholdCitizenRules.Archetype.FISHMONGER_SAILOR;
	}
}
