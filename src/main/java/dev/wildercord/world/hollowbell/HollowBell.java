package dev.wildercord.world.hollowbell;

import dev.wildercord.lore.LoreJournal;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

import static dev.wildercord.world.hollowbell.HollowBellRules.*;

/** An opt-in, plinth-anchored Belowkeeper resonance guardian. Every impact is one server event following a fixed tell. */
public final class HollowBell extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(HollowBell.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(HollowBell.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Float> AIM = SynchedEntityData.defineId(HollowBell.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MARK_X = SynchedEntityData.defineId(HollowBell.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MARK_Z = SynchedEntityData.defineId(HollowBell.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> ACROSS = SynchedEntityData.defineId(HollowBell.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SECOND = SynchedEntityData.defineId(HollowBell.class, EntityDataSerializers.BOOLEAN);
	private final ServerBossEvent bar = new ServerBossEvent(getUUID(), getType().getDescription(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
	private BlockPos plinth, belfry;
	private UUID lockedTarget;
	private final Set<UUID> contributors = new LinkedHashSet<>(), announced = new HashSet<>();
	private boolean active, impacted;
	private int completed, quiet;
	private long readyAt;

	public HollowBell(EntityType<? extends HollowBell> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		setNoGravity(true);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ARMOR, 4).add(Attributes.ARMOR_TOUGHNESS, 2)
			.add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.KNOCKBACK_RESISTANCE, 1);
	}

	@Override protected void registerGoals() {}
	@Override public boolean isPushable() { return false; }
	@Override protected void doPush(Entity other) {}
	@Override public boolean removeWhenFarAway(double distance) { return false; }

	@Override protected void defineSynchedData(SynchedEntityData.Builder b) {
		super.defineSynchedData(b);
		b.define(MOVE, IDLE); b.define(BEGAN, 0L); b.define(AIM, 0F);
		b.define(MARK_X, 0F); b.define(MARK_Z, 0F); b.define(ACROSS, false); b.define(SECOND, false);
	}

	public int move() { return entityData.get(MOVE); }
	public int activeMove() { return move(); }
	public long moveBeganAt() { return entityData.get(BEGAN); }
	public float elapsed(float partial) { return level().getGameTime() - entityData.get(BEGAN) + partial; }
	public float aim() { return entityData.get(AIM); }
	public float aimRadians() { return aim(); }
	public float markX() { return entityData.get(MARK_X); }
	public float markZ() { return entityData.get(MARK_Z); }
	public boolean acrossX() { return entityData.get(ACROSS); }
	public boolean secondPhase() { return entityData.get(SECOND); }
	public boolean isSecondPhase() { return secondPhase(); }
	public boolean active() { return active; }
	public BlockPos plinth() { return plinth; }
	public BlockPos belfryPos() { return belfry; }
	public boolean belongsTo(BlockPos pos) { return pos.equals(belfry); }

	public void bind(BlockPos at, BlockPos source) {
		plinth = at.immutable();
		belfry = source.immutable();
		hold();
	}

	private void hold() {
		if (plinth == null) return;
		var at = Vec3.atBottomCenterOf(plinth.above());
		setPos(at);
		setDeltaMovement(Vec3.ZERO);
		getNavigation().stop();
	}

	private boolean eligible(ServerPlayer player) {
		if (plinth == null || player.level() != level() || !player.isAlive() || player.isCreative() || player.isSpectator()) return false;
		var d = player.position().subtract(Vec3.atBottomCenterOf(plinth.above()));
		return within(d.x, d.y, d.z);
	}

	public boolean challenge(ServerPlayer player) {
		if (active || !isAlive() || !eligible(player)) return false;
		active = true;
		quiet = completed = 0;
		contributors.clear();
		retire();
		entityData.set(SECOND, false);
		bar.setColor(BossEvent.BossBarColor.BLUE);
		readyAt = level().getGameTime() + START;
		setHealth(getMaxHealth());
		return true;
	}

	private void retire() {
		entityData.set(MOVE, IDLE);
		announced.clear();
		lockedTarget = null;
	}

	private void rest() {
		retire();
		active = false;
		quiet = completed = 0;
		contributors.clear();
		entityData.set(SECOND, false);
		bar.removeAllPlayers();
		setHealth(getMaxHealth());
	}

	@Override protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (!isAlive()) return;
		hold();
		if (belfry != null && server.getBlockEntity(belfry) instanceof ListeningBelfryEntity entity && entity.slain()) {
			discard();
			return;
		}
		if (!active) return;
		if (server.getDifficulty() == Difficulty.PEACEFUL || !dev.wildercord.aura.world.Guardians.enabled()) {
			rest();
			return;
		}
		var players = server.players().stream().filter(this::eligible).limit(MAX_CHALLENGERS).toList();
		for (var old : List.copyOf(bar.getPlayers())) if (!players.contains(old)) bar.removePlayer(old);
		for (var player : players) bar.addPlayer(player);
		bar.setProgress(getHealth() / getMaxHealth());
		if (players.isEmpty()) {
			retire();
			if (++quiet >= QUIET_RESET) rest();
			return;
		}
		quiet = 0;
		long now = server.getGameTime();
		if (move() != IDLE) {
			if (impacted) {
				if (elapsed(0) >= tell(move()) + recovery(move())) { retire(); readyAt = now; }
				return;
			}
			var target = server.getPlayerByUUID(lockedTarget);
			if (!(target instanceof ServerPlayer player) || !eligible(player)) { retire(); readyAt = now + RETIRE; return; }
			if (elapsed(0) >= tell(move())) { impact(server, players); return; }
		} else if (now >= readyAt) begin(server, players);
	}

	private void begin(ServerLevel server, List<ServerPlayer> players) {
		impacted = false;
		var target = players.stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElseThrow();
		boolean second = second(getHealth(), getMaxHealth());
		if (second && !secondPhase()) {
			completed = 0;
			entityData.set(SECOND, true);
			bar.setColor(BossEvent.BossBarColor.WHITE);
		}
		int move = nextMove(completed, secondPhase());
		var d = target.position().subtract(position());
		entityData.set(AIM, (float) Math.atan2(d.x, d.z));
		entityData.set(MARK_X, (float) Math.clamp(d.x, -ARENA + CLAPPER_RADIUS, ARENA - CLAPPER_RADIUS));
		entityData.set(MARK_Z, (float) Math.clamp(d.z, -ARENA + CLAPPER_RADIUS, ARENA - CLAPPER_RADIUS));
		entityData.set(ACROSS, secondPhase() && (completed / 3) % 2 == 0);
		entityData.set(BEGAN, server.getGameTime());
		entityData.set(MOVE, move);
		setYRot((float) -Math.toDegrees(aim()));
		setYHeadRot(getYRot());
		yBodyRot = getYRot();
		lockedTarget = target.getUUID();
		announced.clear();
		for (var player : players) announced.add(player.getUUID());
		bar.setName(Component.translatable("boss.wildercord.hollow_bell.status", getType().getDescription(), Component.translatable("boss.wildercord.hollow_bell." + key(move))));
		server.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.2F, move == CHORD ? .6F : .9F);
	}

	private void impact(ServerLevel server, List<ServerPlayer> players) {
		int move = move();
		for (var player : players) {
			var d = player.position().subtract(position());
			if (!announced.contains(player.getUUID()) || !hits(move, d.x, d.y, d.z, markX(), markZ(), acrossX())) continue;
			if (player.hurtServer(server, damageSources().mobAttack(this), damage(move)) && move == CLAPPER) {
				var pull = position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(.35);
				player.push(pull.x, .08, pull.z);
				player.needsSync = true;
			}
		}
		server.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5F, .65F);
		completed++;
		impacted = true;
		announced.clear();
		bar.setName(getType().getDescription());
	}

	@Override public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
		if (!active || !(source.getEntity() instanceof ServerPlayer player) || !eligible(player) || !(amount > 0)) return false;
		boolean had = contributors.contains(player.getUUID());
		if (contributors.size() < MAX_CHALLENGERS) contributors.add(player.getUUID());
		boolean accepted = super.hurtServer(server, source, amount);
		if (!accepted && !had) contributors.remove(player.getUUID());
		return accepted;
	}

	@Override public void die(DamageSource source) {
		retire();
		bar.removeAllPlayers();
		if (level() instanceof ServerLevel server && belfry != null && server.getBlockEntity(belfry) instanceof ListeningBelfryEntity entity && entity.complete(getUUID())) {
			var methods = dev.wildercord.aura.BreathingMethods.BUILT_IN;
			for (var player : server.players()) if (contributors.contains(player.getUUID()) && eligible(player)) {
				var compass = HollowBellContent.compassTo(server, belfry);
				giveOrDrop(player, compass);
				giveOrDrop(player, new ItemStack(dev.wildercord.aura.world.AuraWorld.AURA_SHARD, 2));
				giveOrDrop(player, new ItemStack(dev.wildercord.content.WildercordItems.MANA_CRYSTAL, 2));
				giveOrDrop(player, new ItemStack(dev.wildercord.content.WildercordItems.TORN_PAGE, 1));
				giveOrDrop(player, new ItemStack(dev.wildercord.content.Reagents.GEODE_GRIT, 3));
				if (!methods.isEmpty()) {
					var method = methods.get(server.getRandom().nextInt(methods.size()));
					giveOrDrop(player, dev.wildercord.aura.world.ManualPageItem.of(dev.wildercord.aura.world.AuraWorld.MANUAL_PAGE, method.id()));
				}
				player.giveExperiencePoints(180);
				LoreJournal.record(player, "learned:hollow_bell", true);
			}
		}
		super.die(source);
	}
	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
	}

	@Override public void remove(RemovalReason reason) { bar.removeAllPlayers(); super.remove(reason); }
	@Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bar.removePlayer(player); }

	@Override protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		if (plinth != null) out.store("plinth", BlockPos.CODEC, plinth);
		if (belfry != null) out.store("listening_belfry", BlockPos.CODEC, belfry);
	}

	@Override protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		plinth = in.read("plinth", BlockPos.CODEC).orElse(null);
		belfry = in.read("listening_belfry", BlockPos.CODEC).orElse(null);
		if (getHealth() > 0) rest(); else { retire(); active = false; }
		hold();
	}
}
