package dev.wildercord.gametest;

import dev.wildercord.cast.CastLock;
import dev.wildercord.cast.Exposed;
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
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
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
import java.util.List;
import java.util.Set;

/**
 * The life and arcane balance pass, in a real game: Harm leaves a husk exposed; Resonance in a crowd is linear
 * (a Burst on four husks stays under 30); Smite waits 0.7 s and then lands; a silenced caster can't cast; Restore
 * mends an item once a minute; Nourish feeds a wolf; Venom kills an undead husk (Poison alone never could);
 * Empower ends in a comedown; Manatide gives back a quarter of the next spell.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordLifeArcaneTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.life_arcane";

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
				new Object[] {"Harm exposes", (Check) WildercordLifeArcaneTest::harmExposes},
				new Object[] {"Resonance is linear", (Check) WildercordLifeArcaneTest::resonanceLinear},
				new Object[] {"Smite waits", (Check) WildercordLifeArcaneTest::smiteWaits},
				new Object[] {"Silence locks", (Check) WildercordLifeArcaneTest::silenceLocks},
				new Object[] {"Restore once a minute", (Check) WildercordLifeArcaneTest::restoreOnce},
				new Object[] {"Nourish feeds a wolf", (Check) WildercordLifeArcaneTest::nourishWolf},
				new Object[] {"Venom kills the undead", (Check) WildercordLifeArcaneTest::venomKills},
				new Object[] {"Empower comedown", (Check) WildercordLifeArcaneTest::empowerComedown},
				new Object[] {"Manatide refunds", (Check) WildercordLifeArcaneTest::manatideRefunds},
				new Object[] {"Heal shields and diminishes", (Check) WildercordLifeArcaneTest::healShield});
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
			if (System.getenv("WILDERCORD_LA_SHEET") != null) {
				sheet(context, world);
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("The life and arcane pass went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

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
		player.setAbsorptionAmount(0);
		player.removeAllEffects();
		player.clearFire();
		player.getFoodData().setFoodLevel(20);
	}

	private static Mob husk(ServerLevel level, double dx, double dz) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		if (husk == null) {
			throw new IllegalStateException("couldn't make a husk");
		}
		husk.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		return husk;
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

	private static Mob mob(MinecraftServer server, int id) {
		return player(server).level().getEntity(id) instanceof Mob mob ? mob : null;
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

	// ------------------------------------------------------------------ the checks

	/** Harm leaves the husk exposed (and the mark is a real Exposed one). */
	private static String harmExposes(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5).getId();
			return cast(player(server), Runes.BEAM, Runes.HARM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null || husk.getHealth() > husk.getMaxHealth() - 6.5F) {
				return "Harm should still deal 7 (" + health(husk) + ")";
			}
			return Exposed.has(husk) ? null : "the husk should be left exposed";
		});
	}

	/** A Burst of Resonance on four husks: 4 x 5 + at most 3 echoes of 2.5, not the square of the crowd. */
	private static String resonanceLinear(ClientGameTestContext context, TestSingleplayerContext world) {
		int[][] ids = {new int[4]};
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			double[][] spots = {{-1.5, 2}, {1.5, 2}, {-1.5, 3.5}, {1.5, 3.5}};
			for (int i = 0; i < 4; i++) {
				Mob husk = husk(level, spots[i][0], spots[i][1]);
				husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
				husk.setHealth(100);
				ids[0][i] = husk.getId();
			}
			return cast(player(server), Runes.BURST, Runes.RESONANCE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(6);
		return world.getServer().computeOnServer(server -> {
			double taken = 0;
			for (int id : ids[0]) {
				Mob husk = mob(server, id);
				taken += husk == null ? 100 : 100 - husk.getHealth();
			}
			// The old rule did 4N + N(N-1)... with echoes to every marked one: 40 for four at power 1. Now 5N + 2.5 x (at most 3 per hit, once each).
			if (taken < 19 || taken > 30) {
				return "four husks should take 5 each plus a few echoes (20 to 30 in all), took " + taken;
			}
			return null;
		});
	}

	/** Smite: nothing at first, then 13 (26 on a husk, undead) after the ring has closed. */
	private static String smiteWaits(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5).getId();
			return cast(player(server), Runes.BEAM, Runes.SMITE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		String early = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk != null && husk.getHealth() >= husk.getMaxHealth() ? null : "the column shouldn't have fallen yet (" + health(husk) + ")";
		});
		if (early != null) {
			return early;
		}
		context.waitTicks(20);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk == null || husk.getHealth() <= husk.getMaxHealth() - 19 ? null : "it should land for 13 x 2 against the undead (" + health(husk) + ")";
		});
	}

	/** A silenced player can't cast; the lock lifts. */
	private static String silenceLocks(ClientGameTestContext context, TestSingleplayerContext world) {
		String locked = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			CastLock.lock(player, 30);
			long ready = Spellbooks.readyAt(player, 0);
			String cast = cast(player, Runes.SELF, Runes.HASTE);
			return cast == null ? "a silenced caster cast anyway" : (CastLock.locked(player) ? null : "the lock should hold");
		});
		if (locked != null) {
			return locked;
		}
		context.waitTicks(40);
		return world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.HASTE) == null ? null : "the lock should have lifted");
	}

	/** Restore mends a worn item once, and not again within the minute. */
	private static String restoreOnce(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] damage = {0, 0};
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
			chest.setDamageValue(200);
			player.setItemSlot(EquipmentSlot.CHEST, chest);
			return cast(player, Runes.SELF, Runes.RESTORE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		String second = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			damage[0] = player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
			return cast(player, Runes.SELF, Runes.RESTORE);
		});
		if (second != null) {
			return second;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			damage[1] = player(server).getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
			if (damage[0] >= 200) {
				return "the first Restore should mend the chestplate (still " + damage[0] + ")";
			}
			return damage[1] == damage[0] ? null : "a second Restore inside the minute should mend nothing (" + damage[0] + " became " + damage[1] + ")";
		});
	}

	/** Nourish heals a hurt tamed wolf. */
	private static String nourishWolf(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Wolf wolf = EntityTypes.WOLF.create(player.level(), EntitySpawnReason.COMMAND);
			wolf.snapTo(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 2.0, 180, 0);
			wolf.tame(player);
			wolf.addTag(TAG);
			wolf.setNoAi(true);
			player.level().addFreshEntity(wolf);
			wolf.setHealth(10.0F);
			id[0] = wolf.getId();
			return cast(player, Runes.BURST, Runes.NOURISH);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		return world.getServer().computeOnServer(server -> {
			LivingEntity wolf = player(server).level().getEntity(id[0]) instanceof LivingEntity l ? l : null;
			return wolf != null && wolf.getHealth() >= 15.5F ? null : "the wolf should be fed and heal 6 (" + health(wolf) + ")";
		});
	}

	/** Venom works on an undead husk (Poison alone can't) and kills. */
	private static String venomKills(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			Mob husk = husk(player(server).level(), 0, 5);
			husk.setHealth(4.0F);
			id[0] = husk.getId();
			return cast(player(server), Runes.BEAM, Runes.VENOM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(100);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk == null || !husk.isAlive() ? null : "a husk on 4 health should have died of the venom (" + health(husk) + ")";
		});
	}

	/** Empower: Strength II now; Weakness once it runs out. */
	private static String empowerComedown(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.EMPOWER));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(10);
		String strong = world.getServer().computeOnServer(server -> {
			var strength = player(server).getEffect(MobEffects.STRENGTH);
			return strength != null && strength.getAmplifier() == 1 ? null : "Empower should give Strength II";
		});
		if (strong != null) {
			return strong;
		}
		context.waitTicks(210);
		return world.getServer().computeOnServer(server -> player(server).hasEffect(MobEffects.WEAKNESS) ? null : "Strength should end in a comedown (Weakness)");
	}

	/** Manatide: the next spell gives back a quarter of its cost. */
	private static String manatideRefunds(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.MANATIDE));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(2);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			// Spell 1 is threaded again by cast(): full mana first, then the spell's cost comes off and a quarter returns.
			List<String> ids = List.of(Runes.SELF.id(), Runes.NIGHT_EYE.id());
			SpellCaster.edit(player, 1, ids);
			Spellbooks.setReadyAt(player, 1, 0);
			float max = Mana.max(player);
			Spellbooks.setMana(player, max);
			SpellCaster.cast(player, 1);
			float spent = max - Spellbooks.mana(player);
			// Night Eye costs 3: 3 spent, 0.75 back, and a little regeneration; never the whole cost.
			return spent > 1.5F && spent < 2.6F ? null : "3 mana minus a quarter back should leave about 2.25 spent, saw " + spent;
		});
	}

	/** Heal: past full health it becomes a shield; a second heal in the same cast heals less. */
	private static String healShield(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.HEAL));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return player.getAbsorptionAmount() >= 3.5F ? null : "a heal on a whole player should become a shield of 2 hearts (has " + player.getAbsorptionAmount() + ")";
		});
	}

	// ------------------------------------------------------------------ the contact sheet (WILDERCORD_LA_SHEET)

	/** Casts one spell at what stands ahead, waits, and takes a screenshot named after it. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext world, String name, int wait, boolean wolf, boolean crowd, RuneDef... runes) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.setGameMode(GameType.CREATIVE);
			ServerLevel level = player.level();
			if (wolf) {
				Wolf pet = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
				pet.snapTo(STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 4.0, 180, 0);
				pet.tame(player);
				pet.addTag(TAG);
				pet.setNoAi(true);
				pet.setHealth(10.0F);
				level.addFreshEntity(pet);
			} else {
				husk(level, 0, 4).setHealth(20.0F);
				if (crowd) {
					husk(level, -2, 5);
					husk(level, 2, 5);
					husk(level, 0, 6);
				}
			}
			String failed = cast(player, runes);
			if (failed != null) {
				throw new AssertionError(name + ": " + failed);
			}
		});
		context.waitTicks(wait);
		context.takeScreenshot(TestScreenshotOptions.of("la_" + name).disableCounterPrefix());
		cleanup(context, world);
	}

	private static void sheet(ClientGameTestContext context, TestSingleplayerContext world) {
		shot(context, world, "harm", 3, false, false, Runes.BEAM, Runes.HARM);
		shot(context, world, "heal", 4, true, false, Runes.BURST, Runes.HEAL);
		shot(context, world, "haste", 3, true, false, Runes.BURST, Runes.HASTE);
		shot(context, world, "reveal", 6, false, false, Runes.BEAM, Runes.REVEAL);
		shot(context, world, "silence", 5, false, false, Runes.BEAM, Runes.SILENCE);
		shot(context, world, "smite", 16, false, false, Runes.BEAM, Runes.SMITE);
		shot(context, world, "starfall", 14, false, true, Runes.ZONE, Runes.STARFALL);
		shot(context, world, "venom", 24, false, true, Runes.BURST, Runes.VENOM);
		shot(context, world, "sporebloom", 8, false, true, Runes.BEAM, Runes.SPOREBLOOM);
		shot(context, world, "bramble", 4, true, false, Runes.BURST, Runes.BRAMBLE);
		shot(context, world, "haven", 12, true, false, Runes.BEAM, Runes.HAVEN);
		shot(context, world, "empower", 4, true, false, Runes.BURST, Runes.EMPOWER);
		shot(context, world, "restore", 6, true, false, Runes.BURST, Runes.RESTORE);
		shot(context, world, "regrowth", 8, true, false, Runes.BURST, Runes.REGROWTH);
		shot(context, world, "cleanse", 6, true, false, Runes.BURST, Runes.CLEANSE);
		shot(context, world, "nourish", 4, true, false, Runes.BURST, Runes.NOURISH);
		shot(context, world, "summon", 8, false, false, Runes.SELF, Runes.SUMMON);
		shot(context, world, "prismatic", 5, false, true, Runes.BEAM, Runes.FIRE, Runes.PUSH, Runes.PRISMATIC_BURST);
		shot(context, world, "resonance", 5, false, true, Runes.BURST, Runes.RESONANCE);
		shot(context, world, "fangs", 6, false, false, Runes.BEAM, Runes.FANGS);
		shot(context, world, "starshard", 5, false, true, Runes.BEAM, Runes.STARSHARD);
		shot(context, world, "cometfall", 28, false, true, Runes.BEAM, Runes.COMETFALL);
		shot(context, world, "bloom", 5, true, false, Runes.BURST, Runes.BLOOM);
		shot(context, world, "soulbond", 8, true, false, Runes.BURST, Runes.SOULBOND);
		shot(context, world, "vinelash", 4, false, false, Runes.BEAM, Runes.VINELASH);
		shot(context, world, "nullify", 4, false, false, Runes.BEAM, Runes.NULLIFY);
		shot(context, world, "decree", 5, false, false, Runes.BEAM, Runes.DECREE);
		shot(context, world, "drowse", 6, false, false, Runes.BEAM, Runes.DROWSE);
		shot(context, world, "rootsnare", 5, false, true, Runes.BEAM, Runes.ROOTSNARE);
		shot(context, world, "moonpetal", 5, false, true, Runes.BEAM, Runes.MOONPETAL);
		shot(context, world, "manaburn", 4, false, false, Runes.BEAM, Runes.MANABURN);
		shot(context, world, "spellbrand", 5, false, false, Runes.BEAM, Runes.SPELLBRAND);
		shot(context, world, "reflect", 4, true, false, Runes.BURST, Runes.REFLECT);
		shot(context, world, "barrier", 4, true, false, Runes.BURST, Runes.BARRIER);
		shot(context, world, "halo", 20, true, false, Runes.BURST, Runes.HALO);
		shot(context, world, "lifebloom", 24, true, false, Runes.BURST, Runes.LIFEBLOOM);
		shot(context, world, "reversal", 4, true, false, Runes.BURST, Runes.REVERSAL);
		shot(context, world, "secondwind", 4, true, false, Runes.BURST, Runes.SECOND_WIND);
		shot(context, world, "stitchtime", 4, true, false, Runes.BURST, Runes.STITCHTIME);
		shot(context, world, "light", 4, false, false, Runes.BEAM, Runes.LIGHT);
		shot(context, world, "grow", 4, false, false, Runes.BEAM, Runes.GROW);
	}
}
