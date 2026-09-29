package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * In game, the fused effects of {@code cast.FusedStorm}: each one cast for real (threaded into a spell and cast by the
 * player, at husks on a stone platform high in the air) and checked for what its description promises, at the moments
 * it promises it.
 *
 * <p>Husks that only need to be struck have no AI (they stand still); husks that need to be thrown have their AI but
 * no speed, since a mob without AI doesn't move at all, not even when pushed.</p>
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedStormTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		// The black bolt and both tears in the air.
		new FusedSample(Runes.RIFTBOLT, Runes.BEAM, 4, false),
		// Just after the web strikes (it's woven 15 ticks after the marks).
		new FusedSample(Runes.STORMWEAVE, Runes.BEAM, 17, true),
		// The clock in the sky at its second hour, golden lightning falling.
		new FusedSample(Runes.STORMCLOCK, Runes.BEAM, 41, true),
		// The crimson bolt into the heart.
		new FusedSample(Runes.HEARTSTOPPER, Runes.BEAM, 3, false),
		// The cloud's first strike, by day so the cloud shows against the sky.
		new FusedSample(Runes.THUNDERHEAD, Runes.BEAM, 21, false),
		// The downburst coming down out of the sky.
		new FusedSample(Runes.DOWNDRAFT, Runes.BEAM, 4, false),
		// The column of wind at its height.
		new FusedSample(Runes.UPDRAFT, Runes.BEAM, 6, false),
		// The glyph turning on the ground.
		new FusedSample(Runes.SKYGLYPH, Runes.BEAM, 11, true),
		// The gust, and the stopped clock where the husk stood.
		new FusedSample(Runes.RECOIL, Runes.BEAM, 3, false));

	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.fused_storm";

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
			run(failures, "Riftbolt", () -> riftbolt(context, world));
			run(failures, "Riftbolt by a wall", () -> riftboltWall(context, world));
			run(failures, "Riftbolt on a boss", () -> riftboltBoss(context, world));
			run(failures, "Stormweave", () -> stormweave(context, world));
			run(failures, "Stormclock", () -> stormclock(context, world));
			run(failures, "Heartstopper", () -> heartstopper(context, world));
			run(failures, "Thunderhead", () -> thunderhead(context, world));
			run(failures, "Downdraft", () -> downdraft(context, world));
			run(failures, "Updraft", () -> updraft(context, world));
			run(failures, "Recoil", () -> recoil(context, world));
			run(failures, "A crowd of ten", () -> crowd(context, world));
			run(failures, "Tempest on a bunch", () -> tempestBunch(context, world));
			// Last: a glyph stays on the platform for 10 seconds.
			run(failures, "Skyglyph (enemy)", () -> skyglyphEnemy(context, world));
			run(failures, "Skyglyph (ally)", () -> skyglyphAlly(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The storm and wind fused runes went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private interface Check {
		String run();
	}

	private static void run(List<String> failures, String name, Check check) {
		String failure = check.run();
		if (failure != null) {
			failures.add(name + ": " + failure);
		}
	}

	// ------------------------------------------------------------------ the runes

	/** A husk 5 blocks ahead is struck (6), left in Darkness, and torn through a rift 2 to 5 blocks on, onto the floor. */
	private static String riftbolt(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = spawn(world, EntityTypes.HUSK, 0, 5, 0, false);
		String cast = cast(context, world, Runes.BEAM, Runes.RIFTBOLT);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(5);
		return done(context, world, world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			double moved = flat(h.position(), at(0, 5));
			if (moved < 1.5 || moved > 5.6) {
				return "the rift should carry the husk 2 to 5 blocks (carried " + f(moved) + ")";
			}
			if (Math.abs(h.getY() - STAGE.getY()) > 0.05) {
				return "the husk should come out standing on the floor (feet at " + f(h.getY()) + ")";
			}
			if (!took(h, 6)) {
				return "the bolt should deal 6 (took " + f(taken(h)) + ")";
			}
			return h.hasEffect(MobEffects.DARKNESS) ? null : "the husk should be left in Darkness";
		}));
	}

	/** With a wall just behind the husk, the rift never puts it in or past the wall: it lands in front of it, or stays. */
	private static String riftboltWall(ClientGameTestContext context, TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 6) + " " + y + " " + (z + 7) + " " + (x + 6) + " " + (y + 4) + " " + (z + 7) + " minecraft:stone");
		int husk = spawn(world, EntityTypes.HUSK, 0, 5, 0, false);
		String cast = cast(context, world, Runes.BEAM, Runes.RIFTBOLT);
		context.waitTicks(5);
		String result = cast != null ? cast : world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			if (!h.level().noCollision(h, h.getBoundingBox())) {
				return "the rift put the husk inside a block (at " + h.position() + ")";
			}
			if (h.getZ() > z + 7) {
				return "the rift carried the husk through the wall (at " + h.position() + ")";
			}
			return took(h, 6) ? null : "the bolt should still deal 6 (took " + f(taken(h)) + ")";
		});
		world.getServer().runCommand("fill " + (x - 6) + " " + y + " " + (z + 7) + " " + (x + 6) + " " + (y + 4) + " " + (z + 7) + " minecraft:air");
		return done(context, world, result);
	}

	/** A boss is struck but never torn anywhere. */
	private static String riftboltBoss(ClientGameTestContext context, TestSingleplayerContext world) {
		int warden = spawn(world, EntityTypes.WARDEN, 0, 5, 0, false);
		String cast = cast(context, world, Runes.BEAM, Runes.RIFTBOLT);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(5);
		return done(context, world, world.getServer().computeOnServer(server -> {
			Mob w = mob(server, warden);
			if (w == null) {
				return "the warden is gone";
			}
			if (flat(w.position(), at(0, 5)) > 0.01) {
				return "a boss mustn't be torn through a rift (moved " + f(flat(w.position(), at(0, 5))) + ")";
			}
			return taken(w) > 0 ? null : "the bolt should still strike a boss";
		}));
	}

	/**
	 * The struck husk and the three nearest it are marked; nothing lands at once; a moment later lightning weaves between
	 * all four: 3 each and 1 more for every other one caught (6). A fifth husk further off isn't caught, nor one outside
	 * the 6 blocks.
	 */
	private static String stormweave(ClientGameTestContext context, TestSingleplayerContext world) {
		int struck = spawn(world, EntityTypes.HUSK, 0, 5, 0, false);
		int[] woven = {
			spawn(world, EntityTypes.HUSK, -1.5, 6.2, 0, false),
			spawn(world, EntityTypes.HUSK, 2.5, 6, 0, false),
			spawn(world, EntityTypes.HUSK, 0, 7.8, 0, false)};
		int fifth = spawn(world, EntityTypes.HUSK, -3.2, 8.6, 0, false);
		int far = spawn(world, EntityTypes.HUSK, 8.5, 5, 0, false);
		String cast = cast(context, world, Runes.BEAM, Runes.STORMWEAVE);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(5);
		String early = world.getServer().computeOnServer(server -> {
			Mob h = mob(server, struck);
			return h != null && taken(h) > 0 ? "the lightning should wait a moment after the marks (the husk already took " + f(taken(h)) + ")" : null;
		});
		if (early != null) {
			return done(context, world, early);
		}
		context.waitTicks(20);
		return done(context, world, world.getServer().computeOnServer(server -> {
			List<Integer> web = new ArrayList<>(List.of(struck));
			for (int id : woven) {
				web.add(id);
			}
			for (int id : web) {
				Mob h = mob(server, id);
				if (h == null || !took(h, 6)) {
					return "each of the four caught should take 6 (one took " + (h == null ? "?" : f(taken(h))) + ")";
				}
			}
			Mob fifthHusk = mob(server, fifth);
			if (fifthHusk == null || taken(fifthHusk) > 0) {
				return "only four may be caught: the fifth, furthest off, should be spared";
			}
			Mob farHusk = mob(server, far);
			return farHusk != null && taken(farHusk) == 0 ? null : "a husk 8 blocks off shouldn't be caught";
		}));
	}

	/**
	 * 4 on the struck husk at once; then the same spot is struck at 2 and 4 seconds for 3 each, which also catches a
	 * husk a block away, but never one 3 blocks away.
	 */
	private static String stormclock(ClientGameTestContext context, TestSingleplayerContext world) {
		int struck = spawn(world, EntityTypes.HUSK, 0, 5, 0, false);
		int near = spawn(world, EntityTypes.HUSK, 1.0, 5, 0, false);
		int away = spawn(world, EntityTypes.HUSK, -3.0, 5, 0, false);
		String cast = cast(context, world, Runes.BEAM, Runes.STORMCLOCK);
		if (cast != null) {
			return done(context, world, cast);
		}
		int[][] expected = {{5, 4, 0}, {45, 7, 3}, {88, 10, 6}};
		int waited = 0;
		for (int[] moment : expected) {
			context.waitTicks(moment[0] - waited);
			waited = moment[0];
			String failure = world.getServer().computeOnServer(server -> {
				Mob s = mob(server, struck);
				Mob n = mob(server, near);
				Mob a = mob(server, away);
				if (s == null || n == null || a == null) {
					return "a husk is gone";
				}
				if (!took(s, moment[1])) {
					return "after " + moment[0] + " ticks the struck husk should have taken " + moment[1] + " (took " + f(taken(s)) + ")";
				}
				if (!took(n, moment[2])) {
					return "after " + moment[0] + " ticks the husk a block away should have taken " + moment[2] + " (took " + f(taken(n)) + ")";
				}
				return taken(a) == 0 ? null : "a husk 3 blocks away should never be struck (took " + f(taken(a)) + ")";
			});
			if (failure != null) {
				return done(context, world, failure);
			}
		}
		return done(context, world, null);
	}

	/** 5 at once; then every 2 seconds the husk is stunned for half a second (held: Slowness VII), and free in between. */
	private static String heartstopper(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = spawn(world, EntityTypes.HUSK, 0, 5, 0, false);
		String cast = cast(context, world, Runes.BEAM, Runes.HEARTSTOPPER);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(3);
		String hit = world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			if (!took(h, 5)) {
				return "the shock should deal 5 (took " + f(taken(h)) + ")";
			}
			return stunned(h) ? "the heart shouldn't skip at once" : null;
		});
		if (hit != null) {
			return done(context, world, hit);
		}
		int[][] moments = {{44, 1}, {58, 0}, {84, 1}};
		int waited = 3;
		for (int[] moment : moments) {
			context.waitTicks(moment[0] - waited);
			waited = moment[0];
			String failure = world.getServer().computeOnServer(server -> {
				Mob h = mob(server, husk);
				if (h == null) {
					return "the husk is gone";
				}
				boolean want = moment[1] == 1;
				return stunned(h) == want ? null
					: "after " + moment[0] + " ticks the husk should " + (want ? "be stunned (a skipped beat)" : "be free again") + " (effects " + h.getActiveEffectsMap().keySet() + ")";
			});
			if (failure != null) {
				return done(context, world, failure);
			}
		}
		return done(context, world, null);
	}

	/** A cloud gathers over the husk (nothing in the first half second), then strikes it once a second for 4 seconds: 3 each. A husk 8 blocks away is never struck. */
	private static String thunderhead(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = spawn(world, EntityTypes.HUSK, 0, 5, 0, false);
		int far = spawn(world, EntityTypes.HUSK, 0, 13, 0, false);
		String cast = cast(context, world, Runes.BEAM, Runes.THUNDERHEAD);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(10);
		String gathering = world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			return h != null && taken(h) > 0 ? "the cloud should take a second to gather (took " + f(taken(h)) + " already)" : null;
		});
		if (gathering != null) {
			return done(context, world, gathering);
		}
		context.waitTicks(85);
		return done(context, world, world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			Mob f = mob(server, far);
			if (h == null || f == null) {
				return "a husk is gone";
			}
			if (!took(h, 12)) {
				return "four strikes of 3 should have landed on the husk (took " + f(taken(h)) + ")";
			}
			return taken(f) == 0 ? null : "a husk 8 blocks from the cloud shouldn't be struck";
		}));
	}

	/**
	 * Cast on yourself: a husk hanging 3 blocks up, 3 blocks away, is slammed onto the floor for 4 + 3; a husk on the
	 * ground next to it isn't touched (only the airborne are).
	 */
	private static String downdraft(ClientGameTestContext context, TestSingleplayerContext world) {
		int flying = spawn(world, EntityTypes.HUSK, 0, 3, 3, false);
		int grounded = spawn(world, EntityTypes.HUSK, 2.5, 3, 0, false);
		String cast = cast(context, world, Runes.SELF, Runes.DOWNDRAFT);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(5);
		return done(context, world, world.getServer().computeOnServer(server -> {
			Mob a = mob(server, flying);
			Mob g = mob(server, grounded);
			if (a == null || g == null) {
				return "a husk is gone";
			}
			if (Math.abs(a.getY() - STAGE.getY()) > 0.05 || flat(a.position(), at(0, 3)) > 0.05) {
				return "the airborne husk should be slammed straight down onto the floor (it's at " + a.position() + ")";
			}
			if (!took(a, 7)) {
				return "a 3-block slam should deal 4 + 3 (took " + f(taken(a)) + ")";
			}
			return taken(g) == 0 ? null : "a husk standing on the ground isn't airborne and shouldn't be touched";
		}));
	}

	/**
	 * A husk is thrown high, then smashed straight back down onto the floor for 4 (and no fall damage on top); the
	 * caster's own wolf, standing in the column too, is left alone.
	 */
	private static String updraft(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = spawn(world, EntityTypes.HUSK, 0, 5, 0, true);
		int wolf = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Wolf w = EntityTypes.WOLF.create(player.level(), EntitySpawnReason.COMMAND);
			if (w == null) {
				return -1;
			}
			w.snapTo(STAGE.getX() + 1.9, STAGE.getY(), STAGE.getZ() + 5.5, 180, 0);
			w.tame(player);
			w.setNoAi(true);
			w.addTag(TAG);
			// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
			w.addTag("wildercord.rolled");
			player.level().addFreshEntity(w);
			return w.getId();
		});
		context.waitTicks(3);
		String cast = cast(context, world, Runes.BEAM, Runes.UPDRAFT);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(6);
		String up = world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			return h.getY() > STAGE.getY() + 1.5 ? null : "the husk should be thrown high (it's " + f(h.getY() - STAGE.getY()) + " up)";
		});
		if (up != null) {
			return done(context, world, up);
		}
		context.waitTicks(30);
		return done(context, world, world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			if (h.getY() > STAGE.getY() + 0.05) {
				return "the husk should be smashed back down onto the floor (it's " + f(h.getY() - STAGE.getY()) + " up)";
			}
			if (!took(h, 4) || taken(h) > 5.2) {
				return "the smash should deal 4, and the fall nothing more (took " + f(taken(h)) + ")";
			}
			Mob w = mob(server, wolf);
			return w == null || taken(w) == 0 ? null : "an ally in the column shouldn't be smashed";
		}));
	}

	/** A husk is hurled about 5 blocks back; 2 seconds later it's snapped back to where it stood, for 3. */
	private static String recoil(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = spawn(world, EntityTypes.HUSK, 0, 5, 0, true);
		String cast = cast(context, world, Runes.BEAM, Runes.RECOIL);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(25);
		String thrown = world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			double d = flat(h.position(), at(0, 5));
			if (d < 3.0 || d > 7.0) {
				return "the husk should be hurled about 5 blocks back (went " + f(d) + ")";
			}
			return taken(h) == 0 ? null : "the throw itself shouldn't hurt (took " + f(taken(h)) + ")";
		});
		if (thrown != null) {
			return done(context, world, thrown);
		}
		context.waitTicks(25);
		return done(context, world, world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			double d = flat(h.position(), at(0, 5));
			if (d > 0.6) {
				return "the husk should be snapped back to where it stood (it's " + f(d) + " away)";
			}
			return took(h, 3) ? null : "the snap should deal 3 (took " + f(taken(h)) + ")";
		}));
	}

	/**
	 * A crowd of ten, more than the eight that get a lasting or moving part of their own: Riftbolt's bolt (6),
	 * Heartstopper's shock (5) and Stormclock's first strike (4) each land on every husk, not just the first eight.
	 */
	private static String crowd(ClientGameTestContext context, TestSingleplayerContext world) {
		List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ids.add(spawn(world, EntityTypes.HUSK, -9 + 2 * i, 6, 0, false));
		}
		context.waitTicks(3);
		RuneDef[] runes = {Runes.RIFTBOLT, Runes.HEARTSTOPPER, Runes.STORMCLOCK};
		double[] damage = {6, 5, 4};
		for (int r = 0; r < runes.length; r++) {
			RuneDef rune = runes[r];
			double amount = damage[r];
			String found = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				List<Entity> crowd = new ArrayList<>();
				for (int id : ids) {
					Mob h = mob(server, id);
					if (h == null) {
						return "a husk of the crowd is gone";
					}
					h.setHealth(h.getMaxHealth());
					crowd.add(h);
				}
				SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.BURST, rune)).root().groups.getFirst().effects.getFirst();
				Effects.apply(new Cast(player), node, new Cast.Hit(crowd, at(0, 6), new Vec3(0, 0, 1), player.position(), null, null, false));
				for (int i = 0; i < crowd.size(); i++) {
					LivingEntity h = (LivingEntity) crowd.get(i);
					if (!took(h, amount)) {
						return rune.name() + " should strike husk " + (i + 1) + " of ten for " + f(amount) + " (took " + f(taken(h)) + ")";
					}
				}
				return null;
			});
			if (found != null) {
				return done(context, world, found);
			}
		}
		// Stormclock's later strikes (2 and 4 seconds on) mustn't land on the next check's husk.
		context.waitTicks(90);
		return done(context, world, null);
	}

	/**
	 * Tempest on three husks standing together: a strike lands on each, but where they overlap each husk takes one
	 * (8), not one for every strike round it.
	 */
	private static String tempestBunch(ClientGameTestContext context, TestSingleplayerContext world) {
		List<Integer> ids = List.of(spawn(world, EntityTypes.HUSK, 0, 6, 0, false), spawn(world, EntityTypes.HUSK, 0.8, 6, 0, false),
			spawn(world, EntityTypes.HUSK, -0.8, 6, 0, false));
		context.waitTicks(3);
		String found = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			List<Entity> bunch = new ArrayList<>();
			for (int id : ids) {
				Mob h = mob(server, id);
				if (h == null) {
					return "a husk of the bunch is gone";
				}
				bunch.add(h);
			}
			SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.BURST, Runes.TEMPEST)).root().groups.getFirst().effects.getFirst();
			Effects.apply(new Cast(player), node, new Cast.Hit(bunch, at(0, 6), new Vec3(0, 0, 1), player.position(), null, null, false));
			for (int i = 0; i < bunch.size(); i++) {
				LivingEntity h = (LivingEntity) bunch.get(i);
				if (!h.isAlive() || !took(h, 8)) {
					return "husk " + (i + 1) + " of three should take one strike (8), not one for each strike round it (took " + f(taken(h)) + ")";
				}
			}
			return null;
		});
		return done(context, world, found);
	}

	/** Written under a husk (an enemy), the glyph throws it back about 4 blocks, and doesn't hurt it. */
	private static String skyglyphEnemy(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = spawn(world, EntityTypes.HUSK, 0, 5, 0, true);
		String cast = cast(context, world, Runes.BEAM, Runes.SKYGLYPH);
		if (cast != null) {
			return done(context, world, cast);
		}
		context.waitTicks(30);
		return done(context, world, world.getServer().computeOnServer(server -> {
			Mob h = mob(server, husk);
			if (h == null) {
				return "the husk is gone";
			}
			double d = flat(h.position(), at(0, 5));
			if (d < 2.5 || d > 6.5) {
				return "an enemy on the glyph should be thrown back about 4 blocks (went " + f(d) + ")";
			}
			return taken(h) == 0 ? null : "the glyph shouldn't hurt (took " + f(taken(h)) + ")";
		}));
	}

	/**
	 * Written at your own feet (you're your own ally), the glyph launches you high, and you land without a fall. Cast in a
	 * corner of the platform, facing out of it, clear of the glyph the last test left.
	 */
	private static String skyglyphAlly(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = cast(context, world, Runes.SELF, Runes.SKYGLYPH, -10, -8, 180.0F);
		if (cast != null) {
			return done(context, world, cast);
		}
		double[] highest = {0};
		for (int i = 0; i < 12; i++) {
			context.waitTicks(1);
			highest[0] = Math.max(highest[0], world.getServer().computeOnServer(server -> player(server).getY() - STAGE.getY()));
		}
		if (highest[0] < 2.0) {
			return done(context, world, "the glyph should launch its caster high (rose " + f(highest[0]) + ")");
		}
		context.waitTicks(70);
		return done(context, world, world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (!player.onGround()) {
				return "the caster should have landed by now (" + f(player.getY() - STAGE.getY()) + " up)";
			}
			return player.getHealth() >= player.getMaxHealth() ? null : "a glyph's launch shouldn't cost its caster a fall (health " + f(player.getHealth()) + ")";
		}));
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static Vec3 at(double dx, double dz) {
		return new Vec3(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz);
	}

	/** A 41x41 stone floor with open air above it, an Echo Cord, every rune known, and plenty of mana. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 20) + " " + (y - 1) + " " + (z - 20) + " " + (x + 20) + " " + (y - 1) + " " + (z + 20) + " minecraft:stone");
		// Tall enough for an updraft's throw and a glyph's launch.
		world.getServer().runCommand("fill " + (x - 20) + " " + y + " " + (z - 20) + " " + (x + 20) + " " + (y + 14) + " " + (z + 20) + " minecraft:air");
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

	/** The caster at the middle of the platform, facing south (+Z) and a little down, toward the husks. */
	private static void stand(ServerPlayer player) {
		stand(player, 0, 0, 0.0F);
	}

	private static void stand(ServerPlayer player, double dx, double dz, float yaw) {
		Vec3 spot = at(dx, dz);
		player.teleportTo(player.level(), spot.x, spot.y, spot.z, Set.<Relative>of(), yaw, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
	}

	/**
	 * A creature {@code dx}, {@code dz} from the middle of the platform (and {@code up} blocks over it), facing the
	 * caster. Without AI it stands (or hangs) perfectly still; with it, it has no speed, so it only moves when thrown.
	 */
	private static int spawn(TestSingleplayerContext world, EntityType<? extends Mob> type, double dx, double dz, double up, boolean ai) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			Mob mob = type.create(level, EntitySpawnReason.COMMAND);
			if (mob == null) {
				return -1;
			}
			Vec3 spot = at(dx, dz).add(0, up, 0);
			mob.snapTo(spot.x, spot.y, spot.z, 180, 0);
			if (ai) {
				mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			} else {
				mob.setNoAi(true);
			}
			mob.setPersistenceRequired();
			mob.addTag(TAG);
			// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
			mob.addTag("wildercord.rolled");
			level.addFreshEntity(mob);
			return mob.getId();
		});
		return id;
	}

	/** Threads {@code shape} and {@code effect} into spell 1 and casts it, from the middle of the platform. Returns what went wrong, or null. */
	private static String cast(ClientGameTestContext context, TestSingleplayerContext world, RuneDef shape, RuneDef effect) {
		return cast(context, world, shape, effect, 0, 0, 0.0F);
	}

	/** The same, cast standing {@code dx}, {@code dz} from the middle of the platform and facing {@code yaw}. */
	private static String cast(ClientGameTestContext context, TestSingleplayerContext world, RuneDef shape, RuneDef effect, double dx, double dz,
			float yaw) {
		world.getServer().runOnServer(server -> stand(player(server), dx, dz, yaw));
		// The teleport settles, and whatever was just spawned finds its feet.
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			List<String> ids = List.of(shape.id(), effect.id());
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids);
			if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
				return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
			}
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, Mana.max(player));
			player.removeAttached(WildercordAttachments.RHYTHM);
			long now = player.level().getGameTime();
			SpellCaster.cast(player, 0);
			return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
		});
	}

	/** Clears the platform of this test's creatures and passes {@code result} on. */
	private static String done(ClientGameTestContext context, TestSingleplayerContext world, String result) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(20);
		return result;
	}

	private static Mob mob(MinecraftServer server, int id) {
		return id >= 0 && player(server).level().getEntity(id) instanceof Mob mob && mob.isAlive() ? mob : null;
	}

	private static double taken(LivingEntity e) {
		return e.getMaxHealth() - e.getHealth();
	}

	/**
	 * Whether {@code e} has taken about {@code amount}: a husk's armour can take a little off, and the caster's
	 * elemental leaning can add a tenth.
	 */
	private static boolean took(LivingEntity e, double amount) {
		double t = taken(e);
		return amount == 0 ? t == 0 : t >= amount * 0.9 - 0.05 && t <= amount * 1.2 + 0.05;
	}

	/** Held by a skipped heartbeat (Spirits.hold: Slowness VII). */
	private static boolean stunned(LivingEntity e) {
		MobEffectInstance slow = e.getEffect(MobEffects.SLOWNESS);
		return slow != null && slow.getAmplifier() >= 6;
	}

	private static double flat(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static String f(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
