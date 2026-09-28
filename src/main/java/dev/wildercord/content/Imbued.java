package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.equipment.Equippable;

import java.util.List;

/**
 * A spell imbued into an item (the Imbue link): the runes it holds, how many times it can still
 * release them, its colour, and whether imbuing gave it its glint (so taking the spell away takes
 * the glint too). How it's released depends on the item; see {@link #release}.
 */
public record Imbued(List<String> runes, int charges, int color, boolean glint) {
	public static final int MAX_RUNES = 16;

	public static final Codec<Imbued> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.listOf().fieldOf("runes").forGetter(Imbued::runes),
		Codec.INT.fieldOf("charges").forGetter(Imbued::charges),
		Codec.INT.optionalFieldOf("color", 0xE678DC).forGetter(Imbued::color),
		Codec.BOOL.optionalFieldOf("glint", false).forGetter(Imbued::glint)
	).apply(i, Imbued::new));

	public static final StreamCodec<ByteBuf, Imbued> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_RUNES)), Imbued::runes,
		ByteBufCodecs.VAR_INT, Imbued::charges,
		ByteBufCodecs.INT, Imbued::color,
		ByteBufCodecs.BOOL, Imbued::glint,
		Imbued::new);

	public Imbued {
		runes = List.copyOf(runes.size() > MAX_RUNES ? runes.subList(0, MAX_RUNES) : runes);
		charges = Math.max(0, Math.min(99, charges));
	}

	public Imbued withCharges(int charges) {
		return new Imbued(runes, charges, color, glint);
	}

	/** How an imbued item lets its spell go. */
	public enum Release {
		/** Blocks: placed, the block becomes a glyph that holds it. */
		PLACE,
		/** Bows and crossbows: with the next arrows, where each lands. */
		SHOT,
		/** Armour and shields: at whatever hurts you. */
		WORN,
		/** Tools: at each block they break, and at what they strike. */
		TOOL,
		/** Weapons: at what they strike. */
		WEAPON,
		/** Anything else: when used, at what you're looking at. */
		USE
	}

	public static Release release(ItemStack stack) {
		if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) {
			return Release.PLACE;
		}
		if (stack.getItem() instanceof ProjectileWeaponItem) {
			return Release.SHOT;
		}
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR || stack.has(DataComponents.BLOCKS_ATTACKS)) {
			return Release.WORN;
		}
		// Swords carry a tool component too (for cobwebs), but one that can't break blocks in creative: they're weapons.
		net.minecraft.world.item.component.Tool tool = stack.get(DataComponents.TOOL);
		if (tool != null && tool.canDestroyBlocksInCreative()) {
			return Release.TOOL;
		}
		if (stack.has(DataComponents.WEAPON)) {
			return Release.WEAPON;
		}
		return Release.USE;
	}
}
