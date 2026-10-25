package dev.wildercord.loadout;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.client.CordScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Loadouts in the real game: {@code /loadout save} and {@code load}, a load starting the cooldown of a
 * spell it changes but never cutting one short, the refusals (no name, a taken name, full, charging),
 * rename and delete keeping the current loadout right, the Cord screen's panel loading one over the
 * network, and every loadout kept through a save and reopen.
 */
public final class LoadoutsTest implements FabricClientGameTest {
	private static final List<String> FLAME = List.of(Runes.BOLT.id(), Runes.EMBER.id());
	private static final List<String> GALE = List.of(Runes.SPARK.id(), Runes.WINDCUT.id());

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(35);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbook book = Spellbook.EMPTY;
				for (String id : List.of(Runes.BOLT.id(), Runes.EMBER.id(), Runes.SPARK.id(), Runes.WINDCUT.id())) {
					book = book.learn(id);
				}
				Spellbooks.set(p, book.withSpell(0, FLAME).withName(0, "Flame"));
			});
			w.getServer().runCommand("execute as @p run loadout save Flame");
			w.getServer().runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				Spellbooks.set(p, Spellbooks.get(p).withSpell(0, GALE).withName(0, "Gale"));
			});
			w.getServer().runCommand("execute as @p run loadout save Gale");
			// Saving under a taken name, any case, saves over it and keeps its name.
			w.getServer().runCommand("execute as @p run loadout save gale");
			c.waitTicks(2);

			w.getServer().runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				LoadoutData data = Loadouts.data(p);
				check(data.names().equals(List.of("Flame", "Gale")), "Two loadouts saved by command, the second saved over by name: " + data.names());
				check(data.get(0).spells().getFirst().equals(FLAME) && data.get(1).spells().getFirst().equals(GALE), "Each keeps the spell it was saved with");
				check(!Loadouts.save(p, "§c§l  ", false).ok(), "A name of only formatting codes is refused");
				check(!Loadouts.save(p, "FLAME", false).ok(), "The Cord screen can't save a new loadout under a taken name");

				// Loading a loadout whose spell fires differently starts that spell's cooldown.
				long now = p.level().getGameTime();
				Spellbooks.setReadyAt(p, 0, 0);
				check(Loadouts.load(p, 0).ok(), "Flame loads");
				check(Spellbooks.get(p).spells().getFirst().equals(FLAME) && Spellbooks.get(p).names().getFirst().equals("Flame"), "Loading puts the spell and its name back on the Cord");
				long cooling = Spellbooks.readyAt(p, 0);
				check(cooling > now, "A spell changed by a load starts its cooldown");
				check(Loadouts.data(p).current() == 0, "Flame is the current loadout");

				// Swapping back while it cools never shortens the wait.
				check(Loadouts.load(p, 1).ok(), "Gale loads");
				check(Spellbooks.readyAt(p, 0) == cooling, "A spell cooling down keeps its time through a load");

				// Never while charging: the charge would go off as a different spell.
				p.setAttached(WildercordAttachments.CHARGE, new WildercordAttachments.Charge(0, now, GALE, 20, 1, 0, now + 20, false));
				check(!Loadouts.load(p, 0).ok(), "No loading while a spell charges");
				check(Spellbooks.get(p).spells().getFirst().equals(GALE), "A refused load leaves the Cord as it was");
				p.removeAttached(WildercordAttachments.CHARGE);

				// Full at the most a player keeps.
				for (int i = 3; i <= LoadoutRules.MAX; i++) {
					check(Loadouts.save(p, "Set " + i, false).ok(), "Loadout " + i + " saves");
				}
				check(!Loadouts.save(p, "One Too Many", false).ok() && Loadouts.data(p).size() == LoadoutRules.MAX, "No more than the most a player keeps");

				// Rename and delete.
				check(Loadouts.rename(p, 2, "Mining").ok() && Loadouts.data(p).get(2).name().equals("Mining"), "Rename");
				check(!Loadouts.rename(p, 2, "flame").ok(), "Rename can't take another loadout's name");
				check(Loadouts.load(p, 1).ok(), "Gale loads again");
				check(Loadouts.delete(p, 0).ok(), "Delete Flame");
				LoadoutData after = Loadouts.data(p);
				check(after.size() == LoadoutRules.MAX - 1 && after.get(0).name().equals("Gale") && after.current() == 0, "Deleting one above the current loadout moves it up: " + after.names() + " current " + after.current());
				check(Spellbooks.get(p).spells().getFirst().equals(GALE), "Deleting a loadout leaves the Cord as it is");

				// Make room and put Flame back, so the panel has a different setup to load.
				check(Loadouts.delete(p, Loadouts.find(p, "Set 6")).ok(), "Delete by name");
				Spellbooks.set(p, Spellbooks.get(p).withSpell(0, FLAME).withName(0, "Flame"));
				check(Loadouts.save(p, "Flame", false).ok(), "Flame saved again");
				Spellbooks.set(p, Spellbooks.get(p).withSpell(0, GALE).withName(0, "Gale"));
			});
			c.waitTicks(4);
			c.runOnClient(mc -> {
				LoadoutData data = Loadouts.data(mc.player);
				check(data.size() == LoadoutRules.MAX - 1 && data.names().contains("Mining") && data.names().contains("Flame"), "The loadouts reach the player's client: " + data.names());
				mc.getWindow().setWindowed(1280, 720);
				mc.resizeGui();
				mc.gui.toastManager().clear();
			});

			// The Cord screen: open the panel and load Flame (its last row) with its own button.
			c.setScreen(CordScreen::new);
			c.waitTicks(3);
			click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).loadoutsPoint()));
			check(c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).loadoutsOpen()), "The list badge opens the loadouts panel");
			c.takeScreenshot(TestScreenshotOptions.of("loadouts_panel").disableCounterPrefix());
			int flame = c.computeOnClient(mc -> Loadouts.data(mc.player).names().indexOf("Flame"));
			click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).loadoutPoint(flame, 0)));
			c.waitTicks(4);
			w.getServer().runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				check(Spellbooks.get(p).spells().getFirst().equals(FLAME), "The panel's Load button loads the loadout on the server");
				check(Loadouts.data(p).current() == flame, "and makes it the current one");
			});
			c.runOnClient(mc -> check(Spellbooks.get(mc.player).spells().getFirst().equals(FLAME), "The loaded Cord reaches the client"));
			c.takeScreenshot(TestScreenshotOptions.of("loadouts_loaded").disableCounterPrefix());
			c.setScreen(() -> null);
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> verifyKept(s.getPlayerList().getPlayers().getFirst()));
			c.runOnClient(mc -> verifyKept(mc.player));
		}
	}

	private static void verifyKept(Player p) {
		LoadoutData data = Loadouts.data(p);
		check(data.size() == LoadoutRules.MAX - 1 && data.names().getFirst().equals("Gale") && data.names().contains("Mining"), "Loadouts kept through a save and reopen: " + data.names());
		check(data.get(data.names().indexOf("Flame")).spells().getFirst().equals(FLAME), "A kept loadout keeps its spell");
		check(data.current() == data.names().indexOf("Flame"), "The current loadout is kept");
	}

	private static void click(ClientGameTestContext c, double[] point) {
		check(point != null, "Native control is visible");
		double scale = c.computeOnClient(mc -> mc.getWindow().getGuiScale());
		c.getInput().setCursorPos(point[0] * scale, point[1] * scale);
		c.waitTicks(1);
		c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		c.waitTicks(4);
	}

	private static void check(boolean yes, String why) {
		if (!yes) {
			throw new AssertionError(why);
		}
	}
}
