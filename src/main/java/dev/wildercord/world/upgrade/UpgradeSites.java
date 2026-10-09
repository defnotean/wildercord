package dev.wildercord.world.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.BattlefieldMemoryEntity;
import dev.wildercord.aura.world.SleepingBladeEntity;
import dev.wildercord.aura.world.SwordTombs;
import dev.wildercord.aura.world.TombReliquaryEntity;
import dev.wildercord.world.upgrade.UpgradeCatalog.Site;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import java.util.*;

/**
 * Saved content-version records per site: natural encounters seen in loaded chunks and every upgrade manifest's
 * family/version/phase. The forced journal stays the authority; this mirror adds natural evidence and survives journal archiving.
 */
public final class UpgradeSites extends SavedData {
	static final int MAX_SITES=4096;
	private static final Codec<Site> SITE=RecordCodecBuilder.create(i->i.group(
		Codec.STRING.fieldOf("family").forGetter(Site::family),Codec.INT.fieldOf("chunk_x").forGetter(Site::chunkX),Codec.INT.fieldOf("chunk_z").forGetter(Site::chunkZ),
		Codec.STRING.fieldOf("origin").forGetter(Site::origin),Codec.INT.fieldOf("version").forGetter(Site::version),Codec.STRING.fieldOf("state").forGetter(Site::state)
	).apply(i,Site::new));
	private static final Codec<UpgradeSites> CODEC=RecordCodecBuilder.create(i->i.group(
		SITE.listOf().fieldOf("sites").forGetter(d->List.copyOf(d.sites.values())),
		Codec.BOOL.optionalFieldOf("saturated",false).forGetter(d->d.saturated)
	).apply(i,UpgradeSites::new));
	private static final SavedDataType<UpgradeSites> TYPE=new SavedDataType<>(Wildercord.id("upgrade_sites"),UpgradeSites::new,CODEC,null);
	private final Map<String,Site> sites=new LinkedHashMap<>();
	private boolean saturated;
	public UpgradeSites() {}
	private UpgradeSites(List<Site> list,boolean saturated){for(var s:list)if(sites.size()<MAX_SITES)sites.put(key(s),s);this.saturated=saturated || list.size()>MAX_SITES;}
	private static String key(Site s){return s.family()+"@"+s.chunkX()+","+s.chunkZ()+"/"+s.origin();}
	static UpgradeSites of(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
	public List<Site> sites(){return List.copyOf(sites.values());}
	public boolean saturated(){return saturated;}
	/** Upsert; overflow fails closed for every unique family instead of forgetting evidence. */
	void record(Site site) {
		var old=sites.get(key(site));if(site.equals(old))return;
		if(old==null && sites.size()>=MAX_SITES){if(!saturated){saturated=true;setDirty();}return;}
		sites.put(key(site),site);setDirty();
	}
	/** Mirrors every manifest of a unique family into the saved content-version records. */
	static void mirror(ServerLevel level,Collection<UpgradeJournal.Entry> entries) {
		if(!level.dimension().equals(Level.OVERWORLD))return;var data=of(level);
		for(var e:entries)if(UpgradeCatalog.adapter(e.plan().family()).isPresent())
			data.record(new Site(e.plan().family(),e.plan().chunkX(),e.plan().chunkZ(),Site.UPGRADE,e.plan().version(),e.phase().name()));
	}

	/** Natural evidence: valid Wildercord structure starts and authentic generated anchors, read from a chunk that is already loading. */
	static void observe(ServerLevel level,LevelChunk chunk,Collection<UpgradeJournal.Entry> journal) {
		if(!level.dimension().equals(Level.OVERWORLD))return;
		var structures=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);var pos=chunk.getPos();
		for(var start:chunk.getAllStarts().entrySet()) {
			if(!start.getValue().isValid())continue;var id=structures.getKey(start.getKey());if(id==null)continue;
			UpgradeCatalog.byStructure(id.toString()).ifPresent(f->of(level).record(new Site(f.id(),pos.x(),pos.z(),Site.NATURAL,0,"PRESENT")));
		}
		for(var be:chunk.getBlockEntities().values()) {
			String family=authenticFamily(be);
			if(family!=null && journal.stream().noneMatch(e->be.getBlockPos().equals(UpgradeTemplates.anchor(e.plan()))))
				of(level).record(new Site(family,pos.x(),pos.z(),Site.NATURAL,0,"PRESENT"));
		}
	}
	static String authenticFamily(BlockEntity be) {
		if(be instanceof SleepingBladeEntity s && s.authentic())return UpgradeCatalog.SLEEPING_BLADE;
		if(be instanceof TombReliquaryEntity t && t.authentic())return UpgradeCatalog.SWORD_TOMB;
		if(be instanceof BattlefieldMemoryEntity b && b.oldGround())return UpgradeCatalog.BATTLEFIELD;
		return null;
	}

