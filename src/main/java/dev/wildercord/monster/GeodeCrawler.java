package dev.wildercord.monster;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Geode Crawler: a low, broad beetle of the caves, its back a cluster of amethyst grown through dark chitin, found most
 * often near the geodes it feeds on. Struck, it curls up into a ball of crystal (a fifth of any blow gets through), rattles
 * (the tell), and rolls at whoever hit it; a roll that meets a wall leaves it dazed.
 *
 * <p>Crystal cracks under the right blow: a mace, a pickaxe, a blast, or a shock (storm magic, lightning) lands harder than
 * usual even on a curled one and breaks it open, dazed. Earth and arcane magic glance off its crystal.</p>
 */
public class GeodeCrawler extends WildMonster {
	static final int CURL = 40;
	static final int RATTLE = 12;
	static final int ROLL = 30;
	static final int DAZED = 40;
	static final double ROLL_SPEED = 0.42;
	static final float ROLL_DAMAGE = 6.0F;
	private static final int AMETHYST = 0xB48CF0;

	private enum Phase { CRAWL, CURLED, RATTLE, ROLL, DAZED }

	private Phase phase = Phase.CRAWL;
	private long phaseUntil;
	private long nextCurl;
	private Vec3 rollDir;
	private final Map<UUID, Long> told = new HashMap<>();

