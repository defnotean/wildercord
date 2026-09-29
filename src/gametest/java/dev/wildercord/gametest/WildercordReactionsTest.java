package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.ReactionRules;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
 * In game, the element reactions that joined the first five: each one set up and set off inside a single
 * spell at husks (Touch, at power 1, with every effect applied in order as a cast applies them), and
 * checked for its bonus against the same spell without the mark, for its mark being used up and for its
 * name going into the Grimoire. Fracture is also cast for real from the Cord. On a stone platform high in
 * the air, by day, in clear weather (rain would leave everything wet).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordReactionsTest implements FabricClientGameTest {
	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 200, 40);
	private static final String TAG = "wildercord.reactions";

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
			check(failures, "Fracture from the Cord", fromTheCord(context, world));
			check(failures, "Overload", onServer(world, WildercordReactionsTest::overload));
			clean(context, world);
			check(failures, "Fracture", onServer(world, WildercordReactionsTest::fracture));
			clean(context, world);
			check(failures, "Blight", onServer(world, WildercordReactionsTest::blight));
			clean(context, world);
			check(failures, "Unweave", onServer(world, WildercordReactionsTest::unweave));
			clean(context, world);
			check(failures, "Rupture", onServer(world, WildercordReactionsTest::rupture));
			clean(context, world);
			check(failures, "Elapse", elapse(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("The newer element reactions went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void check(List<String> failures, String reaction, String failure) {
		if (failure != null) {
			failures.add(reaction + ": " + failure);
		}
	}

	// ------------------------------------------------------------------ the stage

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** A wide stone floor with open air above it, an Echo Cord, every rune known, and plenty of mana. */
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

	/** The caster at the middle of the platform, facing south (+Z), whole, with nothing on them and an empty Grimoire. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0);
		player.removeAllEffects();
		player.clearFire();
		// Each check looks for its own reaction's name going in.
		player.setAttached(WildercordAttachments.GRIMOIRE, List.of());
	}

	/** A still monster (no AI) {@code ahead} blocks south of the caster and {@code side} blocks across, at full health. */
	private static Mob mob(ServerLevel level, EntityType<? extends Mob> type, double side, double ahead) {
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		mob.setHealth(mob.getMaxHealth());
		return mob;
	}

	private static Mob husk(ServerLevel level, double side, double ahead) {
		return mob(level, EntityTypes.HUSK, side, ahead);
	}

	/**
	 * One spell, {@code Touch} and then {@code effects}, landing on {@code target} at power 1: every effect
	 * applied in order, as a cast's hit applies them, so a mark made by one is there for the next.
	 */
	private static void cast(ServerPlayer player, List<RuneDef> effects, Entity target) {
		List<RuneDef> runes = new ArrayList<>();
		runes.add(Runes.TOUCH);
		runes.addAll(effects);
		SpellPlan.Group group = SpellCompiler.compile(runes).root().groups.getFirst();
		CastEngine.onHit(new Cast(player), group, new Cast.Hit(List.of(target), target.position(), new Vec3(0, 0, 1), player.position(), null, null, false), null);
	}

	private static <T> T onServer(TestSingleplayerContext world, Function<MinecraftServer, T> task) {
		return world.getServer().computeOnServer(task::apply);
	}

	private static float health(TestSingleplayerContext world, int id) {
		return onServer(world, server -> player(server).level().getEntity(id) instanceof LivingEntity e && e.isAlive() ? e.getHealth() : 0.0F);
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 0.05;
	}

	private static boolean found(ServerPlayer player, String reaction) {
		return Heart.discovered(player, Feats.reactionKey(reaction));
	}

	private static void clean(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(20);
	}

	// ------------------------------------------------------------------ Fracture, cast from the Cord

	/** {@code Touch · Chill · Pelt} cast for real at a husk in front: the frost marks it, the stones crack it. */
	private static String fromTheCord(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			// Two blocks in front, and the caster looking right at it as the spell goes off.
			Mob husk = husk(player.level(), 0, 2);
			Vec3 feet = player.position();
			Vec3 d = husk.getBoundingBox().getCenter().subtract(feet.add(0, player.getEyeHeight(), 0));
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			player.teleportTo(player.level(), feet.x, feet.y, feet.z, Set.<Relative>of(), yaw, pitch, false);
			SpellCaster.edit(player, 0, List.of(Runes.TOUCH.id(), Runes.CHILL.id(), Runes.PELT.id()));
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, Mana.max(player));
			SpellCaster.cast(player, 0);
			return husk.getId();
		});
		context.waitTicks(2);
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			Entity husk = player.level().getEntity(id);
			if (!(husk instanceof LivingEntity living)) {
				return "the husk is gone";
			}
			if (living.getHealth() >= living.getMaxHealth()) {
				return "the spell should have hit the husk in front (health " + living.getHealth() + ")";
			}
			if (!found(player, ReactionRules.FRACTURE)) {
				return "Chill then Pelt in one spell should set off Fracture and write it in the Grimoire";
			}
			if (Reactions.has(living, Reactions.Mark.FROZEN) || !Reactions.has(living, Reactions.Mark.CRACKED)) {
				return "the frost should be used up and the husk left cracked (frozen " + Reactions.has(living, Reactions.Mark.FROZEN) + ", cracked "
					+ Reactions.has(living, Reactions.Mark.CRACKED) + ")";
			}
			return null;
		});
		clean(context, world);
		return failure;
	}

	// ------------------------------------------------------------------ Overload

	/** Jolt on a burning husk: +30%, the flames burst for 4 on the husk beside it (not one 6 blocks off), and the fire goes out. */
	private static String overload(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob plain = husk(level, -6, 4);
		cast(player, List.of(Runes.JOLT), plain);
		float plainLoss = plain.getMaxHealth() - plain.getHealth();
		if (found(player, ReactionRules.OVERLOAD)) {
			return "Jolt on a husk that isn't burning shouldn't set anything off";
		}
		Mob burning = husk(level, 0, 4);
		Mob beside = husk(level, 1.5, 5);
		Mob far = husk(level, 6, 10);
		burning.igniteForSeconds(5);
		cast(player, List.of(Runes.JOLT), burning);
		float loss = burning.getMaxHealth() - burning.getHealth();
		if (plainLoss <= 0 || !near(loss, plainLoss * ReactionRules.OVERLOAD_BONUS)) {
			return "Jolt on a burning husk should hit 30% harder (took " + loss + ", " + plainLoss + " without the fire)";
		}
		if (burning.isOnFire()) {
			return "the fire should go out";
		}
		float blast = beside.getMaxHealth() - beside.getHealth();
		if (blast < ReactionRules.OVERLOAD_DAMAGE - 0.4F || blast > ReactionRules.OVERLOAD_DAMAGE + 0.05F) {
			return "the flames should burst for 4 on the husk beside it, less its armour (it took " + blast + ")";
		}
		if (far.getHealth() < far.getMaxHealth()) {
			return "the burst shouldn't reach a husk 6 blocks away";
		}
		if (!found(player, ReactionRules.OVERLOAD)) {
			return "Overload should go in the Grimoire";
		}
		return null;
	}

	// ------------------------------------------------------------------ Fracture

	/** Chill then Pelt in one spell: +40% on the Pelt, the husk thaws, and it's left cracked: the next Pelt hits 20% harder. */
	private static String fracture(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob plain = husk(level, -6, 4);
		cast(player, List.of(Runes.PELT), plain);
		float pelt = plain.getMaxHealth() - plain.getHealth();
		Mob frozen = husk(level, 0, 4);
		cast(player, List.of(Runes.CHILL, Runes.PELT), frozen);
		// Chill's own 1 freeze damage goes straight through armour.
		float loss = frozen.getMaxHealth() - frozen.getHealth();
		if (pelt <= 0 || !near(loss, 1 + pelt * ReactionRules.FRACTURE_BONUS)) {
			return "Chill then Pelt should crack it for +40% on the Pelt (took " + loss + " in all; Chill is 1 and a Pelt " + pelt + ")";
		}
		if (Reactions.has(frozen, Reactions.Mark.FROZEN) || frozen.getTicksFrozen() > 0) {
			return "the frost should be used up";
		}
		if (!Reactions.has(frozen, Reactions.Mark.CRACKED)) {
			return "it should be left cracked";
		}
		if (!found(player, ReactionRules.FRACTURE)) {
			return "Fracture should go in the Grimoire";
		}
		float before = frozen.getHealth();
		cast(player, List.of(Runes.PELT), frozen);
		float cracked = before - frozen.getHealth();
		if (!near(cracked, pelt * ReactionRules.CRACKED_BONUS)) {
			return "while it's cracked, a Pelt should hit 20% harder (took " + cracked + ", a plain one " + pelt + ")";
		}
		return null;
	}

	// ------------------------------------------------------------------ Blight

	/**
	 * Blind then Venom: the rot bursts for 3 on a vindicator and on a pillager beside it (poisoning the
	 * pillager), not on a husk 8 blocks off, and the caster heals 1 for each. Blind then Harm sets nothing
	 * off: it needs life. (Not a husk as the target: the undead are weak to life, which would change the numbers.)
	 */
	private static String blight(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob wrong = husk(level, -8, 4);
		cast(player, List.of(Runes.BLIND, Runes.HARM), wrong);
		if (found(player, ReactionRules.BLIGHT) || !Reactions.has(wrong, Reactions.Mark.SHADOWED)) {
			return "arcane damage on a shadowed husk shouldn't set off Blight, and should leave it shadowed";
		}
		player.setHealth(10);
		Mob target = mob(level, EntityTypes.VINDICATOR, 0, 4);
		Mob beside = mob(level, EntityTypes.PILLAGER, 2, 5);
		Mob far = husk(level, 0, 12);
		cast(player, List.of(Runes.BLIND, Runes.VENOM), target);
		if (!near(target.getHealth(), target.getMaxHealth() - 2 - ReactionRules.BLIGHT_DAMAGE)) {
			return "the vindicator should take Venom's 2 and the rot's 3 (health " + target.getHealth() + ")";
		}
		if (!near(beside.getHealth(), beside.getMaxHealth() - ReactionRules.BLIGHT_DAMAGE) || !beside.hasEffect(MobEffects.POISON)) {
			return "the rot should spread to the pillager beside it: 3 damage and poison (health " + beside.getHealth() + " of " + beside.getMaxHealth()
				+ ", poisoned " + beside.hasEffect(MobEffects.POISON) + ")";
		}
		if (far.getHealth() < far.getMaxHealth()) {
			return "the rot shouldn't reach a husk 8 blocks away";
		}
		if (!near(player.getHealth(), 10 + 2 * ReactionRules.BLIGHT_HEAL)) {
			return "the caster should heal 1 for each of the two it reached (health " + player.getHealth() + ", 10 before)";
		}
		if (Reactions.has(target, Reactions.Mark.SHADOWED)) {
			return "the shadow should be used up";
		}
		if (!found(player, ReactionRules.BLIGHT)) {
			return "Blight should go in the Grimoire";
		}
		return null;
	}

	// ------------------------------------------------------------------ Unweave

	/**
	 * Harm after one mark does nothing more; after two (frozen, windswept) it hits 60% harder and undoes
	 * them; after three (burning too) 90% harder, and the fire goes out.
	 */
	private static String unweave(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob one = husk(level, -5, 4);
		cast(player, List.of(Runes.CHILL, Runes.HARM), one);
		if (!near(one.getHealth(), one.getMaxHealth() - 1 - 7) || found(player, ReactionRules.UNWEAVE)) {
			return "one mark isn't enough: Chill's 1 and Harm's 7 (health " + one.getHealth() + ")";
		}
		Mob two = husk(level, 0, 4);
		cast(player, List.of(Runes.CHILL, Runes.PUSH, Runes.HARM), two);
		if (!near(two.getHealth(), two.getMaxHealth() - 1 - 7 * ReactionRules.unweave(2))) {
			return "frozen and windswept should make Harm 60% harder: 1 + 11.2 (health " + two.getHealth() + ")";
		}
		if (Reactions.has(two, Reactions.Mark.FROZEN) || Reactions.has(two, Reactions.Mark.WINDSWEPT)) {
			return "both marks should be undone";
		}
		if (!found(player, ReactionRules.UNWEAVE)) {
			return "Unweave should go in the Grimoire";
		}
		Mob three = husk(level, 5, 4);
		three.igniteForSeconds(5);
		cast(player, List.of(Runes.CHILL, Runes.PUSH, Runes.HARM), three);
		if (!near(three.getHealth(), three.getMaxHealth() - 1 - 7 * ReactionRules.unweave(3))) {
			return "burning, frozen and windswept should make Harm 90% harder: 1 + 13.3 (health " + three.getHealth() + ")";
		}
		if (three.isOnFire()) {
			return "the fire should be undone too";
		}
		return null;
	}

	// ------------------------------------------------------------------ Rupture

	/** Rend then Windcut: the Windcut +50%, the wound tears for 4 more through armour, and the caster heals 2. */
	private static String rupture(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		// The same Rend (its armour gone), but the bleeding washed off: how hard a plain Windcut lands.
		Mob plain = husk(level, -6, 4);
		cast(player, List.of(Runes.REND), plain);
		Reactions.clear(plain, Reactions.Mark.BLEEDING);
		float before = plain.getHealth();
		cast(player, List.of(Runes.WINDCUT), plain);
		float windcut = before - plain.getHealth();
		player.setHealth(10);
		Mob cut = husk(level, 0, 4);
		cast(player, List.of(Runes.REND, Runes.WINDCUT), cut);
		float loss = cut.getMaxHealth() - cut.getHealth();
		if (windcut <= 0 || !near(loss, windcut * ReactionRules.RUPTURE_BONUS + ReactionRules.RUPTURE_DAMAGE)) {
			return "Windcut on a bleeding husk should hit 50% harder and tear it for 4 more (took " + loss + ", a plain Windcut " + windcut + ")";
		}
		if (Reactions.has(cut, Reactions.Mark.BLEEDING)) {
			return "the bleeding should be used up";
		}
		if (!near(player.getHealth(), 10 + ReactionRules.RUPTURE_HEAL)) {
			return "the caster should heal 2 (health " + player.getHealth() + ", 10 before)";
		}
		if (!found(player, ReactionRules.RUPTURE)) {
			return "Rupture should go in the Grimoire";
		}
		return null;
	}

	// ------------------------------------------------------------------ Elapse

	/**
	 * Countdown on a burning husk: when it strikes, a second and a half later, the rest of the burn (about
	 * 4.5 seconds) lands at once, half again as hard, and the fire goes out. Beside a husk that only burns.
	 */
	private static String elapse(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Mob burning = husk(level, 0, 4);
			Mob plain = husk(level, -6, 4);
			burning.igniteForSeconds(6);
			plain.igniteForSeconds(6);
			cast(player, List.of(Runes.COUNTDOWN), burning);
			return new int[] {burning.getId(), plain.getId()};
		});
		context.waitTicks(40);
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			if (!(player.level().getEntity(ids[0]) instanceof LivingEntity burning) || !(player.level().getEntity(ids[1]) instanceof LivingEntity plain)) {
				return "a husk is gone";
			}
			if (burning.isOnFire()) {
				return "the burn should be over once the countdown strikes";
			}
			if (!plain.isOnFire()) {
				return "the husk that was only set alight should still be burning";
			}
			// Countdown's 6, and the burn's last 4.5 seconds as 6.75; the other husk only burned for 2 seconds.
			float extra = (plain.getHealth() - burning.getHealth());
			if (extra < 6 + ReactionRules.ELAPSE_MIN + 0.5F || extra > 6 + 6.75F + 1.5F) {
				return "the countdown should land for 6 and the rest of the burn for about 6.75 at once (it lost " + extra + " more than a husk that only burned)";
			}
			if (!found(player, ReactionRules.ELAPSE)) {
				return "Elapse should go in the Grimoire";
			}
			return null;
		});
		clean(context, world);
		return failure;
	}
}
