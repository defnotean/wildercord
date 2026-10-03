package dev.wildercord.aura;

import dev.wildercord.aura.world.TrainingGrounds;
import dev.wildercord.aura.world.TrainingRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.util.Set;

/** Real terrain, input, rewards and trial progress. Artificial terrain is a deterministic habitat fixture. */
public final class TrainingGroundsTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("fill -18 159 -18 18 159 18 stone");
			world.getServer().runCommand("fill -1 160 -1 1 160 1 stone");
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(p.level(), 0.5, 161, 0.5, Set.<Relative>of(), -90, 0, false);
				p.setGameMode(GameType.SURVIVAL);
				p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				Aura.set(p, new AuraAttachments.Data("stone", AuraRules.GLOW, 0, 0, 0));
			});
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280,720);
				mc.options.guiScale().set(2); mc.resizeGui();
				mc.options.toggleCrouch().set(false);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			context.waitTicks(25);
			// Source pools and ordinary high platforms are not training grounds.
			world.getServer().runCommand("fill 2 160 -1 2 162 1 water");
			context.waitTicks(15);
			world.getServer().runOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				check(TrainingGrounds.ground(p)==TrainingRules.Ground.NONE,"A short/source pool must not qualify");
			});
			world.getServer().runCommand("fill 2 160 -1 2 166 1 air");
			// Cliff lip directs a real source into a falling column next to the bank.
			world.getServer().runCommand("fill 2 167 -2 6 167 2 mossy_cobblestone");
			world.getServer().runCommand("setblock 2 167 0 water");
			context.waitTicks(70);
			context.runOnClient(mc -> mc.options.keyShift.setDown(true));
			context.waitTicks(95);
			world.getServer().runOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				check(TrainingGrounds.ground(p)==TrainingRules.Ground.WATERFALL,"Falling column by dry footing qualifies");
				check(Aura.state(p).breathing(),"Real sneak input starts breathing");
				check(Aura.data(p).xp()>0 && Aura.data(p).practice()>0,"Terrain breathing earns bounded practice");
				check(Aura.aura(p)>0,"Terrain breathing restores Aura");
				Aura.set(p,new AuraAttachments.Data("stone",AuraRules.GLOW,0,0,0));
				TrainingGrounds.breathe(p);
				check(Math.abs(Aura.aura(p)-0.75)<0.001,"A full breath adds the configured 25% recovery bonus");
				check(Math.abs(Aura.data(p).xp()-0.5)<0.001,"Practice rate is applied exactly once");
				Aura.set(p,new AuraAttachments.Data("stone",AuraRules.GLOW,AuraRules.threshold(AuraRules.FLOW),0,0));
				check(AuraBreakthroughs.STILLNESS.equals(AuraBreakthroughs.stillTrial(p)),"Waterfall opens the early stillness trial");
			});
			shot(context,"training_waterfall_first_person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5); shot(context,"training_waterfall_third_person");
			context.waitTicks(AuraRules.stillnessTicks(AuraRules.FLOW)+20);
			world.getServer().runOnServer(server -> check(Aura.stage(server.getPlayerList().getPlayers().getFirst())==AuraRules.FLOW,
				"Unbroken waterfall stance completes an actual breakthrough"));
			context.runOnClient(mc -> mc.options.keyShift.setDown(false)); context.waitTicks(15);
			// Remove the fall and build a small exposed peak, surrounded by lower terrain in mountain biome.
			world.getServer().runCommand("fill -18 160 -18 18 168 18 air");
			world.getServer().runCommand("fill -18 159 -18 18 159 18 air");
			world.getServer().runCommand("fill -18 155 -18 18 155 18 stone");
			world.getServer().runCommand("fill -2 156 -2 2 159 2 stone");
			world.getServer().runCommand("fillbiome -18 156 -18 18 164 18 minecraft:stony_peaks");
			world.getServer().runOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(p.level(),0.5,160,0.5,Set.<Relative>of(),0,15,false);
				TrainingGrounds.broken(p);
			});
			context.waitTicks(45);
			world.getServer().runOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				check(TrainingGrounds.ground(p)==TrainingRules.Ground.SUMMIT,"Exposed mountain relief qualifies");
				Aura.set(p,new AuraAttachments.Data("stone",AuraRules.FLOW,AuraRules.threshold(AuraRules.EDGE),0,AuraRules.PRACTICE_CAP));
				check(AuraBreakthroughs.STILLNESS.equals(AuraBreakthroughs.stillTrial(p)),"Summit opens the early trial");
			});
			context.runOnClient(mc -> mc.options.keyShift.setDown(true)); context.waitTicks(95);
			world.getServer().runOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				check(Aura.data(p).practice()==AuraRules.PRACTICE_CAP,"Idle terrain cannot exceed the existing lifetime cap");
				check(Aura.state(p).stillness()>0,"Summit accumulates real trial progress");
				Aura.set(p,new AuraAttachments.Data("stone",AuraRules.FLOW,200,0,AuraRules.PRACTICE_CAP));
				TrainingGrounds.breathe(p);
				check(Aura.data(p).xp()==200,"Spent practice allowance grants no XP even below the stage threshold");
				Aura.set(p,new AuraAttachments.Data("stone",AuraRules.FLOW,AuraRules.threshold(AuraRules.EDGE),0,AuraRules.PRACTICE_CAP));
			});
			shot(context,"training_summit_third_person");
			world.getServer().runCommand("setblock 0 164 0 stone"); context.waitTicks(45);
			world.getServer().runOnServer(server -> {
				var p=server.getPlayerList().getPlayers().getFirst();
				check(TrainingGrounds.ground(p)==TrainingRules.Ground.NONE,"A roof invalidates summit exposure");
				check(Aura.state(p).stillness()==0,"Losing the terrain clears partial trial progress");
				Aura.set(p,new AuraAttachments.Data("stone",AuraRules.EDGE,AuraRules.threshold(AuraRules.FORM),0,0));
				check(AuraBreakthroughs.stillTrial(p)==null,"Ordinary terrain does not bypass the high-stage tempest requirement");
			});
			context.runOnClient(mc -> mc.options.keyShift.setDown(false));
		}
	}
	private static void shot(ClientGameTestContext c,String name) { c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix()); }
	private static void check(boolean value,String why) { if (!value) throw new AssertionError(why); }
}
