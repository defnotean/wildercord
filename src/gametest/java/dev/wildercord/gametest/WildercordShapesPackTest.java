package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The shapes pack in a real game: sixteen of its field and kin shapes cast on a thick stone platform high
 * in the air, each with a real effect, and a check that the blocks and creatures it was meant to strike
 * were struck and the ones beside them were not. A shaft and a stairwell dug, a seam, a pit and a furrow
 * broken, only the ores near the aim mined, five lights set out in a square; animals, pets, the young,
 * fliers and what hunts you picked out of a crowd; a heal that reaches an ally and never a foe.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordShapesPackTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.shapes_pack";

	private interface Check {
		String run(ClientGameTestContext context, TestSingleplayerContext world);
	}

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
			List<Object[]> checks = List.of(
				new Object[] {"Shaft", (Check) WildercordShapesPackTest::shaft},
				new Object[] {"Stairwell", (Check) WildercordShapesPackTest::stairwell},
				new Object[] {"Seam", (Check) WildercordShapesPackTest::seam},
				new Object[] {"Pit", (Check) WildercordShapesPackTest::pit},
				new Object[] {"Furrow", (Check) WildercordShapesPackTest::furrow},
				new Object[] {"Lodeseek", (Check) WildercordShapesPackTest::lodeseek},
				new Object[] {"Lamplit", (Check) WildercordShapesPackTest::lamplit},
				new Object[] {"Herd", (Check) WildercordShapesPackTest::herd},
				new Object[] {"Fellowship", (Check) WildercordShapesPackTest::fellowship},
				new Object[] {"Packbond", (Check) WildercordShapesPackTest::packbond},
				new Object[] {"Nursery", (Check) WildercordShapesPackTest::nursery},
				new Object[] {"Grudge", (Check) WildercordShapesPackTest::grudge},
				new Object[] {"Rearguard", (Check) WildercordShapesPackTest::rearguard},
				new Object[] {"Sentinel", (Check) WildercordShapesPackTest::sentinel},
				new Object[] {"Flock", (Check) WildercordShapesPackTest::flock},
				new Object[] {"Tether", (Check) WildercordShapesPackTest::tether},
				new Object[] {"Footing", (Check) WildercordShapesPackTest::footing});
			for (Object[] check : checks) {
				String failure;
				try {
					failure = ((Check) check[1]).run(context, world);
				} catch (RuntimeException e) {
					failure = "threw " + e;
				} finally {
					cleanup(context, world);
				}
				if (failure != null) {
					failures.add(check[0] + ": " + failure);
				}
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("The shapes pack went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** Floor and ground: twelve blocks of stone under a 21x21 square of open air. */
	private static void floor(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 12) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 10) + " " + (x + 10) + " " + (y + 8) + " " + (z + 10) + " minecraft:air");
	}

	private static void stage(TestSingleplayerContext world) {
		floor(world);
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
			stand(player, 0, 8);
		});
	}

	/** The caster at the middle of the platform, turned to {@code yaw} and {@code pitch}, whole and unburdened. */
	private static void stand(ServerPlayer player, float yaw, float pitch) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), yaw, pitch, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.getFoodData().setFoodLevel(20);
	}

	/** Turns the caster to look at the top of the floor block {@code (dx, -1, dz)} from the middle. */
	private static void lookAtFloor(ServerPlayer player, int dx, int dz) {
		Vec3 eye = new Vec3(STAGE.getX() + 0.5, STAGE.getY() + player.getEyeHeight(), STAGE.getZ() + 0.5);
		Vec3 target = new Vec3(STAGE.getX() + dx + 0.5, STAGE.getY() - 0.05, STAGE.getZ() + dz + 0.5);
		lookAt(player, eye, target);
	}

	private static void lookAt(ServerPlayer player, Vec3 eye, Vec3 target) {
		double ddx = target.x - eye.x;
		double ddy = target.y - eye.y;
		double ddz = target.z - eye.z;
		float yaw = (float) (Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(ddy, Math.sqrt(ddx * ddx + ddz * ddz)));
		stand(player, yaw, pitch);
	}

	private static BlockPos at(int dx, int dy, int dz) {
		return STAGE.offset(dx, dy, dz);
	}

	/** Threads {@code runes} as spell 1 and casts it. Returns what went wrong, or null. */
	private static String cast(ServerPlayer player, RuneDef... runes) {
		List<String> ids = java.util.Arrays.stream(runes).map(RuneDef::id).toList();
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids);
		if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
			return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
		}
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, Mana.max(player));
		player.setAttached(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	/** A creature of {@code type} at {@code (dx, dz)} from the caster, with no mind of its own. */
	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, double dx, double dy, double dz) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new IllegalStateException("couldn't make a " + type);
		}
		mob.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY() + dy, STAGE.getZ() + 0.5 + dz, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		return mob;
	}

	private static Mob husk(ServerLevel level, double dx, double dz) {
		return spawn(level, net.minecraft.world.entity.EntityTypes.HUSK, dx, 0, dz);
	}

	private static Wolf pet(ServerPlayer owner, double dx, double dz) {
		Wolf wolf = spawn(owner.level(), net.minecraft.world.entity.EntityTypes.WOLF, dx, 0, dz);
		wolf.tame(owner);
		return wolf;
	}

	private static LivingEntity byId(MinecraftServer server, int id) {
		Entity e = player(server).level().getEntity(id);
		return e instanceof LivingEntity living ? living : null;
	}

	private static boolean hurt(LivingEntity e) {
		return e == null || !e.isAlive() || e.getHealth() < e.getMaxHealth();
	}

	private static boolean untouched(LivingEntity e) {
		return e != null && e.isAlive() && e.getHealth() >= e.getMaxHealth();
	}

	private static String health(LivingEntity e) {
		return e == null ? "gone" : e.getHealth() + "/" + e.getMaxHealth();
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		floor(world);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			stand(player, 0, 8);
		});
		context.waitTicks(20);
	}

	/** Casts on the server thread after {@code setup}, waits {@code ticks}, then runs {@code verdict} there. */
	private static String castThen(ClientGameTestContext context, TestSingleplayerContext world, java.util.function.Function<ServerPlayer, String> setup,
			int ticks, java.util.function.Function<MinecraftServer, String> verdict) {
		String cast = world.getServer().computeOnServer(server -> setup.apply(player(server)));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(ticks);
		return world.getServer().computeOnServer(verdict::apply);
	}

	private static String airAt(MinecraftServer server, List<BlockPos> blocks, String what) {
		ServerLevel level = player(server).level();
		List<BlockPos> left = blocks.stream().filter(p -> !level.getBlockState(p).isAir()).toList();
		return left.isEmpty() ? null : what + ": still standing at " + left;
	}

	private static String solidAt(MinecraftServer server, List<BlockPos> blocks, String what) {
		ServerLevel level = player(server).level();
		List<BlockPos> gone = blocks.stream().filter(p -> level.getBlockState(p).isAir()).toList();
		return gone.isEmpty() ? null : what + ": broken at " + gone;
	}

	// ------------------------------------------------------------------ field shapes

	/** Shaft · Break: eight blocks straight down from the block you look at, and none beside them. */
	private static String shaft(ClientGameTestContext context, TestSingleplayerContext world) {
		return castThen(context, world, player -> {
			lookAtFloor(player, 0, 3);
			return cast(player, Runes.SHAFT, Runes.BREAK);
		}, 30, server -> {
			List<BlockPos> dug = new ArrayList<>();
			for (int i = 1; i <= 8; i++) dug.add(at(0, -i, 3));
			String air = airAt(server, dug, "the shaft");
			return air != null ? air : solidAt(server, List.of(at(0, -9, 3), at(1, -2, 3), at(0, -2, 4)), "beside or below the shaft");
		});
	}

	/** Stairwell · Break: a flight of eight steps down ahead of you, each one leaving its tread. */
	private static String stairwell(ClientGameTestContext context, TestSingleplayerContext world) {
		return castThen(context, world, player -> {
			stand(player, 0, 30);
			return cast(player, Runes.STAIRWELL, Runes.BREAK);
		}, 30, server -> {
			List<BlockPos> cut = new ArrayList<>();
			List<BlockPos> treads = new ArrayList<>();
			for (int i = 1; i <= 8; i++) {
				cut.add(at(0, -i, i));
				treads.add(at(0, -i - 1, i));
			}
			String air = airAt(server, cut, "the stairwell");
			return air != null ? air : solidAt(server, treads, "the steps' treads");
		});
	}

	/** Seam · Break: seven blocks across your view at the block you look at. */
	private static String seam(ClientGameTestContext context, TestSingleplayerContext world) {
		return castThen(context, world, player -> {
			lookAtFloor(player, 0, 4);
			return cast(player, Runes.SEAM, Runes.BREAK);
		}, 30, server -> {
			List<BlockPos> line = new ArrayList<>();
			for (int k = -3; k <= 3; k++) line.add(at(k, -1, 4));
			String air = airAt(server, line, "the seam");
			return air != null ? air : solidAt(server, List.of(at(0, -1, 3), at(0, -1, 5), at(4, -1, 4), at(0, -2, 4)), "beside the seam");
		});
	}

	/** Pit · Break: a 3x3 hole two deep where you look. */
	private static String pit(ClientGameTestContext context, TestSingleplayerContext world) {
		return castThen(context, world, player -> {
			lookAtFloor(player, 0, 5);
			return cast(player, Runes.PIT, Runes.BREAK);
		}, 30, server -> {
			List<BlockPos> hole = new ArrayList<>();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = 4; dz <= 6; dz++) {
					hole.add(at(dx, -1, dz));
					hole.add(at(dx, -2, dz));
				}
			}
			String air = airAt(server, hole, "the pit");
			return air != null ? air : solidAt(server, List.of(at(0, -3, 5), at(2, -1, 5), at(0, -1, 7)), "around the pit");
		});
	}

	/** Furrow · Break: a row of nine ground blocks running ahead of your feet. */
	private static String furrow(ClientGameTestContext context, TestSingleplayerContext world) {
		return castThen(context, world, player -> {
			stand(player, 0, 10);
			return cast(player, Runes.FURROW, Runes.BREAK);
		}, 30, server -> {
			List<BlockPos> row = new ArrayList<>();
			for (int i = 1; i <= 9; i++) row.add(at(0, -1, i));
			String air = airAt(server, row, "the furrow");
			return air != null ? air : solidAt(server, List.of(at(0, -1, 0), at(0, -1, 10), at(1, -1, 3)), "beside the furrow (and under the caster)");
		});
	}

	/** Lodeseek · Break: only the ores within 3 blocks of the aim are mined; the stone between them stays. */
	private static String lodeseek(ClientGameTestContext context, TestSingleplayerContext world) {
		List<BlockPos> ores = List.of(at(1, -2, 5), at(-2, -3, 6), at(0, -1, 7));
		return castThen(context, world, player -> {
			ServerLevel level = player.level();
			level.setBlockAndUpdate(ores.get(0), Blocks.IRON_ORE.defaultBlockState());
			level.setBlockAndUpdate(ores.get(1), Blocks.COAL_ORE.defaultBlockState());
			level.setBlockAndUpdate(ores.get(2), Blocks.COPPER_ORE.defaultBlockState());
			lookAtFloor(player, 0, 5);
			return cast(player, Runes.LODESEEK, Runes.BREAK);
		}, 30, server -> {
			String air = airAt(server, ores, "the ores");
			return air != null ? air : solidAt(server, List.of(at(0, -1, 5), at(0, -2, 5), at(1, -1, 5), at(0, -1, 6)), "the stone between the ores");
		});
	}

	/** Lamplit · Light: a light over the aim and one at each corner of a square nine blocks across. */
	private static String lamplit(ClientGameTestContext context, TestSingleplayerContext world) {
		return castThen(context, world, player -> {
			lookAtFloor(player, 0, 6);
			return cast(player, Runes.LAMPLIT, Runes.LIGHT);
		}, 20, server -> {
			ServerLevel level = player(server).level();
			List<BlockPos> lamps = List.of(at(0, 0, 6), at(4, 0, 10), at(-4, 0, 10), at(4, 0, 2), at(-4, 0, 2));
			List<BlockPos> dark = lamps.stream().filter(p -> !level.getBlockState(p).is(Blocks.LIGHT)).toList();
			return dark.isEmpty() ? null : "no light at " + dark;
		});
	}

	// ------------------------------------------------------------------ kin shapes

	/** Herd · Harm: strikes the cow, never the husk beside it. */
	private static String herd(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		return castThen(context, world, player -> {
			ids[0] = spawn(player.level(), net.minecraft.world.entity.EntityTypes.COW, 2, 0, 2).getId();
			ids[1] = husk(player.level(), -2, 2).getId();
			return cast(player, Runes.HERD, Runes.HARM);
		}, 30, server -> {
			LivingEntity cow = byId(server, ids[0]);
			LivingEntity husk = byId(server, ids[1]);
			if (!hurt(cow)) return "the cow should be struck (" + health(cow) + ")";
			return untouched(husk) ? null : "the husk isn't an animal of the herd (" + health(husk) + ")";
		});
	}

	/** Fellowship · Heal: the hurt pet wolf is healed; the hurt husk beside it is not. */
	private static String fellowship(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		return castThen(context, world, player -> {
			Wolf wolf = pet(player, 2, 2);
			wolf.setHealth(10);
			Mob husk = husk(player.level(), -2, 2);
			husk.setHealth(10);
			ids[0] = wolf.getId();
			ids[1] = husk.getId();
			return cast(player, Runes.FELLOWSHIP, Runes.HEAL);
		}, 30, server -> {
			LivingEntity wolf = byId(server, ids[0]);
			LivingEntity husk = byId(server, ids[1]);
			if (wolf == null || wolf.getHealth() <= 10) return "the pet should be healed (" + health(wolf) + ")";
			return husk != null && husk.getHealth() <= 10 ? null : "a foe is never healed (" + health(husk) + ")";
		});
	}

	/** Packbond · Heal: your own wolf is healed, a stray wolf is not chosen. */
	private static String packbond(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		return castThen(context, world, player -> {
			Wolf mine = pet(player, 3, 3);
			mine.setHealth(10);
			Wolf stray = spawn(player.level(), net.minecraft.world.entity.EntityTypes.WOLF, -3, 0, 3);
			stray.setHealth(4);
			ids[0] = mine.getId();
			ids[1] = stray.getId();
			return cast(player, Runes.PACKBOND, Runes.HEAL);
		}, 30, server -> {
			LivingEntity mine = byId(server, ids[0]);
			LivingEntity stray = byId(server, ids[1]);
			if (mine == null || mine.getHealth() <= 10) return "your wolf should be healed (" + health(mine) + ")";
			return stray != null && stray.getHealth() <= 4 ? null : "the stray isn't yours (" + health(stray) + ")";
		});
	}

	/** Nursery · Harm: strikes the calf, never the grown cow. */
	private static String nursery(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		return castThen(context, world, player -> {
			Mob calf = spawn(player.level(), net.minecraft.world.entity.EntityTypes.COW, 2, 0, 2);
			((AgeableMob) calf).setBaby(true);
			ids[0] = calf.getId();
			ids[1] = spawn(player.level(), net.minecraft.world.entity.EntityTypes.COW, -2, 0, 2).getId();
			return cast(player, Runes.NURSERY, Runes.HARM);
		}, 30, server -> {
			LivingEntity calf = byId(server, ids[0]);
			LivingEntity cow = byId(server, ids[1]);
			if (!hurt(calf)) return "the calf should be struck (" + health(calf) + ")";
			return untouched(cow) ? null : "the grown cow isn't young (" + health(cow) + ")";
		});
	}

	/** Grudge · Harm: strikes the husk hunting you, never the one that isn't. */
	private static String grudge(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		return castThen(context, world, player -> {
			Mob hunter = husk(player.level(), 4, 6);
			hunter.setTarget(player);
			ids[0] = hunter.getId();
			ids[1] = husk(player.level(), -2, 2).getId();
			return cast(player, Runes.GRUDGE, Runes.HARM);
		}, 30, server -> {
			LivingEntity hunter = byId(server, ids[0]);
			LivingEntity idle = byId(server, ids[1]);
			if (!hurt(hunter)) return "the husk hunting you should be struck (" + health(hunter) + ")";
			return untouched(idle) ? null : "the idle husk bears no grudge (" + health(idle) + ")";
		});
	}

	/** Rearguard · Harm: strikes the husk behind you, never the one in front. */
	private static String rearguard(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		return castThen(context, world, player -> {
			ids[0] = husk(player.level(), 0, -3).getId();
			ids[1] = husk(player.level(), 0, 3).getId();
			stand(player, 0, 8);
			return cast(player, Runes.REARGUARD, Runes.HARM);
		}, 30, server -> {
			LivingEntity behind = byId(server, ids[0]);
			LivingEntity ahead = byId(server, ids[1]);
			if (!hurt(behind)) return "the husk behind should be struck (" + health(behind) + ")";
			return untouched(ahead) ? null : "the husk ahead is not behind you (" + health(ahead) + ")";
		});
	}

	/** Sentinel · Harm: strikes the husk hunting your wolf, never the idle one. */
	private static String sentinel(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[3];
		return castThen(context, world, player -> {
			Wolf wolf = pet(player, 0, 6);
			Mob hunter = husk(player.level(), 2, 7);
			hunter.setTarget(wolf);
			ids[0] = hunter.getId();
			ids[1] = husk(player.level(), -3, 3).getId();
			ids[2] = wolf.getId();
			return cast(player, Runes.SENTINEL, Runes.HARM);
		}, 30, server -> {
			LivingEntity hunter = byId(server, ids[0]);
			LivingEntity idle = byId(server, ids[1]);
			LivingEntity wolf = byId(server, ids[2]);
			if (!hurt(hunter)) return "the husk hunting your wolf should be struck (" + health(hunter) + ")";
			if (!untouched(wolf)) return "your wolf is never struck (" + health(wolf) + ")";
			return untouched(idle) ? null : "the idle husk hunts no ally (" + health(idle) + ")";
		});
	}

	/** Flock · Harm: strikes the bat, never the husk. */
	private static String flock(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[2];
		return castThen(context, world, player -> {
			ids[0] = spawn(player.level(), net.minecraft.world.entity.EntityTypes.BAT, 2, 1.5, 3).getId();
			ids[1] = husk(player.level(), -2, 3).getId();
			return cast(player, Runes.FLOCK, Runes.HARM);
		}, 30, server -> {
			LivingEntity bat = byId(server, ids[0]);
			LivingEntity husk = byId(server, ids[1]);
			if (!hurt(bat)) return "the bat should be struck (" + health(bat) + ")";
			return untouched(husk) ? null : "the husk doesn't fly (" + health(husk) + ")";
		});
	}

	/** Tether · Harm: the husk you look at and the one beside it, never one far off. */
	private static String tether(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = new int[3];
		return castThen(context, world, player -> {
			Mob held = husk(player.level(), 0, 6);
			ids[0] = held.getId();
			ids[1] = husk(player.level(), 2, 6).getId();
			ids[2] = husk(player.level(), -4, -4).getId();
			Vec3 eye = new Vec3(STAGE.getX() + 0.5, STAGE.getY() + player.getEyeHeight(), STAGE.getZ() + 0.5);
			lookAt(player, eye, held.getBoundingBox().getCenter());
			return cast(player, Runes.TETHER, Runes.HARM);
		}, 30, server -> {
			LivingEntity held = byId(server, ids[0]);
			LivingEntity near = byId(server, ids[1]);
			LivingEntity far = byId(server, ids[2]);
			if (!hurt(held)) return "the tethered husk should be struck (" + health(held) + ")";
			if (!hurt(near)) return "the husk within 3 blocks of it should be struck (" + health(near) + ")";
			return untouched(far) ? null : "the far husk is out of the tether's reach (" + health(far) + ")";
		});
	}

	/** Footing · Heal: a field under your own feet that heals you. */
	private static String footing(ClientGameTestContext context, TestSingleplayerContext world) {
		return castThen(context, world, player -> {
			stand(player, 0, 8);
			player.setHealth(8);
			return cast(player, Runes.FOOTING, Runes.HEAL);
		}, 20, server -> {
			ServerPlayer player = player(server);
			return player.getHealth() > 8 ? null : "the caster should be healed (" + player.getHealth() + ")";
		});
	}
}
