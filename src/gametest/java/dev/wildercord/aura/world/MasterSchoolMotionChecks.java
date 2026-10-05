package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Native spectator footage of ordinary AI and a consenting Fabric FakePlayer, not a human duel. */
public final class MasterSchoolMotionChecks {
	private static final float HEALTH = 200;
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "MotionChallenger")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	public record ServerFrame(long gameTick, long acceptedTick, int attackId, float age, boolean noAi,
		boolean pending, boolean windup, boolean guarding, int participants, double aura, float targetHealth,
		Vec3 masterPosition, Vec3 targetPosition) {}
	private record Beat(String name, int age, int requiredWarningSegments) {}
	private ServerLevel level;
	private Vec3 origin;
	private SwordMaster master;
	private Challenger target;
	private long began;

	public void run(ClientGameTestContext context) {
		var saved = context.computeOnClient(MastersNpcCaptureProbe.Options::save);
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false",
				"gamerule advance_time false", "time set 3000", "weather clear")) world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
				level = observer.level();
				origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				observer.setGameMode(GameType.SPECTATOR);
				Vec3 camera = origin.add(7, 3, 7), focus = origin.add(-.6, .8, 1.8);
				Vec3 look = focus.subtract(camera.add(0, observer.getEyeHeight(), 0));
				float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
				float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
				observer.teleportTo(level, camera.x, camera.y, camera.z, Set.of(), yaw, pitch, false);
			});
			context.runOnClient(MastersNpcCaptureProbe::configure);
			context.waitTicks(10);
			world.getConnection().waitForChunksRender();
			for (int school : new int[] {MastersRules.GALE, MastersRules.STONE}) {
				boolean gale = school == MastersRules.GALE;
				List<Beat> beats = gale
					? List.of(new Beat("gather", 4, 2), new Beat("step", 9, 2), new Beat("reply_warning", 16, 3),
						new Beat("release", GaleRepriseRules.TELL, 0), new Beat("recovery", 38, 0))
					: List.of(new Beat("plant", 4, 1), new Beat("brace", 12, 1), new Beat("reply_warning", 25, 3),
						new Beat("release", StoneFractureRules.TELL, 0), new Beat("recovery", 52, 0));
				var poses = new ArrayList<float[]>();
				for (Beat beat : beats) {
					// A fresh natural trial for every readback prevents GPU latency from consuming later beats.
					try {
						beginNaturally(world, school);
						int id = master.getId(), attack = gale ? 7 : 8;
						CompletableFuture<MastersNpcCaptureProbe.Evidence> shot = null;
						for (int tick = 0; tick < beat.age + 10; tick++) {
							ServerFrame serverFrame = world.getServer().computeOnServer(server -> observe(school));
							shot = context.computeOnClient(mc -> {
								check(!mc.isPaused() && mc.gui.screen() == null && mc.player.isSpectator(), "The real observer stays unpaused and outside the roster");
								if (!(mc.level.getEntity(id) instanceof SwordMaster live) || live.attackAnimation() != attack) return null;
								float age = live.attackElapsed(MastersNpcCaptureProbe.PARTIAL);
								if (age < beat.age) return null;
								check(age < beat.age + 1, "Never substitute a neighboring phase for " + beat.name + ": " + age);
								check(mc.level.getGameTime() - (long) live.attackElapsed(0) == began, "Rendered attack must retain the naturally accepted server start");
								String name = "masters_npc_" + (gale ? "gale_crosswind" : "stone_fracture") + "_" + beat.name;
								return MastersNpcCaptureProbe.capture(mc, live, name, beat.name, beat.age, beat.requiredWarningSegments, origin, serverFrame);
							});
							if (shot != null) break;
							context.waitTick();
						}
						check(shot != null, "Native client must observe the requested phase");
						// Observe release before waiting for readback; GPU latency may advance real ticks.
						// A capture does not replace the real release/damage contract.
						world.getServer().waitFor(server -> {
							if (level.getGameTime() < began + (gale ? GaleRepriseRules.TELL : StoneFractureRules.TELL)) return false;
							observe(school);
							return true;
						}, 80);
						var pending = shot;
						context.waitFor(mc -> pending.isDone(), 40);
						poses.add(shot.join().modelPose());
					} finally { world.getServer().runOnServer(server -> cleanup()); }
					context.waitTicks(5);
				}
				check(different(poses.get(0), poses.get(3)) && different(poses.get(3), poses.get(4)),
					"Actual native model transforms must distinguish warning, release and recovery");
			}
		} finally {
			context.runOnClient(mc -> { MastersNpcCaptureProbe.end(); saved.restore(mc); });
		}
	}

	private void beginNaturally(TestSingleplayerContext world, int school) {
		world.getServer().runOnServer(server -> {
			target = new Challenger(level);
			target.setGameMode(GameType.SURVIVAL);
			target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
			target.setHealth(HEALTH);
			target.snapTo(origin.x, origin.y, origin.z + 4, 180, 0);
			target.setYHeadRot(180); target.setYBodyRot(180);
			level.addNewPlayer(target);
			master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
			check(master != null, "The registered master must exist");
			master.setDiscipline(school);
			master.snapTo(origin.x, origin.y, origin.z, 0, 0);
			level.addFreshEntity(master);
			master.mobInteract(target, InteractionHand.MAIN_HAND);
			master.mobInteract(target, InteractionHand.MAIN_HAND);
			check(SwordMaster.ready(target) == 1, "The fake challenger explicitly accepts the native trial");
			master.setTarget(target);
		});
		var opening = school == MastersRules.GALE ? MastersRules.Move.CRESCENT : MastersRules.Move.THRUST;
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP)) return false;
			check(master.attackAnimation() == opening.ordinal() + 1 && master.attackElapsed(0) <= 1,
				"Ordinary AI must select its real sequence-zero opening");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			return true;
		}, 45);
		long resetAt = began + opening.tell + opening.recovery - 1;
		world.getServer().waitFor(server -> {
			if (level.getGameTime() < resetAt) return false;
			check(level.getGameTime() == resetAt && !master.state(AuraFighter.WINDUP)
				&& Math.abs(master.auraRemaining() - 84) < .01, "Opening and its paid recovery must complete naturally");
			// Challenger fixture placement only between completed attacks, never inside a captured form.
			target.setHealth(HEALTH);
			target.teleportTo(level, origin.x, origin.y, origin.z + 4, Set.of(), 180, 0, false);
			target.setDeltaMovement(Vec3.ZERO);
			master.setTarget(target);
			return true;
		}, opening.tell + opening.recovery + 10);
		world.getServer().waitFor(server -> {
			if (!(school == MastersRules.GALE ? master.reprisePending() : master.fracturePending())) return false;
			check(master.attackAnimation() == (school == MastersRules.GALE ? 7 : 8) && master.attackElapsed(0) <= 1,
				"Ordinary AI must naturally admit the school form");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			return true;
		}, 4);
	}

	private ServerFrame observe(int school) {
		boolean gale = school == MastersRules.GALE;
		int tell = gale ? GaleRepriseRules.TELL : StoneFractureRules.TELL;
		long age = level.getGameTime() - began;
		check(master.isAlive() && !master.isNoAi() && master.started() && master.challengerCount() == 1,
			"Live native AI and the single accepted participant must own every frame");
		check(Math.abs(master.auraRemaining() - (gale ? 60 : 56)) < .01, "The real opening and special-form Aura costs remain paid");
		check(master.attackAnimation() == (gale ? 7 : 8), "The captured form must not be cancelled or replaced");
		check((gale ? master.reprisePending() : master.fracturePending()) == (age < tell), "Real pending state must match the promised release");
		check(master.state(AuraFighter.WINDUP) == (age < tell), "Windup ends on the exact native release tick");
		check(master.guarding() == (!gale && age >= StoneFractureRules.PLANT && age < StoneFractureRules.PLANT + StoneFractureRules.BRACE),
			"Only Stone's ordinary brace holds guard");
		Vec3 expected = origin.add(gale ? -GaleRepriseRules.STEP_DISTANCE * GaleRepriseRules.travelFraction(age) : 0, 0, 0);
		check(master.position().distanceToSqr(expected) < .003, "The server body's real step or stationary brace must match its accepted geometry");
		float damage = gale ? 26 : 30.8F;
		check(Math.abs(target.getHealth() - (age < tell ? HEALTH : HEALTH - damage)) < .02,
			"The challenger takes one ordinary projected reply only at release");
		if (age >= tell) check(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == master,
			"The real reply retains the enrolled master as damage source");
		return new ServerFrame(level.getGameTime(), began, master.attackAnimation(), master.attackElapsed(0), master.isNoAi(),
			gale ? master.reprisePending() : master.fracturePending(), master.state(AuraFighter.WINDUP), master.guarding(),
			master.challengerCount(), master.auraRemaining(), target.getHealth(), master.position(), target.position());
	}

	private void cleanup() {
		if (master != null) { master.discard(); master = null; }
		if (target != null) { target.discard(); target = null; }
	}
	private static boolean different(float[] a, float[] b) {
		for (int i = 0; i < a.length; i++) if (Math.abs(a[i] - b[i]) > .025F) return true;
		return false;
	}
	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
