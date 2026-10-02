package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bonded blade lying on the ground is kept for its swordsman (see {@link BondedBlades}): it never rots away, fire, lava, cactus and
 * blasts can't touch it, nobody else can pick it up (no player, no mob: it carries vanilla's "never" pickup delay, and only its own
 * swordsman is let through, once the usual moment after a throw has passed), and hoppers leave it lying ({@code HopperBondMixin}). A
 * blade whose bond is over goes back to being an ordinary item at once.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityBondMixin extends Entity {
	@Shadow
	private int pickupDelay;
	@Shadow
	private int age;

	@Shadow
	public abstract ItemStack getItem();

	/** Ticks before its own swordsman may take it back (the moment vanilla gives a thrown item), or -1 before it's been seen. */
	@Unique
	private int wildercord$ownerDelay = -1;
	/** Whether it's being kept for its swordsman (its bond stands). */
	@Unique
	private boolean wildercord$kept;

	protected ItemEntityBondMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void wildercord$keep(CallbackInfo ci) {
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		ItemStack stack = getItem();
		if (!BondedBlades.bonded(stack)) {
			if (wildercord$kept) {
				wildercord$kept = false;
				pickupDelay = 10;
				age = 0;
			}
			return;
		}
		if (!wildercord$kept || tickCount % 20 == 0) {
			if (!BondedBlades.stands(server, stack)) {
				// Its bond is over: only steel now, for anyone to pick up.
				((ItemEntity) (Object) this).setItem(BondedBlades.lapse(stack));
				wildercord$kept = false;
				pickupDelay = 10;
				age = 0;
				return;
			}
			if (!wildercord$kept) {
				wildercord$ownerDelay = pickupDelay == 32767 ? 0 : Math.max(0, pickupDelay);
				wildercord$kept = true;
			}
		}
		pickupDelay = 32767;
		age = -32768;
		if (wildercord$ownerDelay > 0) {
			wildercord$ownerDelay--;
		}
	}

	@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
	private void wildercord$onlyItsSwordsman(Player player, CallbackInfo ci) {
		if (level().isClientSide() || !wildercord$kept) {
			return;
		}
		ci.cancel();
		ItemStack stack = getItem();
		if (wildercord$ownerDelay > 0 || player.isSpectator() || !BondedBlades.mayPickUp(player, stack)) {
			return;
		}
		int count = stack.getCount();
		Item item = stack.getItem();
		if (player.getInventory().add(stack)) {
			player.take(this, count);
			if (stack.isEmpty()) {
				discard();
				stack.setCount(count);
			}
			player.awardStat(Stats.ITEM_PICKED_UP.get(item), count);
			player.onItemPickup((ItemEntity) (Object) this);
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void wildercord$unharmed(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
		if (BondedBlades.bonded(getItem())) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
	private void wildercord$unburnt(CallbackInfoReturnable<Boolean> cir) {
		if (BondedBlades.bonded(getItem())) {
			cir.setReturnValue(true);
		}
	}

	/** Whether this item is kept for its swordsman now (the game tests ask). */
	@Unique
	public boolean wildercord$isKept() {
		return wildercord$kept;
	}
}