	/** Cheap per-tick check: saved natural/upgrade records plus every other journal manifest of the family. */
	static String duplicate(ServerLevel level,UpgradePlan plan,Collection<UpgradeJournal.Entry> journal) {
		var family=UpgradeCatalog.adapter(plan.family()).orElse(null);if(family==null || !family.unique())return "";
		var data=of(level);if(data.saturated)return "Site records saturated; unique encounters refused (fail closed)";
		var known=new ArrayList<>(data.sites.values());
		for(var e:journal)if(!e.plan().siteId().equals(plan.siteId()))known.add(new Site(e.plan().family(),e.plan().chunkX(),e.plan().chunkZ(),Site.UPGRADE,e.plan().version(),e.phase().name()));
		return UpgradeCatalog.duplicate(family,plan.chunkX(),plan.chunkZ(),known)
			.map(s->"Duplicate "+family.id()+" refused: "+s.origin()+" "+s.state()+" at chunk "+s.chunkX()+","+s.chunkZ()+" shares its spread region or lies within "+family.separation()+" chunks").orElse("");
	}
	/** Approval-time check: resident chunks of nearby regions, live bosses, and natural placement in the generator, without loading chunks. */
	static String admission(ServerLevel level,UpgradePlan plan,Collection<UpgradeJournal.Entry> journal) {
		var family=UpgradeCatalog.adapter(plan.family()).orElse(null);if(family==null || !family.unique())return "";
		var regions=UpgradeCatalog.nearby(family,plan.chunkX(),plan.chunkZ());
		int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
		for(var r:regions){minX=Math.min(minX,r.x());minZ=Math.min(minZ,r.z());maxX=Math.max(maxX,r.x());maxZ=Math.max(maxZ,r.z());}
		int fromX=minX*family.spacing(),fromZ=minZ*family.spacing(),toX=(maxX+1)*family.spacing()-1,toZ=(maxZ+1)*family.spacing()-1;
		var source=level.getChunkSource();
		for(int x=fromX;x<=toX;x++)for(int z=fromZ;z<=toZ;z++){var chunk=source.getChunkNow(x,z);if(chunk!=null)observe(level,chunk,journal);}
		if(family.id().equals(UpgradeCatalog.SWORD_TOMB)) {
			var box=new AABB(fromX*16,level.getMinY(),fromZ*16,(toX+1)*16,level.getMaxY()+1,(toZ+1)*16);
			if(!level.getEntities(SwordTombs.KEEPER,box,e->e.isAlive()).isEmpty())return "A live Gravekeeper is loaded in this spread region; a second keeper site is refused";
		}
		String natural=natural(level,family,plan.chunkX(),plan.chunkZ(),regions);
		if(!natural.isEmpty())return natural;
		return duplicate(level,plan,journal);
	}
	/** START_PRESENT or CHUNK_LOAD_NEEDED both refuse: an unverifiable natural site counts as present. */
	private static String natural(ServerLevel level,UpgradeCatalog.Family family,int chunkX,int chunkZ,List<UpgradeCatalog.Region> regions) {
		var state=level.getChunkSource().getGeneratorState();
		var key=ResourceKey.create(Registries.STRUCTURE_SET,Identifier.parse(family.structureSet()));
		var set=state.possibleStructureSets().stream().filter(h->h.is(key)).findFirst().orElse(null);
		if(set==null)return "";// This generator never places the family (for example a flat or modded preset).
		if(!(set.value().placement() instanceof RandomSpreadStructurePlacement spread) || spread.spacing()!=family.spacing() || spread.separation()!=family.separation())
			return "Natural placement for "+family.structureSet()+" differs from the reviewed spread; refused";
		for(var r:regions) {
			var candidate=spread.getPotentialStructureChunk(state.getLevelSeed(),r.x()*family.spacing(),r.z()*family.spacing());
			if(!r.equals(UpgradeCatalog.region(family,chunkX,chunkZ)) && Math.max(Math.abs(candidate.x()-chunkX),Math.abs(candidate.z()-chunkZ))>family.separation())continue;
			for(var entry:set.value().structures()) {
				var result=level.structureManager().checkStructurePresence(candidate,entry.structure().value(),spread,false);
				if(result!=StructureCheckResult.START_NOT_PRESENT)
					return "Natural "+family.structure()+" "+result+" at candidate chunk "+candidate.x()+","+candidate.z()+" in this or a neighbouring spread region";
			}
		}
		return "";
	}
	/** Rollback must not remove an anchor while its encounter is live. */
	static String busy(ServerLevel level,UpgradePlan plan) {
		BlockPos at=UpgradeTemplates.anchor(plan);if(at==null || level.getChunkSource().getChunkNow(plan.chunkX(),plan.chunkZ())==null)return "";
		var be=level.getBlockEntity(at);
		if(be instanceof TombReliquaryEntity t && t.guardian()!=null)return "Gravekeeper encounter is live; rollback deferred";
		if(be instanceof SleepingBladeEntity && dev.wildercord.aura.world.SleepingBlades.drawingAt(level,at))return "Sleeping Blade draw in progress; rollback deferred";
		return "";
	}
}
