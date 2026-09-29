package dev.wildercord.gametest;

import dev.wildercord.api.WildercordEvents;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNames;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What a spell costs, and what that buys, in a real world: a secret spell is named, priced and timed
 * as one only once it's been found (the cast that finds it costs what every readout said, the
 * ordinary price), and an Echo or a Pulse after On Hit goes off once, as it was paid for, not once
 * for every creature hit.
 *
 * <p>Runs in the full suite; skipped with {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY}
 * or {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordPricesTest implements FabricClientGameTest {
	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 190, 0);

	/** Every shape landing from {@link #watched}'s spells, counted (their effects about to apply). */
	private static final AtomicInteger HITS = new AtomicInteger();
	private static volatile UUID watched;
	private static boolean listening;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!listening) {
			listening = true;
			WildercordEvents.SPELL_HIT.register((caster, targets, point, effects) -> {
				if (caster.getUUID().equals(watched)) {
					HITS.incrementAndGet();
				}
			});
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			List<String> failures = new ArrayList<>();
			String secret = secrets(world);
			if (secret != null) {
				failures.add("Secret spells: " + secret);
			}
			String echo = repeats(context, world, ids(Runes.BURST, Runes.REVEAL, Runes.ON_HIT, Runes.ECHO), 2, 30);
			if (echo != null) {
				failures.add("An Echo after On Hit: " + echo);
			}
			// The first hit's Pulse: three bursts from you, not three for every creature hit.
			String pulse = repeats(context, world, ids(Runes.BURST, Runes.REVEAL, Runes.ON_HIT, Runes.PULSE, Runes.BURST, Runes.REVEAL), 4, 70);
			if (pulse != null) {
				failures.add("A Pulse after On Hit: " + pulse);
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("Spell prices went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			watched = null;
		}
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<String> ids(RuneDef... runes) {
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	/** A player in survival on a stone platform high up, with an Echo Cord, every rune, empty hands and full mana. */
	private static void ready(ServerPlayer player) {
		ServerLevel level = player.level();
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				level.setBlockAndUpdate(STAGE.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
			}
		}
		player.setGameMode(GameType.SURVIVAL);
		Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
		Spellbook book = Spellbooks.get(player).withStarterGiven();
		for (RuneDef rune : Runes.all()) {
			book = book.learn(rune.id());
		}
		for (int i = 0; i < dev.wildercord.gear.SpellSlots.ALL; i++) {
			book = book.withSpell(i, List.of());
		}
		Spellbooks.set(player, book.withSelected(0));
		player.setAttached(WildercordAttachments.CIRCLES, 0);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		player.teleportTo(level, STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0, 0, false);
		Spellbooks.setMana(player, Mana.max(player));
	}

	/** Casts spell 1 fresh: off cooldown, full mana, no rhythm chain. Returns the mana it took, or -1 if it didn't go off. */
	private static float castFresh(ServerPlayer player) {
		Spellbooks.setReadyAt(player, 0, 0);
		player.removeAttached(WildercordAttachments.RHYTHM);
		Spellbooks.setMana(player, Mana.max(player));
		float before = Spellbooks.mana(player);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? before - Spellbooks.mana(player) : -1;
	}

	// ------------------------------------------------------------------ secret spells

	private static String secrets(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			Secrets.Secret secret = Secrets.GLACIAL_LANCE;
			List<RuneDef> runes = secret.runes();
			player.setAttached(WildercordAttachments.GRIMOIRE, Heart.grimoire(player).stream().filter(k -> !k.equals(secret.key())).toList());
			SpellCaster.edit(player, 0, runes.stream().map(RuneDef::id).toList());
			SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);

			// Not found yet: named, priced and timed as the ordinary spell, and the cast that finds it costs that.
			if (!SpellCaster.nameOf(player, Spellbooks.get(player), 0, runes).equals(SpellNames.auto(runes))) {
				return "its name showed before it was found (" + SpellCaster.nameOf(player, Spellbooks.get(player), 0, runes) + ")";
			}
			int ordinary = Heart.manaCost(player, compiled);
			int shown = Heart.manaCost(player, compiled, Heart.secretCost(player, runes));
			int shownCooldown = Heart.cooldownTicks(player, compiled, Heart.secretCooldown(player, runes));
			if (shown != ordinary || shownCooldown != Heart.cooldownTicks(player, compiled)) {
				return "before it's found, it should show the ordinary price and cooldown (" + shown + " vs " + ordinary + ")";
			}
			long now = player.level().getGameTime();
			float spent = castFresh(player);
			if (Math.round(spent) != shown || Spellbooks.readyAt(player, 0) - now != shownCooldown) {
				return "the cast that finds it should cost what was shown (" + spent + " mana for " + shown + ", "
					+ (Spellbooks.readyAt(player, 0) - now) + " ticks for " + shownCooldown + ")";
			}
			if (!Heart.discovered(player, secret.key())) {
				return "casting it should find it";
			}

			// Found: its own name, its real price and cooldown shown, and charged.
			if (!SpellCaster.nameOf(player, Spellbooks.get(player), 0, runes).equals(secret.name())) {
				return "once found, its name should show";
			}
			int found = Heart.manaCost(player, compiled, Heart.secretCost(player, runes));
			int foundCooldown = Heart.cooldownTicks(player, compiled, Heart.secretCooldown(player, runes));
			if (found != Heart.roundCost(compiled.cost() * secret.power() * dev.wildercord.config.Config.costMultiplier(player)) || found <= ordinary) {
				return "once found, it should show its own price (" + found + ", the ordinary spell's is " + ordinary + ")";
			}
			if (foundCooldown != Math.round(Heart.cooldownTicks(player, compiled) * Secrets.COOLDOWN)) {
				return "once found, it should show its longer cooldown (" + foundCooldown + " ticks)";
			}
			now = player.level().getGameTime();
			spent = castFresh(player);
			if (Math.round(spent) != found || Spellbooks.readyAt(player, 0) - now != foundCooldown) {
				return "once found, a cast should cost what's shown (" + spent + " mana for " + found + ", "
					+ (Spellbooks.readyAt(player, 0) - now) + " ticks for " + foundCooldown + ")";
			}
			return null;
		});
	}

	// ------------------------------------------------------------------ repeats after On Hit

	/**
	 * Casts {@code spell} at three husks standing close around the caster (a Burst hits them and the
	 * caster, so On Hit fires four times) and counts the shapes that land in {@code ticks}.
	 */
	private static String repeats(ClientGameTestContext context, TestSingleplayerContext world, List<String> spell, int expected, int ticks) {
		String setup = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			ServerLevel level = player.level();
			for (int i = 0; i < 3; i++) {
				Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				if (husk == null) {
					return "couldn't make a husk";
				}
				husk.snapTo(player.getX() + (i - 1) * 1.6, player.getY(), player.getZ() + 1.8, 180, 0);
				husk.setNoAi(true);
				husk.addTag("wildercord.prices");
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				husk.addTag("wildercord.rolled");
				level.addFreshEntity(husk);
			}
			SpellCaster.edit(player, 0, spell);
			HITS.set(0);
			watched = player.getUUID();
			return castFresh(player) < 0 ? "it didn't cast" : null;
		});
		if (setup != null) {
			return setup;
		}
		context.waitTicks(ticks);
		watched = null;
		int hits = HITS.get();
		world.getServer().runCommand("kill @e[tag=wildercord.prices]");
		context.waitTicks(5);
		return hits == expected ? null : "it paid for " + expected + " shapes landing, but " + hits + " did";
	}
}
