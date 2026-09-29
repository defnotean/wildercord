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
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * In game, the fused effects of {@code cast.FusedFlame}: each one cast for real (a Beam at a husk, or Self) from a
 * stone platform high in the air, and checked for what its description promises: who burns and who doesn't, what is
 * dragged and how far, what is thrown, how much each hurt costs, when things end.
 *
 * <p>Husks here have no armour, so damage reads plainly. Those that must move (pulled, dragged, thrown) keep their AI,
 * since a mob without it never moves at all, but can't walk; the rest stand still with none.</p>
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedFlameTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		new FusedSample(Runes.PHOENIX_PYRE, Runes.SELF, 8, false),
		new FusedSample(Runes.HELLMOUTH, Runes.BOLT, 20, false),
		new FusedSample(Runes.STARFIRE, Runes.BOLT, 11, true),
		new FusedSample(Runes.EVERBURN, Runes.BOLT, 10, false),
		new FusedSample(Runes.BLOODBOIL, Runes.BOLT, 7, false),
		new FusedSample(Runes.CONFLAGRATION, Runes.COMET, 12, true),
		new FusedSample(Runes.MONOLITH, Runes.BOLT, 10, false),
		new FusedSample(Runes.MAGNETIZE, Runes.BOLT, 16, false),
		new FusedSample(Runes.SINKHOLE, Runes.BOLT, 12, false));

	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.fused_flame";
	/** What {@code cast.BlockFx} tags its (and Monolith's, and Sinkhole's) block displays with. */
	private static final String STONE_TAG = "wildercord.fx";

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
			world.getServer().runCommand("difficulty normal");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			check(context, world, failures, "Phoenix Pyre", () -> phoenixPyre(context, world));
			check(context, world, failures, "Hellmouth", () -> hellmouth(context, world));
			check(context, world, failures, "Starfire", () -> starfire(context, world));
			check(context, world, failures, "Everburn", () -> everburn(context, world));
			check(context, world, failures, "Bloodboil", () -> bloodboil(context, world));
			check(context, world, failures, "Conflagration", () -> conflagration(context, world));
			check(context, world, failures, "Monolith", () -> monolith(context, world));
			check(context, world, failures, "Magnetize", () -> magnetize(context, world));
			check(context, world, failures, "Sinkhole", () -> sinkhole(context, world));
			check(context, world, failures, "Firestorm on a crowd", () -> firestormCrowd(world));
			check(context, world, failures, "Magma's later seconds", () -> magmaLingers(context, world));
			check(context, world, failures, "Magma on a bunch", () -> magmaBunch(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The fused runes of flame and stone went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	/** Runs one rune's check, notes what it found wrong under its name, and clears the platform after it. */
	private static void check(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures, String name, Supplier<List<String>> body) {
		try {
			for (String problem : body.get()) {
				failures.add(name + ": " + problem);
			}
		} catch (RuntimeException e) {
			failures.add(name + " threw " + e);
		}
		cleanup(context, world);
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** A 25x25 stone floor with open air above it, an Echo Cord, every rune known, and plenty of mana. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 12) + " " + (x + 12) + " " + (y - 1) + " " + (z + 12) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 12) + " " + (x + 12) + " " + (y + 8) + " " + (z + 12) + " minecraft:air");
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

	/** The caster at the middle of the platform, facing south (+Z) and a little down: a Beam hits a husk 5 blocks ahead. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
	}

	/**
	 * A husk {@code dx}, {@code dz} blocks from the caster, facing it, with no armour. One that {@code moves} keeps its
	 * AI (so it can be pushed and thrown) but can't walk; one that doesn't has no AI at all.
	 */
	private static Mob husk(ServerLevel level, double dx, double dz, boolean moves) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		if (husk == null) {
			throw new IllegalStateException("no husk");
		}
		husk.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		husk.setNoAi(!moves);
		husk.getAttribute(Attributes.ARMOR).setBaseValue(0);
		if (moves) {
			husk.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
		}
		husk.setPersistenceRequired();
		husk.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		return husk;
	}

	/** A wolf tamed by the caster: a pet, which no spell of theirs may burn. */
	private static Wolf pet(ServerPlayer player, double dx, double dz) {
		ServerLevel level = player.level();
		Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
		if (wolf == null) {
			throw new IllegalStateException("no wolf");
		}
		wolf.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 0, 0);
		wolf.tame(player);
		wolf.setNoAi(true);
		wolf.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		wolf.addTag("wildercord.rolled");
		level.addFreshEntity(wolf);
		return wolf;
	}

	/** Threads {@code runes} into spell 1 and casts it, with full mana and no cooldown. Returns what went wrong, or null. */
	private static String cast(ServerPlayer player, RuneDef... runes) {
		List<String> ids = Arrays.stream(runes).map(RuneDef::id).toList();
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids);
		if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
			return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
		}
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, Mana.max(player));
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	private static String castNow(TestSingleplayerContext world, RuneDef... runes) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			return cast(player, runes);
		});
	}

	private static LivingEntity get(MinecraftServer server, int id) {
		return player(server).level().getEntity(id) instanceof LivingEntity living ? living : null;
	}

	/** Health lost from full (all of it for one that's gone: killed by the spell). */
	private static float lost(LivingEntity e) {
		return e == null ? Float.MAX_VALUE : e.getMaxHealth() - e.getHealth();
	}

	private static boolean burning(LivingEntity e) {
		return e != null && e.getRemainingFireTicks() > 0;
	}

	private static double flat(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static String describe(LivingEntity e) {
		if (e == null) {
			return "gone";
		}
		return String.format("%.1f/%.0f health, fire %d, at %.3f %.3f %.3f%s on %s, effects %s", e.getHealth(), e.getMaxHealth(), e.getRemainingFireTicks(),
			e.getX(), e.getY(), e.getZ(), (e.onGround() ? " on the ground" : " in the air") + (e.isNoGravity() ? " (no gravity)" : "") + " moving " + e.getDeltaMovement()
				+ (e instanceof net.minecraft.world.entity.Mob m && m.isNoAi() ? " (no AI)" : "") + " in " + e.level().getBlockState(e.blockPosition()).getBlock()
				+ (e.level().noCollision(e) ? "" : " (stuck)") + " near " + e.level().getEntities(e, e.getBoundingBox().inflate(0.3)).stream().map(o -> o.getType().toShortString()).toList(),
				e.level().getBlockState(e.blockPosition().below()).getBlock(),
			e.getActiveEffectsMap().keySet());
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=block_display,tag=" + STONE_TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(20);
	}

	// ------------------------------------------------------------------ the runes

	/**
	 * Phoenix Pyre on yourself: Regeneration and Fire Resistance; a husk beside you is set alight and burned, one
	 * further off isn't, nor your pet wolf beside you, nor you. After its 6 seconds a husk beside you is safe again.
	 */
	private static List<String> phoenixPyre(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			stand(player);
			return new int[] {husk(level, 1.6, 0, false).getId(), husk(level, -4.5, 0, false).getId(), pet(player, -1.6, 0).getId()};
		});
		String cast = castNow(world, Runes.SELF, Runes.PHOENIX_PYRE);
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(30);
		List<String> out = world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			ServerPlayer player = player(server);
			if (!player.hasEffect(MobEffects.REGENERATION) || !player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
				problems.add("the caster should have Regeneration and Fire Resistance (has " + player.getActiveEffectsMap().keySet() + ")");
			}
			if (player.getRemainingFireTicks() > 0) {
				problems.add("the caster shouldn't be burning");
			}
			LivingEntity near = get(server, ids[0]);
			if (!(lost(near) >= 1.5 && (near == null || burning(near)))) {
				problems.add("a husk 1.6 blocks away should be alight and burned (" + describe(near) + ")");
			}
			LivingEntity far = get(server, ids[1]);
			if (lost(far) > 0 || burning(far)) {
				problems.add("a husk 4.5 blocks away should be untouched (" + describe(far) + ")");
			}
			LivingEntity wolf = get(server, ids[2]);
			if (lost(wolf) > 0 || burning(wolf)) {
				problems.add("the caster's pet wolf mustn't burn (" + describe(wolf) + ")");
			}
			return problems;
		});
		// Past its 6 seconds, the flame is gone: a fresh husk beside the caster stays cool.
		context.waitTicks(100);
		int fresh = world.getServer().computeOnServer(server -> {
			LivingEntity near = get(server, ids[0]);
			if (near != null) {
				near.discard();
			}
			return husk(player(server).level(), 0, 1.6, false).getId();
		});
		context.waitTicks(30);
		String after = world.getServer().computeOnServer(server -> {
			LivingEntity husk = get(server, fresh);
			return lost(husk) > 0 || burning(husk) ? "after 6 seconds the pyre should be out (" + describe(husk) + ")" : null;
		});
		if (after != null) {
			out.add(after);
		}
		return out;
	}

	/**
	 * Hellmouth at a husk: a husk 2.5 blocks from the pit is dragged toward it, one 4 blocks off isn't; the husk in the
	 * pit's core burns for 2 a second, and the pit caves in on it for 4 (at least 8 in all).
	 */
	private static List<String> hellmouth(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			return new int[] {husk(level, 0, 5, true).getId(), husk(level, 2.5, 5, true).getId(), husk(level, -3.5, 5, true).getId()};
		});
		Vec3 pit = new Vec3(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 5.5);
		double[] before = world.getServer().computeOnServer(server -> new double[] {flat(get(server, ids[1]).position(), pit),
			get(server, ids[2]).getX(), get(server, ids[2]).getZ()});
		String cast = castNow(world, Runes.BEAM, Runes.HELLMOUTH);
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(30);
		List<String> out = world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			LivingEntity pulled = get(server, ids[1]);
			if (pulled == null || flat(pulled.position(), pit) > before[0] - 1.0) {
				problems.add(String.format("a husk %.1f blocks from the pit should be dragged at least a block toward it (%s)", before[0], describe(pulled)));
			}
			LivingEntity out4 = get(server, ids[2]);
			if (out4 == null || Math.abs(out4.getX() - before[1]) + Math.abs(out4.getZ() - before[2]) > 0.3 || lost(out4) > 0) {
				problems.add("a husk 4 blocks from the pit should be neither dragged nor hurt (" + describe(out4) + ")");
			}
			return problems;
		});
		context.waitTicks(45);
		String core = world.getServer().computeOnServer(server -> {
			LivingEntity husk = get(server, ids[0]);
			return lost(husk) >= 8 ? null : "the husk in the pit should burn for 2 a second and be caved in on for 4 (" + describe(husk) + ")";
		});
		if (core != null) {
			out.add(core);
		}
		return out;
	}

	/**
	 * Starfire at a husk with two more within 6 blocks: all three are hurt and alight; a husk 11 blocks off and the
	 * caster's pet wolf among them are left alone.
	 */
	private static List<String> starfire(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			return new int[] {husk(level, 0, 5, false).getId(), husk(level, 3, 6, false).getId(), husk(level, -3, 7, false).getId(),
				husk(level, 9, 12, false).getId(), pet(player, 2, 3).getId()};
		});
		String cast = castNow(world, Runes.BEAM, Runes.STARFIRE);
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(35);
		return world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				LivingEntity husk = get(server, ids[i]);
				if (husk != null && !(lost(husk) >= 1.5 && burning(husk))) {
					problems.add("husk " + (i + 1) + " of the three should be struck and alight (" + describe(husk) + ")");
				}
			}
			LivingEntity far = get(server, ids[3]);
			if (lost(far) > 0 || burning(far)) {
				problems.add("a husk 11 blocks off should be left alone (" + describe(far) + ")");
			}
			LivingEntity wolf = get(server, ids[4]);
			if (lost(wolf) > 0 || burning(wolf)) {
				problems.add("the caster's pet wolf should be left alone (" + describe(wolf) + ")");
			}
			return problems;
		});
	}

	/**
	 * Everburn at a husk, beside another set burning just as long by ordinary fire: the Everburned one loses clearly
	 * more; when its fire goes out it rekindles while the other stays out; then it goes out for good.
	 */
	private static List<String> everburn(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			return new int[] {husk(level, 0, 5, false).getId(), husk(level, -4, 5, false).getId()};
		});
		String cast = castNow(world, Runes.BEAM, Runes.EVERBURN);
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(2);
		String lit =world.getServer().computeOnServer(server -> {
			LivingEntity husk = get(server, ids[0]);
			LivingEntity control = get(server, ids[1]);
			if (!burning(husk)) {
				return "it should set the husk alight (" + describe(husk) + ")";
			}
			// The other husk burns just as long by ordinary fire.
			control.setRemainingFireTicks(husk.getRemainingFireTicks());
			return null;
		});
		if (lit != null) {
			return List.of(lit);
		}
		List<String> out = new ArrayList<>();
		context.waitTicks(94);
		String faster = world.getServer().computeOnServer(server -> {
			LivingEntity husk = get(server, ids[0]);
			LivingEntity control = get(server, ids[1]);
			return lost(husk) >= lost(control) + 3 ? null
				: "Everburn's fire should burn clearly faster than ordinary fire (Everburned: " + describe(husk) + "; ordinary: " + describe(control) + ")";
		});
		if (faster != null) {
			out.add(faster);
		}
		context.waitTicks(20);
		String rekindled = world.getServer().computeOnServer(server -> {
			LivingEntity husk = get(server, ids[0]);
			LivingEntity control = get(server, ids[1]);
			if (burning(control)) {
				return "the ordinary fire should be out by now (" + describe(control) + ")";
			}
			return burning(husk) ? null : "Everburn should rekindle once when it goes out (" + describe(husk) + ")";
		});
		if (rekindled != null) {
			out.add(rekindled);
		}
		context.waitTicks(85);
		String spent = world.getServer().computeOnServer(server -> {
			LivingEntity husk = get(server, ids[0]);
			return burning(husk) ? "Everburn should rekindle only once (" + describe(husk) + ")" : null;
		});
		if (spent != null) {
			out.add(spent);
		}
		return out;
	}

	/** Hurts {@code e} for {@code amount} from nowhere in particular, as any blow would. */
	private static void strike(MinecraftServer server, int id, float amount) {
		LivingEntity e = get(server, id);
		if (e != null) {
			Effects.readyToHurt(e);
			e.hurtServer(player(server).level(), player(server).level().damageSources().generic(), amount);
		}
	}

	/**
	 * Bloodboil at a husk: 3 damage, then each blow it takes brings 2 more fire damage (measured against the first hit,
	 * so a caster's affinity doesn't matter), never setting itself off again, and only five times; a boil 5 seconds old
	 * answers nothing.
	 */
	private static List<String> bloodboil(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = world.getServer().computeOnServer(server -> husk(player(server).level(), 0, 5, false).getId());
		String cast = castNow(world, Runes.BEAM, Runes.BLOODBOIL);
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(3);
		float[] start = world.getServer().computeOnServer(server -> new float[] {get(server, husk).getHealth()});
		float power = (20 - start[0]) / 3;
		List<String> out = new ArrayList<>();
		if (power < 0.8F || power > 1.5F) {
			out.add("Bloodboil should deal 3 damage (dealt " + (20 - start[0]) + ")");
			return out;
		}
		world.getServer().runOnServer(server -> strike(server, husk, 0.5F));
		context.waitTicks(3);
		float first = world.getServer().computeOnServer(server -> get(server, husk).getHealth());
		float burst = start[0] - first - 0.5F;
		if (Math.abs(burst - 2 * power) > 0.3F) {
			out.add(String.format("a blow on a boiling husk should bring %.1f more fire damage, just once (it brought %.1f)", 2 * power, burst));
		}
		for (int i = 0; i < 6; i++) {
			world.getServer().runOnServer(server -> strike(server, husk, 0.5F));
			context.waitTicks(3);
		}
		float last = world.getServer().computeOnServer(server -> {
			LivingEntity e = get(server, husk);
			return e == null ? 0.0F : e.getHealth();
		});
		float more = first - last - 6 * 0.5F;
		if (Math.abs(more - 4 * 2 * power) > 0.6F) {
			out.add(String.format("only five blows in all should boil over (the six after the first brought %.1f, not %.1f)", more, 4 * 2 * power));
		}
		// A boil runs out after 5 seconds.
		world.getServer().runOnServer(server -> {
			LivingEntity e = get(server, husk);
			if (e != null) {
				e.discard();
			}
		});
		int second = world.getServer().computeOnServer(server -> husk(player(server).level(), 0, 5, false).getId());
		context.waitTicks(3);
		cast = castNow(world, Runes.BEAM, Runes.BLOODBOIL);
		if (cast != null) {
			out.add("second cast: " + cast);
			return out;
		}
		context.waitTicks(110);
		float before = world.getServer().computeOnServer(server -> get(server, second).getHealth());
		world.getServer().runOnServer(server -> strike(server, second, 0.5F));
		context.waitTicks(3);
		float after = world.getServer().computeOnServer(server -> get(server, second).getHealth());
		if (before - after > 0.6F) {
			out.add(String.format("after 5 seconds a blow should bring no more fire (it cost %.1f)", before - after));
		}
		return out;
	}

	/**
	 * Conflagration at a husk: it's alight for 8 seconds and more and flares up; a burning husk 3 blocks away flares up
	 * too, burning longer; one beside it that isn't burning is left alone.
	 */
	private static List<String> conflagration(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			return new int[] {husk(level, 0, 5, false).getId(), husk(level, 3, 6, false).getId(), husk(level, -3, 6, false).getId()};
		});
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			get(server, ids[1]).igniteForSeconds(4);
			return cast(player, Runes.BEAM, Runes.CONFLAGRATION);
		});
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(12);
		return world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			LivingEntity target = get(server, ids[0]);
			if (target == null || target.getRemainingFireTicks() < 170 || lost(target) < 2.5F) {
				problems.add("the husk struck should be alight for 8 seconds and more, and flare up (" + describe(target) + ")");
			}
			LivingEntity burning = get(server, ids[1]);
			// It was lit for 80 ticks; 12 have gone, and the flare adds 40.
			if (burning == null || burning.getRemainingFireTicks() <= 80 || lost(burning) < 3.5F) {
				problems.add("a burning husk 3 blocks away should flare up for 3 and burn 2 seconds longer (" + describe(burning) + ")");
			}
			LivingEntity cool = get(server, ids[2]);
			if (lost(cool) > 0 || burning(cool)) {
				problems.add("a husk that wasn't burning should be left alone (" + describe(cool) + ")");
			}
			return problems;
		});
	}

	/**
	 * Monolith at a husk: a column of stone displays bursts up under it, it's hurt for 6 and thrown about 3 blocks up,
	 * no real block is placed, and the column is gone a while after its 4 seconds.
	 */
	private static List<String> monolith(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = world.getServer().computeOnServer(server -> husk(player(server).level(), 0, 5, true).getId());
		context.waitTicks(3);
		double start = world.getServer().computeOnServer(server -> get(server, husk).getY());
		Vec3 spot = new Vec3(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 5.5);
		String cast = castNow(world, Runes.BEAM, Runes.MONOLITH);
		if (cast != null) {
			return List.of(cast);
		}
		List<String> out = new ArrayList<>();
		double top = start;
		int stones = 0;
		for (int tick = 0; tick < 26; tick++) {
			context.waitTicks(1);
			double y = world.getServer().computeOnServer(server -> {
				LivingEntity e = get(server, husk);
				return e == null ? start : e.getY();
			});
			top = Math.max(top, y);
			if (tick == 8) {
				stones = world.getServer().computeOnServer(server -> player(server).level().getEntitiesOfClass(Display.BlockDisplay.class,
					new AABB(spot, spot).inflate(2.5, 4, 2.5), e -> e.entityTags().contains(STONE_TAG)).size());
			}
		}
		if (top - start < 2.3) {
			out.add(String.format("the husk should be thrown about 3 blocks up (rose %.2f)", top - start));
		}
		if (stones < 3) {
			out.add("a column of stone should stand under the husk (" + stones + " stone displays)");
		}
		String struck = world.getServer().computeOnServer(server -> {
			LivingEntity e = get(server, husk);
			ServerLevel level = player(server).level();
			for (int up = 0; up < 3; up++) {
				if (!level.getBlockState(BlockPos.containing(spot).above(up)).isAir()) {
					return "the column mustn't be real blocks (found " + level.getBlockState(BlockPos.containing(spot).above(up)) + ")";
				}
			}
			return lost(e) >= 5 ? null : "the husk should be hurt for 6 (" + describe(e) + ")";
		});
		if (struck != null) {
			out.add(struck);
		}
		context.waitTicks(100);
		int left = world.getServer().computeOnServer(server -> player(server).level().getEntitiesOfClass(Display.BlockDisplay.class,
			new AABB(spot, spot).inflate(3, 5, 3), e -> e.entityTags().contains(STONE_TAG)).size());
		if (left > 0) {
			out.add("the column should crumble away after 4 seconds (" + left + " stone displays left)");
		}
		return out;
	}

	/**
	 * Magnetize at a husk: a husk 3.5 blocks away is drawn to it and shocked when it touches; one 8 blocks away is left
	 * alone, and so is the magnet itself.
	 */
	private static List<String> magnetize(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			return new int[] {husk(level, 0, 5, true).getId(), husk(level, 3.5, 5, true).getId(), husk(level, -7.5, 5, true).getId()};
		});
		context.waitTicks(3);
		double[] before = world.getServer().computeOnServer(server -> new double[] {flat(get(server, ids[1]).position(), get(server, ids[0]).position()),
			get(server, ids[2]).getX(), get(server, ids[2]).getZ()});
		String cast = castNow(world, Runes.BEAM, Runes.MAGNETIZE);
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(60);
		return world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			LivingEntity magnet = get(server, ids[0]);
			LivingEntity drawn = get(server, ids[1]);
			if (magnet == null || drawn == null || flat(drawn.position(), magnet.position()) > before[0] - 1.5) {
				problems.add(String.format("a husk %.1f blocks away should be drawn to the magnet (%s)", before[0], describe(drawn)));
			}
			if (lost(drawn) < 2.5F) {
				problems.add("a husk touching the magnet should be shocked for 3 (" + describe(drawn) + ")");
			}
			LivingEntity far = get(server, ids[2]);
			if (far == null || Math.abs(far.getX() - before[1]) + Math.abs(far.getZ() - before[2]) > 0.3 || lost(far) > 0) {
				problems.add("a husk 8 blocks away should be left alone (" + describe(far) + ")");
			}
			if (lost(magnet) > 0) {
				problems.add("the magnet itself shouldn't be hurt (" + describe(magnet) + ")");
			}
			return problems;
		});
	}

	/**
	 * Sinkhole at a husk: a husk 2.2 blocks away is dragged to the middle, both are pinned (Slowness IV, no jumping) and
	 * then crushed for 5; one 6 blocks away is left alone; their jump comes back after.
	 */
	private static List<String> sinkhole(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			return new int[] {husk(level, 0, 5, true).getId(), husk(level, 2.2, 5, true).getId(), husk(level, 0, 11, true).getId()};
		});
		context.waitTicks(3);
		Vec3 hole = new Vec3(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 5.5);
		double before = world.getServer().computeOnServer(server -> flat(get(server, ids[1]).position(), hole));
		String cast = castNow(world, Runes.BEAM, Runes.SINKHOLE);
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(12);
		List<String> out = world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			LivingEntity dragged = get(server, ids[1]);
			if (dragged == null || flat(dragged.position(), hole) > before - 1.0) {
				problems.add(String.format("a husk %.1f blocks away should be dragged toward the middle (%s)", before, describe(dragged)));
			}
			for (int i = 0; i < 2; i++) {
				LivingEntity e = get(server, ids[i]);
				MobEffectInstance slow = e == null ? null : e.getEffect(MobEffects.SLOWNESS);
				if (slow == null || slow.getAmplifier() < 3 || e.getAttributeValue(Attributes.JUMP_STRENGTH) > 0.05) {
					problems.add("a husk in the sinkhole should be pinned: Slowness IV and no jumping (" + describe(e) + ", jump "
						+ (e == null ? "-" : e.getAttributeValue(Attributes.JUMP_STRENGTH)) + ")");
				}
			}
			return problems;
		});
		context.waitTicks(45);
		String crushed = world.getServer().computeOnServer(server -> {
			for (int i = 0; i < 2; i++) {
				LivingEntity e = get(server, ids[i]);
				if (lost(e) < 4.5F) {
					return "the pinned husks should be crushed for 5 (" + describe(e) + ")";
				}
			}
			LivingEntity far = get(server, ids[2]);
			if (lost(far) > 0 || far == null || far.hasEffect(MobEffects.SLOWNESS)) {
				return "a husk 6 blocks away should be left alone (" + describe(far) + ")";
			}
			return null;
		});
		if (crushed != null) {
			out.add(crushed);
		}
		context.waitTicks(10);
		String free = world.getServer().computeOnServer(server -> {
			for (int i = 0; i < 2; i++) {
				LivingEntity e = get(server, ids[i]);
				if (e != null && e.getAttributeValue(Attributes.JUMP_STRENGTH) < 0.3) {
					return "once the pin is over the husks should jump again (jump " + e.getAttributeValue(Attributes.JUMP_STRENGTH) + ")";
				}
			}
			return null;
		});
		if (free != null) {
			out.add(free);
		}
		return out;
	}

	/**
	 * Firestorm landing on a crowd of ten, more than the eight whose fire leaps on: every husk is set alight and
	 * takes its 4 (checked at once, before any fire has burned).
	 */
	private static List<String> firestormCrowd(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			List<Entity> crowd = new ArrayList<>();
			for (int i = 0; i < 10; i++) {
				crowd.add(husk(level, -9 + 2 * i, 6, false));
			}
			SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.BURST, Runes.FIRESTORM)).root().groups.getFirst().effects.getFirst();
			Effects.apply(new Cast(player), node, new Cast.Hit(crowd, crowd.getFirst().position(), new Vec3(0, 0, 1), player.position(), null, null, false));
			List<String> problems = new ArrayList<>();
			for (int i = 0; i < crowd.size(); i++) {
				LivingEntity t = (LivingEntity) crowd.get(i);
				if (!burning(t) || lost(t) < 3.5F) {
					problems.add("husk " + (i + 1) + " of ten should be set alight and take 4 (" + describe(t) + ")");
				}
			}
			return problems;
		});
	}

	/**
	 * Magma on three husks standing together: a pool opens under each, but where they overlap each husk burns once
	 * (2), not once for every pool it stands in.
	 */
	private static List<String> magmaBunch(ClientGameTestContext context, TestSingleplayerContext world) {
		List<Integer> ids = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			ServerLevel level = player.level();
			// With AI (but no speed), so they settle on the floor: magma burns only what stands on it.
			return List.of(husk(level, 0, 7, true).getId(), husk(level, 0.8, 7, true).getId(), husk(level, -0.8, 7, true).getId());
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			List<Entity> bunch = new ArrayList<>();
			for (int id : ids) {
				LivingEntity t = get(server, id);
				if (t != null) {
					bunch.add(t);
				}
			}
			SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.BURST, Runes.MAGMA)).root().groups.getFirst().effects.getFirst();
			Effects.apply(new Cast(player), node, new Cast.Hit(bunch, bunch.getFirst().position(), new Vec3(0, 0, 1), player.position(), null, null, false));
		});
		// Its first second burns 4 ticks in; the next comes 20 ticks after.
		context.waitTicks(10);
		return world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			for (int i = 0; i < ids.size(); i++) {
				LivingEntity t = get(server, ids.get(i));
				if (t == null || lost(t) < 1.5F || lost(t) > 3.5F) {
					problems.add("husk " + (i + 1) + " of three should burn for 2 once, not once for each pool it stands in (" + describe(t) + ")");
				}
			}
			return problems;
		});
	}

	/**
	 * Magma a husk opens under the caster: its later seconds are lingering damage, so a Shield the caster raises by
	 * hand just before one of them blocks it and can't parry it (no counter-burst hurts the husk).
	 */
	private static List<String> magmaLingers(ClientGameTestContext context, TestSingleplayerContext world) {
		long[] opened = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			Mob husk = husk(player.level(), 0, 7, false);
			SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.BEAM, Runes.MAGMA)).root().groups.getFirst().effects.getFirst();
			Effects.apply(new Cast(husk), node, new Cast.Hit(List.<Entity>of(player), player.position(), new Vec3(0, 0, -1), husk.position(), null, null, false));
			return new long[] {player.level().getGameTime(), husk.getId()};
		});
		// Its first second burns 4 ticks in and the next 20 ticks later: the Shield goes up just before that one.
		world.getServer().waitFor(server -> player(server).level().getGameTime() >= opened[0] + 20, 60);
		String raised = world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.SHIELD));
		if (raised != null) {
			return List.of(raised);
		}
		context.waitTicks(15);
		return world.getServer().computeOnServer(server -> {
			List<String> problems = new ArrayList<>();
			LivingEntity husk = get(server, (int) opened[1]);
			if (husk == null || lost(husk) > 0) {
				problems.add("a Shield raised against magma's next second should block it, not parry it with a counter-burst at the husk ("
					+ describe(husk) + ")");
			}
			return problems;
		});
	}
}
