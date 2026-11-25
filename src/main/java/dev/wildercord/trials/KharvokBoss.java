package dev.wildercord.trials;

import dev.wildercord.monster.Tempering;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.wildercord.trials.KharvokRules.*;

/**
 * Kharvok, the Crucible Sovereign: Trial II foundry boss combining Anvil Shatter slams,
 * cooperative Crucible Vent quenching, and Crucible Meltdown cooling soak rings.
 */
public final class KharvokBoss extends PathfinderMob implements Enemy, TrialBoss {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> PHASE_TWO = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> APEX = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Long> STAGGER_UNTIL = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> VENT_MASK = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SOAK_COUNT = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SOAK_FILLED = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<BlockPos> ARENA_CENTER_SYNC = SynchedEntityData.defineId(KharvokBoss.class, EntityDataSerializers.BLOCK_POS);

	private final ServerBossEvent bar = new ServerBossEvent(
		getUUID(), getType().getDescription(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	private final Set<UUID> contributors = new LinkedHashSet<>();

	private @Nullable BlockPos arenaCenter, gatePos, treasuryPos;
	private int partySize = 1;
	private int completedAttacks;
	private int quietTicks;
	private boolean moveImpacted;
	private long readyAtTick;

	public KharvokBoss(EntityType<? extends KharvokBoss> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		addTag(Tempering.TAG);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
			.add(Attributes.MAX_HEALTH, BASE_HEALTH)
			.add(Attributes.ARMOR, 12.0)
			.add(Attributes.ARMOR_TOUGHNESS, 8.0)
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.ATTACK_DAMAGE, 13.0)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.95);
	}

