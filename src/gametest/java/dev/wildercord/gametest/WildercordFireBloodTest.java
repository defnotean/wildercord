package dev.wildercord.gametest;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fire and blood after the spell-feel pass, in a real game: what changed and what was fixed, each cast at husks (or a blaze,
 * or a tamed wolf) on a stone platform high in the air. A crowd takes one Explode blast, not four; a second Cinderheart never
 * stacks on the first; Blood Thread ties four and no more; Rend lets a blood spell through a blaze's resistance; Ember tops up a
 * burn; Kindling's fifth stack leaves its neighbours hurt; Gash weeps; Dismantle's last slash is deeper from behind; Cleave sweeps
 * on; Steam blinds beside its target; Overdrive hits harder as it weakens you; Transfusion heals threefold; Sunscorch makes its
 * target glow; Soulfire hurts a blaze; Lifesteal keeps feeding on what anyone does to its target; Meteor hits once, not per
 * blast; a primed husk that dies goes off at once.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFireBloodTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.fire_blood";

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
				new Object[] {"Explode on a crowd", (Check) WildercordFireBloodTest::explodeCrowd},
				new Object[] {"Meteor on a crowd", (Check) WildercordFireBloodTest::meteorCrowd},
				new Object[] {"Primer dies primed", (Check) WildercordFireBloodTest::primerDies},
				new Object[] {"Blood Thread caps", (Check) WildercordFireBloodTest::bloodThread},
				new Object[] {"Rend tears resistance", (Check) WildercordFireBloodTest::rend},
				new Object[] {"Ember feeds", (Check) WildercordFireBloodTest::ember},
				new Object[] {"Kindling chains", (Check) WildercordFireBloodTest::kindling},
				new Object[] {"Gash weeps", (Check) WildercordFireBloodTest::gash},
				new Object[] {"Dismantle backstab", (Check) WildercordFireBloodTest::dismantle},
				new Object[] {"Cleave sweeps", (Check) WildercordFireBloodTest::cleave},
				new Object[] {"Steam cloud", (Check) WildercordFireBloodTest::steam},
				new Object[] {"Overdrive pain", (Check) WildercordFireBloodTest::overdrive},
				new Object[] {"Transfusion", (Check) WildercordFireBloodTest::transfusion},
				new Object[] {"Sunscorch dazzle", (Check) WildercordFireBloodTest::sunscorch},
				new Object[] {"Soulfire on the fire-proof", (Check) WildercordFireBloodTest::soulfire},
				new Object[] {"Lifesteal siphon", (Check) WildercordFireBloodTest::lifesteal},
				// Last: its aura burns on for twelve seconds, and would hurt whatever stood near the caster after it.
				new Object[] {"Cinderheart never stacks", (Check) WildercordFireBloodTest::cinderheart});
			for (Object[] check : checks) {
				String failure;
				try {
					failure = ((Check) check[1]).run(context, world);
				} finally {
					cleanup(context, world);
				}
				if (failure != null) {
					failures.add(check[0] + ": " + failure);
				}
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("Fire and blood went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

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

	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.getFoodData().setFoodLevel(20);
	}

	private static Mob spawn(ServerLevel level, EntityType<? extends Mob> type, double dx, double dz, float yaw) {
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new IllegalStateException("couldn't make a " + type);
		}
		mob.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, yaw, 0);
		mob.setNoAi(true);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		return mob;
	}

	/** A husk in front of the caster (facing the caster unless {@code yaw} says otherwise). */
	private static Mob husk(ServerLevel level, double dx, double dz) {
		return spawn(level, EntityTypes.HUSK, dx, dz, 180);
	}

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

	private static LivingEntity living(MinecraftServer server, int id) {
		return player(server).level().getEntity(id) instanceof LivingEntity e ? e : null;
	}

	private static float lost(LivingEntity e) {
		return e == null ? 999 : e.getMaxHealth() - e.getHealth();
	}

	private static String health(LivingEntity e) {
		return e == null ? "gone" : e.getHealth() + "/" + e.getMaxHealth();
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			stand(player);
		});
		context.waitTicks(20);
	}

	/** Spawns {@code n} husks bunched about 2.5 blocks in front of the caster, and returns their ids. */
	private static int[] pack(MinecraftServer server, int n) {
		int[] ids = new int[n];
		double[][] spot = {{0, 2.5}, {0.6, 2.5}, {-0.6, 2.5}, {0, 3.2}, {0.6, 3.2}, {-0.6, 3.2}};
		for (int i = 0; i < n; i++) {
			ids[i] = husk(player(server).level(), spot[i][0], spot[i][1]).getId();
		}
		return ids;
	}

	// ------------------------------------------------------------------ blasts

	/** Burst + Explode on four bunched husks: each takes one blast (12 at most), not four. */
	private static String explodeCrowd(ClientGameTestContext context, TestSingleplayerContext world) {
		int[][] ids = new int[1][];
		String cast = world.getServer().computeOnServer(server -> {
			ids[0] = pack(server, 4);
			return cast(player(server), Runes.BURST, Runes.EXPLODE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(10);
		return world.getServer().computeOnServer(server -> {
			for (int id : ids[0]) {
				LivingEntity husk = living(server, id);
				float lost = lost(husk);
				if (lost < 5 || lost > 14) {
					return "each husk of a crowd should take one blast, 5 to 14, never several (" + health(husk) + ")";
				}
			}
			return null;
		});
	}

	/** Burst + Meteor on three bunched husks: 12 at the centre once the rock lands, not one meteor each. */
	private static String meteorCrowd(ClientGameTestContext context, TestSingleplayerContext world) {
		int[][] ids = new int[1][];
		String cast = world.getServer().computeOnServer(server -> {
			ids[0] = pack(server, 3);
			return cast(player(server), Runes.BURST, Runes.METEOR);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(12);
		String early = world.getServer().computeOnServer(server -> {
			for (int id : ids[0]) {
				if (lost(living(server, id)) > 0) {
					return "the meteor should still be falling after half a second (" + health(living(server, id)) + ")";
				}
			}
			return null;
		});
		if (early != null) {
			return early;
		}
		context.waitTicks(20);
		return world.getServer().computeOnServer(server -> {
			for (int id : ids[0]) {
				LivingEntity husk = living(server, id);
				float lost = lost(husk);
				if (lost < 7 || lost > 15) {
					return "each husk should take one meteor, 7 to 15 (" + health(husk) + ")";
				}
			}
			return null;
		});
	}

	/** A primed husk that is killed goes off at once, hurting the one beside it long before the fuse is out. */
	private static String primerDies(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = new int[2];
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			id[0] = husk(level, 0, 5).getId();
			id[1] = husk(level, 1.5, 5).getId();
			return cast(player(server), Runes.BEAM, Runes.PRIMER);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(6);
		world.getServer().runOnServer(server -> {
			LivingEntity primed = living(server, id[0]);
			if (primed != null) {
				primed.kill(player(server).level());
			}
		});
		context.waitTicks(8);
		return world.getServer().computeOnServer(server -> {
			LivingEntity beside = living(server, id[1]);
			return lost(beside) >= 4 ? null : "the primed husk's death should set the bomb off at once, well before 2 seconds (" + health(beside) + ")";
		});
	}

	// ------------------------------------------------------------------ Cinderheart

	/** Casting Cinderheart twice is one aura: the second finds the coals still hot. */
	private static String cinderheart(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			// Its lockout is per creature and lasts 24 s: a fresh player state, so an earlier test never spoils this one.
			ServerPlayer player = player(server);
			id[0] = husk(player.level(), 0, 2).getId();
			String first = cast(player, Runes.SELF, Runes.CINDERHEART);
			return first;
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(8);
		String second = world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.CINDERHEART));
		if (second != null) {
			return second;
		}
		context.waitTicks(60);
		return world.getServer().computeOnServer(server -> {
			LivingEntity husk = living(server, id[0]);
			float lost = lost(husk);
			// One aura: four pulses of 3 by now, and a little burn. Two would have taken twice that.
			if (husk == null || !husk.isAlive() || lost < 6 || lost > 19) {
				return "one aura should have dealt about 12 to 16 by now, not two auras' worth (" + health(husk) + ")";
			}
			return null;
		});
	}

	// ------------------------------------------------------------------ Blood Thread

	/** A Nova of Blood Thread over six husks ties four of them; a hurt to one is shared with exactly three. */
	private static String bloodThread(ClientGameTestContext context, TestSingleplayerContext world) {
		int[][] ids = new int[1][];
		String cast = world.getServer().computeOnServer(server -> {
			ids[0] = pack(server, 6);
			return cast(player(server), Runes.BURST, Runes.BLOOD_THREAD);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			LivingEntity first = living(server, ids[0][0]);
			first.hurtServer(level, level.damageSources().generic(), 5.0F);
		});
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			int hurt = 0;
			for (int id : ids[0]) {
				LivingEntity husk = living(server, id);
				if (lost(husk) > 0) {
					hurt++;
				}
			}
			// The one struck, and the three it shares with: four, however many the Nova caught.
			return hurt == 4 ? null : "a thread should tie four creatures at most, so five damage shared makes four hurt, not " + hurt;
		});
	}

	// ------------------------------------------------------------------ Rend, Ember, Kindling

	/** A blaze resists blood by half; after a Rend it takes a Leech in full. */
	private static String rend(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = spawn(player(server).level(), EntityTypes.BLAZE, 0, 5, 180).getId();
			return cast(player(server), Runes.BEAM, Runes.LEECH);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		float[] resisted = {0};
		world.getServer().runOnServer(server -> {
			resisted[0] = lost(living(server, id[0]));
			living(server, id[0]).setHealth(living(server, id[0]).getMaxHealth());
		});
		String rend = world.getServer().computeOnServer(server -> cast(player(server), Runes.BEAM, Runes.REND));
		if (rend != null) {
			return rend;
		}
		context.waitTicks(5);
		String leech = world.getServer().computeOnServer(server -> cast(player(server), Runes.BEAM, Runes.LEECH));
		if (leech != null) {
			return leech;
		}
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			float full = lost(living(server, id[0]));
			if (resisted[0] < 0.5F || resisted[0] > 2.2F) {
				return "a blaze should resist blood by half, 1.5 of a Leech's 3 (" + resisted[0] + ")";
			}
			return full >= 2.6F && full <= 3.6F ? null : "after a Rend the blaze should take the Leech in full, 3 (" + full + ")";
		});
	}

	/** Ember on something already burning adds 3 seconds instead of starting over. */
	private static String ember(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			Mob husk = husk(player(server).level(), 0, 4);
			husk.setRemainingFireTicks(100);
			id[0] = husk.getId();
			return cast(player(server), Runes.BEAM, Runes.EMBER);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(1);
		return world.getServer().computeOnServer(server -> {
			int ticks = living(server, id[0]).getRemainingFireTicks();
			return ticks >= 150 ? null : "Ember should add 3 seconds to a burn of 5, not restart it at 3 (" + ticks + " ticks left)";
		});
	}

	/** Five Kindlings on one husk: it dies of the fifth, and the husk beside it takes the burst. */
	private static String kindling(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = new int[2];
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			id[0] = husk(level, 0, 4).getId();
			id[1] = husk(level, 1.5, 4).getId();
			return null;
		});
		if (cast != null) {
			return cast;
		}
		for (int i = 0; i < 5; i++) {
			String fail = world.getServer().computeOnServer(server -> cast(player(server), Runes.BEAM, Runes.KINDLING));
			if (fail != null) {
				return fail;
			}
			context.waitTicks(4);
		}
		return world.getServer().computeOnServer(server -> {
			LivingEntity beside = living(server, id[1]);
			float lost = lost(beside);
			return lost >= 12 ? null : "the fifth stack's burst of 14 should reach the husk beside it (" + health(beside) + ")";
		});
	}

	// ------------------------------------------------------------------ wounds

	/** Gash: it weeps 0.4 plus 1% of its bearer's 20 health, about 0.6, every second for 8 s (six weeps in the last 130 ticks). */
	private static String gash(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = new int[2];
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 4).getId();
			// A husk beside it that nothing touches: whatever the world does to a husk in 8 seconds, it does to this one.
			id[1] = husk(player(server).level(), 4, 4).getId();
			return cast(player(server), Runes.BEAM, Runes.GASH);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(48);
		float[] mid = {0};
		world.getServer().runOnServer(server -> mid[0] = lost(living(server, id[0])));
		if (mid[0] < 3) {
			return "a Gash should hurt at once (" + mid[0] + ")";
		}
		context.waitTicks(127);
		return world.getServer().computeOnServer(server -> {
			float weeping = lost(living(server, id[0])) - mid[0];
			return weeping >= 3.0F && weeping <= 4.4F ? null : "a Gash should weep about 0.6 a second (six weeps in 130 ticks, 3.6): " + weeping;
		});
	}

	/** Dismantle: 9 from the front, 12 when the last slash lands on a back turned. */
	private static String dismantle(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = new int[2];
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			id[0] = husk(level, 0, 4).getId();
			id[1] = spawn(level, EntityTypes.HUSK, 3.0, 4, 0).getId();
			return cast(player(server), Runes.BEAM, Runes.DISMANTLE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(8);
		float[] front = {0};
		world.getServer().runOnServer(server -> front[0] = lost(living(server, id[0])));
		// Turn to the husk with its back to us.
		String back = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.teleportTo(player.level(), STAGE.getX() + 3.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
			return cast(player, Runes.BEAM, Runes.DISMANTLE);
		});
		if (back != null) {
			return back;
		}
		context.waitTicks(8);
		return world.getServer().computeOnServer(server -> {
			float behind = lost(living(server, id[1]));
			if (front[0] < 8.5F || front[0] > 9.6F) {
				return "from the front a Dismantle should deal 9 (" + front[0] + ")";
			}
			return behind >= 11.4F && behind <= 12.6F ? null : "from behind its last slash is twice as deep: 12 (" + behind + ")";
		});
	}

	/** Cleave: 8 on a husk (6 + 10% of 20), and half of that on the one beside it. */
	private static String cleave(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = new int[2];
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			id[0] = husk(level, 0, 4).getId();
			id[1] = husk(level, 1.5, 4).getId();
			return cast(player(server), Runes.BEAM, Runes.CLEAVE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			float first = lost(living(server, id[0]));
			float beside = lost(living(server, id[1]));
			if (first < 6.5F || first > 9.5F) {
				return "a Cleave on a husk should deal 6 + 10% of its 20 health, about 8 (" + first + ")";
			}
			return beside >= 3 && beside <= 5 ? null : "the axe should swing on to the husk beside it for half, about 4 (" + beside + ")";
		});
	}

	// ------------------------------------------------------------------ control and buffs

	/** Steam blinds the husk it strikes and the one beside it (its cloud). */
	private static String steam(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = new int[2];
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			id[0] = husk(level, 0, 5).getId();
			id[1] = husk(level, 1.2, 5).getId();
			return cast(player(server), Runes.BEAM, Runes.STEAM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(10);
		return world.getServer().computeOnServer(server -> {
			LivingEntity beside = living(server, id[1]);
			return beside != null && beside.hasEffect(MobEffects.BLINDNESS) ? null : "the steam cloud should blind the husk beside its target";
		});
	}

	/** Overdrive under half health is Strength III. */
	private static String overdrive(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.setHealth(8);
			return cast(player, Runes.SELF, Runes.OVERDRIVE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			var strength = player(server).getEffect(MobEffects.STRENGTH);
			return strength != null && strength.getAmplifier() == 2 ? null
				: "Overdrive at 8 of 20 health should be Strength III (" + (strength == null ? "none" : "level " + (strength.getAmplifier() + 1)) + ")";
		});
	}

	/** Transfusion: the caster gives 4, the wolf heals 12 (and the caster is 4 down). */
	private static String transfusion(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
			if (wolf == null) {
				return "couldn't make the wolf";
			}
			wolf.snapTo(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 4.5, 0, 0);
			wolf.tame(player);
			wolf.setNoAi(true);
			wolf.addTag(TAG);
			wolf.addTag("wildercord.rolled");
			level.addFreshEntity(wolf);
			wolf.setHealth(10);
			id[0] = wolf.getId();
			return cast(player, Runes.BEAM, Runes.TRANSFUSION);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			LivingEntity wolf = living(server, id[0]);
			float player = player(server).getHealth();
			if (wolf == null || wolf.getHealth() < 21.0F || wolf.getHealth() > 23.0F) {
				return "the wolf should heal three times the 4 given, 12 (" + health(wolf) + ")";
			}
			return player <= 16.5F ? null : "the caster should be 4 health down (" + player + ")";
		});
	}

	/** Sunscorch leaves its target glowing. */
	private static String sunscorch(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5).getId();
			return cast(player(server), Runes.BEAM, Runes.SUNSCORCH);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			LivingEntity husk = living(server, id[0]);
			return husk != null && husk.hasEffect(MobEffects.GLOWING) ? null : "the sun's light should leave its target glowing";
		});
	}

	/** Soulfire hurts a blaze, which is fire-proof (fire damage does nothing to it). */
	private static String soulfire(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = spawn(player(server).level(), EntityTypes.BLAZE, 0, 5, 180).getId();
			return cast(player(server), Runes.BEAM, Runes.SOULFIRE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(105);
		return world.getServer().computeOnServer(server -> {
			float lost = lost(living(server, id[0]));
			return lost >= 3 ? null : "soul fire should burn even a blaze (" + lost + " lost)";
		});
	}

	/** Lifesteal's siphon mark: what anyone else does to the target heals the caster for a quarter of it. */
	private static String lifesteal(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			id[0] = husk(player.level(), 0, 5).getId();
			player.setHealth(6);
			player.getFoodData().setFoodLevel(10);
			return cast(player, Runes.BEAM, Runes.LIFESTEAL);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		float[] after = {0};
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			after[0] = player(server).getHealth();
			living(server, id[0]).hurtServer(level, level.damageSources().generic(), 8.0F);
		});
		context.waitTicks(2);
		return world.getServer().computeOnServer(server -> {
			float gained = player(server).getHealth() - after[0];
			return gained >= 1.6F && gained <= 2.4F ? null : "8 damage from anyone should heal the marked husk's caster 2 (" + gained + ")";
		});
	}
}
