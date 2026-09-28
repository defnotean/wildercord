package dev.wildercord.gametest;

import dev.wildercord.Wildercord;
import dev.wildercord.advancement.Advancements;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.HeartCircles;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

/**
 * The Wildercord advancement tab in a real world: the server loaded all of it (the root and one
 * advancement for every feat), and the mod's own criteria grant them. A feat, a reaction, a secret,
 * forming a Heart Circle, learning runes, wearing a Cord and casting each award theirs (and only
 * theirs), a feat with no advancement does nothing, and an advancement taken away comes back when
 * the player's state is checked again, as it is on login.
 *
 * <p>Runs in the full suite; skipped with {@code WILDERCORD_TOUR_ONLY} or {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordAdvancementTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			String failure = world.getServer().computeOnServer(server -> {
				try {
					check(server);
					return null;
				} catch (AssertionError e) {
					return e.getMessage();
				}
			});
			if (failure != null) {
				throw new AssertionError("Advancements went wrong: " + failure);
			}
			// The Heart Circle formed above wakes an innate rune a few seconds later: let it happen cleanly.
			context.waitTicks(80);
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	private static AdvancementHolder get(MinecraftServer server, String path) {
		AdvancementHolder holder = server.getAdvancements().get(Wildercord.id(path));
		check(holder != null, "the server didn't load wildercord:" + path);
		return holder;
	}

	private static boolean done(MinecraftServer server, ServerPlayer player, String path) {
		return player.getAdvancements().getOrStartProgress(get(server, path)).isDone();
	}

	private static void expect(MinecraftServer server, ServerPlayer player, String path, boolean want, String after) {
		check(done(server, player, path) == want, "wildercord:" + path + (want ? " wasn't granted " : " was granted too early, ") + after);
	}

	private static void revoke(MinecraftServer server, ServerPlayer player, String path) {
		AdvancementHolder holder = get(server, path);
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		for (String criterion : progress.getCompletedCriteria()) {
			player.getAdvancements().revoke(holder, criterion);
		}
	}

	private static void check(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		player.setGameMode(GameType.SURVIVAL);

		// Loaded: the root (the tab), every branch, and an advancement for every feat.
		List<String> ours = new ArrayList<>();
		for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
			if (holder.id().getNamespace().equals(Wildercord.MOD_ID) && !holder.id().getPath().startsWith("recipes/")) {
				ours.add(holder.id().getPath());
			}
		}
		check(ours.size() >= 45, "only " + ours.size() + " Wildercord advancements loaded: " + ours);
		check(get(server, "root").value().display().map(d -> d.background().isPresent()).orElse(false), "the root has no background");
		for (String path : List.of("cords/echo", "casting/first_cast", "heart/circle_8", "discovery/runes_all", "discovery/grimoire",
				"world/archive", "world/archivist", "shields/glyph")) {
			get(server, path);
		}
		for (Feats.Feat feat : Feats.FEATS) {
			boolean found = ours.stream().anyMatch(path -> path.endsWith("/" + feat.id()));
			check(found, "no advancement loaded for the feat " + feat.id());
		}

		// A fresh player has none of it.
		for (String path : List.of("casting/overcast", "heart/circle_1", "discovery/runes_10", "cords/twine", "casting/first_cast")) {
			expect(server, player, path, false, "to a fresh player");
		}

		// A feat.
		Grimoire.feat(player, Feats.OVERCAST);
		expect(server, player, "casting/overcast", true, "after the Overcast feat");
		expect(server, player, "casting/rhythm", false, "after the Overcast feat");
		// A feat nothing listens for yet (another add-on's, or one still to come) is harmless.
		Grimoire.feat(player, "not_a_real_feat");

		// A reaction and a secret spell.
		Grimoire.reaction(player, "shatter");
		expect(server, player, "discovery/shatter", true, "after setting off Shatter");
		expect(server, player, "discovery/conduct", false, "after setting off Shatter");
		Grimoire.unlock(player, Secrets.SUNFALL.key());
		expect(server, player, "discovery/secret", true, "after finding Sunfall");
		expect(server, player, "discovery/all_secrets", false, "after finding one secret");

		// Forming a Heart Circle.
		player.setAttached(WildercordAttachments.CIRCLES, 0);
		HeartCircles.form(player);
		expect(server, player, "heart/circle_1", true, "after forming the 1st Circle");
		expect(server, player, "heart/circle_2", false, "after forming the 1st Circle");

		// Learning runes: ten, then every one.
		List<RuneDef> learnable = Runes.all().stream().filter(r -> !Runes.innate(r)).toList();
		Spellbook book = Spellbooks.get(player);
		for (RuneDef rune : learnable.subList(0, 10)) {
			book = book.learn(rune.id());
		}
		Spellbooks.set(player, book);
		expect(server, player, "discovery/runes_10", true, "after learning 10 runes");
		expect(server, player, "discovery/runes_50", false, "after learning " + Spellbooks.get(player).learned().size() + " runes");
		for (RuneDef rune : learnable) {
			book = book.learn(rune.id());
		}
		Spellbooks.set(player, book);
		expect(server, player, "discovery/runes_100", true, "after learning every rune");
		expect(server, player, "discovery/runes_all", true, "after learning every rune");

		// Wearing a Cord: a better one counts for the ones before it.
		Spellbooks.setCord(player, new ItemStack(WildercordItems.COPPER_CORD));
		expect(server, player, "cords/twine", true, "after wearing a Copper Cord");
		expect(server, player, "cords/copper", true, "after wearing a Copper Cord");
		expect(server, player, "cords/amethyst", false, "after wearing a Copper Cord");
		expect(server, player, "root", true, "after wearing a Cord");

		// Casting: any spell, then a long one.
		Advancements.cast(player, 3);
		expect(server, player, "casting/first_cast", true, "after a cast");
		expect(server, player, "casting/long_cast", false, "after a 3-rune cast");
		Advancements.cast(player, 6);
		expect(server, player, "casting/long_cast", true, "after a 6-rune cast");

		// Caught up on login: taken away, then given back from what the player already has.
		for (String path : List.of("discovery/runes_10", "heart/circle_1", "cords/copper", "casting/overcast", "discovery/shatter")) {
			revoke(server, player, path);
			expect(server, player, path, false, "after revoking it");
		}
		Advancements.sync(player);
		for (String path : List.of("discovery/runes_10", "heart/circle_1", "cords/copper", "casting/overcast", "discovery/shatter")) {
			expect(server, player, path, true, "again when the player's state was checked (as on login)");
		}
		// ...but moments aren't state: a revoked first cast stays revoked until the next cast.
		revoke(server, player, "casting/first_cast");
		Advancements.sync(player);
		expect(server, player, "casting/first_cast", false, "by a state check alone");
	}
}
