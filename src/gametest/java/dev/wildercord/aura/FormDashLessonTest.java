package dev.wildercord.aura;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.client.FormDashScreen;
import dev.wildercord.client.MasterFormsClient;
import dev.wildercord.client.MasterFormsScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** An Ember clear and a wandering Ember teacher teach Cinder Lunge; the real key then plants, dashes and cuts on an old Survival save. */
public final class FormDashLessonTest implements FabricClientGameTest {
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.FormDashLessonTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				for (int x = -8; x <= 14; x++) for (int z = -6; z <= 6; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 106; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				p.setGameMode(GameType.SURVIVAL); p.teleportTo(p.level(), .5, 100, .5, Set.of(), -90, 0, false); p.setDeltaMovement(Vec3.ZERO);
				check(FormDash.data(p).equals(FormDash.Progress.NONE), "An old save without the attachment knows no field form");
				p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.SOVEREIGN, 4500, 160, 0));
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.GALE));
				var teacher = AuraWorld.DUELIST.create(p.level(), EntitySpawnReason.COMMAND);
				check(teacher != null, "The wandering-teacher fixture exists");
				teacher.setMethod(BreathingMethods.EMBER); teacher.setNoAi(true); teacher.snapTo(.5, 100, 3.5, 180, 0); p.level().addFreshEntity(teacher);
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.setShiftKeyDown(true);
				check(!FormDashLessons.offer(p, teacher), "Without an Ember clear the Ember teacher offers nothing");
				check(!FormDash.learn(p, FormDashRules.CINDER_LUNGE), "Learning is gated by the recorded clear");
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.GALE).withClear(MastersRules.EMBER));
				check(!FormDashLessons.accept(p, 1), "A forged accept has no teacher offer");
				teacher.interact(p, InteractionHand.MAIN_HAND, teacher.position());
				check(FormDash.data(p).learned() == 0, "The offer neither learns nor equips");
				p.setShiftKeyDown(false);
			});
			context.waitFor(mc -> mc.gui.screen() instanceof FormDashScreen, 40);
			check(context.computeOnClient(mc -> ((FormDashScreen) mc.gui.screen()).form() == FormDashRules.CINDER_LUNGE), "The Ember teacher opens Cinder Lunge");
			context.takeScreenshot(TestScreenshotOptions.of("cinder_lunge_teacher_story").disableCounterPrefix());
			resizeFocus(context, "next"); keyboard(context, "next"); keyboard(context, "next");
			check(context.computeOnClient(mc -> mc.gui.screen().getNarrationMessage().getString().contains("25 Aura")), "The lesson page narrates its fixed price");
			context.takeScreenshot(TestScreenshotOptions.of("cinder_lunge_teacher_diagram").disableCounterPrefix());
			resizeFocus(context, "accept"); keyboard(context, "accept");
			context.waitFor(mc -> FormDash.data(mc.player).knows(FormDashRules.CINDER_LUNGE), 40);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(FormDash.data(p).equipped() == 0 && !MasterForms.data(p).learned(), "Accepting learns only this form, with an empty slot");
				check(Aura.data(p).method().equals("gale") && Aura.data(p).xp() == 4500, "The lesson changes neither method nor XP");
				check(!FormDashLessons.accept(p, 1) && !FormDashLessons.accept(p, 2), "The consumed offer cannot be replayed");
				boolean book = false;
				for (int slot = 0; slot < 36; slot++) {
					var stack = p.getInventory().getItem(slot);
					book |= stack.is(Items.WRITTEN_BOOK) && dev.wildercord.Wildercord.id("cinder_lunge_lesson").equals(stack.get(DataComponents.ITEM_MODEL));
				}
				check(book, "The lesson book has its own cover");
				p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); p.inventoryMenu.broadcastChanges();
			});
			context.waitTicks(4); resizeFocus(context, "equip"); keyboard(context, "equip");
			context.waitFor(mc -> FormDash.data(mc.player).equipped() == FormDashRules.CINDER_LUNGE, 40);
			check(context.computeOnClient(mc -> hudState().getString().contains("Cinder Lunge")), "The HUD state names the equipped form");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitTicks(2);
			context.runOnClient(mc -> mc.gui.setScreen(new MasterFormsScreen(null)));
			context.waitTicks(3);
			click(context, "field");
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof FormDashScreen s && s.form() == FormDashRules.CINDER_LUNGE),
				"The Master-form page reaches the learned field form without a teacher");
			layout(context);
			context.runOnClient(mc -> {
				for (String name : new String[] {"cinder_lunge", "reed_slip"}) for (int page = 1; page <= 3; page++)
					check(mc.font.split(net.minecraft.network.chat.Component.translatable("book.wildercord." + name + "." + page), 114).size() <= 14,
						"Every physical-book page fits the native readable area: " + name);
				mc.gui.setScreen(null);
			});
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				p.teleportTo(p.level(), .5, 100, .5, Set.of(), -90, 0, false); p.setDeltaMovement(Vec3.ZERO);
			});
			context.waitTicks(5);
			context.getInput().pressKey(MasterFormsClient.mapping());
			context.waitFor(mc -> FormDash.view(mc.player).phase() == FormDashRules.SET, 10);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Math.abs(Aura.aura(p) - 135) < .001, "The real key pays exactly 25 Aura once");
				check(MasterForms.committed(p) && FormDash.ownsMotion(p) && p.getX() < 1, "The plant is a visible, stationary tell");
			});
			context.waitTicks(FormDashRules.CINDER_SET_TICKS + FormDashRules.CINDER_TICKS + 4);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(p.getX() > 4.5 && p.getX() <= 5.6 && Math.abs(p.getZ() - .5) < .1 && Math.abs(p.getY() - 100) < .01,
					"The server accepted a level dash of at most five blocks where the player looked: " + p.position());
				check(FormDash.data(p).drilled(FormDashRules.CINDER_LUNGE) && Aura.data(p).xp() == 4500 && Aura.data(p).practice() == 0, "Practice has no farmable XP");
				check(FormDash.data(p).readyAt() > MasterForms.now(p) && Aura.aura(p) <= 135.001, "The shared rest is running and nothing was refunded");
			});
			// A Gale teacher puts Reed Slip first only while it is unlearned; afterwards Wall Turn's lesson and re-read come first again.
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				var gale = AuraWorld.DUELIST.create(p.level(), EntitySpawnReason.COMMAND);
				check(gale != null, "The Gale-teacher fixture exists");
				gale.setMethod(BreathingMethods.GALE); gale.setNoAi(true); gale.snapTo(.5, 100, -3.5, 0, 0); p.level().addFreshEntity(gale);
				check(FormDashLessons.form(p, gale) == 0, "An unlearned, eligible Wall Turn keeps its place first");
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, 0, 0, false, false));
				check(FormDashLessons.form(p, gale) == FormDashRules.REED_SLIP, "After Wall Turn the Gale teacher offers the unlearned Reed Slip");
				var f = FormDash.data(p);
				p.setAttached(FormDash.PROGRESS, new FormDash.Progress(f.learned() | FormDashRules.bit(FormDashRules.REED_SLIP), f.equipped(), f.readyAt(), f.recoveryUntil(), f.practiced()));
				check(FormDashLessons.form(p, gale) == 0, "A learned Reed Slip no longer hides Wall Turn's lesson");
				gale.discard();
			});
		}
	}
	private static net.minecraft.network.chat.Component hudState() { return dev.wildercord.client.FormDashClient.state(); }
	private static void resizeFocus(ClientGameTestContext context, String expected) {
		context.runOnClient(mc -> mc.gui.screen().resize(mc.gui.screen().width, mc.gui.screen().height));
		context.waitTicks(2);
		check(context.computeOnClient(mc -> ((FormDashScreen) mc.gui.screen()).focusedControl()).equals(expected), "Resize retains the intended initial control: " + expected);
	}
	private static void keyboard(ClientGameTestContext context, String action) {
		check(context.computeOnClient(mc -> ((FormDashScreen) mc.gui.screen()).focusedControl()).equals(action), "The native focus is on " + action);
		context.getInput().pressKey(InputConstants.KEY_RETURN); context.waitTicks(2); context.getInput().releaseKey(InputConstants.KEY_RETURN);
	}
	private static void click(ClientGameTestContext context, String action) {
		double[] point = context.computeOnClient(mc -> mc.gui.screen() instanceof FormDashScreen f ? f.point(action) : ((MasterFormsScreen) mc.gui.screen()).point(action));
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(point[0] * scale, point[1] * scale);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); context.waitTicks(2);
	}
	private static void layout(ClientGameTestContext context) {
		int[] old = context.computeOnClient(mc -> new int[] {mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), mc.options.guiScale().get()});
		boolean fullscreen = context.computeOnClient(mc -> mc.options.fullscreen().get());
		try {
			for (int[] size : new int[][] {{854, 480}, {1920, 1080}}) for (int requested = 1; requested <= 4; requested += 3) {
				int scale = requested;
				context.runOnClient(mc -> { mc.getWindow().setFullscreen(false); mc.getWindow().setWindowed(size[0], size[1]); mc.options.guiScale().set(scale); mc.resizeGui();
					mc.gui.setScreen(new FormDashScreen(null, FormDashRules.CINDER_LUNGE)); });
				context.waitTicks(3);
				for (int page = 0; page < 5; page++) {
					check(context.computeOnClient(mc -> ((FormDashScreen) mc.gui.screen()).layoutFits()), "Native font layout fits " + size[0] + "x" + size[1] + " scale " + scale + " page " + page);
					if (page == 0) context.takeScreenshot(TestScreenshotOptions.of("cinder_lunge_slot_" + size[0] + "_" + size[1] + "_gui" + scale).disableCounterPrefix());
					click(context, "next");
				}
			}
		} finally {
			context.runOnClient(mc -> { mc.getWindow().setWindowed(old[0], old[1]); mc.getWindow().setFullscreen(fullscreen); mc.options.guiScale().set(old[2]); mc.resizeGui(); });
			context.waitTicks(3);
		}
	}
}
