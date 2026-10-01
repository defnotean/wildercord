package dev.wildercord.monster;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Targets;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Mana Ooze: a slime of clear, faintly pink jelly from the deep underground and the ley lines, with a mote of raw mana
 * turning at its heart. It eats spells. A spell's harm sinks into it and does nothing but fill it (its heart burning
 * brighter, the jelly swelling); full, it grows a size, healed; full at its biggest, it bursts into two. Touching a
 * caster, it drinks a little of their mana.
 *
 * <p>So meet it with a blade, an arrow or fire: fire's spells and flames boil it (it's weak to fire) and are never drunk.
 * Unlike a plain slime it doesn't split when it dies, only when it's overfed.</p>
 */
public class ManaOoze extends AbstractCubeMob implements Enemy, RuneboundKin {
	private static final EntityDataAccessor<Float> DATA_FULLNESS = SynchedEntityData.defineId(ManaOoze.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Long> DATA_DRANK = SynchedEntityData.defineId(ManaOoze.class, EntityDataSerializers.LONG);
	static final float TOUCH_DRAIN = 4.0F;
	static final float BURST_DAMAGE = 3.0F;
	private static final int PINK = 0xE678DC;
	private static final int VIOLET = 0x9A7CFF;

	private final Map<UUID, Long> told = new HashMap<>();

	public ManaOoze(EntityType<? extends ManaOoze> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.2F).add(Attributes.MAX_HEALTH, 14.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FULLNESS, 0.0F);
		builder.define(DATA_DRANK, 0L);
	}

	@Override
	protected void addBehaviourGoals() {
		goalSelector.addGoal(2, new AbstractCubeMob.CubeMobAttackGoal(this));
	}

