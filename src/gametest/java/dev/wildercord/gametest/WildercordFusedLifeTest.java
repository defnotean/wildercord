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
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * In game, the fused effects of {@code cast.FusedLife}: each one cast for real and checked for what its
 * description promises, on a stone floor high in the air, with husks for enemies and tamed wolves for allies
 * (neither of which heals by itself; natural regeneration is off for the player).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFusedLifeTest implements FabricClientGameTest {
	/** How the showcase films each of these (see {@code WildercordShowcase}): the shape to cast it with and when to shoot. */
	static final List<FusedSample> SAMPLES = List.of(
		new FusedSample(Runes.ZEPHYR, Runes.SELF, 8, false),
		new FusedSample(Runes.CRIMSON_MIST, Runes.BOLT, 24, false),
		new FusedSample(Runes.SOULBOND, Runes.SELF, 5, false),
		new FusedSample(Runes.SECOND_WIND, Runes.SELF, 10, false),
		new FusedSample(Runes.TRANSFUSION, Runes.BOLT, 10, false),
		new FusedSample(Runes.LIFEBLOOM, Runes.SELF, 9, false),
		new FusedSample(Runes.BONESPUR, Runes.BOLT, 14, false),
		new FusedSample(Runes.SANGUINE_RITE, Runes.BEAM, 3, false));

	/** The floor: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.fused_life";

	/** One rune's checks: each adds what went wrong to {@code failures}. */
	private interface Check {
		void run(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule natural_health_regeneration false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			check(context, world, failures, "Zephyr", WildercordFusedLifeTest::zephyr);
			check(context, world, failures, "Crimson Mist", WildercordFusedLifeTest::crimsonMist);
			check(context, world, failures, "Soulbond", WildercordFusedLifeTest::soulbond);
			check(context, world, failures, "Soulbond (a killing blow)", WildercordFusedLifeTest::soulbondDeaths);
			check(context, world, failures, "Second Wind", WildercordFusedLifeTest::secondWind);
			check(context, world, failures, "Transfusion", WildercordFusedLifeTest::transfusion);
			check(context, world, failures, "Lifebloom", WildercordFusedLifeTest::lifebloom);
			check(context, world, failures, "Bonespur", WildercordFusedLifeTest::bonespur);
			check(context, world, failures, "Sanguine Rite", WildercordFusedLifeTest::sanguineRite);
			if (!failures.isEmpty()) {
				throw new AssertionError("The fused runes of life and blood went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void check(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures, String name, Check check) {
		List<String> found = new ArrayList<>();
		try {
			check.run(context, world, found);
		} catch (RuntimeException e) {
			found.add("threw " + e);
		}
		for (String failure : found) {
			failures.add(name + ": " + failure);
		}
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		context.waitTicks(20);
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(5);
	}

	// ------------------------------------------------------------------ the runes

	/** Zephyr, cast for real on Self: the caster and an ally close by run, leap and mend; an ally far off and an enemy close by don't. */
	private static void zephyr(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return new int[] {wolf(player, 2, 1).getId(), wolf(player, 9, 3).getId(), husk(player.level(), -2, 1).getId()};
		});
		context.waitTicks(3);
		String cast = world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.ZEPHYR));
		if (cast != null) {
			failures.add(cast);
			return;
		}
		context.waitTicks(3);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			ServerPlayer player = player(server);
			LivingEntity near = get(server, ids[0]);
			for (Holder<MobEffect> effect : List.of(MobEffects.SPEED, MobEffects.JUMP_BOOST, MobEffects.REGENERATION)) {
				expect(out, "the caster", player, effect, 0, 90, 130);
				expect(out, "an ally 2 blocks away", near, effect, 0, 90, 130);
				if (get(server, ids[1]).hasEffect(effect)) {
					out.add("an ally 9 blocks away shouldn't get " + effect.getRegisteredName());
				}
				if (get(server, ids[2]).hasEffect(effect)) {
					out.add("an enemy shouldn't get " + effect.getRegisteredName());
				}
			}
			return out;
		}));
	}

	/**
	 * Crimson Mist on Self: over its 5 seconds an enemy standing in it bleeds 1 a second (5 in all) and the
	 * hurt caster heals 1 a second; an enemy outside it is untouched.
	 */
	private static void crimsonMist(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.setHealth(10);
			Mob in = husk(player.level(), 1.5, 1.5);
			Mob out = husk(player.level(), 7, 0);
			land(player, Runes.SELF, Runes.CRIMSON_MIST, List.of(player), player.position(), true);
			return new int[] {in.getId(), out.getId()};
		});
		context.waitTicks(50);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			LivingEntity in = get(server, ids[0]);
			float lost = in.getMaxHealth() - in.getHealth();
			if (lost < 1.9F || lost > 4.1F) {
				out.add("an enemy in the mist should have bled about 3 in its first 2.5 seconds (lost " + lost + ")");
			}
			if (player(server).getHealth() < 11.9F) {
				out.add("the caster in the mist should be healing (at " + player(server).getHealth() + " of 20, from 10)");
			}
			return out;
		}));
		context.waitTicks(90);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			LivingEntity in = get(server, ids[0]);
			float lost = in.getMaxHealth() - in.getHealth();
			if (Math.abs(lost - 5) > 0.6F) {
				out.add("an enemy in the mist for all 5 seconds should have bled 5 (lost " + lost + ")");
			}
			float health = player(server).getHealth();
			if (Math.abs(health - 15) > 0.6F) {
				out.add("the caster should have healed half a heart a second for 5 seconds, 10 to 15 (at " + health + ")");
			}
			LivingEntity outside = get(server, ids[1]);
			if (outside.getHealth() < outside.getMaxHealth()) {
				out.add("an enemy outside the mist shouldn't be hurt (at " + outside.getHealth() + ")");
			}
			return out;
		}));
	}

	/**
	 * Soulbond, the caster and a wolf: wounds either takes are halved and the other takes the other half; the
	 * bond snaps beyond 16 blocks; and cast with no ally around it does nothing.
	 */
	private static void soulbond(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		int wolf = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Wolf w = wolf(player, 3, 0);
			// On Self it reaches for the nearest ally.
			land(player, Runes.SELF, Runes.SOULBOND, List.of(player), player.position(), true);
			return w.getId();
		});
		context.waitTicks(2);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			ServerPlayer player = player(server);
			LivingEntity w = get(server, wolf);
			hit(player, 10);
			near(out, "the caster, hit for 10", player, 15);
			near(out, "the bound wolf, when the caster was hit for 10", w, 35);
			hit(w, 8);
			near(out, "the bound wolf, hit for 8", w, 31);
			near(out, "the caster, when the wolf was hit for 8", player, 11);
			// Too far apart: the bond breaks within a few ticks.
			w.teleportTo(STAGE.getX() + 18.5, STAGE.getY(), STAGE.getZ() + 0.5);
			return out;
		}));
		context.waitTicks(10);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			ServerPlayer player = player(server);
			hit(player, 4);
			near(out, "the caster, hit for 4 with the bond broken by distance", player, 7);
			near(out, "the wolf 18 blocks away", get(server, wolf), 31);
			get(server, wolf).discard();
			stand(player);
			// Alone: nothing to bind to, so nothing is shared.
			land(player, Runes.SELF, Runes.SOULBOND, List.of(player), player.position(), true);
			hit(player, 4);
			near(out, "the caster, bound to nobody and hit for 4", player, 16);
			return out;
		}));
	}

	/**
	 * Soulbond and killing blows, between two husks (a husk's spell binds it to another monster): a blow whose
	 * half is survivable leaves the bound one standing; two bound souls both struck down at once both fall.
	 */
	private static void soulbondDeaths(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			Mob a = husk(level, -3, 4);
			Mob b = husk(level, 3, 4);
			land(a, Runes.TOUCH, Runes.SOULBOND, List.of(b), b.position(), false);
			return new int[] {a.getId(), b.getId()};
		});
		context.waitTicks(2);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			LivingEntity a = get(server, ids[0]);
			LivingEntity b = get(server, ids[1]);
			a.setHealth(6);
			hit(a, 10);
			if (!a.isAlive()) {
				out.add("a bound husk at 6 health hit for 10 should live, taking only 5");
			}
			near(out, "a bound husk at 6 health hit for 10", a, 1);
			near(out, "its partner, when it was hit for 10", b, 15);
			a.setHealth(8);
			b.setHealth(8);
			hit(a, 12);
			near(out, "a bound husk at 8 hit for 12", a, 2);
			near(out, "its partner at 8, when it was hit for 12", b, 2);
			hit(b, 12);
			if (a.isAlive() || b.isAlive()) {
				out.add("two bound husks both struck down at once should both die (" + a.getHealth() + " and " + b.getHealth() + " left)");
			}
			return out;
		}));
	}

	/**
	 * Second Wind: a small hit doesn't use it; the first killing blow leaves the ally at 4 health with
	 * Regeneration II; cast twice before it's spent it still saves only once; and once it has saved someone, a
	 * new one cast on them (at once, as a recast off cooldown would) doesn't take, so the next blow kills.
	 */
	private static void secondWind(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			land(player, Runes.SELF, Runes.SECOND_WIND, List.of(player), player.position(), true);
		});
		context.waitTicks(2);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			ServerPlayer player = player(server);
			hit(player, 5);
			near(out, "the caster, hit for 5", player, 15);
			hit(player, 100);
			if (!player.isAlive()) {
				out.add("the first killing blow should be survived");
				return out;
			}
			near(out, "the caster, after a killing blow", player, 4);
			expect(out, "the caster, after a killing blow", player, MobEffects.REGENERATION, 1, 60, 90);
			return out;
		}));
		world.getServer().runOnServer(server -> stand(player(server)));
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			Mob a = husk(level, -3, 4);
			Mob b = husk(level, 3, 4);
			// Twice before it's spent: it lasts longer, it doesn't save twice.
			land(a, Runes.TOUCH, Runes.SECOND_WIND, List.of(b), b.position(), false);
			land(a, Runes.TOUCH, Runes.SECOND_WIND, List.of(b), b.position(), false);
			return new int[] {a.getId(), b.getId()};
		});
		context.waitTicks(2);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			LivingEntity b = get(server, ids[1]);
			hit(b, 100);
			near(out, "a husk after its first killing blow", b, 4);
			hit(b, 100);
			if (b.isAlive()) {
				out.add("a Second Wind cast twice before it was spent should still save only once");
			}
			return out;
		}));
		int wolf = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Wolf w = wolf(player, 2, 3);
			land(player, Runes.TOUCH, Runes.SECOND_WIND, List.of(w), w.position(), false);
			return w.getId();
		});
		context.waitTicks(2);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			ServerPlayer player = player(server);
			LivingEntity w = get(server, wolf);
			hit(w, 100);
			near(out, "a wolf after its first killing blow", w, 4);
			// Recast at once, as soon as the cooldown allows: it won't take for a minute.
			land(player, Runes.TOUCH, Runes.SECOND_WIND, List.of(w), w.position(), false);
			hit(w, 100);
			if (w.isAlive()) {
				out.add("a Second Wind recast on a wolf it saved moments ago shouldn't take (it lived through a second killing blow)");
			}
			return out;
		}));
	}

	/**
	 * Transfusion onto a wolf: the caster gives up to 4 and the wolf heals twice that; the caster never goes
	 * below 2; with nothing to spare nothing happens; on Self it does nothing; and it gives no more than needed.
	 */
	private static void transfusion(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			ServerPlayer player = player(server);
			Wolf w = wolf(player, 2, 1);
			w.setHealth(10);
			land(player, Runes.TOUCH, Runes.TRANSFUSION, List.of(w), w.position(), false);
			near(out, "the caster, after giving to a wounded wolf", player, 16);
			near(out, "the wolf, given 4", w, 18);
			player.setHealth(5);
			land(player, Runes.TOUCH, Runes.TRANSFUSION, List.of(w), w.position(), false);
			near(out, "a caster at 5, who may only give 3", player, 2);
			near(out, "the wolf, given 3", w, 24);
			land(player, Runes.TOUCH, Runes.TRANSFUSION, List.of(w), w.position(), false);
			near(out, "a caster at 2, with nothing to spare", player, 2);
			near(out, "the wolf, when the caster had nothing to spare", w, 24);
			player.setHealth(12);
			land(player, Runes.SELF, Runes.TRANSFUSION, List.of(player), player.position(), true);
			near(out, "the caster, after Transfusion on Self", player, 12);
			player.setHealth(20);
			w.setHealth(39);
			land(player, Runes.TOUCH, Runes.TRANSFUSION, List.of(w), w.position(), false);
			near(out, "the caster, giving to a wolf 1 short of whole", player, 19.5);
			near(out, "the wolf 1 short of whole", w, 40);
			return out;
		}));
	}

	/**
	 * Lifebloom on a wolf: 4 at once, 1 a second for 5 seconds, then a burst of 3 for every ally within 3
	 * blocks of it (a second wolf beside it); not an enemy beside it, not an ally further off.
	 */
	private static void lifebloom(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Wolf target = wolf(player, 0, 6);
			Wolf beside = wolf(player, 1.5, 6);
			Wolf far = wolf(player, 6, 6);
			Mob enemy = husk(player.level(), -1.5, 6);
			for (LivingEntity e : List.of(target, beside, far, enemy)) {
				e.setHealth(10);
			}
			land(player, Runes.TOUCH, Runes.LIFEBLOOM, List.of(target), target.position(), false);
			return new int[] {target.getId(), beside.getId(), far.getId(), enemy.getId()};
		});
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			near(out, "the wolf, just after Lifebloom", get(server, ids[0]), 14);
			return out;
		}));
		context.waitTicks(50);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			near(out, "the wolf, 2.5 seconds into Lifebloom", get(server, ids[0]), 16);
			return out;
		}));
		context.waitTicks(70);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			near(out, "the wolf, after the bloom burst (14, 5 more over time, 3 from the burst)", get(server, ids[0]), 22);
			near(out, "a wolf beside it, healed by the burst", get(server, ids[1]), 13);
			near(out, "a wolf 6 blocks off, outside the burst", get(server, ids[2]), 10);
			near(out, "an enemy beside it", get(server, ids[3]), 10);
			return out;
		}));
	}

	/**
	 * Bonespur among seven husks: spurs under the four nearest the point (5 each, through their armour, then 1
	 * a second for 3 seconds); none under the two further in, nor the one outside 4 blocks.
	 */
	private static void bonespur(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			double[][] at = {{0, 8}, {1, 8}, {0, 9.5}, {-2, 8}, {3.2, 8}, {0, 11.4}, {6, 8}};
			int[] out = new int[at.length];
			List<Mob> husks = new ArrayList<>();
			for (int i = 0; i < at.length; i++) {
				Mob h = husk(level, at[i][0], at[i][1]);
				husks.add(h);
				out[i] = h.getId();
			}
			land(player, Runes.TOUCH, Runes.BONESPUR, List.of(husks.getFirst()), husks.getFirst().position(), false);
			return out;
		});
		context.waitTicks(16);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			for (int i = 0; i < ids.length; i++) {
				LivingEntity h = get(server, ids[i]);
				float lost = h.getMaxHealth() - h.getHealth();
				if (i < 4 && (lost < 4.5F || lost > 5.6F)) {
					out.add("husk " + i + " (among the 4 nearest) should take a spur of about 5 (lost " + lost + ")");
				} else if (i >= 4 && lost > 0) {
					out.add("husk " + i + " shouldn't be struck: only the 4 nearest within 4 blocks are (lost " + lost + ")");
				}
			}
			return out;
		}));
		context.waitTicks(70);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				LivingEntity h = get(server, ids[i]);
				float lost = h.getMaxHealth() - h.getHealth();
				if (lost < 7.4F || lost > 8.6F) {
					out.add("husk " + i + " should have bled 3 more over 3 seconds (lost " + lost + " in all, not about 8)");
				}
			}
			return out;
		}));
	}

	/**
	 * Sanguine Rite: the caster pays 3 for 12 through a husk's diamond armour; at 3 health it's refused and
	 * nothing is paid or dealt; on Self (nothing to strike) nothing is paid; and cast for real with a Beam.
	 */
	private static void sanguineRite(ClientGameTestContext context, TestSingleplayerContext world, List<String> failures) {
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			ServerPlayer player = player(server);
			Mob first = armoured(husk(player.level(), 2, 5));
			land(player, Runes.TOUCH, Runes.SANGUINE_RITE, List.of(first), first.position(), false);
			near(out, "the caster, after the rite", player, 17);
			near(out, "a husk in diamond armour, after the rite", first, 8);
			player.setHealth(3);
			Mob second = armoured(husk(player.level(), -2, 5));
			land(player, Runes.TOUCH, Runes.SANGUINE_RITE, List.of(second), second.position(), false);
			near(out, "a caster at 3, whose rite is refused", player, 3);
			near(out, "the husk, when the rite was refused", second, 20);
			player.setHealth(20);
			land(player, Runes.SELF, Runes.SANGUINE_RITE, List.of(player), player.position(), true);
			near(out, "the caster, after the rite on Self (nothing to strike)", player, 20);
			first.discard();
			second.discard();
			return out;
		}));
		int husk = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			return armoured(husk(player.level(), 0, 5)).getId();
		});
		context.waitTicks(3);
		String cast = world.getServer().computeOnServer(server -> cast(player(server), Runes.BEAM, Runes.SANGUINE_RITE));
		if (cast != null) {
			failures.add(cast);
			return;
		}
		context.waitTicks(3);
		failures.addAll(world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			near(out, "the caster, after casting the rite with a Beam", player(server), 17);
			LivingEntity h = get(server, husk);
			float lost = h == null || !h.isAlive() ? 20 : h.getMaxHealth() - h.getHealth();
			if (lost < 11) {
				out.add("the husk the Beam struck should take about 12 through its armour (lost " + lost + ")");
			}
			return out;
		}));
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static LivingEntity get(MinecraftServer server, int id) {
		return player(server).level().getEntity(id) instanceof LivingEntity living ? living : null;
	}

	/** A 41x41 stone floor with open air above it, an Echo Cord, every rune known, and plenty of mana. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 20) + " " + (y - 1) + " " + (z - 20) + " " + (x + 20) + " " + (y - 1) + " " + (z + 20) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 20) + " " + y + " " + (z - 20) + " " + (x + 20) + " " + (y + 8) + " " + (z + 20) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
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

	/** The caster at the middle of the floor, in survival, facing south (+Z) and a little down, whole and with nothing on them. */
	private static void stand(ServerPlayer player) {
		player.setGameMode(GameType.SURVIVAL);
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0);
		player.clearFire();
		player.getFoodData().setFoodLevel(20);
	}

	/** A husk (an enemy that neither burns by day nor heals) standing still, {@code dx} and {@code dz} from the middle of the floor. */
	private static Mob husk(ServerLevel level, double dx, double dz) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		level.addFreshEntity(husk);
		return husk;
	}

	private static Mob armoured(Mob mob) {
		mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
		mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
		mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
		mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
		return mob;
	}

	/** A wolf tamed by {@code owner} (an ally, with 40 health, that never heals by itself), standing still. */
	private static Wolf wolf(ServerPlayer owner, double dx, double dz) {
		ServerLevel level = owner.level();
		Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
		wolf.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		wolf.tame(owner);
		wolf.setNoAi(true);
		wolf.addTag(TAG);
		level.addFreshEntity(wolf);
		return wolf;
	}

	/** Lands {@code shape} + {@code rune} on {@code struck} at {@code point}, as {@code caster}'s spell (as the shape would). */
	private static void land(LivingEntity caster, RuneDef shape, RuneDef rune, List<? extends Entity> struck, Vec3 point, boolean self) {
		SpellCompiler.Compiled compiled = SpellCompiler.compile(List.of(shape, rune));
		SpellPlan.EffectNode node = compiled.root().groups.getFirst().effects.getFirst();
		Effects.apply(new Cast(caster), node, new Cast.Hit(new ArrayList<Entity>(struck), point, caster.getLookAngle(), caster.position(), null, null, self));
	}

	/** Casts spell 1, threaded with {@code runes}, as the player would. Returns what went wrong, or null. */
	private static String cast(ServerPlayer player, RuneDef... runes) {
		List<String> ids = Arrays.stream(runes).map(RuneDef::id).toList();
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids);
		if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
			return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
		}
		Spellbooks.setMana(player, Mana.max(player));
		Spellbooks.setReadyAt(player, 0, 0);
		player.removeAttached(WildercordAttachments.RHYTHM);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	/** A plain hit (no armour, no attacker), landing in full whatever hit it last. */
	private static void hit(LivingEntity e, float amount) {
		ServerLevel level = (ServerLevel) e.level();
		Effects.readyToHurt(e);
		e.hurtServer(level, level.damageSources().generic(), amount);
	}

	private static void near(List<String> out, String who, LivingEntity e, double health) {
		if (e == null) {
			out.add(who + " is gone");
		} else if (Math.abs(e.getHealth() - health) > 0.3) {
			out.add(who + " should be at " + health + " health (is at " + e.getHealth() + ")");
		}
	}

	private static void expect(List<String> out, String who, LivingEntity e, Holder<MobEffect> effect, int amplifier, int minTicks, int maxTicks) {
		MobEffectInstance instance = e == null ? null : e.getEffect(effect);
		if (instance == null) {
			out.add(who + " should have " + effect.getRegisteredName());
		} else if (instance.getAmplifier() != amplifier || instance.getDuration() < minTicks || instance.getDuration() > maxTicks) {
			out.add(who + " should have " + effect.getRegisteredName() + " " + (amplifier + 1) + " for " + minTicks + " to " + maxTicks + " ticks (has "
				+ (instance.getAmplifier() + 1) + " for " + instance.getDuration() + ")");
		}
	}
}
