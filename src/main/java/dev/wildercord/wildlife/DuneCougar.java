package dev.wildercord.wildlife;

import dev.wildercord.cast.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A Dune Cougar (0.13, see {@link PredatorRules}): a long, tawny cat of the savannas and badlands with a cream muzzle, a
 * dark-tipped rope of a tail and amber eyes, as big as the black bobcat it's kin to. It's warier: raw beef, mutton or pork
 * wins a wild one over one time in four. A tame one standing near its owner watches over them, and every few seconds every
 * monster close by glows for a while, picked out by its eye.
 */
public class DuneCougar extends BlackBobcat {
	public DuneCougar(EntityType<? extends DuneCougar> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 34.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 7.0)
			.add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected double tameChance() {
		return PredatorRules.COUGAR_TAME_CHANCE;
	}

	/** Raw beef, mutton or pork: red meat. */
	@Override
	public boolean tames(ItemStack stack) {
		return stack.is(Items.BEEF) || stack.is(Items.MUTTON) || stack.is(Items.PORKCHOP);
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level && getOwner() instanceof LivingEntity owner && owner.level() == level
			&& PredatorRules.marks(isTame(), isOrderedToSit(), distanceTo(owner), level.getGameTime(), getId())) {
			mark(level, owner);
		}
	}

	/**
	 * The cougar's perk: every monster within {@link PredatorRules#MARK_RADIUS} of {@code owner} glows for a while. Returns how
	 * many it marked; it rumbles when it marks any.
	 */
	public int mark(ServerLevel level, LivingEntity owner) {
		int marked = 0;
		for (Mob monster : level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(PredatorRules.MARK_RADIUS),
			e -> e instanceof Enemy && e.isAlive() && e.distanceTo(owner) <= PredatorRules.MARK_RADIUS)) {
			monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, PredatorRules.MARK_TICKS, 0, false, false), this);
			marked++;
		}
		if (marked > 0 && random.nextInt(4) == 0) {
			Fx.sound(level, position(), SoundEvents.OCELOT_AMBIENT, 0.8F, 0.45F);
		}
		return marked;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.85F;
	}
}
