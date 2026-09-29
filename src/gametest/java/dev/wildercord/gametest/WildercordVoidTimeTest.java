package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Innates;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.VoidTime;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * In game, the void and time runes as the second audit pass changed them: each new mechanic and each fixed bug
 * cast for real at husks on a stone platform high in the air (power 1, straight from Touch, so the numbers are exact).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordVoidTimeTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 200, 0);
	private static final String TAG = "wildercord.void_time";

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
			// Instant ones: everything decided in the tick it is cast.
			check(failures, "Foresight (no refill, capped)", world.getServer().computeOnServer(WildercordVoidTimeTest::foresight));
			check(failures, "Borrowed Time (only what can be mended)", world.getServer().computeOnServer(WildercordVoidTimeTest::borrowedTime));
			check(failures, "Sonic Boom (the line)", world.getServer().computeOnServer(WildercordVoidTimeTest::sonicLine));
			check(failures, "Hex (+25% and a fixation), Veil (ambush)", world.getServer().computeOnServer(WildercordVoidTimeTest::hexAndVeil));
			check(failures, "Shadowstep (backstab)", world.getServer().computeOnServer(WildercordVoidTimeTest::shadowstep));
			check(failures, "Anchor, Starmaw (devours wards)", world.getServer().computeOnServer(WildercordVoidTimeTest::anchorAndStarmaw));
			check(failures, "Wither (IV, no healing, contagion)", world.getServer().computeOnServer(WildercordVoidTimeTest::wither));
			check(failures, "Collect (48 at most)", world.getServer().computeOnServer(WildercordVoidTimeTest::collect));
			check(failures, "Accelerate (tempo)", world.getServer().computeOnServer(WildercordVoidTimeTest::accelerate));
			check(failures, "Time Skip (a skipped second, not twice)", world.getServer().computeOnServer(WildercordVoidTimeTest::timeSkip));
			check(failures, "Timesteal (nothing to steal), Warp (pulled)", world.getServer().computeOnServer(WildercordVoidTimeTest::stealAndWarp));
			check(failures, "Shades (frail, dim-light hounds)", world.getServer().computeOnServer(WildercordVoidTimeTest::shades));
			check(failures, "Hush (a pocket where enemies cannot cast)", hush(context, world));
			check(failures, "Blind (a monster lashes out)", blind(context, world));
			check(failures, "Hollow (one centre)", hollow(context, world));
			check(failures, "Doomclock (one burst per enemy)", doomclock(context, world));
			check(failures, "Dragon Breath (no stacking, it drifts)", dragonBreath(context, world));
			check(failures, "Countdown (the moment finds another)", countdown(context, world));
			check(failures, "Eclipse and Umbra (the light web)", eclipse(context, world));
			check(failures, "Singularity (swallows what is thrown at it)", singularity(context, world));
			check(failures, "Infinity (the closer, the slower)", infinity(context, world));
			check(failures, "Shulkershell (bullets)", shulkershell(context, world));
			check(failures, "Reckoning (heals a share)", reckoning(context, world));
			check(failures, "Chronoshift (mana back)", chronoshift(context, world));
			check(failures, "Entropy (strips armour)", entropy(context, world));
			check(failures, "Malison (stronger as it passes)", malison(context, world));
			check(failures, "Portalfall (the landing slam)", portalfall(context, world));
			check(failures, "Presentation (every drawing helper, crowd budgets)", world.getServer().computeOnServer(WildercordVoidTimeTest::presentation));
			if (!failures.isEmpty()) {
				throw new AssertionError("The void and time runes went wrong:\n  " + String.join("\n  ", failures));
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

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 10) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 14) + " " + y + " " + (z - 10) + " " + (x + 14) + " " + (y + 12) + " " + (z + 24) + " minecraft:air");
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
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.setAbsorptionAmount(0);
		player.removeAllEffects();
		player.clearFire();
	}

	/** A husk {@code ahead} blocks south of the caster and {@code side} across, on 100 health so a test can count what it took. */
	private static Mob husk(ServerLevel level, double side, double ahead) {
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.snapTo(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead, 180, 0);
		husk.setNoAi(true);
		husk.addTag(TAG);
		husk.addTag("wildercord.rolled");
		level.addFreshEntity(husk);
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		husk.setHealth(100);
		return husk;
	}

	private static float taken(LivingEntity e) {
		return e.getMaxHealth() - e.getHealth();
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 0.05;
	}

	private static SpellPlan.EffectNode node(RuneDef rune) {
		return SpellCompiler.compile(List.of(Runes.TOUCH, rune)).root().groups.getFirst().effects.getFirst();
	}

	/** {@code rune} from {@code cast}, landing on {@code targets}. */
	private static void apply(Cast cast, RuneDef rune, List<? extends Entity> targets) {
		ServerPlayer player = (ServerPlayer) cast.caster;
		Vec3 at = targets.isEmpty() ? player.position() : targets.getFirst().position();
		boolean self = targets.size() == 1 && targets.getFirst() == player;
		Effects.apply(cast, node(rune), new Cast.Hit(List.copyOf(targets), at, player.getLookAngle(), player.position(), null, null, self));
	}

	private static void touch(ServerPlayer player, RuneDef rune, Entity target) {
		apply(new Cast(player), rune, List.of(target));
	}

	private static void clean(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=arrow]");
		world.getServer().runCommand("kill @e[type=wolf]");
		world.getServer().runOnServer(server -> stand(player(server)));
		context.waitTicks(15);
	}

	private static void sweep(ServerPlayer player) {
		player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(30), m -> m.entityTags().contains(TAG)).forEach(Mob::discard);
		stand(player);
	}

	private static <T> T onServer(TestSingleplayerContext world, Function<MinecraftServer, T> task) {
		return world.getServer().computeOnServer(task::apply);
	}

	private static float health(TestSingleplayerContext world, int id) {
		return onServer(world, server -> player(server).level().getEntity(id) instanceof LivingEntity e && e.isAlive() ? e.getHealth() : 0.0F);
	}

	private static void blow(ServerPlayer player, Mob striker, float amount) {
		Effects.readyToHurt(player);
		player.hurtServer(player.level(), player.level().damageSources().mobAttack(striker), amount);
	}

	// ------------------------------------------------------------------ instant checks

	/** Two dodges from one cast, and a Zone-style recast of the same cast does not refill them; a dodge stops 12 at most. */
	private static String foresight(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			Mob striker = husk(player.level(), 0, 3);
			Cast cast = new Cast(player);
			apply(cast, Runes.FORESIGHT, List.of(player));
			float before = player.getHealth();
			blow(player, striker, 5);
			blow(player, striker, 5);
			if (player.getHealth() < before) {
				return "two blows should be dodged (health " + before + " to " + player.getHealth() + ")";
			}
			// The same cast puts it up again (a Zone's next pulse): nothing is refilled.
			apply(cast, Runes.FORESIGHT, List.of(player));
			blow(player, striker, 5);
			if (player.getHealth() > before - 3) {
				return "the same cast landing again must not refill the dodges (health " + player.getHealth() + ", was " + before + ")";
			}
			// A new cast is a new ward; a huge blow is only softened by 12.
			stand(player);
			apply(new Cast(player), Runes.FORESIGHT, List.of(player));
			float full = player.getHealth();
			blow(player, striker, 20);
			float lost = full - player.getHealth();
			if (lost < 6 || lost > 9) {
				return "a 20-damage blow should lose only the 12 the sidestep turns away, 8 left (lost " + lost + ")";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	/** Heals only what is missing, so a health already back is not owed. */
	private static String borrowedTime(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			player.setAttached(WildercordAttachments.INNATE, Runes.BORROWED_TIME.id());
			Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().magic(), 10);
			player.setHealth(player.getMaxHealth());
			apply(new Cast(player), Runes.BORROWED_TIME, List.of(player));
			if (Innates.owed(player) > 0.01F) {
				return "health already back: nothing should be owed (owes " + Innates.owed(player) + ")";
			}
			player.setHealth(player.getMaxHealth() - 4);
			Effects.readyToHurt(player);
			player.hurtServer(player.level(), player.level().damageSources().magic(), 2);
			player.setHealth(player.getMaxHealth() - 4);
			apply(new Cast(player), Runes.BORROWED_TIME, List.of(player));
			// Missing 4, and 12 taken in the last 5 seconds (the ten as well): only the 4 that can be mended, and a fifth on top.
			if (!near(Innates.owed(player), 4 * 1.2)) {
				return "only the 4 that are missing can be mended, so 4.8 owed with the fifth (owes " + Innates.owed(player) + ")";
			}
			return null;
		} finally {
			player.setAttached(WildercordAttachments.INNATE, "");
			sweep(player);
		}
	}

	/** 16 on the target, 8 on the one beside the line, and never twice on a creature that is a target itself. */
	private static String sonicLine(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			Mob near = husk(player.level(), 0.3, 3);
			Mob far = husk(player.level(), 0, 8);
			far.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			Mob aside = husk(player.level(), 6, 3);
			touch(player, Runes.SONIC_BOOM, far);
			if (!near(taken(far), 16)) {
				return "the target should take 16 through diamond armour (took " + taken(far) + ")";
			}
			if (!near(taken(near), 8)) {
				return "a husk on the line should take 8 (took " + taken(near) + ")";
			}
			if (taken(aside) > 0) {
				return "a husk well off the line should be untouched (took " + taken(aside) + ")";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	private static String hexAndVeil(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			Mob a = husk(player.level(), 0, 4);
			a.setNoAi(false);
			a.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			touch(player, Runes.HEX, a);
			touch(player, Runes.HARM, a);
			if (!near(taken(a), 7 * 1.25)) {
				return "a hexed creature should take +25% from the hexer's Harm, 8.75 (took " + taken(a) + ")";
			}
			// Veil: the first damage from the dark is half again as hard and ends the veil.
			Mob b = husk(player.level(), 3, 4);
			apply(new Cast(player), Runes.VEIL, List.of(player));
			if (!player.hasEffect(MobEffects.INVISIBILITY)) {
				return "Veil should make you invisible";
			}
			touch(player, Runes.HARM, b);
			if (!near(taken(b), 7 * 1.5)) {
				return "the first blow from a Veil should land half again as hard, 10.5 (took " + taken(b) + ")";
			}
			if (player.hasEffect(MobEffects.INVISIBILITY)) {
				return "the ambush should end the veil";
			}
			float once = taken(b);
			touch(player, Runes.HARM, b);
			if (!near(taken(b) - once, 7)) {
				return "the second blow should be plain (took " + (taken(b) - once) + ")";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	private static String shadowstep(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			Mob target = husk(player.level(), 0, 6);
			touch(player, Runes.SHADOWSTEP, target);
			if (player.position().distanceTo(target.position()) > 3.5) {
				return "Shadowstep should put you beside the creature (" + player.position().distanceTo(target.position()) + " blocks away)";
			}
			touch(player, Runes.HARM, target);
			if (!near(taken(target), 7 * 1.5)) {
				return "the next blow after a Shadowstep should be half again as hard, 10.5 (took " + taken(target) + ")";
			}
			float once = taken(target);
			touch(player, Runes.HARM, target);
			if (!near(taken(target) - once, 7)) {
				return "the backstab is used once (second took " + (taken(target) - once) + ")";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	private static String anchorAndStarmaw(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			Mob husk = husk(player.level(), 0, 4);
			apply(new Cast(player), Runes.ANCHOR, List.of(player));
			if (!VoidTime.anchored(player)) {
				return "Anchor should hold you fast";
			}
			// Starmaw devours it, and a ward, and the good effects, 4 each on top of 14.
			// A ward on an enemy: the husk puts Foresight on itself (it is its own ally).
			Effects.apply(new Cast(husk), node(Runes.FORESIGHT), new Cast.Hit(List.of(husk), husk.position(), husk.getLookAngle(), husk.position(), null, null, true));
			husk.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SPEED, 200, 0));
			touch(player, Runes.STARMAW, husk);
			if (!near(taken(husk), 14 + 4 * 2)) {
				return "Starmaw should take 14 + 4 for the Speed + 4 for the Foresight = 22 (took " + taken(husk) + ")";
			}
			if (husk.hasEffect(MobEffects.SPEED)) {
				return "Starmaw should swallow the good effect";
			}
			Mob other = husk(player.level(), 2, 4);
			apply(new Cast(player), Runes.ANCHOR, List.of(other));
			return null;
		} finally {
			sweep(player);
		}
	}

	private static String wither(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			Mob husk = husk(player.level(), 0, 3);
			touch(player, Runes.WITHER, husk);
			net.minecraft.world.effect.MobEffectInstance wither = husk.getEffect(MobEffects.WITHER);
			if (wither == null || wither.getAmplifier() != 3 || wither.getDuration() < 110 || wither.getDuration() > 120) {
				return "Wither should be Wither IV for 6 seconds (has " + wither + ")";
			}
			float low = 50;
			husk.setHealth(low);
			husk.heal(10);
			if (husk.getHealth() > low + 0.01F) {
				return "the withered should not heal (health " + husk.getHealth() + ")";
			}
			// Whoever strikes it in melee catches the rot.
			Effects.readyToHurt(husk);
			husk.hurtServer(player.level(), player.level().damageSources().playerAttack(player), 2.0F);
			if (!player.hasEffect(MobEffects.WITHER)) {
				return "the one who strikes the withered by hand should catch Wither";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	private static String collect(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		ServerLevel level = player.level();
		try {
			for (int i = 0; i < 60; i++) {
				ItemEntity item = new ItemEntity(level, STAGE.getX() + 0.5 + (i % 6) - 3, STAGE.getY() + 0.3, STAGE.getZ() + 5.5 + (i / 6) * 0.3, new ItemStack(Items.STICK));
				item.setNoPickUpDelay();
				level.addFreshEntity(item);
			}
			apply(new Cast(player), Runes.COLLECT, List.of());
			// A Touch hit with no creature lands on the caster's own spot: reach is 8 around it.
			long here = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2.0)).size();
			long left = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(20.0)).size() - here;
			if (here != 48 || left != 12) {
				return "Collect should bring 48 of the 60 and leave 12 (brought " + here + ", left " + left + ")";
			}
			return null;
		} finally {
			level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(30.0)).forEach(Entity::discard);
			sweep(player);
		}
	}

	private static String accelerate(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			int slow = Charging.fullTicks(player);
			apply(new Cast(player), Runes.ACCELERATE, List.of(player));
			MobEffectCheck speed = new MobEffectCheck(player.getEffect(MobEffects.SPEED));
			if (!speed.is(1)) {
				return "Accelerate should give Speed II (has " + player.getEffect(MobEffects.SPEED) + ")";
			}
			if (!VoidTime.hurried(player)) {
				return "Accelerate's Haste III should mark hurried time";
			}
			int quick = Charging.fullTicks(player);
			if (quick >= slow || quick != Math.round(slow / 1.4F)) {
				return "a charge should fill 40% faster (" + slow + " ticks, then " + quick + ")";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	private record MobEffectCheck(net.minecraft.world.effect.MobEffectInstance effect) {
		boolean is(int amplifier) {
			return effect != null && effect.getAmplifier() == amplifier;
		}
	}

	private static String timeSkip(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			apply(new Cast(player), Runes.TIME_SKIP, List.of(player));
			net.minecraft.world.effect.MobEffectInstance first = player.getEffect(MobEffects.RESISTANCE);
			if (first == null || first.getAmplifier() != 4) {
				return "a Time Skip should leave a moment of Resistance V (has " + first + ")";
			}
			player.removeEffect(MobEffects.RESISTANCE);
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			apply(new Cast(player), Runes.TIME_SKIP, List.of(player));
			if (player.hasEffect(MobEffects.RESISTANCE)) {
				return "a second skip within 5 seconds should not skip time again";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	private static String stealAndWarp(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		try {
			Mob bare = husk(player.level(), 0, 4);
			touch(player, Runes.TIMESTEAL, bare);
			if (!bare.hasEffect(MobEffects.SLOWNESS) || !player.hasEffect(MobEffects.SPEED) || !player.hasEffect(MobEffects.HASTE)) {
				return "with nothing to steal it should steal a moment: the target drags, you are quickened";
			}
			player.removeAllEffects();
			Mob other = husk(player.level(), 0, 6);
			touch(player, Runes.WARP, other);
			if (!Reactions.has(other, Reactions.Mark.PULLED)) {
				return "Warp should leave the enemy pulled";
			}
			if (!player.hasEffect(MobEffects.INVISIBILITY)) {
				return "Warp should leave you unseen for a second";
			}
			return null;
		} finally {
			sweep(player);
		}
	}

	private static String shades(MinecraftServer server) {
		ServerPlayer player = player(server);
		stand(player);
		ServerLevel level = player.level();
		try {
			apply(new Cast(player), Runes.SHADES, List.of(player));
			List<net.minecraft.world.entity.animal.wolf.Wolf> hounds = level.getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class,
				player.getBoundingBox().inflate(10), w -> w.entityTags().contains(VoidTime.SHADE_TAG));
			if (hounds.size() != 2) {
				return "two hounds should rise (" + hounds.size() + ")";
			}
			for (var hound : hounds) {
				if (!near(hound.getMaxHealth(), 20)) {
					return "a hound should be frail, 20 health (" + hound.getMaxHealth() + ")";
				}
				if (hound.hasEffect(MobEffects.STRENGTH)) {
					return "a hound in the light should have no Strength";
				}
			}
			return null;
		} finally {
			level.getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class, player.getBoundingBox().inflate(30)).forEach(Entity::discard);
			sweep(player);
		}
	}

	// ------------------------------------------------------------------ timed checks

	private static String hush(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob inside = husk(player.level(), 0, 4);
			Mob outside = husk(player.level(), 0, 14);
			inside.setNoAi(false);
			inside.setTarget(player);
			apply(new Cast(player), Runes.HUSH, List.of(inside));
			return new int[] {inside.getId(), outside.getId()};
		});
		context.waitTicks(15);
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob inside = (Mob) player.level().getEntity(ids[0]);
			Mob outside = (Mob) player.level().getEntity(ids[1]);
			if (!VoidTime.hushed(inside)) {
				return "an enemy inside the pocket should be hushed";
			}
			if (VoidTime.hushed(outside)) {
				return "an enemy outside should not be";
			}
			if (VoidTime.hushed(player)) {
				return "the one who cast it is never hushed by it";
			}
			if (inside.hasEffect(MobEffects.BLINDNESS)) {
				return "Hush should no longer blind (that is Blind's job)";
			}
			return inside.hasEffect(MobEffects.WEAKNESS) ? null : "a hushed monster should be weakened";
		});
		// Its own wits may take a target back between pulses (every half second): look for the forgetting over a few ticks.
		boolean forgot = false;
		for (int i = 0; i < 12 && !forgot; i++) {
			context.waitTicks(1);
			forgot = onServer(world, server -> ((Mob) player(server).level().getEntity(ids[0])).getTarget() == null);
		}
		if (failure == null && !forgot) {
			failure = "a hushed monster should forget its target";
		}
		context.waitTicks(125);
		if (failure == null) {
			failure = onServer(world, server -> VoidTime.hushed((LivingEntity) player(server).level().getEntity(ids[0])) ? "the pocket should close after its 6 seconds" : null);
		}
		clean(context, world);
		return failure;
	}

	private static String blind(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob a = husk(player.level(), 0, 5);
			Mob b = husk(player.level(), 2, 5);
			a.setNoAi(false);
			b.setNoAi(false);
			a.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			b.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			a.setTarget(player);
			touch(player, Runes.BLIND, a);
			return new int[] {a.getId(), b.getId()};
		});
		// Its own wits may pull it back to the player between our refreshes: look for the lashing out over a few ticks.
		boolean lashed = false;
		for (int i = 0; i < 12 && !lashed; i++) {
			context.waitTicks(1);
			lashed = onServer(world, server -> ((Mob) player(server).level().getEntity(ids[0])).getTarget() == player(server).level().getEntity(ids[1]));
		}
		boolean lashedOut = lashed;
		String failure = onServer(world, server -> {
			Mob a = (Mob) player(server).level().getEntity(ids[0]);
			if (!lashedOut) {
				return "a blinded monster should turn on the one beside it (its target is " + a.getTarget() + ")";
			}
			return a.hasEffect(MobEffects.BLINDNESS) ? null : "it should be blind";
		});
		clean(context, world);
		return failure;
	}

	/** Three husks in a cluster: only the one struck is a centre (20); the others are dragged and take 8 once. */
	private static String hollow(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob a = husk(player.level(), 0, 5);
			Mob b = husk(player.level(), 1.2, 5);
			Mob c = husk(player.level(), -1.2, 5);
			Cast cast = new Cast(player);
			SpellPlan.EffectNode node = node(Runes.HOLLOW);
			Effects.apply(cast, node, new Cast.Hit(List.of(a, b, c), a.position(), player.getLookAngle(), player.position(), null, null, false));
			// Again in the same cast (a Zone's next pulse): it does not erase them a second time.
			Effects.apply(cast, node, new Cast.Hit(List.of(a, b, c), a.position(), player.getLookAngle(), player.position(), null, null, false));
			return new int[] {a.getId(), b.getId(), c.getId()};
		});
		context.waitTicks(14);
		float[] took = onServer(world, server -> {
			ServerLevel level = player(server).level();
			float[] out = new float[3];
			for (int i = 0; i < 3; i++) {
				LivingEntity e = (LivingEntity) level.getEntity(ids[i]);
				out[i] = e.getMaxHealth() - e.getHealth();
			}
			return out;
		});
		String failure = null;
		float sum = took[0] + took[1] + took[2];
		float max = Math.max(took[0], Math.max(took[1], took[2]));
		if (max < 19 || max > 21) {
			failure = "the one struck should take 20, once (took " + java.util.Arrays.toString(took) + ")";
		} else if (Math.abs(sum - (20 + 8 + 8)) > 0.5) {
			failure = "the other two should take 8 each and no more (took " + java.util.Arrays.toString(took) + ")";
		}
		clean(context, world);
		return failure;
	}

	/** Three husks, three clocks, one cast: each takes the 8 of a single burst, not three. */
	private static String doomclock(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob a = husk(player.level(), 0, 5);
			Mob b = husk(player.level(), 1.0, 5);
			Mob c = husk(player.level(), -1.0, 5);
			apply(new Cast(player), Runes.DOOMCLOCK, List.of(a, b, c));
			return new int[] {a.getId(), b.getId(), c.getId()};
		});
		context.waitTicks(75);
		String failure = onServer(world, server -> {
			ServerLevel level = player(server).level();
			for (int id : ids) {
				LivingEntity e = (LivingEntity) level.getEntity(id);
				if (!near(taken(e), 8)) {
					return "each enemy should take one burst of 8, not one from every clock (took " + taken(e) + ")";
				}
			}
			return null;
		});
		clean(context, world);
		return failure;
	}

	/** The same cast laying it twice does not double it; it rolls on along the way it was blown. */
	private static String dragonBreath(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob a = husk(player.level(), 0, 5);
			Mob far = husk(player.level(), 0, 11);
			Cast cast = new Cast(player);
			apply(cast, Runes.DRAGON_BREATH, List.of(a));
			apply(cast, Runes.DRAGON_BREATH, List.of(a));
			return new int[] {a.getId(), far.getId()};
		});
		context.waitTicks(105);
		String failure = onServer(world, server -> {
			ServerLevel level = player(server).level();
			LivingEntity a = (LivingEntity) level.getEntity(ids[0]);
			if (taken(a) > 25.5 || taken(a) < 5) {
				return "a breath laid twice by one cast should still be one cloud, at most 25 (took " + taken(a) + ")";
			}
			LivingEntity far = (LivingEntity) level.getEntity(ids[1]);
			return taken(far) > 0 ? null : "the breath should roll on down the lane and reach the husk 6 blocks further on";
		});
		clean(context, world);
		return failure;
	}

	private static String countdown(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob a = husk(player.level(), 0, 5);
			Mob b = husk(player.level(), 3, 5);
			touch(player, Runes.COUNTDOWN, a);
			return new int[] {a.getId(), b.getId()};
		});
		context.waitTicks(5);
		onServer(world, server -> {
			player(server).level().getEntity(ids[0]).discard();
			return null;
		});
		context.waitTicks(35);
		String failure = onServer(world, server -> {
			LivingEntity b = (LivingEntity) player(server).level().getEntity(ids[1]);
			return near(taken(b), 6) ? null : "the moment should find the next husk when its mark is gone, for 6 (took " + taken(b) + ")";
		});
		clean(context, world);
		return failure;
	}

	/**
	 * The presentation of the void and time runes: every drawing helper runs on a real creature without throwing (a bad
	 * particle or sound name fails here, not in front of a player), and a crowd cannot flood the wire (a Countdown on twenty
	 * husks draws eight faces; the budget is spent within the tick).
	 */
	private static String presentation(MinecraftServer server) {
		ServerPlayer player = player(server);
		ServerLevel level = player.level();
		Mob target = husk(level, 0, 5);
		Vec3 at = target.position();
		Vec3 far = at.add(6, 0, 3);
		try {
			dev.wildercord.cast.VoidFx.hook(level, target, player.position());
			dev.wildercord.cast.VoidFx.hookLanded(level, at);
			dev.wildercord.cast.VoidFx.disc(level, at, 7, 10);
			dev.wildercord.cast.VoidFx.discSnap(level, at, 7);
			dev.wildercord.cast.VoidFx.slit(level, at.add(0, 1, 0), new Vec3(0, 0, 1), 2.6, 0.6, 20);
			dev.wildercord.cast.VoidFx.cutout(level, at.add(0, 1, 0), 0.75);
			dev.wildercord.cast.VoidFx.smear(level, at, far);
			dev.wildercord.cast.VoidFx.rope(level, at.add(0, 1, 0), far);
			dev.wildercord.cast.VoidFx.spirals(level, at, far);
			dev.wildercord.cast.VoidFx.returnSigil(level, at, 60);
			dev.wildercord.cast.VoidFx.returnSnap(level, at);
			dev.wildercord.cast.VoidFx.zipShut(level, at.add(0, 1, 0), new Vec3(0, 0, 1), 0xD8B040, 0x404048);
			dev.wildercord.cast.VoidFx.eclipseDisc(level, at, 4, 4.5, 100);
			dev.wildercord.cast.VoidFx.eclipseLift(level, at, 4);
			dev.wildercord.cast.VoidFx.veins(level, target);
			dev.wildercord.cast.VoidFx.blackHeart(level, at.add(0, 1, 0));
			dev.wildercord.cast.VoidFx.clank(level, target);
			dev.wildercord.cast.VoidFx.claws(level, target, true);
			dev.wildercord.cast.VoidFx.claws(level, target, false);
			dev.wildercord.cast.TimeFx.face(level, at.add(0, 2, 0), new Vec3(0, 0, 1), 0.6, 20, false);
			dev.wildercord.cast.TimeFx.stoppedFace(level, at.add(0, 2, 0), new Vec3(1, 0, 0), 0.6, 1.0, 20);
			dev.wildercord.cast.TimeFx.fuse(level, at.add(0, 2, 0), player.getEyePosition(), 0.5, 11);
			dev.wildercord.cast.TimeFx.halo(level, target, 2, 30);
			dev.wildercord.cast.TimeFx.pipSpent(level, target);
			dev.wildercord.cast.TimeFx.skip(level, at, far);
			dev.wildercord.cast.TimeFx.rewindPath(level, at, far);
			dev.wildercord.cast.TimeFx.stasisColumn(level, target, 100);
			dev.wildercord.cast.TimeFx.stasisShard(level, target, 100);
			dev.wildercord.cast.TimeFx.stasisRelease(level, target, 9);
			dev.wildercord.cast.TimeFx.hourglass(level, at, 1.6, 0xF2D98A, 24);
			dev.wildercord.cast.TimeFx.sandThread(level, at.add(0, 1, 0), far, 0xF2D98A);
			dev.wildercord.cast.TimeFx.coin(level, target);
			dev.wildercord.cast.TimeFx.ending(level, at.add(0, 1, 0), 0xF2D98A, "time_tick", 0.6F);
		} catch (RuntimeException e) {
			return "a drawing helper threw: " + e;
		}
		// The budget: eight in a tick, then no more until the next one.
		int drawn = 0;
		for (int i = 0; i < 20; i++) {
			if (dev.wildercord.cast.TimeFx.allow(level, "vt_budget_test", 8)) {
				drawn++;
			}
		}
		if (drawn != 8) {
			return "the budget should allow 8 in one tick (allowed " + drawn + ")";
		}
		// A Countdown on twenty husks spends the eight faces of its tick and draws the other twelve as a spark.
		List<Mob> crowd = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			crowd.add(husk(level, -8 + (i % 10) * 1.6, 8 + (i / 10) * 1.6));
		}
		Cast cast = new Cast(player);
		apply(cast, Runes.COUNTDOWN, crowd);
		if (dev.wildercord.cast.TimeFx.allow(level, "countdown_face", 8)) {
			return "twenty Countdown marks should have spent the whole face budget of the tick";
		}
		return null;
	}

	/** An Eclipse makes the light dim, so Umbra bites double even at noon, and the eclipse adds 20% to it. */
	private static String eclipse(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob a = husk(player.level(), 0, 6);
			// The light on a husk at noon under the open sky is 15: plain Umbra is 4 (x1.0).
			touch(player, Runes.UMBRA, a);
			if (!near(taken(a), 4)) {
				return -1;
			}
			a.setHealth(100);
			Reactions.clear(a, Reactions.Mark.SHADOWED);
			apply(new Cast(player), Runes.ECLIPSE, List.of(a));
			return a.getId();
		});
		if (id < 0) {
			clean(context, world);
			return "Umbra at noon should be the plain 4";
		}
		context.waitTicks(3);
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			LivingEntity a = (LivingEntity) player.level().getEntity(id);
			a.setHealth(100);
			touch(player, Runes.UMBRA, a);
			// 4 x2 (dim under the eclipse) x1.2 (the eclipse's own bonus)
			return near(taken(a), 8 * 1.2) ? null : "under an Eclipse, Umbra should bite double and 20% harder, 9.6 (took " + taken(a) + ")";
		});
		// Let its disc close (5 seconds) so it does not lean on the next test.
		context.waitTicks(105);
		clean(context, world);
		return failure;
	}

	private static String singularity(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Mob husk = husk(level, 0, 8);
			husk.setNoAi(false);
			husk.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			Mob archer = husk(level, 6, 3);
			Vec3 centre = new Vec3(STAGE.getX() + 0.5, STAGE.getY() + 1.2, STAGE.getZ() + 8.5);
			SpellPlan.EffectNode node = node(Runes.SINGULARITY);
			Effects.apply(new Cast(player), node, new Cast.Hit(List.of(), centre, new Vec3(0, 0, 1), player.position(), null, null, false));
			Arrow arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
			arrow.snapTo(centre.x + 1.5, centre.y, centre.z, 0, 0);
			arrow.setOwner(archer);
			arrow.setNoGravity(true);
			level.addFreshEntity(arrow);
			return new int[] {husk.getId(), arrow.getId()};
		});
		context.waitTicks(10);
		String failure = onServer(world, server -> player(server).level().getEntity(ids[1]) == null || player(server).level().getEntity(ids[1]).isRemoved() ? null
			: "an enemy arrow that enters the black hole should be swallowed");
		float ten = health(world, ids[0]);
		context.waitTicks(46);
		if (failure == null) {
			float early = health(world, ids[0]);
			context.waitTicks(10);
			float late = health(world, ids[0]);
			failure = Math.abs((100 - late) - 7) < 0.6 ? null : "the burst should be 6 and 1 more for the arrow swallowed = 7 (health 100, " + ten + " at 10 ticks, " + early + " then " + late + ")";
		}
		clean(context, world);
		return failure;
	}

	private static String infinity(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob near = husk(player.level(), 0, 2.4);
			apply(new Cast(player), Runes.INFINITY, List.of(player));
			return near.getId();
		});
		context.waitTicks(8);
		String failure = onServer(world, server -> {
			LivingEntity e = (LivingEntity) player(server).level().getEntity(id);
			net.minecraft.world.effect.MobEffectInstance slow = e.getEffect(MobEffects.SLOWNESS);
			return slow != null && slow.getAmplifier() >= 3 ? null : "a hostile thing 2.4 blocks off should be slowed hard (has " + slow + ")";
		});
		clean(context, world);
		return failure;
	}

	private static String shulkershell(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), 0, 6);
			apply(new Cast(player), Runes.SHULKERSHELL, List.of(player));
			blow(player, husk, 10);
			return husk.getId();
		});
		context.waitTicks(95);
		String failure = onServer(world, server -> {
			LivingEntity husk = (LivingEntity) player(server).level().getEntity(id);
			return taken(husk) > 1.5 && taken(husk) < 4.5 ? null : "the 8 the shell turned aside should leave as bullets, about 2.7 each (took " + taken(husk) + ")";
		});
		clean(context, world);
		return failure;
	}

	private static String reckoning(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), 0, 4);
			touch(player, Runes.RECKONING, husk);
			for (int i = 0; i < 3; i++) {
				Effects.readyToHurt(husk);
				husk.hurtServer(player.level(), player.level().damageSources().magic(), 10);
			}
			player.setHealth(8);
			return husk.getId();
		});
		context.waitTicks(85);
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			LivingEntity husk = (LivingEntity) player.level().getEntity(id);
			if (!near(taken(husk), 30 + 12)) {
				return "30 counted should come due as at most 12 (took " + taken(husk) + ")";
			}
			return player.getHealth() >= 13.5F && player.getHealth() <= 14.6F ? null : "half of what comes due (6 at most) should heal the caster from 8 (health " + player.getHealth() + ")";
		});
		clean(context, world);
		return failure;
	}

	private static String chronoshift(ClientGameTestContext context, TestSingleplayerContext world) {
		onServer(world, server -> {
			ServerPlayer player = player(server);
			Spellbooks.setMana(player, 0);
			VoidTime.spent(player, 40);
			return null;
		});
		context.waitTicks(3);
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			apply(new Cast(player), Runes.CHRONOSHIFT, List.of(player));
			float mana = Spellbooks.mana(player);
			return mana >= 11.9F && mana < 20 ? null : "a third of the 40 mana spent in the last 5 seconds should come back, 12 (mana " + mana + ")";
		});
		clean(context, world);
		return failure;
	}

	private static String entropy(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob husk = husk(player.level(), 0, 4);
			husk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			touch(player, Runes.ENTROPY, husk);
			return husk.getId();
		});
		double[] armour = new double[3];
		context.waitTicks(3);
		armour[0] = onServer(world, server -> ((LivingEntity) player(server).level().getEntity(id)).getArmorValue());
		context.waitTicks(62);
		armour[1] = onServer(world, server -> ((LivingEntity) player(server).level().getEntity(id)).getArmorValue());
		context.waitTicks(60);
		armour[2] = onServer(world, server -> ((LivingEntity) player(server).level().getEntity(id)).getArmorValue());
		String failure = null;
		if (armour[1] > armour[0] - 2.5) {
			failure = "three wounds in, it should have lost about 3 points of armour (" + armour[0] + " to " + armour[1] + ")";
		} else if (armour[2] < armour[0] - 0.01) {
			failure = "its armour should come back when the unravelling ends (" + armour[0] + " then " + armour[2] + ")";
		}
		clean(context, world);
		return failure;
	}

	private static String malison(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob a = husk(player.level(), 0, 5);
			Mob b = husk(player.level(), 2, 5);
			touch(player, Runes.MALISON, a);
			a.kill(player.level());
			return new int[] {a.getId(), b.getId()};
		});
		context.waitTicks(6);
		String failure = onServer(world, server -> {
			ServerPlayer player = player(server);
			LivingEntity b = (LivingEntity) player.level().getEntity(ids[1]);
			b.setHealth(100);
			touch(player, Runes.HARM, b);
			return near(taken(b), 7 * 1.30) ? null : "the heir of a curse should hit 30% harder: 9.1 (took " + taken(b) + ")";
		});
		clean(context, world);
		return failure;
	}

	private static String portalfall(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = onServer(world, server -> {
			ServerPlayer player = player(server);
			Mob fallen = husk(player.level(), 0, 6);
			Mob beside = husk(player.level(), 1.2, 6);
			fallen.setNoAi(false);
			fallen.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			touch(player, Runes.PORTALFALL, fallen);
			return new int[] {fallen.getId(), beside.getId()};
		});
		context.waitTicks(50);
		String failure = onServer(world, server -> {
			LivingEntity beside = (LivingEntity) player(server).level().getEntity(ids[1]);
			return near(taken(beside), 3) ? null : "where it lands, a husk beside it should take 3 (took " + taken(beside) + ")";
		});
		clean(context, world);
		return failure;
	}
}
