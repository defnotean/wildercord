package dev.wildercord.gametest;

import dev.wildercord.cast.CraftedRunes;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.TemporaryBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneColors;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The second batch of new runes, in a real game: each of the eighteen cast at husks (and a tamed wolf)
 * on a stone platform high in the air, with a check of what it's for. A glaive strikes on the way out
 * and back, an imprint waits then erupts, a latch strikes again and again; Kindred heals the wolf too,
 * Thirst heals the caster, Belated lands late; On Reaction fires only after a Shatter, On Weakness only
 * on a weakness struck; a brand bursts on the next spell, a gash stops healing and leaves a bleed, ore
 * glows under the floor, a seared blow burns, the soaked freeze and the dry don't, a sleeper wakes when
 * hurt, a spark lights a lamp and goes, good effects last longer, the dark bites harder at midnight, and
 * a sword is snatched and given back. Then three of their circles, side by side (new_runes_2_circles).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordNewRunes2Test implements FabricClientGameTest {
	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.new_runes_2";

	/** One check: what it's called, and what went wrong (null when it's right). */
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
				new Object[] {"Glaive", (Check) WildercordNewRunes2Test::glaive},
				new Object[] {"Imprint", (Check) WildercordNewRunes2Test::imprint},
				new Object[] {"Latch", (Check) WildercordNewRunes2Test::latch},
				new Object[] {"Kindred", (Check) WildercordNewRunes2Test::kindred},
				new Object[] {"Thirst", (Check) WildercordNewRunes2Test::thirst},
				new Object[] {"Belated", (Check) WildercordNewRunes2Test::belated},
				new Object[] {"On Reaction", (Check) WildercordNewRunes2Test::onReaction},
				new Object[] {"On Weakness", (Check) WildercordNewRunes2Test::onWeakness},
				new Object[] {"Spellbrand", (Check) WildercordNewRunes2Test::spellbrand},
				new Object[] {"Gash", (Check) WildercordNewRunes2Test::gash},
				new Object[] {"Prospect", (Check) WildercordNewRunes2Test::prospect},
				new Object[] {"Searing Edge", (Check) WildercordNewRunes2Test::searingEdge},
				new Object[] {"Flash Freeze", (Check) WildercordNewRunes2Test::flashFreeze},
				new Object[] {"Drowse", (Check) WildercordNewRunes2Test::drowse},
				new Object[] {"Galvanize", (Check) WildercordNewRunes2Test::galvanize},
				new Object[] {"Prolong", (Check) WildercordNewRunes2Test::prolong},
				new Object[] {"Umbra", (Check) WildercordNewRunes2Test::umbra},
				new Object[] {"Disarm", (Check) WildercordNewRunes2Test::disarm});
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
			circles(context, world);
			if (!failures.isEmpty()) {
				throw new AssertionError("The second batch of new runes went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** A 21x21 stone floor with open air above it, an Echo Cord, every rune known, and plenty of mana. */
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

	/** The caster at the middle of the platform, facing south (+Z) and a little down, whole and unburdened. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.getFoodData().setFoodLevel(20);
	}

	/** A husk at {@code dz} blocks in front of the caster (and {@code dx} to the side), with no mind of its own unless {@code ai}. */
	private static Mob husk(ServerLevel level, double dx, double dz, boolean ai) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		if (husk == null) {
			throw new IllegalStateException("couldn't make a husk");
		}
		husk.snapTo(STAGE.getX() + 0.5 + dx, STAGE.getY(), STAGE.getZ() + 0.5 + dz, 180, 0);
		husk.setNoAi(!ai);
		husk.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		return husk;
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
		// Never on the beat: a rhythm's bonus would make the numbers below drift from cast to cast.
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

	// ------------------------------------------------------------------ shapes

	/** A glaive flies out through a husk and comes back through it: two hits of Harm. */
	private static String glaive(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.GLAIVE, Runes.HARM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(50);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk == null || husk.getHealth() <= husk.getMaxHealth() - 13 ? null
				: "the husk should be struck on the way out and again on the way back (" + health(husk) + ")";
		});
	}

	/** An imprint waits where you stood, then erupts on the husk beside it. */
	private static String imprint(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 2, false).getId();
			return cast(player(server), Runes.IMPRINT, Runes.HARM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(10);
		String early = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk != null && husk.getHealth() >= husk.getMaxHealth() ? null : "it should wait 2 seconds before it erupts (" + health(husk) + ")";
		});
		if (early != null) {
			return early;
		}
		context.waitTicks(45);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk == null || husk.getHealth() < husk.getMaxHealth() ? null : "the husk within 3 blocks should be struck when it erupts";
		});
	}

	/** A latch holds on to its husk and strikes it four times at 70%. */
	private static String latch(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.LATCH, Runes.HARM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(75);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			// Four strikes of 7 at 70%: 19.6 in all.
			return husk == null || husk.getHealth() <= husk.getMaxHealth() - 14 ? null : "the latch should strike its husk again and again (" + health(husk) + ")";
		});
	}

	// ------------------------------------------------------------------ modifiers

	/** Kindred: a Heal on yourself heals your tamed wolf nearby too, by half. */
	private static String kindred(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
			if (wolf == null) {
				return "couldn't make the wolf";
			}
			wolf.snapTo(STAGE.getX() + 2.5, STAGE.getY(), STAGE.getZ() + 1.5, 0, 0);
			wolf.tame(player);
			wolf.setNoAi(true);
			wolf.addTag(TAG);
			wolf.addTag("wildercord.rolled");
			level.addFreshEntity(wolf);
			wolf.setHealth(10);
			id[0] = wolf.getId();
			player.setHealth(10);
			return cast(player, Runes.SELF, Runes.HEAL, Runes.KINDRED);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			LivingEntity wolf = player.level().getEntity(id[0]) instanceof LivingEntity e ? e : null;
			if (player.getHealth() < 17.5F) {
				return "the Heal should mend the caster in full (" + health(player) + ")";
			}
			if (wolf == null || wolf.getHealth() < 13.5F || wolf.getHealth() > 15.0F) {
				return "the caster's wolf close by should be healed by half as much, 4 (" + health(wolf) + ")";
			}
			return null;
		});
	}

	/** Thirst: a Harm through a beam heals its caster for a quarter of it. */
	private static String thirst(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			husk(player.level(), 0, 5, false);
			player.setHealth(10);
			// Hungry enough that nothing heals it but the spell.
			player.getFoodData().setFoodLevel(10);
			return cast(player, Runes.BEAM, Runes.HARM, Runes.THIRST);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		return world.getServer().computeOnServer(server -> {
			float health = player(server).getHealth();
			// A ley line under the stage would make the Harm (and so the drink) a little stronger.
			return health >= 11.6F && health <= 12.5F ? null : "the caster should drink a quarter of the 7 it dealt, 1.75 (" + health + "/20)";
		});
	}

	/** Belated: nothing at first; a moment later, 25% more. */
	private static String belated(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.BEAM, Runes.HARM, Runes.BELATED);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(10);
		String early = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk != null && husk.getHealth() >= husk.getMaxHealth() ? null : "it should land 1.5 seconds late (" + health(husk) + ")";
		});
		if (early != null) {
			return early;
		}
		context.waitTicks(30);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk == null || husk.getHealth() <= husk.getMaxHealth() - 8.5F ? null : "it should land 25% stronger, 8.75 (" + health(husk) + ")";
		});
	}

	// ------------------------------------------------------------------ links

	/** Casts {@code runes} at a fresh husk and says whether it's glowing a moment later (the link's Reveal). */
	private static Boolean revealed(ClientGameTestContext context, TestSingleplayerContext world, RuneDef... runes) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), runes);
		});
		if (cast != null) {
			return null;
		}
		context.waitTicks(5);
		Boolean glowing = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk != null && husk.hasEffect(MobEffects.GLOWING);
		});
		cleanup(context, world);
		return glowing;
	}

	/** On Reaction: Frost then Fire shatters, and the Reveal after it goes off; Fire alone sets nothing off, and it doesn't. */
	private static String onReaction(ClientGameTestContext context, TestSingleplayerContext world) {
		Boolean shattered = revealed(context, world, Runes.BEAM, Runes.FROST, Runes.FIRE, Runes.ON_REACTION, Runes.REVEAL);
		if (shattered == null || !shattered) {
			return "a Shatter should fire what's after it";
		}
		Boolean plain = revealed(context, world, Runes.BEAM, Runes.FIRE, Runes.ON_REACTION, Runes.REVEAL);
		return plain != null && !plain ? null : "with no reaction nothing after it should fire";
	}

	/** On Weakness: Venom (life) on a husk (undead, weak to life) fires the Reveal; Harm (arcane) doesn't. */
	private static String onWeakness(ClientGameTestContext context, TestSingleplayerContext world) {
		Boolean weak = revealed(context, world, Runes.BEAM, Runes.VENOM, Runes.ON_WEAKNESS, Runes.REVEAL);
		if (weak == null || !weak) {
			return "life on an undead husk should fire what's after it";
		}
		Boolean plain = revealed(context, world, Runes.BEAM, Runes.HARM, Runes.ON_WEAKNESS, Runes.REVEAL);
		return plain != null && !plain ? null : "arcane, which a husk isn't weak to, shouldn't";
	}

	// ------------------------------------------------------------------ effects

	/** Spellbrand: the brand bursts for 6 when the Delay's Harm lands a second later. */
	private static String spellbrand(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.BEAM, Runes.SPELLBRAND, Runes.DELAY, Runes.BEAM, Runes.HARM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(35);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk == null || husk.getHealth() <= husk.getMaxHealth() - 12.5F ? null : "the Harm should burst the brand for 6 more (" + health(husk) + ")";
		});
	}

	/** Gash: a cut, a bleed, and no healing while it lasts. */
	private static String gash(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.BEAM, Runes.GASH);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null || husk.getHealth() > husk.getMaxHealth() - 2.9F) {
				return "it should cut for 3 (" + health(husk) + ")";
			}
			if (!Reactions.has(husk, Reactions.Mark.BLEEDING)) {
				return "it should leave the husk bleeding";
			}
			float before = husk.getHealth();
			husk.heal(6);
			return husk.getHealth() <= before ? null : "a gashed husk shouldn't heal (" + before + " became " + husk.getHealth() + ")";
		});
	}

	/** Prospect: a diamond ore under the floor glows through it. */
	private static String prospect(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos ore = STAGE.offset(3, -3, 3);
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
			return cast(player, Runes.SELF, Runes.PROSPECT);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		String result = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			List<Display.BlockDisplay> glows = level.getEntitiesOfClass(Display.BlockDisplay.class, new AABB(ore).inflate(1.0),
				d -> d.entityTags().contains("wildercord.prospect"));
			if (glows.isEmpty()) {
				return "the diamond ore 3 blocks under the floor should glow";
			}
			if (!glows.getFirst().hasGlowingTag() || !glows.getFirst().getBlockState().is(Blocks.DIAMOND_ORE)) {
				return "its glow should be the ore itself, glowing";
			}
			return level.getBlockState(ore).is(Blocks.DIAMOND_ORE) ? null : "the ore itself should be left alone";
		});
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			level.setBlockAndUpdate(ore, Blocks.AIR.defaultBlockState());
			level.getEntitiesOfClass(Display.BlockDisplay.class, new AABB(ore).inflate(2.0), d -> d.entityTags().contains("wildercord.prospect"))
				.forEach(Display::discard);
		});
		return result;
	}

	/** Searing Edge: a punch from the seared caster sets the husk alight. */
	private static String searingEdge(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 1.5, false).getId();
			return cast(player(server), Runes.SELF, Runes.SEARING_EDGE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(15);
		world.getServer().runOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk != null) {
				player(server).attack(husk);
			}
		});
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null) {
				return null;
			}
			if (husk.getRemainingFireTicks() <= 0) {
				return "the seared blow should set the husk alight";
			}
			return husk.getHealth() <= husk.getMaxHealth() - 2.5F ? null : "the seared blow should deal 2 more fire damage (" + health(husk) + ")";
		});
	}

	/** Flash Freeze: a soaked husk freezes solid; a dry one is only slowed. */
	private static String flashFreeze(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			Mob husk = husk(player(server).level(), 0, 5, false);
			Reactions.mark(husk, Reactions.Mark.SOAKED);
			id[0] = husk.getId();
			return cast(player(server), Runes.BEAM, Runes.FLASH_FREEZE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		String soaked = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null || husk.getHealth() >= husk.getMaxHealth()) {
				return "it should deal 4 freeze damage (" + health(husk) + ")";
			}
			return Reactions.has(husk, Reactions.Mark.FROZEN) && husk.getTicksFrozen() > 0 ? null : "a soaked husk should freeze solid";
		});
		if (soaked != null) {
			return soaked;
		}
		cleanup(context, world);
		cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.BEAM, Runes.FLASH_FREEZE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null) {
				return "the dry husk should still stand";
			}
			if (Reactions.has(husk, Reactions.Mark.FROZEN)) {
				return "a dry husk shouldn't freeze";
			}
			MobEffectInstance slow = husk.getEffect(MobEffects.SLOWNESS);
			return slow != null && slow.getAmplifier() >= 1 ? null : "a dry husk should be slowed (Slowness II)";
		});
	}

	/** Drowse: a husk (with a mind of its own) falls asleep, and wakes the moment it's hurt. */
	private static String drowse(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			// A husk awake could come for a survival player; a creative one is left alone.
			player.setGameMode(GameType.CREATIVE);
			id[0] = husk(player.level(), 0, 5, true).getId();
			return cast(player, Runes.BEAM, Runes.DROWSE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		String asleep = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null) {
				return "the husk should still be there";
			}
			if (!CraftedRunes.asleep(husk) || !husk.isNoAi() || !husk.hasAttached(WildercordAttachments.FROZEN_UNTIL)) {
				return "the husk should be asleep, its mind stopped";
			}
			husk.hurtServer(player(server).level(), player(server).level().damageSources().generic(), 1.0F);
			return null;
		});
		if (asleep != null) {
			return asleep;
		}
		context.waitTicks(2);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk != null && !CraftedRunes.asleep(husk) && !husk.isNoAi() ? null : "being hurt should wake it";
		});
	}

	/** Galvanize: a spark against a redstone lamp lights it, and is gone (with nothing left behind) after 5 seconds. */
	private static String galvanize(ClientGameTestContext context, TestSingleplayerContext world) {
		BlockPos lamp = STAGE.offset(0, 1, 4);
		BlockPos spark = lamp.north();
		String cast = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.setBlockAndUpdate(lamp.below(), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(lamp, Blocks.REDSTONE_LAMP.defaultBlockState());
			return cast(player(server), Runes.RAY, Runes.GALVANIZE);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(5);
		String lit = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (!level.getBlockState(spark).is(Blocks.REDSTONE_BLOCK) || !TemporaryBlocks.recorded(level, spark)) {
				return "a spark of power (written down to go) should sit against the lamp, found " + level.getBlockState(spark);
			}
			return level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT) ? null : "the spark should light the lamp";
		});
		if (lit != null) {
			return lit;
		}
		context.waitTicks(110);
		String result = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (!level.getBlockState(spark).isAir() || TemporaryBlocks.recorded(level, spark)) {
				return "the spark should be gone after 5 seconds";
			}
			return !level.getBlockState(lamp).getValue(RedstoneLampBlock.LIT) ? null : "the lamp should go out with it";
		});
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			level.setBlockAndUpdate(lamp, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(lamp.below(), Blocks.AIR.defaultBlockState());
		});
		return result;
	}

	/** Prolong: a Swift's 10 seconds of Speed become 25. */
	private static String prolong(ClientGameTestContext context, TestSingleplayerContext world) {
		String cast = world.getServer().computeOnServer(server -> cast(player(server), Runes.SELF, Runes.SWIFT, Runes.PROLONG));
		if (cast != null) {
			return cast;
		}
		context.waitTicks(3);
		return world.getServer().computeOnServer(server -> {
			MobEffectInstance speed = player(server).getEffect(MobEffects.SPEED);
			return speed != null && speed.getDuration() > 400 ? null
				: "Speed should last 15 seconds longer (" + (speed == null ? "none" : speed.getDuration() + " ticks") + ")";
		});
	}

	/** Umbra: it bites twice as hard at midnight as at noon, and leaves its husk shadowed. */
	private static String umbra(ClientGameTestContext context, TestSingleplayerContext world) {
		float[] bright = {0};
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.BEAM, Runes.UMBRA);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		String noon = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null) {
				return "the husk should survive a bite at noon";
			}
			bright[0] = husk.getMaxHealth() - husk.getHealth();
			return Reactions.has(husk, Reactions.Mark.SHADOWED) ? null : "it should leave the husk shadowed";
		});
		if (noon != null) {
			return noon;
		}
		cleanup(context, world);
		world.getServer().runCommand("time set 18000");
		context.waitTicks(10);
		cast = world.getServer().computeOnServer(server -> {
			id[0] = husk(player(server).level(), 0, 5, false).getId();
			return cast(player(server), Runes.BEAM, Runes.UMBRA);
		});
		if (cast != null) {
			world.getServer().runCommand("time set 6000");
			return cast;
		}
		context.waitTicks(4);
		String result = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			float dim = husk == null ? 20 : husk.getMaxHealth() - husk.getHealth();
			if (bright[0] < 3.5F) {
				return "it should bite for 4 at noon (" + bright[0] + ")";
			}
			return dim >= bright[0] * 1.8F ? null : "it should bite twice as hard in the dark (" + dim + " at midnight, " + bright[0] + " at noon)";
		});
		world.getServer().runCommand("time set 6000");
		return result;
	}

	/** Disarm: the husk's sword is snatched away, and back in its hand 5 seconds later. */
	private static String disarm(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] id = {0};
		String cast = world.getServer().computeOnServer(server -> {
			Mob husk = husk(player(server).level(), 0, 5, false);
			husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			id[0] = husk.getId();
			return cast(player(server), Runes.BEAM, Runes.DISARM);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(4);
		String taken = world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			if (husk == null) {
				return "the husk should still be there";
			}
			return husk.getMainHandItem().isEmpty() && CraftedRunes.disarmed(husk) ? null : "the sword should be snatched out of its hand";
		});
		if (taken != null) {
			return taken;
		}
		context.waitTicks(110);
		return world.getServer().computeOnServer(server -> {
			Mob husk = mob(server, id[0]);
			return husk != null && husk.getMainHandItem().is(Items.IRON_SWORD) && !CraftedRunes.disarmed(husk) ? null : "the sword should be given back";
		});
	}

	// ------------------------------------------------------------------ circles

	/** Three spells made of the new runes, their circles side by side in front of the camera. */
	private static void circles(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		});
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			List<List<RuneDef>> spells = List.of(
				List.of(Runes.GLAIVE, Runes.SPELLBRAND, Runes.KINDRED, Runes.ON_REACTION, Runes.UMBRA),
				List.of(Runes.IMPRINT, Runes.FLASH_FREEZE, Runes.BELATED, Runes.ON_WEAKNESS, Runes.DROWSE, Runes.GALVANIZE),
				List.of(Runes.LATCH, Runes.GASH, Runes.THIRST, Runes.PROLONG, Runes.SEARING_EDGE, Runes.PROSPECT, Runes.DISARM));
			Vec3 eye = player.getEyePosition();
			for (int i = 0; i < spells.size(); i++) {
				List<RuneDef> runes = spells.get(i);
				Vec3 at = eye.add((i - 1) * 2.6, 0, 4.5);
				Sigils.spell(level, at, new Vec3(0, 0, -1), runes, RuneColors.of(runes.get(1)), 1.1F, 200);
			}
		});
		context.waitTicks(40);
		context.takeScreenshot(TestScreenshotOptions.of("new_runes_2_circles").disableCounterPrefix());
	}
}
