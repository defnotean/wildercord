package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Exercises the real Cord cast path and photographs the formation and release beats. */
public class WildercordMagicReleaseTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
			context.waitTicks(60);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1600, 900);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.gui.toastManager().clear();
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) book = book.learn(rune.id());
				Spellbooks.set(player, book);
				Spellbooks.setMana(player, 400);
				player.setYRot(0);
				player.setXRot(0);
				player.teleportTo(player.level(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false);
			});
			context.waitTicks(40);
			// Build after the target chunks have loaded; world generation can otherwise
			// replace a floor filled too early and the camera falls out of the stage.
			world.getServer().runCommand("fill -68 159 -8 68 159 40 polished_deepslate");
			world.getServer().runCommand("fill -68 160 24 68 174 24 black_concrete");
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
				.teleportTo(server.overworld(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false));
			context.waitTicks(20);
			cast(context, world, "beam_fire_wind", Runes.BEAM, Runes.FIRE, Runes.WINDCUT);
			cast(context, world, "bolt_fire_wind", Runes.BOLT, Runes.FIRE, Runes.WINDCUT);
			cast(context, world, "spark_storm", Runes.SPARK, Runes.SHOCK);
			cast(context, world, "cone_fire", Runes.CONE, Runes.FIRE);
			cast(context, world, "ring_frost", Runes.RING, Runes.FROST);
			cast(context, world, "self_wind", Runes.SELF, Runes.SWIFT);
			cast(context, world, "rain_fire_wind", Runes.RAIN, Runes.FIRE, Runes.WINDCUT);
			cast(context, world, "zone_life", Runes.ZONE, Runes.GROW);
			for (RuneDef rune : Runes.all()) if (rune.family() == dev.wildercord.spell.RuneFamily.SHAPE) {
				world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
					.teleportTo(server.overworld(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false));
				context.waitTicks(3);
				cast(context, world, "shape_" + rune.path(), rune, Runes.FIRE);
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			world.getServer().runOnServer(server -> {
				var player=server.getPlayerList().getPlayers().getFirst();
				SpellCaster.edit(player,0,List.of(Runes.BEAM.id(),Runes.FIRE.id(),Runes.WINDCUT.id()));
				Spellbooks.setReadyAt(player,0,0);Spellbooks.setMana(player,400);SpellCaster.cast(player,0);
			});
			context.waitTicks(2);
			context.runOnClient(mc -> {mc.player.setYRot(180);mc.player.setXRot(-20);});
			world.getServer().runOnServer(server -> {var player=server.getPlayerList().getPlayers().getFirst();player.setYRot(180);player.setXRot(-20);});
			context.waitTicks(2);
			shot(context,"turn_rear_circle");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			shot(context,"turn_first_person_clearance");
		}
	}

	private static void cast(ClientGameTestContext context, TestSingleplayerContext world, String name, RuneDef... runes) {
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		List<String> ids = Arrays.stream(runes).map(RuneDef::id).toList();
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			if (!player.level().getBlockState(new BlockPos(0, 159, 0)).is(Blocks.POLISHED_DEEPSLATE)
					|| player.getY() < 159.5) {
				throw new AssertionError("magic screenshot stage is missing");
			}
			SpellCaster.edit(player, 0, ids);
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, 400);
			SpellCaster.cast(player, 0);
		});
		context.waitTicks(2);
		shot(context, name + "_form");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		shot(context, name + "_first_person");
		context.waitTicks(2);
		shot(context, name + "_first_person_release");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		shot(context, name + "_release");
		context.waitTicks(50);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of("magic_" + name).disableCounterPrefix());
	}
}
