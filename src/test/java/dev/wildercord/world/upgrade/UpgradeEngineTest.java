package dev.wildercord.world.upgrade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.world.upgrade.UpgradeJournal.Phase.*;

/** Real forced journal files, simulated crash boundaries and compare-and-set conflicts; no Minecraft mocks. */
class UpgradeEngineTest {
	@TempDir Path folder;
	static UpgradePlan cairn(int version) {
		var cells=new ArrayList<UpgradePlan.Cell>();
		for(int y=-1;y<=4;y++)for(int x=6;x<=10;x++)for(int z=6;z<=10;z++) {
			String before=y==-1?"minecraft:stone":"minecraft:air",after=before;
			if(y==0 && x>=7 && x<=9 && z>=7 && z<=9)after="minecraft:mossy_stone_bricks";
			if(x==8 && z==8 && y>=1 && y<=3)after="minecraft:stone_bricks";
			cells.add(new UpgradePlan.Cell(new UpgradePlan.Point(x,70+y,z),before,after));
		}
		return new UpgradePlan(12,"minecraft:overworld","wildercord:test_cairn",version,0,0,cells);
	}
	static final class World implements UpgradeEngine.World {
		final Map<UpgradePlan.Point,String> cells=new HashMap<>();String refusal="",policy="provider-1";int writes;
		int crashAfter=-1; Runnable duringWrite,duringRefusal;boolean denyAfterWrite;
		World(UpgradePlan plan){plan.cells().forEach(c->cells.put(c.point(),c.before()));}
		public String refusal(UpgradePlan p,boolean rollback){if(duringRefusal!=null)duringRefusal.run();return refusal;}
		public String policy(){return policy;}
		public String state(UpgradePlan.Point p){return cells.get(p);}
		public boolean set(UpgradePlan.Point p,String expected,String replacement){
			if(!state(p).equals(expected))return false;
			cells.put(p,replacement);writes++;
			if(duringWrite!=null)duringWrite.run();
			if(writes==crashAfter)throw new Crash();return !denyAfterWrite;
		}
	}
	static final class Crash extends RuntimeException {}
	UpgradeEngine engine() throws IOException{return new UpgradeEngine(new UpgradeJournal(folder));}
	UpgradeEngine approved(UpgradePlan plan,World world) throws IOException {
		var engine=engine();engine.preview(plan,world.policy());engine.approve(plan.hash(),"operator","offline-backup-42",true,world);return engine;
	}
	void finish(UpgradeEngine engine,UpgradePlan plan,World world) throws IOException {
		for(int i=0;i<7;i++)engine.step(plan.hash(),world,2);
		assertEquals(APPLIED,engine.findHash(plan.hash()).orElseThrow().phase());
	}
	@Test void previewHasNoWritesAndRequiresAllApprovalComponents() throws Exception {
		var plan=cairn(1);var world=new World(plan);var engine=engine();engine.preview(plan,world.policy());
		assertEquals(12,plan.writes().size());assertEquals(150,plan.cells().size());
		assertEquals(0,engine.step(plan.hash(),world,2).writes());assertEquals(0,world.writes);
		assertThrows(IllegalArgumentException.class,()->engine.approve(plan.hash(),"op","",true,world));
		assertThrows(IllegalArgumentException.class,()->engine.approve(plan.hash(),"op","backup",false,world));
		world.refusal="UNKNOWN claims";assertThrows(IllegalStateException.class,()->engine.approve(plan.hash(),"op","backup",true,world));
		world.refusal="";world.policy="changed";assertThrows(IllegalStateException.class,()->engine.approve(plan.hash(),"op","backup",true,world));
		assertEquals(0,world.writes);
	}
	@Test void providerRevisionChangeDuringApprovalCannotUpgradePreviewAuthority() throws Exception {
		var p=cairn(1);var w=new World(p);var e=engine();e.preview(p,w.policy());w.duringRefusal=()->w.policy="changed-inside-query";
		assertThrows(IllegalStateException.class,()->e.approve(p.hash(),"op","backup",true,w));
		assertEquals(PREVIEW,e.findHash(p.hash()).orElseThrow().phase());assertEquals(0,w.writes);
	}
	@Test void siteIsStableAcrossBlueprintVersionsAndTombstonesPreventDuplicates() throws Exception {
		var p=cairn(1);assertEquals(p.siteId(),cairn(2).siteId());assertNotEquals(p.hash(),cairn(2).hash());
		var w=new World(p);var e=approved(p,w);finish(e,p,w);
		assertThrows(IllegalStateException.class,()->e.preview(cairn(2),w.policy()));
		for(int i=0;i<7;i++)e.rollback(p.hash(),w,2);
		assertEquals(ROLLED_BACK,e.findHash(p.hash()).orElseThrow().phase());
		assertThrows(IllegalStateException.class,()->engine().preview(cairn(2),w.policy()));
		assertEquals(0,e.step(p.hash(),w,2).writes());
	}
	@Test void everyCrashBoundaryResumesWithoutDuplicateWrites() throws Exception {
		for(int boundary=1;boundary<=12;boundary++) {
			Path child=folder.resolve("crash-"+boundary);var p=cairn(1);var w=new World(p);
			var e=new UpgradeEngine(new UpgradeJournal(child));e.preview(p,w.policy());e.approve(p.hash(),"op","backup",true,w);
			w.crashAfter=boundary;final var started=e;
			assertThrows(Crash.class,()->{for(int i=0;i<6;i++)started.step(p.hash(),w,2);});
			var disk=new UpgradeJournal(child).read(p.siteId()).orElseThrow();
			assertEquals(APPLYING,disk.phase());assertEquals(boundary-1,disk.intent(),"Intent persisted before mutation");
			w.crashAfter=-1;e=new UpgradeEngine(new UpgradeJournal(child));finish(e,p,w);assertEquals(12,w.writes);
			assertEquals(0,e.step(p.hash(),w,2).writes(),"Reentrant resume is idempotent");
		}
	}
	@Test void appliedManifestReconcilesChunksThatSavedOnlySomeWrites() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);finish(e,p,w);
		for(int i=0;i<6;i++)w.cells.put(p.writes().get(i).point(),p.writes().get(i).before());
		e=engine();w.policy="new-session";
		assertEquals(0,e.step(p.hash(),w,2).writes(),"No automatic old-session authority");
		e.reauthorize(p.hash(),"op","backup",true,w);finish(e,p,w);assertEquals(18,w.writes);
		w.refusal="unloaded";assertFalse(e.step(p.hash(),w,2).complete(),"APPLIED journal does not complete a deferred disk reconciliation");
	}
	@Test void journalFailureOccursBeforeAnyBlockMutation() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);
		Files.createDirectory(folder.resolve(p.siteId()+".tmp"));
		assertThrows(IOException.class,()->e.step(p.hash(),w,2));assertEquals(0,w.writes);assertFalse(e.healthy());
		assertThrows(IOException.class,()->e.step(p.hash(),w,2));
	}
	@Test void fullSnapshotAndPerWriteExpectedStatePreventStalePlacement() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);var guard=p.cells().getFirst();w.cells.put(guard.point(),"minecraft:chest");
		assertEquals(CONFLICT,e.step(p.hash(),w,2).phase());assertEquals(0,w.writes);
	}
	@Test void unloadPlayersAndClaimsDeferWithoutMutation() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);
		for(String reason:List.of("unloaded","player approach","CLAIMED","UNKNOWN/error","NO_PROVIDER_CONFIGURED")) {
			w.refusal=reason;assertEquals(0,e.step(p.hash(),w,2).writes());assertEquals(0,w.writes);
		}
		w.refusal="";assertEquals(2,e.step(p.hash(),w,2).writes());assertThrows(IllegalArgumentException.class,()->e.step(p.hash(),w,3));
	}
	@Test void rollbackPreservesLaterEditsIncludingAnIdenticalReplacement() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);finish(e,p,w);var changed=p.writes().getFirst();
		e.fence(p.dimension(),changed.point());w.cells.put(changed.point(),changed.before());w.cells.put(changed.point(),changed.after());
		e=engine();for(int i=0;i<7;i++)e.rollback(p.hash(),w,2);
		assertEquals(CONFLICT,e.findHash(p.hash()).orElseThrow().phase());assertEquals(changed.after(),w.state(changed.point()));
		for(var cell:p.writes().subList(1,12))assertEquals(cell.before(),w.state(cell.point()));
	}
	@Test void concurrentEditFenceCannotBeOverwrittenByProgress() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);var changed=p.writes().getFirst();
		w.duringWrite=()->{try{e.fence(p.dimension(),changed.point());}catch(IOException ex){throw new RuntimeException(ex);}};
		assertEquals(CONFLICT,e.step(p.hash(),w,2).phase());assertEquals(1,w.writes);
		assertTrue(engine().findHash(p.hash()).orElseThrow().edits().contains(changed.point()));
	}
	@Test void rollbackRecoversAtEveryCrashBoundary() throws Exception {
		for(int boundary=1;boundary<=12;boundary++) {
			Path child=folder.resolve("rollback-"+boundary);var p=cairn(1);var w=new World(p);
			var e=new UpgradeEngine(new UpgradeJournal(child));e.preview(p,w.policy());e.approve(p.hash(),"op","backup",true,w);finish(e,p,w);
			w.crashAfter=12+boundary;final var started=e;
			assertThrows(Crash.class,()->{for(int i=0;i<6;i++)started.rollback(p.hash(),w,2);});
			assertEquals(ROLLING_BACK,new UpgradeJournal(child).read(p.siteId()).orElseThrow().phase());
			w.crashAfter=-1;e=new UpgradeEngine(new UpgradeJournal(child));for(int i=0;i<7;i++)e.rollback(p.hash(),w,2);
			assertEquals(ROLLED_BACK,e.findHash(p.hash()).orElseThrow().phase());assertEquals(24,w.writes);
		}
	}
	@Test void falseWriteResultCannotEraseNestedRollbackFence() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);finish(e,p,w);var edited=p.writes().getLast();
		w.denyAfterWrite=true;w.duringWrite=()->{try{e.fence(p.dimension(),edited.point());w.cells.put(edited.point(),edited.after());}catch(IOException ex){throw new RuntimeException(ex);}};
		assertEquals(2,e.rollback(p.hash(),w,2).writes(),"Failed post-mutation attempts still consume the per-tick budget");
		assertTrue(engine().findHash(p.hash()).orElseThrow().edits().contains(edited.point()));
		w.denyAfterWrite=false;w.duringWrite=null;for(int i=0;i<7;i++)e.rollback(p.hash(),w,2);
		assertEquals(edited.after(),w.state(edited.point()));assertTrue(engine().findHash(p.hash()).orElseThrow().edits().contains(edited.point()));
	}
	@Test void falseWriteResultCannotEraseNestedPlacementFence() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);var edited=p.writes().getFirst();
		w.denyAfterWrite=true;w.duringWrite=()->{try{e.fence(p.dimension(),edited.point());}catch(IOException ex){throw new RuntimeException(ex);}};
		assertEquals(CONFLICT,e.step(p.hash(),w,2).phase());
		assertTrue(engine().findHash(p.hash()).orElseThrow().edits().contains(edited.point()));
	}
	@Test void rollbackOnlyReauthorizationCannotGrantPlacementAuthority() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);e.step(p.hash(),w,2);w.policy="new-session";
		e.reauthorizeRollback(p.hash(),"op","backup",w);
		assertEquals(ROLLING_BACK,engine().findHash(p.hash()).orElseThrow().phase());
		assertEquals(0,e.step(p.hash(),w,2).writes());assertEquals(2,w.writes);
	}
	@Test void rollbackTombstoneReconcilesLaggingChunkSaveWithoutReauthorizingPlacement() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);finish(e,p,w);
		for(int i=0;i<7;i++)e.rollback(p.hash(),w,2);
		var restored=p.writes().getFirst();w.cells.put(restored.point(),restored.after());
		e=engine();w.policy="restart-policy";
		assertEquals(0,e.rollback(p.hash(),w,2).writes());
		e.reauthorizeRollback(p.hash(),"op","backup",w);
		assertEquals(1,e.rollback(p.hash(),w,2).writes());assertEquals(restored.before(),w.state(restored.point()));
		assertEquals(0,e.step(p.hash(),w,2).writes(),"Rollback reauthorization cannot revive a site");
	}
	@Test void failedJournalDoesNotBlockUnrelatedWorldEdits() throws Exception {
		var p=cairn(1);var w=new World(p);var e=approved(p,w);Files.createDirectory(folder.resolve(p.siteId()+".tmp"));
		assertThrows(IOException.class,()->e.step(p.hash(),w,2));
		assertDoesNotThrow(()->e.fence(p.dimension(),new UpgradePlan.Point(1000,70,1000)));
		assertThrows(IOException.class,()->e.fence(p.dimension(),p.writes().getFirst().point()));
	}
	@Test void corruptManifestFailsClosedAndNeverSilentlyStartsFresh() throws Exception {
		var p=cairn(1);var w=new World(p);approved(p,w);var path=folder.resolve(p.siteId()+".wcu");byte[] bytes=Files.readAllBytes(path);bytes[22]^=1;Files.write(path,bytes);
		assertThrows(IOException.class,this::engine);
	}
}
