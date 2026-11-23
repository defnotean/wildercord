package dev.wildercord.monster;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Clockwork wooden-and-brass shrine automaton patrolling Cat Sanctuary floors and Lantern Bathhouse halls.
 * Winds its spring arm briefly before delivering a measured sweeping blow so the fight remains readable and chill.
 */
public class KarakuriSentinel extends WildMonster {
	private int spinCooldown = 45;
	private int windupTicks;

	public KarakuriSentinel(EntityType<? extends KarakuriSentinel> type, Level level) {
		super(type, level);
		this.xpReward = 7;
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.SUZU_CHIME), List.of(Runes.SELF, Runes.OFUDA_WARD));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 52.0)
			.add(Attributes.ARMOR, 10.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.55)
			.add(Attributes.MOVEMENT_SPEED, 0.29)
			.add(Attributes.ATTACK_DAMAGE, 7.5)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.75));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep() {
		ServerLevel level = (ServerLevel) level();
		super.customServerAiStep();
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			windupTicks = 0;
			setState(WINDUP, false);
			setState(ACTING, false);
			return;
		}
		if (windupTicks > 0) {
			windupTicks--;
			getNavigation().stop();
			setState(WINDUP, true);
			if (windupTicks == 0) {
				setState(WINDUP, false);
				setState(ACTING, true);
				level.playSound(null, blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 0.75F, 1.3F);
				level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.0, getZ(), 14, 1.0, 0.3, 1.0, 0.08);
				Vec3 lunge = target.position().subtract(position()).multiply(1, 0, 1);
				if (lunge.lengthSqr() > 0.01) {
					setDeltaMovement(lunge.normalize().scale(0.45).add(0, 0.14, 0));
				}
				if (distanceToSqr(target) <= 13.0) {
					target.hurt(damageSources().mobAttack(this), 9.0F);
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
					Vec3 push = target.position().subtract(position()).multiply(1, 0, 1);
					if (push.lengthSqr() > 0.01) {
						Vec3 v = push.normalize().scale(0.55);
						target.push(v.x, 0.22, v.z);
					}
				}
			}
			return;
		}
		setState(ACTING, false);
		if (spinCooldown > 0) {
			spinCooldown--;
		} else if (distanceToSqr(target) <= 12.0) {
			spinCooldown = 45;
			windupTicks = 14;
			level.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 0.8F, 1.1F);
		}
	}
}
