package dev.wildercord.aura.world;

import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import java.util.*;

/** Actual cover goals, predator avoidance, finite meals, saved satiety and attack interruption. */
public final class HighlandEcologyTest implements FabricClientGameTest {
	private static Galeclaw runner;
	private static Stonehorn grazer;
	private static Rimehare hare;
	private static Galeclaw probe;
	@Override public void runTest(ClientGameTestContext c) {
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s -> {
				for(int x=-28;x<=28;x++)for(int z=-28;z<=28;z++)s.overworld().setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
				var p=p(s);p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),.5,101,-5,Set.<Relative>of(),0,0,false);
				runner=AuraBeasts.GALECLAW.create(s.overworld(),EntitySpawnReason.COMMAND);runner.snapTo(.5,101,2,0,0);runner.setNoAi(true);s.overworld().addFreshEntity(runner);
				hare=Wildlife.RIMEHARE.create(s.overworld(),EntitySpawnReason.COMMAND);hare.snapTo(.5,101,5,0,0);s.overworld().addFreshEntity(hare);
				check(hare.boltsFrom(runner),"Hungry predator is recognized as a threat");
				for(int x=18;x<=22;x++)for(int z=8;z<=12;z++)s.overworld().setBlock(new BlockPos(x,104,z),Blocks.SPRUCE_LOG.defaultBlockState(),2);
				probe=AuraBeasts.GALECLAW.create(s.overworld(),EntitySpawnReason.COMMAND);probe.snapTo(20.5,101,10.5,0,0);s.overworld().addFreshEntity(probe);
			});
			c.waitTicks(110);
			w.getServer().runOnServer(s -> {
				check(probe.pose()==BeastRules.REST,"A covered hunter settles after a bounded search interval: pose="+probe.pose()+", position="+probe.position()+", covered="+HighlandShelterGoal.covered(probe,probe.blockPosition())+", tick="+probe.tickCount+", goals="+probe.getGoalSelector().getAvailableGoals().stream().map(g -> g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList());probe.discard();
			});
			c.waitTicks(45);
			w.getServer().runOnServer(s -> {check(hare.distanceToSqr(runner)>16,"Registered avoidance goal moves actual prey away from the hungry predator");hare.setNoAi(true);});
			shot(c,"highland_prey_escape");
			w.getServer().runOnServer(s -> {
				runner.setNoAi(false);runner.tickCount=39;runner.pose(BeastRules.IDLE,0);
				var meat=new ItemEntity(s.overworld(),runner.getX(),runner.getY(),runner.getZ(),new ItemStack(Items.RABBIT,2));s.overworld().addFreshEntity(meat);
			});
			c.waitTicks(4);
			w.getServer().runOnServer(s -> {
				check(!runner.hungry() && !hare.boltsFrom(runner),"Scavenged meal makes the animal sated and removes hunting fear");
				int meat=s.overworld().getEntitiesOfClass(ItemEntity.class,runner.getBoundingBox().inflate(3),e -> e.getItem().is(Items.RABBIT)).stream().mapToInt(e -> e.getItem().getCount()).sum();
				check(meat==1,"A meal consumes exactly one dropped food");
				s.overworld().getEntitiesOfClass(ItemEntity.class,runner.getBoundingBox().inflate(3),e -> e.getItem().is(Items.RABBIT)).forEach(Entity::discard);
				var out=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess());runner.saveWithoutId(out);
				var loaded=AuraBeasts.GALECLAW.create(s.overworld(),EntitySpawnReason.LOAD);loaded.load(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess(),out.buildResult()));
				check(loaded.fedUntil()==runner.fedUntil() && !loaded.hungry(),"Satiety deadline survives entity save/load without being renewed");loaded.discard();
				hare.snapTo(runner.getX()+4,101,runner.getZ(),0,0);runner.pose(BeastRules.IDLE,0);runner.tickCount=99;
			});
			c.waitTicks(30);check(w.getServer().computeOnServer(s -> runner.getTarget()==null && hare.getHealth()==hare.getMaxHealth()),"Sated predator leaves nearby prey alone");
			// Let the actual deadline expire, rather than deleting the saved state to make this test pass.
			c.waitTicks(HighlandRules.MEAL_REST+1);
			check(w.getServer().computeOnServer(s -> runner.hungry()),"Actual world ticks expire the meal");
			w.getServer().runOnServer(s -> {
				runner.setNoAi(true);runner.setTarget(null);hare.discard();runner.discard();
				grazer=AuraBeasts.STONEHORN.create(s.overworld(),EntitySpawnReason.COMMAND);grazer.snapTo(-3.5,101,3,0,0);s.overworld().addFreshEntity(grazer);
				runner=AuraBeasts.GALECLAW.create(s.overworld(),EntitySpawnReason.COMMAND);runner.snapTo(3.5,101,3,0,0);s.overworld().addFreshEntity(runner);
				for(int x=-7;x<=7;x++)for(int z=0;z<=6;z++)s.overworld().setBlock(new BlockPos(x,104,z),Blocks.SPRUCE_LOG.defaultBlockState(),2);
			});
			c.waitTicks(110);
			check(w.getServer().computeOnServer(s -> runner.pose()==BeastRules.REST && grazer.pose()!=BeastRules.REST),
				w.getServer().computeOnServer(s -> "Covered hunter rests at midday while the grazer stays awake: hunter="+runner.pose()+", grazer="+grazer.pose()+", clock="+s.overworld().getOverworldClockTime()+", covered="+HighlandShelterGoal.covered(runner,runner.blockPosition())));photo(c,w,runner,"highland_midday_roost");
			w.getServer().runCommand("time set 16000");c.waitTicks(110);
			check(w.getServer().computeOnServer(s -> grazer.pose()==BeastRules.REST && runner.pose()!=BeastRules.REST),"Covered grazer rests at night while the hunter wakes");photo(c,w,grazer,"highland_night_grazer");
			// Put the awake runner beneath the same roof before testing its weather response. Shelter travel is a separate scenario.
			w.getServer().runOnServer(s -> runner.teleportTo(3.5,101,3));
			w.getServer().runCommand("weather rain");c.waitTicks(110);
			check(w.getServer().computeOnServer(s -> grazer.pose()==BeastRules.REST && runner.pose()==BeastRules.REST),"Rain makes both species shelter");photo(c,w,runner,"highland_rain_shelter");photo(c,w,grazer,"highland_rain_grazer");
			w.getServer().runOnServer(s -> {
				var p=p(s);p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),grazer.getX(),101,grazer.getZ()-5,Set.<Relative>of(),0,0,false);
				grazer.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p),1);
			});c.waitTicks(3);
			check(w.getServer().computeOnServer(s -> grazer.getTarget()==p(s) && grazer.pose()!=BeastRules.REST),"Damage interrupts rest and preserves defensive retaliation");
		}
	}
	private static void photo(ClientGameTestContext c,TestSingleplayerContext w,AuraBeast beast,String name) {
		w.getServer().runOnServer(s -> {
			var p=p(s);p.setGameMode(GameType.CREATIVE);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1200,0,false,false));
			var at=beast.position().add(3.5,0,-3.5);var d=beast.getBoundingBox().getCenter().subtract(at.add(0,p.getEyeHeight(),0));
			p.teleportTo(s.overworld(),at.x,at.y,at.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);
		});c.waitTicks(4);shot(c,name);
	}
	private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
	private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
	private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
