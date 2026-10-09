package dev.wildercord.world.upgrade;

import dev.wildercord.world.upgrade.UpgradeCatalog.Site;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.world.upgrade.UpgradeJournal.Phase.*;

/** Reviewed catalog coverage, blueprint budgets, spread-region uniqueness and the engine's anchor/admission hooks. */
class UpgradeCatalogTest {
	@TempDir Path folder;
	static final Path WORLDGEN=Path.of("src/main/resources/data/wildercord/worldgen");

	@Test void everyWorldgenStructureAndFeatureHasAnIndividualDecision() throws Exception {
		var ids=new HashSet<String>();
		for(var dir:List.of("structure","feature"))try(var files=Files.list(WORLDGEN.resolve(dir))) {
			files.forEach(f->ids.add("wildercord:"+f.getFileName().toString().replace(".json","")));
		}
		assertTrue(ids.size()>=18,"Worldgen inventory read");
		for(var id:ids)assertTrue(UpgradeCatalog.ALL.stream().anyMatch(f->f.id().equals(id)),"Undecided worldgen family "+id);
		assertEquals(UpgradeCatalog.ALL.size(),UpgradeCatalog.ALL.stream().map(UpgradeCatalog.Family::id).distinct().count());
		for(var f:UpgradeCatalog.ALL)assertFalse(f.reason().isBlank());
		// Only families with a reviewed blueprint can be adapters; everything else stays worldgen/runtime only.
		for(var f:UpgradeCatalog.ALL)assertEquals(f.decision()==UpgradeCatalog.Decision.BOUNDED_ADAPTER,UpgradeBlueprints.of(f.id()).isPresent(),f.id());
		assertTrue(UpgradeCatalog.adapter("wildercord:archive").isEmpty());
	}
	@Test void uniqueFamiliesMirrorTheirShippedRandomSpread() throws Exception {
		var number=Pattern.compile("\"(spacing|separation)\"\\s*:\\s*(\\d+)");
		for(var f:UpgradeCatalog.ALL) {
			if(!f.unique())continue;
			assertTrue(UpgradeCatalog.ALL.stream().anyMatch(o->o.id().equals(f.structure()) && o.decision()==UpgradeCatalog.Decision.WORLDGEN_ONLY));
			String json=Files.readString(WORLDGEN.resolve("structure_set/"+f.structureSet().substring("wildercord:".length())+".json"));
			assertTrue(json.contains("\""+f.structure()+"\"") && json.contains("random_spread"),f.id());
			var m=number.matcher(json);var values=new HashMap<String,Integer>();while(m.find())values.put(m.group(1),Integer.parseInt(m.group(2)));
			assertEquals(f.spacing(),values.get("spacing"),f.id());assertEquals(f.separation(),values.get("separation"),f.id());
			assertEquals(f,UpgradeCatalog.byStructure(f.structure()).orElseThrow());
		}
	}
	@Test void blueprintsStayInsideOneChunkAndTheBoundedBudget() {
		for(var b:UpgradeBlueprints.ALL) {
			int r=b.radius(),writes=0,anchors=0;
			assertTrue(8-r>=0 && 8+r<=15,"Guard inside one chunk");
			assertTrue(b.cells()<=UpgradePlan.MAX_CELLS,b.family());
			for(int y=-1;y<=b.top();y++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++) {
				char c=b.at(x,y,z);if(c==' ')continue;
				assertTrue(y>=0,"Foundation is guard-only");writes++;
				if(c=='A'){anchors++;assertEquals(new UpgradePlan.Point(x,y,z),b.anchor());}
			}
			assertEquals(b.writes(),writes,b.family());assertTrue(writes<=UpgradePlan.MAX_WRITES);
			assertEquals(b.anchor()==null?0:1,anchors,b.family());
			if(b.anchor()!=null)assertEquals(' ',b.at(b.anchor().x(),b.anchor().y()+1,b.anchor().z()),"Anchor has headroom");
		}
		assertEquals(123,UpgradeBlueprints.of(UpgradeCatalog.PAVILION).orElseThrow().writes());
		assertEquals(729,UpgradeBlueprints.of(UpgradeCatalog.PAVILION).orElseThrow().cells());
		// The north-facing reliquary raises its keeper 8 blocks south: inside the guard, on floor, with three empty cells.
		var tomb=UpgradeBlueprints.of(UpgradeCatalog.SWORD_TOMB).orElseThrow();var a=tomb.anchor();
		assertTrue(Math.abs(a.z()+8)<=tomb.radius());assertEquals('B',tomb.at(a.x(),0,a.z()+8));
		for(int y=1;y<=3;y++)assertEquals(' ',tomb.at(a.x(),y,a.z()+8));
		assertTrue(tomb.top()>=3);
	}
	@Test void oneEncounterPerSpreadRegionWhateverItsOrigin() {
		var f=UpgradeCatalog.adapter(UpgradeCatalog.SWORD_TOMB).orElseThrow();
		assertEquals(new UpgradeCatalog.Region(0,0),UpgradeCatalog.region(f,75,0));
		assertEquals(new UpgradeCatalog.Region(-1,-1),UpgradeCatalog.region(f,-1,-76));
		assertEquals(List.of(new UpgradeCatalog.Region(0,0)),UpgradeCatalog.nearby(f,38,38));
		assertTrue(UpgradeCatalog.nearby(f,2,38).contains(new UpgradeCatalog.Region(-1,0)));
		var natural=new Site(f.id(),70,10,Site.NATURAL,0,"PRESENT");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(natural)).isPresent(),"Natural site anywhere in the region");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(new Site(f.id(),5,5,Site.NATURAL,0,"PRESENT"))).isPresent(),"Natural evidence in the very chunk");
		assertTrue(UpgradeCatalog.duplicate(f,80,5,List.of(natural)).isPresent(),"Neighbour region within separation");
		assertTrue(UpgradeCatalog.duplicate(f,150,5,List.of(natural)).isEmpty(),"Far region is independent");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(new Site(f.id(),9,9,Site.UPGRADE,1,APPLIED.name()))).isPresent(),"Another applied upgrade");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(new Site(f.id(),9,9,Site.UPGRADE,1,CONFLICT.name()))).isPresent(),"Conflicted anchor may remain");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(new Site(f.id(),9,9,Site.UPGRADE,1,ROLLED_BACK.name()))).isEmpty(),"Rolled back site is gone");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(new Site(f.id(),9,9,Site.UPGRADE,1,PREVIEW.name()))).isEmpty(),"Previews never block");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(new Site(f.id(),5,5,Site.UPGRADE,2,APPLYING.name()))).isEmpty(),"A site never blocks its own resume");
		assertTrue(UpgradeCatalog.duplicate(f,5,5,List.of(new Site(UpgradeCatalog.BATTLEFIELD,5,5,Site.NATURAL,0,"PRESENT"))).isEmpty(),"Families are independent");
		var pavilion=UpgradeCatalog.adapter(UpgradeCatalog.PAVILION).orElseThrow();
		assertFalse(pavilion.unique());assertTrue(UpgradeCatalog.duplicate(pavilion,0,0,List.of(new Site(pavilion.id(),0,1,Site.UPGRADE,1,APPLIED.name()))).isEmpty());
	}

	static final class World implements UpgradeEngine.World {
		final Map<UpgradePlan.Point,String> cells=new HashMap<>();String admission="",activation="";int activations;
		World(UpgradePlan plan){plan.cells().forEach(c->cells.put(c.point(),c.before()));}
		public String refusal(UpgradePlan p,boolean rollback){return "";}
		public String policy(){return "provider-1";}
		public String state(UpgradePlan.Point p){return cells.get(p);}
		public boolean set(UpgradePlan.Point p,String expected,String replacement){if(!state(p).equals(expected))return false;cells.put(p,replacement);return true;}
		public String admission(UpgradePlan plan){return admission;}
		public String activate(UpgradePlan plan){if(activation.isEmpty())activations++;return activation;}
	}
	@Test void admissionRefusesApprovalAndAnchorsActivateBeforeApplied() throws Exception {
		var plan=UpgradeEngineTest.cairn(1);var world=new World(plan);var engine=new UpgradeEngine(new UpgradeJournal(folder));
		engine.preview(plan,world.policy());world.admission="Duplicate encounter";
		assertThrows(IllegalStateException.class,()->engine.approve(plan.hash(),"op","backup",true,world));
		assertEquals(PREVIEW,engine.findHash(plan.hash()).orElseThrow().phase());
		world.admission="";engine.approve(plan.hash(),"op","backup",true,world);world.activation="Anchor missing; deferred";
		for(int i=0;i<7;i++)engine.step(plan.hash(),world,2);
		assertEquals(APPLYING,engine.findHash(plan.hash()).orElseThrow().phase(),"All writes observed but activation deferred");
		assertEquals(0,world.activations);
		// A restart re-reads the manifest; a duplicate found meanwhile blocks the placement reauthorization.
		var reopened=new UpgradeEngine(new UpgradeJournal(folder));world.admission="Natural site appeared";
		assertThrows(IllegalStateException.class,()->reopened.reauthorize(plan.hash(),"op","backup",true,world));
		world.admission="";world.activation="";reopened.reauthorize(plan.hash(),"op","backup",true,world);
		var result=reopened.step(plan.hash(),world,2);
		assertEquals(APPLIED,result.phase());assertEquals(0,result.writes());assertEquals(1,world.activations);
		// Idempotent on repeated resume; rollback renewal is never blocked by placement admission.
		reopened.step(plan.hash(),world,2);assertEquals(2,world.activations);
		world.admission="Duplicate";reopened.reauthorizeRollback(plan.hash(),"op","backup",world);
		for(int i=0;i<7;i++)reopened.rollback(plan.hash(),world,2);
		assertEquals(ROLLED_BACK,reopened.findHash(plan.hash()).orElseThrow().phase());
		for(var c:plan.cells())assertEquals(c.before(),world.state(c.point()));
	}
}
