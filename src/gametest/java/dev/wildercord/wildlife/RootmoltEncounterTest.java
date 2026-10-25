package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A Rootmolt's attack in the real game: a creative player is never taken as a target, a survival one gets the warning
 * (which sends a Sporeback beside it into its shell) and then the rake and a timed hold with the tether on them, the hold
 * lets go once they get clear, it rests before it can warn again, a hit from the player breaks a second hold, and its
 * attack rest is kept through a save and reopen with nobody left held.
 */
public final class RootmoltEncounterTest implements FabricClientGameTest {
	private static UUID rootmoltId, snailId;
	private static long rest;

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static RootmoltStrider rootmolt(MinecraftServer s) {
		return (RootmoltStrider) s.overworld().getEntity(rootmoltId);
	}

	private static SporebackSnail snail(MinecraftServer s) {
		return (SporebackSnail) s.overworld().getEntity(snailId);
	}

	/** Puts the player {@code dz} blocks south of wherever the Rootmolt has wandered, facing it. */
	private static void stand(MinecraftServer s, double dz) {
		var r = rootmolt(s);
		p(s).teleportTo(s.overworld(), r == null ? 0.5 : r.getX(), 101, (r == null ? 0.5 : r.getZ()) + dz, Set.<Relative>of(), 180, 20, false);
	}

	private static boolean held(MinecraftServer s) {
		return p(s).hasEffect(RootmoltContent.TETHER)
			|| !p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER, RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER);
	}

	/** Waits a tick at a time, up to {@code ticks}, for {@code test} to hold on the server. */
	private static boolean await(ClientGameTestContext c, TestServerContext server, Predicate<MinecraftServer> test, int ticks) {
		for (int i = 0; i < ticks; i++) {
			if (server.computeOnServer(test::test)) return true;
			c.waitTicks(1);
		}
		return server.computeOnServer(test::test);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			TestServerContext server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("difficulty normal");
			server.runCommand("time set 18000");
			server.runOnServer(s -> {
				var l = s.overworld();
				for (int x = -8; x <= 8; x++) for (int z = -8; z <= 12; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.CREATIVE);
				p(s).addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000));
				p(s).teleportTo(l, 0.5, 101, 2.5, Set.<Relative>of(), 180, 20, false);
				RootmoltStrider rootmolt = RootmoltContent.STRIDER.create(l, EntitySpawnReason.COMMAND);
				rootmolt.snapTo(0.5, 101, 0.5, 0, 0);
				rootmolt.setPersistenceRequired();
				l.addFreshEntity(rootmolt);
				rootmoltId = rootmolt.getUUID();
				rootmolt.setTarget(p(s));
			});

			// A creative player is never taken: no warning, and the target is dropped.
			c.waitTicks(25);
			server.runOnServer(s -> check(rootmolt(s).pose() == RootmoltStrider.IDLE && rootmolt(s).getTarget() == null,
				"A creative player is never warned or held, pose " + rootmolt(s).pose()));

			// A survival player two blocks ahead gets the warning, and a Sporeback beside it hides.
			server.runOnServer(s -> {
				var l = s.overworld();
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).setHealth(p(s).getMaxHealth());
				stand(s, 2);
				SporebackSnail snail = SporebackContent.SNAIL.create(l, EntitySpawnReason.COMMAND);
				snail.snapTo(rootmolt(s).getX() + 2, 101, rootmolt(s).getZ(), 0, 0);
				l.addFreshEntity(snail);
				snailId = snail.getUUID();
				check(snail.pose() != 2, "The Sporeback starts out of its shell");
				rootmolt(s).setTarget(p(s));
			});
			check(await(c, server, s -> rootmolt(s).pose() == RootmoltStrider.WARNING, 20), "The Rootmolt warns before it strikes");
			server.runOnServer(s -> {
				long now = s.overworld().getGameTime();
				rest = rootmolt(s).attackReady();
				check(rest > now && rest <= now + RootmoltRules.ATTACK_REST, "The warning starts its attack rest, " + (rest - now) + " ticks");
				check(!held(s), "A warning alone holds nobody");
			});

			// Then the rake and the hold: hurt, tethered and owned by this Rootmolt.
			check(await(c, server, s -> rootmolt(s).pose() == RootmoltStrider.HOLDING, RootmoltRules.WINDUP + RootmoltRules.RAKE + 10),
				"The rake catches a player standing in its line");
			server.runOnServer(s -> {
				check(rootmolt(s).holding(p(s)), "It holds the player it raked");
				check(p(s).hasEffect(RootmoltContent.TETHER), "The held player is tethered");
				check(p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER, RootmoltContent.NO_OWNER).equals(rootmoltId), "The hold names this Rootmolt");
				check(p(s).getHealth() < p(s).getMaxHealth(), "The rake hurts");
				check(snail(s).pose() == 2 && snail(s).hiddenUntil() > s.overworld().getGameTime(), "The warning sends the Sporeback beside it into its shell");
			});
			c.waitTicks(2);
			c.takeScreenshot(TestScreenshotOptions.of("rootmolt_hold").disableCounterPrefix());

			// Getting clear breaks the hold and the Rootmolt recovers.
			server.runOnServer(s -> stand(s, 7));
			check(await(c, server, s -> rootmolt(s).pose() == RootmoltStrider.RECOVERING, 5), "Getting clear ends the hold");
			server.runOnServer(s -> check(!held(s), "Out of its reach nobody is held"));

			// It settles back, and doesn't warn again before its rest is over.
			check(await(c, server, s -> rootmolt(s).pose() == RootmoltStrider.IDLE, RootmoltRules.RECOVERY + 5), "It settles after recovering");
			server.runOnServer(s -> {
				stand(s, 2);
				check(s.overworld().getGameTime() < rest, "The test reaches idle before the attack rest ends");
				rootmolt(s).setTarget(p(s));
			});
			c.waitTicks(20);
			server.runOnServer(s -> check(rootmolt(s).pose() == RootmoltStrider.IDLE && !held(s), "No second warning during the attack rest"));

			// Once rested it warns again; this time the player hits it and breaks the hold.
			check(await(c, server, s -> s.overworld().getGameTime() >= rest, RootmoltRules.ATTACK_REST), "The attack rest runs out");
			server.runOnServer(s -> {
				stand(s, 2);
				rootmolt(s).setTarget(p(s));
			});
			check(await(c, server, s -> rootmolt(s).pose() == RootmoltStrider.HOLDING, RootmoltRules.WINDUP + RootmoltRules.RAKE + 20),
				"After its rest it strikes again");
			server.runOnServer(s -> {
				var l = s.overworld();
				check(rootmolt(s).attackReady() > rest, "The new strike starts a new rest");
				rootmolt(s).hurtServer(l, p(s).damageSources().playerAttack(p(s)), 1);
				check(rootmolt(s).pose() == RootmoltStrider.RECOVERING && !held(s), "A hit from the player breaks the hold");
				check(rootmolt(s).getTarget() == p(s), "and turns it on whoever hit it");
				rest = rootmolt(s).attackReady();
				check(rest >= l.getGameTime() + RootmoltRules.ATTACK_REST - 1, "Breaking free leaves it a full rest");
			});
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(40);
			w.getServer().runOnServer(s -> {
				RootmoltStrider rootmolt = rootmolt(s);
				check(rootmolt != null, "The Rootmolt is there after a reopen");
				check(rootmolt.attackReady() == rest, "Its attack rest is kept through a save and reopen");
				check(rootmolt.pose() != RootmoltStrider.HOLDING && !held(s), "Nobody is held after a reopen");
			});
		}
	}
}
