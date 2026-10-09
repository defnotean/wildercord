package dev.wildercord.aura;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.client.MasterFormsClient;
import dev.wildercord.client.MasterFormsScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Stone teacher lesson, equip in the shared slot, and a real zombie blow turned aside or left alone on a Survival save. */
public final class StoneHingeLessonTest implements FabricClientGameTest {
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	/** Aura read the moment the paid brace is published, like Wall Turn's payment check. */
	private static double paid;
	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			// Stone Hinge is gated behind a server switch until its peer and latency checks pass: this test switches it on and restores it.
			String settings = world.getServer().computeOnServer(server -> {
				try { return java.nio.file.Files.readString(dev.wildercord.config.Config.path()); }
				catch (java.io.IOException e) { throw new AssertionError("couldn't read the settings: " + e); }
			});
			try { lesson(context, world); }
			finally {
				world.getServer().runOnServer(server -> {
					try { java.nio.file.Files.writeString(dev.wildercord.config.Config.path(), settings); }
					catch (java.io.IOException e) { throw new AssertionError("couldn't restore the settings: " + e); }
					dev.wildercord.config.Config.reload(server);
				});
			}
		}
	}
	/** Switches Stone Hinge's experimental gate in the live settings (as an edited file and a reload would) and tells the client. */
	private static void gate(net.minecraft.server.MinecraftServer server, boolean on) {
		try {
			java.nio.file.Path path = dev.wildercord.config.Config.path();
			java.nio.file.Files.writeString(path, java.nio.file.Files.readString(path)
				.replaceAll("\"experimental_stone_hinge\": (true|false)", "\"experimental_stone_hinge\": " + on));
			dev.wildercord.config.Config.reload(server);
		} catch (java.io.IOException e) {
			throw new AssertionError("couldn't switch Stone Hinge: " + e);
		}
		check(dev.wildercord.config.Config.get().aura().sparring().experimentalStoneHinge() == on, "Stone Hinge's switch is " + on);
	}
	private static void lesson(ClientGameTestContext context, TestSingleplayerContext world) {
		{
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set midnight");
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.StoneHingeLessonTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 106; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				p.setGameMode(GameType.SURVIVAL); p.teleportTo(.5, 100, .5); p.setDeltaMovement(Vec3.ZERO);
				p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.STONE));
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.setShiftKeyDown(true);
				var teacher = AuraWorld.DUELIST.create(p.level(), EntitySpawnReason.COMMAND);
				check(teacher != null, "The wandering-teacher fixture exists");
				teacher.setMethod(BreathingMethods.STONE); teacher.setNoAi(true); teacher.snapTo(2.5, 100, .5, 90, 0); p.level().addFreshEntity(teacher);
				// Switched off, an otherwise eligible swordsman is neither offered nor taught it, and an old save that knows it can't equip it.
				gate(server, false);
				check(!MasterForms.eligibleLesson(p, MasterForms.STONE_HINGE) && !MasterFormLessons.offer(p, teacher) && !MasterForms.learn(p, MasterForms.STONE_HINGE)
					&& !MasterForms.data(p).hingeLearned(), "With the switch off the Stone teacher neither offers nor teaches Stone Hinge");
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(false, 0, 0, false, false, 0, 0, true));
			});
			context.waitFor(mc -> MasterForms.data(mc.player).hingeLearned() && !MasterForms.testedHinge(mc.player), 40);
			context.runOnClient(mc -> MasterFormsClient.send(StoneHingeRules.EQUIP));
			context.waitTicks(6);
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				check(MasterForms.data(p).hingeLearned() && MasterForms.data(p).equipped() == 0, "With the switch off a learned Stone Hinge stays learned but can't be equipped");
				p.setAttached(MasterForms.PROGRESS, MasterForms.Progress.NONE);
				gate(server, true);
				p.setShiftKeyDown(true);
				var teacher = p.level().getEntities(AuraWorld.DUELIST, d -> true).getFirst();
				teacher.interact(p, InteractionHand.MAIN_HAND, teacher.position());
				check(!MasterForms.data(p).hingeLearned() && !MasterForms.data(p).learned(), "The Stone offer neither learns nor equips");
				p.setShiftKeyDown(false);
			});
			context.waitFor(mc -> mc.gui.screen() instanceof MasterFormsScreen, 40);
			check(context.computeOnClient(mc -> {
				String said = mc.gui.screen().getNarrationMessage().getString();
				String stone = net.minecraft.network.chat.Component.translatable("book.wildercord.stone_hinge.1").getString();
				String wall = net.minecraft.network.chat.Component.translatable("book.wildercord.wall_turn.1").getString();
				return !stone.equals("book.wildercord.stone_hinge.1") && !wall.equals("book.wildercord.wall_turn.1") && said.contains(stone) && !said.contains(wall);
			}), "A Stone teacher narrates Stone Hinge's first story page in full and none of Wall Turn's");
			resizeFocus(context, "next"); keyboard(context, "next"); keyboard(context, "next");
			context.takeScreenshot(TestScreenshotOptions.of("stone_hinge_teacher_illustration").disableCounterPrefix());
			resizeFocus(context, "accept"); keyboard(context, "accept");
			context.waitFor(mc -> MasterForms.data(mc.player).hingeLearned(), 40);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(MasterForms.data(p).hingeLearned() && !MasterForms.data(p).learned() && MasterForms.data(p).equipped() == 0, "Only Stone Hinge is learned; the slot stays empty");
				check(Aura.data(p).method().equals("ember") && Aura.data(p).xp() == 4500, "The lesson changes neither method nor XP");
				boolean book = false;
				for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++)
					book |= dev.wildercord.Wildercord.id("stone_hinge_lesson").equals(p.getInventory().getItem(slot).get(DataComponents.ITEM_MODEL));
				check(book, "The physical Gatepost Ledger copy is given");
				p.level().getEntities(AuraWorld.DUELIST, d -> true).forEach(d -> d.discard());
				p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); p.inventoryMenu.broadcastChanges();
			});
			context.waitTicks(4); resizeFocus(context, "equip"); keyboard(context, "equip");
			context.waitFor(mc -> MasterForms.data(mc.player).equipped() == MasterForms.STONE_HINGE, 40);
			context.takeScreenshot(TestScreenshotOptions.of("stone_hinge_equipped_slot").disableCounterPrefix());
			context.runOnClient(mc -> {
				for (int page = 1; page <= 3; page++) check(mc.font.split(net.minecraft.network.chat.Component.translatable("book.wildercord.stone_hinge." + page), 114).size() <= 14,
					"Every physical-book page fits the native readable area");
				mc.gui.setScreen(null);
			});
			// One slot, one shared rest: a Stone Hinge rest refuses a field form, and a field-form rest refuses Stone Hinge.
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.STONE).withClear(FormDashRules.school(FormDashRules.CINDER_LUNGE)));
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(false, MasterForms.STONE_HINGE, MasterForms.now(p) + WallTurnRules.REST_TICKS, false, false, 0, 0, true));
				p.setAttached(FormDash.PROGRESS, new FormDash.Progress(FormDashRules.bit(FormDashRules.CINDER_LUNGE), 0, 0, 0, 0));
				check(FormDash.eligible(p, FormDashRules.CINDER_LUNGE), "The field form is otherwise free to equip");
			});
			context.waitTicks(2);
			context.runOnClient(mc -> dev.wildercord.client.FormDashClient.send(WallTurnRules.EQUIP, FormDashRules.CINDER_LUNGE));
			context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(FormDash.data(p).equipped() == 0 && MasterForms.data(p).equipped() == MasterForms.STONE_HINGE, "Stone Hinge's rest refuses equipping a field form");
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(false, 0, 0, false, false, 0, 0, true));
				p.setAttached(FormDash.PROGRESS, new FormDash.Progress(FormDashRules.bit(FormDashRules.CINDER_LUNGE), FormDashRules.CINDER_LUNGE,
					MasterForms.now(p) + FormDashRules.CINDER_REST, 0, 0));
			});
			context.waitTicks(2);
			context.runOnClient(mc -> MasterFormsClient.send(StoneHingeRules.EQUIP));
			context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(MasterForms.data(p).equipped() == 0 && FormDash.data(p).equipped() == FormDashRules.CINDER_LUNGE, "A field form's rest refuses equipping Stone Hinge");
				var f = FormDash.data(p);
				p.setAttached(FormDash.PROGRESS, new FormDash.Progress(f.learned(), f.equipped(), 0, 0, f.practiced()));
			});
			context.waitTicks(2);
			context.runOnClient(mc -> MasterFormsClient.send(StoneHingeRules.EQUIP));
			context.waitFor(mc -> MasterForms.data(mc.player).equipped() == MasterForms.STONE_HINGE, 20);
			world.getServer().runOnServer(server -> check(FormDash.data(server.getPlayerList().getPlayers().getFirst()).equipped() == 0,
				"Once rested, equipping Stone Hinge empties the field-form slot"));
			world.getServer().runCommand("tp @a 0.5 100 0.5 0 0");
			context.waitTicks(5);
			// No strafe: refused before payment.
			context.getInput().pressKey(MasterFormsClient.mapping()); context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Math.abs(Aura.aura(p) - 160) < .001 && !MasterForms.committed(p), "Without a strafe the plant refuses and charges nothing");
			});
			// A drop on the held side refuses before payment: left strafe at yaw 0 is +X, and the floor there is gone.
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				for (int x = 1; x <= 2; x++) for (int z = -1; z <= 1; z++) for (int y = 96; y <= 99; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				check(!WallTurn.hingeLane(p, new Vec3(1, 0, 0)) && WallTurn.hingeLane(p, new Vec3(-1, 0, 0)), "Only the side over the drop is unsafe");
			});
			context.getInput().holdKey(o -> o.keyLeft); context.waitTicks(1);
			context.getInput().pressKey(MasterFormsClient.mapping()); context.waitTicks(4);
			context.getInput().releaseKey(o -> o.keyLeft);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Math.abs(Aura.aura(p) - 160) < .001 && !MasterForms.committed(p) && !MasterForms.ownsMotion(p), "A plant beside a drop refuses and charges nothing");
				for (int x = 1; x <= 2; x++) for (int z = -1; z <= 1; z++) for (int y = 96; y <= 99; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
			});
			context.waitTicks(3);
			// Left strafe at yaw 0 is +X. A zombie in front (+Z) knocks toward -Z; the hinge turns that to +X.
			Vec3 turned = strike(context, world, 0, 1.5);
			check(turned.x > .25 && Math.abs(turned.z) < .12, "The frontal blow's impulse turns toward the held side: " + turned);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Aura.aura(p) <= paid + .001, "The turn neither refunds nor charges again: " + Aura.aura(p) + " after " + paid);
				check(p.getHealth() < p.getMaxHealth(), "The blow's damage still lands");
			});
			context.waitFor(mc -> MasterForms.view(mc.player).phase() == StoneHingeRules.TURN, 5);
			// After the recovery, a press inside the shared rest is refused and charges nothing.
			context.waitTicks(StoneHingeRules.RECOVERY_TICKS + 3);
			double rested = world.getServer().computeOnServer(server -> Aura.aura(server.getPlayerList().getPlayers().getFirst()));
			context.getInput().holdKey(o -> o.keyLeft); context.waitTicks(1);
			context.getInput().pressKey(MasterFormsClient.mapping()); context.waitTicks(4);
			context.getInput().releaseKey(o -> o.keyLeft);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(MasterForms.now(p) < MasterForms.data(p).readyAt(), "The fixture is still inside the rest");
				check(Math.abs(Aura.aura(p) - rested) < .001 && !MasterForms.ownsMotion(p), "A press during the rest is refused before payment");
			});
			context.waitTicks(WallTurnRules.REST_TICKS + 5);
			// A blow from behind is not a frontal catch: refused, and the native knockback and damage stand.
			float health = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().getHealth());
			Vec3 refused = strike(context, world, 0, -1.5);
			check(refused.z > .25 && Math.abs(refused.x) < .12, "A blow from behind keeps the ordinary knockback: " + refused);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Aura.aura(p) <= paid + .001, "A refused turn does not refund the paid plant: " + Aura.aura(p) + " after " + paid);
				check(p.getHealth() < health, "A refused turn keeps the ordinary damage");
				long owed = MasterForms.data(p).recoveryUntil() - MasterForms.now(p);
				check(MasterForms.view(p).phase() != StoneHingeRules.TURN && !MasterForms.ownsMotion(p), "The refused catch ends without a turn");
				check(owed > 0 && owed <= StoneHingeRules.RECOVERY_TICKS, "The refused catch, like every ending, owes the normal recovery: " + owed);
			});
			context.waitTicks(WallTurnRules.REST_TICKS + 5);
			// An oblique blow, 45 degrees off the planted facing, is either refused or sent purely sideways: never forward or back.
			Vec3 oblique = strike(context, world, 1.5 * Math.sqrt(.5), 1.5 * Math.sqrt(.5));
			// Turned: all of it toward the held side (+X). Refused: the native shove away from the zombie at (+X, +Z) stands untouched.
			check(oblique.x > .25 && Math.abs(oblique.z) < .05 || oblique.x < -.1 && oblique.z < -.1 && Math.abs(oblique.x - oblique.z) < .05,
				"An oblique blow is either refused or sent purely sideways, never forward or back: " + oblique);
			context.waitTicks(WallTurnRules.REST_TICKS + 5);
			// Jumping off the plant ends it: no refund, and the normal recovery.
			world.getServer().runCommand("tp @a 0.5 100 0.5 0 0");
			context.waitTicks(3);
			long recoveredAt = world.getServer().computeOnServer(server -> MasterForms.data(server.getPlayerList().getPlayers().getFirst()).recoveryUntil());
			context.getInput().holdKey(o -> o.keyLeft); context.waitTicks(1);
			context.getInput().pressKey(MasterFormsClient.mapping());
			context.waitFor(mc -> MasterForms.view(mc.player).phase() == StoneHingeRules.BRACE, 10);
			double jumpPaid = world.getServer().computeOnServer(server -> Aura.aura(server.getPlayerList().getPlayers().getFirst()));
			context.getInput().releaseKey(o -> o.keyLeft);
			context.getInput().holdKey(o -> o.keyJump);
			context.waitFor(mc -> MasterForms.data(mc.player).recoveryUntil() > recoveredAt, 20);
			context.getInput().releaseKey(o -> o.keyJump);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				long left = MasterForms.data(p).recoveryUntil() - MasterForms.now(p);
				check(!MasterForms.ownsMotion(p) && Aura.aura(p) <= jumpPaid + .001, "A jump cancels the plant with no refund: " + Aura.aura(p) + " after " + jumpPaid);
				check(left > 0 && left <= StoneHingeRules.RECOVERY_TICKS && MasterForms.committed(p), "The cancelled plant owes the normal recovery: " + left);
			});
		}
	}
	/** Plants with a held left strafe, waits for the catch, and lets a real zombie strike from {@code (dx, dz)}. Returns the server velocity. */
	private static Vec3 strike(ClientGameTestContext context, TestSingleplayerContext world, double dx, double dz) {
		world.getServer().runCommand("tp @a 0.5 100 0.5 0 0");
		context.waitTicks(3);
		context.getInput().holdKey(o -> o.keyLeft); context.waitTicks(1);
		double before = world.getServer().computeOnServer(server -> Aura.aura(server.getPlayerList().getPlayers().getFirst()));
		context.getInput().pressKey(MasterFormsClient.mapping());
		context.waitFor(mc -> MasterForms.view(mc.player).phase() == StoneHingeRules.BRACE, 10);
		paid = world.getServer().computeOnServer(server -> Aura.aura(server.getPlayerList().getPlayers().getFirst()));
		check(Math.abs(before - paid - StoneHingeRules.COST) < .1, "The plant pays 20 Aura once: " + before + " to " + paid);
		context.waitFor(mc -> MasterForms.view(mc.player).phase() == StoneHingeRules.CATCH, 20);
		context.getInput().releaseKey(o -> o.keyLeft);
		return world.getServer().computeOnServer(server -> {
			var p = server.getPlayerList().getPlayers().getFirst();
			check(MasterForms.data(p).equipped() == MasterForms.STONE_HINGE && MasterForms.catching(p), "The planted body is catching");
			p.setDeltaMovement(Vec3.ZERO);
			var zombie = EntityTypes.ZOMBIE.create(p.level(), EntitySpawnReason.COMMAND);
			check(zombie != null, "The zombie fixture exists");
			// The zombie faces the player, so its native shove points away from it.
			zombie.setNoAi(true); zombie.snapTo(p.getX() + dx, 100, p.getZ() + dz, (float) Math.toDegrees(Math.atan2(dx, -dz)), 0); p.level().addFreshEntity(zombie);
			check(zombie.doHurtTarget(p.level(), p), "The zombie's real melee lands");
			Vec3 motion = p.getDeltaMovement();
			zombie.discard();
			return motion;
		});
	}
	private static void resizeFocus(ClientGameTestContext context, String expected) {
		context.runOnClient(mc -> mc.gui.screen().resize(mc.gui.screen().width, mc.gui.screen().height));
		context.waitTicks(2);
		check(context.computeOnClient(mc -> ((MasterFormsScreen) mc.gui.screen()).focusedControl()).equals(expected), "Resize retains the intended initial control: " + expected);
	}
	private static void keyboard(ClientGameTestContext context, String action) {
		check(context.computeOnClient(mc -> ((MasterFormsScreen) mc.gui.screen()).focusedControl()).equals(action), "The native focus is on " + action);
		context.getInput().pressKey(InputConstants.KEY_RETURN); context.waitTicks(2); context.getInput().releaseKey(InputConstants.KEY_RETURN);
	}
}
