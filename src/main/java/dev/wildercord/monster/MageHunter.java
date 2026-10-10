package dev.wildercord.monster;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Dungeons;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A mage-hunter (0.13): a hooded illager who hunts casters. See {@link MageHunterRules}. Bolts come apart in the air around
 * it, spells hurt it half as much, and its axe takes mana with every blow. It is never Runebound.
 */
public class MageHunter extends Vindicator implements RuneboundKin {
	/** When it last showed a spell coming apart, so a barrage doesn't drown everything in smoke. */
	private long unravelledFxAt;

	public MageHunter(EntityType<? extends Vindicator> type, Level level) {
		super(type, level);
		xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Vindicator.createAttributes().add(Attributes.MAX_HEALTH, 32.0).add(Attributes.MOVEMENT_SPEED, 0.36)
			.add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of();
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || !isAlive()) return;
		boolean unravelled = false;
		for (RuneBolt bolt : level.getEntitiesOfClass(RuneBolt.class, getBoundingBox().inflate(MageHunterRules.NULL_RANGE),
			b -> b.distanceTo(this) <= MageHunterRules.NULL_RANGE)) {
			double x = bolt.getX(), y = bolt.getY(), z = bolt.getZ();
			if (bolt.unravel(this)) {
				unravelled = true;
				level.sendParticles(ParticleTypes.SMOKE, x, y, z, 6, 0.15, 0.15, 0.15, 0.02);
			}
		}
		long now = level.getGameTime();
		if (unravelled && now >= unravelledFxAt) {
			unravelledFxAt = now + 6;
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 1.6F);
		}
		// A thin grey haze about it: the null field, there to be seen before the first spell dies in it.
		if (now % 10 == 0) {
			level.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.0, getZ(), 3, 0.6, 0.6, 0.6, 0.0);
		}
	}

	/** Whether the harm landing now is a spell's: one being worked, one landing, or magic of any kind. */
	private boolean spellHarm(DamageSource source) {
		Cast cast = Effects.applyingCast();
		return cast != null && cast.caster != this || Dungeons.spellLanding()
			|| source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (spellHarm(source)) damage = MageHunterRules.spellHarm(damage);
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof ServerPlayer player && Spellbooks.tier(player) != null) {
			float taken = MageHunterRules.drained(Spellbooks.mana(player));
			if (taken > 0) {
				Spellbooks.setMana(player, Spellbooks.mana(player) - taken);
				level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.2, player.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 0.7F, 0.6F);
				MonsterMagic.tell(player, Component.translatable("message.wildercord.mage_hunter.drained", (int) taken));
			}
		}
		return hit;
	}
}
