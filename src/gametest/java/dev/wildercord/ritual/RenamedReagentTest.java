package dev.wildercord.ritual;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.ritual.RitualRules.Ritual;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** A renamed reagent is still the reagent: Clear Skies is worked with a sunflower named in an anvil, and that sunflower is the one spent. */
public final class RenamedReagentTest implements FabricClientGameTest {
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(30);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runOnServer(s -> {
				var p = p(s);
				p.setGameMode(GameType.SURVIVAL);
				Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
				p.setAttached(WildercordAttachments.CIRCLES, 8);
				p.getInventory().clearContent();
				ItemStack named = new ItemStack(Items.SUNFLOWER, 2);
				named.set(DataComponents.CUSTOM_NAME, Component.literal("Morning Bloom"));
				p.getInventory().setItem(5, named);
			});
			w.getServer().runCommand("weather rain");
			c.waitTicks(5);
			w.getServer().runOnServer(s -> {
				var p = p(s);
				var l = s.overworld();
				// The weather flag, not isRaining(): the rain level takes many ticks to fade in or out.
				check(l.getWeatherData().isRaining(), "It rains before the ritual");
				Spellbooks.setMana(p, 1000);
				var first = Rituals.check(l, p, Ritual.CLEAR_SKIES);
				check(first == Rituals.Result.DONE, "The renamed sunflower is accepted as the reagent (" + first + ")");
				check(Rituals.perform(l, p, Ritual.CLEAR_SKIES) == Rituals.Result.DONE, "Clear Skies is worked");
				ItemStack left = p.getInventory().getItem(5);
				check(left.is(Items.SUNFLOWER) && left.getCount() == 1, "One renamed sunflower is spent (" + left + ")");
				check(left.get(DataComponents.CUSTOM_NAME) != null, "The rest keep their name");
				p.getInventory().clearContent();
				check(Rituals.check(l, p, Ritual.CLEAR_SKIES) == Rituals.Result.NO_REAGENT, "Without a sunflower the ritual is refused");
			});
			c.waitTicks(5);
			check(w.getServer().computeOnServer(s -> !s.overworld().getWeatherData().isRaining()), "The rain stops");
		}
	}

	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }

	private static void check(boolean yes, String why) { if (!yes) throw new AssertionError(why); }
}
