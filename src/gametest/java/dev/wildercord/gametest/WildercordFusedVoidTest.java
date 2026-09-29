package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * In game, the fused effects of {@code cast.FusedVoid}: each one cast for real and checked for what its
 * description promises. On a stone platform high in the air, at husks (in diamond armour where
 * "through armour" matters), cast straight from a Touch at power 1 so the numbers are exact; Chronoshift
 * is cast from the Cord, since what it does is to the caster's own spells.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedVoidTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		// A second after it lands: the first pip gone dark, the husk fraying.
		new FusedSample(Runes.ENTROPY, Runes.BEAM, 24, false),
		// The jaws half shut.
		new FusedSample(Runes.DEVOUR, Runes.BEAM, 3, false),
		new FusedSample(Runes.TIMESTEAL, Runes.BEAM, 4, false),
		new FusedSample(Runes.HEMOMANCY, Runes.BEAM, 4, false),
		// The seal open over the husk, its hand part way round.
		new FusedSample(Runes.RECKONING, Runes.BEAM, 16, false),
		// The black hole a second in, by night so its disk glows.
		new FusedSample(Runes.SINGULARITY, Runes.BOLT, 30, true),
		new FusedSample(Runes.PRISMATIC_BURST, Runes.BEAM, 4, false),
		new FusedSample(Runes.CHRONOSHIFT, Runes.SELF, 8, false));

	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 200, 0);
	private static final String TAG = "wildercord.fused_void";

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
			check(failures, "Entropy", entropy(context, world));
			check(failures, "Devour", world.getServer().computeOnServer(WildercordFusedVoidTest::devour));
			check(failures, "Timesteal", world.getServer().computeOnServer(WildercordFusedVoidTest::timesteal));
			check(failures, "Timesteal (a crowd of ten)", world.getServer().computeOnServer(WildercordFusedVoidTest::timestealCrowd));
			check(failures, "Hemomancy", world.getServer().computeOnServer(WildercordFusedVoidTest::hemomancy));
			check(failures, "Reckoning", reckoning(context, world));
			check(failures, "Singularity", singularity(context, world));
			check(failures, "Prismatic Burst", world.getServer().computeOnServer(WildercordFusedVoidTest::prismaticBurst));
			check(failures, "Chronoshift", chronoshift(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The fused runes of void, arcane and time went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void check(List<String> failures, String rune, String failure) {
		if (failure != null) {
			failures.add(rune + ": " + failure);
		}
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** A wide stone floor with open air above it (room for Singularity to fling), an Echo Cord, every rune known, and plenty of mana. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 10) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 14) + " " + y + " " + (z - 10) + " " + (x + 14) + " " + (y + 8) + " " + (z + 24) + " minecraft:air");
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

	/** The caster at the middle of the platform, facing south (+Z), whole and with nothing on them. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0);
		player.removeAllEffects();
		player.clearFire();
	}

	/** A still husk (no AI) {@code ahead} blocks south of the caster and {@code side} blocks across, at full health. */
	private static Mob husk(ServerLevel level, double side, double ahead) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.snapTo(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead, 180, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		husk.setHealth(husk.getMaxHealth());
		return husk;
	}

	/** {@code rune} cast straight from a Touch at {@code targets}, at power 1 (no affinity, gear or charge). */
	private static void touch(ServerPlayer player, RuneDef rune, List<? extends Entity> targets) {
		SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.TOUCH, rune)).root().groups.getFirst().effects.getFirst();
		Vec3 at = targets.isEmpty() ? player.position() : targets.getFirst().position();
		Effects.apply(new Cast(player), node, new Cast.Hit(List.copyOf(targets), at, player.getLookAngle(), player.position(), null, null, false));
	}

	private static void touch(ServerPlayer player, RuneDef rune, Entity target) {
		touch(player, rune, List.of(target));
	}

	private static <T> T onServer(TestSingleplayerContext world, Function<MinecraftServer, T> task) {
		return world.getServer().computeOnServer(task::apply);
	}

	private static float health(TestSingleplayerContext world, int id) {
		return onServer(world, server -> player(server).level().getEntity(id) instanceof LivingEntity e && e.isAlive() ? e.getHealth() : 0.0F);
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 0.02;
	}

	private static void clean(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(20);
	}

	// ------------------------------------------------------------------ Entropy

	/** 1, 1.5, 2, 2.5 and 3 over five seconds, straight through diamond armour; struck twice, it's still one. */
	private static String entropy(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), 0, 4);
			husk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			husk.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
			touch(player, Runes.ENTROPY, husk);
			touch(player, Runes.ENTROPY, husk);
			return husk.getId();
		});
		String failure = null;
		context.waitTicks(5);
		float early = health(world, id);
		context.waitTicks(20);
		float first = health(world, id);
		context.waitTicks(90);
		float end = health(world, id);
		context.waitTicks(30);
		float after = health(world, id);
		if (!near(early, 20)) {
			failure = "it should do nothing the moment it lands (health " + early + " after 5 ticks)";
		} else if (!near(first, 19)) {
			failure = "the first wound, a second in, should be 1 through the armour, and only one of them though it was cast twice (health " + first + ")";
		} else if (!near(end, 10)) {
			failure = "five wounds of 1, 1.5, 2, 2.5 and 3 should take 10 through the armour (health " + end + ")";
		} else if (!near(after, end)) {
			failure = "it should stop after five seconds (health " + end + " then " + after + ")";
		}
		clean(context, world);
		return failure;
	}

	// ------------------------------------------------------------------ Devour

	/** 5 damage; a kill feeds 10 mana and 4 absorption, which never piles past 4; a cast feeds at most twice. */
	private static String devour(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		stand(player);
		Spellbooks.setMana(player, 20);
		try {
			Mob whole = husk(level, 0, 4);
			touch(player, Runes.DEVOUR, whole);
			if (!near(whole.getHealth(), 15)) {
				return "it should deal 5 (health " + whole.getHealth() + ")";
			}
			if (!near(Spellbooks.mana(player), 20) || player.getAbsorptionAmount() > 0) {
				return "a bite that doesn't kill shouldn't feed (mana " + Spellbooks.mana(player) + ", absorption " + player.getAbsorptionAmount() + ")";
			}
			Mob weak = husk(level, 1.5, 4);
			weak.setHealth(3);
			touch(player, Runes.DEVOUR, weak);
			if (weak.isAlive()) {
				return "5 damage should kill a husk on 3 health";
			}
			if (!near(Spellbooks.mana(player), 30) || !near(player.getAbsorptionAmount(), 4) || !player.hasEffect(MobEffects.ABSORPTION)) {
				return "a kill should feed 10 mana and 4 absorption (mana " + Spellbooks.mana(player) + " of 20 before, absorption " + player.getAbsorptionAmount() + ")";
			}
			Mob another = husk(level, -1.5, 4);
			another.setHealth(3);
			touch(player, Runes.DEVOUR, another);
			if (!near(player.getAbsorptionAmount(), 4)) {
				return "a second kill shouldn't stack the absorption past 4 (has " + player.getAbsorptionAmount() + ")";
			}
			if (!near(Spellbooks.mana(player), 40)) {
				return "a second kill should feed 10 more mana (has " + Spellbooks.mana(player) + ", 30 before)";
			}
			List<Mob> flock = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				Mob prey = husk(level, -2 + 2 * i, 6);
				prey.setHealth(3);
				flock.add(prey);
			}
			touch(player, Runes.DEVOUR, flock);
			if (flock.stream().anyMatch(LivingEntity::isAlive)) {
				return "one cast should bite all three husks it touches";
			}
			if (!near(Spellbooks.mana(player), 60)) {
				return "one cast should feed at most twice, 20 mana for three kills (has " + Spellbooks.mana(player) + ", 40 before)";
			}
			return null;
		} finally {
			level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20), m -> m.entityTags().contains(TAG)).forEach(Mob::discard);
			stand(player);
		}
	}

	// ------------------------------------------------------------------ Timesteal

	/**
	 * The two best good effects (the strongest, then the longest) change hands with their time, at most
	 * 30 seconds; the rest, and anything harmful, stay. A boss only loses the time taken.
	 */
	private static String timesteal(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		stand(player);
		try {
			Mob husk = husk(level, 0, 4);
			husk.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 1));
			husk.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
			// Resistance, not Regeneration, for the endless one: a husk is undead and can't hold Regeneration at all.
			husk.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0));
			husk.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 0));
			touch(player, Runes.TIMESTEAL, husk);
			if (husk.hasEffect(MobEffects.SPEED) || husk.hasEffect(MobEffects.RESISTANCE)) {
				return "the husk's Speed II and endless Resistance should be taken (it has " + husk.getActiveEffectsMap().keySet() + ")";
			}
			if (!husk.hasEffect(MobEffects.STRENGTH) || !husk.hasEffect(MobEffects.SLOWNESS)) {
				return "only two should be taken, and never a harmful one (it has " + husk.getActiveEffectsMap().keySet() + ")";
			}
			MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
			MobEffectInstance endless = player.getEffect(MobEffects.RESISTANCE);
			if (speed == null || speed.getAmplifier() != 1 || speed.getDuration() > 600 || speed.getDuration() < 590) {
				return "the caster should get Speed II for 30 seconds, the most it may take of 60 (has " + speed + ")";
			}
			if (endless == null || endless.isInfiniteDuration() || endless.getDuration() > 600 || endless.getDuration() < 590) {
				return "an endless Resistance should come across as 30 seconds (has " + endless + ")";
			}
			if (player.hasEffect(MobEffects.STRENGTH) || player.hasEffect(MobEffects.SLOWNESS)) {
				return "the caster should get only what was taken (has " + player.getActiveEffectsMap().keySet() + ")";
			}
			player.removeAllEffects();
			Mob warden = EntityTypes.WARDEN.create(level, EntitySpawnReason.COMMAND);
			if (warden == null) {
				return "couldn't make a warden";
			}
			warden.snapTo(STAGE.getX() + 4.5, STAGE.getY(), STAGE.getZ() + 6.5, 180, 0);
			warden.setNoAi(true);
			warden.addTag(TAG);
			// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
			warden.addTag("wildercord.rolled");
			level.addFreshEntity(warden);
			warden.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0));
			touch(player, Runes.TIMESTEAL, warden);
			MobEffectInstance kept = warden.getEffect(MobEffects.SPEED);
			if (kept == null || kept.getDuration() > 605 || kept.getDuration() < 590) {
				return "a boss should lose only the 30 seconds taken, keeping the rest (has " + kept + " of 60 s)";
			}
			MobEffectInstance got = player.getEffect(MobEffects.SPEED);
			if (got == null || got.getDuration() < 590) {
				return "the caster should still get the boss's 30 seconds (has " + got + ")";
			}
			return null;
		} finally {
			level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20), m -> m.entityTags().contains(TAG)).forEach(Mob::discard);
			stand(player);
		}
	}

	/** Ten husks with Speed, more than the eight that are drawn: one Timesteal takes it from every one of them. */
	private static String timestealCrowd(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		stand(player);
		try {
			List<Mob> husks = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				Mob husk = husk(level, -9 + 2 * i, 4);
				husk.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0));
				husks.add(husk);
			}
			touch(player, Runes.TIMESTEAL, husks);
			for (int i = 0; i < husks.size(); i++) {
				if (husks.get(i).hasEffect(MobEffects.SPEED)) {
					return "husk " + (i + 1) + " of ten should have had its Speed taken too";
				}
			}
			return null;
		} finally {
			level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20), m -> m.entityTags().contains(TAG)).forEach(Mob::discard);
			stand(player);
		}
	}

	// ------------------------------------------------------------------ Hemomancy

	/** 4 magic damage, and 1 more for every 2 health the caster is missing, up to 6 more. */
	private static String hemomancy(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		stand(player);
		try {
			float[][] cases = {{20, 4}, {13, 8}, {2, 12}};
			for (float[] c : cases) {
				player.setHealth(c[0]);
				Mob husk = husk(level, 0, 4);
				husk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
				touch(player, Runes.HEMOMANCY, husk);
				float dealt = husk.getMaxHealth() - husk.getHealth();
				husk.discard();
				if (!near(dealt, c[1])) {
					return "on " + c[0] + " of 20 health it should deal " + c[1] + " (dealt " + dealt + ")";
				}
			}
			return null;
		} finally {
			level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20), m -> m.entityTags().contains(TAG)).forEach(Mob::discard);
			stand(player);
		}
	}

	// ------------------------------------------------------------------ Reckoning

	/** Four seconds of wounds, from anything, counted; half comes due at once, at most 12; a second Reckoning never doubles it. */
	private static String reckoning(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Mob counted = husk(level, -2, 4);
			Mob twice = husk(level, 0, 4);
			Mob big = husk(level, 2, 4);
			big.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
			big.setHealth(100);
			touch(player, Runes.RECKONING, counted);
			touch(player, Runes.RECKONING, twice);
			touch(player, Runes.RECKONING, twice);
			touch(player, Runes.RECKONING, big);
			// Wounds from something else entirely.
			wound(level, counted, 6);
			wound(level, counted, 6);
			wound(level, twice, 10);
			wound(level, big, 30);
			return new int[] {counted.getId(), twice.getId(), big.getId()};
		});
		context.waitTicks(20);
		float counted = health(world, ids[0]);
		float twice = health(world, ids[1]);
		float big = health(world, ids[2]);
		String failure = null;
		if (!near(counted, 8) || !near(twice, 10) || !near(big, 70)) {
			failure = "nothing should come due before the four seconds are up (health " + counted + ", " + twice + ", " + big + ")";
		}
		context.waitTicks(80);
		if (failure == null) {
			counted = health(world, ids[0]);
			twice = health(world, ids[1]);
			big = health(world, ids[2]);
			if (!near(counted, 2)) {
				failure = "two wounds of 6 should come due as 6 (health " + counted + ", 8 before)";
			} else if (!near(twice, 5)) {
				failure = "two Reckonings on one target should still be one ledger: 10 in wounds, 5 due (health " + twice + ", 10 before)";
			} else if (!near(big, 58)) {
				failure = "30 in wounds should come due as at most 12 (health " + big + ", 70 before)";
			}
		}
		context.waitTicks(20);
		if (failure == null && !near(health(world, ids[0]), counted)) {
			failure = "the reckoning's own blow shouldn't open or feed another ledger (health " + counted + " then " + health(world, ids[0]) + ")";
		}
		clean(context, world);
		return failure;
	}

	/** A wound from outside the spell (magic, so armour doesn't blur the count). */
	private static void wound(ServerLevel level, LivingEntity target, float amount) {
		Effects.readyToHurt(target);
		target.hurtServer(level, level.damageSources().magic(), amount);
	}

	// ------------------------------------------------------------------ Singularity

	/** Enemies within 5 blocks are drawn in for 2.5 seconds, then take 6 and are flung out. */
	private static String singularity(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 centre = new Vec3(STAGE.getX() + 0.5, STAGE.getY() + 1.2, STAGE.getZ() + 7.5);
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			int[] out = new int[3];
			double[][] spots = {{-4, 7}, {4, 7}, {0, 11}};
			for (int i = 0; i < 3; i++) {
				// With its wits (so it falls and slides like any creature) but no legs of its own.
				Mob husk = husk(level, spots[i][0], spots[i][1]);
				husk.setNoAi(false);
				husk.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
				out[i] = husk.getId();
			}
			SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.SINGULARITY)).root().groups.getFirst().effects.getFirst();
			Effects.apply(new Cast(player), node, new Cast.Hit(List.of(), centre, new Vec3(0, 0, 1), player.position(), null, null, false));
			return out;
		});
		context.waitTicks(40);
		double[] drawn = distances(world, ids, centre);
		float[] before = new float[3];
		for (int i = 0; i < 3; i++) {
			before[i] = health(world, ids[i]);
		}
		context.waitTicks(22);
		double[] flung = distances(world, ids, centre);
		String failure = null;
		for (int i = 0; i < 3 && failure == null; i++) {
			float after = health(world, ids[i]);
			if (drawn[i] > 2.5) {
				failure = "a husk 4 blocks out should be drawn in (still " + String.format("%.2f", drawn[i]) + " blocks off after 2 seconds)";
			} else if (!near(before[i], 20)) {
				failure = "nothing should be hurt before the burst (health " + before[i] + ")";
			} else if (after > 14.05F || after < 13.0F) {
				failure = "the burst should deal 6 (health " + after + ")";
			} else if (flung[i] < drawn[i] + 1.0) {
				failure = "the burst should fling it out (" + String.format("%.2f", drawn[i]) + " blocks off, then " + String.format("%.2f", flung[i]) + ")";
			}
		}
		clean(context, world);
		return failure;
	}

	private static double[] distances(TestSingleplayerContext world, int[] ids, Vec3 centre) {
		return onServer(world, server -> {
			double[] out = new double[ids.length];
			for (int i = 0; i < ids.length; i++) {
				Entity e = player(server).level().getEntity(ids[i]);
				out[i] = e == null ? -1 : Math.hypot(e.getX() - centre.x, e.getZ() - centre.z);
			}
			return out;
		});
	}

	// ------------------------------------------------------------------ Prismatic Burst

	/** 5 damage, and 4 more for every mark (five at most) (burning, frozen, windswept, pulled, soaked, wet), each used up. */
	private static String prismaticBurst(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		stand(player);
		try {
			Mob plain = husk(level, -2, 4);
			touch(player, Runes.PRISMATIC_BURST, plain);
			if (!near(plain.getHealth(), 15)) {
				return "with no marks it should deal 5 (health " + plain.getHealth() + ")";
			}
			Mob marked = husk(level, 0, 4);
			marked.igniteForSeconds(5);
			Reactions.mark(marked, Reactions.Mark.FROZEN);
			Reactions.mark(marked, Reactions.Mark.WINDSWEPT);
			touch(player, Runes.PRISMATIC_BURST, marked);
			if (!near(marked.getHealth(), 3)) {
				return "burning, frozen and windswept should make it 5 + 12 = 17 (health " + marked.getHealth() + ")";
			}
			if (marked.isOnFire() || Reactions.has(marked, Reactions.Mark.FROZEN) || Reactions.has(marked, Reactions.Mark.WINDSWEPT)) {
				return "the marks should be used up (on fire " + marked.isOnFire() + ", frozen " + Reactions.has(marked, Reactions.Mark.FROZEN) + ", windswept "
					+ Reactions.has(marked, Reactions.Mark.WINDSWEPT) + ")";
			}
			touch(player, Runes.PRISMATIC_BURST, marked);
			if (marked.isAlive()) {
				return "with its marks used up, the next should deal 5 and finish it (health " + marked.getHealth() + ")";
			}
			Mob all = husk(level, 2, 4);
			all.igniteForSeconds(5);
			for (Reactions.Mark mark : List.of(Reactions.Mark.FROZEN, Reactions.Mark.WINDSWEPT, Reactions.Mark.PULLED, Reactions.Mark.SOAKED, Reactions.Mark.WET)) {
				Reactions.mark(all, mark);
			}
			all.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
			all.setHealth(40);
			touch(player, Runes.PRISMATIC_BURST, all);
			if (!near(all.getHealth(), 15)) {
				return "six marks count as five: 5 + 20 = 25 (health " + all.getHealth() + " of 40)";
			}
			return null;
		} finally {
			level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20), m -> m.entityTags().contains(TAG)).forEach(Mob::discard);
			stand(player);
		}
	}

	// ------------------------------------------------------------------ Chronoshift

	/**
	 * Cast from the Cord on Self: the caster's other spells come off cooldown 3 seconds sooner (more with
	 * the cast's power, never over twice), but not this spell nor another holding Chronoshift; Haste and
	 * Speed; and cast again at once, it doesn't turn the clock a second time.
	 */
	private static String chronoshift(ClientGameTestContext context, TestSingleplayerContext world) {
		long[] start = onServer(world, server -> {
			ServerPlayer player = player(server);
			stand(player);
			SpellCaster.edit(player, 0, List.of(Runes.SELF.id(), Runes.CHRONOSHIFT.id()));
			SpellCaster.edit(player, 1, List.of(Runes.BOLT.id(), Runes.FIRE.id()));
			SpellCaster.edit(player, 2, List.of(Runes.BOLT.id(), Runes.CHRONOSHIFT.id()));
			long now = player.level().getGameTime();
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setReadyAt(player, 1, now + 200);
			Spellbooks.setReadyAt(player, 2, now + 200);
			Spellbooks.setReadyAt(player, 3, 0);
			Spellbooks.setMana(player, Mana.max(player));
			// Its cooldown as the cast will work it out (before the cast's own mana is condensed into the heart).
			int cooldown = Heart.cooldownTicks(player, SpellCompiler.compile(List.of(Runes.SELF, Runes.CHRONOSHIFT)));
			SpellCaster.cast(player, 0);
			return new long[] {now, cooldown};
		});
		context.waitTicks(3);
		long now = start[0];
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			long own = Spellbooks.readyAt(player, 0);
			long other = Spellbooks.readyAt(player, 1);
			long twin = Spellbooks.readyAt(player, 2);
			if (own <= now) {
				return "it didn't cast (spell 1 ready at " + own + ", now " + now + ")";
			}
			if (own != now + start[1]) {
				return "its own spell's cooldown shouldn't be shortened (ready in " + (own - now) + " ticks, its cooldown is " + start[1] + ")";
			}
			long by = now + 200 - other;
			if (by < 55 || by > 121) {
				return "another spell should come off cooldown about 3 seconds sooner (" + by + " ticks sooner)";
			}
			if (twin != now + 200) {
				return "another spell holding Chronoshift shouldn't be shortened (" + (now + 200 - twin) + " ticks sooner)";
			}
			if (!player.hasEffect(MobEffects.HASTE) || !player.hasEffect(MobEffects.SPEED)) {
				return "the caster should have Haste and Speed (has " + player.getActiveEffectsMap().keySet() + ")";
			}
			// Again at once: the Haste and Speed come again, the clock doesn't turn twice.
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, Mana.max(player));
			player.removeAllEffects();
			SpellCaster.cast(player, 0);
			if (Spellbooks.readyAt(player, 1) != other) {
				return "cast again while the clock is still turned, it shouldn't turn it again (" + (other - Spellbooks.readyAt(player, 1)) + " ticks more)";
			}
			return null;
		});
		context.waitTicks(3);
		if (failure == null) {
			boolean buffed = onServer(world, server -> player(server).hasEffect(MobEffects.HASTE) && player(server).hasEffect(MobEffects.SPEED));
			if (!buffed) {
				failure = "cast again, it should still give Haste and Speed";
			}
		}
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (int i = 0; i < 4; i++) {
				SpellCaster.edit(player, i, List.of());
				Spellbooks.setReadyAt(player, i, 0);
			}
		});
		clean(context, world);
		return failure;
	}
}
