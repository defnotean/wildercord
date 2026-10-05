package dev.wildercord.world.upgrade;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Real fixture-world close/reopen, actual blocks and journal/mixin integration. Never targets an existing user save. */
public final class UpgradeRecoveryTest implements FabricClientGameTest {
	private static final int CX=64,CZ=64,Y=180;
	private static final class SimulatedInterruption extends RuntimeException {}
	@Override public void runTest(ClientGameTestContext context) {
		try{runFixtures(context);}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
	}
	private void runFixtures(ClientGameTestContext context) throws java.io.IOException {
		UpgradePlan[] saved=new UpgradePlan[1];TestWorldSave save;
		try(var world=context.worldBuilder().create()) {
			save=world.getWorldSave();context.waitTicks(20);
			world.getServer().runCommand("gamerule spawn_mobs false");world.getServer().runCommand("gamerule random_tick_speed 0");
			world.getServer().runCommand("forceload add 1024 1024");
			world.getServer().waitFor(server->server.overworld().getChunkSource().getChunkNow(CX,CZ)!=null);
			world.getServer().runOnServer(server->{
				var level=server.overworld();fixture(level,CX,CZ);var session=WorldUpgrades.session(server);
				check(!session.enabled,"Startup is read-only");var plan=WorldUpgrades.preview(level,CX,Y,CZ);
				check(plan.writes().size()==123 && plan.cells().size()==729,"Exact approved pavilion/guard budgets");assertCount(level,plan,0);
				boolean refused=false;try{session.engine.approve(plan.hash(),"fixture-op","fixture-backup",true,WorldUpgrades.world(level));}catch(IllegalStateException expected){refused=true;}
				check(refused,"Preview cannot authorize disabled writes");assertCount(level,plan,0);
				WorldUpgrades.enable(server,true,session.mods);
				var obstruction=new BlockPos(CX*16+8,Y,CZ*16+8);
				level.setBlock(obstruction,Blocks.CHEST.defaultBlockState(),WorldUpgrades.WRITE_FLAGS);
				refused=false;try{WorldUpgrades.preview(level,CX,Y,CZ);}catch(IllegalStateException expected){refused=true;}
				check(refused && level.getBlockState(obstruction).is(Blocks.CHEST),"Actual container is excluded without modification");
				level.setBlock(obstruction,Blocks.AIR.defaultBlockState(),WorldUpgrades.WRITE_FLAGS);
				plan=WorldUpgrades.preview(level,CX,Y,CZ);saved[0]=plan;
				session.engine.approve(plan.hash(),"fixture-op","fixture-save-before-placement",true,WorldUpgrades.world(level));
				var actual=WorldUpgrades.world(level);int[] writes={0};
				var interrupted=new UpgradeEngine.World() {
					public String refusal(UpgradePlan p,boolean rollback){return actual.refusal(p,rollback);}
					public String policy(){return actual.policy();}
					public String state(UpgradePlan.Point p){return actual.state(p);}
					public boolean set(UpgradePlan.Point p,String expected,String after){boolean changed=actual.set(p,expected,after);if(changed && ++writes[0]==5)throw new SimulatedInterruption();return changed;}
				};
				try{for(int i=0;i<3;i++)session.engine.step(plan.hash(),interrupted,2);throw new AssertionError("Expected injected interruption");}catch(SimulatedInterruption expected){}
				assertCount(level,plan,5);var disk=new UpgradeJournal(session.directory).read(plan.siteId()).orElseThrow();
				check(disk.phase()==UpgradeJournal.Phase.APPLYING && disk.intent()==4,"Fifth-write intent is durable before interrupted return");
			});
		}
		// The actual integrated server stops and reopens this isolated fixture save, not just an in-memory engine.
		try(var world=save.open()) {
			context.waitTicks(20);world.getServer().waitFor(server->server.overworld().getChunkSource().getChunkNow(CX,CZ)!=null);
			world.getServer().runOnServer(server->{
				var level=server.overworld();var session=WorldUpgrades.session(server);var plan=saved[0];
				check(!session.enabled && session.activeHash.isEmpty(),"Restart neither enables nor resumes placement");assertCount(level,plan,5);
				check(session.engine.step(plan.hash(),WorldUpgrades.world(level),2).writes()==0,"Disabled restart remains zero-write");
				WorldUpgrades.enable(server,true,session.mods);
				check(session.engine.step(plan.hash(),WorldUpgrades.world(level),2).writes()==0,"Old-session approval cannot survive enable-policy change");
				session.engine.reauthorize(plan.hash(),"fixture-op","fixture-save-before-placement",true,WorldUpgrades.world(level));
				for(int i=0;i<62;i++)session.engine.step(plan.hash(),WorldUpgrades.world(level),2);
				assertCount(level,plan,123);check(session.engine.findHash(plan.hash()).orElseThrow().phase()==UpgradeJournal.Phase.APPLIED,"Native restart resumes exact structure once");
				check(session.engine.step(plan.hash(),WorldUpgrades.world(level),2).writes()==0,"Repeated native resume cannot duplicate structure");
				boolean duplicate=false;try{session.engine.preview(plan,session.policy());}catch(IllegalStateException expected){duplicate=true;}check(duplicate,"Native site/version tombstone prevents re-preview");
			});
			world.getServer().runCommand("forceload remove 1024 1024");
			world.getServer().waitFor(server->server.overworld().getChunkSource().getChunkNow(CX,CZ)==null,1200);
			world.getServer().runOnServer(server->{
				var s=WorldUpgrades.session(server);check(s.engine.rollback(saved[0].hash(),WorldUpgrades.world(server.overworld()),2).writes()==0,"Unloaded rollback defers");
				check(server.overworld().getChunkSource().getChunkNow(CX,CZ)==null,"Recovery never reloads an unloaded chunk");
			});
			world.getServer().runCommand("forceload add 1024 1024");
			world.getServer().waitFor(server->server.overworld().getChunkSource().getChunkNow(CX,CZ)!=null);
			world.getServer().runOnServer(server->{
				var l=server.overworld();var s=WorldUpgrades.session(server);var plan=saved[0];var edited=plan.writes().getFirst();var pos=WorldUpgrades.pos(edited.point());
				check(l.setBlock(pos,Blocks.AIR.defaultBlockState(),WorldUpgrades.WRITE_FLAGS),"Actual later removal");
				check(l.setBlock(pos,Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),WorldUpgrades.WRITE_FLAGS),"Actual identical replacement");
				check(new UpgradeJournal(s.directory).read(plan.siteId()).orElseThrow().edits().contains(edited.point()),"Mixin durably records native ABA edit before mutation");
				for(int i=0;i<63;i++)s.engine.rollback(plan.hash(),WorldUpgrades.world(l),2);
				check(s.engine.findHash(plan.hash()).orElseThrow().phase()==UpgradeJournal.Phase.CONFLICT,"Conflict-aware native rollback retains later edit");
				check(l.getBlockState(pos).is(Blocks.MOSSY_STONE_BRICKS),"Later identical block is never overwritten");
				for(var cell:plan.cells())if(!cell.point().equals(edited.point()))check(WorldUpgrades.world(l).state(cell.point()).equals(cell.before()),"All unedited native cells restored exactly");
			});
		}
	}
	private static void fixture(ServerLevel level,int cx,int cz) {
		var chunk=level.getChunk(cx,cz);chunk.setInhabitedTime(0);chunk.setAllStarts(Map.of());chunk.setAllReferences(Map.of());
		for(var existing:List.copyOf(chunk.getBlockEntitiesPos()))level.setBlock(existing,Blocks.AIR.defaultBlockState(),WorldUpgrades.WRITE_FLAGS);
		// Deliberate fixture construction is outside the runtime scanner and placement implementation.
		for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int dy=-1;dy<=7;dy++)
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
