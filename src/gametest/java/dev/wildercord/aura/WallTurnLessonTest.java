package dev.wildercord.aura;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.client.MasterFormsClient;
import dev.wildercord.client.MasterFormsScreen;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.CordScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Production teacher acceptance, current controls and real jump/brace/kick/landing on an old eligible Survival save. */
public final class WallTurnLessonTest implements FabricClientGameTest {
	private InputConstants.Key beforeBinding;
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			openThroughKeyboard(context);
			resizeFocus(context, "next");
			keyboard(context, "next");
			check(context.computeOnClient(mc -> mc.gui.screen().getNarrationMessage().getString().contains("Gale")), "The locked source action opens real requirements and narrated help");
			resizeFocus(context, "next"); keyboard(context, "next");
			check(context.computeOnClient(mc -> mc.gui.screen().getNarrationMessage().getString().contains("Learn at Sovereign")), "The source page returns to the locked overview by keyboard");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitTicks(2);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen), "Escape returns to the retained Aura parent");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitTicks(2);
			check(context.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen), "A second Escape returns to the retained Cord parent");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.WallTurnLessonTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				for (int x = -8; x <= 12; x++) for (int z = -8; z <= 8; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 108; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				for (int y = 100; y <= 105; y++) for (int z = -3; z <= 3; z++) p.level().setBlockAndUpdate(new BlockPos(-1, y, z), Blocks.STONE.defaultBlockState());
				p.setGameMode(GameType.SURVIVAL); p.teleportTo(.5, 100, .5); p.setDeltaMovement(Vec3.ZERO);
				p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, 160, 0));
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.GALE));
				check(!MasterFormLessons.accept(p, 1), "A forged lesson accept has no teacher offer");
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.setShiftKeyDown(true);
				var teacher = AuraWorld.DUELIST.create(p.level(), EntitySpawnReason.COMMAND);
				check(teacher != null, "The wandering-teacher fixture exists");
				teacher.setMethod(BreathingMethods.GALE); teacher.setNoAi(true); teacher.snapTo(2.5, 100, .5, 90, 0); p.level().addFreshEntity(teacher);
				teacher.interact(p, InteractionHand.MAIN_HAND, teacher.position());
				check(!MasterForms.data(p).learned(), "The offer does not learn or auto-equip the form");
				// Fill after the empty-hand invitation: acceptance still has no room for its optional physical book.
				for (int slot = 0; slot < 36; slot++) p.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
				p.inventoryMenu.broadcastChanges(); p.setShiftKeyDown(false);
			});
			context.waitFor(mc -> mc.gui.screen() instanceof MasterFormsScreen, 40);
			context.takeScreenshot(TestScreenshotOptions.of("wall_turn_teacher_story").disableCounterPrefix());
			resizeFocus(context, "next"); keyboard(context, "next"); keyboard(context, "next");
			context.takeScreenshot(TestScreenshotOptions.of("wall_turn_teacher_illustration").disableCounterPrefix());
			resizeFocus(context, "accept");
			keyboard(context, "accept");
			context.waitFor(mc -> MasterForms.data(mc.player).learned(), 40);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(MasterForms.data(p).learned() && MasterForms.data(p).equipped() == 0, "Full inventory retains the permanent lesson and explicit empty slot");
				check(Aura.data(p).method().equals("ember") && Aura.data(p).xp() == 4500, "The lesson changes neither method nor progression XP");
				check(!MasterFormLessons.accept(p, 1), "The consumed offer cannot be replayed");
				p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); p.inventoryMenu.broadcastChanges();
			});
			context.waitTicks(4); resizeFocus(context, "equip"); keyboard(context, "equip");
			context.waitFor(mc -> MasterForms.data(mc.player).equipped() == MasterForms.WALL_TURN, 40);
			context.takeScreenshot(TestScreenshotOptions.of("wall_turn_equipped_slot").disableCounterPrefix());
			context.getInput().pressKey(InputConstants.KEY_TAB); context.waitTicks(1);
			check(context.computeOnClient(mc -> ((MasterFormsScreen) mc.gui.screen()).focusedControl()).equals("previous"), "Tab visits a native focusable previous control");
			context.getInput().pressKey(InputConstants.KEY_TAB); context.waitTicks(1); keyboard(context, "next");
			check(context.computeOnClient(mc -> mc.gui.screen().getNarrationMessage().getString().contains("ledger")), "Keyboard readback exposes the retained story to narration");
			resizeFocus(context, "next"); keyboard(context, "next");
			click(context, "previous");
			check(context.computeOnClient(mc -> mc.gui.screen().getNarrationMessage().getString().contains("ledger")), "Mouse and keyboard navigate the same retained pages");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitTicks(2);
			check(context.computeOnClient(mc -> mc.gui.screen() == null), "Leaving the teacher's offered lesson returns to play");
			openThroughKeyboard(context);
			context.getInput().pressKey(InputConstants.KEY_TAB); context.waitTicks(1);
			context.getInput().pressKey(InputConstants.KEY_TAB); context.waitTicks(1); keyboard(context, "next");
			check(context.computeOnClient(mc -> mc.gui.screen().getNarrationMessage().getString().contains("ledger")), "The learned story can be reopened without a teacher, book, Cord or mouse");
			layout(context);
			context.runOnClient(mc -> {
				for (int page = 1; page <= 3; page++) check(mc.font.split(net.minecraft.network.chat.Component.translatable("book.wildercord.wall_turn." + page), 114).size() <= 14,
					"Every physical-book page fits the native readable area");
				var key = MasterFormsClient.mapping();
				check(key.getDefaultKey().getValue() == InputConstants.KEY_C, "The form has its own C default");
				for (KeyMapping other : mc.options.keyMappings) {
					if (other.getName().startsWith("key.debug.")) continue;
					if (other == mc.options.keySaveHotbarActivator) {
						check(other.getDefaultKey().getValue() == InputConstants.KEY_C, "The only normal shared C control is the Creative hotbar chord");
						continue;
					}
					check(other == key || !other.getDefaultKey().equals(key.getDefaultKey()), "No default conflict with " + other.getName());
				}
				check(mc.options.keyQuickActions.getDefaultKey().getValue() == InputConstants.KEY_G, "Ordinary Quick Actions remains on its actual 26.3 G default");
				KeyMapping.set(key.getDefaultKey(), true);
				try { check(key.isDown() && mc.options.keySaveHotbarActivator.isDown(), "C updates both mapping states; Creative's modifier remains usable"); }
				finally { KeyMapping.set(key.getDefaultKey(), false); }
				beforeBinding = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(key);
				key.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_I)); KeyMapping.resetMapping();
				check(MasterFormsClient.binding().equals(key.getTranslatedKeyMessage()), "The displayed instruction follows rebinding");
				mc.gui.setScreen(null);
			});
			context.waitTicks(3);
			context.getInput().holdKey(o -> o.keyJump); context.waitTicks(2); context.getInput().releaseKey(o -> o.keyJump);
			context.getInput().pressKey(MasterFormsClient.mapping());
			context.waitFor(mc -> MasterForms.view(mc.player).phase() == WallTurnRules.BRACE, 10);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(Math.abs(Aura.aura(p) - 140) < .001, "A valid airborne wall brace pays exactly 20 Aura once");
				check(MasterForms.committed(p) && MasterForms.data(p).airborneUsed(), "Accepted brace owns movement and consumes the one airborne use");
			});
			context.waitTicks(1); context.getInput().pressKey(MasterFormsClient.mapping());
			context.waitFor(mc -> MasterForms.view(mc.player).phase() == WallTurnRules.KICK, 10);
			context.waitTicks(24);
			world.getServer().runOnServer(server -> {
				var p = server.getPlayerList().getPlayers().getFirst();
				check(p.getX() > 3.5 && p.getX() <= 4.6, "The server accepted a useful outward kick of at most four horizontal blocks");
				check(MasterForms.data(p).practiced() && !MasterForms.data(p).airborneUsed(), "A genuine safe landing completes the real practice path");
				check(Aura.data(p).xp() == 4500 && Aura.data(p).practice() == 0, "Practice grants no farmable XP");
				check(Aura.aura(p) <= 140.001, "Release and landing never refund or charge a second price");
			});
		} finally {
			if (beforeBinding != null) context.runOnClient(mc -> { MasterFormsClient.mapping().setKey(beforeBinding); KeyMapping.resetMapping(); });
		}
	}
	private static void openThroughKeyboard(ClientGameTestContext context) {
		var open = context.computeOnClient(mc -> java.util.Arrays.stream(mc.options.keyMappings)
			.filter(key -> key.getName().equals("key.wildercord.open_cord")).findFirst().orElseThrow());
		context.getInput().pressKey(open); context.waitTicks(2);
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen), "The existing bound Cord key opens the ordinary entry screen without a Cord");
		nativeEntry(context, "Aura");
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen), "The native Aura badge is reachable and activated by keyboard");
		nativeEntry(context, "Master form");
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof MasterFormsScreen), "The native Master-form entry opens retained source and lesson access");
	}
	private static void nativeEntry(ClientGameTestContext context, String expected) {
		context.getInput().pressKey(InputConstants.KEY_TAB); context.waitTicks(1);
		check(context.computeOnClient(mc -> mc.gui.screen().getFocused() instanceof net.minecraft.client.gui.components.Button button
			&& button.isFocused() && button.getMessage().getString().contains(expected)
			&& button.narrationPriority() == net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority.FOCUSED), "A native narrated entry is focused: " + expected);
		context.getInput().pressKey(InputConstants.KEY_RETURN); context.waitTicks(2);
	}
	private static void resizeFocus(ClientGameTestContext context, String expected) {
		context.runOnClient(mc -> mc.gui.screen().resize(mc.gui.screen().width, mc.gui.screen().height));
		context.waitTicks(2);
		check(context.computeOnClient(mc -> ((MasterFormsScreen) mc.gui.screen()).focusedControl()).equals(expected), "Resize retains the intended initial control: " + expected);
	}
	private static void layout(ClientGameTestContext context) {
		int[] old = context.computeOnClient(mc -> new int[] {mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), mc.options.guiScale().get()});
		boolean fullscreen = context.computeOnClient(mc -> mc.options.fullscreen().get());
		try {
			for (int[] size : new int[][] {{854, 480}, {1280, 720}, {1920, 1080}}) for (int requested = 1; requested <= 4; requested++) {
				int scale = requested;
				context.runOnClient(mc -> { mc.getWindow().setFullscreen(false); mc.getWindow().setWindowed(size[0], size[1]); mc.options.guiScale().set(scale); mc.resizeGui(); mc.gui.setScreen(new MasterFormsScreen(null)); });
				context.waitTicks(3);
				for (int page = 0; page < 5; page++) {
					check(context.computeOnClient(mc -> ((MasterFormsScreen) mc.gui.screen()).layoutFits()), "Native font layout fits " + size[0] + "x" + size[1] + " scale " + scale + " page " + page);
					if (page == 0) context.takeScreenshot(TestScreenshotOptions.of("wall_turn_slot_" + size[0] + "_" + size[1] + "_gui" + scale).disableCounterPrefix());
					click(context, "next");
				}
			}
		} finally {
			context.runOnClient(mc -> { mc.getWindow().setWindowed(old[0], old[1]); mc.getWindow().setFullscreen(fullscreen); mc.options.guiScale().set(old[2]); mc.resizeGui(); });
			context.waitTicks(3);
		}
	}
	private static void keyboard(ClientGameTestContext context, String action) {
		check(context.computeOnClient(mc -> ((MasterFormsScreen) mc.gui.screen()).focusedControl()).equals(action), "The native focus is on " + action);
		context.getInput().pressKey(InputConstants.KEY_RETURN); context.waitTicks(2); context.getInput().releaseKey(InputConstants.KEY_RETURN);
	}
	private static void click(ClientGameTestContext context, String action) {
		double[] point = context.computeOnClient(mc -> ((MasterFormsScreen) mc.gui.screen()).point(action));
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(point[0] * scale, point[1] * scale);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); context.waitTicks(2);
	}
}
