package dev.wildercord.cast;

import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.wildercord.cast.NextSignatureNative.check;

/** Four paid deliveries through one genuinely connected spectator, using the existing two-client gate. */
public final class SpectatorPairedCases {
	public static final List<String> CASES = List.of("SPECTATOR_BOLT_VENOM", "SPECTATOR_SPARK_VENOM",
		"SPECTATOR_RAY_VENOM", "SPECTATOR_TOUCH_VENOM");
	private static final List<RuneDef> SHAPES = List.of(Runes.BOLT, Runes.SPARK, Runes.RAY, Runes.TOUCH);
	private UUID hostId, peerId;
	private Vec3 origin;
	private Pillager victim;
	private GameType hostMode, peerMode;
	private float spectatorHealth, victimHealth;

	public void runConnectedPair(ClientGameTestContext context, TestServerContext server, UUID host, UUID peer,
		BiConsumer<String, Map<String, String>> observed, Consumer<String> completed) {
		check(!host.equals(peer), "Spectator and Survival caster are distinct real profiles");
		hostId = host; peerId = peer;
		server.waitFor(s -> !dev.wildercord.aura.MastersArts.committed(host(s)) && !CastLock.locked(host(s)), NextSignatureRules.REST + 5);
		server.runOnServer(s -> {
			hostMode = host(s).gameMode.getGameModeForPlayer(); peerMode = peer(s).gameMode.getGameModeForPlayer();
			origin = host(s).position();
			for (int x = -4; x <= 4; x++) for (int z = -4; z <= 12; z++)
				host(s).level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
		});
		try {
			for (int i = 0; i < SHAPES.size(); i++) {
				delivery(context, server, CASES.get(i), SHAPES.get(i), observed);
				completed.accept(CASES.get(i));
			}
		} finally {
			server.runOnServer(s -> {
				cleanup();
				host(s).setGameMode(hostMode); peer(s).setGameMode(peerMode);
				check(host(s).gameMode.getGameModeForPlayer() == hostMode && peer(s).gameMode.getGameModeForPlayer() == peerMode,
					"Both actual connected players regain their original modes before terminal witnesses");
			});
		}
	}

	private void delivery(ClientGameTestContext context, TestServerContext server, String id, RuneDef shape,
		BiConsumer<String, Map<String, String>> observed) {
		var proof = new HashMap<String, String>();
		server.runOnServer(s -> {
			var actor = host(s); var spectator = peer(s);
			prepare(actor); prepare(spectator);
			NextSignatureNative.teach(actor); spectator.setGameMode(GameType.SPECTATOR);
			check(actor.teleportTo(actor.level(), origin.x, origin.y, origin.z, Set.of(), 0, 0, false), "Real Survival caster reaches its fixture");
			actor.setDeltaMovement(Vec3.ZERO); NextSignatureNative.ground(actor);
			check(spectator.teleportTo(actor.level(), origin.x, origin.y, origin.z + 1, Set.of(), 180, 0, false), "Real spectator reaches the near cast segment");
			spectator.setDeltaMovement(Vec3.ZERO);
			victim = EntityTypes.PILLAGER.create(actor.level(), EntitySpawnReason.COMMAND);
			check(victim != null, "Fresh exact hostile fixture is constructible");
			victim.setNoAi(true); victim.setSilent(true); victim.snapTo(origin.x, origin.y, origin.z + 2.8, 180, 0);
			check(actor.level().addFreshEntity(victim), "Fresh exact hostile enters the native world");
		});
		context.waitTicks(3);
		server.runOnServer(s -> {
			var actor = host(s); var spectator = peer(s);
			check(actor.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && spectator.isSpectator()
				&& actor.connection.hasClientLoaded() && spectator.connection.hasClientLoaded(), "Both genuine loaded connections retain the exact caster/spectator roles");
			check(actor.level() == spectator.level() && actor.level().getEntity(victim.getUUID()) == victim
				&& Targets.canHarm(actor, victim) && !Targets.canHarm(actor, spectator), "Hostile victim is legally hittable while the real spectator is protected");
			var eye = actor.getEyePosition(); var end = eye.add(0, 0, 3);
			var near = spectator.getBoundingBox().clip(eye, end); var far = victim.getBoundingBox().clip(eye, end);
			check(near.isPresent() && far.isPresent() && eye.distanceToSqr(near.get()) < eye.distanceToSqr(far.get()),
				"Actual spectator bounds intersect the paid cast segment strictly before its exact hostile victim");
			spectatorHealth = spectator.getHealth(); victimHealth = victim.getHealth();
			check(victimHealth == victim.getMaxHealth() && !victim.hasEffect(MobEffects.POISON)
				&& !spectator.hasEffect(MobEffects.POISON), "Each paid shape starts with a fresh unmarked hostile and unmarked spectator");
			NextSignatureNative.cast(actor, shape, Runes.VENOM);
			proof.put("shape", shape.path()); proof.put("paidMana", Float.toString(Spellbooks.mana(actor)));
			proof.put("victimUuid", victim.getUUID().toString()); proof.put("victimEntity", Integer.toString(victim.getId()));
			proof.put("victimHealthBefore", Float.toString(victimHealth)); proof.put("spectatorHealthBefore", Float.toString(spectatorHealth));
			proof.put("casterMode", "survival"); proof.put("spectatorMode", "spectator");
		});
		// Preserve the original delivery window, including native projectile travel and ordinary status ticking.
		context.waitTicks(20);
		server.runOnServer(s -> verify(s, shape));
		observed.accept(id, Map.copyOf(proof));
		server.runOnServer(s -> { verify(s, shape); cleanup(); });
	}

