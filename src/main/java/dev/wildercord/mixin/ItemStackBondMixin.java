package dev.wildercord.mixin;

import dev.wildercord.aura.BladeBond;
import dev.wildercord.aura.BondedBlades;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bonded blade, as an item: once it has a name it goes by it (in its colour, plain text: never a name typed by a player read as
 * anything else), and it never breaks: worn to its last point it stays there, notched, until it's mended.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackBondMixin {
	@Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
	private void wildercord$bladeName(CallbackInfoReturnable<Component> cir) {
		BladeBond b = BondedBlades.bond((ItemStack) (Object) this);
		if (b != null) {
			String name = b.shownName();
			if (!name.isEmpty()) {
				cir.setReturnValue(Component.literal(name).withColor(0xFF000000 | (b.color() == 0 ? 0xE8E0F0 : b.color())));
			}
		}
	}

	@ModifyVariable(method = "applyDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int wildercord$neverBreaks(int damage) {
		ItemStack self = (ItemStack) (Object) this;
		if (damage >= self.getMaxDamage() && self.getMaxDamage() > 1 && BondedBlades.bonded(self)) {
			return self.getMaxDamage() - 1;
		}
		return damage;
	}
}
