package dev.wildercord.content;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.ExciseLessonScreen;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.content.dungeons.DungeonAltarBlockEntity;
import dev.wildercord.content.dungeons.DungeonBlocks;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.MasterStudyRules;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Real Grimoire/altar interactions and packets, including actual reconnect and body replacement. */
public final class ExciseLessonChecks {
	private ExciseLessonChecks() {}
	private static final BlockPos ALTAR = new BlockPos(0, 180, 0);

	public static void run(ClientGameTestContext context) {
		Properties properties = new Properties();
		properties.setProperty("server-ip", "127.0.0.1");
		properties.setProperty("server-port", Integer.toString(port()));
		properties.setProperty("online-mode", "false");
		properties.setProperty("enforce-secure-profile", "false");
		properties.setProperty("pause-when-empty-seconds", "-1");
		properties.setProperty("view-distance", "3");
		properties.setProperty("simulation-distance", "3");
		try (var server = context.worldBuilder().createServer(properties)) {
			server.runOnServer(s -> Wildercord.LOGGER.info(
				"WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.cast.ExcisePlayableTest#lesson\",\"seed\":\"{}\"}", s.overworld().getSeed()));
			TestDedicatedServerConnection connection = server.connect();
			try {
				connection.waitForChunksDownload(); connection.waitForClientboundPackets();
				server.runCommand("gamerule spawn_mobs false");
				server.runCommand("gamerule fall_damage false");
            server.runCommand("gamerule keep_inventory true");
				server.runOnServer(s -> {
					for (var level : List.of(s.overworld(), s.getLevel(Level.NETHER))) {
						for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
							level.setBlock(new BlockPos(x, 179, z), Blocks.STONE.defaultBlockState(), 2);
							for (int y = 180; y <= 184; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
						}
					}
					var p = player(s);
					p.setGameMode(GameType.SURVIVAL);
					p.teleportTo(s.overworld(), .5, 180, -2.5, Set.<Relative>of(), 0, 22, false);
					p.getInventory().clearContent();
					Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
					Spellbooks.set(p, Spellbook.EMPTY);
					p.setAttached(WildercordAttachments.CIRCLES, 16);
					p.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
					p.setAttached(WildercordAttachments.GRIMOIRE, List.of());
					p.setItemInHand(InteractionHand.MAIN_HAND, ExciseLesson.book());
				});
				context.waitTicks(20);
				context.runOnClient(mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
				context.waitTicks(4);
				server.runOnServer(s -> check(!MasterStudies.knowsExcise(player(s)) && !Spellbooks.knows(player(s), Runes.EXCISE.id()), "A forged ledger book cannot teach"));
				context.setScreen(() -> new ExciseLessonScreen(null));
				context.waitTicks(5);
				notReading(context, "A forged retrieval without Heartwood cannot open a study");
				server.runOnServer(s -> {
					var p = player(s);
					check(!MasterStudies.hasExciseLesson(p), "A retrieval cannot manufacture Heartwood or a receipt");
					var entries = new ArrayList<>(Heart.grimoire(p)); entries.add("feat:root_guardian");
					p.setAttached(WildercordAttachments.GRIMOIRE, List.copyOf(entries));
					p.setAttached(WildercordAttachments.CIRCLES, 15);
					p.getInventory().clearContent();
					for (int slot = 0; slot < 36; slot++) p.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
				});
				context.waitTicks(4);
				context.setScreen(() -> new ExciseLessonScreen(null));
				context.waitTicks(5);
				notReading(context, "An old Heartwood holder below XVI cannot begin study");
				server.runOnServer(s -> player(s).setAttached(WildercordAttachments.CIRCLES, 16));
				context.waitTicks(4);
				retrieve(context);
				long first = nonce(context);
				server.runOnServer(s -> {
					var p = player(s);
					check(s.overworld().getBlockEntity(ALTAR) == null && Heart.discovered(p, MasterStudyRules.EXCISE_COPIED), "Old Heartwood recovers the ledger without any altar or book");
					check(!MasterStudies.knowsExcise(p) && !MasterStudies.practicedExcise(p) && books(p) == 0, "Recovery alone does not teach, practice or give physical items");
				});
				send(context, first + 1, 3); send(context, first, 3); send(context, first, 2);
				page(context, 0, true);
				advance(context); page(context, 1, true);
				send(context, first, 1); page(context, 1, true);
				context.runOnClient(mc -> mc.gui.screen().onClose()); context.waitTicks(3);
				send(context, first, 2); send(context, first, 3);
				server.runOnServer(s -> check(!MasterStudies.knowsExcise(player(s)), "Skipped, wrong-nonce, repeated and closed-session packets cannot finish study"));

				retrieve(context); long cracked = nonce(context);
				server.runOnServer(s -> player(s).setAttached(WildercordAttachments.CRACKS, new WildercordAttachments.Cracks(1, s.overworld().getGameTime() + 200)));
				context.waitTicks(5); notReading(context, "Cracking below XVI retires the reading");
				server.runOnServer(s -> player(s).setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE));
				context.waitTicks(3);
				retrieve(context); long expired = nonce(context);
				send(context, cracked, 1); page(context, 0, true);
				server.runOnServer(s -> ((ServerLevelData) s.overworld().getLevelData()).setGameTime(s.overworld().getGameTime() + MasterStudyRules.READING_TICKS));
				context.waitTicks(5); notReading(context, "Five-minute expiry closes a real reading without a stale entry crash");
				send(context, expired, 1); send(context, expired, 2); send(context, expired, 3);

				retrieve(context); long dimension = nonce(context);
				server.runOnServer(s -> check(player(s).teleportTo(s.getLevel(Level.NETHER), .5, 180, -2.5, Set.of(), 0, 22, false), "Native dimension change succeeds"));
				context.waitTicks(20); notReading(context, "Dimension change retires the reading");
				server.runOnServer(s -> player(s).teleportTo(s.overworld(), .5, 180, -2.5, Set.of(), 0, 22, false));
				context.waitTicks(20);
				send(context, dimension, 1); send(context, dimension, 2); send(context, dimension, 3);
				retrieve(context); long deceased = nonce(context);
				server.runOnServer(s -> {
					var original = player(s); original.kill(original.level());
					original.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
					var replacement = player(s);
					check(replacement != original && replacement.getUUID().equals(original.getUUID()), "Real death and respawn replace the owner body");
					replacement.teleportTo(s.overworld(), .5, 180, -2.5, Set.of(), 0, 22, false);
				});
				context.waitTicks(20);
				send(context, deceased, 1); send(context, deceased, 2); send(context, deceased, 3);
				server.runOnServer(s -> check(MasterStudies.hasExciseLesson(player(s)) && !MasterStudies.knowsExcise(player(s)), "Death preserves copied progress, never the old body reading"));

				retrieve(context); long disconnected = nonce(context); advance(context); page(context, 1, true);
				ServerPlayer original = server.computeOnServer(ExciseLessonChecks::player);
				connection.close(); context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
				connection = server.connect(); connection.waitForChunksDownload(); connection.waitForClientboundPackets();
				server.runOnServer(s -> check(player(s) != original && MasterStudies.hasExciseLesson(player(s)) && !MasterStudies.knowsExcise(player(s)), "Actual reconnect persists unfinished Heartwood recovery with a distinct connected body"));
				send(context, disconnected, 2); send(context, disconnected, 3);
				retrieve(context); long completed = nonce(context);
				context.takeScreenshot(TestScreenshotOptions.of("excise_rootbound_history").disableCounterPrefix());
				advance(context); page(context, 1, true); advance(context); page(context, 2, true);
				context.takeScreenshot(TestScreenshotOptions.of("excise_rootbound_controls").disableCounterPrefix());
				int xp = server.computeOnServer(s -> player(s).totalExperience);
				int condensed = server.computeOnServer(s -> Heart.condensed(player(s)));
				advance(context); page(context, 2, false); send(context, completed, 3);
				server.runOnServer(s -> {
					var p = player(s);
					check(MasterStudies.knowsExcise(p) && Spellbooks.knows(p, Runes.EXCISE.id()), "Three ordered pages teach exactly the saved Excise shape");
					check(!MasterStudies.practicedExcise(p), "Reading alone never claims the optional lane-hit practice");
					check(p.totalExperience == xp && Heart.condensed(p) == condensed, "Study does not add XP or condensed mana");
					check(Heart.grimoire(p).stream().filter(MasterStudyRules.EXCISE::equals).count() == 1, "Completion replay cannot duplicate the lesson");
					for (int slot = 0; slot < 36; slot++) p.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                });
                close(context); retrieve(context); page(context, 0, false);
                server.runOnServer(s -> {
                    check(books(player(s)) == 0 && !Heart.discovered(player(s), MasterStudyRules.EXCISE_COPY), "Full inventory keeps the original optional book entitlement");
					player(s).getInventory().setItem(9, ItemStack.EMPTY);
				});
				close(context); retrieve(context);
				server.runOnServer(s -> { check(books(player(s)) == 1 && Heart.discovered(player(s), MasterStudyRules.EXCISE_COPY), "Grimoire gives the original once space exists"); removeBooks(player(s)); });
				close(context); retrieve(context);
				server.runOnServer(s -> { check(books(player(s)) == 1 && Heart.discovered(player(s), MasterStudyRules.EXCISE_REPLACEMENT), "One lost book has one replacement"); removeBooks(player(s)); });
				close(context); retrieve(context);
				server.runOnServer(s -> check(books(player(s)) == 0 && MasterStudies.knowsExcise(player(s)), "Further losses cannot farm copies or remove the permanent lesson"));
				close(context);
				connection.close(); context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
				connection = server.connect(); connection.waitForChunksDownload(); connection.waitForClientboundPackets();
				server.runOnServer(s -> {
					var p = player(s);
					check(MasterStudies.knowsExcise(p) && Spellbooks.knows(p, Runes.EXCISE.id()) && Heart.discovered(p, MasterStudyRules.EXCISE_REPLACEMENT), "Reconnect persists learned rune and both physical-copy receipts");
					p.setAttached(WildercordAttachments.CIRCLES, 15);
				});
				context.waitTicks(4); retrieve(context); page(context, 0, false);
				context.takeScreenshot(TestScreenshotOptions.of("excise_grimoire_retrieval").disableCounterPrefix());
			} finally {
				if (context.computeOnClient(mc -> mc.level != null)) connection.close();
			}
		}
	}

