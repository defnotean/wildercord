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
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * In game, the fused effects of {@code cast.FusedFrost}: each one cast for real (threaded on a Cord and
 * cast with {@link SpellCaster#cast}) at husks standing on a stone platform high in the air, or on the
 * caster, and checked for what its description promises: the numbers, the timings, who it touches and who
 * it leaves alone, a Cryostasis that can't be renewed while it holds, and two Geodes that don't trade
 * shards forever.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedFrostTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		// The storm well under way: its funnel of rings, snow whipping round.
		new FusedSample(Runes.BLIZZARD, Runes.BOLT, 18, false),
		// The ice lotus open at the caster's feet.
		new FusedSample(Runes.FROSTBLOOM, Runes.SELF, 6, false),
		// The violet-black ice closed on the husk, its dark spikes still out.
		new FusedSample(Runes.BLACK_ICE, Runes.BOLT, 10, false),
		// The sigil written out, and the husk that stood on it for a second frozen in a pillar of light.
		new FusedSample(Runes.RIME_SEAL, Runes.BOLT, 28, true),
		// The cocoon grown, its clock turning.
		new FusedSample(Runes.CRYOSTASIS, Runes.SELF, 10, false),
		// The frost jaws snapping shut.
		new FusedSample(Runes.FROSTBITE, Runes.BOLT, 7, false),
		// The cold closing in (on husks not yet cold: they're slowed, not frozen solid).
		new FusedSample(Runes.ABSOLUTE_ZERO, Runes.BOLT, 7, false),
		// Turned to stone, the clock stopped round it.
		new FusedSample(Runes.FOSSILIZE, Runes.BOLT, 70, false),
		// The amethyst up round the caster and the crystal cage closing.
		new FusedSample(Runes.GEODE, Runes.SELF, 6, false));

	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.fused_frost";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			check(failures, "Cryostasis", cryostasis(context, world));
			cleanup(context, world);
			check(failures, "Blizzard", blizzard(context, world));
			cleanup(context, world);
			check(failures, "Frostbloom", frostbloom(context, world));
			cleanup(context, world);
			check(failures, "Black Ice", blackIce(context, world));
			cleanup(context, world);
			check(failures, "Rime Seal", rimeSeal(context, world));
			cleanup(context, world);
			check(failures, "Frostbite", frostbite(context, world));
			cleanup(context, world);
			check(failures, "Absolute Zero", absoluteZero(context, world));
			cleanup(context, world);
			check(failures, "Fossilize", fossilize(context, world));
			cleanup(context, world);
			check(failures, "Geode", geode(context, world));
			cleanup(context, world);
			if (!failures.isEmpty()) {
				throw new AssertionError("The fused runes of frost went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void check(List<String> failures, String name, List<String> found) {
		found.forEach(f -> failures.add(name + ": " + f));
	}

	// ------------------------------------------------------------------ the runes

	/**
	 * Cryostasis on yourself: a cocoon of ice, held where you stand, nothing hurts you, 6 health back over
	 * the two seconds; casting it again while it holds doesn't make it last longer; afterwards blows land
	 * again and the ice is gone.
	 */
	private static List<String> cryostasis(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> {
			player.setHealth(10.0F);
			return husk(player, 3, 3);
		});
		String cast = on(world, player -> cast(player, Runes.SELF, Runes.CRYOSTASIS));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(3);
		String untouchable = on(world, player -> {
			// A shove: the ice holds them where they were sealed.
			player.setDeltaMovement(0.9, 0.35, 0.0);
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
			float before = player.getHealth();
			Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().mobAttack(mob(player, husk)), 4.0F);
			if (player.getHealth() < before) {
				return "a sealed ally shouldn't be hurt (" + before + " to " + player.getHealth() + ")";
			}
			if (!has(player, MobEffects.SLOWNESS, 6)) {
				return "a sealed ally should be held still (effects " + player.getActiveEffectsMap().keySet() + ")";
			}
			return null;
		});
		context.waitTicks(8);
		String held = on(world, player -> {
			Vec3 anchor = new Vec3(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5);
			double moved = player.position().subtract(anchor).horizontalDistance();
			if (moved > 0.35) {
				return "a sealed ally shouldn't move (moved " + moved + ")";
			}
			int ice = player.level().getEntitiesOfClass(Display.BlockDisplay.class, player.getBoundingBox().inflate(2)).size();
			if (ice < 2) {
				return "a sealed ally should be closed in ice (" + ice + " blocks of it)";
			}
			// Again while it holds: it mustn't renew.
			return cast(player, Runes.SELF, Runes.CRYOSTASIS);
		});
		context.waitTicks(35);
		String opened = on(world, player -> {
			if (player.getHealth() < 15.5F) {
				return "the seal should have healed 6 (at " + player.getHealth() + " from 10)";
			}
			int ice = player.level().getEntitiesOfClass(Display.BlockDisplay.class, player.getBoundingBox().inflate(2)).size();
			if (ice > 0) {
				return "the ice should be gone once the seal opens (" + ice + " blocks left)";
			}
			float before = player.getHealth();
			Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().mobAttack(mob(player, husk)), 4.0F);
			if (player.getHealth() >= before) {
				return "after two seconds blows should land again: casting it a second time mustn't renew it (" + before + " to " + player.getHealth() + ")";
			}
			return null;
		});
		return found(untouchable, held, opened);
	}

	/** Blizzard on a husk: for 4 seconds it's slowed (Slowness II) and bitten once a second; one 4.5 blocks off is left alone. */
	private static List<String> blizzard(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] husks = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 4.5, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.BLIZZARD));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(30);
		String early = on(world, player -> {
			Mob in = mob(player, husks[0]);
			if (in == null || !has(in, MobEffects.SLOWNESS, 1)) {
				return "a husk in the storm should be slowed (Slowness II)";
			}
			return in.getTicksFrozen() > 0 ? null : "a husk in the storm should be chilled";
		});
		context.waitTicks(45);
		String late = on(world, player -> {
			Mob in = mob(player, husks[0]);
			Mob out = mob(player, husks[1]);
			if (in == null || out == null) {
				return "the husks should still be there";
			}
			float taken = in.getMaxHealth() - in.getHealth();
			if (taken < 2.5F || taken > 5.0F) {
				return "a husk in the storm should take about 1 a second for 4 seconds (took " + taken + ")";
			}
			if (out.getHealth() < out.getMaxHealth() || out.hasEffect(MobEffects.SLOWNESS)) {
				return "a husk 4.5 blocks away should be left alone";
			}
			return null;
		});
		return found(early, late);
	}

	/** Frostbloom on yourself: Regeneration II; a husk that strikes you is frozen stiff (Slowness III), and nothing else happens to you. */
	private static List<String> frostbloom(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 1.5, 1.5));
		String cast = on(world, player -> cast(player, Runes.SELF, Runes.FROSTBLOOM));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(5);
		String result = on(world, player -> {
			if (!has(player, MobEffects.REGENERATION, 1)) {
				return "the ally should have Regeneration II";
			}
			Mob attacker = mob(player, husk);
			Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().mobAttack(attacker), 2.0F);
			if (!has(attacker, MobEffects.SLOWNESS, 2)) {
				return "a husk that strikes the ally should get Slowness III (effects " + attacker.getActiveEffectsMap().keySet() + ")";
			}
			return player.hasEffect(MobEffects.SLOWNESS) ? "the ally shouldn't be slowed by its own Frostbloom" : null;
		});
		return found(result);
	}

	/**
	 * Black Ice on a husk: frozen, then Weakness II; killed within 5 seconds it shatters, 4 damage to an
	 * enemy beside it and none to one 5 blocks off.
	 */
	private static List<String> blackIce(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] husks = on(world, player -> new int[] {husk(player, 0, 5), husk(player, 1.6, 5), husk(player, -5, 5)});
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.BLACK_ICE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(5);
		String frozen = on(world, player -> {
			Mob target = mob(player, husks[0]);
			if (target == null || !has(target, MobEffects.SLOWNESS, 6)) {
				return "the husk should be frozen";
			}
			Mob near = mob(player, husks[1]);
			return near != null && near.getHealth() >= near.getMaxHealth() && !near.hasEffect(MobEffects.WEAKNESS) ? null
				: "only the husk struck should be touched at first";
		});
		context.waitTicks(35);
		String weak = on(world, player -> {
			Mob target = mob(player, husks[0]);
			if (target == null) {
				return "the husk should still be there";
			}
			MobEffectInstance weakness = target.getEffect(MobEffects.WEAKNESS);
			if (weakness == null || weakness.getAmplifier() != 1) {
				return "after the freeze the husk should be left with Weakness II (has " + weakness + ")";
			}
			target.kill(player.level());
			return null;
		});
		context.waitTicks(6);
		String shattered = on(world, player -> {
			Mob near = mob(player, husks[1]);
			Mob far = mob(player, husks[2]);
			if (near == null || far == null) {
				return "the other husks should still be there";
			}
			float taken = near.getMaxHealth() - near.getHealth();
			if (taken < 3.0F || taken > 5.0F) {
				return "the shatter should deal 4 to the husk beside it (took " + taken + ")";
			}
			return far.getHealth() < far.getMaxHealth() ? "the shatter shouldn't reach a husk 5 blocks away" : null;
		});
		return found(frozen, weak, shattered);
	}

	/**
	 * Rime Seal under a husk: after standing on it a second it freezes and takes 3, once; another stepping
	 * on later is frozen in turn; the first isn't frozen again.
	 */
	private static List<String> rimeSeal(ClientGameTestContext context, TestSingleplayerContext world) {
		int first = on(world, player -> husk(player, 0, 5));
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.RIME_SEAL));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(10);
		String waiting = on(world, player -> {
			Mob husk = mob(player, first);
			return husk != null && husk.getHealth() >= husk.getMaxHealth() && !has(husk, MobEffects.SLOWNESS, 6) ? null
				: "the seal should wait a second before it freezes";
		});
		context.waitTicks(20);
		float[] after = {0};
		int[] second = {0};
		String sealed = on(world, player -> {
			Mob husk = mob(player, first);
			if (husk == null) {
				return "the husk should still be there";
			}
			float taken = husk.getMaxHealth() - husk.getHealth();
			if (!has(husk, MobEffects.SLOWNESS, 6) || taken < 2.5F || taken > 4.0F) {
				return "after a second on the seal the husk should be frozen and take 3 (took " + taken + ", effects "
					+ husk.getActiveEffectsMap().keySet() + ")";
			}
			after[0] = husk.getHealth();
			second[0] = husk(player, 0.8, 5);
			return null;
		});
		context.waitTicks(40);
		String another = on(world, player -> {
			Mob husk = mob(player, second[0]);
			if (husk == null) {
				return "the second husk should still be there";
			}
			return has(husk, MobEffects.SLOWNESS, 6) && husk.getHealth() < husk.getMaxHealth() ? null
				: "a husk stepping onto the seal later should be frozen in turn";
		});
		context.waitTicks(30);
		String once = on(world, player -> {
			Mob husk = mob(player, first);
			if (husk == null) {
				return "the husk should still be there";
			}
			// (Frozen through, it may take a point of vanilla freezing; not another 3.)
			return after[0] - husk.getHealth() < 2.0F ? null : "the seal should take each enemy only once";
		});
		return found(waiting, sealed, another, once);
	}

	/** Frostbite on a husk: 3 at once, then Slowness I, II and III as the cold sets in, 1 a second, and frozen at the end. */
	private static List<String> frostbite(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 0, 5));
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.FROSTBITE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(4);
		String bite = on(world, player -> {
			Mob t = mob(player, husk);
			float taken = t == null ? 0 : t.getMaxHealth() - t.getHealth();
			return taken >= 2.5F && taken <= 4.0F && slowness(t) == 0 ? null : "the bite should deal 3 and slow (Slowness I) (took " + taken + ")";
		});
		context.waitTicks(46);
		String second = on(world, player -> slowness(mob(player, husk)) == 1 ? null
			: "by the middle the cold should be at Slowness II (at " + slowness(mob(player, husk)) + ")");
		context.waitTicks(38);
		String third = on(world, player -> slowness(mob(player, husk)) == 2 ? null
			: "near the end the cold should be at Slowness III (at " + slowness(mob(player, husk)) + ")");
		context.waitTicks(18);
		String end = on(world, player -> {
			Mob t = mob(player, husk);
			if (t == null) {
				return "the husk should still be there";
			}
			float taken = t.getMaxHealth() - t.getHealth();
			if (taken < 6.5F || taken > 9.5F) {
				return "3 and then 1 a second for 5 seconds should come to about 8 (took " + taken + ")";
			}
			return slowness(t) >= 6 ? null : "at the end the husk should freeze (Slowness at " + slowness(t) + ")";
		});
		return found(bite, second, third, end);
	}

	/** Absolute Zero: a husk not yet cold only gets Slowness IV; one already slowed freezes solid and takes 7. */
	private static List<String> absoluteZero(ClientGameTestContext context, TestSingleplayerContext world) {
		int warm = on(world, player -> husk(player, 0, 5));
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.ABSOLUTE_ZERO));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(4);
		String slowed = on(world, player -> {
			Mob t = mob(player, warm);
			if (t == null) {
				return "the husk should still be there";
			}
			if (slowness(t) != 3) {
				return "a husk that wasn't cold should get Slowness IV and no more (Slowness at " + slowness(t) + ")";
			}
			return t.getHealth() < t.getMaxHealth() ? "a husk that wasn't cold shouldn't be hurt" : null;
		});
		cleanup(context, world);
		int cold = on(world, player -> {
			int id = husk(player, 0, 5);
			mob(player, id).addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0));
			return id;
		});
		cast = on(world, player -> cast(player, Runes.BEAM, Runes.ABSOLUTE_ZERO));
		if (cast != null) {
			return found(slowed, cast);
		}
		context.waitTicks(4);
		String solid = on(world, player -> {
			Mob t = mob(player, cold);
			if (t == null) {
				return "the cold husk should still be there";
			}
			float taken = t.getMaxHealth() - t.getHealth();
			if (slowness(t) < 6 || taken < 5.5F || taken > 8.0F) {
				return "a husk already slowed should freeze solid and take 7 (took " + taken + ", Slowness at " + slowness(t) + ")";
			}
			return null;
		});
		return found(slowed, solid);
	}

	/** Fossilize on a husk: Slowness I, II, III a second each, then stone (held) with no damage yet, then cracked for 6. */
	private static List<String> fossilize(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 0, 5));
		String cast = on(world, player -> cast(player, Runes.BEAM, Runes.FOSSILIZE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(8);
		String first = on(world, player -> slowness(mob(player, husk)) == 0 ? null : "first Slowness I (at " + slowness(mob(player, husk)) + ")");
		context.waitTicks(22);
		String second = on(world, player -> slowness(mob(player, husk)) == 1 ? null : "then Slowness II (at " + slowness(mob(player, husk)) + ")");
		context.waitTicks(20);
		String third = on(world, player -> slowness(mob(player, husk)) == 2 ? null : "then Slowness III (at " + slowness(mob(player, husk)) + ")");
		context.waitTicks(22);
		String stone = on(world, player -> {
			Mob t = mob(player, husk);
			if (t == null) {
				return "the husk should still be there";
			}
			if (slowness(t) < 6) {
				return "after 3 seconds it should be stone, held (Slowness at " + slowness(t) + ")";
			}
			return t.getHealth() < t.getMaxHealth() ? "the stone shouldn't crack until the hold is over" : null;
		});
		context.waitTicks(40);
		String cracked = on(world, player -> {
			Mob t = mob(player, husk);
			if (t == null) {
				return "the husk should still be there";
			}
			float taken = t.getMaxHealth() - t.getHealth();
			return taken >= 5.5F && taken <= 7.0F ? null : "the stone should crack for 6 (took " + taken + ")";
		});
		return found(first, second, third, stone, cracked);
	}

	/**
	 * Geode on yourself: Resistance II, and a husk that strikes you is cut for 2. Then a husk with a
	 * Geode of its own: striking it cuts you, and your Geode doesn't answer its shards (no endless trade).
	 */
	private static List<String> geode(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = on(world, player -> husk(player, 1.5, 1.5));
		String cast = on(world, player -> cast(player, Runes.SELF, Runes.GEODE));
		if (cast != null) {
			return List.of(cast);
		}
		context.waitTicks(5);
		String cut = on(world, player -> {
			if (!has(player, MobEffects.RESISTANCE, 1)) {
				return "the ally should have Resistance II";
			}
			Mob attacker = mob(player, husk);
			Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().mobAttack(attacker), 2.0F);
			float taken = attacker.getMaxHealth() - attacker.getHealth();
			return taken >= 1.5F && taken <= 2.5F ? null : "a husk that strikes the ally should be cut for 2 (took " + taken + ")";
		});
		String trade = on(world, player -> {
			// The husk gets a Geode of its own, cast by itself.
			Mob other = mob(player, husk);
			other.heal(100);
			Effects.apply(new Cast(other), SpellCompiler.compile(List.of(Runes.SELF, Runes.GEODE)).root().groups.getFirst().effects.getFirst(),
				new Cast.Hit(List.<Entity>of(other), other.position(), other.getLookAngle(), other.position(), null, null, true));
			float mine = player.getHealth();
			float theirs = other.getHealth();
			Effects.readyToHurt(other);
			other.hurtServer(player.level(), player.level().damageSources().playerAttack(player), 2.0F);
			float lost = mine - player.getHealth();
			float dealt = theirs - other.getHealth();
			if (lost <= 0) {
				return "striking a husk with a Geode should cut the striker";
			}
			if (lost > 2.5F || dealt > 2.5F || !player.isAlive() || !other.isAlive()) {
				return "two Geodes mustn't trade shards back and forth (the player lost " + lost + ", the husk " + dealt + ")";
			}
			return null;
		});
		return found(cut, trade);
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static List<String> found(String... results) {
		List<String> out = new ArrayList<>();
		for (String result : results) {
			if (result != null) {
				out.add(result);
			}
		}
		return out;
	}

	private static boolean has(LivingEntity e, Holder<MobEffect> effect, int amplifier) {
		MobEffectInstance instance = e == null ? null : e.getEffect(effect);
		return instance != null && instance.getAmplifier() >= amplifier;
	}

	/** The amplifier of a creature's Slowness, or -1 without it. */
	private static int slowness(LivingEntity e) {
		MobEffectInstance instance = e == null ? null : e.getEffect(MobEffects.SLOWNESS);
		return instance == null ? -1 : instance.getAmplifier();
	}

	/** A 21x21 stone floor with open air above it, an Echo Cord, every rune known. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 1) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 10) + " " + (x + 10) + " " + (y + 8) + " " + (z + 10) + " minecraft:air");
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
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.setTicksFrozen(0);
	}

	/** A husk (no AI, so it stands still) {@code dx}, {@code dz} from the middle of the platform, facing the caster. */
	private static int husk(ServerPlayer player, double dx, double dz) {
		ServerLevel level = player.level();
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		level.addFreshEntity(husk);
		return husk.getId();
	}

	private static Mob mob(ServerPlayer player, int id) {
		return player.level().getEntity(id) instanceof Mob mob && mob.isAlive() ? mob : null;
	}

	/** Threads {@code shape} and {@code effect} into spell 1 and casts it at once, full mana and no cooldown. Returns what went wrong, or null. */
	private static String cast(ServerPlayer player, RuneDef shape, RuneDef effect) {
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
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(20);
	}
}
