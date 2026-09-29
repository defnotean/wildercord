package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * In game, what the storm and earth audit changed, cast at still husks on a stone platform high in the air by day:
 * Lightning hits a crowd once each (not once for every neighbour), Ripple heals a quarter of what it took, Thunderclap's
 * crack comes after its flash, Aftershock's second blow lands on the spot (a dodge works, a neighbour is hit), Stalactite
 * can be stepped out of, Plasma ionises so a following Jolt conducts on a dry husk, and Stoneform answers a blow from an
 * attacker but not fire.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordStormEarthTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 200, 40);
	private static final String TAG = "wildercord.stormearth";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			check(failures, "Lightning on a crowd", onServer(world, WildercordStormEarthTest::lightningCrowd));
			clean(context, world);
			check(failures, "Ripple", onServer(world, WildercordStormEarthTest::ripple));
			clean(context, world);
			check(failures, "Plasma ionises", onServer(world, WildercordStormEarthTest::plasma));
			clean(context, world);
			check(failures, "Stoneform", onServer(world, WildercordStormEarthTest::stoneform));
			clean(context, world);
			check(failures, "Thunderclap", thunderclap(context, world));
			clean(context, world);
			check(failures, "Aftershock", aftershock(context, world));
			clean(context, world);
			check(failures, "Stalactite", stalactite(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The storm and earth changes went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ the stage (as the reactions test lays it)

	private static void check(List<String> failures, String what, String failure) {
		if (failure != null) {
			failures.add(what + ": " + failure);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 6) + " " + (x + 12) + " " + (y - 1) + " " + (z + 18) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 6) + " " + (x + 12) + " " + (y + 6) + " " + (z + 18) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CRYSTALS, Mana.MAX_CRYSTALS);
			stand(player);
		});
	}

	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0);
		player.removeAllEffects();
		player.clearFire();
		player.setAttached(WildercordAttachments.GRIMOIRE, List.of());
	}

	private static Mob mob(ServerLevel level, EntityType<? extends Mob> type, double side, double ahead) {
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null && type == EntityTypes.HUSK) {
			health.setBaseValue(200);
		}
		mob.setHealth(mob.getMaxHealth());
		return mob;
	}

	private static Mob husk(ServerLevel level, double side, double ahead) {
		return mob(level, EntityTypes.HUSK, side, ahead);
	}

	private static float lost(Mob mob) {
		return mob.getMaxHealth() - mob.getHealth();
	}

	/** {@code Touch} and then {@code effects}, one hit landing on every one of {@code targets} at once, as a Burst's does. */
	private static void castAt(ServerPlayer player, List<RuneDef> effects, List<? extends Entity> targets) {
		List<RuneDef> runes = new ArrayList<>();
		runes.add(Runes.TOUCH);
		runes.addAll(effects);
		SpellPlan.Group group = SpellCompiler.compile(runes).root().groups.getFirst();
		Entity first = targets.getFirst();
		CastEngine.onHit(new Cast(player), group, new Cast.Hit(List.copyOf(targets), first.position(), new Vec3(0, 0, 1), player.position(), null, null, false), null);
	}

	private static <T> T onServer(TestSingleplayerContext world, Function<MinecraftServer, T> task) {
		return world.getServer().computeOnServer(task::apply);
	}

	private static void clean(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(20);
	}

	// ------------------------------------------------------------------ the checks

	/** Five husks packed together, one Lightning: each takes one strike (about 12), not one for every neighbour. */
	private static String lightningCrowd(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		List<Mob> pack = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			pack.add(husk(level, -2 + i * 0.4, 8));
		}
		castAt(player, List.of(Runes.LIGHTNING), pack);
		for (Mob husk : pack) {
			float loss = lost(husk);
			if (loss < 5 || loss > 13.0F) {
				return "each husk of a packed crowd should take one strike, about 12 (one took " + loss + ")";
			}
		}
		return null;
	}

	/** Ripple heals a quarter of what it took: 12 on a husk (doubled against undead) is 3; 6 on a pig is 1.5. */
	private static String ripple(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob husk = husk(level, 0, 4);
		player.setHealth(10.0F);
		castAt(player, List.of(Runes.RIPPLE), List.of(husk));
		float healed = player.getHealth() - 10.0F;
		if (Math.abs(healed - 3.0F) > 0.15F) {
			return "12 damage to an undead husk should heal a quarter, 3 (healed " + healed + ")";
		}
		Mob pig = mob(level, EntityTypes.PIG, 4, 4);
		player.setHealth(10.0F);
		castAt(player, List.of(Runes.RIPPLE), List.of(pig));
		healed = player.getHealth() - 10.0F;
		if (Math.abs(healed - 1.5F) > 0.15F) {
			return "6 damage to a pig should heal 1.5 (healed " + healed + ")";
		}
		return null;
	}

	/** A Jolt alone on a dry husk conducts nothing; after Plasma in the same spell it does (the husk is ionised). */
	private static String plasma(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob dry = husk(level, -4, 4);
		castAt(player, List.of(Runes.JOLT), List.of(dry));
		if (Heart.discovered(player, Feats.reactionKey("conduct"))) {
			return "a dry husk shouldn't conduct";
		}
		Mob ionised = husk(level, 0, 4);
		castAt(player, List.of(Runes.PLASMA, Runes.JOLT), List.of(ionised));
		if (!Heart.discovered(player, Feats.reactionKey("conduct"))) {
			return "a husk Plasma just ionised should conduct the Jolt after it";
		}
		return null;
	}

	/** Stoneform answers a blow from an attacker with an aftershock, but burning does not. */
	private static String stoneform(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob attacker = husk(level, 0, 1.5);
		Mob bystander = husk(level, 1.5, 0);
		castAt(player, List.of(Runes.STONEFORM), List.of(player));
		player.hurtServer(level, level.damageSources().inFire(), 1.0F);
		if (lost(attacker) > 0 || lost(bystander) > 0) {
			return "fire should not set off the aftershock (the husks lost " + lost(attacker) + " and " + lost(bystander) + ")";
		}
		player.hurtServer(level, level.damageSources().mobAttack(attacker), 3.0F);
		if (lost(bystander) < 2.0F) {
			return "a blow from an attacker should send an aftershock to the husk beside (it lost " + lost(bystander) + ")";
		}
		return null;
	}

	/** Thunderclap: nothing at the flash, the crack a few ticks later. */
	private static String thunderclap(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), 0, 5);
			castAt(player, List.of(Runes.THUNDERCLAP), List.of(husk));
			if (lost(husk) > 0) {
				return -1;
			}
			return husk.getId();
		});
		if (id < 0) {
			return "the crack should come after the flash, not with it";
		}
		context.waitTicks(10);
		return onServer(world, server -> player(server).level().getEntity(id) instanceof Mob husk && lost(husk) >= 4.0F ? null
			: "the crack should have struck 5 by now");
	}

	/** Aftershock: the second blow hits the spot. The husk that stayed is not the one hit twice if it moved; a neighbour that stepped in is. */
	private static String aftershock(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Mob first = husk(level, 0, 5);
			castAt(player, List.of(Runes.AFTERSHOCK), List.of(first));
			// It walks off, and another steps into the spot before the ground is struck again.
			first.snapTo(STAGE.getX() + 0.5 + 7, STAGE.getY(), STAGE.getZ() + 0.5 + 5);
			Mob second = husk(level, 0, 5);
			return new int[] {first.getId(), second.getId()};
		});
		context.waitTicks(16);
		return onServer(world, server -> {
			ServerLevel level = player(server).level();
			Mob first = (Mob) level.getEntity(ids[0]);
			Mob second = (Mob) level.getEntity(ids[1]);
			if (lost(first) > 5.05F) {
				return "the husk that walked off should have taken only the first blow (it lost " + lost(first) + ")";
			}
			if (lost(second) < 3.0F) {
				return "whoever stands on the spot takes the second blow (the husk there lost " + lost(second) + ")";
			}
			return null;
		});
	}

	/** Stalactite falls where it was aimed: stepping out dodges it, staying does not. */
	private static String stalactite(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Mob dodger = husk(level, -3, 6);
			Mob stayer = husk(level, 3, 6);
			castAt(player, List.of(Runes.STALACTITE), List.of(dodger, stayer));
			dodger.snapTo(STAGE.getX() + 0.5 - 3, STAGE.getY(), STAGE.getZ() + 0.5 + 12);
			return new int[] {dodger.getId(), stayer.getId()};
		});
		context.waitTicks(14);
		return onServer(world, server -> {
			ServerLevel level = player(server).level();
			Mob dodger = (Mob) level.getEntity(ids[0]);
			Mob stayer = (Mob) level.getEntity(ids[1]);
			if (lost(dodger) > 0) {
				return "the husk that stepped out should not be hit (it lost " + lost(dodger) + ")";
			}
			if (lost(stayer) < 5.0F) {
				return "the husk that stayed should take the drop (it lost " + lost(stayer) + ")";
			}
			return null;
		});
	}
}
