package dev.wildercord.party;

import com.mojang.authlib.GameProfile;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.arts.ArtFields;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.VerdantArts;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Scheduler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Actual second-form performers: paid fixed releases, cancelled windups, and independently owned rain/field effects. */
public final class MastersSecondFormTimelineTest implements FabricClientGameTest {
	private static final List<Integer> MARKS = List.of(SwordString.Token.SWING.bit() | SwordString.Token.LEAP.bit(),
		SwordString.Token.SWING.bit() | SwordString.Token.LOW.bit());
	private final Map<String, Integer> completions = new HashMap<>();
	private final Map<String, Long> releaseTicks = new HashMap<>();
	private UUID owner;

	private static final class Ally extends FakePlayer {
		Ally(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}

	private static final class Probe {
		Mob front, behind, rain;
		ServerLevel level;
		Ally ally;
		boolean preFrameObserved, untouchedBeforeFrame, rainBoundaryObserved, rainWasEarly;
		int completedBefore;
		long cooldown, acceptedAt;
		float allyHealth;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		AuraApi.onString((player, art, receipt) -> {
			if (player.getUUID().equals(owner) && (art.id().equals("rising_cinders") || art.id().equals("blossom_fall"))) {
				check(receipt.struck() == null, "These explicit cone profiles do not preserve an out-of-cone last-swing victim");
				check(Effects.applying() == player && Effects.applyingCast() == null, "The accepted release retains Aura source identity");
				completions.merge(art.id(), 1, Integer::sum);
				releaseTicks.put(art.id(), player.level().getGameTime());
			}
		});
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("fill -8 99 -8 8 99 8 minecraft:stone");
			world.getServer().runCommand("fill -8 100 -8 8 108 8 minecraft:air");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				owner = player.getUUID();
				player.level().getGameRules().set(GameRules.PVP, true, server);
			});
			for (String id : new String[] {"rising_cinders", "blossom_fall"}) {
				refusals(context, world, id);
				for (String cause : new String[] {"damage", "weapon round trip", "method", "spectator", "dimension"}) {
					cancelBeforeRelease(context, world, id, cause);
				}
				releasedEffect(context, world, id);
			exitAfterRelease(context, world, id, false);
			if (id.equals("rising_cinders")) exitAfterRelease(context, world, id, true);
			}
		}
	}

	private void refusals(ClientGameTestContext context, TestSingleplayerContext world, String id) {
		context.waitTicks(90);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			prepare(player, id);
			var art = AuraApi.artOf(player, id).orElseThrow();
			check(art.cost() == 8 && art.cooldownTicks() == 80 && art.stage() == AuraRules.FLOW,
				"Second forms keep their original stage, base price and cooldown");
			check(art.string().fits(MARKS.stream().mapToInt(Integer::intValue).toArray()), "The original leap/low string remains valid");
			float before = Aura.aura(player);
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			check(!SwordStrings.perform(player, art, MARKS), "A missing weapon cannot begin a second form");
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.SOVEREIGN, 4500, before, 0));
			check(!SwordStrings.perform(player, art, MARKS), "A different method cannot begin the registered art");
			prepare(player, id);
			player.setXRot(Float.NaN);
			check(!SwordStrings.perform(player, art, MARKS), "Non-finite server aim cannot begin a second form");
			player.setXRot(0);
			check(Aura.aura(player) == before && !MastersArts.committed(player), "Refusals never pay or advertise a commitment");
		});
	}

	private void cancelBeforeRelease(ClientGameTestContext context, TestSingleplayerContext world, String id, String cause) {
		context.waitTicks(90);
		Probe probe = new Probe();
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			prepare(player, id);
			probe.front = foe(player.level(), .5, 2.5);
			accept(player, id, probe);
			switch (cause) {
				case "damage" -> {
					player.setInvulnerableTime(0);
					check(player.hurtServer(player.level(), player.level().damageSources().generic(), 1), "Cancellation uses actual accepted damage");
				}
				case "weapon round trip" -> {
					int slot = player.getInventory().getSelectedSlot();
					player.getInventory().setSelectedSlot((slot + 1) % 9);
					player.getInventory().setSelectedSlot(slot);
				}
				case "method" -> player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.SOVEREIGN, 4500, Aura.aura(player), 0));
				case "spectator" -> player.setGameMode(GameType.SPECTATOR);
				case "dimension" -> {
					player.setNoGravity(true);
					player.teleportTo(server.getLevel(Level.NETHER), .5, 120, .5, java.util.Set.of(), 0, 0, false);
				}
				default -> throw new AssertionError(cause);
			}
			check(MastersArts.committed(player), "A cancelled second form retains paid recovery");
		});
		context.waitTicks(MastersStyleRules.of(id).windup() + 3);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			check(probe.preFrameObserved && probe.untouchedBeforeFrame, "The windup has no art damage, fire, or field");
			check(probe.front.getHealth() == 100 && !probe.front.isOnFire(), cause + " cancels the pending second-form hit");
			check(ArtFields.count(player, VerdantArts.BLOSSOM) == 0, cause + " never creates an unreleased field");
			check(completions.getOrDefault(id, 0) == probe.completedBefore, "Cancelled windups never run completion hooks");
			check(SwordStrings.readyAt(player, id) == probe.cooldown, "Cancellation preserves the original paid cooldown");
			player.setGameMode(GameType.SURVIVAL);
			if (player.level() != probe.level) player.teleportTo(probe.level, .5, 100, .5, java.util.Set.of(), 0, 0, false);
			player.setNoGravity(false);
			probe.front.discard();
		});
	}

	private void releasedEffect(ClientGameTestContext context, TestSingleplayerContext world, String id) {
		context.waitTicks(90);
		boolean cinders = id.equals("rising_cinders");
		var style = MastersStyleRules.of(id);
		Probe probe = new Probe();
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			prepare(player, id);
			probe.front = foe(player.level(), .5, 2.5);
			probe.behind = foe(player.level(), .5, -2.5);
			player.setLastHurtMob(probe.behind);
			player.setXRot(90); // A vertical camera still retains the server's yaw and the art's level cone.
			accept(player, id, probe);
			player.setYRot(180);
			if (cinders) Scheduler.later(style.windup() + ArtRules.CINDERS_RAIN_DELAY - 1, () -> {
				probe.rainBoundaryObserved = true;
				probe.rainWasEarly = probe.rain == null || probe.rain.getHealth() < 100;
			});
		});
		context.waitTicks(style.windup() + 2);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			check(probe.preFrameObserved && probe.untouchedBeforeFrame, "Neither second form releases during its windup");
			check(probe.front.getHealth() < 100 && probe.behind.getHealth() == 100,
				"The active cut uses its accepted cone, not free-look or the old struck target behind it");
			check(completions.getOrDefault(id, 0) == probe.completedBefore + 1, "Each actual release completes exactly once");
			check(releaseTicks.get(id) == probe.acceptedAt + style.windup(), "The actual release matches its announced server active tick");
			check(Effects.applying() == null, "The release restores source scope afterward");
			probe.front.snapTo(.5, 100, 2.5, 0, 0);
			probe.front.setDeltaMovement(Vec3.ZERO);
			probe.front.clearFire();
			probe.ally = new Ally(player.level(), cinders ? "CindersAlly" : "BlossomAlly");
			probe.ally.setGameMode(GameType.SURVIVAL);
			probe.ally.snapTo(1.5, 100, 2.5, 0, 0);
			probe.ally.setNoGravity(true);
			player.level().addNewPlayer(probe.ally);
			probe.ally.setHealth(cinders ? 20 : 10);
			probe.allyHealth = probe.ally.getHealth();
			probe.rain = foe(player.level(), -.5, 2.5);
			check(ArtKit.harmable(player, probe.ally), "Before accepting the late invitation this player is harmable");
			var rules = Parties.session(player.level().getServer()).rules;
			long now = player.level().getGameTime();
			check(rules.invite(player.getUUID(), probe.ally.getUUID(), now) == PartyRules.Result.OK, "A released effect's future recipient is invited");
			check(rules.accept(probe.ally.getUUID(), player.getUUID(), now) == PartyRules.Result.OK, "Membership changes after release, before the delayed effect");
			player.setInvulnerableTime(0);
			check(player.hurtServer(player.level(), player.level().damageSources().generic(), 1), "Actual damage cancels the remaining physical recovery pose");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.SOVEREIGN, 4500, Aura.aura(player), 0));
			if (!cinders) check(ArtFields.count(player, VerdantArts.BLOSSOM) == 1, "Blossom releases exactly one original field");
		});
		context.waitTicks(cinders ? ArtRules.CINDERS_RAIN_DELAY + 2 : 21);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			check(SwordStrings.readyAt(player, id) == probe.cooldown, "The independent effect never pays/rests a second time");
			check(completions.getOrDefault(id, 0) == probe.completedBefore + 1, "The independent effect never duplicates completion hooks");
			if (cinders) {
				check(probe.rainBoundaryObserved && !probe.rainWasEarly, "Cinder rain keeps its original 12-tick post-release delay");
				check(probe.rain.getHealth() < 100 && probe.rain.isOnFire(), "Already-released cinders still damage and ignite after interruption and weapon/method loss");
				check(probe.ally.getHealth() == probe.allyHealth && !probe.ally.isOnFire(), "Rain rechecks party admission and cannot damage or ignite the new ally");
			} else {
				check(ArtFields.count(player, VerdantArts.BLOSSOM) == 1, "The field outlives the physical pose and weapon/method loss");
				check(probe.ally.getHealth() > probe.allyHealth && !probe.ally.hasEffect(MobEffects.SLOWNESS), "The late ally is mended and never slowed");
				check(probe.rain.hasEffect(MobEffects.SLOWNESS), "An enemy entering after release is slowed by the original field pulse");
				probe.allyHealth = probe.ally.getHealth();
				check(Parties.session(server).rules.leave(probe.ally.getUUID()) == PartyRules.Result.OK, "Membership can change again during the field");
			}
		});
		if (!cinders) {
			context.waitTicks(20);
			world.getServer().runOnServer(server -> {
				check(probe.ally.getHealth() == probe.allyHealth && probe.ally.hasEffect(MobEffects.SLOWNESS),
					"The next pulse stops healing and admits the now-unallied recipient's slow");
			});
			context.waitTicks(ArtRules.BLOSSOM_TICKS);
			world.getServer().runOnServer(server -> check(ArtFields.count(server.getPlayerList().getPlayers().getFirst(), VerdantArts.BLOSSOM) == 0,
				"The field expires rather than being kept alive by the animation"));
		}
		world.getServer().runOnServer(server -> {
			Parties.session(server).rules.leave(probe.ally.getUUID());
			Parties.session(server).rules.leave(owner);
			probe.ally.discard(); probe.front.discard(); probe.behind.discard(); probe.rain.discard();
		});
	}


	/** A released effect retains its ordinary lifetime only while its original world/owner still exists. */
	private void exitAfterRelease(ClientGameTestContext context, TestSingleplayerContext world, String id, boolean sourceLeaves) {
		context.waitTicks(90);
		Probe probe = new Probe();
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			prepare(player, id);
			probe.front = foe(player.level(), .5, 2.5);
			accept(player, id, probe);
		});
		context.waitTicks(MastersStyleRules.of(id).windup() + 2);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			check(probe.front.getHealth() < 100, "The cut really released before its owner/source leaves");
			probe.front.snapTo(.5, 100, 2.5, 0, 0);
			probe.front.setDeltaMovement(Vec3.ZERO);
			probe.front.clearFire();
			probe.rain = foe(player.level(), -.5, 2.5);
			if (sourceLeaves) {
				check(probe.front.teleportTo(server.getLevel(Level.NETHER), .5, 120, 2.5, java.util.Set.of(), 0, 0, false),
					"The lifted source really changes dimension");
			} else {
				player.setNoGravity(true);
				player.teleportTo(server.getLevel(Level.NETHER), .5, 120, .5, java.util.Set.of(), 0, 0, false);
				check(player.level() != probe.level, "The owner really changes dimension");
			}
		});
		context.waitTicks(ArtRules.CINDERS_RAIN_DELAY + 3);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			check(probe.rain.getHealth() == 100 && !probe.rain.isOnFire(), "Rain cannot act through a departed owner or cross-world source");
			check(ArtFields.count(player, VerdantArts.BLOSSOM) == 0, "A released field ends when its owner leaves the world");
			if (player.level() != probe.level) player.teleportTo(probe.level, .5, 100, .5, java.util.Set.of(), 0, 0, false);
			player.setNoGravity(false);
			var transferred = server.getLevel(Level.NETHER).getEntity(probe.front.getUUID());
			if (transferred != null) transferred.discard();
			probe.front.discard(); probe.rain.discard();
		});
	}

	private void accept(ServerPlayer player, String id, Probe probe) {
		var art = AuraApi.artOf(player, id).orElseThrow();
		var style = MastersStyleRules.of(id);
		probe.level = player.level();
		probe.completedBefore = completions.getOrDefault(id, 0);
		long now = player.level().getGameTime();
		probe.acceptedAt = now;
		double cost = SwordStrings.price(player, art);
		float before = Aura.aura(player);
		check(SwordStrings.perform(player, art, MARKS), "The checked " + id + " enters its paid fixed windup");
		check(Math.abs(Aura.aura(player) - (before - cost)) < .001, "Acceptance pays the unchanged effective price exactly once");
		probe.cooldown = now + SwordStrings.rest(player, art);
		check(SwordStrings.readyAt(player, id) == probe.cooldown, "The unchanged individual cooldown starts at acceptance");
		float paid = Aura.aura(player);
		check(!SwordStrings.perform(player, art, MARKS) && Aura.aura(player) == paid, "A repeated request cannot start or pay twice");
		Scheduler.later(style.windup() - 1, () -> {
			probe.preFrameObserved = true;
			probe.untouchedBeforeFrame = probe.front.getHealth() == 100 && !probe.front.isOnFire()
				&& ArtFields.count(player, VerdantArts.BLOSSOM) == 0 && completions.getOrDefault(id, 0) == probe.completedBefore;
		});
	}

	private static void prepare(ServerPlayer player, String id) {
		player.setGameMode(GameType.SURVIVAL);
		player.teleportTo(.5, 100, .5);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.setYRot(0); player.setXRot(0);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(ArtRules.art(id).method(), AuraRules.SOVEREIGN, 4500, 160, 0));
	}

	private static Mob foe(ServerLevel level, double x, double z) {
		Mob foe = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(foe != null, "The deterministic second-form target exists");
		foe.addTag("wildercord.rolled");
		foe.setNoAi(true); foe.setNoGravity(true);
		foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		foe.setHealth(100);
		foe.snapTo(x, 100, z, 180, 0);
		level.addFreshEntity(foe);
		return foe;
	}

	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
