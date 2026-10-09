package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
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

/** The actual server AI admits, braces, releases and recovers; fixture inputs never edit its private attack state. */
final class StoneFractureChecks {
	private static final class Challenger extends FakePlayer {
		private final List<String> messages = new ArrayList<>();
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
		@Override public void sendSystemMessage(Component message) {
			if (messages != null) messages.add(message.toString());
			super.sendSystemMessage(message);
		}
		boolean heard(String key) { return messages.stream().anyMatch(message -> message.contains(key)); }
	}
	private enum Answer {
		HOLD, EARLY_SIDESTEP, REPLY_SIDESTEP, BACKSTEP, PARRY, HELD_GUARD, FORESIGHT,
		WARNED_COVER, NEW_COVER, REMOVED_COVER, PLANT_REAR, FRONTAL_DAMAGE, FRONTAL_ZERO, REAR_ZERO,
		OUTSIDER_REAR, REPLY_REAR, LOCKED_REAR, LOCKED_INTERRUPT,
		REAR_DAMAGE, REAR_ABSORPTION, FRONTAL_KNOCKBACK, AXE, STANCE, EARLY_INTERRUPT, IMPULSE, DISPLACEMENT, NO_AI, LEAVE, TARGET_DEATH, DEATH
	}
	private static final float HEALTH = 200;
	private static final double REPLY_DAMAGE = 30.8;
	private final List<Challenger> party = new ArrayList<>();
	private SwordMaster master;
	private Challenger target, bystander;
	private ServerLevel level;
	private Vec3 origin, defendedPosition;
	private long began, cancelledAt;
	private float aimYaw, releasedHealth;
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
			resourcesAfterCancellation(world);
		}
	}

	private void scenario(TestSingleplayerContext world, Answer answer) {
		int count = answer == Answer.HOLD ? MastersRules.MAX_PARTICIPANTS : answer == Answer.LEAVE || answer == Answer.TARGET_DEATH ? 2 : 1;
		beginNaturally(world, count);
		at(world, StoneFractureRules.PLANT - 1, () -> {
			assertStationary();
			check(!master.guarding() && !master.perfectNow() && target.getHealth() == HEALTH, "The full eight-tick plant is exposed and harmless");
			check(StoneFracture.prepare(master, target, MastersRules.STONE, 1, 100, level.getGameTime(), 0) == null,
				"A committed Stone Fracture cannot accept another paid form");
			if (answer == Answer.EARLY_SIDESTEP) place(target, 3, 4);
			if (answer == Answer.PLANT_REAR || answer == Answer.FRONTAL_ZERO || answer == Answer.REAR_ZERO) {
				place(target, 0, -3);
				float before = master.getHealth();
				check(hit(target, 8, true) && master.getHealth() < before && master.fracturePending(),
					"A landed rear hit during the unguarded plant does not invent a brace cancellation");
				place(target, 0, 4);
			}
		});
		at(world, StoneFractureRules.PLANT, () -> {
			assertStationary();
			check(master.guarding() && !master.perfectNow() && master.fracturePending(), "Tick eight starts an ordinary half guard with no perfect retaliation");
			switch (answer) {
				case FRONTAL_DAMAGE -> {
					check(close(master.guarded(level, level.damageSources().playerAttack(target), 8), 4), "The actual frontal guard passes exactly half the input damage");
					float before = master.getHealth();
					check(hit(target, 8, true) && master.getHealth() < before && master.fracturePending(), "A landed frontal receipt retains the brace");
					check(!target.hasEffect(MobEffects.SLOWNESS) && !target.hasEffect(MobEffects.WEAKNESS), "The brace never retaliates with native perfect-guard stagger");
				}
				case FRONTAL_ZERO, REAR_ZERO -> {
					if (answer == Answer.REAR_ZERO) place(target, 0, -3);
					float before = master.getHealth();
					float absorption = master.getAbsorptionAmount();
					check(!hit(target, 8, false) && master.getHealth() == before && master.getAbsorptionAmount() == absorption,
						"Native invulnerability frames deny the repeated " + answer + " receipt");
					check(master.fracturePending() && master.guarding(), "Zero landed health or absorption damage cannot cancel the brace");
					place(target, 0, 4);
				}
				case OUTSIDER_REAR -> {
					place(bystander, 0, -3);
					float before = master.getHealth();
					DamageSource source = master.getLastDamageSource();
					Effects.withSource(target, () -> check(!hit(bystander, 8, true), "An enrolled ambient scope cannot launder a bystander's rear hit"));
					check(master.getHealth() == before && master.getLastDamageSource() == source && master.fracturePending(),
						"Rejected outsider provenance changes neither health, source nor commitment");
				}
				case REAR_DAMAGE, REAR_ABSORPTION -> {
					place(target, 0, -3);
					if (answer == Answer.REAR_ABSORPTION) {
						master.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);
						master.setAbsorptionAmount(20);
						check(close(master.getAbsorptionAmount(), 20), "The absorption-specific fixture has twenty real absorption health before the rear hit");
					}
					float before = master.getHealth(), absorption = master.getAbsorptionAmount();
					hit(target, 8, true);
					check(answer == Answer.REAR_DAMAGE ? master.getHealth() < before
						: master.getHealth() == before && master.getAbsorptionAmount() < absorption,
						"The enrolled rear counter really lands on " + answer);
					check(!master.fracturePending(), "A landed rear health or absorption receipt cancels immediately during brace");
					place(target, 0, 4);
				}
				case FRONTAL_KNOCKBACK -> {
					Effects.readyToHurt(master);
					float before = master.getHealth();
					check(master.hurtServer(level, level.damageSources().playerAttack(target), 8) && master.getHealth() < before
						&& master.fracturePending(), "A real frontal melee receipt does not trigger the brace's rear-hit cancellation");
					check(close(master.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), .65)
						&& master.getDeltaMovement().horizontalDistanceSqr() > .0025,
						"Default Stone knockback resistance leaves a real external impulse that cancels on the next native AI tick");
				}
				case AXE -> {
					target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
					Effects.readyToHurt(master);
					check(master.hurtServer(level, level.damageSources().playerAttack(target), 8) && !master.guarding() && !master.fracturePending(),
						"A native frontal axe hit breaks the guard and cancels the paid reply");
				}
				case STANCE -> {
					target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
					Effects.withSource(target, () -> check(Stance.wear(target, master, 10000, StanceRules.Source.BLOW) > 0,
						"An eligible enrolled attack wears the real stance pool through Stance.wear"));
					check(Stance.opened(master) && !master.fracturePending(), "The native stance break cancels the commitment instead of granting a reply");
				}
				case IMPULSE -> master.setDeltaMovement(.4, master.getDeltaMovement().y, 0);
				case DISPLACEMENT -> master.move(MoverType.SELF, new Vec3(.5, 0, 0));
				case NO_AI -> master.setNoAi(true);
				case LEAVE -> place(target, 40, 0);
				case TARGET_DEATH -> { target.setHealth(0); target.die(level.damageSources().generic()); }
				case DEATH -> { master.setHealth(0); master.die(level.damageSources().generic()); }
				default -> {}
			}
			cancelledAt = began + StoneFractureRules.PLANT + (answer == Answer.FRONTAL_KNOCKBACK || answer == Answer.IMPULSE || answer == Answer.DISPLACEMENT
				|| answer == Answer.LEAVE || answer == Answer.TARGET_DEATH ? 1 : 0);
		});
		at(world, StoneFractureRules.PLANT + 1, () -> {
			if (cancelled(answer) && answer != Answer.EARLY_INTERRUPT) assertCancelled(answer);
			else { assertStationary(); check(master.guarding() && target.getHealth() == HEALTH, "The native brace holds without striking early"); }
		});
		at(world, StoneFractureRules.PLANT + StoneFractureRules.BRACE - 1, () -> {
			if (cancelled(answer) && answer != Answer.EARLY_INTERRUPT) return;
			assertStationary();
			check(master.guarding() && master.fracturePending() && target.getHealth() == HEALTH, "The twelfth brace frame remains guarded and harmless");
			if (answer == Answer.WARNED_COVER || answer == Answer.REMOVED_COVER) replyCover(true);
		});
		at(world, StoneFractureRules.PLANT + StoneFractureRules.BRACE, () -> {
			if (cancelled(answer) && answer != Answer.EARLY_INTERRUPT) return;
			assertStationary();
			check(!master.guarding() && master.fracturePending() && target.getHealth() == HEALTH, "Tick twenty drops guard before the separate twelve-tick reply warning");
			if (answer == Answer.REPLY_SIDESTEP) place(target, 3, 4);
			if (answer == Answer.BACKSTEP) place(target, 0, 6.8);
			if (answer == Answer.NEW_COVER) replyCover(true);
			if (answer == Answer.REMOVED_COVER) replyCover(false);
			if (answer == Answer.HELD_GUARD) guard();
			if (answer == Answer.FORESIGHT) {
				CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
				defendedPosition = target.position();
			}
			if (answer == Answer.REPLY_REAR) {
				place(target, 0, -3);
				float before = master.getHealth();
				check(hit(target, 8, true) && master.getHealth() < before && master.fracturePending(), "A rear hit after brace ends cannot erase the separately warned reply");
				place(target, 0, 4);
			}
		});
		at(world, StoneFractureRules.TELL - MastersRules.AIM_LOCK - 1, () -> {
			if (answer == Answer.EARLY_INTERRUPT) {
				Effects.withSource(target, () -> check(master.interruptWindup(), "An enrolled interrupt still succeeds on the last early-tell frame"));
				cancelledAt = level.getGameTime();
				assertCancelled(answer);
			}
		});
		at(world, StoneFractureRules.TELL - MastersRules.AIM_LOCK, () -> {
			if (answer == Answer.LOCKED_INTERRUPT) Effects.withSource(target,
				() -> check(!master.interruptWindup() && master.fracturePending(), "The existing final-six-tick commitment rejects late interruption"));
			if (answer == Answer.LOCKED_REAR) {
				place(target, 0, -3);
				float before = master.getHealth();
				check(hit(target, 8, true) && master.getHealth() < before && master.fracturePending(), "A landed rear hit during the final six ticks does not revive the expired brace counter");
				place(target, 0, 4);
			}
		});
		at(world, StoneFractureRules.TELL - 1, () -> {
			if (answer != Answer.TARGET_DEATH) check(target.getHealth() == HEALTH, "Neither phase harms the challenger before all 32 tell ticks: " + answer);
			if (answer == Answer.PARRY) guard();
		});
		at(world, StoneFractureRules.TELL, () -> {
			if (hits(answer)) {
				check(close(HEALTH - target.getHealth(), REPLY_DAMAGE), "The captured target receives one 28-damage reply through Stone's existing 1.1 multiplier: " + answer);
				assertSource();
			} else if (answer == Answer.HELD_GUARD) {
				check(target.getHealth() < HEALTH && target.getHealth() > HEALTH - REPLY_DAMAGE, "Held frontal Aura Guard reduces the ordinary projected reply");
				assertSource();
			} else if (answer == Answer.FORESIGHT) {
				check(close(HEALTH - target.getHealth(), REPLY_DAMAGE - 12) && target.position().distanceToSqr(defendedPosition) > .5,
					"Native Foresight dodges and stops its normal capped twelve damage");
				assertSource();
			} else if (answer != Answer.TARGET_DEATH) check(target.getHealth() == HEALTH, "The defensive answer prevents the captured reply: " + answer);
			check(!master.fracturePending() && !master.state(AuraFighter.WINDUP) && !master.guarding(), "Every outcome consumes its one pending reply and drops the brace");
			check(bystander.getHealth() == HEALTH, "A bystander standing in the accepted lane is unharmed");
			for (Challenger guest : party) if (guest != target) check(guest.getHealth() == HEALTH, "Other enrolled players cannot inherit or multiply the selected-target reply");
			check(close(master.auraRemaining(), 56), "Thrust and every completed, cancelled or baited Fracture retain exactly 16 + 28 Aura payments");
			releasedHealth = target.getHealth();
			if (answer == Answer.NO_AI) master.setNoAi(false);
			if (answer == Answer.HOLD) threat = incomingThreat();
		});
		int recoveryEnd = cancelled(answer) ? (int) (cancelledAt - began) + StoneFractureRules.RECOVERY : StoneFractureRules.TELL + StoneFractureRules.RECOVERY;
		observeRecovery(world, recoveryEnd - 1, answer);
		if (answer == Answer.HOLD) at(world, recoveryEnd, () -> {
			check(ordinaryMove(master) != null && master.attackElapsed(0) == 0
				&& !master.guarding() && close(master.auraRemaining(), 40),
				"A completed Fracture advances sequence to the next admitted ordinary attack without buying a redundant guard after its paid brace");
		});
		world.getServer().runOnServer(server -> cleanup());
	}

	/** Observe the actual AI; never force an attack, sequence, cooldown, tick or NoAI-assisted admission. */
	private void beginNaturally(TestSingleplayerContext world, int count) {
		completeOpening(world, count, () -> {});
		world.getServer().waitFor(server -> {
			if (!master.fracturePending()) return false;
			check(master.attackAnimation() == 8 && master.attackAnimation() == MasterAnimationRules.STONE_FRACTURE
				&& master.attackElapsed(0) <= 1 && master.attackTellTicks() == StoneFractureRules.TELL && master.attackRecoveryTicks() == StoneFractureRules.RECOVERY - 1,
				"Natural sequence one replaces Stone's due guard with synchronized ID 8 and its complete tell + recovery timing");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			aimYaw = master.getYRot();
			check(Math.abs(aimYaw) < .1 && master.onGround() && !master.isNoAi() && !master.getMoveControl().hasWanted(), "Admission captures the forward aim and brakes a grounded native AI body");
			check(master.challengerCount() == count && !master.canHarmParticipant(bystander) && close(master.auraRemaining(), 56), "The locked consenting roster and single 28-Aura payment survive admission");
			for (Challenger guest : party) check(guest.heard("message.wildercord.master.stone_lesson"), "Every enrolled challenger hears Stone's counterplay lesson");
			check(!bystander.heard("message.wildercord.master.stone_lesson"), "The nearby nonparticipant receives no trial lesson");
			return true;
		}, 4);
	}

	private void completeOpening(TestSingleplayerContext world, int count, Runnable beforeReady) {
		world.getServer().runOnServer(server -> setup(count));
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP)) return false;
			check(master.attackAnimation() == MastersRules.Move.THRUST.ordinal() + 1 && master.attackElapsed(0) <= 1, "Fresh Stone ordinary AI begins sequence zero with Thrust");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			return true;
		}, 45);
		at(world, MastersRules.Move.THRUST.tell, () -> check(!master.state(AuraFighter.WINDUP) && close(master.auraRemaining(), 84), "The opening Thrust genuinely completes and pays its ordinary Aura"));
		at(world, MastersRules.Move.THRUST.tell + MastersRules.Move.THRUST.recovery - 1, () -> {
			for (Challenger guest : party) { guest.setHealth(HEALTH); place(guest, guest == target ? 0 : .2, 4); }
			place(bystander, -.2, 4); bystander.setHealth(HEALTH);
			master.setTarget(target); // Select the next fixture challenger only between completed attacks.
			check(!master.canBeginFracture(level.getGameTime()), "The opening Thrust owns its complete recovery before Fracture admission");
			beforeReady.run();
		});
	}

	private void blockedAdmission(TestSingleplayerContext world) {
		for (int obstruction = 0; obstruction < 4; obstruction++) {
			int kind = obstruction;
			completeOpening(world, 1, () -> {
				if (kind == 0) place(target, 0, 1.4);
				else if (kind == 1) place(target, 0, 5.6);
				else if (kind == 2) replyCover(true);
				else {
					target.setNoGravity(true);
					target.teleportTo(level, origin.x, origin.y + 1.1, origin.z + 4, Set.of(), 180, 0, false);
				}
			});
			at(world, MastersRules.Move.THRUST.tell + MastersRules.Move.THRUST.recovery, () -> {
				check(!master.fracturePending() && !master.state(AuraFighter.WINDUP) && master.guarding() && close(master.auraRemaining(), 72),
					"Out-of-band distance, height or existing cover declines Fracture before payment and preserves Stone's ordinary due guard: " + kind);
				cleanup();
			});
		}
	}

	private void resourcesAfterCancellation(TestSingleplayerContext world) {
		beginNaturally(world, 1);
		at(world, StoneFractureRules.PLANT, () -> Effects.withSource(target,
			() -> check(master.interruptWindup(), "The resource probe cancels a naturally paid Fracture")));
		at(world, StoneFractureRules.PLANT + StoneFractureRules.RECOVERY - 1, () -> check(!master.state(AuraFighter.WINDUP) && !master.guarding() && close(master.auraRemaining(), 56), "Cancellation retains all forty paid recovery frames"));
		at(world, StoneFractureRules.PLANT + StoneFractureRules.RECOVERY, () -> {
			check(ordinaryMove(master) != null && master.attackElapsed(0) == 0
				&& !master.fracturePending() && !master.guarding() && close(master.auraRemaining(), 40),
				"Cancellation leaves sequence one intact and clears the redundant guard, so native AI selects a legal ordinary attack");
		});
		world.getServer().waitFor(server -> {
			if (master.auraRemaining() >= StoneFractureRules.COST) return false;
			check(close(master.auraRemaining(), 12) && ordinaryMove(master) != null,
				"Two admitted ordinary moves and one normal guard spend the finite remainder without a refund");
			began = level.getGameTime() - (long) master.attackElapsed(0);
			return true;
		}, 120);
		MastersRules.Move exhaustedMove = ordinaryMove(master);
		at(world, exhaustedMove.tell + exhaustedMove.recovery, () -> {
			check(close(master.auraRemaining(), 12) && !master.state(AuraFighter.WINDUP) && !master.guarding() && !master.fracturePending(), "Exhausted native AI breathes exposed instead of borrowing Aura for another form");
		});
		at(world, exhaustedMove.tell + exhaustedMove.recovery + 10, () -> {
			check(master.auraRemaining() > 12 && !master.state(AuraFighter.WINDUP) && !master.guarding(), "Ordinary breathing gradually restores the exhausted resource");
			cleanup();
		});
	}

	private static MastersRules.Move ordinaryMove(SwordMaster master) {
		return MasterMoveCatalog.legacy().byWireId(master.attackAnimation()).map(MasterMoveCatalog.Definition::legacyMove)
			.filter(move -> move == MastersRules.Move.SWEEP || move == MastersRules.Move.THRUST || move == MastersRules.Move.CRESCENT).orElse(null);
	}

	private void setup(int count) {
		target = add("FractureTarget", 0, 4); party.add(target);
		for (int i = 1; i < count; i++) party.add(add("FractureAlly" + i, 6, -6 + i * 1.5));
		bystander = add("FractureBystander", -6, 4);
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		if (master != null) master.plainOrdinaryOnly();
		check(master != null, "The Stone fixture is constructible");
		master.setDiscipline(MastersRules.STONE); master.snapTo(origin.x, origin.y, origin.z, 0, 0);
		level.addFreshEntity(master);
		for (Challenger player : party) { master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND); }
		check(SwordMaster.ready(target) == 1, "Each challenger consents before the ordinary-AI trial starts");
		master.setTarget(target);
	}

	private void observeRecovery(TestSingleplayerContext world, int lastAge, Answer answer) {
		long last = began + lastAge;
		long renewAt = began + StoneFractureRules.TELL + 24;
		RuneBolt firstThreat = threat;
		world.getServer().waitFor(server -> {
			long now = level.getGameTime();
			check(now <= last, "Recovery observation cannot skip its final promised frame");
			check(!master.fracturePending() && !master.state(AuraFighter.WINDUP) && !master.guarding() && !master.state(AuraFighter.DASH), "Every observed recovery frame stays exposed: " + answer);
			check(close(master.auraRemaining(), 56) && target.getHealth() == releasedHealth, "Recovery neither refunds Aura, pays for evasions nor repeats the reply");
			if (answer != Answer.DEATH) {
				check(!master.canBeginFracture(now), "Recovery is never free Fracture admission");
				check(StoneFracture.prepare(master, target, MastersRules.STONE, 1, 100, now, 0) == null, "An otherwise eligible request cannot replace the promised recovery");
			}
			if (answer == Answer.HOLD) {
				check(threat != null && !threat.isRemoved() && threat.hostileSpellTo(master), "An admitted hostile Bolt remains inside the actual recovery threat sensor");
				check(master.position().distanceToSqr(origin) < .003, "An incoming threat cannot convert recovery into unannounced movement");
				if (now >= renewAt && threat == firstThreat) {
					check(now == renewAt, "Renew the recovery stimulus on its exact twenty-fourth frame, before its native lifespan expires");
					// Slow velocity does not extend RuneBolt's native lifespan. Overlap two admitted bolts within this callback.
					RuneBolt replacement = incomingThreat();
					check(!replacement.isRemoved() && !threat.isRemoved(), "The next real hostile Bolt exists before the previous live stimulus is removed");
					threat.discard();
					threat = replacement;
				}
				if (now == last) { threat.discard(); threat = null; }
			}
			return now == last;
		}, lastAge + 20);
	}

	private void assertStationary() {
		check(master.fracturePending() && master.state(AuraFighter.WINDUP) && master.position().distanceToSqr(origin) < .003, "Stone remains planted at its originally warned origin");
		check(Math.abs(master.getYRot() - aimYaw) < .1 && master.getDeltaMovement().horizontalDistanceSqr() < .00001, "Target movement cannot rotate or chase through the fixed-facing form");
	}
	private void assertCancelled(Answer answer) {
		check(!master.fracturePending() && master.attackAnimation() == 0 && !master.guarding(), "Cancellation clears the warning, brace and reply: " + answer);
		if (answer != Answer.AXE) check(master.getDeltaMovement().horizontalDistanceSqr() < .00001, "Cancellation brakes form movement: " + answer);
		if (answer == Answer.LEAVE || answer == Answer.TARGET_DEATH) check(master.challengerCount() == 2 && master.canHarmParticipant(party.get(1)), "The locked roster survives but the remaining challenger cannot inherit a lost target's reply");
	}
	private boolean hit(Challenger attacker, float damage, boolean fresh) {
		if (fresh) Effects.readyToHurt(master);
		// Vanilla MAGIC carries no_knockback; keep receipt cancellation distinct from the separate actual impulse probe.
		return master.hurtServer(level, level.damageSources().source(DamageTypes.MAGIC, attacker, attacker), damage);
	}
	private void guard() {
		target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
		target.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		target.setShiftKeyDown(true);
		target.setYRot(180); target.setYHeadRot(180); target.setYBodyRot(180); target.setXRot(0);
		check(AuraGuard.raise(target) && AuraGuard.guarding(target) && AuraGuard.facing(target, master.position()), "The challenger raises and pays for its real frontal Aura Guard");
	}
	private RuneBolt incomingThreat() {
		var group = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
		RuneBolt bolt = RuneBolt.launch(new Cast(target), group, null, master.getBoundingBox().getCenter().add(5, 0, 0), new Vec3(-1, 0, 0), false);
		check(bolt != null && bolt.hostileSpellTo(master), "An enrolled caster supplies the incoming recovery threat");
		bolt.setDeltaMovement(-.01, 0, 0);
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
		for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++)
			level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, y, 2), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void cleanup() {
		if (threat != null) { threat.discard(); threat = null; }
		master.discard(); for (Challenger player : party) player.discard(); party.clear(); bystander.discard(); replyCover(false);
	}
	private void assertSource() {
		check(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == master, "The projected reply retains its actual enrolled trial source");
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			long now = level.getGameTime(); if (now < expected) return false;
			check(now == expected, "Observe exact native Fracture frame " + age + ": expected=" + expected + ", actual=" + now);
			action.run(); return true;
		}, age + 20);
	}
	private static boolean cancelled(Answer answer) { return answer.ordinal() >= Answer.REAR_DAMAGE.ordinal(); }
	private static boolean hits(Answer answer) {
		return switch (answer) {
			case HOLD, PLANT_REAR, FRONTAL_DAMAGE, FRONTAL_ZERO, REAR_ZERO, OUTSIDER_REAR, REPLY_REAR, LOCKED_REAR, LOCKED_INTERRUPT -> true;
			default -> false;
		};
	}
	private static boolean close(double a, double b) { return Math.abs(a - b) < .01; }
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
