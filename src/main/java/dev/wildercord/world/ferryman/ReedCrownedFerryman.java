package dev.wildercord.world.ferryman;

import dev.wildercord.lore.LoreJournal;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildercord.world.ferryman.FerrymanRules.*;

/** A deliberately challenged, moored guardian. Every impact is one server event following a fixed tell. */
public final class ReedCrownedFerryman extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(ReedCrownedFerryman.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(ReedCrownedFerryman.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Float> AIM = SynchedEntityData.defineId(ReedCrownedFerryman.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MARK_X = SynchedEntityData.defineId(ReedCrownedFerryman.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MARK_Z = SynchedEntityData.defineId(ReedCrownedFerryman.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> ACROSS = SynchedEntityData.defineId(ReedCrownedFerryman.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SECOND = SynchedEntityData.defineId(ReedCrownedFerryman.class, EntityDataSerializers.BOOLEAN);
	private final ServerBossEvent bar = new ServerBossEvent(getUUID(), getType().getDescription(), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
	private BlockPos mooring, bell;
	private UUID lockedTarget;
	private final Set<UUID> contributors = new LinkedHashSet<>(), announced = new HashSet<>();
	private boolean active, impacted;
	private int completed, quiet;
	private long readyAt;

	public ReedCrownedFerryman(EntityType<? extends ReedCrownedFerryman> type, Level level) {
		super(type, level); setPersistenceRequired(); setNoGravity(true); xpReward = 0;
	}
	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ARMOR, 4).add(Attributes.ARMOR_TOUGHNESS, 2)
			.add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}
	@Override protected void registerGoals() {}
	@Override public boolean canBreatheUnderwater() { return true; }
	@Override public boolean isPushable() { return false; }
	@Override protected void doPush(Entity other) {}
	@Override public boolean removeWhenFarAway(double distance) { return false; }
	@Override protected void defineSynchedData(SynchedEntityData.Builder b) {
		super.defineSynchedData(b); b.define(MOVE, IDLE); b.define(BEGAN, 0L); b.define(AIM, 0F);
		b.define(MARK_X, 0F); b.define(MARK_Z, 0F); b.define(ACROSS, false); b.define(SECOND, false);
	}
	public int move() { return entityData.get(MOVE); }
	public float elapsed(float partial) { return level().getGameTime() - entityData.get(BEGAN) + partial; }
	public float aim() { return entityData.get(AIM); }
	public float markX() { return entityData.get(MARK_X); }
	public float markZ() { return entityData.get(MARK_Z); }
	public boolean acrossX() { return entityData.get(ACROSS); }
	public boolean secondPhase() { return entityData.get(SECOND); }
	public boolean active() { return active; }
	public BlockPos mooring() { return mooring; }
	public boolean belongsTo(BlockPos pos) { return pos.equals(bell); }
	public void bind(BlockPos at, BlockPos source) { mooring = at.immutable(); bell = source.immutable(); hold(); }
	private void hold() {
		if (mooring == null) return;
		var at = Vec3.atBottomCenterOf(mooring); setPos(at); setDeltaMovement(Vec3.ZERO); getNavigation().stop();
	}
	private boolean eligible(ServerPlayer player) {
		if (mooring == null || player.level() != level() || !player.isAlive() || player.isCreative() || player.isSpectator()) return false;
		var d = player.position().subtract(Vec3.atBottomCenterOf(mooring)); return within(d.x, d.y, d.z);
	}
	public boolean challenge(ServerPlayer player) {
		if (active || !isAlive() || !eligible(player)) return false;
		active = true; quiet = completed = 0; contributors.clear(); retire(); entityData.set(SECOND, false);
		bar.setColor(BossEvent.BossBarColor.GREEN); readyAt = level().getGameTime() + START; setHealth(getMaxHealth()); return true;
	}
	private void retire() { entityData.set(MOVE, IDLE); announced.clear(); lockedTarget = null; }
	private void rest() {
		retire(); active = false; quiet = completed = 0; contributors.clear(); entityData.set(SECOND, false);
		bar.removeAllPlayers(); setHealth(getMaxHealth());
	}
	@Override protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server); if (!isAlive()) return; hold();
		if (bell != null && server.getBlockEntity(bell) instanceof CrossingBellEntity crossing && crossing.slain()) { discard(); return; }
		if (!active) return;
		if (server.getDifficulty() == Difficulty.PEACEFUL || !dev.wildercord.aura.world.Guardians.enabled()) { rest(); return; }
		var players = server.players().stream().filter(this::eligible).limit(MAX_CHALLENGERS).toList();
		for (var old : List.copyOf(bar.getPlayers())) if (!players.contains(old)) bar.removePlayer(old);
		for (var player : players) bar.addPlayer(player);
		bar.setProgress(getHealth() / getMaxHealth());
		if (players.isEmpty()) {
			retire(); if (++quiet >= QUIET_RESET) rest(); return;
		}
		quiet = 0;
		long now = server.getGameTime();
		if (move() != IDLE) {
			if (impacted) {
				if (elapsed(0) >= tell(move()) + recovery(move())) { retire(); readyAt = now; }
				return;
			}
			// Losing the announced target cancels the entire impact, even if another challenger remains.
			var target = server.getPlayerByUUID(lockedTarget);
			if (!(target instanceof ServerPlayer player) || !eligible(player)) { retire(); readyAt = now + RETIRE; return; }
			if (elapsed(0) >= tell(move())) { impact(server, players); return; }
		} else if (now >= readyAt) begin(server, players);
	}
	private void begin(ServerLevel server, List<ServerPlayer> players) {
		impacted = false;
		var target = players.stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElseThrow();
		boolean second = second(getHealth(), getMaxHealth());
		if (second && !secondPhase()) { completed = 0; entityData.set(SECOND, true); bar.setColor(BossEvent.BossBarColor.YELLOW); }
		int move = nextMove(completed, secondPhase());
		var d = target.position().subtract(position());
		entityData.set(AIM, (float)Math.atan2(d.x, d.z));
		entityData.set(MARK_X, (float)Math.clamp(d.x, -ARENA + ANCHOR_RADIUS, ARENA - ANCHOR_RADIUS));
		entityData.set(MARK_Z, (float)Math.clamp(d.z, -ARENA + ANCHOR_RADIUS, ARENA - ANCHOR_RADIUS));
		entityData.set(ACROSS, secondPhase() && (completed / 3) % 2 == 0);
		entityData.set(BEGAN, server.getGameTime()); entityData.set(MOVE, move);
		setYRot((float)-Math.toDegrees(aim())); setYHeadRot(getYRot()); yBodyRot = getYRot();
		lockedTarget = target.getUUID(); announced.clear(); for (var player : players) announced.add(player.getUUID());
		bar.setName(Component.translatable("boss.wildercord.ferryman.status", getType().getDescription(), Component.translatable("boss.wildercord.ferryman." + key(move))));
		server.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1, move == WAKE ? .55F : .9F);
	}
	private void impact(ServerLevel server, List<ServerPlayer> players) {
		int move = move();
		for (var player : players) {
			var d = player.position().subtract(position());
			if (!announced.contains(player.getUUID()) || !hits(move, d.x, d.y, d.z, aim(), markX(), markZ(), acrossX())) continue;
			if (player.hurtServer(server, damageSources().mobAttack(this), damage(move)) && move == ANCHOR) {
				var pull = position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(.35);
				player.push(pull.x, .08, pull.z); player.needsSync = true;
			}
		}
		server.playSound(null, blockPosition(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.HOSTILE, 1.5F, .65F);
		completed++; impacted = true; announced.clear();
		bar.setName(getType().getDescription());
	}
	@Override public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
		if (!active || !(source.getEntity() instanceof ServerPlayer player) || !eligible(player) || !(amount > 0)) return false;
		// Record before a lethal hit invokes die; failed damage is removed afterwards unless already earned.
		boolean had = contributors.contains(player.getUUID());
		if (contributors.size() < MAX_CHALLENGERS) contributors.add(player.getUUID());
		boolean accepted = super.hurtServer(server, source, amount);
		if (!accepted && !had) contributors.remove(player.getUUID());
		return accepted;
	}
	@Override public void die(DamageSource source) {
		retire(); bar.removeAllPlayers();
		if (level() instanceof ServerLevel server && bell != null && server.getBlockEntity(bell) instanceof CrossingBellEntity crossing && crossing.complete(getUUID())) {
			for (var player : server.players()) if (contributors.contains(player.getUUID()) && eligible(player)) {
				var compass = new ItemStack(Items.COMPASS);
				compass.set(DataComponents.CUSTOM_NAME, Component.translatable("item.wildercord.crossing_compass"));
				compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(server.dimension(), bell)), false));
				if (!player.getInventory().add(compass)) player.drop(compass, false, net.minecraft.util.Prediction.SERVER_ONLY);
				player.giveExperiencePoints(80); LoreJournal.record(player, "learned:last_crossing", true);
			}
		}
		super.die(source);
	}
	@Override public void remove(RemovalReason reason) { bar.removeAllPlayers(); super.remove(reason); }
	@Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bar.removePlayer(player); }
	@Override protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		if (mooring != null) out.store("mooring", BlockPos.CODEC, mooring);
		if (bell != null) out.store("crossing_bell", BlockPos.CODEC, bell);
	}
	@Override protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in); mooring = in.read("mooring", BlockPos.CODEC).orElse(null); bell = in.read("crossing_bell", BlockPos.CODEC).orElse(null);
		// A reload requires another deliberate ring. A saved mid-tell attack cannot become an invisible impact.
		if (getHealth()>0) rest(); else { retire(); active=false; }
		hold();
	}
}