	/** Lets the combat suite learn through the ordinary Grimoire before its equip-and-cast flow. */
	public static void learnFromGrimoire(ClientGameTestContext context) {
		retrieve(context);
		page(context, 0, true);
		advance(context); page(context, 1, true);
		advance(context); page(context, 2, true);
		advance(context); page(context, 2, false);
		context.runOnClient(mc -> mc.gui.screen().onClose());
		context.waitTicks(3);
	}

	private static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
	private static int port() {
		try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); }
		catch (java.io.IOException exception) { throw new RuntimeException(exception); }
	}
	private static void retrieve(ClientGameTestContext context) {
		context.setScreen(CordScreen::new); context.waitTicks(3);
		click(context, context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(2)));
		click(context, context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).exciseLessonPoint()));
		context.waitTicks(3);
	}
	private static long nonce(ClientGameTestContext context) { page(context, 0, true); return context.computeOnClient(mc -> ((ExciseLessonScreen) mc.gui.screen()).sessionNonce()); }
	private static void send(ClientGameTestContext context, long nonce, int page) {
		context.runOnClient(mc -> ClientPlayNetworking.send(new ExciseLesson.Turn(nonce, page))); context.waitTicks(3);
	}
	private static void advance(ClientGameTestContext context) {
		context.getInput().pressKey(InputConstants.KEY_END); context.waitTicks(2);
		click(context, context.computeOnClient(mc -> ((ExciseLessonScreen) mc.gui.screen()).nextPagePoint()));
	}
	private static void click(ClientGameTestContext context, double[] point) {
		check(point != null, "Requested Grimoire or ledger control is visible");
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(point[0] * scale, point[1] * scale); context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); context.waitTicks(5);
	}
	private static void page(ClientGameTestContext context, int page, boolean studying) {
		context.runOnClient(mc -> check(mc.gui.screen() instanceof ExciseLessonScreen lesson && lesson.pageNumber() == page && lesson.isStudying() == studying, "Expected Excise page " + page + ", studying=" + studying));
	}
	private static void notReading(ClientGameTestContext context, String reason) { context.runOnClient(mc -> check(!(mc.gui.screen() instanceof ExciseLessonScreen), reason)); }
	private static void close(ClientGameTestContext context) {
		context.runOnClient(mc -> { if (mc.gui.screen() != null) mc.gui.screen().onClose(); mc.gui.setScreen(null); }); context.waitTicks(3);
	}
	private static int books(ServerPlayer player) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			var stack = player.getInventory().getItem(slot);
			if (Wildercord.id("excise_lesson").equals(stack.get(DataComponents.ITEM_MODEL))) count += stack.getCount();
		}
		return count;
	}
	private static void removeBooks(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
			if (Wildercord.id("excise_lesson").equals(player.getInventory().getItem(slot).get(DataComponents.ITEM_MODEL))) player.getInventory().setItem(slot, ItemStack.EMPTY);
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