	public GeodeCrawler(EntityType<? extends GeodeCrawler> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.MOVEMENT_SPEED, 0.22)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.FOLLOW_RANGE, 20.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false) {
			@Override
			public boolean canUse() {
				return phase == Phase.CRAWL && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return phase == Phase.CRAWL && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7) {
			@Override
			public boolean canUse() {
				return phase == Phase.CRAWL && super.canUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.PELT), List.of(Runes.RING, Runes.TREMOR), List.of(Runes.BOLT, Runes.SHACKLE));
	}

	/** Whether it's balled up in its crystal (curled, rattling or rolling). */
	public boolean curled() {
		return phase == Phase.CURLED || phase == Phase.RATTLE || phase == Phase.ROLL;
	}

	public boolean rolling() {
		return phase == Phase.ROLL;
	}

	public boolean dazed() {
		return phase == Phase.DAZED;
	}

	// ------------------------------------------------------------------ curling and rolling

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		switch (phase) {
			case CURLED -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					if (target != null && target.isAlive() && Targets.canHarm(this, target) && distanceTo(target) < 14 && hasLineOfSight(target)) {
						enter(Phase.RATTLE, now, RATTLE);
						MonsterMagic.sound(level, position(), "monster_geode_rattle", 1.0F, 1.0F);
					} else {
						uncurl(now);
					}
				}
			}
			case RATTLE -> {
				getNavigation().stop();
				if (target != null) {
					face(target);
				}
				if (now >= phaseUntil) {
					if (target == null || !target.isAlive()) {
						uncurl(now);
					} else {
						Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
						rollDir = d.lengthSqr() < 1.0E-4 ? getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
						enter(Phase.ROLL, now, ROLL);
						MonsterMagic.sound(level, position(), "monster_geode_roll", 1.0F, 1.0F);
					}
				}
			}
			case ROLL -> {
				getNavigation().stop();
				setDeltaMovement(rollDir.x * ROLL_SPEED, getDeltaMovement().y, rollDir.z * ROLL_SPEED);
				float yaw = (float) Math.toDegrees(Math.atan2(-rollDir.x, rollDir.z));
				setYRot(yaw);
				setYBodyRot(yaw);
				if (now % 2 == 0) {
					BlockState under = level.getBlockState(blockPosition().below());
					if (!under.isAir()) {
						level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), 3, 0.3, 0.05, 0.3, 0.05);
					}
				}
				if (target != null && target.isAlive() && getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
					if (target.hurtServer(level, damageSources().mobAttack(this), ROLL_DAMAGE)) {
						MonsterMagic.knock(target, position(), 0.9);
					}
					MonsterMagic.sound(level, position(), "monster_geode_curl", 1.0F, 0.8F);
					uncurl(now);
				} else if (horizontalCollision) {
					// It rolled into the wall: dazed.
					daze(level, now);
				} else if (now >= phaseUntil) {
					uncurl(now);
				}
			}
			case DAZED -> {
				getNavigation().stop();
				if (now % 5 == 0) {
					ElementFx.shimmer(level, position().add(0, getBbHeight() + 0.2, 0), 0.3, 2);
				}
				if (now >= phaseUntil) {
					enter(Phase.CRAWL, now, 0);
				}
			}
			default -> {
			}
		}
		setState(GUARD, curled());
		setState(WINDUP, phase == Phase.RATTLE);
		setState(ACTING, phase == Phase.ROLL);
		setState(STUNNED, phase == Phase.DAZED);
	}

	private void enter(Phase next, long now, int ticks) {
		phase = next;
		phaseUntil = now + ticks;
	}

	/** Balls up in its crystal for {@code ticks}. */
	public void curl(ServerLevel level, int ticks) {
		long now = level.getGameTime();
		enter(Phase.CURLED, now, ticks);
		nextCurl = now + CURL + 100;
		setState(GUARD, true);
		getNavigation().stop();
		MonsterMagic.sound(level, position(), "monster_geode_curl", 1.0F, 1.0F);
	}

	private void uncurl(long now) {
		enter(Phase.CRAWL, now, 0);
		setState(GUARD, false);
	}

	private void daze(ServerLevel level, long now) {
		enter(Phase.DAZED, now, DAZED);
		setDeltaMovement(getDeltaMovement().multiply(-0.3, 1, -0.3));
		ElementFx.shatterRing(level, position().add(0, 0.4, 0), 1.0);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()), getX(), getY() + 0.5, getZ(), 12,
			0.4, 0.3, 0.4, 0.1);
		MonsterMagic.sound(level, position(), "monster_geode_hurt", 1.0F, 0.7F);
	}

	// ------------------------------------------------------------------ its crystal

	/** Whether a blow is the kind that cracks crystal: a mace, a pickaxe, a blast, lightning, or a storm spell. */
	public static boolean cracking(DamageSource source) {
		if (source.is(DamageTypes.MACE_SMASH) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_LIGHTNING)) {
			return true;
		}
		if (MonsterMagic.spellLanding() && MonsterMagic.spellElement().equals("storm")) {
			return true;
		}
		ItemStack weapon = source.getWeaponItem();
		return weapon != null && (weapon.is(ItemTags.PICKAXES) || weapon.is(ItemTags.MACE_ENCHANTABLE));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		long now = level.getGameTime();
		if (curled()) {
			boolean cracks = cracking(source);
			float dealt = MonsterRules.curled(damage, cracks);
			boolean hurt = super.hurtServer(level, source, dealt);
			if (cracks) {
				// Cracked open: it uncurls, dazed.
				daze(level, now);
				MonsterMagic.sound(level, position(), "monster_geode_crack", 1.2F, 1.0F);
			} else {
				ElementFx.shimmer(level, getBoundingBox().getCenter(), 0.5, 4);
				playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.2F);
				if (source.getEntity() instanceof ServerPlayer player) {
					Long last = told.get(player.getUUID());
					if (last == null || now - last > 300) {
						told.put(player.getUUID(), now);
						MonsterMagic.tell(player, Component.translatable("message.wildercord.geode_crawler_curled").withColor(0xD8C4FF));
					}
				}
			}
			return hurt;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && isAlive() && phase == Phase.CRAWL && now >= nextCurl && !cracking(source)) {
			curl(level, CURL);
		}
		return hurt;
	}

	@Override
	public boolean isPushable() {
		return !curled() && super.isPushable();
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return MonsterMagic.kit("monster_geode_ambient");
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return MonsterMagic.kit("monster_geode_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return MonsterMagic.kit("monster_geode_death");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.SPIDER_STEP, 0.25F, 1.3F);
		if (getRandom().nextInt(3) == 0) {
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.3F, 1.0F + getRandom().nextFloat() * 0.4F);
		}
	}
}