	private void verify(MinecraftServer server, RuneDef shape) {
		var spectator = peer(server);
		check(victim != null && host(server).level().getEntity(victim.getUUID()) == victim && victim.isAlive()
			&& victim.getHealth() < victimHealth && victim.hasEffect(MobEffects.POISON),
			"Paid " + shape.path() + " passes the actual spectator and damages/marks its exact hostile victim");
		check(spectator.isSpectator() && spectator.getHealth() == spectatorHealth && !spectator.hasEffect(MobEffects.POISON),
			"The same genuinely connected spectator remains unharmed and unmarked");
	}

	private static void prepare(ServerPlayer player) {
		Charging.forget(player); player.removeAllEffects(); player.setAbsorptionAmount(0); player.setHealth(player.getMaxHealth());
		player.setPermanentlyInvulnerable(false);
		for (var slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET))
			player.setItemSlot(slot, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
	}
	private void cleanup() { if (victim != null) { victim.discard(); victim = null; } }
	private ServerPlayer host(MinecraftServer server) { return connected(server, hostId); }
	private ServerPlayer peer(MinecraftServer server) { return connected(server, peerId); }
	private static ServerPlayer connected(MinecraftServer server, UUID id) {
		var player = server.getPlayerList().getPlayer(id);
		check(player != null && player.isAlive() && player.connection != null && player.connection.player == player
			&& server.getPlayerList().getPlayers().size() == 2, "Spectator delivery uses an exact current body of the two real connections");
		return player;
	}

	/** Vanilla synchronizes a remote mob's effect particles, not its private active-effect map. Read only that native metadata. */
	@SuppressWarnings("unchecked") public static boolean hasPoisonParticles(LivingEntity entity) {
		try {
			var field = LivingEntity.class.getDeclaredField("DATA_EFFECT_PARTICLES"); field.setAccessible(true);
			var key = (EntityDataAccessor<List<ParticleOptions>>) field.get(null);
			var expected = (ColorParticleOption) MobEffects.POISON.value().createParticleOptions(new MobEffectInstance(MobEffects.POISON, 1));
			return entity.getEntityData().get(key).stream().anyMatch(option -> option instanceof ColorParticleOption color
				&& color.getType() == expected.getType() && color.getRed() == expected.getRed()
				&& color.getGreen() == expected.getGreen() && color.getBlue() == expected.getBlue());
		} catch (ReflectiveOperationException failure) { throw new AssertionError("Read-only native poison particle metadata", failure); }
	}
}
