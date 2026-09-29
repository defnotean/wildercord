package dev.wildercord.gametest;

import dev.wildercord.client.CordScreen;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

/**
 * The Cord screen with every Cord, driven with the real mouse: selecting spells, threading runes
 * from the Codex, taking them off with either button, dragging to reorder, being refused (and told
 * why) for a locked spell, a rune too strong for the Cord and a full spell, the Passives page, and
 * switching page mid-drag. After every change the server's saved spellbook is checked too.
 *
 * <p>Runs in the full suite; {@code WILDERCORD_CORDS_ONLY=1} runs just this.</p>
 */
public class WildercordCordTest implements FabricClientGameTest {
	private static final int LEFT = 0;
	private static final int RIGHT = 1;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			List<String> failures = new ArrayList<>();
			for (CordTier tier : CordTier.values()) {
				try {
					checkCord(context, world, tier);
				} catch (AssertionError e) {
					failures.add(tier.name() + ": " + e.getMessage());
				}
				context.runOnClient(mc -> mc.gui.setScreen(null));
				context.waitTicks(5);
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("The Cord screen went wrong:\n  " + String.join("\n  ", failures));
			}
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

	private static Item cordItem(CordTier tier) {
		return switch (tier) {
			case TWINE -> WildercordItems.TWINE_CORD;
			case COPPER -> WildercordItems.COPPER_CORD;
			case AMETHYST -> WildercordItems.AMETHYST_CORD;
			case ECHO -> WildercordItems.ECHO_CORD;
		};
	}

