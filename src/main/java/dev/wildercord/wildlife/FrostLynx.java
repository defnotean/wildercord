package dev.wildercord.wildlife;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A Frost Lynx (0.13, see {@link PredatorRules}): a silver, dark-spotted cat of the snowfields with long black ear tufts and
 * pale green eyes, a little smaller than the black bobcat it's kin to. Cold never touches it. Raw rabbit or chicken wins a
 * wild one over one time in three; its bite chills what it bites, slowing it and frosting it over.
 */
public class FrostLynx extends BlackBobcat {
	public FrostLynx(EntityType<? extends FrostLynx> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected double tameChance() {
		return PredatorRules.LYNX_TAME_CHANCE;
	}

	/** Raw rabbit or raw chicken: what it hunts. */
	@Override
	public boolean tames(ItemStack stack) {
		return stack.is(Items.RABBIT) || stack.is(Items.CHICKEN);
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity bitten) {
			chill(bitten);
		}
		return hit;
	}

	/** The lynx's perk: slows what it bit and frosts it over (the frost is only a look unless it's already near frozen). */
	public void chill(LivingEntity bitten) {
		bitten.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PredatorRules.CHILL_TICKS, PredatorRules.CHILL_AMPLIFIER), this);
		if (bitten.canFreeze()) {
			bitten.setTicksFrozen(PredatorRules.chilled(bitten.getTicksFrozen(), bitten.getTicksRequiredToFreeze()));
		}
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.25F;
	}
}
