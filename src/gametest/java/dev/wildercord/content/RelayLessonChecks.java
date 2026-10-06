package dev.wildercord.content;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.RelayLessonScreen;
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
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.net.InetAddress;
import java.net.ServerSocket;

/** Real lectern-use and page packets; call from the registered Relay suite. No direct teaching hooks. */
public final class RelayLessonChecks {
	private RelayLessonChecks() {}
	private static final BlockPos LECTERN = new BlockPos(0, 180, 0);

	public static void run(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				for (int x = -5; x <= 30; x++) for (int z = -5; z <= 5; z++) {
					level.setBlock(new BlockPos(x, 179, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 180; y <= 184; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				level.setBlock(LECTERN, WildercordBlocks.ARCHIVE_LECTERN.defaultBlockState().setValue(ArchiveLecternBlock.AWAKE, true), 3);
				check(!((ArchiveLecternBlockEntity) level.getBlockEntity(LECTERN)).isSlain(), "Legacy spent lectern has no modern slain flag");
				var player = player(server);
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, .5, 180, -2.5, Set.<Relative>of(), 0, 22, false);
				player.getInventory().clearContent();
				player.setItemInHand(InteractionHand.MAIN_HAND, RelayLesson.book());
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbooks.set(player, Spellbook.EMPTY);
				player.setAttached(WildercordAttachments.CIRCLES, 7);
				player.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:archivist"));
				player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
				player.setAttached(WildercordAttachments.CONDENSED, 123);
				rewards(player, "fixture_seeded");
			});
			context.waitTicks(10);
			// A forged portable copy can be read normally but cannot grant a permanent study or rune.
			context.runOnClient(mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				check(!MasterStudies.knowsRelay(player(server)) && !Spellbooks.knows(player(server), Runes.RELAY.id()), "A forged book cannot teach");
				player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			});
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
			use(context);
			checkNotReading(context, "Below VIII cannot begin study");
			world.getServer().runOnServer(server -> {
				player(server).setAttached(WildercordAttachments.CIRCLES, 8);
				// Exercise only the missing-victory gate; retain the ordinary one-time awakening record.
				player(server).setAttached(WildercordAttachments.GRIMOIRE,
					Heart.grimoire(player(server)).stream().filter(key -> !key.equals("feat:archivist")).toList());
			});
			context.waitTicks(3);
			use(context);
			checkNotReading(context, "VIII without the permanent Archivist feat cannot begin study");
			world.getServer().runOnServer(server -> {
				var player = player(server);
				var found = new java.util.ArrayList<>(Heart.grimoire(player)); found.add("feat:archivist");
				player.setAttached(WildercordAttachments.GRIMOIRE, List.copyOf(found));
				for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
			});
			context.waitTicks(3);
			// Current qualification and a portable book are insufficient for a forged Grimoire request.
			context.runOnClient(mc -> ClientPlayNetworking.send(new RelayLesson.Retrieve(71, false)));
			context.waitTicks(3);
			world.getServer().runOnServer(server -> check(!MasterStudies.hasRelayLesson(player(server)), "Retrieve packet cannot manufacture a copied receipt"));
			world.getServer().runOnServer(server -> server.overworld().setBlock(LECTERN.north(), Blocks.STONE.defaultBlockState(), 3));
			context.waitTicks(3);
			use(context);
			world.getServer().runOnServer(server -> {
				check(!MasterStudies.hasRelayLesson(player(server)), "Blocked lectern access cannot copy the lesson");
				server.overworld().setBlock(LECTERN.north(), Blocks.AIR.defaultBlockState(), 3);
			});
			context.waitTicks(3);
			Rewards beforeCopy = world.getServer().computeOnServer(server -> {
				Rewards before = rewards(player(server), "before_copy");
				check(!before.innate().isEmpty() && before.grimoire().contains("feat:innate")
					&& before.runes().contains(before.innate()) && before.starterGiven(),
					"Ordinary one-time awakening and starter learning finish before the copy reward boundary: " + before);
				return before;
			});
			use(context);
			long originalReading = nonce(context);
			world.getServer().runOnServer(server -> {
				var player = player(server);
				check(Heart.discovered(player, MasterStudyRules.RELAY_COPIED) && !MasterStudies.knowsRelay(player)
					&& !Spellbooks.knows(player, Runes.RELAY.id()) && !MasterStudies.practicedRelay(player), "Real access copies but neither teaches nor completes practice");
				check(books(player) == 0, "A full inventory cannot prevent the permanent copied receipt");
				Rewards afterCopy = rewards(player, "after_copy");
				check(afterCopy.condensed() == beforeCopy.condensed() && afterCopy.xp() == beforeCopy.xp(),
					"Copying adds no condensed mana or XP: before=" + beforeCopy + ", after=" + afterCopy);
				var expected = new java.util.ArrayList<>(beforeCopy.grimoire()); expected.add(MasterStudyRules.RELAY_COPIED);
				check(afterCopy.grimoire().equals(expected) && afterCopy.runes().equals(beforeCopy.runes())
					&& afterCopy.innate().equals(beforeCopy.innate()) && afterCopy.starterGiven() == beforeCopy.starterGiven(),
					"First copying changes only the verified lesson receipt");
			});
			context.runOnClient(mc -> mc.gui.screen().onClose()); context.waitTicks(3);
			Rewards beforeRepeat = world.getServer().computeOnServer(server -> rewards(player(server), "before_repeat_copy"));
			use(context);
			long first = nonce(context);
			check(first != originalReading, "Repeated real access opens a new transient reading rather than reusing its nonce");
			world.getServer().runOnServer(server -> {
				var player = player(server);
				Rewards afterRepeat = rewards(player, "after_repeat_copy");
				check(afterRepeat.equals(beforeRepeat) && books(player) == 0
					&& afterRepeat.grimoire().stream().filter(MasterStudyRules.RELAY_COPIED::equals).count() == 1,
					"Repeated real copying is idempotent and grants no rewards, duplicate receipt or runes: before=" + beforeRepeat + ", after=" + afterRepeat);
				roundTrip(player, false);
				player.teleportTo(player.level(), 25.5, 180, -2.5, Set.<Relative>of(), 0, 22, false);
			});
			context.waitTicks(105);
			world.getServer().runOnServer(server -> {
				long now = server.overworld().getGameTime();
				((net.minecraft.world.level.storage.ServerLevelData) server.overworld().getLevelData()).setGameTime(now + dev.wildercord.content.dungeons.DungeonAltarBlockEntity.REARM_TICKS + 1);
			});
			context.waitTicks(105);
			assertPage(context, 0, true);
			world.getServer().runOnServer(server -> {
				check(!server.overworld().getBlockState(LECTERN).getValue(ArchiveLecternBlock.AWAKE), "Legacy missing-boss rearm is not delayed by reading");
				player(server).teleportTo(server.overworld(), .5, 180, -2.5, Set.<Relative>of(), 0, 22, false);
			});
			context.waitTicks(25);
			world.getServer().runOnServer(server -> {
				check(!server.overworld().getEntitiesOfClass(dev.wildercord.cast.Archivist.class,
					new net.minecraft.world.phys.AABB(LECTERN).inflate(48), dev.wildercord.cast.Archivist::isAlive).isEmpty(), "Existing boss respawn behaviour remains active");
				player(server).teleportTo(server.overworld(), 25.5, 180, -2.5, Set.<Relative>of(), 0, 22, false);
			});
			context.waitTicks(3);
			assertPage(context, 0, true);
			send(context, first + 1, 3);
			send(context, first, 3);
			assertPage(context, 0, true);
			world.getServer().runOnServer(server -> check(!MasterStudies.knowsRelay(player(server)), "Forged nonce and skipped pages do not teach"));
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(3);
			retrieve(context);
			long second = nonce(context);
			check(first != second, "New saved reading has a fresh nonce");
			send(context, first, 1);
			assertPage(context, 0, true);
			world.getServer().runOnServer(server -> player(server).setAttached(WildercordAttachments.CRACKS,
				new WildercordAttachments.Cracks(1, server.overworld().getGameTime() + 100)));
			context.waitTicks(5);
			checkNotReading(context, "Copied receipt does not bypass a cracked eighth circle");
			world.getServer().runOnServer(server -> {
				check(MasterStudies.hasRelayLesson(player(server)), "Interruption does not erase verified copying");
				player(server).setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
			});
			context.waitTicks(3);
			retrieve(context);
			send(context, second, 1);
			long completed = nonce(context);
			context.takeScreenshot(TestScreenshotOptions.of("relay_archive_history").disableCounterPrefix());
			advance(context);
			assertPage(context, 1, true);
			advance(context);
			assertPage(context, 2, true);
			context.takeScreenshot(TestScreenshotOptions.of("relay_archive_practical").disableCounterPrefix());
			int experience = world.getServer().computeOnServer(server -> player(server).totalExperience);
			int condensed = world.getServer().computeOnServer(server -> Heart.condensed(player(server)));
			advance(context);
			assertPage(context, 2, false);
			send(context, completed, 3);
			world.getServer().runOnServer(server -> {
				var player = player(server);
				check(MasterStudies.knowsRelay(player) && Spellbooks.knows(player, Runes.RELAY.id()), "Saved three-page reading teaches despite a respawned Archivist");
				check(books(player) == 0, "Full inventory retains no dropped or duplicate physical book");
				check(Heart.condensed(player) == condensed && player.totalExperience == experience, "Learning adds no condensed mana or XP");
				check(!MasterStudies.practicedRelay(player), "Reading cannot claim Dummy practice");
				roundTrip(player, true);
			});
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			// The in-game Grimoire reads the retained pages without a physical book.
			context.setScreen(CordScreen::new);
			context.waitTicks(3);
			click(context, context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(2)));
			context.waitTicks(3);
			click(context, context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).relayLessonPoint()));
			assertPage(context, 0, false);
			context.takeScreenshot(TestScreenshotOptions.of("relay_grimoire_retrieval").disableCounterPrefix());
			context.runOnClient(mc -> mc.gui.setScreen(null));
			world.getServer().runOnServer(server -> {
				player(server).getInventory().setItem(9, ItemStack.EMPTY);
				for (var keeper : server.overworld().getEntitiesOfClass(dev.wildercord.cast.Archivist.class,
					new net.minecraft.world.phys.AABB(LECTERN).inflate(48), dev.wildercord.cast.Archivist::isAlive)) keeper.discard();
				((ArchiveLecternBlockEntity) server.overworld().getBlockEntity(LECTERN)).slain();
				player(server).teleportTo(server.overworld(), .5, 180, -2.5, Set.<Relative>of(), 0, 22, false);
			});
			context.waitTicks(5);
			use(context);
			world.getServer().runOnServer(server -> {
				var player = player(server);
				check(books(player) == 1 && Heart.discovered(player, MasterStudyRules.RELAY_COPY), "Lectern supplies original copy once there is room");
				removeBooks(player);
			});
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
			use(context);
			world.getServer().runOnServer(server -> {
				var player = player(server);
				check(books(player) == 1 && Heart.discovered(player, MasterStudyRules.RELAY_REPLACEMENT), "Lost copy can be replaced once");
				removeBooks(player);
			});
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
			use(context);
			assertPage(context, 0, false);
			world.getServer().runOnServer(server -> check(books(player(server)) == 0 && MasterStudies.knowsRelay(player(server)), "Further losses retain readable lesson without farming book copies"));
			context.runOnClient(mc -> mc.gui.setScreen(null));
		}
		reconnect(context);
	}

	/** The actual socket is closed: no session or original body is reused after the saved copy returns. */
	private static void reconnect(ClientGameTestContext context) {
		Properties properties = new Properties();
		properties.setProperty("server-ip", "127.0.0.1"); properties.setProperty("server-port", Integer.toString(port()));
		properties.setProperty("online-mode", "false"); properties.setProperty("enforce-secure-profile", "false");
		properties.setProperty("pause-when-empty-seconds", "-1"); properties.setProperty("view-distance", "3"); properties.setProperty("simulation-distance", "3");
		try (var server = context.worldBuilder().createServer(properties)) {
			TestDedicatedServerConnection connection = server.connect();
			try {
				connection.waitForChunksDownload(); connection.waitForClientboundPackets();
				server.runCommand("gamerule spawn_mobs false"); server.runCommand("gamerule fall_damage false");
				ServerPlayer original = server.computeOnServer(s -> {
					var level = s.overworld();
					for (int x = -3; x <= 3; x++) for (int z = -4; z <= 3; z++) {
						level.setBlock(new BlockPos(x, 179, z), Blocks.STONE.defaultBlockState(), 2);
						for (int y = 180; y <= 184; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
					}
					level.setBlock(LECTERN, WildercordBlocks.ARCHIVE_LECTERN.defaultBlockState().setValue(ArchiveLecternBlock.AWAKE, true), 3);
					var player = player(s); player.setGameMode(GameType.SURVIVAL);
					player.teleportTo(level, .5, 180, -2.5, Set.<Relative>of(), 0, 22, false);
					player.setAttached(WildercordAttachments.CIRCLES, 8);
					player.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:archivist"));
					Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
					for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
					return player;
				});
				context.waitTicks(10); use(context); long oldNonce = nonce(context);
				advance(context); assertPage(context, 1, true);
				connection.close(); context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
				server.runOnServer(s -> s.overworld().setBlock(LECTERN, Blocks.STONE.defaultBlockState(), 3));
				connection = server.connect(); connection.waitForChunksDownload(); connection.waitForClientboundPackets();
				server.runOnServer(s -> {
					var joined = player(s);
					check(joined != original && joined.getUUID().equals(original.getUUID()), "Reconnect creates a distinct connected body");
					check(MasterStudies.hasRelayLesson(joined) && !MasterStudies.knowsRelay(joined)
						&& !MasterStudies.practicedRelay(joined) && books(joined) == 0, "Unfinished copied lesson survives actual reconnect without a book");
				});
				send(context, oldNonce, 2); send(context, oldNonce, 3);
				server.runOnServer(s -> check(!MasterStudies.knowsRelay(player(s)), "Original body page receipt cannot finish after reconnect"));
				retrieve(context); assertPage(context, 0, true);
				advance(context); advance(context); advance(context); assertPage(context, 2, false);
				server.runOnServer(s -> check(MasterStudies.knowsRelay(player(s)), "Saved verified copy completes through Grimoire after the lectern is gone"));
			} finally { if (context.computeOnClient(mc -> mc.level != null)) connection.close(); }
		}
	}

	private static int port() {
		try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); }
		catch (java.io.IOException exception) { throw new RuntimeException(exception); }
	}

	private static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
	private static void use(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(LECTERN), Direction.NORTH, LECTERN, false)));
		context.waitTicks(5);
	}
	private static void retrieve(ClientGameTestContext context) {
		context.setScreen(CordScreen::new);
		context.waitTicks(3);
		click(context, context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(2)));
		click(context, context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).relayLessonPoint()));
		context.waitTicks(3);
	}

	private static long nonce(ClientGameTestContext context) {
		assertPage(context, 0, true);
		return context.computeOnClient(mc -> ((RelayLessonScreen) mc.gui.screen()).sessionNonce());
	}
	private static void send(ClientGameTestContext context, long nonce, int page) {
		context.runOnClient(mc -> ClientPlayNetworking.send(new RelayLesson.Turn(nonce, page)));
		context.waitTicks(3);
	}
	private static void advance(ClientGameTestContext context) {
		context.getInput().pressKey(InputConstants.KEY_END);
		context.waitTicks(2);
		click(context, context.computeOnClient(mc -> ((RelayLessonScreen) mc.gui.screen()).nextPagePoint()));
	}
	private static void click(ClientGameTestContext context, double[] point) {
		check(point != null, "Requested lesson control is visible");
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(point[0] * scale, point[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(5);
	}
	private static void assertPage(ClientGameTestContext context, int page, boolean studying) {
		context.runOnClient(mc -> check(mc.gui.screen() instanceof RelayLessonScreen lesson
			&& lesson.pageNumber() == page && lesson.isStudying() == studying, "Expected Relay page " + page + ", studying=" + studying));
	}
	private static void checkNotReading(ClientGameTestContext context, String reason) {
		context.runOnClient(mc -> check(!(mc.gui.screen() instanceof RelayLessonScreen), reason));
	}
	private static int books(ServerPlayer player) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (Wildercord.id("relay_lesson").equals(stack.get(DataComponents.ITEM_MODEL))) count += stack.getCount();
		}
		return count;
	}
	private static void removeBooks(ServerPlayer player) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (Wildercord.id("relay_lesson").equals(player.getInventory().getItem(slot).get(DataComponents.ITEM_MODEL)))
				player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
	}
	private static void roundTrip(ServerPlayer player, boolean learned) {
		var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
		player.saveWithoutId(saved);
		player.setAttached(WildercordAttachments.GRIMOIRE, List.of());
		Spellbooks.set(player, Spellbook.EMPTY);
		player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), saved.buildResult()));
		check(MasterStudies.hasRelayLesson(player) && MasterStudies.knowsRelay(player) == learned
			&& Spellbooks.knows(player, Runes.RELAY.id()) == learned, "Copying survives save/load independently of learning");
	}
	private record Rewards(int condensed, int xp, String innate, List<String> grimoire, List<String> runes, boolean starterGiven) {}

	/** Exact action-boundary snapshots; historical pre-copy values are printed rather than inferred. */
	private static Rewards rewards(ServerPlayer player, String phase) {
		Rewards state = new Rewards(Heart.condensed(player), player.totalExperience, Heart.innate(player),
			List.copyOf(Heart.grimoire(player)), List.copyOf(Spellbooks.get(player).learned()), Spellbooks.get(player).starterGiven());
		var value = new com.google.gson.JsonObject();
		value.addProperty("phase", phase); value.addProperty("tick", player.level().getGameTime());
		value.addProperty("condensed", state.condensed()); value.addProperty("xp", state.xp());
		value.addProperty("circles", Heart.circles(player)); value.addProperty("innate", state.innate());
		value.addProperty("starterGiven", state.starterGiven());
		var found = new com.google.gson.JsonArray(); state.grimoire().stream().limit(32).forEach(found::add);
		value.add("grimoire", found); value.addProperty("grimoireCount", state.grimoire().size());
		var runes = new com.google.gson.JsonArray(); state.runes().stream().limit(32).forEach(runes::add);
		value.add("runes", runes); value.addProperty("runeCount", state.runes().size());
		System.out.println("WILDERCORD_RELAY_LESSON_REWARDS " + value);
		return state;
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
