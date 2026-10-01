package dev.wildercord.gametest;

import dev.wildercord.api.SpellMasteryApi;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.PowerPlaces;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.WorldBonds;
import dev.wildercord.cast.WorldResonances;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.Resonance;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneQuirks;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Where the world's own magic, mastery and residues meet (see WorldBonds): a harmony bound to a ley crossing stays
 * asleep away from one and wakes at one; and a spell with one of this world's quirked runes is offered World-Tuned.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordBondsTest implements FabricClientGameTest {
	/** Where crossings are looked for: this far out from the origin, in steps of this. */
	private static final int SEARCH = 6000;
	private static final int STEP = 8;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				Spellbooks.set(player, book);
				player.setGameMode(GameType.CREATIVE);
			});
			List<String> failures = new ArrayList<>();
			crossingHarmony(context, world, failures);
			worldTuned(world, failures);
			if (!failures.isEmpty()) {
				throw new AssertionError("Where the world's magic meets mastery and residues:\n  " + String.join("\n  ", failures));
			}
		}
	}

	/** A crossing-bound harmony: asleep at an ordinary spot, awake at a ley crossing. */
	private static void crossingHarmony(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		Optional<Resonance> bound = world.getServer().computeOnServer(server ->
			WorldResonances.of(server).stream().filter(WorldBonds::crossingBound).findFirst());
		if (bound.isEmpty()) {
			// Seeds draw names, and names decide which are bound: this world happens to have none.
			return;
		}
		Resonance harmony = bound.get();
		BlockPos plain = world.getServer().computeOnServer(server -> find(server, false));
		BlockPos crossing = world.getServer().computeOnServer(server -> find(server, true));
		if (crossing == null) {
			failures.add("no ley crossing within " + SEARCH + " blocks to test at");
			return;
		}
		stand(context, world, plain);
		boolean placeOfPower = world.getServer().computeOnServer(server -> PowerPlaces.isPlaceOfPower(server.overworld(), player(server).blockPosition()));
		if (placeOfPower) {
			failures.add("the ordinary spot " + plain + " counts as a place of power");
		}
		cast(context, world, harmony);
		if (discovered(world, harmony)) {
			failures.add(harmony.name() + " woke away from a ley crossing");
		}
		stand(context, world, crossing);
		placeOfPower = world.getServer().computeOnServer(server -> PowerPlaces.isPlaceOfPower(server.overworld(), player(server).blockPosition()));
		if (!placeOfPower) {
			failures.add("the crossing at " + crossing + " isn't a place of power");
		}
		cast(context, world, harmony);
		if (!discovered(world, harmony)) {
			failures.add(harmony.name() + " didn't wake at a ley crossing");
		}
	}

	/** A spell with one of this world's quirked runes is offered World-Tuned. */
	private static void worldTuned(TestSingleplayerContext world, List<String> failures) {
		String trouble = world.getServer().computeOnServer(server -> {
			List<RuneQuirks.Quirk> quirks = WorldResonances.quirks(server);
			if (quirks.isEmpty()) {
				return null;
			}
			RuneDef quirked = Runes.get(quirks.getFirst().rune()).orElse(null);
			if (quirked == null) {
				return "a quirk names a rune that doesn't exist: " + quirks.getFirst().rune();
			}
			List<RuneDef> spell = List.of(Runes.BOLT, quirked);
			boolean offered = SpellMasteryApi.offers(player(server), "test", spell, 2).stream()
				.anyMatch(w -> w.trait().equals(WorldBonds.WORLD_TUNED.id()));
			boolean plain = SpellMasteryApi.offers(player(server), "test", List.of(Runes.BOLT, unquirked(quirks)), 2).stream()
				.anyMatch(w -> w.trait().equals(WorldBonds.WORLD_TUNED.id()));
			if (!offered) {
				return "World-Tuned isn't offered for a spell with the quirked rune " + quirked.id();
			}
			if (plain) {
				return "World-Tuned is offered for a spell with no quirked rune";
			}
			return MasteryTraits.get(WorldBonds.WORLD_TUNED.id()).isPresent() ? null : "World-Tuned isn't registered";
		});
		if (trouble != null) {
			failures.add(trouble);
		}
	}

	/** A harmful rune with no quirk in this world. */
	private static RuneDef unquirked(List<RuneQuirks.Quirk> quirks) {
		for (RuneDef rune : List.of(Runes.FIRE, Runes.FROST, Runes.SHOCK, Runes.HARM)) {
			if (quirks.stream().noneMatch(q -> rune.is(q.rune()))) {
				return rune;
			}
		}
		throw new AssertionError("every candidate rune is quirked");
	}

	/** The nearest spot (by a widening square) that is a ley crossing, or isn't, or null. */
	private static BlockPos find(MinecraftServer server, boolean crossing) {
		long seed = LeyWalker.seed(server.overworld());
		for (int r = 0; r <= SEARCH; r += STEP) {
			for (int x = -r; x <= r; x += STEP) {
				for (int z : new int[]{-r, r}) {
					if (LeyLines.atCrossing(seed, x + 0.5, z + 0.5) == crossing) {
						return new BlockPos(x, 100, z);
					}
				}
			}
			for (int z = -r + STEP; z < r; z += STEP) {
				for (int x : new int[]{-r, r}) {
					if (LeyLines.atCrossing(seed, x + 0.5, z + 0.5) == crossing) {
						return new BlockPos(x, 100, z);
					}
				}
			}
		}
		return null;
	}

	/** Puts the player on a little stone floor at {@code at}, high above anything. */
	private static void stand(ClientGameTestContext context, TestSingleplayerContext world, BlockPos at) {
		world.getServer().runOnServer(server -> player(server).teleportTo(server.overworld(), at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5,
			Set.<Relative>of(), 0.0F, 0.0F, false));
		context.waitTicks(40);
		world.getServer().runCommand("fill " + (at.getX() - 2) + " " + at.getY() + " " + (at.getZ() - 2) + " " + (at.getX() + 2) + " " + at.getY() + " "
			+ (at.getZ() + 2) + " minecraft:stone");
		world.getServer().runOnServer(server -> player(server).teleportTo(server.overworld(), at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5,
			Set.<Relative>of(), 0.0F, 0.0F, false));
		context.waitTicks(5);
	}

	private static void cast(ClientGameTestContext context, TestSingleplayerContext world, Resonance harmony) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.set(player, Spellbooks.get(player).withSpell(0, harmony.runes()).withSelected(0));
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, 500);
			SpellCaster.cast(player, 0);
		});
		context.waitTicks(20);
	}

	private static boolean discovered(TestSingleplayerContext world, Resonance harmony) {
		return world.getServer().computeOnServer(server -> Heart.discovered(player(server), harmony.key()));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
