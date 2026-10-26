package dev.wildercord.guild;

import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.guild.GuildRules.Guild;
import dev.wildercord.guild.GuildRules.Kind;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Founding a guild and a coven in the real game, through the player's own commands: refused before learning a breathing
 * method and short of emeralds (none taken), founded for the full cost, a bad or taken name refused for the coven, a coven
 * founded alongside the guild, an away member kicked by name, the leader leaving handing the guild to the longest-standing
 * member, both kept through a save and reopen, and an invitation
 * joining only the kind it was for.
 */
public final class GuildFoundingTest implements FabricClientGameTest {
	private static final String GUILD = "Ashen Blades";
	private static final String COVEN = "Grey Lantern";
	private static final UUID WREN = UUID.fromString("5e1d0c2a-7b44-4c3e-9a51-0f2b8d6e4a17");
	private static final UUID ODA = UUID.fromString("9a3f61b8-2c05-4e7d-b1a9-6d4e2f8c0b35");

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static void run(MinecraftServer s, String command) {
		s.getCommands().performPrefixedCommand(p(s).createCommandSourceStack(), command);
	}

	private static void emeralds(MinecraftServer s, int n) {
		p(s).getInventory().clearContent();
		p(s).getInventory().add(new ItemStack(Items.EMERALD, n));
	}

	private static int emeralds(MinecraftServer s) {
		return p(s).getInventory().countItem(Items.EMERALD);
	}

	private static Guild of(MinecraftServer s, Kind kind) {
		return Guilds.ledger(s).roster().of(p(s).getUUID(), kind);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runOnServer(s -> {
				p(s).setGameMode(GameType.SURVIVAL);
				Spellbooks.setCord(p(s), ItemStack.EMPTY);

				// Before a breathing method the guild is refused and the emeralds kept.
				emeralds(s, 40);
				run(s, "guild found " + GUILD);
				check(of(s, Kind.GUILD) == null && emeralds(s) == 40, "A guild is refused to one with no breathing method");

				// A swordsman short of the cost is refused too, and keeps what they had.
				p(s).setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FLOW, AuraRules.threshold(AuraRules.FLOW), 0, 0));
				check(Guilds.eligible(p(s), Kind.GUILD), "A breathing method makes a swordsman");
				emeralds(s, GuildRules.FOUND_COST - 1);
				run(s, "guild found " + GUILD);
				check(of(s, Kind.GUILD) == null && emeralds(s) == GuildRules.FOUND_COST - 1, "Short of the cost, nothing is founded or taken");

				// With enough, it is founded for exactly the cost and led by its founder.
				emeralds(s, 40);
				run(s, "guild found " + GUILD);
				Guild guild = of(s, Kind.GUILD);
				check(guild != null && guild.name().equals(GUILD) && guild.leader().equals(p(s).getUUID()), "The guild is founded and led by its founder");
				check(emeralds(s) == 40 - GuildRules.FOUND_COST, "Founding takes the cost, had " + emeralds(s));

				// A coven wants a mage, a good name and one not taken in any case.
				emeralds(s, 40);
				run(s, "coven found " + COVEN);
				check(of(s, Kind.COVEN) == null && emeralds(s) == 40, "A coven is refused to one who isn't a mage");
				p(s).setAttached(WildercordAttachments.CIRCLES, 1);
				run(s, "coven found ab");
				check(of(s, Kind.COVEN) == null, "A name too short is refused");
				run(s, "coven found " + GUILD.toLowerCase());
				check(of(s, Kind.COVEN) == null && emeralds(s) == 40, "A name a guild already has is refused, whatever its case");
				run(s, "coven found " + COVEN);
				check(of(s, Kind.COVEN) != null && of(s, Kind.GUILD) != null, "A mage swordsman stands in a coven and a guild at once");
				check(emeralds(s) == 40 - GuildRules.FOUND_COST, "The coven takes its own cost");

				// Two members who are away: they count for nothing near the leader.
				Guilds.Ledger ledger = Guilds.ledger(s);
				ledger.remember(WREN, "Wren");
				ledger.remember(ODA, "Oda");
				check(ledger.change(r -> r.join(WREN, GUILD)) == GuildRules.Result.OK, "Wren joins");
				check(ledger.change(r -> r.join(ODA, GUILD)) == GuildRules.Result.OK, "Oda joins");
				check(Guilds.nearby(p(s), Kind.GUILD) == 0 && Guilds.bonus(p(s), Kind.GUILD) == 1, "Members away don't count");
				run(s, "guild");
			});
			c.waitTicks(3);
			c.takeScreenshot(TestScreenshotOptions.of("guild_roster").disableCounterPrefix());

			w.getServer().runOnServer(s -> {
				// The leader kicks an away member by name.
				run(s, "guild kick oda");
				check(!of(s, Kind.GUILD).has(ODA) && of(s, Kind.GUILD).members().size() == 2, "Oda is kicked by name while away");

				// The leader leaving hands the guild to the longest-standing member.
				run(s, "guild leave");
				check(of(s, Kind.GUILD) == null, "The leader is out of the guild");
				Guild after = Guilds.ledger(s).roster().named(GUILD);
				check(after != null && after.leader().equals(WREN) && after.members().size() == 1, "Wren now leads what is left");
				run(s, "guild disband");
				check(Guilds.ledger(s).roster().named(GUILD) != null, "One out of the guild can't disband it");
			});
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> {
				Guild guild = Guilds.ledger(s).roster().named(GUILD);
				check(guild != null && guild.leader().equals(WREN), "The guild and its new leader are kept through a reopen");
				Guild coven = of(s, Kind.COVEN);
				check(coven != null && coven.name().equals(COVEN) && coven.leader().equals(p(s).getUUID()), "The coven is kept through a reopen");

				// The last one out ends a coven.
				run(s, "coven leave");
				check(Guilds.ledger(s).roster().named(COVEN) == null, "The last member leaving ends the coven");

				// An invitation names its kind: one to a coven can't land its guest in a guild of the same name.
				check(Guilds.ledger(s).change(r -> r.join(ODA, GUILD, Kind.COVEN)) == GuildRules.Result.NO_SUCH, "A coven invitation doesn't join a guild");
				check(!Guilds.ledger(s).roster().named(GUILD).has(ODA), "and Oda is still out of the guild");
			});
		}
	}
}
