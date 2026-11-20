package dev.wildercord.monster;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.List;

/**
 * A hovering chochin paper-lantern spirit guarding Japanese shrines, Cat Sanctuary halls, and onsen bathhouses.
 * Pulses a gentle glowing ember ring that can be stepped back from before expanding.
 */
public class PaperLanternSpirit extends WildMonster {
	private int pulseCooldown = 55;
	private int telegraphTicks;

	public PaperLanternSpirit(EntityType<? extends PaperLanternSpirit> type, Level level) {
		super(type, level);
		this.xpReward = 6;
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.FIRE), List.of(Runes.BURST, Runes.EMBER));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05, false));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!onGround() && getDeltaMovement().y < -0.06) {
			setDeltaMovement(getDeltaMovement().multiply(1.0, 0.65, 1.0));
		}
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive()) {
			telegraphTicks = 0;
			setState(WINDUP, false);
			setState(ACTING, false);
			return;
		}
		if (telegraphTicks > 0) {
			telegraphTicks--;
			setState(WINDUP, true);
			if (telegraphTicks % 4 == 0) {
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.8, getZ(), 4, 0.5, 0.3, 0.5, 0.01);
			}
			if (telegraphTicks == 0) {
				setState(WINDUP, false);
				setState(ACTING, true);
				level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.55F, 1.6F);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.7, getZ(), 14, 1.4, 0.2, 1.4, 0.04);
				if (distanceToSqr(target) <= 12.25) {
					target.hurtServer(level, damageSources().indirectMagic(this, this), 3.5F);
				}
			}
			return;
		}
		setState(ACTING, false);
		if (pulseCooldown > 0) {
			pulseCooldown--;
		} else if (distanceToSqr(target) <= 16.0) {
			pulseCooldown = 80;
			telegraphTicks = 18;
			level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.7F, 1.7F);
		}
	}
}