	@Override
	protected void addTargetingGoals() {
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> Math.abs(target.getY() - getY()) <= 4.0));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.ORB, Runes.HARM), List.of(Runes.BOLT, Runes.SILENCE), List.of(Runes.BOLT, Runes.HARM));
	}

	// ------------------------------------------------------------------ size and fullness

	@Override
	public void setSize(int size, boolean updateHealth) {
		super.setSize(Math.min(MonsterRules.MAX_OOZE, size), updateHealth);
		int actual = getSize();
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(actual + 1);
		this.xpReward = actual + 2;
	}

	@Override
	protected void setCubeMobHealth(int size) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(MonsterRules.oozeHealth(size));
	}

	/** How much spell it has drunk toward its next size, 0 to its capacity. */
	public float fullness() {
		return entityData.get(DATA_FULLNESS);
	}

	/** How full it is, 0 (empty) to 1 (about to grow, or burst). */
	public float fill() {
		return Mth.clamp(fullness() / MonsterRules.oozeCapacity(getSize()), 0, 1);
	}

	/** When it last drank a spell (game time): its heart flares for a moment after. */
	public long drankAt() {
		return entityData.get(DATA_DRANK);
	}

	@Override
	protected void setSpawnSize(ServerLevelAccessor level, DifficultyInstance difficulty) {
		// Mostly small, sometimes middling, now and then big.
		float roll = level.getRandom().nextFloat();
		setSize(roll < 0.35F ? 1 : roll < 0.85F ? 2 : 3, true);
	}

	@Override
	protected boolean canBeABaby() {
		return false;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		return super.finalizeSpawn(level, difficulty, reason, groupData == null ? new AgeableMob.AgeableMobGroupData(false) : groupData);
	}

	@Override
	protected int getSplitCount() {
		// Death doesn't split it: only overfeeding does.
		return 0;
	}

	// ------------------------------------------------------------------ eating spells

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FIRE) && !MonsterMagic.spellLanding()) {
			// Flames boil it.
			damage *= 1.5F;
		}
		if (MonsterMagic.spellLanding() && !MonsterMagic.spellElement().equals("fire") && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			drink(level, damage, source.getEntity());
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	/** A spell's harm sinks into it: it fills, and grows or bursts when full. */
	public void drink(ServerLevel level, float amount, @Nullable Entity caster) {
		if (amount <= 0 || !isAlive()) {
			return;
		}
		MonsterRules.Feed feed = MonsterRules.feed(getSize(), fullness(), amount);
		entityData.set(DATA_DRANK, level.getGameTime());
		Vec3 c = getBoundingBox().getCenter();
		Motes.seek(level, c.add(getRandom().nextGaussian() * 0.6, 0.6, getRandom().nextGaussian() * 0.6), c, PINK, 0.16, 8, 0.5);
		ElementFx.ring(level, c, new Vec3(0, 1, 0), VIOLET, getBbWidth() * 0.7, 0.1, 0.06, 8);
		MonsterMagic.sound(level, c, "monster_ooze_absorb", 1.0F, 1.2F - 0.15F * getSize());
		if (caster instanceof ServerPlayer player) {
			Long last = told.get(player.getUUID());
			if (last == null || level.getGameTime() - last > 200) {
				told.put(player.getUUID(), level.getGameTime());
				MonsterMagic.tell(player, Component.translatable("message.wildercord.mana_ooze_drinks").withColor(0xFFD8FA));
			}
		}
		if (feed.split()) {
			split(level);
			return;
		}
		if (feed.size() > getSize()) {
			setSize(feed.size(), true);
			growFx(level, c);
			MonsterMagic.sound(level, c, "monster_ooze_grow", 1.0F, 1.0F);
		}
		entityData.set(DATA_FULLNESS, feed.fullness());
	}

	/** Grown a size: a flash of pink light and a shimmer through the jelly. */
	private void growFx(ServerLevel level, Vec3 c) {
		dev.wildercord.cast.Sigils.flash(level, c, PINK, getBbWidth() * 1.6F);
		ElementFx.shimmer(level, c, getBbWidth() * 0.5, 8);
	}

	/** Too full to hold: it bursts into two halves, throwing raw mana at everyone close by. */
	private void split(ServerLevel level) {
		Vec3 c = getBoundingBox().getCenter();
		int half = MonsterRules.MAX_OOZE / 2;
		for (int i = 0; i < 2; i++) {
			ManaOoze child = MonsterContent.MANA_OOZE.create(level, EntitySpawnReason.TRIGGERED);
			if (child == null) {
				continue;
			}
			child.setSize(half, true);
			double side = i == 0 ? -0.6 : 0.6;
			child.snapTo(getX() + side, getY() + 0.5, getZ() + side * 0.5, getRandom().nextFloat() * 360, 0);
			child.setDeltaMovement(side * 0.3, 0.35, side * 0.15);
			child.addTag("wildercord.rolled");
			if (getTarget() != null) {
				child.setTarget(getTarget());
			}
			level.addFreshEntity(child);
		}
		for (Entity e : level.getEntities(this, new AABB(c, c).inflate(3.0), e -> Targets.canHarm(this, e))) {
			if (e.distanceToSqr(c) <= 9) {
				MonsterMagic.hurt(level, this, (LivingEntity) e, BURST_DAMAGE);
			}
		}
		dev.wildercord.cast.Sigils.flash(level, c, PINK, 3.0F);
		ElementFx.arcaneImpact(level, c, 1.4);
		MonsterMagic.sound(level, c, "monster_ooze_split", 1.3F, 1.0F);
		discard();
	}

	// ------------------------------------------------------------------ touching

	@Override
	protected void dealDamage(LivingEntity target) {
		float before = target.getHealth();
		super.dealDamage(target);
		// A touch that landed drinks a little of a caster's mana too.
		if (target.getHealth() < before && target instanceof ServerPlayer player && !Spellbooks.cord(player).isEmpty()) {
			float mana = Spellbooks.mana(player);
			if (mana > 0) {
				Spellbooks.setMana(player, Math.max(0, mana - TOUCH_DRAIN));
				if (level() instanceof ServerLevel level) {
					Motes.seek(level, player.getBoundingBox().getCenter(), getBoundingBox().getCenter(), VIOLET, 0.12, 10, 0.3);
				}
			}
		}
	}

	@Override
	protected @Nullable ParticleOptions getParticleType() {
		return ParticleTypes.WITCH;
	}

	@Override
	protected SoundEvent getJumpSound() {
		return isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
	}

	@Override
	protected SoundEvent getSquishSound() {
		return isTiny() ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		SoundEvent kit = MonsterMagic.kit("monster_ooze_hurt");
		return kit != null ? kit : SoundEvents.SLIME_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		SoundEvent kit = MonsterMagic.kit("monster_ooze_death");
		return kit != null ? kit : SoundEvents.SLIME_DEATH;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return MonsterMagic.kit("monster_ooze_ambient");
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("fullness", fullness());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_FULLNESS, input.getFloatOr("fullness", 0.0F));
	}

	/** Unused by the game: a Mana Ooze never breeds. */
	@Override
	public @Nullable AbstractCubeMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}
}
