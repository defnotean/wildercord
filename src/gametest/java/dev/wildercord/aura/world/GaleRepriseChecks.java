package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Real server AI owns admission, each movement tick, release, and the complete exposed recovery. */
final class GaleRepriseChecks {
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private enum Answer {
		HOLD, EARLY_SIDESTEP, REPLY_SIDESTEP, PARRY, HELD_GUARD, FORESIGHT,
		STEP_COVER, REPLY_COVER, REMOVED_COVER, WALL, GROUND_GAP, INTERRUPT, IMPULSE, NO_AI, LEAVE, TARGET_DEATH, DEATH
	}
	private static final float HEALTH = 200;
	private final List<Challenger> party = new ArrayList<>();
	private SwordMaster master;
	private Challenger target, bystander;
	private ServerLevel level;
	private Vec3 origin, stopped, defendedPosition;
	private long began, cancelledAt;
	private float aimYaw;
	private RuneBolt threat;

	void run(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
				world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
				level = observer.level();
				origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				for (int x = -28; x <= 48; x++) for (int z = -28; z <= 28; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				observer.setGameMode(GameType.SPECTATOR);
				place(observer, 8, 8);
			});
			for (Answer answer : Answer.values()) scenario(world, answer);
			blockedAdmission(world);
			landingDisplacement(world);
			resourceAndCooldown(world);
		}
	}

	private void scenario(TestSingleplayerContext world, Answer answer) {
		int count = answer == Answer.HOLD ? MastersRules.MAX_PARTICIPANTS : answer == Answer.LEAVE || answer == Answer.TARGET_DEATH ? 2 : 1;
		beginNaturally(world, count);
		at(world, GaleRepriseRules.GATHER - 1, () -> {
			check(master.position().distanceToSqr(origin) < .001 && target.getHealth() == HEALTH, "Eight gather ticks precede all movement and harm");
			check(GaleReprise.prepare(master, target, MastersRules.GALE, 1, 100, level.getGameTime(), 0) == null,
				"Another Reprise cannot be prepared over an existing committed attack");
			if (answer == Answer.EARLY_SIDESTEP) place(target, 3, 4);
		});
		at(world, GaleRepriseRules.GATHER, () -> assertStep(1));
		at(world, GaleRepriseRules.GATHER + 1, () -> {
			assertStep(2);
			switch (answer) {
				case WALL -> stepWall(true);
				case GROUND_GAP -> landingFloor(false);
				case STEP_COVER, REMOVED_COVER -> replyCover(true);
				case INTERRUPT -> Effects.withSource(target, () -> check(master.interruptWindup(), "An enrolled early interrupt cancels Reprise"));
				case IMPULSE -> master.setDeltaMovement(.4, master.getDeltaMovement().y, 0);
				case NO_AI -> master.setNoAi(true);
				case LEAVE -> place(target, 40, 0);
				case TARGET_DEATH -> { target.setHealth(0); target.die(level.damageSources().generic()); }
				case DEATH -> { master.setHealth(0); master.die(level.damageSources().generic()); }
				default -> {}
			}
			stopped = master.position();
			cancelledAt = began + GaleRepriseRules.GATHER + (answer == Answer.INTERRUPT || answer == Answer.NO_AI || answer == Answer.DEATH ? 1 : 2);
		});
		at(world, GaleRepriseRules.GATHER + 2, () -> {
			if (cancelled(answer)) assertCancelled(answer);
			else assertStep(3);
		});
		at(world, GaleRepriseRules.GATHER + 3, () -> { if (!cancelled(answer)) assertStep(4); });
		at(world, GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS, () -> {
			if (cancelled(answer)) return;
			check(master.reprisePending() && target.getHealth() == HEALTH, "The landing begins a separate harmless reply tell");
			check(master.position().distanceToSqr(origin.add(-1.8, 0, 0)) < .003, "Four native steps land exactly 1.8 blocks to the accepted side");
			check(Math.abs(master.getYRot() - aimYaw) < .1, "Moving the target never re-aims the accepted reply");
			if (answer == Answer.REPLY_SIDESTEP) place(target, 3, 4);
			if (answer == Answer.REPLY_COVER) replyCover(true);
			if (answer == Answer.REMOVED_COVER) replyCover(false);
			if (answer == Answer.HELD_GUARD) guard();
			if (answer == Answer.FORESIGHT) {
				CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
				defendedPosition = target.position();
			}
		});
		at(world, GaleRepriseRules.TELL - 1, () -> {
			if (answer != Answer.TARGET_DEATH) check(target.getHealth() == HEALTH, "Neither phase harms a challenger before the full 22-tick tell: " + answer);
			if (answer == Answer.PARRY) guard();
		});
		at(world, GaleRepriseRules.TELL, () -> {
			if (answer == Answer.HOLD) {
				check(close(HEALTH - target.getHealth(), 26), "The captured target takes the one ordinary 26-damage reply");
				assertSource();
			} else if (answer == Answer.HELD_GUARD) {
				check(target.getHealth() < HEALTH && target.getHealth() > HEALTH - 26, "A held frontal Aura Guard reduces the ordinary projected hit");
				assertSource();
			} else if (answer == Answer.FORESIGHT) {
				check(close(HEALTH - target.getHealth(), 14) && target.position().distanceToSqr(defendedPosition) > .5,
					"Ordinary Foresight performs its native dodge and stops its capped 12 damage");
				assertSource();
			} else if (answer != Answer.TARGET_DEATH) check(target.getHealth() == HEALTH, "The defensive answer prevents the reply: " + answer);
			check(!master.reprisePending() && !master.state(AuraFighter.WINDUP), "Every attempt consumes its one pending strike");
			float health = target.getHealth();
			master.customServerAiStep(level);
			check(target.getHealth() == health, "Repeated same-tick AI cannot duplicate the reply");
			check(bystander.getHealth() == HEALTH, "An unaccepted body in the marked lane remains unharmed");
			for (Challenger guest : party) if (guest != target) check(guest.getHealth() == HEALTH,
				"Other enrolled bodies cannot multiply or inherit a captured-target reply");
			check(close(master.auraRemaining(), 60), "The Crescent and every completed, cancelled, or baited Reprise retain their 16 + 24 Aura payments");
			if (answer == Answer.NO_AI) master.setNoAi(false);
			if (answer == Answer.HOLD) threat = incomingThreat();
		});
		int recoveryEnd = cancelled(answer) ? (int) (cancelledAt - began) + GaleRepriseRules.RECOVERY : GaleRepriseRules.TELL + GaleRepriseRules.RECOVERY;
		at(world, recoveryEnd - 1, () -> {
			check(!master.reprisePending() && !master.state(AuraFighter.WINDUP) && !master.guarding() && !master.state(AuraFighter.DASH),
				"The complete 32-tick recovery stays exposed after release or cancellation: " + answer);
			check(close(master.auraRemaining(), 60), "Recovery neither refunds Aura nor spends it on another attack, guard, or dodge");
			if (answer != Answer.DEATH) {
				check(!master.canBeginReprise(level.getGameTime()), "A recovery frame is never a free Reprise admission");
				check(GaleReprise.prepare(master, target, MastersRules.GALE, 1, 100, level.getGameTime(), 0) == null,
					"Even an otherwise eligible request cannot replace the promised recovery");
			}
			if (answer == Answer.HOLD) {
				check(threat != null && !threat.isRemoved() && threat.hostileSpellTo(master), "A real admitted hostile Bolt remains incoming throughout recovery");
				check(master.position().distanceToSqr(origin.add(-1.8, 0, 0)) < .003, "An incoming threat cannot cancel recovery into evasive motion");
			}
		});
		world.getServer().runOnServer(server -> cleanup());
	}

	/** No private-state edits, forced sequence changes, manual AI advancement, or NoAI-assisted admission. */
	private void beginNaturally(TestSingleplayerContext world, int count) {
		completeOpening(world, count, () -> {});
		world.getServer().waitFor(server -> {
			if (!master.reprisePending()) return false;
			check(master.attackAnimation() == 7 && master.attackAnimation() == MasterAnimationRules.CROSSWIND_REPRISE
				&& master.attackElapsed(0) <= 1 && master.attackTellTicks() == 22,
				"Ordinary AI admits sequence one's synchronized ID 7 Reprise with its exact tell");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			aimYaw = master.getYRot();
			check(Math.abs(aimYaw - Math.toDegrees(Math.atan2(-1.8, 4))) < .1,
				"Accepted aim already points from the predicted landing to the original target before departure");
			check(master.onGround() && !master.isNoAi() && !master.getMoveControl().hasWanted(), "Admission grounds and brakes the actual AI body");
			check(master.challengerCount() == count && !master.canHarmParticipant(bystander) && close(master.auraRemaining(), 60),
				"The locked consenting roster and one 24-Aura payment are preserved at admission");
			return true;
		}, 4);
	}

	private void completeOpening(TestSingleplayerContext world, int count, Runnable beforeReady) {
		world.getServer().runOnServer(server -> setup(count));
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP)) return false;
			check(master.attackAnimation() == MastersRules.Move.CRESCENT.ordinal() + 1 && master.attackElapsed(0) <= 1,
				"Fresh Gale ordinary AI begins sequence zero with Crescent");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			return true;
		}, 45);
		at(world, MastersRules.Move.CRESCENT.tell, () -> {
			check(!master.state(AuraFighter.WINDUP) && close(master.auraRemaining(), 84), "The opening Crescent really completes and spends its normal Aura");
		});
		at(world, MastersRules.Move.CRESCENT.tell + MastersRules.Move.CRESCENT.recovery - 1, () -> {
			for (Challenger guest : party) { guest.setHealth(HEALTH); place(guest, guest == target ? 0 : .2, 4); }
			place(bystander, -.2, 4); bystander.setHealth(HEALTH);
			master.setTarget(target); // Select the next test challenger only between completed attacks.
			check(!master.canBeginReprise(level.getGameTime()), "Crescent recovery also prevents early Reprise admission");
			beforeReady.run();
		});
	}

	private void blockedAdmission(TestSingleplayerContext world) {
		for (int obstruction = 0; obstruction < 4; obstruction++) {
			int kind = obstruction;
			completeOpening(world, 1, () -> {
				if (kind == 0) stepWall(true);
				else if (kind == 1) landingFloor(false);
				else place(target, 0, kind == 2 ? 2.4 : 4.9);
			});
			at(world, MastersRules.Move.CRESCENT.tell + MastersRules.Move.CRESCENT.recovery, () -> {
				check(!master.reprisePending() && ordinaryMove(master) != null
					&& close(master.auraRemaining(), 68) && master.position().distanceToSqr(origin) < .003,
					"A blocked complete path or out-of-band target declines Reprise before payment or sideways movement: " + kind);
				cleanup();
			});
		}
	}

	private void landingDisplacement(TestSingleplayerContext world) {
		beginNaturally(world, 1);
		at(world, GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS - 1, () -> {
			assertStep(4);
			master.move(MoverType.SELF, new Vec3(0, 0, .5));
			check(master.position().distanceToSqr(origin.add(-1.8, 0, .5)) < .003, "A real external displacement changes the final landing after its last step");
		});
		at(world, GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS, () -> {
			check(!master.reprisePending() && master.attackAnimation() == 0 && close(master.auraRemaining(), 60),
				"The reply refuses a displaced final landing instead of inventing a new warned origin");
		});
		at(world, GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS + GaleRepriseRules.RECOVERY - 1, () -> {
			check(target.getHealth() == HEALTH && !master.state(AuraFighter.WINDUP) && !master.guarding() && close(master.auraRemaining(), 60),
				"Final-landing cancellation preserves its harmless, fully paid 32-tick recovery");
			cleanup();
		});
	}

	private void resourceAndCooldown(TestSingleplayerContext world) {
		beginNaturally(world, 1);
		at(world, GaleRepriseRules.GATHER + 1, () -> Effects.withSource(target,
			() -> check(master.interruptWindup(), "The resource probe cancels a naturally paid Reprise")));
		at(world, GaleRepriseRules.GATHER + GaleRepriseRules.RECOVERY, () -> {
			check(!master.state(AuraFighter.WINDUP) && close(master.auraRemaining(), 60), "Cancelled Reprise retains its full paid recovery through the last frame");
		});
		at(world, GaleRepriseRules.GATHER + 1 + GaleRepriseRules.RECOVERY, () -> {
			check(ordinaryMove(master) != null && master.attackElapsed(0) == 0
				&& !master.reprisePending() && close(master.auraRemaining(), 44),
				"The next still-eligible sequence uses an admitted ordinary attack because the paid Reprise's 140-tick cooldown survives cancellation");
		});
		world.getServer().waitFor(server -> {
			if (master.auraRemaining() >= GaleRepriseRules.COST) return false;
			check(close(master.auraRemaining(), 16) && ordinaryMove(master) != null,
				"Two admitted ordinary moves and the scheduled guard spend the remaining finite Aura");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			return true;
		}, 120);
		MastersRules.Move exhaustedMove = ordinaryMove(master);
		at(world, exhaustedMove.tell + exhaustedMove.recovery, () -> {
			check(close(master.auraRemaining(), 16) && !master.state(AuraFighter.WINDUP) && !master.guarding() && !master.reprisePending(),
				"Exhausted ordinary AI enters exposed breathing instead of borrowing Aura for another form");
		});
		at(world, exhaustedMove.tell + exhaustedMove.recovery + 10, () -> {
			check(master.auraRemaining() > 16 && !master.state(AuraFighter.WINDUP) && !master.guarding(), "Native breathing gradually restores the exhausted resource");
			cleanup();
		});
	}

	private static MastersRules.Move ordinaryMove(SwordMaster master) {
		return MasterMoveCatalog.legacy().byWireId(master.attackAnimation()).map(MasterMoveCatalog.Definition::legacyMove)
			.filter(move -> move == MastersRules.Move.SWEEP || move == MastersRules.Move.THRUST || move == MastersRules.Move.CRESCENT).orElse(null);
	}

	private void setup(int count) {
		target = add("RepriseTarget", 0, 4); party.add(target);
		for (int i = 1; i < count; i++) party.add(add("RepriseAlly" + i, 6, -6 + i * 1.5));
		bystander = add("RepriseBystander", -6, 4);
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(master != null, "The Gale fixture is constructible");
		master.setDiscipline(MastersRules.GALE); master.snapTo(origin.x, origin.y, origin.z, 0, 0);
		level.addFreshEntity(master);
		for (Challenger player : party) {
			master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND);
		}
		check(SwordMaster.ready(target) == 1, "Each participant explicitly consents before the ordinary-AI trial starts");
		master.setTarget(target);
	}

	private void assertStep(int step) {
		check(master.reprisePending() && master.state(AuraFighter.WINDUP) && target.getHealth() == HEALTH, "Every native lateral step remains harmless");
		check(master.position().distanceToSqr(origin.add(-.45 * step, 0, 0)) < .003,
			"Native movement step " + step + " advances exactly 0.45 blocks, without a teleport or forward chase");
		check(Math.abs(master.getYRot() - aimYaw) < .1, "The body retains its pre-departure reply aim during footwork");
	}

	private void assertCancelled(Answer answer) {
		check(!master.reprisePending() && master.attackAnimation() == 0 && master.getDeltaMovement().horizontalDistanceSqr() < .00001,
			"Cancellation clears the warning, later strike, and horizontal velocity: " + answer);
		if (answer != Answer.IMPULSE && answer != Answer.DEATH) check(master.position().distanceToSqr(stopped) < .02, "A cancelled body cannot continue the lateral step: " + answer);
		if (answer == Answer.LEAVE || answer == Answer.TARGET_DEATH) check(master.challengerCount() == 2 && master.canHarmParticipant(party.get(1)),
			"A surviving enrolled challenger cannot inherit the captured target's cancelled strike");
	}

	private void guard() {
		target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
		target.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		target.setShiftKeyDown(true);
		Vec3 facing = master.position().subtract(target.position());
		float yaw = (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
		target.setYRot(yaw); target.setYHeadRot(yaw); target.setYBodyRot(yaw); target.setXRot(0);
		check(AuraGuard.raise(target), "The native Aura Guard raise accepts and pays for the fixture");
		check(target.isShiftKeyDown() && AuraGuard.guarding(target), "The fake challenger's native guard remains held");
		check(AuraGuard.facing(target, master.position()), "The challenger's head and body face the landed Master's damage source");
	}

	private RuneBolt incomingThreat() {
		var group = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
		Vec3 from = master.getBoundingBox().getCenter().add(5, 0, 0);
		RuneBolt bolt = RuneBolt.launch(new Cast(target), group, null, from, new Vec3(-1, 0, 0), false);
		check(bolt != null && bolt.hostileSpellTo(master), "A real enrolled caster supplies the incoming recovery threat");
		bolt.setDeltaMovement(-.01, 0, 0); // Stay in the native eight-block threat sensor without reaching the body during this window.
		return bolt;
	}

	private Challenger add(String name, double x, double z) {
		Challenger player = new Challenger(level, name); player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 180, 0);
		player.setYHeadRot(180); player.setYBodyRot(180); level.addNewPlayer(player); return player;
	}
	private void place(ServerPlayer player, double x, double z) {
		player.teleportTo(level, origin.x + x, origin.y, origin.z + z, Set.of(), 180, 0, false);
		player.setYHeadRot(180); player.setYBodyRot(180); player.setDeltaMovement(Vec3.ZERO);
	}
	private void replyCover(boolean present) {
		for (int x = -3; x <= 1; x++) for (int y = 0; y <= 2; y++)
			level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, y, 2), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void stepWall(boolean present) {
		for (int z = -1; z <= 1; z++) for (int y = 0; y <= 2; y++)
			level.setBlockAndUpdate(BlockPos.containing(origin).offset(-2, y, z), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void landingFloor(boolean present) {
		level.setBlockAndUpdate(BlockPos.containing(origin).offset(-2, -1, 0), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void cleanup() {
		if (threat != null) { threat.discard(); threat = null; }
		master.discard(); for (Challenger player : party) player.discard(); party.clear(); bystander.discard();
		replyCover(false); stepWall(false); landingFloor(true);
	}
	private void assertSource() {
		check(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == master, "Reply damage retains the actual enrolled trial source");
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			long now = level.getGameTime(); if (now < expected) return false;
			check(now == expected, "Observe exact native Reprise frame " + age + ": expected=" + expected + ", actual=" + now);
			action.run(); return true;
		}, age + 20);
	}
	private static boolean cancelled(Answer answer) { return answer.ordinal() >= Answer.WALL.ordinal(); }
	private static boolean close(double a, double b) { return Math.abs(a - b) < .01; }
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
