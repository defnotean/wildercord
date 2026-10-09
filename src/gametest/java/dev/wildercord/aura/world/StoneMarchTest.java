package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Effects;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static dev.wildercord.aura.world.StoneMarchFixture.check;
import static dev.wildercord.aura.world.StoneMarchFixture.close;

/** Actual native admission, defenses, terrain changes and simultaneous callback boundaries. Real-input jump trials live separately. */
public final class StoneMarchTest implements FabricClientGameTest {
	private static final double DAMAGE = 26.4;
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { this(level, new GameProfile(UUID.randomUUID(), name)); }
		Challenger(ServerLevel level, GameProfile profile) { super(level, profile); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private enum Answer { HOLD, SIDE, INNER, OUTER, SPENT, ONE_ATTEMPT, GUARD, PARRY, ABSORPTION, ARMOR, FORESIGHT,
		NEW_COVER, REMOVED_THIN_COVER, NEW_GAP, REMOVED_GAP, BLOCK_EXITS, BODY_BLOCK, NEW_HAZARD, UPPER_FLUID, INTERRUPT, OUTSIDE_INTERRUPT,
		NO_AI, DISPLACE, SPECTATOR, LEAVE, PARTY_JOIN, OWNER_DEATH, TARGET_DEATH, DUPLICATE, SKIPPED,
		CALLBACK_CANCEL, CALLBACK_ENTRY, CALLBACK_EXIT, CALLBACK_REENTRY, CALLBACK_BAD_CLOCK, CALLBACK_COVER, CALLBACK_BODY, CALLBACK_HAZARD, CALLBACK_REPLACE, CALLBACK_DEFEAT }
	private static StoneMarchTest active;
	private static boolean hooks;
	private ServerLevel level;
	private Vec3 origin;
	private StoneMarchFixture fixture;
	private final List<Challenger> party = new ArrayList<>();
	private Challenger target, bystander, blocker, replacement;
	private Answer answer;
	private int callbacks;

	@Override public void runTest(ClientGameTestContext context) {
		installHooks();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
				world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst(); level = observer.level();
				origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.StoneMarchTest\",\"seed\":\"{}\"}", level.getSeed());
				for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				observer.setGameMode(GameType.SPECTATOR);
			});
			for (Answer answer : Answer.values()) scenario(world, answer);
			partyTrial(world);
			admissionPriority(world);
		} finally { active = null; }
	}

	private void scenario(TestSingleplayerContext world, Answer chosen) {
		answer = chosen; callbacks = 0;
		boolean callback = chosen.name().startsWith("CALLBACK_");
		world.getServer().runOnServer(server -> setup(callback ? 2 : 1));
		// The first band remains usable while initial cover/gaps permanently remove the farther prefix.
		if (chosen == Answer.REMOVED_THIN_COVER) world.getServer().runOnServer(server -> thinCover(true));
		if (chosen == Answer.REMOVED_GAP) world.getServer().runOnServer(server -> floor(5, false));
		fixture.await(world);
		fixture.at(world, 6, () -> {
			check(target.getHealth() == 200 && fixture.master.marchPending(), "The early gather is harmless");
			check(StoneMarch.prepare(fixture.master, target, MastersRules.STONE, 3, 100, level.getGameTime(), 0) == null,
				"A running form cannot prepare or pay twice");
			switch (chosen) {
				case SIDE -> fixture.place(target, 2.5, 2.5);
				case INNER -> fixture.place(target, 0, 1.2);
				case OUTER -> fixture.place(target, 0, 8);
				case SPENT -> fixture.place(target, 0, 4.4);
				case GUARD -> guard();
				case ABSORPTION -> { target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(40); target.setAbsorptionAmount(40); check(close(target.getAbsorptionAmount(), 40), "Native absorption is actually funded"); }
				case ARMOR -> { target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE)); target.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS)); }
				case NEW_COVER -> cover(2, true);
				case REMOVED_THIN_COVER -> { thinCover(false); fixture.place(target, 0, 4.5); }
				case NEW_GAP -> floor(2, false);
				case REMOVED_GAP -> { floor(5, true); fixture.place(target, 0, 6.5); }
				case BLOCK_EXITS -> { sideCover(-2, true); sideCover(2, true); }
				case BODY_BLOCK -> { fixture.place(bystander, -1.1, 2.0); fixture.place(blocker, 1.1, 2.0); }
				case NEW_HAZARD -> roses(2, true);
				case UPPER_FLUID -> upperFluid(2, true);
				case INTERRUPT -> Effects.withSource(target, () -> check(fixture.master.interruptWindup(), "An enrolled early interrupt cancels the paid form"));
				case OUTSIDE_INTERRUPT -> Effects.withSource(bystander, () -> check(!fixture.master.interruptWindup(), "An outsider cannot cancel enrollment-owned action"));
				case NO_AI -> fixture.master.setNoAi(true);
				case DISPLACE -> fixture.master.setDeltaMovement(.4, fixture.master.getDeltaMovement().y, 0);
				case SPECTATOR -> target.setGameMode(GameType.SPECTATOR);
				case LEAVE -> fixture.place(target, 40, 0);
				case PARTY_JOIN -> {
					var rules = partyRules(); long now = level.getGameTime();
					check(rules.invite(target.getUUID(), bystander.getUUID(), now) == dev.wildercord.party.PartyRules.Result.OK
						&& rules.accept(bystander.getUUID(), target.getUUID(), now) == dev.wildercord.party.PartyRules.Result.OK, "The live native party changes during the accepted tell");
					fixture.place(bystander, 0, 3.1);
					check(dev.wildercord.party.Parties.sameParty(target, bystander) && fixture.master.challengers().size() == 1, "Joining a challenger's party does not enroll a bystander");
				}
				case OWNER_DEATH -> { fixture.master.setHealth(0); fixture.master.die(level.damageSources().generic()); }
				case TARGET_DEATH -> { target.setHealth(0); target.die(level.damageSources().generic()); }
				case SKIPPED -> check(!fixture.accepted.tick(level.getGameTime() + 2), "A skipped/mismatched callback cannot catch up a pulse");
				case CALLBACK_ENTRY -> fixture.place(party.get(1), 2.5, 3.1);
				default -> {}
			}
		});
		fixture.at(world, 31, () -> {
			if (chosen == Answer.PARRY) guard();
			if (chosen == Answer.FORESIGHT) CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
			if (chosen == Answer.CALLBACK_DEFEAT) party.forEach(player -> player.setHealth(20));
			if (callback || chosen == Answer.ONE_ATTEMPT) active = this;
		});
		fixture.at(world, 32, () -> {
			active = null;
			if (chosen == Answer.CALLBACK_REPLACE) check(replacement != null && replacement.getHealth() == 200 && !fixture.master.marchPending(),
				"A same-UUID replacement body cannot inherit a pending hit or the remaining action");
			if (chosen == Answer.PARTY_JOIN) check(bystander.getHealth() == bystander.getMaxHealth(), "A new party member in the resolving band stays outside its exact outgoing roster");
			if (chosen == Answer.DUPLICATE) { check(fixture.accepted.tick(level.getGameTime()), "A duplicate current tick is inert"); check(close(target.getHealth(), 200 - DAMAGE), "A duplicate pulse does not damage twice"); }
			if (chosen == Answer.HOLD || chosen == Answer.OUTSIDE_INTERRUPT || chosen == Answer.PARTY_JOIN || chosen == Answer.DUPLICATE)
				check(close(target.getHealth(), 200 - DAMAGE), "Standing in the first band uses the existing Stone multiplier and provenance");
			if (chosen == Answer.ABSORPTION) check(close(target.getHealth(), 200) && close(target.getAbsorptionAmount(), 40 - DAMAGE), "The real absorption sink consumes the ordinary damage budget");
			if (chosen == Answer.ARMOR || chosen == Answer.GUARD) check(target.getHealth() > 200 - DAMAGE && target.getHealth() < 200, "Actual armor/held guard reduces the ordinary projected hit");
			if (chosen == Answer.FORESIGHT) check(target.getHealth() > 200 - DAMAGE, "The actual ward remains authoritative");
			if (chosen == Answer.CALLBACK_DEFEAT) check(party.stream().noneMatch(ServerPlayer::isAlive), "A lethal callback cannot shield a later UUID in the same simultaneous band");
			else if (callback) check(callbacks == 1 && party.stream().filter(p -> p.getHealth() < 200).count() == 1,
				"Callbacks cancel, remove a snapshot victim or preserve safe non-victims: " + chosen);
			if (chosen == Answer.ONE_ATTEMPT) {
				check(target.getHealth() == 200 && fixture.master.marchPending(), "A native zero-damage veto consumes the attempt without canceling later pulses");
				target.setShiftKeyDown(false); target.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE); fixture.place(target, 0, 4.5);
			}
			if (chosen == Answer.SPENT) fixture.place(target, 0, 2.5);
		});
		fixture.at(world, 40, () -> {
			if (chosen == Answer.ONE_ATTEMPT) { check(target.getHealth() == 200, "Full defense cannot receive another band attempt"); fixture.place(target, 0, 6.5); }
		});
		fixture.at(world, 48, () -> {
			if (List.of(Answer.SIDE, Answer.INNER, Answer.OUTER, Answer.SPENT, Answer.ONE_ATTEMPT, Answer.PARRY,
				Answer.NEW_COVER, Answer.REMOVED_THIN_COVER, Answer.NEW_GAP, Answer.REMOVED_GAP, Answer.BLOCK_EXITS, Answer.BODY_BLOCK, Answer.NEW_HAZARD, Answer.UPPER_FLUID,
				Answer.INTERRUPT, Answer.NO_AI, Answer.DISPLACE, Answer.SPECTATOR, Answer.LEAVE, Answer.OWNER_DEATH, Answer.SKIPPED).contains(chosen))
				check(target.getHealth() == 200, "The declared route or cancellation prevents all later harm: " + chosen);
			check(close(fixture.master.auraRemaining(), fixture.paidAura) && fixture.readyAt() == fixture.began + StoneMarchRules.COOLDOWN, "Cancellation/miss/defense never refunds payment or rest");
			check(!fixture.accepted.tick(level.getGameTime()), "The whole action cannot restart after its final scheduled pulse");
		});
		fixture.at(world, 95, () -> {
			if (fixture.master.isAlive() && !fixture.master.isRemoved()) check(!fixture.master.guarding() && !fixture.master.state(AuraFighter.WINDUP)
				&& close(fixture.master.auraRemaining(), fixture.paidAura), "Every normal and cancelled action retains the complete exposed window through95");
			cleanup();
		});
	}

	private void partyTrial(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> setup(8)); fixture.await(world);
		fixture.at(world, 48, () -> {
			check(party.stream().allMatch(player -> close(player.getHealth(), 200 - DAMAGE)), "All eight have separate exactly-once budgets across the advancing bands");
			check(bystander.getHealth() == bystander.getMaxHealth() && blocker.getHealth() == blocker.getMaxHealth(), "Outsiders never enter the outgoing roster");
			for (Challenger player : party) check(player.getLastDamageSource() != null && player.getLastDamageSource().getEntity() == fixture.master, "Each pulse keeps the native Master source");
			cleanup();
		});
	}

	private void admissionPriority(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> setup(1)); fixture.await(world);
		fixture.at(world, 10, () -> fixture.master.setNoAi(true));
		fixture.at(world, StoneMarchRules.COOLDOWN - 1, () -> {
			fixture.master.setNoAi(false);
			check(fixture.master.canBeginMarch(level.getGameTime()), "The body is free before the signature rest expires");
			check(StoneMarch.prepare(fixture.master, target, MastersRules.STONE, fixture.admittedSequence, fixture.paidAura, level.getGameTime(), fixture.readyAt()) == null,
				"The native signature rest still declines at acceptance+219");
			check(StoneMarch.prepare(fixture.master, target, MastersRules.STONE, fixture.admittedSequence, fixture.paidAura, level.getGameTime(), level.getGameTime()) != null,
				"Only the real rest deadline, not geometry or busy state, prevented the read-only proposal");
			fixture.master.setNoAi(true);
		});
		fixture.at(world, StoneMarchRules.COOLDOWN, () -> {
			fixture.master.setNoAi(false); fixture.master.setTarget(target);
			fixture.planner().observe(target.getUUID(), fixture.master.challengers());
			check(fixture.master.canBeginMarch(level.getGameTime()), "Original rest and full cancellation recovery expired in a genuinely free native slot");
			double aura = fixture.master.auraRemaining(); long rest = fixture.readyAt(); var state = fixture.planner().state();
			BlockPos lowCover = BlockPos.containing(origin).offset(0, 0, 1);
			level.setBlockAndUpdate(lowCover, Blocks.STONE_SLAB.defaultBlockState());
			check(fixture.master.hasLineOfSight(target), "Low cover declines March while leaving the ordinary eye-line intact");
			@SuppressWarnings("unchecked") var eligible = (java.util.Set<MastersRules.Move>) invoke("ordinaryEligibility",
				new Class<?>[] {ServerLevel.class, net.minecraft.world.entity.LivingEntity.class, long.class}, level, target, level.getGameTime());
			check(!eligible.isEmpty(), "A legitimate ordinary proposal exists before the priority opening appears");
			var proposal = fixture.planner().propose(MastersRules.Move.THRUST, eligible, aura);
			level.setBlockAndUpdate(lowCover, Blocks.AIR.defaultBlockState());
			StoneMarch opening = StoneMarch.prepare(fixture.master, target, MastersRules.STONE, fixture.admittedSequence, aura, level.getGameTime(), rest);
			check(opening != null, "Removing low cover creates a new read-only priority opening in the same free tick");
			check(!(boolean) invoke("tryBeginOrdinary", new Class<?>[] {ServerLevel.class, net.minecraft.world.entity.LivingEntity.class,
				long.class, MasterOrdinaryPlanner.Proposal.class}, level, target, level.getGameTime(), proposal), "A still-current ordinary ticket cannot steal the new March priority");
			roses(2, true);
			check(StoneMarch.prepare(fixture.master, target, MastersRules.STONE, fixture.admittedSequence, aura, level.getGameTime(), rest) == null,
				"Initial noncollision hazards beyond the lane make both lateral routes unsafe");
			check(!admit(opening), "Atomic revalidation declines a prepared opening after escape hazards appear");
			roses(2, false); floor(2, false);
			check(!admit(opening), "Missing initial support cannot propagate a ground band");
			floor(2, true); upperFluid(2, true);
			check(!admit(opening), "Upper-body fluid invalidates an escape even with dry sturdy support");
			upperFluid(2, false);
			check(close(fixture.master.auraRemaining(), aura) && fixture.readyAt() == rest && fixture.planner().state().equals(state),
				"Every declined proposal leaves Aura, rest and accepted history unchanged");
			check(admit(opening) && close(fixture.master.auraRemaining(), aura - StoneMarchRules.COST), "The original exact proposal pays once after a final valid recheck");
			check(!admit(opening) && close(fixture.master.auraRemaining(), aura - StoneMarchRules.COST), "Duplicate admission of the same proposal cannot pay twice");
			cleanup();
		});
	}
	private boolean admit(StoneMarch opening) {
		return (boolean) invoke("tryBeginMarch", new Class<?>[] {ServerLevel.class, ServerPlayer.class, long.class, StoneMarch.class},
			level, target, level.getGameTime(), opening);
	}
	private Object invoke(String name, Class<?>[] types, Object... args) {
		try { var method = SwordMaster.class.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(fixture.master, args); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}

	private static void installHooks() {
		if (hooks) return; hooks = true;
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((living, source, amount) -> {
			StoneMarchTest test = active;
			return test == null || test.answer != Answer.ONE_ATTEMPT || living != test.target || source.getEntity() != test.fixture.master;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((living, source, base, taken, blocked) -> {
			StoneMarchTest test = active;
			if (test == null || source.getEntity() != test.fixture.master || !test.party.contains(living)) return;
			test.callbacks++;
			if (test.callbacks != 1 || test.answer == Answer.CALLBACK_DEFEAT) return;
			var other = test.party.stream().filter(player -> player != living).findFirst().orElseThrow();
			switch (test.answer) {
				case CALLBACK_ENTRY -> test.fixture.place(other, 0, 3.1);
				case CALLBACK_EXIT -> test.fixture.place(other, 2.5, 3.1);
				case CALLBACK_REENTRY -> { check(test.fixture.accepted.tick(test.level.getGameTime()), "Reentrant same-tick pulse is inert"); test.fixture.place(other, 2.5, 3.1); }
				case CALLBACK_BAD_CLOCK -> check(!test.fixture.accepted.tick(test.level.getGameTime() + 1), "A mismatched reentrant clock cancels before any later snapshot victim");
				case CALLBACK_COVER -> test.cover(2, true);
				case CALLBACK_BODY -> { test.fixture.place(test.bystander, -1.1, other.getZ() - test.origin.z); test.fixture.place(test.blocker, 1.1, other.getZ() - test.origin.z); }
				case CALLBACK_HAZARD -> test.roses((int) Math.floor(other.getZ() - test.origin.z), true);
				case CALLBACK_REPLACE -> {
					Vec3 position = other.position();
					test.level.removePlayerImmediately(other, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
					test.replacement = new Challenger(test.level, other.getGameProfile());
					test.replacement.setGameMode(GameType.SURVIVAL);
					test.replacement.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000); test.replacement.setHealth(200);
					test.replacement.snapTo(position.x, position.y, position.z, 180, 0); test.level.addNewPlayer(test.replacement);
					check(test.level.getPlayerByUUID(other.getUUID()) == test.replacement && test.replacement != other
						&& test.fixture.master.canHarmParticipant(test.replacement), "Native same-UUID replacement is current and otherwise lawful; only accepted body identity excludes it");
				}
				default -> test.fixture.master.setNoAi(true);
			}
		});
	}

	private void setup(int count) {
		target = add("MarchTarget", 0, 2.0); party.add(target);
		if (count == 2) party.add(add("MarchPeer", 0, 3.1));
		if (count == 8) {
			for (double z : new double[] {3.2, 4.5, 5.8}) for (int sign : new int[] {-1, 1}) party.add(add("MarchPeer" + party.size(), sign, z));
			party.add(add("MarchLast", 0, 7));
		}
		bystander = add("MarchOutside", -8, -8); blocker = add("MarchBlocker", 8, -8);
		fixture = new StoneMarchFixture(level, origin, party);
	}
	private Challenger add(String name, double x, double z) {
		Challenger player = new Challenger(level, name); player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000); player.setHealth(2000);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 180, 0); level.addNewPlayer(player); return player;
	}
	private void guard() {
		target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
		target.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE); target.setShiftKeyDown(true);
		target.setYRot(180); target.setYHeadRot(180); target.setYBodyRot(180); target.setXRot(0);
		check(AuraGuard.raise(target) && AuraGuard.facing(target, fixture.master.position()), "The real paid guard faces the fixed Stone origin");
	}
	private void cover(int forward, boolean present) {
		for (int y = 0; y < 3; y++) level.setBlockAndUpdate(BlockPos.containing(origin).offset(0, y, forward), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void thinCover(boolean present) { level.setBlockAndUpdate(BlockPos.containing(origin).offset(1, 0, 4), (present ? Blocks.IRON_BARS : Blocks.AIR).defaultBlockState()); }
	private void floor(int forward, boolean present) { level.setBlockAndUpdate(BlockPos.containing(origin).offset(0, -1, forward), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState()); }
	private void sideCover(int side, boolean present) {
		for (int y = 0; y < 3; y++) level.setBlockAndUpdate(BlockPos.containing(origin).offset(side, y, 2), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void roses(int forward, boolean present) {
		for (int side : new int[] {-3, 3}) {
			BlockPos pos = BlockPos.containing(origin).offset(side, 0, forward);
			level.setBlockAndUpdate(pos.below(), (present ? Blocks.DIRT : Blocks.STONE).defaultBlockState());
			level.setBlockAndUpdate(pos, (present ? Blocks.WITHER_ROSE : Blocks.AIR).defaultBlockState());
		}
	}
	private dev.wildercord.party.PartyRules partyRules() {
		try {
			var method = dev.wildercord.party.Parties.class.getDeclaredMethod("session", net.minecraft.server.MinecraftServer.class);
			method.setAccessible(true);
			return (dev.wildercord.party.PartyRules) StoneMarchFixture.field(method.invoke(null, level.getServer()), "rules");
		} catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private void upperFluid(int forward, boolean present) {
		for (int side : new int[] {-3, 3}) level.setBlockAndUpdate(BlockPos.containing(origin).offset(side, 1, forward),
			(present ? Blocks.WATER : Blocks.AIR).defaultBlockState());
	}
	private void cleanup() {
		active = null; if (fixture != null) fixture.master.discard();
		if (target != null) partyRules().leave(target.getUUID()); if (bystander != null) partyRules().leave(bystander.getUUID());
		party.forEach(ServerPlayer::discard); party.clear(); if (replacement != null) { replacement.discard(); replacement = null; } if (bystander != null) bystander.discard(); if (blocker != null) blocker.discard();
		cover(2, false); thinCover(false); floor(2, true); floor(5, true); sideCover(-2, false); sideCover(2, false); roses(2, false); roses(3, false); upperFluid(2, false);
		// Remove test-only flow that may have advanced during the upper-fluid cancellation scenario.
		for (int x = -12; x <= 12; x++) for (int z = -10; z <= 14; z++) for (int y = 0; y <= 2; y++) {
			BlockPos pos = BlockPos.containing(origin).offset(x, y, z);
			if (!level.getFluidState(pos).isEmpty()) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}
}
