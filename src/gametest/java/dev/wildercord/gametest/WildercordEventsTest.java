package dev.wildercord.gametest;

import dev.wildercord.cast.Runebound;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.events.EventContent;
import dev.wildercord.cast.events.FallenStarBlockEntity;
import dev.wildercord.cast.events.FallenStars;
import dev.wildercord.cast.events.ManaStorm;
import dev.wildercord.cast.events.RiftSiege;
import dev.wildercord.cast.events.WorldEvents;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The world events, each started through its API as the command does: a mana storm (the player is
 * under it on both sides, spells cost less, mana flows faster, twenty casts earn Stormcaller, and it
 * crystallises one of its runes for the player once and never twice), a
 * fallen star (it lands with a rune inside, its guards rise when the player is near, it won't open
 * while they stand, nor when one is sent away without being killed, then, once they're killed, opens,
 * crumbles and earns Stargazer) and a rift siege (its first wave pours out, it can't be sealed in the
 * first wave, and once the second has come three spells of different elements seal it, earning
 * Riftwarden).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY} and {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordEventsTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			setup(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			run(failures, "mana storm", () -> storm(context, world));
			run(failures, "fallen star", () -> star(context, world));
			run(failures, "rift siege", () -> rift(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("World events went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void run(List<String> failures, String name, Runnable test) {
		try {
			test.run();
		} catch (AssertionError e) {
			failures.add(name + ": " + e.getMessage());
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
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	private static void setup(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			server.setDifficulty(Difficulty.NORMAL, true);
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			book = book.withSpell(0, ids(Runes.BOLT, Runes.HARM)).withSelected(0);
			Spellbooks.set(player, book);
		});
	}

	/** Casts spell {@code spell} now, whatever its cooldown, with full mana. */
	private static void castNow(ServerPlayer player, int spell) {
		Spellbooks.setReadyAt(player, spell, 0);
		Spellbooks.setMana(player, Mana.max(player));
		SpellCaster.cast(player, spell);
	}

	// ------------------------------------------------------------------ mana storm

	/** How many of a mana storm's runes the player carries. */
	private static int stormRunes(ServerPlayer player) {
		int n = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			RuneDef rune = RuneItem.runeOf(stack).orElse(null);
			if (rune != null && (rune.is(Runes.MANABURN.id()) || rune.is(Runes.MANATIDE.id()))) {
				n += stack.getCount();
			}
		}
		return n;
	}

	private static void storm(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] before = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			SpellCompiler.Compiled compiled = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM));
			int cost = Heart.manaCost(player, compiled);
			ManaStorm storm = WorldEvents.startStorm(player.level(), player, true);
			return new int[] {cost, storm == null ? 0 : 1};
		});
		check(before[1] == 1, "a storm should start right over the player");
		context.waitTicks(25);
		String server = world.getServer().computeOnServer(s -> {
			ServerPlayer player = player(s);
			if (!ManaStorm.inside(player)) {
				return "the player should be under the storm";
			}
			if (WorldEvents.stormAt(player.level(), player.position()) == null) {
				return "the storm should cover the player";
			}
			int cost = Heart.manaCost(player, SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)));
			if (cost >= before[0]) {
				return "spells should cost less under a storm (" + cost + ", was " + before[0] + ")";
			}
			if (Mana.of(player).regenMultiplier() < 2.0F - 1.0E-3F) {
				return "mana should flow twice as fast under a storm (x" + Mana.of(player).regenMultiplier() + ")";
			}
			return null;
		});
		check(server == null, server);
		boolean client = context.computeOnClient(mc -> mc.player != null && ManaStorm.inside(mc.player));
		check(client, "the client should know the player is under the storm (for the HUD and the sky)");

		// Twenty casts under it: Stormcaller. Some may surge on the way; none may throw.
		world.getServer().runOnServer(s -> {
			ServerPlayer player = player(s);
			for (int i = 0; i < 20; i++) {
				castNow(player, 0);
			}
		});
		context.waitTicks(20);
		boolean feat = world.getServer().computeOnServer(s -> Heart.discovered(player(s), "feat:" + Feats.STORMCALLER));
		check(feat, "twenty casts under a storm should earn Stormcaller");

		// Its runes: Manaburn or Manatide, once per storm for each player (a surge in those twenty casts may already have given one).
		String crystal = world.getServer().computeOnServer(s -> {
			ServerPlayer player = player(s);
			ManaStorm storm = WorldEvents.stormAt(player.level(), player.position());
			if (storm == null) {
				return "the storm should still be over the player";
			}
			int had = stormRunes(player);
			boolean first = storm.crystallise(player);
			boolean second = storm.crystallise(player);
			int after = stormRunes(player);
			if (second) {
				return "a storm should give each player one of its runes at most";
			}
			if (first != (had == 0) || after != 1) {
				return "the player should hold exactly one of the storm's runes (had " + had + ", has " + after + ")";
			}
			return null;
		});
		check(crystal == null, crystal);

		world.getServer().runOnServer(s -> WorldEvents.storms().forEach(ManaStorm::stop));
		context.waitTicks(5);
		boolean after = world.getServer().computeOnServer(s -> ManaStorm.inside(player(s)));
		check(!after, "once the storm ends the player shouldn't be under it");
	}

	// ------------------------------------------------------------------ fallen star

	private static void star(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos land = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return WorldEvents.startStar(player.level(), player, true);
		});
		check(land != null, "a star should find somewhere to land in front of the player");
		// It falls for two and a half seconds; its guards rise the next second, the player being close.
		context.waitTicks(100);
		BlockPos star = world.getServer().computeOnServer(server -> find(player(server).level(), land));
		check(star != null, "a Fallen Star should lie where it landed (" + land.toShortString() + ")");
		String held = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (!(level.getBlockEntity(star) instanceof FallenStarBlockEntity entity)) {
				return "the star should have its block entity";
			}
			RuneDef rune = Runes.get(entity.rune()).orElse(null);
			if (rune == null || rune.tier() < 3) {
				return "the star should hold a Tier III or IV rune (has " + entity.rune() + ")";
			}
			List<Mob> guards = FallenStars.guards(level, star);
			if (guards.size() < 2 || guards.size() > 4) {
				return "two to four guards should rise round the star (" + guards.size() + ")";
			}
			for (Mob guard : guards) {
				if (Runebound.spellOf(guard).isEmpty()) {
					return "every guard should be Runebound";
				}
				if (guard.canPickUpLoot()) {
					return "an event's monster shouldn't pick things up (it goes with its event, and what it carried would go too)";
				}
			}
			// Guarded: it won't open.
			FallenStars.open(level, star, player(server));
			if (!level.getBlockState(star).is(EventContent.FALLEN_STAR)) {
				return "the star shouldn't open while its guards stand";
			}
			// One sent away without being killed is no way in: another takes its place.
			guards.getFirst().discard();
			FallenStars.open(level, star, player(server));
			if (!level.getBlockState(star).is(EventContent.FALLEN_STAR)) {
				return "a guard sent away (not killed) shouldn't let the star open";
			}
			FallenStars.guards(level, star).forEach(guard -> guard.kill(level));
			FallenStars.open(level, star, player(server));
			if (level.getBlockState(star).is(EventContent.FALLEN_STAR)) {
				return "with its guards dead the star should open and crumble";
			}
			if (!Heart.discovered(player(server), "feat:" + Feats.STARGAZER)) {
				return "looting a star should earn Stargazer";
			}
			if (FallenStars.any(level)) {
				return "with the star gone, the world should be free for another to fall";
			}
			return null;
		});
		check(held == null, held);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		// Something built where a star is to land, while it falls: the star burns up rather than crush it.
		BlockPos second = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos at = WorldEvents.startStar(player.level(), player, true);
			if (at != null) {
				player.level().setBlockAndUpdate(at, Blocks.GOLD_BLOCK.defaultBlockState());
			}
			return at;
		});
		check(second != null, "a second star should find somewhere to land");
		context.waitTicks(80);
		String built = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			boolean kept = level.getBlockState(second).is(Blocks.GOLD_BLOCK);
			boolean lies = find(level, second) != null;
			level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
			if (!kept || lies) {
				return "a star landing where something was built while it fell should burn up and leave it be";
			}
			return FallenStars.any(level) ? "a star that burnt up shouldn't count as lying in its world" : null;
		});
		check(built == null, built);
		// A star taken away some other way (a command here; a Wither can't) still takes its guards with it.
		BlockPos third = world.getServer().computeOnServer(server -> WorldEvents.startStar(player(server).level(), player(server), true));
		check(third != null, "a third star should find somewhere to land");
		context.waitTicks(100);
		String removed = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			BlockPos at = find(level, third);
			if (at == null) {
				return "the third star should lie where it landed";
			}
			if (!level.getBlockState(at).is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) {
				return "a Fallen Star should be safe from the Wither";
			}
			List<Mob> guards = FallenStars.guards(level, at);
			if (guards.isEmpty()) {
				return "guards should have risen round the third star";
			}
			level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
			for (Mob guard : guards) {
				if (!guard.isRemoved()) {
					return "a star taken away should take its guards with it";
				}
			}
			return FallenStars.any(level) ? "a star taken away shouldn't count as lying in its world" : null;
		});
		check(removed == null, removed);
	}

	/** The star near where it was meant to land: in its cell, or sunk to its crater's floor. */
	private static BlockPos find(ServerLevel level, BlockPos land) {
		for (int dy = 1; dy >= -5; dy--) {
			BlockPos p = land.offset(0, dy, 0);
			if (level.getBlockState(p).is(EventContent.FALLEN_STAR)) {
				return p;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ rift siege

	private static void rift(ClientGameTestContext context, TestSingleplayerContext world) {
		boolean opened = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return WorldEvents.startRift(player.level(), player, true) != null;
		});
		check(opened, "a rift should open in front of the player");
		// The first wave comes three seconds in, one monster every few ticks.
		context.waitTicks(110);
		String wave = world.getServer().computeOnServer(server -> {
			RiftSiege rift = WorldEvents.riftIn(player(server).level());
			if (rift == null) {
				return "the rift should still be open";
			}
			if (rift.wave() != 1) {
				return "the first wave should have come (wave " + rift.wave() + ")";
			}
			if (rift.alive() < 1) {
				return "the first wave's monsters should be out";
			}
			// One of them turning into another (a zombie drowning, a skeleton freezing) is still one of its monsters.
			ServerLevel level = player(server).level();
			List<Mob> out = level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(rift.base(), rift.base()).inflate(48),
				m -> m.isAlive() && m.entityTags().contains(WorldEvents.TAG));
			if (!out.isEmpty()) {
				int before = rift.alive();
				Mob converted = out.getFirst().convertTo(net.minecraft.world.entity.EntityTypes.DROWNED,
					net.minecraft.world.entity.ConversionParams.single(out.getFirst(), true, false), mob -> {});
				if (converted == null || converted.isRemoved()) {
					return "a rift's monster turned into another should stay in the world, still the rift's";
				}
				if (rift.alive() != before) {
					return "a rift's monster turned into another should still count as one of its monsters (" + before + " out, then " + rift.alive() + ")";
				}
			}
			// Too raw to seal in its first wave.
			rift.strike(player(server), "fire");
			if (!rift.elements().isEmpty()) {
				return "a spell shouldn't count toward sealing the rift in its first wave";
			}
			// On to the second.
			rift.nextWave();
			return rift.wave() == 2 ? null : "the second wave should come (wave " + rift.wave() + ")";
		});
		check(wave == null, wave);
		context.waitTicks(40);

		// Three spells of different elements, bursting against the tear, seal it.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			RiftSiege rift = WorldEvents.riftIn(player.level());
			Vec3 at = rift.base().add(1.2, 0, 0.4);
			player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), 90, 0, false);
			Spellbook book = Spellbooks.get(player)
				.withSpell(0, ids(Runes.BURST, Runes.FIRE))
				.withSpell(1, ids(Runes.BURST, Runes.FROST))
				.withSpell(2, ids(Runes.BURST, Runes.SHOCK));
			Spellbooks.set(player, book);
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (int spell = 0; spell < 3; spell++) {
				castNow(player, spell);
			}
		});
		context.waitTicks(10);
		String sealed = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (WorldEvents.riftIn(player.level()) != null) {
				RiftSiege rift = WorldEvents.riftIn(player.level());
				return "three elements should seal the rift (it has taken " + rift.elements() + ")";
			}
			if (!Heart.discovered(player, "feat:" + Feats.RIFTWARDEN)) {
				return "sealing a rift should earn Riftwarden";
			}
			return null;
		});
		check(sealed == null, sealed);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
	}
}