	@Override
	protected void registerGoals() {}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, IDLE);
		builder.define(BEGAN, 0L);
		builder.define(AIM_YAW, 0.0F);
		builder.define(PHASE_TWO, false);
		builder.define(APEX, false);
		builder.define(STAGGER_UNTIL, 0L);
		builder.define(VENT_MASK, 0);
		builder.define(SOAK_COUNT, 0);
		builder.define(SOAK_FILLED, 0);
		builder.define(ARENA_CENTER_SYNC, BlockPos.ZERO);
	}

	@Override
	public void configureTrial(BlockPos center, BlockPos gate, BlockPos treasury, int partyCount, boolean apex) {
		this.arenaCenter = center.immutable();
		this.entityData.set(ARENA_CENTER_SYNC, this.arenaCenter);
		this.entityData.set(APEX, apex);
		this.gatePos = gate.immutable();
		this.treasuryPos = treasury.immutable();
		this.partySize = Math.max(1, partyCount);
		double scaledHp = maxHealthFor(this.partySize, apex);
		var attr = getAttribute(Attributes.MAX_HEALTH);
		if (attr != null) {
			attr.setBaseValue(scaledHp);
		}
		setHealth((float) scaledHp);
		bar.setColor(apex ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
		updateBarSubtitle(IDLE);
		snapTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 180.0F, 0.0F);
		setYHeadRot(getYRot());
		yBodyRot = getYRot();
		this.readyAtTick = (level() != null ? level().getGameTime() : 0L) + 12L;
	}

	public int move() { return entityData.get(MOVE); }
	public float elapsed(float partial) { return (level().getGameTime() - entityData.get(BEGAN)) + partial; }
	public float aimYaw() { return entityData.get(AIM_YAW); }
	public boolean phaseTwo() { return entityData.get(PHASE_TWO); }
	public boolean apex() { return entityData.get(APEX); }
	public boolean staggered() { return level().getGameTime() < entityData.get(STAGGER_UNTIL); }
	public int ventMask() { return entityData.get(VENT_MASK); }
	public int soakCount() { return entityData.get(SOAK_COUNT); }
	public int soakFilledMask() { return entityData.get(SOAK_FILLED); }

	public BlockPos effectiveCenter() {
		BlockPos synced = entityData.get(ARENA_CENTER_SYNC);
		if (arenaCenter != null) return arenaCenter;
		return !BlockPos.ZERO.equals(synced) ? synced : blockPosition();
	}

	private boolean eligible(ServerPlayer player) {
		if (player.level() != level() || !player.isAlive() || player.isCreative() || player.isSpectator()) {
			return false;
		}
		Vec3 d = player.position().subtract(Vec3.atBottomCenterOf(effectiveCenter()));
		return VaelkhorRules.withinArena(d.x, d.y, d.z);
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (!isAlive()) return;
		keepInsideArena();
		if (server.getDifficulty() == Difficulty.PEACEFUL || !dev.wildercord.aura.world.Guardians.enabled()) {
			abortTrial(server);
			return;
		}

		List<ServerPlayer> challengers = server.players().stream().filter(this::eligible).limit(MAX_PARTY_PLAYERS).toList();
		for (ServerPlayer old : List.copyOf(bar.getPlayers())) {
			if (!challengers.contains(old)) bar.removePlayer(old);
		}
		for (ServerPlayer p : challengers) bar.addPlayer(p);
		bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));

		if (challengers.isEmpty()) {
			clearVentLights(server);
			entityData.set(MOVE, IDLE);
			entityData.set(VENT_MASK, 0);
			entityData.set(SOAK_COUNT, 0);
			if (++quietTicks >= QUIET_RESET_TICKS) abortTrial(server);
			return;
		}
		quietTicks = 0;

		long now = server.getGameTime();
		if (staggered()) {
			getNavigation().stop();
			bar.setName(Component.translatable("boss.wildercord.kharvok.status",
				bossTitle(), Component.translatable("boss.wildercord.kharvok.staggered")));
			return;
		}

		if (isPhaseTwo(getHealth(), getMaxHealth()) && !phaseTwo()) {
			entityData.set(PHASE_TWO, true);
			bar.setColor(BossEvent.BossBarColor.RED);
			server.playSound(null, blockPosition(), SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 1.3F, 0.55F);
		}

		int currentMove = move();
		if (currentMove == ANVIL_SHATTER || currentMove == CHAIN_SHACKLE || currentMove == CRUCIBLE_MELTDOWN) {
			tickAttack(server, challengers, now, currentMove);
			return;
		}

		ServerPlayer target = challengers.stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElseThrow();
		facePosition(target.position());

		double distSq = distanceToSqr(target);
		if (now >= readyAtTick && (distSq <= (SHATTER_REACH * SHATTER_REACH) || completedAttacks % 2 == 1)) {
			beginAttack(server, challengers, target, now);
			return;
		}

		if (distSq > 9.0) {
			if (currentMove != STRIDE) {
				entityData.set(MOVE, STRIDE);
				entityData.set(BEGAN, now);
				updateBarSubtitle(STRIDE);
			}
			Vec3 dir = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
			double len = dir.length();
			if (len > 0.05) {
				Vec3 step = dir.scale(0.17 / len);
				setDeltaMovement(step.x, getDeltaMovement().y, step.z);
			}
		} else {
			if (currentMove != IDLE) {
				entityData.set(MOVE, IDLE);
				entityData.set(BEGAN, now);
				updateBarSubtitle(IDLE);
			}
			setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
		}
	}

	private void beginAttack(ServerLevel server, List<ServerPlayer> challengers, ServerPlayer target, long now) {
		int move = nextAttack(completedAttacks, phaseTwo());
		moveImpacted = false;
		setDeltaMovement(Vec3.ZERO);
		facePosition(target.position());
		entityData.set(AIM_YAW, getYRot());
		entityData.set(BEGAN, now);
		entityData.set(MOVE, move);
		updateBarSubtitle(move);

		if (move == ANVIL_SHATTER) {
			server.playSound(null, blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.2F, 0.6F);
		} else if (move == CHAIN_SHACKLE) {
			int count = ventCount(Math.max(partySize, challengers.size()), apex());
			int mask = 0;
			int start = completedAttacks % 4;
			for (int i = 0; i < count; i++) {
				int idx = (start + i) % 4;
				mask |= (1 << idx);
				setVentLit(server, idx, true);
			}
			entityData.set(VENT_MASK, mask);
			server.playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.6F, 0.6F);
		} else if (move == CRUCIBLE_MELTDOWN) {
			entityData.set(SOAK_COUNT, soakRingCount(Math.max(partySize, challengers.size()), apex()));
			entityData.set(SOAK_FILLED, 0);
			server.playSound(null, blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 1.6F, 0.55F);
		}
	}

	private void tickAttack(ServerLevel server, List<ServerPlayer> challengers, long now, int move) {
		long el = now - entityData.get(BEGAN);
		boolean apexMode = apex();
		if (move == ANVIL_SHATTER) {
			if (!moveImpacted && el >= SHATTER_TELL_TICKS + 1) {
				moveImpacted = true;
				float yaw = aimYaw();
				float dmg = shatterDamage(phaseTwo(), apexMode);
				for (ServerPlayer p : challengers) {
					Vec3 d = p.position().subtract(position());
					if (VaelkhorRules.inCleaveArc(d.x, d.z, yaw, SHATTER_REACH, SHATTER_HALF_ANGLE_RAD)) {
						p.hurtServer(server, damageSources().mobAttack(this), dmg);
						p.push(0.0, 0.35, 0.0);
						p.needsSync = true;
					}
				}
				server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.5, getZ(), 24, 2.2, 0.3, 2.2, 0.06);
				server.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.55F);
			}
			if (el >= SHATTER_DURATION_TICKS) {
				finishMove(now, VaelkhorRules.scaleCooldown(15L, apexMode));
			}
		} else if (move == CHAIN_SHACKLE) {
			if (ventMask() == 0) {
				finishMove(now, VaelkhorRules.scaleCooldown(12L, apexMode));
				return;
			}
			if (el >= SHACKLE_WINDOW_TICKS) {
				int remaining = Integer.bitCount(ventMask());
				float dmg = VaelkhorRules.scaleDamage(SHACKLE_PULSE_DAMAGE * Math.max(1, remaining), apexMode);
				for (ServerPlayer p : challengers) {
					p.hurtServer(server, damageSources().onFire(), dmg);
					p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0, false, true));
				}
				clearVentLights(server);
				entityData.set(VENT_MASK, 0);
				server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.7F);
				finishMove(now, VaelkhorRules.scaleCooldown(16L, apexMode));
			}
		} else if (move == CRUCIBLE_MELTDOWN) {
			int count = soakCount();
			BlockPos center = effectiveCenter();
			int filled = 0;
			for (int i = 0; i < count; i++) {
				double rx = center.getX() + 0.5 + MELTDOWN_SOAK_OFFSETS[i][0];
				double rz = center.getZ() + 0.5 + MELTDOWN_SOAK_OFFSETS[i][1];
				for (ServerPlayer p : challengers) {
					if (VaelkhorRules.inRing(p.getX() - rx, p.getZ() - rz, SOAK_RADIUS)) {
						filled |= (1 << i);
						contributors.add(p.getUUID());
					}
				}
			}
			entityData.set(SOAK_FILLED, filled);

			if (!moveImpacted && el >= MELTDOWN_TELL_TICKS) {
				moveImpacted = true;
				int unsoaked = count - Integer.bitCount(filled);
				for (ServerPlayer p : challengers) {
					boolean safe = false;
					for (int i = 0; i < count; i++) {
						double rx = center.getX() + 0.5 + MELTDOWN_SOAK_OFFSETS[i][0];
						double rz = center.getZ() + 0.5 + MELTDOWN_SOAK_OFFSETS[i][1];
						if ((filled & (1 << i)) != 0 && VaelkhorRules.inRing(p.getX() - rx, p.getZ() - rz, SOAK_RADIUS)) {
							safe = true;
							break;
						}
					}
					float dmg = (safe && unsoaked == 0) ? MELTDOWN_SOAKED_DAMAGE : (safe ? MELTDOWN_SOAKED_DAMAGE + 4.0F * unsoaked : MELTDOWN_UNSOAKED_DAMAGE);
					p.hurtServer(server, damageSources().onFire(), VaelkhorRules.scaleDamage(dmg, apexMode));
					if (safe) {
						p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, false, true));
					}
				}
				server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.8, getZ(), 40, 4.5, 1.0, 4.5, 0.08);
				server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.65F);
			}
			if (el >= MELTDOWN_TELL_TICKS + MELTDOWN_RECOVERY_TICKS) {
				entityData.set(SOAK_COUNT, 0);
				entityData.set(SOAK_FILLED, 0);
				finishMove(now, VaelkhorRules.scaleCooldown(18L, apexMode));
			}
		}
	}

	private void finishMove(long now, long cooldown) {
		completedAttacks++;
		entityData.set(MOVE, IDLE);
		entityData.set(BEGAN, now);
		readyAtTick = now + cooldown;
		updateBarSubtitle(IDLE);
	}

	public boolean tryQuenchVent(BlockPos pos, ServerPlayer player) {
		if (!(level() instanceof ServerLevel server) || move() != CHAIN_SHACKLE) return false;
		int mask = ventMask();
		if (mask == 0) return false;
		BlockPos center = effectiveCenter();
		for (int idx = 0; idx < 4; idx++) {
			if ((mask & (1 << idx)) == 0) continue;
			if (ventPos(center, idx).equals(pos)) {
				int nextMask = mask & ~(1 << idx);
				entityData.set(VENT_MASK, nextMask);
				setVentLit(server, idx, false);
				contributors.add(player.getUUID());
				server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.05);
				server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 1.1F);
				if (nextMask == 0) {
					long now = server.getGameTime();
					entityData.set(STAGGER_UNTIL, now + STAGGER_TICKS);
					entityData.set(MOVE, IDLE);
					entityData.set(BEGAN, now);
					completedAttacks++;
					readyAtTick = now + STAGGER_TICKS;
					server.playSound(null, blockPosition(), SoundEvents.SHIELD_BREAK.value(), SoundSource.HOSTILE, 1.6F, 0.65F);
					broadcastArena(server, Component.translatable("message.wildercord.trial.vent_broken").withStyle(ChatFormatting.GREEN));
				} else {
					broadcastArena(server, Component.translatable("message.wildercord.trial.vent_quenched", Integer.bitCount(nextMask))
						.withStyle(ChatFormatting.GOLD));
				}
				return true;
			}
		}
		return false;
	}

	private static BlockPos ventPos(BlockPos center, int idx) {
		return center.offset(VENT_OFFSETS[idx][0], 1, VENT_OFFSETS[idx][1]);
	}

	private void setVentLit(ServerLevel server, int idx, boolean lit) {
		if (arenaCenter == null) return;
		BlockPos p = ventPos(arenaCenter, idx);
		if (server.hasChunkAt(p)) {
			BlockState state = server.getBlockState(p);
			if (state.is(CoopTrialContent.CRUCIBLE_VENT) && state.getValue(CrucibleVentBlock.LIT) != lit) {
				server.setBlock(p, state.setValue(CrucibleVentBlock.LIT, lit), 3);
			}
		}
	}

	private void clearVentLights(ServerLevel server) {
		for (int i = 0; i < 4; i++) setVentLit(server, i, false);
	}

	private void keepInsideArena() {
		Vec3 c = Vec3.atBottomCenterOf(effectiveCenter());
		Vec3 d = position().subtract(c);
		if (Math.hypot(d.x, d.z) > ARENA_RADIUS - 2.0 || d.y < ARENA_Y_MIN || d.y > ARENA_Y_MAX) {
			snapTo(c.x, c.y, c.z, getYRot(), 0.0F);
			setDeltaMovement(Vec3.ZERO);
		}
	}

	private void facePosition(Vec3 target) {
		double dx = target.x - getX();
		double dz = target.z - getZ();
		if (dx * dx + dz * dz < 1.0e-4) return;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		setYRot(yaw);
		setYHeadRot(yaw);
		yBodyRot = yaw;
	}

	private Component bossTitle() {
		if (apex()) {
			return Component.literal("[APEX] ").withStyle(ChatFormatting.GOLD)
				.append(getType().getDescription().copy().withStyle(ChatFormatting.WHITE));
		}
		return getType().getDescription();
	}

	private void updateBarSubtitle(int move) {
		bar.setName(Component.translatable("boss.wildercord.kharvok.status",
			bossTitle(), Component.translatable("boss.wildercord.kharvok." + moveKey(move))));
	}

	private void broadcastArena(ServerLevel server, Component msg) {
		for (ServerPlayer p : server.players()) {
			if (eligible(p)) p.sendOverlayMessage(msg);
		}
	}

	private void abortTrial(ServerLevel server) {
		clearVentLights(server);
		bar.removeAllPlayers();
		if (gatePos != null && server.getBlockEntity(gatePos) instanceof TrialGateBlockEntity gate) {
			gate.resetActiveTrial(getUUID());
		}
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
		if (!(source.getEntity() instanceof ServerPlayer player) || !eligible(player) || !(amount > 0.0F)) {
			return false;
		}
		float effective = staggerDamage(amount, staggered());
		boolean had = contributors.contains(player.getUUID());
		if (contributors.size() < 64) contributors.add(player.getUUID());
		boolean accepted = super.hurtServer(server, source, effective);
		if (!accepted && !had) contributors.remove(player.getUUID());
		return accepted;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		bar.removeAllPlayers();
		if (level() instanceof ServerLevel server) {
			clearVentLights(server);
			Set<UUID> rewardIds = new HashSet<>(contributors);
			if (source.getEntity() instanceof ServerPlayer killer) rewardIds.add(killer.getUUID());
			for (ServerPlayer p : server.players()) {
				if (eligible(p)) rewardIds.add(p.getUUID());
			}
			if (gatePos != null && server.getBlockEntity(gatePos) instanceof TrialGateBlockEntity gate) {
				gate.complete(getUUID());
			}
			if (treasuryPos != null && server.getBlockEntity(treasuryPos) instanceof TrialTreasuryBlockEntity treasury) {
				treasury.unlockFor(rewardIds, gatePos, "crucible", apex());
			}
			String key = apex() ? "message.wildercord.trial.victory_apex" : "message.wildercord.trial.victory";
			for (ServerPlayer p : server.players()) {
				if (rewardIds.contains(p.getUUID())) {
					p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD));
				}
			}
		}
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (arenaCenter != null) output.store("arena_center", BlockPos.CODEC, arenaCenter);
		if (gatePos != null) output.store("gate_pos", BlockPos.CODEC, gatePos);
		if (treasuryPos != null) output.store("treasury_pos", BlockPos.CODEC, treasuryPos);
		output.putInt("party_size", partySize);
		output.putInt("completed_attacks", completedAttacks);
		output.putBoolean("apex", apex());
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		arenaCenter = input.read("arena_center", BlockPos.CODEC).orElse(null);
		gatePos = input.read("gate_pos", BlockPos.CODEC).orElse(null);
		treasuryPos = input.read("treasury_pos", BlockPos.CODEC).orElse(null);
		partySize = Math.max(1, input.getIntOr("party_size", 1));
		completedAttacks = input.getIntOr("completed_attacks", 0);
		entityData.set(APEX, input.getBooleanOr("apex", false));
	}
}
