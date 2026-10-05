package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.Shields;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.Runes;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.Targets;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Real entity/damage-path checks. A benchmark fixture, not proof of a subjective difficulty multiple. */
public final class SwordMasterTrialTest implements FabricClientGameTest {
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}

	private SwordMaster master;
	private Challenger ally, spectator;
	private float before;
	private BlockPos stage;
	private long chargeStarted, attackStarted, abandonmentStarted;
	private int abandonmentMasterTick;

	private static void check(boolean result, String message) {
		if (!result) throw new AssertionError(message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) {
				world.getServer().runCommand(command);
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
				for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) level.setBlockAndUpdate(stage.offset(x, 0, z), Blocks.STONE.defaultBlockState());
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, stage.getX() + 0.5, 181, stage.getZ() + 0.5, Set.of(), 0, 0, false);
				ally = add(level, "MasterAlly", 2);
				spectator = add(level, "Bystander", 4);
				master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
				check(master != null, "The registered master is constructible");
				master.snapTo(stage.getX() + 0.5, 181, stage.getZ() + 3.5, 180, 0);
				master.setNoAi(true); // Advance its AI explicitly at the assertions, while world time is real.
				level.addFreshEntity(master);
				master.mobInteract(player, InteractionHand.MAIN_HAND);
				check(master.challengerCount() == 0 && !master.started(), "Talking once is an invitation, not enrollment");
				master.mobInteract(player, InteractionHand.MAIN_HAND);
				check(master.challengerCount() == 1 && !master.started(), "Only the speaker's second interaction enrolls them");
				check(!master.canHarmParticipant(spectator), "The nearby bystander was not enrolled");
				master.mobInteract(ally, InteractionHand.MAIN_HAND);
				check(master.challengerCount() == 1, "Another player must confirm independently");
				master.mobInteract(ally, InteractionHand.MAIN_HAND);
				check(master.challengerCount() == 2, "The second consenting challenger joins");
				check(SwordMaster.ready(player) == 1, "The challenger can close the lobby");
				master.customServerAiStep(level);
				check(master.started() && master.challengerCount() == 2, "The accepted roster is locked");
				check(Math.abs(master.getMaxHealth() - MastersRules.health(2)) < .01, "Health comes from the accepted roster");
				check(Math.abs(master.postureMultiplier() - MastersRules.postureMultiplier(2)) < .001, "Posture scales from the same locked count");
				master.mobInteract(spectator, InteractionHand.MAIN_HAND);
				master.mobInteract(spectator, InteractionHand.MAIN_HAND);
				check(master.challengerCount() == 2, "No late enrollment or re-scaling after the fight begins");
				check(master.projected(spectator, 100) == 0 && !Targets.canHarm(master, spectator), "Both direct and redirected magic exclude bystanders");
				check(!master.hurtServer(level, level.damageSources().playerAttack(spectator), 20), "A bystander cannot contribute uncounted damage");
				check(!Targets.canHarm(spectator, master), "Target selection rejects uncounted control as well as damage");
				check(!master.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3), spectator),
					"A bystander's harmful vanilla potion cannot stagger the master");
				Effects.withSource(spectator, () -> Statuses.silence(master, 20));
				check(!master.hasEffect(MobEffects.SLOWNESS) && !Statuses.silenced(master), "Explicit and scoped hostile status routes preserve the roster");
				check(!master.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100), spectator)
					&& !master.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100, 2), spectator),
					"A bystander's beneficial vanilla effects cannot grief the trial either");
				check(!master.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100)), "An unattributed beneficial effect is rejected on an active master");
				master.setHealth(master.getMaxHealth() - 10);
				float wounded = master.getHealth();
				master.heal(5);
				Effects.withSource(spectator, () -> master.heal(5));
				check(master.getHealth() == wounded, "Unattributed vanilla healing and outsider-scoped healing cannot change the fight");
				Effects.withSource(player, () -> master.heal(5));
				check(master.getHealth() > wounded, "An attributable enrolled source remains an admitted influence");
				check(ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40), player), "The master-only boundary preserves ordinary player healing buffs");
				master.setHealth(master.getMaxHealth());

				assertReturnedBoltsStayInsideTrial(player);

				dress(player);
			});
			// Vanilla installs armour/toughness attribute modifiers during its next equipment tick.
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				check(player.getArmorValue() == 20 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 12,
					"The benchmark's real netherite armour and toughness are active before measuring damage");
				player.setHealth(20);
				player.setAbsorptionAmount(0);
				Effects.readyToHurt(player);
				float taken = master.projected(player, MastersRules.damage(2, MastersRules.EMBER, MastersRules.Move.THRUST));
				Wildercord.LOGGER.info("[masters-benchmark] Normal, 20 health, full Protection IV netherite: thrust took {} health", taken);
				check(taken >= 7 && taken <= 12, "An unguarded heavy strike seriously hurts full Protection IV netherite without a healthy one-shot");
				player.setHealth(20);
				// A real perfect Aura Guard still defeats this large, correctly attributed hit.
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
				long now = level.getGameTime();
				player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE.guard(now, now + 40));
				check(master.projected(player, MastersRules.Move.THRUST.damage) == 0, "A correctly timed frontal parry negates the heavy strike");
				player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
				player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
				master.removeAllEffects();
				master.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 3), player);
				check(master.hasEffect(MobEffects.SLOWNESS) && !master.staggered(), "An enrolled slow hinders movement without creating permanent stagger");
				// The completed damage probes can leave real knockback; start this separate aim scenario on its marked positions.
				player.teleportTo(level, stage.getX() + .5, 181, stage.getZ() + .5, Set.of(), 0, 0, false);
				player.setDeltaMovement(Vec3.ZERO);
				master.snapTo(stage.getX() + .5, 181, stage.getZ() + 3.5, 180, 0);
				master.setDeltaMovement(Vec3.ZERO);
				check(Math.abs(master.distanceTo(player) - 3) < .001, "The charged-caster scenario starts three blocks away" + combatState(player));
				// Normal charge upkeep fizzles a caster without an equipped Cord, even when AI is advanced manually.
				Spellbooks.setCord(player, new ItemStack(WildercordItems.TWINE_CORD));
				master.setTarget(player);
				charge(player);
				before = player.getHealth();
			});
			context.waitTicks(21);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				assertChargedTarget(player);
				master.customServerAiStep(player.level());
				check(master.state(AuraFighter.WINDUP), "The close charged caster triggers a visible windup" + combatState(player));
				assertAttackStart(player);
				check(player.getHealth() == before, "Beginning the tell deals no immediate harm" + combatState(player));
				Effects.withSource(player, () -> check(Statuses.interrupt(master) && master.attackAnimation() == 0 && master.attackAimPitch() == 0,
					"A correctly timed enrolled interrupt cancels attack and body animation together" + combatState(player)));
			});
			context.waitTicks(21);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				assertChargedTarget(player);
				master.customServerAiStep(player.level());
				assertAttackStart(player);
				Effects.withSource(player, () -> check(master.state(AuraFighter.WINDUP) && !Statuses.interrupt(master),
					"A fresh tell survives repeated interruption during the shared 160-tick immunity" + combatState(player)));
			});
			atAttackTick(world, MastersRules.Move.BREAK_CAST.tell - MastersRules.AIM_LOCK, player -> {
				master.customServerAiStep(player.level()); // Fix the aim while the player is still in front.
				player.teleportTo(player.level(), stage.getX() + 3.5, 181, stage.getZ() + 0.5, Set.of(), 0, 0, false);
			});
			atAttackTick(world, MastersRules.Move.BREAK_CAST.tell, player -> {
				assertLiveCharge(player);
				master.customServerAiStep(player.level());
				check(player.getHealth() == before, "Sidestepping after aim lock evades the thrust" + combatState(player));
				check(master.attackAnimation() == MastersRules.Move.BREAK_CAST.ordinal() + 1 && master.attackElapsed(0) == master.attackTellTicks(),
					"The active animation frame is the exact server strike frame, and recovery remains visible" + combatState(player));
				check(player.hasAttached(WildercordAttachments.CHARGE), "A missed interrupt cannot cancel a cast" + combatState(player));
				assertLiveCharge(player);
				player.teleportTo(player.level(), stage.getX() + .5, 181, stage.getZ() + .5, Set.of(), 0, 0, false);
				master.setTarget(player);
			});
			atAttackTick(world, MastersRules.Move.BREAK_CAST.tell + MastersRules.Move.BREAK_CAST.recovery + 1, player -> {
				assertChargedTarget(player);
				master.customServerAiStep(player.level());
				assertAttackStart(player);
			});
			atAttackTick(world, MastersRules.Move.BREAK_CAST.tell, player -> {
				assertChargedTarget(player);
				float healthBeforeStrike = player.getHealth();
				master.customServerAiStep(player.level());
				check(master.attackAnimation() == MastersRules.Move.BREAK_CAST.ordinal() + 1 && master.attackElapsed(0) == master.attackTellTicks(),
					"The landed spellbreaker releases on its exact server strike frame" + combatState(player));
				check(player.getHealth() < healthBeforeStrike, "The real spellbreaker thrust damages its charged target" + combatState(player));
				check(!player.hasAttached(WildercordAttachments.CHARGE), "An actual landed spellbreaker thrust cancels a live charge" + combatState(player));
				charge(player);
				check(!Statuses.interrupt(player) && player.hasAttached(WildercordAttachments.CHARGE), "Shared interrupt immunity preserves the caster's next opportunity");
				player.removeAttached(WildercordAttachments.CHARGE);
				// Outside the 24-block arena, but still on the fixture's solid platform.
				ally.teleportTo(ally.level(), stage.getX() + 27.5, 181, stage.getZ() + 0.5, Set.of(), 0, 0, false);
				assertSafeDeparture(ally);
			});
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				master.customServerAiStep(player.level());
				check(master.challengerCount() == 2 && Math.abs(master.getMaxHealth() - MastersRules.health(2)) < .01,
					"Leaving never lowers the locked difficulty");
				check(!master.canHarmParticipant(ally), "An out-of-arena player is no longer a legal target");
				assertSafeDeparture(ally);
				player.teleportTo(player.level(), stage.getX() + 27.5, 181, stage.getZ() + .5, Set.of(), 0, 0, false);
				assertSafeDeparture(player);
				ally.discard();
				spectator.discard();
				master.setNoAi(false);
				abandonmentStarted = player.level().getGameTime();
				abandonmentMasterTick = master.tickCount;
				check(master.isAlive() && !master.isRemoved() && !master.isNoAi()
					&& player.level().isPositionEntityTicking(master.blockPosition())
					&& player.level().areEntitiesActuallyLoadedAndTicking(master.chunkPosition()),
					"The abandonment probe starts with a living, AI-enabled master in an entity-ticking chunk" + abandonmentState(player));
			});
			long abandonmentDeadline = abandonmentStarted + MastersRules.ABANDON_TICKS + 20;
			world.getServer().waitFor(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				long now = player.level().getGameTime();
				if (now < abandonmentDeadline) return false;
				String receipt = abandonmentState(player);
				Wildercord.LOGGER.info("[masters-abandonment] {}", receipt);
				check(now == abandonmentDeadline, "The abandonment check observes its unchanged server-clock deadline" + receipt);
				assertSafeDeparture(player);
				check(master.isRemoved(), "An abandoned challenge cleans up instead of hunting bystanders" + receipt);
				return true;
			}, MastersRules.ABANDON_TICKS + 20);
		}
		new EmberAfterburnChecks().run(context);
		new MasterPursuitChecks().run(context);
	}

	private void assertSafeDeparture(ServerPlayer player) {
		check(player.isAlive() && player.level().getBlockState(player.blockPosition().below()).isSolidRender()
			&& player.distanceToSqr(Vec3.atCenterOf(stage.above())) > MastersRules.ARENA_RADIUS * MastersRules.ARENA_RADIUS
			&& !master.canHarmParticipant(player),
			"The departed challenger remains alive on solid ground outside the arena" + abandonmentState(player));
	}

	private String abandonmentState(ServerPlayer player) {
		ServerLevel level = player.level();
		return " [time=" + level.getGameTime() + ", abandonmentStarted=" + abandonmentStarted
			+ ", serverTicks=" + (level.getGameTime() - abandonmentStarted) + ", masterTicks=" + (master.tickCount - abandonmentMasterTick)
			+ ", masterAlive=" + master.isAlive() + ", masterHealth=" + master.getHealth() + ", masterRemoved=" + master.isRemoved()
			+ ", masterNoAi=" + master.isNoAi() + ", masterPosition=" + master.position() + ", masterVelocity=" + master.getDeltaMovement()
			+ ", entityTicking=" + level.isPositionEntityTicking(master.blockPosition())
			+ ", entitiesLoadedAndTicking=" + level.areEntitiesActuallyLoadedAndTicking(master.chunkPosition())
			+ ", participants=" + master.challengers() + ", target=" + (master.getTarget() == null ? null : master.getTarget().getUUID())
			+ ", player=" + player.getUUID() + ", playerAlive=" + player.isAlive() + ", playerHealth=" + player.getHealth()
			+ ", playerPosition=" + player.position() + ", playerVelocity=" + player.getDeltaMovement()
			+ ", playerArenaDistanceSqr=" + player.distanceToSqr(Vec3.atCenterOf(stage.above())) + "]";
	}

	private void assertReturnedBoltsStayInsideTrial(ServerPlayer player) {
		ServerLevel level = player.level();
		var fire = SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE)).root().groups.getFirst();
		var frost = SpellCompiler.compile(List.of(Runes.BOLT, Runes.FROST)).root().groups.getFirst();
		Vec3 front = master.getBoundingBox().getCenter().add(0, 0, -2);
		spectator.snapTo(front.x + 1, 181, front.z, 0, 0);
		float bystanderHealth = spectator.getHealth();
		RuneBolt returned = RuneBolt.launch(new Cast(player), fire, null, front, new Vec3(0, 0, 1), false);
		check(returned != null && returned.swordRedirect(master), "A simple incoming hostile bolt can be redirected");
		check(returned.getOwner() == master && returned.isReflected(), "Redirection transfers ownership and records reflection");
		RuneBolt crossing = RuneBolt.launch(new Cast(ally), frost, null, front.add(0, 0, -.25), new Vec3(0, 0, 1), false);
		check(crossing != null, "A second participant's collision bolt exists");
		returned.tick();
		check(returned.isRemoved() && crossing.isRemoved(), "The real returned-bolt collision occurred");
		check(spectator.getHealth() == bystanderHealth && !spectator.isOnFire(), "Collision credit cannot leak trial splash or statuses to a bystander");

		RuneBolt held = RuneBolt.launch(new Cast(player), fire, null, front, new Vec3(0, 0, 1), false);
		check(held != null && held.swordRedirect(master), "The held-Shield return was accepted");
		Shields.give(player, 100, 100, List.of(Runes.SHIELD.id()));
		float defended = player.getHealth();
		held.tick();
		check(held.isRemoved() && player.getHealth() == defended && Shields.strength(player) == 0,
			"An ordinary held Shield spends itself to stop the bounded return");

		// A real Shield sends the master's controlled return back as the participant's; its trial boundary must survive.
		RuneBolt twice = RuneBolt.launch(new Cast(player), fire, null, front, new Vec3(0, 0, 1), false);
		check(twice != null && twice.swordRedirect(master), "The second return was accepted");
		CastEngine.cast(player, SpellCompiler.compile(List.of(Runes.SELF, Runes.SHIELD)).root());
		twice.tick();
		check(!twice.isRemoved() && twice.getOwner() == player, "A participant can actually parry a master return back");
		spectator.snapTo(front.x, 181, front.z + .75, 180, 0);
		Shields.give(spectator, 100, 100, List.of(Runes.SHIELD.id()));
		float shield = Shields.strength(spectator);
		float masterHealth = master.getHealth();
		for (int i = 0; i < 4 && !twice.isRemoved(); i++) twice.tick();
		check(Shields.strength(spectator) == shield && spectator.getHealth() == bystanderHealth,
			"A bystander's Shield and health survive the participant-owned return");
		check(master.getHealth() < masterHealth, "The protected bystander does not eat the participant's valid return to the master");
		twice.discard();
		spectator.removeAttached(WildercordAttachments.SPELL_SHIELD);
		player.removeAttached(WildercordAttachments.SPELL_SHIELD);
		spectator.snapTo(stage.getX() + 4.5, 181, stage.getZ() + .5, 0, 0);
		master.setHealth(master.getMaxHealth());
	}

	private Challenger add(ServerLevel level, String name, int x) {
		Challenger player = new Challenger(level, name);
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(stage.getX() + x + .5, 181, stage.getZ() + .5, 0, 0);
		level.addNewPlayer(player);
		return player;
	}

	private void charge(ServerPlayer player) {
		long now = player.level().getGameTime();
		chargeStarted = now;
		player.setAttached(WildercordAttachments.CHARGE, new WildercordAttachments.Charge(0, now, List.of("bolt", "harm"), 30, 0, 0, now + 30, false));
	}

	private void assertLiveCharge(ServerPlayer player) {
		var charge = player.getAttached(WildercordAttachments.CHARGE);
		check(Spellbooks.tier(player) != null && charge != null && charge.start() == chargeStarted,
			"The original charge remains live on a properly equipped caster" + combatState(player));
	}

	private void assertChargedTarget(ServerPlayer player) {
		assertLiveCharge(player);
		check(master.getTarget() == player && master.canHarmParticipant(player) && master.distanceTo(player) <= 4
			&& !MastersRules.needsCrescent(master.distanceTo(player), player.getBoundingBox().getCenter().y - master.slashOrigin().y),
			"The live charged challenger is the master's close, grounded target" + combatState(player));
	}

	private void assertAttackStart(ServerPlayer player) {
		attackStarted = player.level().getGameTime();
		check(master.attackAnimation() == MastersRules.Move.BREAK_CAST.ordinal() + 1 && master.attackElapsed(0) == 0
			&& master.attackTellTicks() == MastersRules.Move.BREAK_CAST.tell,
			"The synced body animation starts with the server's exact move and tell" + combatState(player));
	}

	/** Observe and advance the real AI in the same server callback at its accepted begin tick plus the requested offset. */
	private void atAttackTick(TestSingleplayerContext world, int elapsed, Consumer<ServerPlayer> action) {
		long expected = attackStarted + elapsed;
		world.getServer().waitFor(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			long now = player.level().getGameTime();
			if (now < expected) return false;
			check(now == expected, "The server-clock observation must not skip the requested attack frame: expected=" + expected + combatState(player));
			action.accept(player);
			return true;
		}, elapsed + 20);
	}

	private String combatState(ServerPlayer player) {
		long now = player.level().getGameTime();
		var charge = player.getAttached(WildercordAttachments.CHARGE);
		return " [time=" + now + ", attackStarted=" + attackStarted + ", move=" + master.attackAnimation()
			+ ", elapsed=" + master.attackElapsed(0) + ", tell=" + master.attackTellTicks() + ", windup=" + master.state(AuraFighter.WINDUP)
			+ ", tier=" + Spellbooks.tier(player) + ", charge=" + charge + ", chargeAge=" + (charge == null ? -1 : now - charge.start())
			+ ", target=" + (master.getTarget() == null ? null : master.getTarget().getUUID()) + ", distance=" + master.distanceTo(player)
			+ ", masterPosition=" + master.position() + ", masterVelocity=" + master.getDeltaMovement()
			+ ", playerPosition=" + player.position() + ", playerVelocity=" + player.getDeltaMovement() + "]";
	}

	private static void dress(ServerPlayer player) {
		var protection = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		Item[] items = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		for (int i = 0; i < items.length; i++) {
			ItemStack piece = new ItemStack(items[i]);
			piece.enchant(protection, 4);
			player.setItemSlot(slots[i], piece);
		}
		player.removeAllEffects();
	}
}
