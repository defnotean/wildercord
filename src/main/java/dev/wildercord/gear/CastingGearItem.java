package dev.wildercord.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A staff, the Tome of the Fifth Page or a focus: held while casting, it changes your spells. */
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
		builder.accept(Component.translatable(def.kind().staff() ? "tooltip.wildercord.gear.either_hand" : "tooltip.wildercord.gear.off_hand")
			.withStyle(ChatFormatting.DARK_AQUA));
	}
}
