package dev.wildercord.aura;

import dev.wildercord.client.MasterFormsClient;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.CircleVows;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Priority 2 smoke: an old Circle XX save without vows loads at baseline, a real player takes, pays to release and
 * re-takes a vow through the player command, the choice survives a restart, and a real Wall Turn brace produces
 * the segmented articulated frame when articulated presentation is on.
 */
public final class ProgressionVowWallTurnNativeTest implements FabricClientGameTest {
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	private static final double[] BASE = new double[2];

	@Override public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(35);
			context.runOnClient(mc -> mc.gui.setScreen(null));
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				// An old save: twenty circles, saved before vows existed.
				p.setAttached(WildercordAttachments.CIRCLES, 20);
				p.removeAttached(WildercordAttachments.CIRCLE_VOWS);
				check(Heart.vows(p) == 0 && Heart.vowEffect(p).equals(CircleVows.Effect.NONE), "An old save holds no vow and no vow effect");
				check(CircleVows.open(Heart.vows(p), Heart.active(p)).size() == CircleVows.ALL.size(), "An old Circle XX heart is offered every vow");
				var b = Heart.bonuses(p);
				BASE[0] = b.power(); BASE[1] = b.cost();
				p.experienceLevel = 0;
			});
			context.runOnClient(mc -> mc.player.connection.sendCommand("vow take keen_edge"));
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(CircleVows.choice(Heart.vows(p), CircleVows.at(11)) == CircleVows.FIRST, "The player command takes Keen Edge");
				check(Math.abs(Heart.bonuses(p).power() / BASE[0] - 1.06) < 1e-6, "Keen Edge makes spells 6% stronger");
			});
			context.runOnClient(mc -> mc.player.connection.sendCommand("vow take spare_hand"));
			context.runOnClient(mc -> mc.player.connection.sendCommand("vow release 11"));
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(CircleVows.choice(Heart.vows(p), CircleVows.at(11)) == CircleVows.FIRST, "Neither the other side nor an unpaid release changes the vow");
				p.giveExperienceLevels(CircleVows.RELEASE_LEVELS);
			});
			context.runOnClient(mc -> mc.player.connection.sendCommand("vow release 11"));
			context.waitTicks(5);
			context.runOnClient(mc -> mc.player.connection.sendCommand("vow take spare_hand"));
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(p.experienceLevel == 0, "The release cost exactly " + CircleVows.RELEASE_LEVELS + " levels");
				check(CircleVows.choice(Heart.vows(p), CircleVows.at(11)) == CircleVows.SECOND, "After a paid release the other side can be taken");
				check(Math.abs(Heart.bonuses(p).power() / BASE[0] - 1) < 1e-6 && Math.abs(Heart.bonuses(p).cost() / BASE[1] - .94) < 1e-6,
					"Spare Hand replaces Keen Edge: 6% cheaper, no stronger");
				p.setAttached(WildercordAttachments.CIRCLES, 10);
				check(Heart.vowEffect(p).equals(CircleVows.Effect.NONE), "An unformed Circle XI silences its vow");
				p.setAttached(WildercordAttachments.CIRCLES, 20);
			});
			context.waitTicks(10);
			save = world.getWorldSave();
		}
		try (var world = save.open()) {
			context.waitTicks(35);
			context.runOnClient(mc -> mc.gui.setScreen(null));
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(Heart.circles(p) == 20 && CircleVows.choice(Heart.vows(p), CircleVows.at(11)) == CircleVows.SECOND, "Circles and the chosen vow survive a restart");
				check(Math.abs(Heart.vowEffect(p).cost() - .94) < 1e-6, "The reloaded vow still applies");
				for (int x = -8; x <= 12; x++) for (int z = -8; z <= 8; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 108; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				for (int y = 100; y <= 105; y++) for (int z = -3; z <= 3; z++) p.level().setBlockAndUpdate(new BlockPos(-1, y, z), Blocks.STONE.defaultBlockState());
				p.setGameMode(GameType.SURVIVAL); p.teleportTo(.5, 100, .5); p.setDeltaMovement(Vec3.ZERO);
				p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
				p.setAttached(dev.wildercord.aura.world.MasterVictories.RECORD,
					dev.wildercord.aura.world.MasterVictoryRules.Progress.NONE.withClear(dev.wildercord.aura.world.MastersRules.GALE));
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, MasterForms.WALL_TURN, 0, false, false));
				p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			});
			context.waitFor(mc -> MasterForms.data(mc.player).equipped() == MasterForms.WALL_TURN, 40);
			String previous = System.getProperty(ArticulatedCombat.ENABLE_PROPERTY);
			System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, "true");
			try {
				// Let the teleport, footing and the reopened session settle before the real jump.
				context.waitFor(mc -> mc.player.onGround() && Math.abs(mc.player.getX() - .5) < .01 && Math.abs(mc.player.getZ() - .5) < .01, 60);
				context.waitTicks(20);
				context.getInput().holdKey(o -> o.keyJump); context.waitTicks(2); context.getInput().releaseKey(o -> o.keyJump);
				context.getInput().pressKey(MasterFormsClient.mapping());
				try {
					context.waitFor(mc -> MasterForms.view(mc.player).phase() == WallTurnRules.BRACE, 10);
				} catch (AssertionError failure) {
					world.getServer().runOnServer(server -> {
						var p = server.getPlayerList().getPlayers().getFirst();
						System.out.println("PROG_WALL_TURN_DIAG pos=" + p.position() + " ground=" + p.onGround() + " data=" + MasterForms.data(p)
							+ " eligible=" + MasterForms.eligibleLesson(p) + " aura=" + Aura.aura(p) + " stage=" + Aura.stage(p)
							+ " weapon=" + Aura.holdsWeapon(p) + " enabled=" + Aura.enabled(p) + " spent=" + Awakening.spent(p) + " time=" + p.level().getGameTime());
					});
					throw failure;
				}
				context.waitFor(mc -> {
					var playback = MasterFormsClient.timeline(mc.player);
					if (playback == null || playback.event().phase() != WallTurnRules.BRACE) return false;
					var frame = state(mc).getData(ArticulatedCombat.FRAME);
					return frame != null && frame.move() == -2 - WallTurnRules.BRACE && frame.pose().weight() > 0;
				}, 4);
				// Kick inside the ten-tick brace window, as the real lesson test does.
				context.waitTicks(1);
				context.getInput().pressKey(MasterFormsClient.mapping());
				context.waitFor(mc -> MasterForms.view(mc.player).phase() == WallTurnRules.KICK, 10);
				var samples = new java.util.ArrayList<String>();
				try {
					context.waitFor(mc -> {
						var frame = state(mc).getData(ArticulatedCombat.FRAME);
						var playback = MasterFormsClient.timeline(mc.player);
						var movement = state(mc).getData(dev.wildercord.client.MastersArtPose.FRAME);
						samples.add("t=" + mc.level.getGameTime() + " event=" + (playback == null ? null : playback.event().phase() + "/" + playback.event().ticks() + "@" + playback.received())
							+ " movement=" + (movement == null ? null : movement.move()) + " frame=" + (frame == null ? null : frame.move() + "/" + frame.pose().weight()));
						return frame != null && frame.move() == -2 - WallTurnRules.KICK && frame.pose().weight() > 0;
					}, 10);
				} catch (AssertionError failure) {
					System.out.println("PROG_WALL_TURN_KICK_SAMPLES " + samples);
					throw failure;
				}
				context.waitTicks(24);
				world.getServer().runOnServer(server -> {
					var p = server.getPlayerList().getPlayers().getFirst();
					check(p.getX() > 3.5 && p.getX() <= 4.6, "The articulated presentation leaves the server kick unchanged");
				});
			} finally {
				if (previous == null) System.clearProperty(ArticulatedCombat.ENABLE_PROPERTY);
				else System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, previous);
			}
		}
	}

	private static AvatarRenderState state(Minecraft mc) {
		return (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 0);
	}
}
