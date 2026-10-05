package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Statuses;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
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
import java.util.function.Consumer;

/** Native movement, damage, charging and cancellation contracts, run from the existing Masters trial suite. */
final class MasterPursuitChecks {
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private enum Answer { HOLD, APPROACH, SIDESTEP, PARRY, COVER, WALL, RELEASE, INTERRUPT, IMPULSE, NO_AI, LEAVE, DEATH, TARGET_DEATH }
	private SwordMaster master;
	private ServerPlayer caster;
	private final List<Challenger> guests = new ArrayList<>();
	private Challenger bystander;
	private Vec3 origin, stopped;
	private long began;
	private float before;
	private int school;
	private double targetDistance = 6;
	private boolean previouslyInterrupted;

	void run(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
				for (int x = -30; x <= 55; x++) for (int z = -30; z <= 30; z++)
					player.level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
			});
			for (int discipline : new int[] {MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE}) {
				school = discipline;
				scenario(context, world, Answer.HOLD, discipline == MastersRules.GALE ? 8 : 1);
			}
			school = MastersRules.GALE;
			for (Answer answer : Answer.values()) if (answer != Answer.HOLD) scenario(context, world, answer, answer == Answer.TARGET_DEATH ? 2 : 1);
			resourceAndFeintChecks(context, world);
		}
	}

	private void scenario(ClientGameTestContext context, TestSingleplayerContext world, Answer answer, int count) {
		// A new Master does not reset the shared target UUID's anti-lockout window.
		if (previouslyInterrupted && (answer == Answer.HOLD || answer == Answer.APPROACH)) context.waitTicks(Statuses.INTERRUPT_GAP);
		targetDistance = answer == Answer.HOLD ? school == MastersRules.GALE ? 9 : school == MastersRules.EMBER ? 7 : 6 : 6;
		world.getServer().runOnServer(server -> setup(server.getPlayerList().getPlayers().getFirst(), count, answer == Answer.TARGET_DEATH, true));
		if (answer == Answer.APPROACH) {
			context.waitTicks(4);
			world.getServer().runOnServer(server -> {
				check(master.onGround() && master.started(), "Ordinary AI and vertical travel naturally ground the Master");
				master.setNoAi(true); // Pause only after actual grounding, before the opening becomes free.
			});
			context.waitTicks(23);
			world.getServer().runOnServer(server -> {
				check(master.getNavigation().moveTo(caster, 1.3), "A real approach path is queued before pursuit admission");
				master.setNoAi(false);
			});
		}
		awaitBegin(world);
		at(world, 11, player -> {
			check(master.position().distanceToSqr(origin) < .001 && caster.getHealth() == before, "Pursuit warning precedes movement and harm");
		});
		at(world, 13, player -> {
			check(master.getZ() > origin.z + .3 && master.getZ() < origin.z + 2, "The real body advances through bounded native movement");
			switch (answer) {
				case WALL -> wall(player.level(), 2, true);
				case RELEASE -> Charging.interrupt(caster);
				case INTERRUPT -> Effects.withSource(caster, () -> check(master.interruptWindup(), "An admitted early interruption stops the advancing Master"));
				case IMPULSE -> master.setDeltaMovement(.4, master.getDeltaMovement().y, 0);
				case NO_AI -> { master.setNoAi(false); master.setNoAi(true); }
				case LEAVE -> { place(caster, 50, 0); for (var guest : guests) place(guest, 50, 2); }
				case DEATH -> { master.setHealth(0); master.die(player.level().damageSources().generic()); }
				case TARGET_DEATH -> { caster.setHealth(0); caster.die(player.level().damageSources().generic()); }
				default -> {}
			}
			stopped = master.position();
		});
		at(world, 20, player -> {
			if (cancelled(answer)) {
				check(!master.pursuitPending() && master.attackAnimation() == 0, "Interrupted pursuit cancels its warning, motion and later hit: " + answer);
				check(master.getDeltaMovement().horizontalDistanceSqr() < .00001, "Cancellation brakes horizontal velocity: " + answer);
				if (answer != Answer.WALL && answer != Answer.IMPULSE) check(master.position().distanceToSqr(stopped) < .02, "Cancelled movement does not drift: " + answer);
				if (answer == Answer.WALL) check(master.getZ() < origin.z + 1.5, "A new wall stops the dash before contact");
				return;
			}
			check(master.pursuitPending() && caster.getHealth() == before, "Landing starts a second harmless warning");
			double expected = MasterPursuitRules.travel(school, targetDistance, 1);
			check(Math.abs(master.getZ() - origin.z - expected) < .05, "Movement ends at the school-bounded accepted endpoint");
			switch (answer) {
				case SIDESTEP -> place(caster, 2, 6);
				case COVER -> wall(player.level(), 5, true);
				default -> {}
			}
		});
		if (answer == Answer.PARRY) context.getInput().holdKey(options -> options.keyShift);
		at(world, 31, player -> {
			if (!cancelled(answer)) check(caster.getHealth() == before, "No strike arrives before all twelve final warning ticks");
			if (answer == Answer.PARRY) {
				check(caster.isShiftKeyDown(), "Native client sneak input holds the parry through the strike");
				caster.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
				caster.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
				long now = caster.level().getGameTime();
				caster.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE.guard(now, now + 40));
			}
		});
		at(world, 32, player -> {
			if (answer == Answer.HOLD || answer == Answer.APPROACH) {
				check(caster.getHealth() < before && !caster.hasAttached(WildercordAttachments.CHARGE), "An actual damaging strike breaks the held spell");
				check(caster.getLastDamageSource() != null && caster.getLastDamageSource().getEntity() == master, "Pursuit damage keeps the trial's real source");
				previouslyInterrupted = true;
				charge(caster);
				long protectedCharge = caster.getAttached(WildercordAttachments.CHARGE).start();
				Effects.withSource(master, () -> check(!Statuses.interrupt(caster), "A second interruption inside the shared window is rejected"));
				check(caster.getAttached(WildercordAttachments.CHARGE) != null
					&& caster.getAttached(WildercordAttachments.CHARGE).start() == protectedCharge,
					"A fresh held spell retains its identity under the real per-target interruption immunity");
				Charging.interrupt(caster); // End this isolated probe; never clear or bypass Statuses' cooldown.
			} else if (answer != Answer.TARGET_DEATH) check(caster.getHealth() == before, "The defensive answer prevents damage: " + answer);
			if (answer == Answer.PARRY || answer == Answer.SIDESTEP || answer == Answer.COVER)
				check(caster.hasAttached(WildercordAttachments.CHARGE), "A miss, cover or zero-damage parry never breaks charge: " + answer);
			check(!master.pursuitPending(), "One attempt has at most one strike");
			float health = caster.getHealth();
			master.customServerAiStep(player.level());
			check(caster.getHealth() == health, "Same-tick re-entry cannot repeat damage");
			check(bystander.getHealth() == 200, "A nearby unaccepted caster is never pursued or hit");
			for (var guest : guests) if (guest != caster) check(guest.getHealth() == 200, "Eight-player spread cannot multiply this targeted strike");
			check(master.auraRemaining() == MastersRules.AURA_MAX - MasterPursuitRules.school(school).cost(), "Completed, interrupted and baited attempts keep their Aura payment");
		});
		if (answer == Answer.PARRY) context.getInput().releaseKey(options -> options.keyShift);
		if (answer == Answer.NO_AI) {
			world.getServer().runOnServer(server -> master.setNoAi(false));
			at(world, 42, player -> check(!master.pursuitPending() && master.attackAnimation() == 0 && !master.guarding(),
				"Resuming AI cannot resurrect a cancelled dash or erase its exposed recovery"));
		}
		if (!cancelled(answer)) at(world, 61, player -> {
			check(!master.state(AuraFighter.WINDUP) && !master.guarding(), "A full thirty-tick exposed recovery follows the strike");
			check(master.auraRemaining() == MastersRules.AURA_MAX - MasterPursuitRules.school(school).cost(), "Recovery cannot be cancelled into another paid dash");
		});
		world.getServer().runOnServer(server -> cleanup(server.getPlayerList().getPlayers().getFirst()));
	}

	private void resourceAndFeintChecks(ClientGameTestContext context, TestSingleplayerContext world) {
		targetDistance = 6;
		world.getServer().runOnServer(server -> setup(server.getPlayerList().getPlayers().getFirst(), 1, false, false));
		context.waitTicks(4);
		world.getServer().runOnServer(server -> {
			check(master.onGround() && master.started(), "Resource fixture grounds under actual AI before a deliberate pause");
			master.setNoAi(true);
		});
		context.waitTicks(21);
		for (int i = 0; i < 4; i++) {
			world.getServer().runOnServer(server -> charge(caster));
			context.waitTicks(1);
			world.getServer().runOnServer(server -> {
				check(MasterPursuit.prepare(master, caster, school, master.auraRemaining(), caster.level().getGameTime(), 0) == null,
					"One-tick tap/cancel cannot qualify for the pursuit opportunity");
				Charging.interrupt(caster);
				check(master.auraRemaining() == 100 && !master.pursuitPending(), "Rejected feints cost the Master no Aura or exposed reset");
			});
			context.waitTicks(6);
		}
		for (int i = 0; i < 3; i++) {
			world.getServer().runOnServer(server -> { master.snapTo(origin.x, origin.y, origin.z, 0, 0); master.setDeltaMovement(Vec3.ZERO); place(caster, 0, 6); charge(caster); });
			context.waitTicks(6);
			world.getServer().runOnServer(server -> {
				check(MasterPursuit.prepare(master, caster, school, master.auraRemaining(), caster.level().getGameTime(), 0) != null,
					"The same admitted caster qualifies after six observable charge ticks");
				master.setNoAi(false);
			});
			awaitBegin(world);
			at(world, 13, player -> Charging.interrupt(caster));
			at(world, 14, player -> {
				check(!master.pursuitPending() && master.attackAnimation() == 0, "Releasing a held cast safely baits one paid cancellation");
				charge(caster);
			});
			at(world, 43, player -> {
				check(!master.pursuitPending() && !master.state(AuraFighter.WINDUP) && !master.guarding(),
					"A new charge cannot erase the baited attempt's thirty-tick punish window");
				master.setNoAi(true);
				Charging.interrupt(caster);
			});
			context.waitTicks(MasterPursuitRules.school(school).cooldown());
		}
		world.getServer().runOnServer(server -> {
			check(master.auraRemaining() == 22, "Three Gale attempts consume finite Aura rather than refunding a bait");
			charge(caster);
		});
		context.waitTicks(6);
		world.getServer().runOnServer(server -> master.setNoAi(false));
		context.waitTicks(1);
		world.getServer().runOnServer(server -> {
			check(!master.pursuitPending() && !master.state(AuraFighter.WINDUP) && !master.guarding(), "Exhaustion enters exposed breathing instead of borrowing Aura");
			cleanup(server.getPlayerList().getPlayers().getFirst());
		});
	}

	private void setup(ServerPlayer player, int count, boolean fakeCaster, boolean heldCharge) {
		player.setGameMode(GameType.SURVIVAL); player.removeAllEffects(); player.setAbsorptionAmount(0);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); player.setHealth(200);
		player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) player.setItemSlot(slot, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE); player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
		place(player, fakeCaster ? 6 : 0, 6);
		for (int i = 1; i < count; i++) guests.add(add(player.level(), "PursuitAlly" + i, 6, -8 + i * 2));
		caster = fakeCaster ? guests.getFirst() : player;
		place(caster, 0, targetDistance);
		bystander = add(player.level(), "PursuitBystander", 2, 7);
		master = AuraWorld.SWORD_MASTER.create(player.level(), EntitySpawnReason.COMMAND);
		check(master != null, "Pursuit Master is constructible");
		master.setDiscipline(school); master.snapTo(origin.x, origin.y, origin.z, 0, 0);
		player.level().addFreshEntity(master);
		enroll(player); for (var guest : guests) enroll(guest);
		probePath(player.level());
		if (heldCharge) charge(caster);
		charge(bystander); // A genuine outside charge must never be an admitted opportunity.
		check(SwordMaster.ready(player) == 1, "Every challenger explicitly accepts the trial");
		master.setTarget(caster);
		check(master.challengerCount() == count && !master.challengers().contains(bystander.getUUID()), "The enrolled party excludes the unaccepted caster");
	}

	private void awaitBegin(TestSingleplayerContext world) {
		world.getServer().waitFor(server -> {
			if (!master.pursuitPending()) return false;
			long now = caster.level().getGameTime();
			began = now - (long) master.attackElapsed(0); before = caster.getHealth();
			check(master.attackAnimation() == MasterAnimationRules.PURSUIT_BREAK && master.attackElapsed(0) <= 1,
				"Observe the naturally accepted pursuit at its start: school=" + school + ", onGround=" + master.onGround());
			check(master.onGround() && master.challengerCount() == guests.size() + 1, "A naturally grounded Master retains its locked roster");
			check(!master.getMoveControl().hasWanted(), "Pursuit clears queued approach movement before MoveControl and travel run");
			return true;
		}, 45);
	}

	private void charge(ServerPlayer player) {
		Charging.interrupt(player);
		Spellbooks.setCord(player, new ItemStack(WildercordItems.TWINE_CORD));
		Spellbooks.set(player, new Spellbook(List.of("bolt", "harm"), List.of(List.of("bolt", "harm")), 0, true));
		Spellbooks.setReadyAt(player, 0, 0); Spellbooks.setMana(player, 100);
		Charging.request(player, 0, true);
		check(player.hasAttached(WildercordAttachments.CHARGE), "The native Charging entrypoint accepted this held spell");
	}

	private void probePath(ServerLevel level) {
		check(MasterPursuit.safePath(master, level, new Vec3(0, 0, 4)), "Clear supported runway is admissible");
		wall(level, 2, true); check(!MasterPursuit.safePath(master, level, new Vec3(0, 0, 4)), "Swept body refuses a wall"); wall(level, 2, false);
		BlockPos floor = BlockPos.containing(origin.add(0, -1, 2));
		for (var block : List.of(Blocks.AIR, Blocks.MAGMA_BLOCK, Blocks.WATER)) {
			level.setBlockAndUpdate(floor, block.defaultBlockState());
			check(!MasterPursuit.safePath(master, level, new Vec3(0, 0, 4)), "Full-footprint probes refuse gaps, damaging floor and fluid");
		}
		level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
		check(!MasterPursuit.safePath(master, level, new Vec3(0, 0, -25)), "A swept path away from the challenger-centred arena cannot leave its edge");
		var border = level.getWorldBorder(); double size = border.getSize(), x = border.getCenterX(), z = border.getCenterZ();
		try {
			border.setCenter(origin.x, origin.z); border.setSize(4);
			check(!MasterPursuit.safePath(master, level, new Vec3(0, 0, 2)), "World border rejects body overhang even when endpoint centre lies at its edge");
		} finally { border.setCenter(x, z); border.setSize(size); }
	}

	private void cleanup(ServerPlayer player) {
		master.discard(); for (var guest : guests) { Charging.forget(guest); guest.discard(); } guests.clear(); Charging.forget(bystander); bystander.discard();
		Charging.forget(player); player.removeAllEffects(); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setHealth(200);
		wall(player.level(), 2, false); wall(player.level(), 5, false);
	}
	private void enroll(ServerPlayer player) { master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND); }
	private Challenger add(ServerLevel level, String name, double x, double z) {
		Challenger player = new Challenger(level, name); player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); player.setHealth(200);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 180, 0); level.addNewPlayer(player); return player;
	}
	private void place(ServerPlayer player, double x, double z) {
		player.teleportTo(player.level(), origin.x + x, origin.y, origin.z + z, Set.of(), 180, 0, false); player.setDeltaMovement(Vec3.ZERO);
	}
	private void wall(ServerLevel level, int z, boolean present) {
		for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++)
			level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, y, z), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private static boolean cancelled(Answer answer) { return answer.ordinal() >= Answer.WALL.ordinal(); }
	private void at(TestSingleplayerContext world, int age, Consumer<ServerPlayer> action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			var player = server.getPlayerList().getPlayers().getFirst(); long now = player.level().getGameTime();
			if (now < expected) return false;
			check(now == expected, "Observe exact pursuit frame " + age + ", expected=" + expected + ", actual=" + now);
			action.accept(player); return true;
		}, age + 20);
	}
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
