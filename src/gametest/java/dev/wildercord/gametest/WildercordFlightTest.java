package dev.wildercord.gametest;

import com.mojang.authlib.GameProfile;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Soar;
import dev.wildercord.cast.SoarRules;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.Targets;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.player.WildercordAttachments.Soaring;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Soar, cast for real on a stone platform high in the air: casting gives flight and a double-tap of jump
 * takes off, jump climbs; a flight running out 60 blocks up warns, then lets the player down with no fall
 * damage and nothing left of it; a creative player is left alone (and one who turns creative mid-flight
 * keeps creative's flight); an ally a Burst reaches can fly and a stranger can't, while a pet only falls
 * slowly; a pull from a monster grounds a flier and rests the wings for 30 seconds, and Weigh grounds
 * too; a dungeon's ward won't let it lift anyone and sets down a flier who comes in; a flight saved in the middle
 * of a crash is tidied as the player logs in; death leaves nothing; and a
 * flight carried through a real save and reload is saved without flight, given back on loading, and runs
 * out safely. Screenshots of the wings, the wake, the fading and the descent from third person
 * ({@code soar_*}).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFlightTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 100, 0);
	private static final String TAG = "wildercord.flight";

	private interface Check {
		String run(ClientGameTestContext context, TestSingleplayerContext world);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		List<String> failures = new ArrayList<>();
		TestWorldSave save;
		UUID id;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_health_regeneration false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<Object[]> checks = List.of(
				new Object[] {"Casting gives flight", (Check) WildercordFlightTest::castingGivesFlight},
				new Object[] {"Running out lets you down gently", (Check) WildercordFlightTest::runningOut},
				new Object[] {"Creative is left alone", (Check) WildercordFlightTest::creative},
				new Object[] {"Allies", (Check) WildercordFlightTest::allies},
				// Before grounding, whose last hit leaves the player grounded for a moment.
				new Object[] {"Warded arenas", (Check) WildercordFlightTest::wards},
				new Object[] {"Grounding", (Check) WildercordFlightTest::grounding},
				new Object[] {"A crashed flight is tidied at login", (Check) WildercordFlightTest::crashTidied},
				new Object[] {"Death", (Check) WildercordFlightTest::death});
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
			// Last: a flight carried through a real save and reload.
			String before = beforeTheSave(context, world);
			if (before != null) {
				failures.add("Relog: " + before);
			}
			id = world.getServer().computeOnServer(server -> player(server).getUUID());
			save = world.getWorldSave();
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
		String saved = savedWithoutFlight(save, id);
		if (saved != null) {
			failures.add("Relog: " + saved);
		}
		try (TestSingleplayerContext again = save.open()) {
			context.waitTicks(20);
			String after = afterTheSave(context, again);
			if (after != null) {
				failures.add("Relog: " + after);
			}
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("Soar went wrong:\n  " + String.join("\n  ", failures));
		}
	}

	// ------------------------------------------------------------------ the checks

	/** Self · Soar gives mayfly and a gentler speed; a double-tap of jump takes off, and holding jump climbs. */
	private static String castingGivesFlight(ClientGameTestContext context, TestSingleplayerContext world) {
		String given = on(world, player -> {
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			Soaring note = player.getAttached(WildercordAttachments.SOARING);
			long left = note == null ? -1 : note.until() - player.level().getGameTime();
			if (!player.getAbilities().mayfly || !Soar.soaring(player)) {
				return "the caster should be able to fly (mayfly " + player.getAbilities().mayfly + ")";
			}
			if (left < 390 || left > 400) {
				return "the flight should last 20 seconds (" + left + " ticks left)";
			}
			if (Math.abs(player.getAbilities().getFlyingSpeed() - SoarRules.FLY_SPEED) > 1.0E-5) {
				return "the flying speed should be Soar's gentler one (" + player.getAbilities().getFlyingSpeed() + ")";
			}
			return player.getAbilities().flying ? "standing on the ground, it shouldn't start flying on its own" : null;
		});
		if (given != null) {
			return given;
		}
		context.waitTicks(5);
		if (!context.computeOnClient(mc -> mc.player.getAbilities().mayfly)) {
			return "the client should hear that it may fly";
		}
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			hideHud(mc);
		});
		// Take off as in creative: a double-tap of jump (each tap held for a tick, so the movement input sees it).
		context.getInput().holdKeyFor(options -> options.keyJump, 1);
		context.waitTicks(2);
		context.getInput().holdKeyFor(options -> options.keyJump, 1);
		context.waitTicks(1);
		shot(context, "soar_takeoff");
		context.waitTicks(4);
		if (!context.computeOnClient(mc -> mc.player.getAbilities().flying)) {
			return "a double-tap of jump should take off";
		}
		if (!on(world, player -> player.getAbilities().flying)) {
			return "the server should know the player is flying";
		}
		double low = on(world, player -> player.getY());
		context.getInput().holdKeyFor(options -> options.keyJump, 20);
		context.waitTicks(4);
		double high = on(world, player -> player.getY());
		if (high - low < 1.5) {
			return "holding jump should climb (" + f(low) + " to " + f(high) + ")";
		}
		// Hovering: it stays up.
		context.waitTicks(20);
		double still = on(world, player -> player.getY());
		if (still < high - 0.5) {
			return "a flier should hover, not fall (" + f(high) + " to " + f(still) + ")";
		}
		world.getServer().runCommand("time set 13200");
		context.waitTicks(6);
		shot(context, "soar_wings_back");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(6);
		shot(context, "soar_wings_front");
		// On the move: the wake off the wingtips.
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(14);
		shot(context, "soar_wake");
		context.getInput().releaseKey(options -> options.keyUp);
		context.waitTicks(10);
		// Landing while flying stops the flying, as in creative, but the flight goes on.
		return on(world, player -> Soar.soaring(player) ? null : "the flight should go on after moving about");
	}

	/**
	 * A flight running out 64 blocks above the platform: the warning, then no flight, slow falling and a
	 * gentle landing with not a point of damage, and nothing of the flight left afterwards.
	 */
	private static String runningOut(ClientGameTestContext context, TestSingleplayerContext world) {
		double top = STAGE.getY() + 64;
		String up = on(world, player -> {
			hover(player, 64);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			fly(player);
			// Its end brought close, so the warning comes almost at once.
			long now = player.level().getGameTime();
			Soaring note = player.getAttached(WildercordAttachments.SOARING);
			player.setAttached(WildercordAttachments.SOARING, new Soaring(now + SoarRules.WARNING_TICKS + 10, note.speed(), false));
			return null;
		});
		if (up != null) {
			return up;
		}
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			hideHud(mc);
		});
		context.waitTicks(12);
		String warned = on(world, player -> {
			if (player.getY() < top - 2) {
				return "the flier should still be up there before the flight ends (y " + f(player.getY()) + ")";
			}
			return Soar.fading(player) ? null : "three seconds before the end the wind should start to fade";
		});
		if (warned != null) {
			return warned;
		}
		shot(context, "soar_fading");
		context.waitTicks(SoarRules.WARNING_TICKS + 4);
		String ended = on(world, player -> {
			if (Soar.soaring(player) || !Soar.descending(player)) {
				return "the flight should be over and the player coming down";
			}
			if (player.getAbilities().mayfly || player.getAbilities().flying) {
				return "the flight should be taken back (mayfly " + player.getAbilities().mayfly + ")";
			}
			if (Math.abs(player.getAbilities().getFlyingSpeed() - SoarRules.DEFAULT_FLY_SPEED) > 1.0E-5) {
				return "the flying speed should be back to what it was (" + player.getAbilities().getFlyingSpeed() + ")";
			}
			return player.hasEffect(MobEffects.SLOW_FALLING) ? null : "coming down, the player should fall slowly";
		});
		if (ended != null) {
			return ended;
		}
		if (context.computeOnClient(mc -> mc.player.getAbilities().mayfly)) {
			return "the client should hear the flight is over";
		}
		context.waitTicks(30);
		shot(context, "soar_descent");
		world.getServer().waitFor(server -> player(server).onGround(), 900);
		context.waitTicks(10);
		return on(world, player -> {
			if (!player.onGround() || player.getY() > STAGE.getY() + 1) {
				return "the player should be down on the platform (y " + f(player.getY()) + ")";
			}
			if (player.getHealth() < player.getMaxHealth()) {
				return "coming down 64 blocks should do no damage (health " + player.getHealth() + ")";
			}
			if (player.hasAttached(WildercordAttachments.SOARING)) {
				return "once down, nothing of the flight should be left";
			}
			return player.getAbilities().mayfly ? "once down, the player shouldn't be able to fly" : null;
		});
	}

	/** Creative: Soar leaves its flight and speed alone; turning creative mid-flight hands the flight to the game mode. */
	private static String creative(ClientGameTestContext context, TestSingleplayerContext world) {
		String inCreative = on(world, player -> {
			player.setGameMode(GameType.CREATIVE);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			if (player.hasAttached(WildercordAttachments.SOARING)) {
				return "a creative player shouldn't get a flight from Soar";
			}
			if (!player.getAbilities().mayfly || Math.abs(player.getAbilities().getFlyingSpeed() - SoarRules.DEFAULT_FLY_SPEED) > 1.0E-5) {
				return "a creative player's own flight should be untouched";
			}
			player.setGameMode(GameType.SURVIVAL);
			return player.getAbilities().mayfly ? "back in survival there should be no flight" : null;
		});
		if (inCreative != null) {
			return inCreative;
		}
		String turned = on(world, player -> {
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			player.setGameMode(GameType.CREATIVE);
			return Soar.soaring(player) ? null : "the flight should have begun before turning creative";
		});
		if (turned != null) {
			return turned;
		}
		context.waitTicks(3);
		return on(world, player -> {
			if (player.hasAttached(WildercordAttachments.SOARING)) {
				return "turning creative should hand the flight over to the game mode";
			}
			if (!player.getAbilities().mayfly) {
				return "turning creative mid-flight shouldn't take creative's flight away";
			}
			if (Math.abs(player.getAbilities().getFlyingSpeed() - SoarRules.DEFAULT_FLY_SPEED) > 1.0E-5) {
				return "creative flight should be at its usual speed again (" + player.getAbilities().getFlyingSpeed() + ")";
			}
			player.setGameMode(GameType.SURVIVAL);
			return player.getAbilities().mayfly ? "back in survival there should be no flight" : null;
		});
	}

	/**
	 * Burst · Soar: the caster and a player on their team can fly, a stranger can't; a tamed wolf in a real
	 * cast falls slowly instead.
	 */
	private static String allies(ClientGameTestContext context, TestSingleplayerContext world) {
		String players = on(world, player -> {
			ServerLevel level = player.level();
			MinecraftServer server = level.getServer();
			FakePlayer ally = new Teammate(level, new GameProfile(UUID.nameUUIDFromBytes("wildercord-soar-ally".getBytes()), "SoarAlly"));
			FakePlayer stranger = FakePlayer.get(level, new GameProfile(UUID.nameUUIDFromBytes("wildercord-soar-stranger".getBytes()), "SoarStranger"));
			PlayerTeam team = server.getScoreboard().addPlayerTeam("wildercord_soar");
			try {
				server.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
				server.getScoreboard().addPlayerToTeam(ally.getScoreboardName(), team);
				if (!Targets.canHelp(player, ally) || Targets.canHelp(player, stranger)) {
					return "the ally should be on the caster's side and the stranger not (test setup)";
				}
				SpellPlan.EffectNode node = node(Runes.BURST, Runes.SOAR);
				Vec3 at = player.position().add(0, 1, 0);
				Effects.apply(new Cast(player), node, new Cast.Hit(List.<Entity>of(player, ally, stranger), at, player.getLookAngle(), at, null, null, false));
				if (!ally.getAbilities().mayfly || !Soar.soaring(ally)) {
					return "a player on the caster's side should be able to fly";
				}
				if (stranger.getAbilities().mayfly || stranger.hasAttached(WildercordAttachments.SOARING)) {
					return "a stranger shouldn't be lifted: Soar only helps allies";
				}
				return Soar.soaring(player) ? null : "the caster inside their own Burst should fly too";
			} finally {
				Soar.stop(ally);
				Soar.stop(stranger);
				Soar.stop(player);
				server.getScoreboard().removePlayerTeam(team);
			}
		});
		if (players != null) {
			return players;
		}
		int[] wolf = {0};
		String cast = on(world, player -> {
			Wolf pet = EntityTypes.WOLF.create(player.level(), EntitySpawnReason.COMMAND);
			pet.snapTo(STAGE.getX() + 2.5, STAGE.getY(), STAGE.getZ() + 0.5, 0, 0);
			pet.tame(player);
			pet.addTag(TAG);
			player.level().addFreshEntity(pet);
			wolf[0] = pet.getId();
			return null;
		});
		context.waitTicks(2);
		cast = on(world, player -> cast(player, Runes.BURST, Runes.SOAR));
		if (cast != null) {
			return cast;
		}
		return on(world, player -> {
			if (!(player.level().getEntity(wolf[0]) instanceof Wolf pet)) {
				return "the wolf went missing (test setup)";
			}
			if (!pet.hasEffect(MobEffects.SLOW_FALLING)) {
				return "a pet the Burst reached should fall slowly (only players fly)";
			}
			return Soar.soaring(player) ? null : "the caster should fly from their own Burst";
		});
	}

	/**
	 * A husk's Pull on a flier: the flight is gone (coming down gently) and Soar won't lift them again for 3
	 * seconds; afterwards it will. Weigh grounds a flier too.
	 */
	private static String grounding(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] husk = {0};
		String pulled = on(world, player -> {
			hover(player, 12);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			fly(player);
			Mob caster = husk(player.level(), 0, 6);
			husk[0] = caster.getId();
			pull(caster, player, Runes.PULL);
			if (Soar.soaring(player) || player.getAbilities().mayfly) {
				return "a pull should tear the flight away (mayfly " + player.getAbilities().mayfly + ")";
			}
			if (!Soar.descending(player)) {
				return "grounded, the player should still come down gently";
			}
			cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			return Soar.soaring(player) || player.getAbilities().mayfly ? "grounded, the wind shouldn't lift them again straight away" : null;
		});
		if (pulled != null) {
			return pulled;
		}
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			hideHud(mc);
		});
		context.waitTicks(1);
		shot(context, "soar_grounded");
		context.waitTicks(SoarRules.REST_TICKS);
		String weighed = on(world, player -> {
			hover(player, 12);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			if (!Soar.soaring(player)) {
				return "30 seconds after a grounding the wind should lift them again";
			}
			fly(player);
			if (!(player.level().getEntity(husk[0]) instanceof Mob caster)) {
				return "the husk went missing (test setup)";
			}
			pull(caster, player, Runes.WEIGH);
			return Soar.soaring(player) ? "Weigh should ground a flier" : null;
		});
		// The lockout run out, so the checks after this one can fly.
		context.waitTicks(SoarRules.REST_TICKS + 2);
		return weighed;
	}

	/**
	 * A dungeon's ward (a room filed the way a dungeon piece files its arena as it's built, off to one side of the
	 * platform): Soar won't lift anyone inside it, and a flier who comes in is set down gently.
	 */
	private static String wards(ClientGameTestContext context, TestSingleplayerContext world) {
		BoundingBox room = new BoundingBox(STAGE.getX() + 40, STAGE.getY() - 10, STAGE.getZ() - 10, STAGE.getX() + 60, STAGE.getY() + 30, STAGE.getZ() + 10);
		world.getServer().runOnServer(server -> DungeonWards.remember(player(server).level(), () -> List.of(room)));
		context.waitTicks(3);
		String inside = on(world, player -> {
			if (!DungeonWards.warded(player.level(), room.getCenter())) {
				return "the ward should be filed (test setup)";
			}
			player.teleportTo(player.level(), room.getCenter().getX() + 0.5, STAGE.getY() + 4, room.getCenter().getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			player.setOnGround(false);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			return Soar.soaring(player) || player.getAbilities().mayfly ? "Soar shouldn't lift anyone inside a ward" : null;
		});
		if (inside != null) {
			return inside;
		}
		String outside = on(world, player -> {
			player.teleportTo(player.level(), room.minX() - 8.5, STAGE.getY() + 4, room.getCenter().getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
			player.setOnGround(false);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			if (!Soar.soaring(player)) {
				return "outside the ward Soar should lift as usual";
			}
			fly(player);
			player.teleportTo(player.level(), room.minX() + 3.5, STAGE.getY() + 4, room.getCenter().getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			return null;
		});
		if (outside != null) {
			return outside;
		}
		context.waitTicks(SoarRules.WARD_CHECK_TICKS + 3);
		return on(world, player -> {
			if (Soar.soaring(player) || player.getAbilities().mayfly) {
				return "a flier who comes into a ward should lose the flight";
			}
			return Soar.descending(player) ? null : "set down by a ward, the player should still come down gently";
		});
	}

	/**
	 * A flight saved in the middle of a crash (mayfly still on, the flight already over) is tidied as the
	 * player logs in: flight taken back, speed put back, and a gentle descent. One from another world's clock
	 * too.
	 */
	private static String crashTidied(ClientGameTestContext context, TestSingleplayerContext world) {
		return on(world, player -> {
			hover(player, 6);
			long now = player.level().getGameTime();
			player.getAbilities().mayfly = true;
			player.getAbilities().setFlyingSpeed(SoarRules.FLY_SPEED);
			player.setAttached(WildercordAttachments.SOARING, new Soaring(now - 5, SoarRules.DEFAULT_FLY_SPEED, false));
			Soar.login(player);
			if (player.getAbilities().mayfly || !Soar.descending(player)) {
				return "a flight that ran out during a crash should be taken back at login (mayfly " + player.getAbilities().mayfly + ")";
			}
			if (Math.abs(player.getAbilities().getFlyingSpeed() - SoarRules.DEFAULT_FLY_SPEED) > 1.0E-5) {
				return "its speed should be put back (" + player.getAbilities().getFlyingSpeed() + ")";
			}
			player.getAbilities().mayfly = true;
			player.setAttached(WildercordAttachments.SOARING, new Soaring(now + 5_000_000L, SoarRules.DEFAULT_FLY_SPEED, false));
			Soar.login(player);
			if (player.getAbilities().mayfly || !Soar.descending(player)) {
				return "a flight from another world's clock shouldn't be trusted at login";
			}
			player.onUpdateAbilities();
			return null;
		});
	}

	/** Dying mid-flight: the new body has no flight, no note of one and the usual speed. */
	private static String death(ClientGameTestContext context, TestSingleplayerContext world) {
		String up = on(world, player -> {
			hover(player, 10);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			if (!Soar.soaring(player)) {
				return "the flight should have begun before dying (test setup)";
			}
			fly(player);
			player.kill(player.level());
			return null;
		});
		if (up != null) {
			return up;
		}
		context.waitTicks(5);
		context.runOnClient(mc -> {
			mc.player.respawn();
			mc.gui.setScreen(null);
		});
		context.waitTicks(20);
		String after = on(world, player -> {
			if (player.getAbilities().mayfly || player.getAbilities().flying) {
				return "after dying mid-flight the player shouldn't be able to fly";
			}
			if (player.hasAttached(WildercordAttachments.SOARING)) {
				return "after dying nothing of the flight should be left";
			}
			return Math.abs(player.getAbilities().getFlyingSpeed() - SoarRules.DEFAULT_FLY_SPEED) > 1.0E-5
				? "after dying the flying speed should be the usual one (" + player.getAbilities().getFlyingSpeed() + ")" : null;
		});
		if (after != null) {
			return after;
		}
		return context.computeOnClient(mc -> mc.player.getAbilities().mayfly) ? "the client shouldn't think it can fly after respawning" : null;
	}

	/** Before the save: soaring, hovering 20 blocks up, with 5 seconds of flight left. */
	private static String beforeTheSave(ClientGameTestContext context, TestSingleplayerContext world) {
		String up = on(world, player -> {
			hover(player, 20);
			String cast = cast(player, Runes.SELF, Runes.SOAR);
			if (cast != null) {
				return cast;
			}
			if (!Soar.soaring(player)) {
				return "the flight should have begun before the save (test setup)";
			}
			fly(player);
			long now = player.level().getGameTime();
			Soaring note = player.getAttached(WildercordAttachments.SOARING);
			player.setAttached(WildercordAttachments.SOARING, new Soaring(now + 100, note.speed(), false));
			return null;
		});
		context.waitTicks(5);
		return up;
	}

	/** In the saved player: no flight (it was taken away as they left), but the note of it, to be given back. */
	private static String savedWithoutFlight(TestWorldSave save, UUID id) {
		Path file = save.getSaveDirectory().resolve("players/data/" + id + ".dat");
		if (!Files.exists(file)) {
			return "the saved player wasn't where it should be (" + file + ")";
		}
		try {
			CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			if (tag.getCompoundOrEmpty("abilities").getBooleanOr("mayfly", true)) {
				return "a player who logged out mid-flight should be saved unable to fly (abilities " + tag.getCompoundOrEmpty("abilities") + ")";
			}
			CompoundTag attachments = tag.getCompoundOrEmpty("fabric:attachments");
			return attachments.contains("wildercord:soaring") ? null
				: "the note of the flight should be saved with the player (attachments " + attachments.keySet() + ")";
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** After loading: the flight given back (hovering), then running out and a safe landing, and nothing left. */
	private static String afterTheSave(ClientGameTestContext context, TestSingleplayerContext world) {
		String resumed = on(world, player -> {
			if (!Soar.soaring(player) || !player.getAbilities().mayfly) {
				return "logging back in with time left should give the flight back (mayfly " + player.getAbilities().mayfly + ")";
			}
			if (Math.abs(player.getAbilities().getFlyingSpeed() - SoarRules.FLY_SPEED) > 1.0E-5) {
				return "the flight given back should be at Soar's speed";
			}
			return player.getAbilities().flying ? null : "logging back in up in the air, the player should be flying, not falling";
		});
		if (resumed != null) {
			return resumed;
		}
		world.getServer().waitFor(server -> !player(server).getAbilities().mayfly, 200);
		world.getServer().waitFor(server -> player(server).onGround(), 600);
		context.waitTicks(10);
		return on(world, player -> {
			if (player.getAbilities().mayfly) {
				return "after the flight ran out the player shouldn't be able to fly";
			}
			if (player.hasAttached(WildercordAttachments.SOARING)) {
				return "once down, nothing of the flight should be left";
			}
			return player.getHealth() < player.getMaxHealth() ? "the landing after a reload should do no damage (health " + player.getHealth() + ")" : null;
		});
	}

	/**
	 * A second player on the caster's team. Fabric's fake players never belong to a team (their team is looked up
	 * by the name on their profile, which a mod's fake player shouldn't share), so this one looks its team up as
	 * a real player does.
	 */
	private static final class Teammate extends FakePlayer {
		Teammate(ServerLevel level, GameProfile profile) {
			super(level, profile);
		}

		@Override
		public PlayerTeam getTeam() {
			return level().getScoreboard().getPlayersTeam(getScoreboardName());
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
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 2) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
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

	/** On the middle of the platform, facing south, level, whole and clean. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.resetFallDistance();
	}

	/** {@code dy} blocks above the middle of the platform, not moving, and counted as off the ground. */
	private static void hover(ServerPlayer player, double dy) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY() + dy, STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 10.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setOnGround(false);
		player.resetFallDistance();
	}

	/** Flying (what a double-tap of jump does on the client: the take-off itself is checked once, for real). */
	private static void fly(ServerPlayer player) {
		player.getAbilities().flying = true;
		player.onUpdateAbilities();
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

	/** {@code caster} lands {@code effect} (a harmful one, from a Bolt) on {@code target}. */
	private static void pull(LivingEntity caster, LivingEntity target, RuneDef effect) {
		SpellPlan.EffectNode node = node(Runes.BOLT, effect);
		Vec3 at = target.getBoundingBox().getCenter();
		Effects.apply(new Cast(caster), node, new Cast.Hit(List.<Entity>of(target), at, target.position().subtract(caster.position()).normalize(),
			caster.position(), null, null, false));
	}

	private static SpellPlan.EffectNode node(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes)).root().groups.getFirst().effects.getFirst();
	}

	private static String cast(ServerPlayer player, RuneDef... runes) {
		List<String> ids = java.util.Arrays.stream(runes).map(RuneDef::id).toList();
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids);
		if (!Spellbooks.get(player).spells().getFirst().equals(ids)) {
			return "the spell didn't thread";
		}
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, Mana.max(player));
		player.setAttached(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Soar.stop(player);
			// Each check starts fresh; keep rest within a check, not across independent checks.
			player.removeAttached(WildercordAttachments.SOAR_REST);
			player.setGameMode(GameType.SURVIVAL);
			player.getAbilities().setFlyingSpeed(SoarRules.DEFAULT_FLY_SPEED);
			player.onUpdateAbilities();
			stand(player);
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		world.getServer().runCommand("time set 6000");
		context.waitTicks(20);
	}

	private static void hideHud(net.minecraft.client.Minecraft mc) {
		if (!mc.gui.hud.isHidden()) {
			mc.gui.hud.toggle();
		}
	}

	private static <T> T on(TestSingleplayerContext world, java.util.function.Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static String f(double value) {
		return String.format("%.2f", value);
	}
}
