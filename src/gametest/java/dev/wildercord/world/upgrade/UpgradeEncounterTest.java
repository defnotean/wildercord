package dev.wildercord.world.upgrade;

import dev.wildercord.aura.world.BattlefieldMemoryEntity;
import dev.wildercord.aura.world.SleepingBladeEntity;
import dev.wildercord.aura.world.SleepingBlades;
import dev.wildercord.aura.world.SwordTombs;
import dev.wildercord.aura.world.TombReliquaryEntity;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Encounter adapters in a disposable fixture world: preview, approval, claims, duplicate refusal, restart resume, edits and rollback. */
public final class UpgradeEncounterTest implements FabricClientGameTest {
	private static final int Y=180,TOMB=64,SECOND=66,BLADE=68,KEEPER_SITE=100,Z=64;
	private static final String PROVIDER="fixture-encounter-claims";
	private static volatile UpgradeClaims.Status claims=UpgradeClaims.Status.CLEAR;
	private static volatile String revision="1";
	@Override public void runTest(ClientGameTestContext context) {
		UpgradeClaims.register(new UpgradeClaims.Provider() {
			public String id(){return PROVIDER;}
			public String revision(){return revision;}
			public UpgradeClaims.Status query(ServerLevel level,UpgradePlan plan){return claims;}
		});
		try{runFixtures(context);}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
		finally{UpgradeClaims.unregister(PROVIDER);}
	}
	private void runFixtures(ClientGameTestContext context) throws java.io.IOException {
		UpgradePlan[] saved=new UpgradePlan[2];UUID[] keeperId=new UUID[1];TestWorldSave save;
		try(var world=context.worldBuilder().create()) {
			save=world.getWorldSave();context.waitTicks(20);
			world.getServer().runCommand("gamerule spawn_mobs false");world.getServer().runCommand("gamerule random_tick_speed 0");world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("forceload add 1024 1024 1100 1024");world.getServer().runCommand("forceload add 1600 1024");
			world.getServer().waitFor(server->server.overworld().getChunkSource().getChunkNow(BLADE,Z)!=null && server.overworld().isPositionEntityTicking(new BlockPos(KEEPER_SITE*16,Y,Z*16)));
			world.getServer().runOnServer(server->{
				var level=server.overworld();var session=WorldUpgrades.session(server);
				for(int cx:new int[]{TOMB,SECOND,KEEPER_SITE})fixture(level,cx,Z);
				WorldUpgrades.enable(server,false,session.mods);
				var plan=WorldUpgrades.preview(level,UpgradeCatalog.SWORD_TOMB,TOMB,Y,Z);saved[0]=plan;
				check(plan.writes().size()==182 && plan.cells().size()==1014,"Exact reviewed tomb budget");assertCount(level,plan,0);
				var anchor=UpgradeTemplates.anchor(plan);check(anchor!=null && level.getBlockState(anchor).isAir(),"Preview writes nothing, not even the anchor");
				claims=UpgradeClaims.Status.CLAIMED;
				boolean refused=false;try{session.engine.approve(plan.hash(),"fixture-op","fixture-backup",true,WorldUpgrades.world(level));}catch(IllegalStateException expected){refused=expected.getMessage().contains("CLAIMED");}
				check(refused,"Claimed region cannot be approved");claims=UpgradeClaims.Status.CLEAR;
				session.engine.approve(plan.hash(),"fixture-op","fixture-backup",true,WorldUpgrades.world(level));
				for(int i=0;i<10;i++)session.engine.step(plan.hash(),WorldUpgrades.world(level),2);assertCount(level,plan,20);
				// A claim appearing after approval stops placement; a provider revision invalidates the old approval outright.
				claims=UpgradeClaims.Status.CLAIMED;
				var veto=session.engine.step(plan.hash(),WorldUpgrades.world(level),2);check(veto.writes()==0 && veto.reason().contains("CLAIMED"),"Changed claim refuses: "+veto.reason());
				claims=UpgradeClaims.Status.CLEAR;revision="2";
				check(session.engine.step(plan.hash(),WorldUpgrades.world(level),2).writes()==0,"Claim-provider revision requires reauthorization");assertCount(level,plan,20);
				session.engine.reauthorize(plan.hash(),"fixture-op","fixture-backup",true,WorldUpgrades.world(level));
				for(int i=0;i<40;i++)session.engine.step(plan.hash(),WorldUpgrades.world(level),2);assertCount(level,plan,100);
				check(session.engine.findHash(plan.hash()).orElseThrow().phase()==UpgradeJournal.Phase.APPLYING,"Interrupted mid-placement");

				// Second tomb in the same spread region: the in-progress manifest already owns the one keeper.
				refusal(()->WorldUpgrades.preview(level,UpgradeCatalog.SWORD_TOMB,SECOND,Y,Z),"Duplicate","Second tomb in one spread region");
				// A naturally generated, authentic Sleeping Blade two chunks away blocks an upgraded one.
				var natural=new BlockPos(BLADE*16+3,Y,Z*16+3);level.setBlock(natural,SleepingBlades.STONE.defaultBlockState(),WorldUpgrades.WRITE_FLAGS);
				check(level.getBlockEntity(natural) instanceof SleepingBladeEntity,"Natural blade fixture");((SleepingBladeEntity)level.getBlockEntity(natural)).awaken();
				refusal(()->WorldUpgrades.preview(level,UpgradeCatalog.SLEEPING_BLADE,SECOND,Y,Z),"Duplicate","Naturally generated blade in the region");
				check(UpgradeSites.of(level).sites().contains(new UpgradeCatalog.Site(UpgradeCatalog.SLEEPING_BLADE,BLADE,Z,UpgradeCatalog.Site.NATURAL,0,"PRESENT")),"Natural evidence is recorded");
				// Families are independent: a battlefield memorial still fits the same chunk and is marked authentic only once complete.
				var field=WorldUpgrades.preview(level,UpgradeCatalog.BATTLEFIELD,SECOND,Y,Z);saved[1]=field;
				session.engine.approve(field.hash(),"fixture-op","fixture-backup",true,WorldUpgrades.world(level));
				for(int i=0;i<28;i++)session.engine.step(field.hash(),WorldUpgrades.world(level),2);
				var memorial=level.getBlockEntity(UpgradeTemplates.anchor(field));
				check(memorial==null || memorial instanceof BattlefieldMemoryEntity m && !m.oldGround(),"Memorial anchor stays inert until activation");
				var done=session.engine.step(field.hash(),WorldUpgrades.world(level),2);
				check(done.phase()==UpgradeJournal.Phase.APPLIED && level.getBlockEntity(UpgradeTemplates.anchor(field)) instanceof BattlefieldMemoryEntity m && m.oldGround(),"Activated once every write is observed");assertCount(level,field,58);
				// A live Gravekeeper anywhere in the spread region refuses a new keeper site; no entity is ever spawned by the adapter.
				var keeper=SwordTombs.KEEPER.create(level,EntitySpawnReason.COMMAND);keeper.setNoAi(true);
				keeper.snapTo(KEEPER_SITE*16+0.5,Y,Z*16+0.5,0,0);check(level.addFreshEntity(keeper),"Keeper fixture");keeperId[0]=keeper.getUUID();
			});
			context.waitTicks(5);
			world.getServer().runOnServer(server->{
				var level=server.overworld();var keeper=level.getEntity(keeperId[0]);
				var near=level.getEntities(SwordTombs.KEEPER,new net.minecraft.world.phys.AABB(KEEPER_SITE*16-8,level.getMinY(),Z*16-8,KEEPER_SITE*16+8,level.getMaxY(),Z*16+8),e->e.isAlive()).size();
				check(keeper!=null && keeper.isAlive(),"Keeper fixture stays loaded: "+keeper+" "+level.getDifficulty()+" near="+near);
				refusal(()->WorldUpgrades.preview(level,UpgradeCatalog.SWORD_TOMB,KEEPER_SITE,Y,Z),"Gravekeeper","Live boss in the region (near="+near+")");
				keeper.discard();
				var free=WorldUpgrades.preview(level,UpgradeCatalog.SWORD_TOMB,KEEPER_SITE,Y,Z);assertCount(level,free,0);
				check(level.getEntities(SwordTombs.KEEPER,new net.minecraft.world.phys.AABB(KEEPER_SITE*16-1200,level.getMinY(),Z*16-1200,KEEPER_SITE*16+1200,level.getMaxY(),Z*16+1200),e->e.isAlive()).isEmpty(),"Previews never spawn a keeper");
			});
		}
		// Restart-equivalent: the integrated server stops and reopens the same disposable save.
		try(var world=save.open()) {
			context.waitTicks(20);world.getServer().waitFor(server->server.overworld().getChunkSource().getChunkNow(TOMB,Z)!=null && server.overworld().getChunkSource().getChunkNow(BLADE,Z)!=null);
			world.getServer().runOnServer(server->{
				var level=server.overworld();var session=WorldUpgrades.session(server);var plan=saved[0];
				check(!session.enabled && session.activeHash.isEmpty(),"Restart neither enables nor resumes");assertCount(level,plan,100);
				var records=UpgradeSites.of(level).sites();
				check(records.contains(new UpgradeCatalog.Site(UpgradeCatalog.SLEEPING_BLADE,BLADE,Z,UpgradeCatalog.Site.NATURAL,0,"PRESENT")),"Natural record survives restart");
				check(records.contains(new UpgradeCatalog.Site(UpgradeCatalog.BATTLEFIELD,SECOND,Z,UpgradeCatalog.Site.UPGRADE,1,"APPLIED")),"Content-version record survives restart");
				check(((BattlefieldMemoryEntity)level.getBlockEntity(UpgradeTemplates.anchor(saved[1]))).oldGround(),"Activated anchor saved");
				WorldUpgrades.enable(server,false,session.mods);
				check(session.engine.step(plan.hash(),WorldUpgrades.world(level),2).writes()==0,"Old approval cannot survive restart");
				session.engine.reauthorize(plan.hash(),"fixture-op","fixture-backup",true,WorldUpgrades.world(level));
				for(int i=0;i<41;i++)session.engine.step(plan.hash(),WorldUpgrades.world(level),2);
				check(session.engine.findHash(plan.hash()).orElseThrow().phase()==UpgradeJournal.Phase.APPLIED,"Resume completes exactly once");assertCount(level,plan,182);
				var anchor=UpgradeTemplates.anchor(plan);
				check(level.getBlockEntity(anchor) instanceof TombReliquaryEntity t && t.authentic() && t.guardian()==null,"Authentic reliquary, keeper still asleep");
				check(session.engine.step(plan.hash(),WorldUpgrades.world(level),2).writes()==0,"Repeated resume writes nothing");
				fixture(level,TOMB+1,Z);refusal(()->WorldUpgrades.preview(level,UpgradeCatalog.SWORD_TOMB,TOMB+1,Y,Z),"Duplicate","Still one tomb per region after restart");
				// Later player edit inside the applied site is preserved by conflict-aware rollback.
				var edited=plan.writes().getFirst();var pos=WorldUpgrades.pos(edited.point());
				check(level.setBlock(pos,Blocks.GOLD_BLOCK.defaultBlockState(),WorldUpgrades.WRITE_FLAGS),"Later player edit");
				for(int i=0;i<100;i++)session.engine.rollback(plan.hash(),WorldUpgrades.world(level),2);
				check(session.engine.findHash(plan.hash()).orElseThrow().phase()==UpgradeJournal.Phase.CONFLICT,"Rollback stops at conflict");
				check(level.getBlockState(pos).is(Blocks.GOLD_BLOCK),"Edited block never overwritten");
				for(var cell:plan.cells())if(!cell.point().equals(edited.point()))check(WorldUpgrades.world(level).state(cell.point()).equals(cell.before()),"Unedited cells restored exactly");
				check(level.getBlockEntity(anchor)==null,"Reliquary removed with the rollback");
				UpgradeSites.mirror(level,session.engine.entries());
				check(UpgradeSites.of(level).sites().contains(new UpgradeCatalog.Site(UpgradeCatalog.SWORD_TOMB,TOMB,Z,UpgradeCatalog.Site.UPGRADE,1,"CONFLICT")),"Conflict record stays live (fail closed)");
				// Clean rollback of the memorial tombstones it and frees the region record.
				var field=saved[1];check(session.engine.rollback(field.hash(),WorldUpgrades.world(level),2).writes()==0,"Pre-restart rollback authority expired");
				session.engine.reauthorizeRollback(field.hash(),"fixture-op","fixture-backup",WorldUpgrades.world(level));UpgradeEngine.Result last=null;for(int i=0;i<30;i++)last=session.engine.rollback(field.hash(),WorldUpgrades.world(level),2);
				check(session.engine.findHash(field.hash()).orElseThrow().phase()==UpgradeJournal.Phase.ROLLED_BACK,"Clean rollback: "+last);assertCount(level,field,0);
				UpgradeSites.mirror(level,session.engine.entries());
				check(UpgradeSites.of(level).sites().contains(new UpgradeCatalog.Site(UpgradeCatalog.BATTLEFIELD,SECOND,Z,UpgradeCatalog.Site.UPGRADE,1,"ROLLED_BACK")),"Rolled-back record");
				refusal(()->WorldUpgrades.preview(level,UpgradeCatalog.BATTLEFIELD,SECOND,Y,Z),"reserved","Tombstoned site is never re-previewed");
			});
		}
	}
	@FunctionalInterface private interface Attempt {Object run() throws Exception;}
	private static void refusal(Attempt attempt,String expected,String message) {
		try{attempt.run();}catch(IllegalStateException ex){check(ex.getMessage().contains(expected),message+": "+ex.getMessage());return;}catch(Exception ex){throw new AssertionError(message,ex);}
		throw new AssertionError(message+": expected refusal");
	}
	private static void fixture(ServerLevel level,int cx,int cz) {
		var chunk=level.getChunk(cx,cz);chunk.setInhabitedTime(0);chunk.setAllStarts(Map.of());chunk.setAllReferences(Map.of());
		for(var existing:List.copyOf(chunk.getBlockEntitiesPos()))level.setBlock(existing,Blocks.AIR.defaultBlockState(),WorldUpgrades.WRITE_FLAGS);
		// Deliberate fixture construction is outside the runtime scanner and placement implementation.
		for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int dy=-1;dy<=7;dy++)
			level.setBlock(new BlockPos(cx*16+8+x,Y+dy,cz*16+8+z),(dy==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),WorldUpgrades.WRITE_FLAGS);
		check(chunk.getBlockEntitiesPos().isEmpty(),"Fixture chunk contains no unrelated block entities");
	}
	private static void assertCount(ServerLevel level,UpgradePlan plan,int after) {
		var world=WorldUpgrades.world(level);int count=0;
		for(var cell:plan.cells()) {
			String actual=world.state(cell.point());
			if(cell.changes() && actual.equals(cell.after()))count++;
			else check(actual.equals(cell.before()),"Every remaining guard/before block unchanged");
		}
		check(count==after,"Exact actual written block count: expected "+after+", got "+count);
	}
	private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
