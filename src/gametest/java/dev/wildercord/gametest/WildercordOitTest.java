package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * Improved Transparency (order-independent transparency) draws translucent particles in passes of
 * its own, and vanilla stops the game if a translucent particle layer has no pipeline for them. This
 * turns it on and casts spells whose light glows and whose void darkens, and a Shield's circles, so
 * every Wildercord particle layer is drawn that way at least once. It passes by not crashing.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY}
 * and {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordOitTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		boolean before = context.computeOnClient(mc -> mc.options.improvedTransparency().get());
		context.runOnClient(mc -> mc.options.improvedTransparency().set(true));
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				book = book.withSpell(0, ids(Runes.NOVA, Runes.FIRE)).withSpell(1, ids(Runes.BOLT, Runes.HOLLOW))
					.withSpell(2, ids(Runes.SELF, Runes.SHIELD)).withSpell(3, ids(Runes.BURST, Runes.LIGHTNING));
				Spellbooks.set(player, book);
			});
			context.waitTicks(10);
			for (int spell = 0; spell < 4; spell++) {
				int s = spell;
				world.getServer().runOnServer(server -> {
					Spellbooks.setReadyAt(player(server), s, 0);
					SpellCaster.cast(player(server), s);
				});
				// Several frames with the spell's light, circles and darkness on screen.
				context.waitTicks(12);
			}
			context.takeScreenshot("oit_spells");
		} finally {
			context.runOnClient(mc -> mc.options.improvedTransparency().set(before));
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<String> ids(RuneDef... runes) {
		return Arrays.stream(runes).map(RuneDef::id).toList();
	}
}
