package dev.wildercord.aura.world;

import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.EntityHitResult;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Animals begin away from resources; actual AI must discover and reach them. */
public final class HighlandTravelTest implements FabricClientGameTest {
	private static Stonehorn stone;
	private static Galeclaw gale;
	private static Rimehare hare;
	private static UUID savedGale;
	private static long savedMeal;
	private static final BlockPos FOOD=new BlockPos(4,101,0);
	@Override public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s -> {
				var l=s.overworld();for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
				var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(l,3,103,-6,Set.<Relative>of(),0,20,false);
				l.setBlock(FOOD,HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				stone=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);stone.snapTo(.5,101,.5,0,0);stone.getRandom().setSeed(214);l.addFreshEntity(stone);
				for(int x=7;x<=9;x++)for(int z=11;z<=13;z++)l.setBlock(new BlockPos(x,104,z),Blocks.SPRUCE_LOG.defaultBlockState(),2);
				gale=AuraBeasts.GALECLAW.create(l,EntitySpawnReason.COMMAND);gale.snapTo(3.5,101,12.5,0,0);gale.getRandom().setSeed(316);l.addFreshEntity(gale);
			});c.waitTicks(5);
			w.getServer().runOnServer(s -> {var path=stone.getNavigation().createPath(FOOD,0);check(path!=null && path.canReach(),"A grounded animal's navigation reaches a separate food cell: "+path+", ground="+stone.onGround());});c.waitTicks(420);
			check(w.getServer().computeOnServer(s -> s.overworld().getBlockState(FOOD).getValue(WindreedBlock.AGE)==1),w.getServer().computeOnServer(s -> "Animal must reach and graze a separate crop: position="+stone.position()+", pose="+stone.pose()+", left="+stone.left+", navDone="+stone.getNavigation().isDone()+", path="+stone.getNavigation().getPath()+", crop="+s.overworld().getBlockState(FOOD)+", goals="+stone.getGoalSelector().getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList()));
			check(w.getServer().computeOnServer(s -> gale.pose()==BeastRules.REST && HighlandShelterGoal.covered(gale,gale.blockPosition())),"Animal must reach separate cover and settle");shot(c,"highland_separate_food_and_cover");
			w.getServer().runOnServer(s -> {
				stone.discard();gale.setNoAi(true);var l=s.overworld();l.removeBlock(FOOD,false);
				// A two-block-high wall forces a real detour to the Stonehorn's food.
				for(int z=-11;z<=-9;z++)for(int y=101;y<=102;y++)l.setBlock(new BlockPos(-8,y,z),Blocks.STONE_BRICKS.defaultBlockState(),2);
				l.setBlock(new BlockPos(-6,101,-10),HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				stone=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);stone.snapTo(-9.5,101,-9.5,0,0);stone.getRandom().setSeed(214);l.addFreshEntity(stone);
				l.setBlock(new BlockPos(-6,101,10),HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				hare=Wildlife.RIMEHARE.create(l,EntitySpawnReason.COMMAND);hare.snapTo(-9.5,101,10.5,0,0);hare.getRandom().setSeed(714);l.addFreshEntity(hare);
			});c.waitTicks(460);
			w.getServer().runOnServer(s -> {
				check(s.overworld().getBlockState(new BlockPos(-6,101,-10)).getValue(WindreedBlock.AGE)==1,"Stonehorn follows a detour around a wall: "+stone.position());
				check(s.overworld().getBlockState(new BlockPos(-6,101,10)).getValue(WindreedBlock.AGE)==1,"Rimehare reaches separate forage through its native bounding AI: "+hare.position());
				var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),-7,104,-15,Set.<Relative>of(),0,30,false);
			});c.waitTicks(4);shot(c,"highland_forage_detour");
			w.getServer().runOnServer(s -> {
				stone.discard();hare.discard();var l=s.overworld();l.setBlock(FOOD,HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				stone=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);stone.snapTo(.5,101,.5,0,0);stone.getRandom().setSeed(214);l.addFreshEntity(stone);
			});awaitForage(c,w);
			w.getServer().runOnServer(s -> {
				var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT));
				p.teleportTo(s.overworld(),stone.getX(),101,stone.getZ()-2,Set.<Relative>of(),0,0,false);
			});c.waitTicks(3);
			int id=w.getServer().computeOnServer(s -> stone.getId());
			c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});c.waitTicks(4);
			w.getServer().runOnServer(s -> {
				check(stone.pose()==BeastRules.FORAGE && stone.left>=50 && stone.calmTicks()>190,"Player feeding keeps its own animation and calm when crop navigation stops: pose="+stone.pose()+", left="+stone.left);
				check(!foraging(stone),"Timed player feeding preempts natural crop grazing");
			});shot(c,"highland_offered_meal_interrupts_travel");
			w.getServer().runOnServer(s -> {
				stone.discard();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(s.overworld(),0,104,-8,Set.<Relative>of(),0,20,false);
				stone=AuraBeasts.STONEHORN.create(s.overworld(),EntitySpawnReason.COMMAND);stone.snapTo(.5,101,.5,0,0);stone.getRandom().setSeed(214);s.overworld().addFreshEntity(stone);
			});awaitForage(c,w);
			w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),stone.getX(),101,stone.getZ()-2,Set.<Relative>of(),0,0,false);});c.waitTicks(12);
			w.getServer().runOnServer(s -> {check(stone.getTarget()==s.getPlayerList().getPlayers().getFirst() && stone.pose()==BeastRules.WARN && !foraging(stone),"Unsafe proximity preempts crop travel with an authored warning");});shot(c,"highland_grazer_travel_warning");
			w.getServer().runOnServer(s -> {
				stone.discard();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(s.overworld(),0,104,-8,Set.<Relative>of(),0,20,false);
				// Fully sealed crop: the nearest valid plant must not be eaten through a wall.
				var l=s.overworld();l.removeBlock(FOOD,false);
				for(int x=4;x<=6;x++)for(int z=-1;z<=1;z++)for(int y=101;y<=104;y++)if(x!=5 || z!=0)l.setBlock(new BlockPos(x,y,z),Blocks.STONE_BRICKS.defaultBlockState(),2);
				l.setBlock(new BlockPos(5,101,0),HighlandContent.REED.defaultBlockState().setValue(WindreedBlock.AGE,2),2);
				stone=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);stone.snapTo(1.5,101,.5,0,0);stone.getRandom().setSeed(214);l.addFreshEntity(stone);
			});c.waitTicks(360);
			w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(new BlockPos(5,101,0)).getValue(WindreedBlock.AGE)==2 && !foraging(stone),"Unreachable crop is refused without remote grazing");gale.ate();savedGale=gale.getUUID();savedMeal=gale.fedUntil();});
			save=w.getWorldSave();
		}
		try(var w=save.open()) {
			c.waitTicks(30);w.getServer().runOnServer(s -> {var loaded=s.overworld().getEntity(savedGale);check(loaded instanceof Galeclaw g && g.fedUntil()==savedMeal && !g.hungry(),"A complete server restart retains the same Galeclaw and exact meal deadline");});
		}
	}
	private static boolean foraging(Stonehorn b) {return b.getGoalSelector().getAvailableGoals().stream().anyMatch(g -> g.isRunning() && g.getGoal() instanceof WindreedForageGoal);}
	private static void awaitForage(ClientGameTestContext c,TestSingleplayerContext w) {for(int i=0;i<60;i++) {if(w.getServer().computeOnServer(s -> foraging(stone)))return;c.waitTicks(5);}throw new AssertionError("Crop navigation never began: "+w.getServer().computeOnServer(s -> stone.position()+", pose="+stone.pose()));}
	private static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);}
	private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
