package dev.wildercord.gametest;

import dev.wildercord.cast.RuneReadings;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.WorldResonances;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.GrimoireToast;
import dev.wildercord.client.RuneReadingText;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Resonance;
import dev.wildercord.spell.ResonanceLore;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneHints;
import dev.wildercord.spell.RuneQuirks;
import dev.wildercord.spell.RuneReading;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Each world's own magic, and reading runes, in real worlds. Two worlds with different seeds draw different
 * resonances, and a third with the first one's seed draws the same ones again. In the first: casting a resonance
 * finds it (the Grimoire entry, the first finder recorded, the toast and the server's announcement), a Torn Page's
 * riddle tells its name and riddle but never its runes, and through it all the client never holds a resonance its
 * player hasn't found, read or been told of. A few resonances are cast at husks and filmed ({@code resonance_twist_*}).
 * A rune learned from its item starts unread, the first cast glimpses it and a few more that land understand it, each
 * stage filmed in the Codex ({@code resonance_codex_*}); runes the player already knew are understood.
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE}
 * skip it.</p>
 */
public class WildercordResonanceTest implements FabricClientGameTest {
	private static final String SEED_A = "1234567";
	private static final String SEED_B = "7654321";
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.resonance_test";
	/** How far down the player looks: bolts reach the husks, and where a spell aims falls just behind them. */
	private static final float PITCH = 8.0F;

	/** Every system message the client was sent, to find the announcement among. */
	private static final List<Component> MESSAGES = new CopyOnWriteArrayList<>();
	private static boolean listening;

