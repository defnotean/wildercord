package dev.wildercord.world.upgrade;

import java.util.*;

/** Every family worldgen places today, each with an individually reviewed old-world decision. Pure; no game state. */
public final class UpgradeCatalog {
	private UpgradeCatalog() {}
	public enum Decision { BOUNDED_ADAPTER, WORLDGEN_ONLY, RUNTIME_ONLY }
	/** Encounter adapters name the natural structure/set they must never duplicate; spacing/separation mirror its random spread. */
	public record Family(String id,Decision decision,String structure,String structureSet,int spacing,int separation,String reason) {
		public boolean unique(){return structureSet!=null;}
	}
	public static final String PAVILION=UpgradeTemplates.FAMILY,SLEEPING_BLADE="wildercord:sleeping_blade_rest",
		BATTLEFIELD="wildercord:battlefield_memorial",SWORD_TOMB="wildercord:sword_tomb_duel_ring";
	public static final List<Family> ALL=List.of(
		new Family(PAVILION,Decision.BOUNDED_ADAPTER,null,null,0,0,"Inert 9x9x9 pavilion; no entity, loot or block entity; not a unique encounter"),
		new Family(SLEEPING_BLADE,Decision.BOUNDED_ADAPTER,"wildercord:sleeping_blade","wildercord:sleeping_blades",92,36,
			"7x7 rest with one authentic Sleeping Blade stone; one per sleeping_blades spread region; never spawns entities"),
		new Family(BATTLEFIELD,Decision.BOUNDED_ADAPTER,"wildercord:old_battlefield","wildercord:old_battlefields",64,24,
			"9x9 memorial ring with one authentic memorial; no loot chest; one per old_battlefields spread region"),
		new Family(SWORD_TOMB,Decision.BOUNDED_ADAPTER,"wildercord:sword_tomb","wildercord:sword_tombs",76,28,
			"13x13 duel ring with one authentic reliquary; the Gravekeeper only rises through the existing reliquary trigger; no gates or loot; one per sword_tombs spread region"),
		worldgen("wildercord:archive","Multi-chunk excavated structure with the Archivist boss arena, vault loot and generation-time guards"),
		worldgen("wildercord:ember_sanctum","Nether; multi-chunk; postProcess spawns guards and fills loot"),
		worldgen("wildercord:astral_observatory","End; multi-chunk; postProcess spawns guards and fills loot"),
		worldgen("wildercord:drowned_scriptorium","Deep ocean; requires fluid replacement, which the engine never performs"),
		worldgen("wildercord:rootbound_maze","Multi-chunk; terrain excavation and generation-time guards/loot"),
		worldgen("wildercord:storm_spire","Multi-chunk tower; generation-time guards/loot"),
		worldgen("wildercord:clockwork_crypt","Underground multi-chunk; terrain excavation and generation-time guards/loot"),
		worldgen("wildercord:living_greenhouse","Multi-chunk; generation-time guards/loot"),
		worldgen("wildercord:moving_sky_ruin","Multi-chunk floating ruin; generation-time guards/loot"),
		worldgen("wildercord:old_battlefield","Full 31x31 site carries a loot chest; old worlds get the bounded memorial adapter instead"),
		worldgen("wildercord:sword_tomb","Full 41x55 underground tomb needs excavation, intent gates and loot; old worlds get the bounded duel ring instead"),
		worldgen("wildercord:sleeping_blade","Full 19x19 site; old worlds get the bounded rest instead"),
		worldgen("wildercord:belowkeeper_drainhouse","Cave feature in lush/dripstone caves; underground edits of unknown history"),
		worldgen("wildercord:breathmark_site","Cave/fungal feature with an authentic mark entity; underground edits of unknown history"),
		worldgen("wildercord:cinder_fern_patch","Plant patch; vegetation regrowth is not an upgrade"),
		worldgen("wildercord:glowcap_patch","Plant patch; vegetation regrowth is not an upgrade"),
		worldgen("wildercord:moonreed_patch","Plant patch; vegetation regrowth is not an upgrade"),
		worldgen("wildercord:windreed_patch","Plant patch; vegetation regrowth is not an upgrade"),
		runtime("wildercord:training_grounds","Detected at runtime from natural waterfalls/summits; old worlds already have them"),
		runtime("wildercord:masters_and_duelists","Masters, Duelists and knights spawn at runtime by their own rules; no block placement"),
		runtime("wildercord:village_tournaments","Bounded runtime visitor event at inhabited village bells"));
	private static Family worldgen(String id,String reason){return new Family(id,Decision.WORLDGEN_ONLY,null,null,0,0,reason);}
	private static Family runtime(String id,String reason){return new Family(id,Decision.RUNTIME_ONLY,null,null,0,0,reason);}
	public static Optional<Family> adapter(String id){return ALL.stream().filter(f->f.decision()==Decision.BOUNDED_ADAPTER && f.id().equals(id)).findFirst();}
	public static Optional<Family> byStructure(String structure){return ALL.stream().filter(f->f.unique() && f.structure().equals(structure)).findFirst();}

	public record Region(int x,int z) {}
	public static Region region(Family f,int chunkX,int chunkZ){return new Region(Math.floorDiv(chunkX,f.spacing()),Math.floorDiv(chunkZ,f.spacing()));}
	/** The site's own spread region plus every neighbour region with a chunk within separation of the site. */
	public static List<Region> nearby(Family f,int chunkX,int chunkZ) {
		var own=region(f,chunkX,chunkZ);var result=new ArrayList<Region>();
		for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
			var r=new Region(own.x()+dx,own.z()+dz);
			if(gap(r.x()*f.spacing(),chunkX,f.spacing())<=f.separation() && gap(r.z()*f.spacing(),chunkZ,f.spacing())<=f.separation())result.add(r);
		}
		return result;
	}
	private static int gap(int start,int at,int size){return at<start?start-at:at>=start+size?at-start-size+1:0;}

	/** Known evidence of one encounter: a natural start/anchor, or an upgrade manifest/record. */
	public record Site(String family,int chunkX,int chunkZ,String origin,int version,String state) {
		public static final String NATURAL="NATURAL",UPGRADE="UPGRADE";
		public boolean live(){return !state.equals(UpgradeJournal.Phase.ROLLED_BACK.name()) && !state.equals(UpgradeJournal.Phase.PREVIEW.name());}
	}
	/** One encounter per spread region and none within separation chunks, whatever the origin. The site never blocks its own upgrade. */
	public static Optional<Site> duplicate(Family f,int chunkX,int chunkZ,Collection<Site> known) {
		if(!f.unique())return Optional.empty();
		var own=region(f,chunkX,chunkZ);
		return known.stream().filter(s->s.family().equals(f.id()) && s.live())
			.filter(s->!(s.origin().equals(Site.UPGRADE) && s.chunkX()==chunkX && s.chunkZ()==chunkZ))
			.filter(s->region(f,s.chunkX(),s.chunkZ()).equals(own) || Math.max(Math.abs(s.chunkX()-chunkX),Math.abs(s.chunkZ()-chunkZ))<=f.separation())
			.findFirst();
	}
}
