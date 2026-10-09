package dev.wildercord.aura.world;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.MethodsBArtRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.cast.Effects;
import dev.wildercord.aura.world.MethodsBSignatureRules.Signature;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * The methods-b pack played for real: each of Echo, Dawn and Venom learned from its manual (the later two by the confirmed switch),
 * two of its arts performed through {@link SwordStrings} with their outcomes on a live foe, then its Master challenged, the trial
 * accepted, a plain technique taken in the face and the signature met twice: once answered (no harm) and once not (cut and afflicted).
 */
public final class MethodsBMastersClientTest implements FabricClientGameTest {
	private static final float FOE_HEALTH = 200;

	private BlockPos stage;
	private LivingEntity foe;
	private SwordMaster master;
	private long began, stepped;
	private int hurt;
	private float mark;

	private static void check(boolean result, String message) {
		if (!result) throw new AssertionError(message);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) {
				world.getServer().runCommand(command);
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
				for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) {
					level.setBlockAndUpdate(stage.offset(x, 0, z), Blocks.STONE.defaultBlockState());
					for (int y = 1; y <= 4; y++) level.setBlockAndUpdate(stage.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
				player.setGameMode(GameType.SURVIVAL);
				home(player, 0);
			});
			context.waitTicks(5);

			String[] methods = {MethodsBArtRules.ECHO, MethodsBArtRules.DAWN, MethodsBArtRules.VENOM};
			for (int i = 0; i < methods.length; i++) {
				learn(world, methods[i], i > 0);
				arts(context, world, methods[i]);
			}
			for (int school : MethodsBMasters.SCHOOLS) trial(context, world, school);
		}
	}

	/** Reads the method's manual. A player who already breathes another method reads twice: once asked, once confirmed. */
	private void learn(TestSingleplayerContext world, String method, boolean switching) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			if (!switching) player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
			String before = Aura.data(player).method();
			player.setItemInHand(InteractionHand.MAIN_HAND, BreathingManualItem.of(method));
			ItemStack manual = player.getMainHandItem();
			manual.getItem().use(level, player, InteractionHand.MAIN_HAND);
			if (switching) {
				check(Aura.data(player).method().equals(before) && !manual.isEmpty(), method + ": the first read of a new manual only asks to switch");
				manual.getItem().use(level, player, InteractionHand.MAIN_HAND);
			}
			check(Aura.data(player).method().equals(method), method + ": reading the manual teaches it, now " + Aura.data(player).method());
			check(Aura.data(player).stage() >= AuraRules.GLOW && manual.isEmpty(), method + ": learning sets a stage and uses up the manual");
			check(AuraApi.arts(method).size() == 5, method + ": the method has its five arts");
		});
	}

	/** The method's first two arts, performed through the real string path on a foe two blocks ahead. */
	private void arts(ClientGameTestContext context, TestSingleplayerContext world, String method) {
		var server = world.getServer();
		for (int slot = 0; slot < 2; slot++) {
			final int s = slot;
			server.runOnServer(srv -> {
				ServerPlayer player = player(srv);
				armed(player, method);
				home(player, 0);
				// Poison never takes on the undead (vanilla), so Venom practises on a living illager; the others on a husk.
				foe = foe(player.level(), Vec3.atBottomCenterOf(stage.above()).add(0, 0, 2), method.equals(MethodsBArtRules.VENOM));
			});
			context.waitTicks(3);
			server.runOnServer(srv -> {
				ServerPlayer player = player(srv);
				AuraApi.StringArt art = AuraApi.arts(method).get(s);
				List<Integer> marks = art.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList();
				check(SwordStrings.perform(player, art, marks), art.id() + ": the string is admitted");
				mark = foe.getHealth();
			});
			String id = AuraApi.arts(method).get(s).id();
			switch (id) {
				case "ringing_cut" -> {
					// The cut lands, then its ring strikes the same foe again and it reels.
					server.waitFor(srv -> foe.getHealth() < mark, 60);
					server.runOnServer(srv -> {
						check(!foe.hasEffect(MobEffects.SLOWNESS), "Ringing Cut: the first cut does not reel yet");
						mark = foe.getHealth();
					});
					server.waitFor(srv -> foe.getHealth() < mark && foe.hasEffect(MobEffects.SLOWNESS), 40);
				}
				case "resonant_chord" -> server.waitFor(srv -> foe.getHealth() < mark && foe.hasEffect(MobEffects.SLOWNESS), 60);
				case "first_light" -> server.waitFor(srv -> foe.getHealth() < mark && foe.hasEffect(MobEffects.GLOWING), 60);
				case "sunrise_arc" -> server.waitFor(srv -> foe.getHealth() < mark && foe.hasEffect(MobEffects.BLINDNESS) && foe.hasEffect(MobEffects.GLOWING), 60);
				case "fang_strike" -> server.waitFor(srv -> foe.getHealth() < mark && foe.hasEffect(MobEffects.POISON), 60);
				case "spitting_cobra" -> server.waitFor(srv -> foe.hasEffect(MobEffects.POISON) && foe.hasEffect(MobEffects.WEAKNESS), 60);
				default -> throw new AssertionError("Unexpected art in slot " + s + ": " + id);
			}
			server.runOnServer(srv -> {
				dev.wildercord.Wildercord.LOGGER.info("[methods-b] {} took {} -> {} {}", id, FOE_HEALTH, foe.getHealth(), foe.getActiveEffects());
				check(SwordStrings.readyAt(player(srv), id) > srv.overworld().getGameTime(), id + ": the art starts its cooldown");
			});
			server.waitFor(srv -> !MastersArts.committed(player(srv)), 60);
			server.runOnServer(srv -> foe.discard());
		}
	}

	/** Challenge, accept, take a plain technique, then the signature answered and unanswered. */
	private void trial(ClientGameTestContext context, TestSingleplayerContext world, int school) {
		var server = world.getServer();
		String method = MethodsBMasters.id(school);
		String plain = switch (school) {
			case MethodsBMasters.ECHO -> "tuning_fork";
			case MethodsBMasters.DAWN -> "sunbeam";
			default -> "viper_bite";
		};
		String signatureKey = switch (school) {
			case MethodsBMasters.ECHO -> "tolling_bell";
			case MethodsBMasters.DAWN -> "noon_glare";
			default -> "serpent_coil";
		};
		Signature signature = MethodsBMasters.signature(signatureKey);
		check(signature != null, signatureKey + " is a signature");
		server.runOnServer(srv -> {
			ServerPlayer player = player(srv);
			armed(player, method);
			home(player, 0);
			check(SwordMaster.challenge(player, school) == 1, method + ": the challenge spawns a Master");
			var found = player.level().getEntitiesOfClass(SwordMaster.class, new AABB(player.blockPosition()).inflate(16), m -> m.isAlive());
			check(found.size() == 1, method + ": exactly one Master answers, found " + found.size());
			master = found.getFirst();
			check(master.school() == school, method + ": the Master is of the asked school, got " + master.school());
			master.setNoAi(true); // Its AI is advanced explicitly, one step per server tick.
			check(SwordMaster.ready(player) == 1, method + ": the challenger accepts and closes the lobby");
			master.customServerAiStep(player.level());
			check(master.started() && master.challengerCount() == 1, method + ": the consented trial begins with one challenger");
		});

		// A plain technique: a readable tell, then the cut.
		cast(context, world, plain, null, false);
		server.runOnServer(srv -> check(hurt > 0, plain + ": standing in its line is cut"));

		// The signature answered: no harm and no affliction.
		cast(context, world, signatureKey, signature, true);
		server.runOnServer(srv -> {
			ServerPlayer player = player(srv);
			check(hurt == 0, signatureKey + ": its answer takes no harm, hurt " + hurt);
			check(!player.hasEffect(affliction(signature)), signatureKey + ": its answer is not afflicted");
		});
		// The signature unanswered: cut and afflicted.
		cast(context, world, signatureKey, signature, false);
		server.runOnServer(srv -> {
			ServerPlayer player = player(srv);
			check(hurt > 0, signatureKey + ": staying put is cut");
			check(player.hasEffect(affliction(signature)), signatureKey + ": staying put is afflicted");
			dev.wildercord.Wildercord.LOGGER.info("[methods-b] {} signature {} verified", method, signatureKey);
			master.discard();
			player.removeAllEffects();
		});
		context.waitTicks(2);
	}

	private static Holder<MobEffect> affliction(Signature signature) {
		return switch (signature) {
			case TOLL -> MobEffects.NAUSEA;
			case GLARE -> MobEffects.BLINDNESS;
			case COIL -> MobEffects.POISON;
		};
	}

	/**
	 * Begins {@code key} with the player three blocks in front, then steps the Master once per server tick to the chain's end.
	 * No harm may land before the first impact. With {@code answer}, two ticks before each impact (after the aim has locked) the
	 * player answers the signature: leaves the toll's lane, turns from the glare, or steps into the coil's gap.
	 */
	private void cast(ClientGameTestContext context, TestSingleplayerContext world, String key, Signature signature, boolean answer) {
		var server = world.getServer();
		var technique = MasterTechniques.forSchool(master.school()).stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
		int first = technique.impact(0), last = technique.impact(technique.strikes().size() - 1);
		check(first >= 12 && first <= 14, key + ": its first tell reads at 12 to 14 ticks, got " + first);
		server.runOnServer(srv -> {
			ServerPlayer player = player(srv);
			player.removeAllEffects();
			guarded(player);
			home(player, 0);
			master.snapTo(stage.getX() + 0.5, stage.getY() + 1, stage.getZ() + 3.5, 180, 0);
			master.setDeltaMovement(Vec3.ZERO);
			master.setTarget(player);
		});
		context.waitTicks(3);
		server.runOnServer(srv -> {
			ServerLevel level = player(srv).level();
			check(master.beginNamedTechnique(key), key + ": the Master begins it");
			began = level.getGameTime();
			stepped = began - 1;
			hurt = 0;
		});
		server.waitFor(srv -> {
			ServerPlayer player = player(srv);
			ServerLevel level = player.level();
			long now = level.getGameTime();
			if (now == stepped) return false;
			check(now == stepped + 1, key + ": the fixture stepped every tick, skipped to " + now + " from " + stepped);
			stepped = now;
			Effects.readyToHurt(player);
			player.setHealth(player.getMaxHealth());
			if (answer) for (int i = 0; i < technique.strikes().size(); i++) if (now == began + technique.impact(i) - 2) answer(player, signature, i);
			master.customServerAiStep(level);
			if (player.getHealth() < player.getMaxHealth()) {
				check(now - began >= first, key + ": no harm before the first impact, hurt at " + (now - began));
				hurt++;
			}
			if (now - began == 1) check(master.state(AuraFighter.WINDUP), key + ": the tell shows as a windup");
			return now >= began + last + 1;
		}, last + 40);
	}

	/** The answer to each signature, given after the aim has locked. */
	private void answer(ServerPlayer player, Signature signature, int strike) {
		ServerLevel level = player.level();
		Vec3 at = player.position();
		switch (signature) {
			case TOLL -> {
				// Leave the lane: three blocks to the side. The second toll rings down the same lane, wherever the Master turns.
				if (strike == 0) player.teleportTo(level, at.x + 3, at.y, at.z, Set.of(), player.getYRot(), 0, false);
			}
			// Turn the eyes from the light.
			case GLARE -> player.teleportTo(level, at.x, at.y, at.z, Set.of(), 180, 0, false);
			case COIL -> {
				if (strike != 0) return;
				// Step into the gap: the coil's open side, three blocks from the Master, set by when it began.
				Vec3 aim = new Vec3(at.x - master.getX(), 0, at.z - master.getZ()).normalize();
				Vec3 side = new Vec3(-aim.z, 0, aim.x);
				Vec3 gap = MethodsBMasters.rotate(aim, side, MethodsBSignatureRules.gapDegrees(began)).scale(3);
				player.teleportTo(level, master.getX() + gap.x, at.y, master.getZ() + gap.z, Set.of(), player.getYRot(), 0, false);
			}
		}
		player.setDeltaMovement(Vec3.ZERO);
	}

	/** At the stage's centre, facing +Z (toward the Master's mark and the foes). */
	private void home(ServerPlayer player, float yaw) {
		player.teleportTo(player.level(), stage.getX() + 0.5, stage.getY() + 1, stage.getZ() + 0.5, Set.of(), yaw, 0, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setShiftKeyDown(false);
		player.setSprinting(false);
	}

	/** High in the method, full of Aura, a sword in hand, no cooldowns. */
	private static void armed(ServerPlayer player, String method) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, AuraRules.SOVEREIGN, AuraRules.threshold(AuraRules.SOVEREIGN),
			AuraRules.capacity(AuraRules.SOVEREIGN), 0));
		player.setAttached(SwordStrings.COOLDOWNS, SwordStrings.Cooldowns.NONE);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
	}

	/** Deep health and resistance, so a Master's cut is measured, never fatal. */
	private static void guarded(ServerPlayer player) {
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 2, false, false));
		player.setHealth(player.getMaxHealth());
	}

	private static LivingEntity foe(ServerLevel level, Vec3 at, boolean living) {
		net.minecraft.world.entity.Mob foe = living ? EntityTypes.VINDICATOR.create(level, EntitySpawnReason.COMMAND)
			: EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(foe != null, "A foe to practise on");
		foe.addTag("wildercord.rolled");
		foe.setNoAi(true);
		foe.setPersistenceRequired();
		foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(FOE_HEALTH);
		foe.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		foe.setHealth(FOE_HEALTH);
		foe.snapTo(at.x, at.y, at.z, 180, 0);
		check(level.addFreshEntity(foe), "The foe is admitted");
		return foe;
	}
}
