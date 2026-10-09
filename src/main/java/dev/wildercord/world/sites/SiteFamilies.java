package dev.wildercord.world.sites;

import dev.wildercord.world.upgrade.UpgradeCatalog.Decision;
import dev.wildercord.world.upgrade.UpgradeCatalog.Family;

import java.util.*;

/** Every explorable site's reviewed old-world decision. Pure; no game state. Each pack adds its own block. */
public final class SiteFamilies {
	private SiteFamilies() {}
	private static final List<Family> FAMILIES = new ArrayList<>();
	public static final List<Family> ALL = Collections.unmodifiableList(FAMILIES);

	private static void worldgen(String id, String reason) {
		FAMILIES.add(new Family(id, Decision.WORLDGEN_ONLY, null, null, 0, 0, reason));
	}

	static {
		// ---- sites-farm pack
		worldgen("wildercord:farm_windmill", "New farmstead site; generates only in newly explored chunks, never retrofitted into built land.");
		worldgen("wildercord:farm_herbalist", "New herbalist cottage; generates only in newly explored chunks.");
		worldgen("wildercord:farm_apiary", "New apiary terraces; generates only in newly explored chunks.");
		worldgen("wildercord:farm_orchard", "New walled orchard; generates only in newly explored chunks.");
		worldgen("wildercord:farm_shepherd", "New shepherd's hut and pen; generates only in newly explored chunks.");
		worldgen("wildercord:farm_mushroom_ring", "New sunken mushroom ring; generates only in newly explored chunks.");
		worldgen("wildercord:farm_granary", "New granary barn and harvest shrine; generates only in newly explored chunks.");
		worldgen("wildercord:farm_scarecrow", "New abandoned scarecrow field; generates only in newly explored chunks.");
		// ---- sites-masters pack
		worldgen("wildercord:master_forge_dojo", "New Master hall for Ember and Crimson; appears only in newly generated chunks.");
		worldgen("wildercord:master_wind_gate", "New Master shrine for Gale and Thunder; appears only in newly generated chunks.");
		worldgen("wildercord:master_quarry_hall", "New Master hall for Stone and Iron; appears only in newly generated chunks.");
		worldgen("wildercord:master_waterfall_shrine", "New Master shrine for Rime and Tide; appears only in newly generated chunks.");
		worldgen("wildercord:master_root_temple", "New Master temple for Verdant and Venom; appears only in newly generated chunks.");
		worldgen("wildercord:master_sundial_court", "New Master court for Dune and Hourglass; appears only in newly generated chunks.");
		worldgen("wildercord:master_star_terrace", "New Master terrace for Starlit and Dawn; appears only in newly generated chunks.");
		worldgen("wildercord:master_resonance_chamber", "New underground Master chamber for Hollow and Echo; appears only in newly generated chunks.");
		// ---- sites-wilds pack
		worldgen("wildercord:wilds_venom_ziggurat", "Jungle ruin with traps and a sealed vault; builds a full ziggurat, so new chunks only.");
		worldgen("wildercord:wilds_dune_temple", "Desert temple with a sanctum dug below the floor; new chunks only.");
		worldgen("wildercord:wilds_rime_monastery", "Snowy ruin with a crypt under the chapel; new chunks only.");
		worldgen("wildercord:wilds_iron_gatehouse", "Badlands towers and gate hall; new chunks only.");
		worldgen("wildercord:wilds_dawn_pavilion", "Cherry grove pavilion and shrine; clears its own ground, so new chunks only.");
		worldgen("wildercord:wilds_echo_post", "Deep dark cave building; carves its own room, so new chunks only.");
		worldgen("wildercord:wilds_ember_outpost", "Nether cave outpost; carves its own yard, so new chunks only.");
		worldgen("wildercord:wilds_void_lantern", "End island spire; new chunks only.");
		// ---- sites-travel pack
		worldgen("wildercord:travel_wayfarer_inn", "Crossroads inn with beds, a ledger and a pantry chest; new chunks only so no inn is built into existing land.");
		worldgen("wildercord:travel_watchtower", "Hilltop tower with a signal fire and watch chest; new chunks only.");
		worldgen("wildercord:travel_broken_bridge", "Ruined bridge that carves its own creek; never placed into explored terrain.");
		worldgen("wildercord:travel_rune_library", "Small library with a shelf riddle and hidden chest; new chunks only.");
		worldgen("wildercord:travel_standing_stones", "Stone ring with a lore book and a buried gift; new chunks only.");
		worldgen("wildercord:travel_caravan_camp", "Camp with a resident trader and llamas; spawning residents into old chunks is avoided.");
		worldgen("wildercord:travel_cartographer_hut", "Hut with wall maps and a desk chest map to another site; new chunks only.");
		worldgen("wildercord:travel_vow_circle", "Shrine explaining the Circle Vows; new chunks only.");
		// ---- sites-water pack
		worldgen("wildercord:water_stilt_smokehouse", "New water site: generates only in newly explored chunks.");
		worldgen("wildercord:water_lighthouse", "New water site: generates only in newly explored chunks.");
		worldgen("wildercord:water_watermill", "New water site: generates only in newly explored chunks.");
		worldgen("wildercord:water_ice_camp", "New water site: generates only in newly explored chunks.");
		worldgen("wildercord:water_pier_boathouse", "New water site: generates only in newly explored chunks.");
		worldgen("wildercord:water_sunken_shrine", "New water site: generates only in newly explored chunks.");
		worldgen("wildercord:water_netweaver_village", "New water site: generates only in newly explored chunks.");
		worldgen("wildercord:water_tidepool_grotto", "New water site: generates only in newly explored chunks.");
		// ---- sites-mine pack
		worldgen("wildercord:mine_hillside_mine", "New hillside mine entrance; generates only in new chunks.");
		worldgen("wildercord:mine_forge_hall", "New mountain forge hall; generates only in new chunks.");
		worldgen("wildercord:mine_crystal_lab", "New buried geode lab; carving into existing terrain would cut through player builds.");
		worldgen("wildercord:mine_collapsed_delve", "New collapsed delve; generates only in new chunks.");
		worldgen("wildercord:mine_basalt_foundry", "New Nether foundry; generates only in new chunks.");
		worldgen("wildercord:mine_deep_vault", "New deep vault; its long shaft must not cut through existing terrain.");
		worldgen("wildercord:mine_prospector_camp", "New badlands camp; generates only in new chunks.");
		worldgen("wildercord:mine_miners_rest", "New cliff-edge shrine; generates only in new chunks.");
	}
}
