package dev.wildercord.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A staff, the Tome of the Fifth Page or a focus: worn in its gear slot (or held, while the slot is empty), it changes your spells. */
public class CastingGearItem extends Item {
	public final GearDef def;

	public CastingGearItem(GearDef def, Properties properties) {
		super(properties);
		this.def = def;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Gear.effect(def).copy().withStyle(ChatFormatting.GRAY));
		if (def.kind().staff()) {
			builder.accept(Component.translatable("tooltip.wildercord.gear.flourish").withStyle(ChatFormatting.GRAY));
		}
		GearSlot slot = GearSlot.of(def);
		if (slot != null) {
			builder.accept(Component.translatable("tooltip.wildercord.gear.slot", Component.translatable(slot.nameKey())).withStyle(ChatFormatting.DARK_AQUA));
		}
		builder.accept(Component.translatable(def.kind().staff() ? "tooltip.wildercord.gear.either_hand" : "tooltip.wildercord.gear.off_hand")
			.withStyle(slot != null ? ChatFormatting.DARK_GRAY : ChatFormatting.DARK_AQUA));
	}
}