	/** What a world drew: its resonances' ids, sequences and names, in order. */
	private record Draw(long seed, List<String> ids, List<List<String>> runes, List<String> names, List<String> quirks) {}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!listening) {
			listening = true;
			ClientReceiveMessageEvents.GAME.register((message, overlay) -> MESSAGES.add(message));
		}
		List<String> failures = new ArrayList<>();
		Draw first;
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(s -> s.setSeed(SEED_A)).create()) {
			context.waitTicks(40);
			first = draw(world);
			check(failures, first.ids().size() == 12, "a world should draw 12 resonances, it drew " + first.ids().size());
			check(failures, first.quirks().size() == RuneQuirks.DEFAULT_COUNT, "a world should draw " + RuneQuirks.DEFAULT_COUNT + " quirks");
			check(failures, first.seed() == Long.parseLong(SEED_A), "the world should have the seed it was made with (" + first.seed() + ")");
			stage(world);
			context.waitTicks(10);
			failures.addAll(privacy(context, world, Set.of()));
			failures.addAll(discovery(context, world));
			failures.addAll(twists(context, world));
			failures.addAll(reading(context, world));
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(s -> s.setSeed(SEED_B)).create()) {
			context.waitTicks(40);
			Draw second = draw(world);
			check(failures, !second.ids().equals(first.ids()), "a world with another seed should draw other resonances");
			Set<List<String>> shared = new HashSet<>(first.runes());
			shared.retainAll(new HashSet<>(second.runes()));
			check(failures, shared.size() <= 1, "two worlds should share almost no sequences, these share " + shared);
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(s -> s.setSeed(SEED_A)).create()) {
			context.waitTicks(40);
			Draw again = draw(world);
			check(failures, again.ids().equals(first.ids()) && again.runes().equals(first.runes()) && again.names().equals(first.names())
				&& again.quirks().equals(first.quirks()), "a new world with the same seed should draw the same resonances and quirks");
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("A world's own magic went wrong:\n  " + String.join("\n  ", failures));
		}
	}

	// ------------------------------------------------------------------ finding one

	/** Casting a resonance's exact runes finds it: Grimoire, first finder, toast and announcement, and it's priced as found. */
	private static List<String> discovery(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		// One that wakes anywhere: a few first wake only at a ley crossing (see WorldBonds and WildercordBondsTest).
		Resonance target = on(world, player -> WorldResonances.of(server(player)).stream().filter(r -> !dev.wildercord.cast.WorldBonds.crossingBound(r))
			.findFirst().orElseThrow());
		MESSAGES.clear();
		cast(context, world, target);
		context.waitTicks(20);
		boolean found = on(world, player -> Heart.discovered(player, target.key()));
		check(out, found, "casting " + target.name() + " " + target.runes() + " should find it");
		String finder = on(world, player -> {
			ResonanceLore.View view = lore(player).resonances().stream().filter(v -> v.id().equals(target.id())).findFirst().orElse(null);
			return view == null ? "" : view.finder();
		});
		String name = on(world, player -> player.getGameProfile().name());
		check(out, finder.equals(name), "the world should record who found it first (it says '" + finder + "')");
		boolean announced = MESSAGES.stream().anyMatch(m -> m.getContents() instanceof TranslatableContents t
			&& t.getKey().equals("message.wildercord.resonance_found") && m.getString().contains(target.name()));
		check(out, announced, "the server should announce the find in chat");
		boolean toast = context.computeOnClient(mc -> mc.gui.toastManager().getToast(GrimoireToast.class,
			WorldResonances.Revealed.RESONANCE + ":" + target.name()) != null);
		check(out, toast, "the finder should see the resonance's toast");
		double price = on(world, player -> Heart.secretCost(player, target.defs().orElseThrow()));
		check(out, Math.abs(price - Resonance.COST) < 1e-9, "a found resonance should cost " + Resonance.COST + " times the spell, it costs " + price);
		// A second cast finds nothing new and says nothing more.
		MESSAGES.clear();
		cast(context, world, target);
		context.waitTicks(10);
		check(out, MESSAGES.stream().noneMatch(m -> m.getString().contains(target.name())), "a resonance is announced only the first time");
		// A Torn Page's riddle: its name and riddle reach the client, never its runes or its twist.
		Resonance read = on(world, player -> WorldResonances.hint(player).orElse(null));
		check(out, read != null, "a Torn Page should find one of this world's riddles to read");
		context.waitTicks(5);
		if (read != null) {
			ResonanceLore.View view = context.computeOnClient(mc -> lore(mc.player).resonances().stream().filter(v -> v.id().equals(read.id())).findFirst().orElse(null));
			check(out, view != null && view.riddle().equals(read.riddle()) && view.runes().isEmpty() && view.twist().isEmpty() && !view.found(),
				"a riddle read should tell its name and riddle, never its runes: " + view);
		}
		Set<String> known = new HashSet<>(List.of(target.id()));
		if (read != null) {
			known.add(read.id());
		}
		out.addAll(privacy(context, world, known));
		return out;
	}

	/** The client holds only resonances its player found ({@code known} holds found and read ones), and runes only of those found. */
	private static List<String> privacy(ClientGameTestContext context, TestSingleplayerContext world, Set<String> known) {
		List<String> out = new ArrayList<>();
		List<Resonance> all = on(world, player -> WorldResonances.of(server(player)));
		WildercordAttachments.WorldLore lore = context.computeOnClient(mc -> lore(mc.player));
		List<String> grimoire = context.computeOnClient(mc -> List.copyOf(Heart.grimoire(mc.player)));
		check(out, lore.total() == all.size(), "the client may know how many resonances the world holds (" + lore.total() + ")");
		for (Resonance resonance : all) {
			boolean mayKnow = known.contains(resonance.id());
			ResonanceLore.View view = lore.resonances().stream().filter(v -> v.id().equals(resonance.id())).findFirst().orElse(null);
			if (!mayKnow) {
				check(out, view == null, "the client holds " + resonance.name() + ", which its player never found, read or heard of");
				check(out, grimoire.stream().noneMatch(key -> key.endsWith(resonance.id())), "the client's Grimoire names an undiscovered resonance");
			}
			if (view != null && !view.found()) {
				check(out, view.runes().isEmpty(), "the client holds the runes of " + resonance.name() + ", which its player hasn't found");
			}
		}
		// Nothing of a resonance's sequence is anywhere in what the client was told, unless it was found.
		for (ResonanceLore.View view : lore.resonances()) {
			check(out, view.found() == grimoire.contains(Resonance.KEY_PREFIX + view.id()), "a view's found flag should match the Grimoire");
		}
		return out;
	}

	// ------------------------------------------------------------------ the twists, filmed

	/**
	 * Casts a few of the world's resonances at a row of husks, from a camera a little behind and above, and films each
	 * twist twice: early, and as it plays out. Each is found first, so the films show the twist and not the moment of
	 * finding it.
	 */
	private static List<String> twists(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		List<Resonance> all = on(world, player -> WorldResonances.of(server(player)).stream()
			.filter(r -> !dev.wildercord.cast.WorldBonds.crossingBound(r)).toList());
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(6.5);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		try {
			for (int i = 1; i < Math.min(all.size(), 7); i++) {
				Resonance resonance = all.get(i);
				husks(world);
				context.waitTicks(5);
				cast(context, world, resonance);
				context.waitTicks(40);
				boolean found = on(world, player -> Heart.discovered(player, resonance.key()));
				check(out, found, "casting " + resonance.name() + " should find it too");
				cleanup(context, world);
				husks(world);
				context.waitTicks(10);
				director(context, world, new Vec3(STAGE.getX() - 8.5, STAGE.getY() + 4.0, STAGE.getZ() + 1.0),
					new Vec3(STAGE.getX() + 0.5, STAGE.getY() + 1.0, STAGE.getZ() + 4.5));
				cast(context, world, resonance);
				walkIfNeeded(context, world, resonance, 0, 4);
				context.waitTicks(6);
				shot(context, "resonance_twist_" + resonance.twist() + "_1");
				walkIfNeeded(context, world, resonance, 4, 8);
				context.waitTicks(14);
				shot(context, "resonance_twist_" + resonance.twist() + "_2");
				cut(context);
				context.waitTicks(30);
				cleanup(context, world);
			}
		} finally {
			on(world, player -> {
				player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
				return null;
			});
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
		return out;
	}

	/** Films from a fixed point: the client looks through an invisible marker placed there (as the feature tour does). */
	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, player(server).getBoundingBox().inflate(128),
				e -> e.entityTags().contains("wildercord.camera")).forEach(net.minecraft.world.entity.Entity::discard);
			net.minecraft.world.entity.Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			Vec3 d = target.subtract(eye);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			camera.snapTo(eye.x, eye.y, eye.z, yaw, pitch);
			camera.addTag("wildercord.camera");
			camera.addTag("wildercord.rolled");
			level.addFreshEntity(camera);
			return camera.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			net.minecraft.world.entity.Entity camera = mc.level.getEntity(id);
			if (camera != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(camera);
			}
		});
	}

	/** Back to the player's own eyes after a director shot. */
	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		});
	}

	/** For a twist that follows its caster's steps (Rime Steps), walks the caster sideways, a step every few ticks. */
	private static void walkIfNeeded(ClientGameTestContext context, TestSingleplayerContext world, Resonance resonance, int from, int to) {
		if (!resonance.twist().equals("rime_steps")) {
			return;
		}
		for (int step = from; step < to; step++) {
			int s = step;
			on(world, player -> {
				player.teleportTo(player.level(), STAGE.getX() + 0.5 - 4 + s * 1.1, STAGE.getY(), STAGE.getZ() + 2.5, Set.<Relative>of(), 0.0F, PITCH, false);
				return null;
			});
			context.waitTicks(4);
		}
	}

	// ------------------------------------------------------------------ reading a rune

	/** A rune learned from its item goes unread, glimpsed, understood, filmed at each stage; runes known before are understood. */
	private static List<String> reading(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		RuneDef rune = Runes.FROST;
		// Runes the player knew already (everything, as an older player would) are understood, with nothing to read.
		boolean understoodBefore = context.computeOnClient(mc -> RuneReadingText.stage(Runes.FIRE) == RuneReading.Stage.UNDERSTOOD
			&& RuneReadingText.stage(rune) == RuneReading.Stage.UNDERSTOOD && RuneReadings.reading(mc.player).isEmpty());
		check(out, understoodBefore, "runes a player knew before reading existed should all be understood");
		// Forgotten, then learned again from its item, as a new player would.
		on(world, player -> {
			Spellbook book = Spellbooks.get(player);
			List<String> learned = new ArrayList<>(book.learned());
			learned.remove(rune.id());
			Spellbooks.set(player, new Spellbook(learned, book.spells(), book.selected(), book.starterGiven(), book.passives(), book.passivesOff(), book.names()));
			ItemStack item = RuneItem.stack(rune);
			player.setItemInHand(InteractionHand.MAIN_HAND, item);
			item.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return null;
		});
		context.waitTicks(5);
		RuneReading.Stage stage = context.computeOnClient(mc -> RuneReadingText.stage(rune));
		check(out, stage == RuneReading.Stage.UNREAD, "a rune just learned from its item should be unread, it's " + stage);
		String shown = context.computeOnClient(mc -> RuneReadingText.describe(rune).getString());
		check(out, shown.equals(RuneHints.hint(rune)), "an unread rune should show its hint, it shows: " + shown);
		codex(context, rune, "resonance_codex_unread");
		// The first cast glimpses it.
		husks(world);
		context.waitTicks(5);
		castRunes(context, world, List.of(Runes.BOLT.id(), rune.id()));
		context.waitTicks(20);
		stage = context.computeOnClient(mc -> RuneReadingText.stage(rune));
		check(out, stage == RuneReading.Stage.GLIMPSED, "a rune cast once should be glimpsed, it's " + stage);
		String glimpse = context.computeOnClient(mc -> RuneReadingText.describe(rune).getString());
		check(out, glimpse.equals(RuneItem.runeDescription(rune).getString()), "a glimpse is the rune's own text, its numbers veiled (drawn shimmering)");
		codex(context, rune, "resonance_codex_glimpsed");
		// A few more that land: understood, and nothing left to keep for it.
		List<String> trail = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			castRunes(context, world, List.of(Runes.BOLT.id(), rune.id()));
			context.waitTicks(25);
			trail.add(on(world, player -> RuneReadings.reading(player).get(rune.id()) + " (husks at " + huskHealth(player) + ")"));
		}
		stage = context.computeOnClient(mc -> RuneReadingText.stage(rune));
		check(out, stage == RuneReading.Stage.UNDERSTOOD, "a rune seen at work a few times should be understood, it's " + stage + "; progress " + trail);
		boolean dropped = on(world, player -> !RuneReadings.reading(player).containsKey(rune.id()));
		check(out, dropped, "an understood rune should leave nothing in the reading");
		codex(context, rune, "resonance_codex_understood");
		cleanup(context, world);
		return out;
	}

	/** Opens the Cord screen, finds {@code rune} in the Codex, hovers it, and films the tooltip. */
	private static void codex(ClientGameTestContext context, RuneDef rune, String name) {
		context.runOnClient(mc -> mc.gui.setScreen(new CordScreen()));
		context.waitTicks(5);
		context.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor(rune.name()));
		context.waitTicks(2);
		double[] at = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).codexPoint(rune.id()));
		if (at != null) {
			double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
			context.getInput().setCursorPos(at[0] * scale, at[1] * scale);
		}
		context.waitTicks(3);
		shot(context, name);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(3);
	}

	// ------------------------------------------------------------------ helpers

	private static Draw draw(TestSingleplayerContext world) {
		return on(world, player -> {
			MinecraftServer server = server(player);
			List<Resonance> all = WorldResonances.of(server);
			return new Draw(server.overworld().getSeed(), all.stream().map(Resonance::id).toList(), all.stream().map(Resonance::runes).toList(),
				all.stream().map(Resonance::name).toList(), WorldResonances.quirks(server).stream().map(RuneQuirks.Quirk::text).toList());
		});
	}

	/** Threads {@code resonance}'s runes as spell 1 and casts it for real, with mana to spare and no cooldown. */
	private static void cast(ClientGameTestContext context, TestSingleplayerContext world, Resonance resonance) {
		castRunes(context, world, resonance.runes());
	}

	private static void castRunes(ClientGameTestContext context, TestSingleplayerContext world, List<String> runes) {
		on(world, player -> {
			Spellbooks.set(player, Spellbooks.get(player).withSpell(0, runes).withSelected(0));
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, 500);
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, PITCH, false);
			SpellCaster.cast(player, 0);
			return null;
		});
		context.waitTicks(2);
	}

	/** The test's husks' health, for a failure's message. */
	private static String huskHealth(ServerPlayer player) {
		return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), e -> e.entityTags().contains(TAG)).stream()
			.map(e -> String.valueOf(Math.round(e.getHealth()))).toList().toString();
	}

	private static WildercordAttachments.WorldLore lore(net.minecraft.world.entity.player.Player player) {
		return player.getAttachedOrElse(WildercordAttachments.WORLD_LORE, WildercordAttachments.WorldLore.NONE);
	}

	private static MinecraftServer server(ServerPlayer player) {
		return player.level().getServer();
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static void check(List<String> failures, boolean ok, String what) {
		if (!ok) {
			failures.add(what);
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	/** An Echo Cord, every rune known, survival, a stone platform in the sky at noon, and nothing spawning. */
	private static void stage(TestSingleplayerContext world) {
		world.getServer().runCommand("gamerule spawn_mobs false");
		world.getServer().runCommand("gamerule advance_time false");
		world.getServer().runCommand("time set 6000");
		world.getServer().runCommand("weather clear");
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 1) + " " + (z - 4) + " " + (x + 10) + " " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 4) + " " + (x + 10) + " " + (y + 8) + " " + (z + 24) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setGameMode(GameType.SURVIVAL);
			sturdy(player);
			player.teleportTo(server.overworld(), x + 0.5, y, z + 0.5, Set.<Relative>of(), 0.0F, PITCH, false);
			player.setDeltaMovement(Vec3.ZERO);
		});
	}

	/** Three husks standing still in a row in front of the player, and one close by at their side; sturdy, never Runebound. */
	private static void husks(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			for (int i = -1; i <= 2; i++) {
				Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				if (husk == null) {
					continue;
				}
				// The fourth stands within reach of the player, off the line their spells fly along.
				double x = i == 2 ? 2.8 : i * 2.2;
				double z = i == 2 ? 2.0 : 6.0 + Math.abs(i);
				husk.snapTo(STAGE.getX() + 0.5 + x, STAGE.getY(), STAGE.getZ() + z, 180, 0);
				husk.setNoAi(true);
				husk.addTag(TAG);
				husk.addTag("wildercord.rolled");
				level.addFreshEntity(husk);
				sturdy(husk);
			}
		});
	}

	private static void sturdy(net.minecraft.world.entity.LivingEntity mob) {
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(200);
		}
		mob.setHealth(mob.getMaxHealth());
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=wildercord:rune_bolt]");
		world.getServer().runCommand("kill @e[type=minecraft:skeleton_horse]");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.stopRiding();
			player.clearFire();
			player.setHealth(player.getMaxHealth());
			player.teleportTo(server.overworld(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, PITCH, false);
		});
		context.waitTicks(5);
	}
}