	private static CordScreen screen(ClientGameTestContext context) {
		return context.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen s ? s : null);
	}

	/** Clicks a point given in GUI coordinates (the window is GUI scale times as large). */
	private static void click(ClientGameTestContext context, double[] gui, int button) {
		check(gui != null, "nothing to click there (not on screen)");
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(button);
		context.waitTicks(3);
	}

	private static List<String> clientRow(ClientGameTestContext context, int row) {
		return context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).rowRunes(row));
	}

	private static List<String> serverSpell(TestSingleplayerContext world, int spell) {
		return world.getServer().computeOnServer(server -> List.copyOf(Spellbooks.get(player(server)).spells().get(spell)));
	}

	private static List<String> ids(RuneDef... runes) {
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	private static void checkCord(ClientGameTestContext context, TestSingleplayerContext world, CordTier tier) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(cordItem(tier)));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			for (int i = 0; i < CordTier.MAX_SPELLS; i++) {
				book = book.withSpell(i, List.of()).withName(i, "");
			}
			for (int i = 0; i < dev.wildercord.spell.Passives.MAX; i++) {
				book = book.withPassive(i, List.of());
			}
			book = book.withSpell(0, ids(Runes.BOLT, Runes.PUSH)).withSelected(0);
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CIRCLES, 1);
		});
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gui.setScreen(new CordScreen()));
		context.waitTicks(5);
		check(screen(context) != null, "the Cord screen should open");

		// Selecting the 2nd spell: a Cord with room for it switches to it; the Twine Cord refuses, and says why.
		click(context, screen(context).rowPoint(1), LEFT);
		if (tier.spells >= 2) {
			check(screen(context).editingRow() == 1, "clicking spell 2 should select it");
			int selected = world.getServer().computeOnServer(server -> Spellbooks.get(player(server)).selected());
			check(selected == 1, "selecting spell 2 should tell the server (it has " + (selected + 1) + ")");
			click(context, screen(context).rowPoint(0), LEFT);
			check(screen(context).editingRow() == 0, "clicking spell 1 should go back to it");
			// A name typed for spell 1, then spell 2 picked: Enter mustn't give spell 2 that name.
			click(context, screen(context).toolPoint(0), LEFT);
			context.getInput().typeChars("Misnamed");
			context.waitTicks(1);
			click(context, screen(context).rowPoint(1), LEFT);
			context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.getKey(new net.minecraft.client.input.KeyEvent(
				com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0)));
			context.waitTicks(3);
			String names = world.getServer().computeOnServer(server -> Spellbooks.get(player(server)).name(0) + "|" + Spellbooks.get(player(server)).name(1));
			check(names.equals("|"), "a name being typed should be dropped when another spell is picked (names are " + names + ")");
			click(context, screen(context).rowPoint(0), LEFT);
		} else {
			check(screen(context).editingRow() == 0, "a locked spell shouldn't be selected");
			check(screen(context).lastRefusal() != null, "clicking a locked spell should say why it's locked");
		}

		// Taking a rune off with a left click.
		click(context, screen(context).socketPoint(0, 1), LEFT);
		check(clientRow(context, 0).equals(ids(Runes.BOLT)), "a left click on a threaded rune should take it off (has " + clientRow(context, 0) + ")");
		check(serverSpell(world, 0).equals(ids(Runes.BOLT)), "taking a rune off should be saved (server has " + serverSpell(world, 0) + ")");

		// Threading a rune from the Codex with a left click.
		context.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor("Harm"));
		click(context, screen(context).codexPoint(Runes.HARM.id()), LEFT);
		check(clientRow(context, 0).equals(ids(Runes.BOLT, Runes.HARM)), "a left click on a Codex rune should thread it (has " + clientRow(context, 0) + ")");
		check(serverSpell(world, 0).equals(ids(Runes.BOLT, Runes.HARM)), "threading a rune should be saved (server has " + serverSpell(world, 0) + ")");

		// A resize straight after an edit, before the server has sent the spellbook back, keeps the edit.
		String resized = context.computeOnClient(mc -> {
			CordScreen cord = (CordScreen) mc.gui.screen();
			double[] at = cord.socketPoint(0, 1);
			net.minecraft.client.input.MouseButtonEvent press = new net.minecraft.client.input.MouseButtonEvent(at[0], at[1],
				new net.minecraft.client.input.MouseButtonInfo(LEFT, 0));
			cord.mouseClicked(press, false);
			cord.mouseReleased(press);
			List<String> edited = cord.rowRunes(0);
			cord.resize(cord.width, cord.height);
			return edited.equals(ids(Runes.BOLT)) && cord.rowRunes(0).equals(edited) ? null : "took " + edited + ", then after a resize " + cord.rowRunes(0);
		});
		check(resized == null, "a resize straight after an edit should keep it (" + resized + ")");
		context.waitTicks(3);
		check(serverSpell(world, 0).equals(ids(Runes.BOLT)), "the edit made before the resize should be saved (server has " + serverSpell(world, 0) + ")");
		click(context, screen(context).codexPoint(Runes.HARM.id()), LEFT);
		check(clientRow(context, 0).equals(ids(Runes.BOLT, Runes.HARM)), "Harm should thread again (has " + clientRow(context, 0) + ")");

		// The same with the right button.
		click(context, screen(context).socketPoint(0, 1), RIGHT);
		check(clientRow(context, 0).equals(ids(Runes.BOLT)), "a right click on a threaded rune should take it off (has " + clientRow(context, 0) + ")");
		click(context, screen(context).codexPoint(Runes.HARM.id()), RIGHT);
		check(clientRow(context, 0).equals(ids(Runes.BOLT, Runes.HARM)), "a right click on a Codex rune should thread it (has " + clientRow(context, 0) + ")");

		// Dragging a rune to another socket reorders the spell.
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		double[] from = screen(context).socketPoint(0, 0);
		double[] to = screen(context).socketPoint(0, 1);
		context.getInput().setCursorPos(from[0] * scale, from[1] * scale);
		context.waitTicks(1);
		context.getInput().holdMouse(LEFT);
		context.waitTicks(1);
		for (int i = 1; i <= 4; i++) {
			context.getInput().setCursorPos((from[0] + (to[0] + 4 - from[0]) * i / 4) * scale, from[1] * scale);
			context.waitTicks(1);
		}
		context.getInput().releaseMouse(LEFT);
		context.waitTicks(3);
		check(clientRow(context, 0).equals(ids(Runes.HARM, Runes.BOLT)), "dragging a rune along the spell should move it (has " + clientRow(context, 0) + ")");
		check(serverSpell(world, 0).equals(ids(Runes.HARM, Runes.BOLT)), "a reorder should be saved (server has " + serverSpell(world, 0) + ")");

		// A rune too strong for the Cord is refused, with the reason; one it holds is threaded.
		context.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor("Lightning"));
		double[] lightning = screen(context).codexPoint(Runes.LIGHTNING.id());
		click(context, lightning, LEFT);
		if (tier.holds(Runes.LIGHTNING.tier())) {
			check(clientRow(context, 0).equals(ids(Runes.HARM, Runes.BOLT, Runes.LIGHTNING)), "a rune the Cord holds should thread (has " + clientRow(context, 0) + ")");
			click(context, screen(context).socketPoint(0, 2), LEFT);
		} else {
			check(clientRow(context, 0).equals(ids(Runes.HARM, Runes.BOLT)), "a rune too strong for the Cord shouldn't thread");
			check(screen(context).lastRefusal() != null, "a rune too strong for the Cord should say why");
		}

		// Filling the spell: every socket takes a rune, then one more is refused, with the reason.
		context.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor("Harm"));
		double[] harm = screen(context).codexPoint(Runes.HARM.id());
		for (int n = clientRow(context, 0).size(); n < tier.sockets; n++) {
			click(context, harm, LEFT);
		}
		check(clientRow(context, 0).size() == tier.sockets, "every socket should take a rune (has " + clientRow(context, 0).size() + " of " + tier.sockets + ")");
		click(context, harm, LEFT);
		check(clientRow(context, 0).size() == tier.sockets, "a full spell shouldn't take another rune");
		check(screen(context).lastRefusal() != null, "a full spell should say it's full");
		check(serverSpell(world, 0).size() == tier.sockets, "a full spell should be saved full (server has " + serverSpell(world, 0).size() + ")");

		// The Passives page: a buff goes in, a heal is refused (it can't be a passive).
		click(context, screen(context).pagePoint(1), LEFT);
		context.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor("Swift"));
		click(context, screen(context).codexPoint(Runes.SWIFT.id()), LEFT);
		check(clientRow(context, 0).equals(ids(Runes.SWIFT)), "a buff should go into the first passive (has " + clientRow(context, 0) + ")");
		List<String> passive = world.getServer().computeOnServer(server -> List.copyOf(Spellbooks.get(player(server)).passives().get(0)));
		check(passive.equals(ids(Runes.SWIFT)), "a passive should be saved (server has " + passive + ")");
		context.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor("Heal"));
		click(context, screen(context).codexPoint(Runes.HEAL.id()), LEFT);
		check(clientRow(context, 0).equals(ids(Runes.SWIFT)), "a heal shouldn't go into a passive");
		check(screen(context).lastRefusal() != null, "a rune that can't be a passive should say why");

		// Pressing the other button mid-drag (over the Passives tab) is ignored: no page switch, no crash; letting go
		// off the Cord takes the dragged rune off, as it always does.
		click(context, screen(context).pagePoint(0), LEFT);
		double[] held = screen(context).socketPoint(0, 0);
		double[] passives = screen(context).pagePoint(1);
		context.getInput().setCursorPos(held[0] * scale, held[1] * scale);
		context.waitTicks(1);
		context.getInput().holdMouse(LEFT);
		context.getInput().setCursorPos(passives[0] * scale, passives[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(RIGHT);
		context.waitTicks(1);
		context.getInput().releaseMouse(LEFT);
		context.waitTicks(3);
		check(screen(context) != null, "the Cord screen should survive another button mid-drag");
		check(screen(context).editingRow() == 0 && serverSpell(world, 0).size() == tier.sockets - 1,
			"a rune dragged off the Cord should come off (server has " + serverSpell(world, 0).size() + " of " + tier.sockets + ")");
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(3);

		// Casting: each of the Cord's spells casts; a spell past what the Cord holds doesn't.
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			long now = player.level().getGameTime();
			for (int spell = 0; spell < CordTier.MAX_SPELLS; spell++) {
				Spellbooks.set(player, Spellbooks.get(player).withSpell(spell, ids(Runes.BOLT, Runes.HARM)));
				Spellbooks.setReadyAt(player, spell, 0);
				Spellbooks.setMana(player, dev.wildercord.player.Mana.max(player));
				dev.wildercord.cast.SpellCaster.cast(player, spell);
				boolean went = Spellbooks.readyAt(player, spell) > now;
				if (went != spell < tier.spells) {
					return "spell " + (spell + 1) + (went ? " cast, but the Cord holds only " + tier.spells : " didn't cast");
				}
			}
			return null;
		});
		check(cast == null, cast);
	}
}
