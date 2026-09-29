package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.WildSurge;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Parry;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WildMagic;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parrying and wild magic, checked on a small stone floor:
 * <ul>
 *   <li>a husk fires a Harm bolt at the player, who raises a Shield just before it lands: the bolt
 *       turns round and hits the husk, the player takes nothing, and the Parry feat is written;</li>
 *   <li>the same bolt at a Shield raised a second earlier isn't turned (the husk is unhurt);</li>
 *   <li>a husk's Beam (a spell that doesn't fly) parried at the last moment is negated and answered
 *       with a counter-burst that hurts the husk;</li>
 *   <li>an overcast forced into every wild magic outcome in turn goes off without error, never kills
 *       the caster, and the outcomes that leave a mark leave the right one;</li>
 *   <li>Borrowed Time cast again adds to the debt still owed, without borrowing back what was paid.</li>
 * </ul>
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY} and {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordParryTest implements FabricClientGameTest {
	private static final String TAG = "wildercord.parry_test";
	/** Where the player stands (set once the floor is laid). */
	private static Vec3 spot;

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
			world.getServer().runOnServer(server -> server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true));
			setup(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			run(failures, "parrying a bolt", () -> parryBolt(context, world));
			run(failures, "a Shield raised too early", () -> earlyShield(context, world));
			run(failures, "parrying a beam", () -> parryBeam(context, world));
			run(failures, "wild magic", () -> wildMagic(context, world));
			// Last: the debt it leaves is still being paid when the world closes.
			run(failures, "Borrowed Time", () -> borrowedTime(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Parrying or wild magic went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void run(List<String> failures, String what, Runnable test) {
		try {
			test.run();
		} catch (AssertionError e) {
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
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	/** A stone floor 25 blocks across with open air above; the player in survival with an Echo Cord and every rune. */
	private static void setup(TestSingleplayerContext world) {
		int[] at = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return new int[] {player.getBlockX(), player.getBlockY(), player.getBlockZ()};
		});
		int x = at[0];
		int y = at[1];
		int z = at[2];
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 12) + " " + (x + 12) + " " + (y + 8) + " " + (z + 14) + " air");
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 12) + " " + (x + 12) + " " + (y - 1) + " " + (z + 14) + " stone");
		spot = new Vec3(x + 0.5, y, z + 0.5);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			for (int i = 0; i < dev.wildercord.spell.Passives.MAX; i++) {
				book = book.withPassive(i, List.of());
			}
			book = book.withSpell(0, ids(Runes.SELF, Runes.SHIELD)).withSpell(1, ids(Runes.BOLT, Runes.HARM)).withSelected(0);
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CIRCLES, 3);
			player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
			stand(player);
		});
	}

	/** The player back on the spot, looking down the floor (+z), healthy, with full mana and nothing lingering. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), spot.x, spot.y, spot.z, Set.<Relative>of(), 0, 0, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.removeAttached(WildercordAttachments.SPELL_SHIELD);
		Spellbooks.setMana(player, Mana.max(player));
		for (int i = 0; i < 4; i++) {
			Spellbooks.setReadyAt(player, i, 0);
		}
	}

	/** A still husk {@code distance} blocks down the floor, facing the player (marked so it never rolls Runebound). */
	private static Mob husk(ServerLevel level, double distance, double side) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.snapTo(spot.x + side, spot.y, spot.z + distance, 180, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		husk.lookAt(EntityAnchorArgument.Anchor.EYES, spot.add(0, 1.2, 0));
		return husk;
	}

	private static Mob testHusk(MinecraftServer server) {
		List<Mob> found = player(server).level().getEntitiesOfClass(Mob.class, player(server).getBoundingBox().inflate(32),
			e -> e.entityTags().contains(TAG) && e.isAlive());
		return found.isEmpty() ? null : found.getFirst();
	}

	private static void clearHusks(TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
	}

	/** The husk's Harm bolt, fired from just in front of it straight at the player's chest. */
	private static void fireBolt(ServerPlayer player, Mob husk) {
		SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
		Vec3 chest = player.getBoundingBox().getCenter();
		Vec3 from = husk.getEyePosition().add(chest.subtract(husk.getEyePosition()).normalize().scale(0.8));
		RuneBolt.launch(new Cast(husk), group, null, from, chest.subtract(from), false);
	}

	// ------------------------------------------------------------------ parrying

	private static void parryBolt(ClientGameTestContext context, TestSingleplayerContext world) {
		clearHusks(world);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			// Six blocks off, a 1.6-a-tick bolt reaches the Shield's front circle in about three ticks: inside the window.
			Mob husk = husk(player.level(), 6, 0);
			fireBolt(player, husk);
			SpellCaster.cast(player, 0);
			check(player.hasAttached(WildercordAttachments.SPELL_SHIELD), "casting Self Shield should raise a Shield");
		});
		int turned = world.getServer().waitFor(server -> {
			Mob husk = testHusk(server);
			return husk != null && husk.getHealth() < husk.getMaxHealth();
		}, 60);
		context.waitTicks(2);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			check(player.getHealth() >= player.getMaxHealth(), "a parried bolt shouldn't hurt the one who parried it (health " + player.getHealth() + ")");
			check(!player.hasAttached(WildercordAttachments.SPELL_SHIELD), "a parry should spend the Shield");
			check(Heart.discovered(player, "feat:" + Feats.PARRY), "a parry is a feat");
		});
		check(turned >= 0, "the parried bolt should fly back and hit the husk that cast it");
		context.waitTicks(10);
		clearHusks(world);
	}

	private static void earlyShield(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			SpellCaster.cast(player, 0);
			husk(player.level(), 6, 0);
		});
		// Well past the window.
		context.waitTicks(Parry.WINDOW + 20);
		world.getServer().runOnServer(server -> fireBolt(player(server), testHusk(server)));
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Mob husk = testHusk(server);
			check(husk != null && husk.getHealth() >= husk.getMaxHealth(), "a bolt meeting a Shield raised long before shouldn't be turned back");
		});
		clearHusks(world);
	}

	private static void parryBeam(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			Mob husk = husk(player.level(), 7, 0);
			SpellCaster.cast(player, 0);
			// A beam lands at once: the Shield went up this very tick, so it parries, and answers with a counter-burst.
			CastEngine.cast(new Cast(husk), SpellCompiler.compile(List.of(Runes.BEAM, Runes.HARM)).root());
		});
		context.waitTicks(5);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = testHusk(server);
			check(player.getHealth() >= player.getMaxHealth(), "a parried beam shouldn't hurt (health " + player.getHealth() + ")");
			check(husk != null && husk.getHealth() < husk.getMaxHealth(), "a parried beam should be answered with a counter-burst at its caster");
			check(!player.hasAttached(WildercordAttachments.SPELL_SHIELD), "a parry should spend the Shield");
		});
		context.waitTicks(5);
		clearHusks(world);
	}

	// ------------------------------------------------------------------ wild magic

	private static void wildMagic(ClientGameTestContext context, TestSingleplayerContext world) {
		for (WildMagic.Surge surge : WildMagic.Surge.values()) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				stand(player);
				player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
				// Something to aim at, a bystander to stray onto, and someone to heal.
				husk(player.level(), 8, 0);
				Mob near = husk(player.level(), 3, 2.5);
				near.setHealth(10);
				Spellbooks.setMana(player, 1);
				WildSurge.force(player, surge);
				// The first press only asks; the second overcasts, and the forced surge goes off.
				SpellCaster.cast(player, 1);
				check(Heart.cracked(player) == 0, surge.id + ": the first press only asks");
				SpellCaster.cast(player, 1);
				check(Heart.cracked(player) == 1, surge.id + ": the second press should overcast");
				check(player.isAlive() && player.getHealth() >= 1, surge.id + ": wild magic must never kill its caster");
				switch (surge) {
					case BACKFIRE -> check(player.getHealth() < player.getMaxHealth(), "a backfire should hurt a little");
					case HEAL_ALL -> check(near.getHealth() > 10, "Heal All should heal the husk nearby");
					case LEVITATE -> check(player.hasEffect(MobEffects.LEVITATION) && player.hasEffect(MobEffects.SLOW_FALLING),
						"gravity flipping should float you, and let you down gently");
					case SLOW_TIME -> check(player.hasEffect(MobEffects.SLOWNESS) && near.hasEffect(MobEffects.SLOWNESS), "time slowing should slow everyone near");
					case WARD -> check(player.hasAttached(WildercordAttachments.SPELL_SHIELD), "the Ward surge should put a Shield on you");
					case BLINK -> check(player.position().distanceTo(spot) > 2.0, "a Blink surge should throw you a few blocks (moved "
						+ String.format(java.util.Locale.ROOT, "%.1f", player.position().distanceTo(spot)) + ")");
					case FREE_RECAST -> {
						long now = player.level().getGameTime();
						check(WildSurge.freeRecast(player, now), "a Free Recast should be waiting");
						// No mana and the spell still cooling: only a free recast goes off now (and is spent doing so).
						Spellbooks.setMana(player, 0);
						check(Spellbooks.readyAt(player, 1) > now, "the overcast spell should be cooling");
						SpellCaster.cast(player, 1);
						check(!WildSurge.freeRecast(player, now), "the free recast should cast despite the cooldown, and be spent");
						check(Heart.cracked(player) == 1, "the free recast shouldn't need another overcast");
					}
					default -> {
					}
				}
				check(Heart.discovered(player, "feat:" + Feats.WILD_SURGE), "the first wild surge is a feat");
			});
			// Long enough for Twice's second cast, the butterflies and the wisps to play out.
			context.waitTicks(24);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				check(player.isAlive(), surge.id + ": the caster should survive what follows too");
				WildSurge.force(player, null);
			});
			clearHusks(world);
			context.waitTicks(2);
		}
	}

	// ------------------------------------------------------------------ Borrowed Time

	/** Borrowing again adds to what's still owed: a payment is never borrowed back, and a recast never wipes the debt. */
	private static void borrowedTime(ClientGameTestContext context, TestSingleplayerContext world) {
		SpellPlan.Segment borrow = SpellCompiler.compile(List.of(Runes.SELF, Runes.BORROWED_TIME)).root();
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.setAttached(WildercordAttachments.INNATE, Runes.BORROWED_TIME.id());
			dev.wildercord.cast.Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().magic(), 10);
			CastEngine.cast(player, borrow);
			check(dev.wildercord.cast.Innates.owed(player) > 9, "Borrowed Time should heal the 10 just taken and owe it (owes " + dev.wildercord.cast.Innates.owed(player) + ")");
		});
		// Long enough for a payment or two.
		context.waitTicks(45);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			float before = dev.wildercord.cast.Innates.owed(player);
			check(before > 0 && before < 10, "the debt should be paid a little each second (owes " + before + ")");
			dev.wildercord.cast.Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().magic(), 4);
			CastEngine.cast(player, borrow);
			float after = dev.wildercord.cast.Innates.owed(player);
			check(after > before + 3.5 && after < before + 4.5, "borrowing again should add the 4 just taken to what's owed, and never the payments ("
				+ before + " owed, then " + after + ")");
			player.setAttached(WildercordAttachments.INNATE, "");
		});
	}
}
