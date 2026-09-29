package dev.wildercord.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.CordScreen;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.loadout.LoadoutData;
import dev.wildercord.loadout.LoadoutRules;
import dev.wildercord.loadout.Loadouts;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loadouts, driven through the Cord screen's panel with the real mouse and keyboard where a player
 * would, and checked against what the server saved: saving the Cord as a new loadout and renaming it,
 * changing the spells and loading it back (the screen shows the loaded spells); a loadout holding a
 * rune the player no longer knows, and one from a bigger Cord, load with those runes kept but quiet;
 * loading puts each changed spell on its cooldown and leaves one already cooling alone; loading is
 * refused, and says why, while a spell is being charged; the limit of six (and deleting, which asks
 * twice); the quick-switch order; and the panel's screenshots, {@code loadouts_panel} (1920x1080) and
 * {@code loadouts_panel_854x480}, both at GUI scale 2.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordLoadoutsTest implements FabricClientGameTest {
	private static final int LEFT = 0;
	private static final int LOAD = 0;
	private static final int RENAME = 2;
	private static final int DELETE = 3;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1920, 1080);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			List<String> failures = new ArrayList<>();
			attempt(failures, "saving and loading from the panel", () -> saveAndLoad(context, world));
			attempt(failures, "quiet runes", () -> quietRunes(context, world));
			attempt(failures, "cooldowns", () -> cooldowns(context, world));
			attempt(failures, "refused while charging", () -> refusedWhileCharging(context, world));
			attempt(failures, "the limit of six", () -> limit(context, world));
			attempt(failures, "the quick switch", () -> quickSwitch(context, world));
			attempt(failures, "the panel's screenshots", () -> screenshots(context, world));
			context.runOnClient(mc -> mc.gui.setScreen(null));
			if (!failures.isEmpty()) {
				throw new AssertionError("Loadouts went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void attempt(List<String> failures, String what, Runnable test) {
		try {
			test.run();
		} catch (AssertionError | RuntimeException e) {
			failures.add(what + ": " + e.getMessage());
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	private static List<String> ids(RuneDef... runes) {
		return Arrays.stream(runes).map(RuneDef::id).toList();
	}

	/** The translation key of a message, to tell which answer it was. */
	private static String key(Component message) {
		return message.getContents() instanceof TranslatableContents t ? t.getKey() : message.getString();
	}

	/**
	 * A fresh start: an Echo Cord, every rune known, spell 1 {@code Bolt · Harm}, spell 2
	 * {@code Self · Swift}, a Night Eye passive, no cooldowns and no loadouts.
	 */
	private static void fresh(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			player.removeAttached(WildercordAttachments.CHARGE);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			for (int i = 0; i < dev.wildercord.gear.SpellSlots.ALL; i++) {
				book = book.withSpell(i, List.of()).withName(i, "");
				Spellbooks.setReadyAt(player, i, 0);
			}
			book = book.withSpell(0, ids(Runes.BOLT, Runes.HARM)).withSpell(1, ids(Runes.SELF, Runes.SWIFT))
				.withPassive(0, ids(Runes.SELF, Runes.NIGHT_EYE)).withPassive(1, List.of()).withPassiveOn(0, true).withSelected(0);
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CIRCLES, 1);
			player.setAttached(Loadouts.DATA, LoadoutData.EMPTY);
			Spellbooks.setMana(player, dev.wildercord.player.Mana.max(player));
		});
	}

	private static CordScreen screen(ClientGameTestContext context) {
		return context.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen s ? s : null);
	}

	private static void openScreen(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.gui.setScreen(new CordScreen()));
		context.waitTicks(5);
		check(screen(context) != null, "the Cord screen should open");
	}

	private static void closeScreen(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(3);
	}

	/** Clicks a point given in GUI coordinates (the window is GUI scale times as large). */
	private static void click(ClientGameTestContext context, double[] gui) {
		check(gui != null, "nothing to click there (not on screen)");
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(LEFT);
		context.waitTicks(4);
	}

	private static void press(ClientGameTestContext context, int key) {
		context.getInput().pressKey(InputConstants.getKey(new KeyEvent(key, 0, 0)));
		context.waitTicks(4);
	}

	private static LoadoutData saved(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> Loadouts.data(player(server)));
	}

	private static Spellbook book(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> Spellbooks.get(player(server)));
	}

	// ------------------------------------------------------------------ the tests

	/** Save as new and rename through the panel, change everything, then load it back through the panel. */
	private static void saveAndLoad(ClientGameTestContext context, TestSingleplayerContext world) {
		fresh(world);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.set(player, Spellbooks.get(player).withName(1, "Zoom").withSelected(1));
		});
		context.waitTicks(5);
		Spellbook original = book(world);
		openScreen(context);
		click(context, screen(context).loadoutsPoint());
		check(screen(context).loadoutsOpen(), "the list badge should open the loadouts panel");
		// Save the Cord as a new loadout under the name offered ("Loadout 1").
		click(context, screen(context).saveNewLoadoutPoint());
		press(context, InputConstants.KEY_RETURN);
		context.waitTicks(3);
		LoadoutData data = saved(world);
		check(data.size() == 1, "saving as new should save one loadout (has " + data.size() + ")");
		check(data.get(0).name().equals("Loadout 1"), "the new loadout should take the name offered (is " + data.get(0).name() + ")");
		check(data.get(0).spells().equals(original.spells()) && data.get(0).names().equals(original.names())
			&& data.get(0).passives().equals(original.passives()) && data.get(0).selected() == 1,
			"the loadout should hold the whole Cord: every spell, its name, the passives and the selected spell");
		// Rename it with the keyboard.
		click(context, screen(context).loadoutPoint(0, RENAME));
		for (int i = 0; i < "Loadout 1".length(); i++) {
			press(context, InputConstants.KEY_BACKSPACE);
		}
		context.getInput().typeChars("Fighting");
		context.waitTicks(1);
		press(context, InputConstants.KEY_RETURN);
		context.waitTicks(3);
		check(saved(world).get(0).name().equals("Fighting"), "renaming should be saved (is " + saved(world).get(0).name() + ")");
		press(context, InputConstants.KEY_ESCAPE);
		check(!screen(context).loadoutsOpen(), "Esc should close the panel first");
		check(screen(context) != null, "and leave the Cord screen open");
		closeScreen(context);

		// Everything changes: other runes, no names, the passive off, another spell selected.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.set(player, Spellbooks.get(player).withSpell(0, ids(Runes.BEAM, Runes.SHOCK)).withSpell(1, List.of())
				.withName(1, "").withPassiveOn(0, false).withSelected(0));
		});
		context.waitTicks(5);
		openScreen(context);
		check(screen(context).rowRunes(0).equals(ids(Runes.BEAM, Runes.SHOCK)), "the changed spell should show before loading");
		click(context, screen(context).loadoutsPoint());
		click(context, screen(context).loadoutPoint(0, LOAD));
		context.waitTicks(5);
		Spellbook loaded = book(world);
		check(loaded.spells().equals(original.spells()), "loading should bring every spell back (has " + loaded.spells() + ")");
		check(loaded.name(1).equals("Zoom"), "loading should bring the spell's name back");
		check(loaded.passiveOn(0), "loading should switch the passive back on");
		check(loaded.selected() == 1, "loading should select the loadout's spell");
		check(loaded.learned().equals(original.learned()), "loading should never change the runes learned");
		check(saved(world).current() == 0, "the loadout loaded should be the current one");
		check(!screen(context).loadoutsOpen(), "loading should close the panel");
		check(screen(context).rowRunes(0).equals(ids(Runes.BOLT, Runes.HARM)), "the Cord screen should show the loaded spells (has " + screen(context).rowRunes(0) + ")");
		check(screen(context).editingRow() == 1, "the Cord screen should edit the loaded selected spell");
		closeScreen(context);
	}

	/** A rune no longer known, and a loadout from a bigger Cord, load with their runes kept but quiet. */
	private static void quietRunes(ClientGameTestContext context, TestSingleplayerContext world) {
		fresh(world);
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			List<String> twelve = new ArrayList<>(ids(Runes.BOLT, Runes.HARM, Runes.METEOR));
			while (twelve.size() < CordTier.MAX_SOCKETS) {
				twelve.add(Runes.PUSH.id());
			}
			Spellbook book = Spellbooks.get(player).withSpell(0, twelve).withSpell(3, ids(Runes.BOLT, Runes.FIRE));
			Spellbooks.set(player, book);
			if (!Loadouts.save(player, "Big", true).ok()) {
				return "saving should work";
			}
			// Meteor forgotten (as a removed add-on's rune would be), and spell 1 changed.
			List<String> learned = new ArrayList<>(book.learned());
			learned.remove(Runes.METEOR.id());
			Spellbook forgot = new Spellbook(learned, book.spells(), book.selected(), true, book.passives(), book.passivesOff(), book.names())
				.withSpell(0, ids(Runes.BOLT));
			Spellbooks.set(player, forgot);
			Loadouts.Result result = Loadouts.load(player, 0);
			if (!result.ok()) {
				return "loading should work (" + result.message().getString() + ")";
			}
			if (!key(result.message()).endsWith("loaded_quiet")) {
				return "loading should say runes stay quiet (said " + result.message().getString() + ")";
			}
			Spellbook now = Spellbooks.get(player);
			if (now.knows(Runes.METEOR.id())) {
				return "loading must never teach a rune";
			}
			if (!now.spells().get(0).equals(twelve)) {
				return "the unknown rune should stay threaded (has " + now.spells().get(0) + ")";
			}
			List<Integer> firing = SpellCaster.activeSockets(now.spells().get(0), now, 0, CordTier.ECHO);
			if (firing.contains(2) || firing.size() != CordTier.MAX_SOCKETS - 1) {
				return "the unknown rune should be quiet, and only it (firing " + firing + ")";
			}
			// The same loadout on a Twine Cord: every row kept, only three Tier I sockets of the first spell fire.
			Spellbooks.setCord(player, new ItemStack(WildercordItems.TWINE_CORD));
			Spellbooks.set(player, Spellbooks.get(player).withSpell(0, List.of()).withSpell(3, List.of()));
			result = Loadouts.load(player, 0);
			now = Spellbooks.get(player);
			String twine = !result.ok() ? "loading on a Twine Cord should work"
				: !now.spells().get(0).equals(twelve) || !now.spells().get(3).equals(ids(Runes.BOLT, Runes.FIRE)) ? "a smaller Cord should keep every rune and row"
				: !SpellCaster.activeSockets(now.spells().get(0), now, 0, CordTier.TWINE).equals(List.of(0, 1)) ? "only the Twine Cord's sockets and tiers should fire"
				: !SpellCaster.activeSockets(now.spells().get(3), now, 3, CordTier.TWINE).isEmpty() ? "a row the Twine Cord doesn't have should be quiet"
				: null;
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			return twine;
		});
		check(problem == null, problem);
	}

	/** Changed spells start their cooldown; unchanged ones and ones already cooling keep their time. */
	private static void cooldowns(ClientGameTestContext context, TestSingleplayerContext world) {
		fresh(world);
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbook book = Spellbooks.get(player).withSpell(2, ids(Runes.BOLT, Runes.PUSH));
			Spellbooks.set(player, book);
			Loadouts.save(player, "A", true);
			// Another Cord: spell 1 and 3 differ, spell 2 is the same.
			Spellbooks.set(player, book.withSpell(0, ids(Runes.BOLT, Runes.FIRE)).withSpell(2, ids(Runes.BEAM, Runes.SHOCK)));
			long now = player.level().getGameTime();
			for (int i = 0; i < dev.wildercord.gear.SpellSlots.ALL; i++) {
				Spellbooks.setReadyAt(player, i, 0);
			}
			Spellbooks.setReadyAt(player, 2, now + 400);
			Loadouts.Result result = Loadouts.load(player, 0);
			if (!result.ok()) {
				return "loading should work (" + result.message().getString() + ")";
			}
			long first = Spellbooks.readyAt(player, 0);
			if (first <= now) {
				return "a changed spell that was ready should start its cooldown";
			}
			if (Spellbooks.readyAt(player, 1) != 0) {
				return "an unchanged spell should stay ready";
			}
			if (Spellbooks.readyAt(player, 2) != now + 400) {
				return "a spell already cooling should keep its time (ready at " + Spellbooks.readyAt(player, 2) + ", not " + (now + 400) + ")";
			}
			// And it's a real cooldown: casting the loaded spell straight away is refused.
			SpellCaster.cast(player, 0);
			if (Spellbooks.readyAt(player, 0) != first) {
				return "the loaded spell shouldn't cast while it cools down";
			}
			return null;
		});
		check(problem == null, problem);
	}

	/** Loading while a spell is charged is refused, from the server and from the screen, with the reason. */
	private static void refusedWhileCharging(ClientGameTestContext context, TestSingleplayerContext world) {
		fresh(world);
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Loadouts.save(player, "Calm", true);
			Spellbooks.set(player, Spellbooks.get(player).withSpell(0, ids(Runes.BEAM, Runes.SHOCK)));
			player.setAttached(WildercordAttachments.CHARGE, new WildercordAttachments.Charge(0, player.level().getGameTime(), ids(Runes.BEAM)));
			Loadouts.Result result = Loadouts.load(player, 0);
			if (result.ok()) {
				return "loading while charging should be refused";
			}
			if (!key(result.message()).endsWith("charging")) {
				return "the refusal should say it's the charge (said " + result.message().getString() + ")";
			}
			if (!Spellbooks.get(player).spells().get(0).equals(ids(Runes.BEAM, Runes.SHOCK))) {
				return "a refused load shouldn't change the spells";
			}
			return null;
		});
		check(problem == null, problem);
		// Asked from the client too (the quick-switch key's packet): still refused.
		context.runOnClient(mc -> ClientPlayNetworking.send(new WildercordNetworking.LoadoutRequest(Loadouts.NEXT, -1, "")));
		context.waitTicks(5);
		check(book(world).spells().get(0).equals(ids(Runes.BEAM, Runes.SHOCK)), "the quick switch should be refused while charging too");
		world.getServer().runOnServer(server -> player(server).removeAttached(WildercordAttachments.CHARGE));
		context.waitTicks(2);
	}

	/** Six can be kept; a seventh is refused (from the panel and the server); deleting asks twice. */
	private static void limit(ClientGameTestContext context, TestSingleplayerContext world) {
		fresh(world);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (int i = 1; i < LoadoutRules.MAX; i++) {
				Loadouts.save(player, "Set " + i, false);
			}
		});
		context.waitTicks(5);
		check(saved(world).size() == LoadoutRules.MAX - 1, "five should save");
		openScreen(context);
		click(context, screen(context).loadoutsPoint());
		click(context, screen(context).saveNewLoadoutPoint());
		press(context, InputConstants.KEY_RETURN);
		context.waitTicks(3);
		check(saved(world).size() == LoadoutRules.MAX, "the sixth should save from the panel (has " + saved(world).size() + ")");
		check(screen(context).saveNewLoadoutPoint() == null, "with six saved the panel shouldn't offer to save another");
		String seventh = world.getServer().computeOnServer(server -> {
			Loadouts.Result result = Loadouts.save(player(server), "Seventh", false);
			return result.ok() ? "a seventh shouldn't save" : key(result.message()).endsWith("full") ? null : "the refusal should say it's full";
		});
		check(seventh == null, seventh);
		context.runOnClient(mc -> ClientPlayNetworking.send(new WildercordNetworking.LoadoutRequest(Loadouts.SAVE_NEW, -1, "Seventh")));
		context.waitTicks(5);
		check(saved(world).size() == LoadoutRules.MAX, "a seventh asked for by the client shouldn't save either");
		// Saving over one by name is still allowed.
		String over = world.getServer().computeOnServer(server -> Loadouts.save(player(server), "set 2", true).ok() ? null : "saving over one by name should work when full");
		check(over == null, over);
		// Delete asks twice.
		click(context, screen(context).loadoutPoint(5, DELETE));
		check(saved(world).size() == LoadoutRules.MAX, "one click on Delete should only ask");
		click(context, screen(context).loadoutPoint(5, DELETE));
		check(saved(world).size() == LoadoutRules.MAX - 1, "a second click should delete it (has " + saved(world).size() + ")");
		check(screen(context).saveNewLoadoutPoint() != null, "with one deleted there's room again");
		closeScreen(context);
	}

	/** The quick switch goes round in order, and names each. */
	private static void quickSwitch(ClientGameTestContext context, TestSingleplayerContext world) {
		fresh(world);
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			List<List<String>> spells = List.of(ids(Runes.BOLT, Runes.HARM), ids(Runes.BEAM, Runes.SHOCK), ids(Runes.CONE, Runes.FIRE));
			for (int i = 0; i < spells.size(); i++) {
				Spellbooks.set(player, Spellbooks.get(player).withSpell(0, spells.get(i)));
				Loadouts.save(player, "Q" + i, false);
			}
			player.setAttached(Loadouts.DATA, Loadouts.data(player).withCurrent(-1));
			for (int step = 0; step < 4; step++) {
				Spellbooks.setReadyAt(player, 0, 0);
				Loadouts.Result result = Loadouts.next(player);
				int expected = step % spells.size();
				if (!result.ok() || !key(result.message()).startsWith("message.wildercord.loadout.switched")) {
					return "switch " + (step + 1) + " should load and name a loadout (said " + result.message().getString() + ")";
				}
				if (Loadouts.data(player).current() != expected || !Spellbooks.get(player).spells().get(0).equals(spells.get(expected))) {
					return "switch " + (step + 1) + " should load Q" + expected;
				}
			}
			return null;
		});
		check(problem == null, problem);
		// From the client, as the key sends it.
		context.runOnClient(mc -> ClientPlayNetworking.send(new WildercordNetworking.LoadoutRequest(Loadouts.NEXT, -1, "")));
		context.waitTicks(5);
		check(saved(world).current() == 1, "the key's request should load the next loadout (current is " + saved(world).current() + ")");
	}

	/** The panel with a few loadouts, on a big window and a small one, both at GUI scale 2. */
	private static void screenshots(ClientGameTestContext context, TestSingleplayerContext world) {
		fresh(world);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbook book = Spellbooks.get(player);
			Spellbooks.set(player, book.withSpell(0, ids(Runes.BOLT, Runes.HOMING_MOD, Runes.FIRE, Runes.AMPLIFY, Runes.SPLIT_MOD, Runes.ON_HIT, Runes.BURST, Runes.EXPLODE)));
			Loadouts.save(player, "Fighting", false);
			Spellbooks.set(player, book.withSpell(0, ids(Runes.BEAM, Runes.BREAK)).withSpell(1, ids(Runes.SELF, Runes.NIGHT_EYE, Runes.HASTE)));
			Loadouts.save(player, "Mining", false);
			Spellbooks.set(player, book.withSpell(0, ids(Runes.SELF, Runes.HEAL, Runes.WIDEN)).withName(0, "Mend"));
			Loadouts.save(player, "Helping friends out", false);
			Loadouts.load(player, 0);
		});
		context.waitTicks(5);
		for (int[] size : new int[][] {{1920, 1080}, {854, 480}}) {
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(size[0], size[1]);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			context.waitTicks(5);
			openScreen(context);
			click(context, screen(context).loadoutsPoint());
			check(screen(context).loadoutsOpen(), "the panel should open at " + size[0] + "x" + size[1]);
			context.getInput().setCursorPos(2, 2);
			context.waitTicks(3);
			String name = size[0] == 1920 ? "loadouts_panel" : "loadouts_panel_" + size[0] + "x" + size[1];
			context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
			closeScreen(context);
		}
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
		});
		context.waitTicks(3);
	}
}
