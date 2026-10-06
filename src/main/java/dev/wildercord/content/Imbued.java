package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.equipment.Equippable;

import java.util.List;
import java.util.UUID;

/**
 * A spell imbued into an item (the Imbue link): the runes it holds, how many times it can still
 * release them, its colour, whether imbuing gave it its glint (so taking the spell away takes the
 * glint too), and who imbued it with which serial (a caster keeps only so many imbued items: see
 * {@code Imbuing.MAX_ITEMS}; serial 0 is an item from before that, which is never counted). How it's
 * released depends on the item; see {@link #release}.
 */
public record Imbued(List<String> runes, int charges, int color, boolean glint, UUID maker, long serial) {
	public static final int MAX_RUNES = 16;
	public static final UUID NOBODY = new UUID(0L, 0L);

	public static final Codec<Imbued> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.listOf().fieldOf("runes").forGetter(Imbued::runes),
		Codec.INT.fieldOf("charges").forGetter(Imbued::charges),
		Codec.INT.optionalFieldOf("color", 0xE678DC).forGetter(Imbued::color),
		Codec.BOOL.optionalFieldOf("glint", false).forGetter(Imbued::glint),
		UUIDUtil.CODEC.optionalFieldOf("maker", NOBODY).forGetter(Imbued::maker),
		Codec.LONG.optionalFieldOf("serial", 0L).forGetter(Imbued::serial)
	).apply(i, Imbued::new));

	public static final StreamCodec<ByteBuf, Imbued> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_RUNES)), Imbued::runes,
		ByteBufCodecs.VAR_INT, Imbued::charges,
		ByteBufCodecs.INT, Imbued::color,
		ByteBufCodecs.BOOL, Imbued::glint,
		UUIDUtil.STREAM_CODEC, Imbued::maker,
		ByteBufCodecs.VAR_LONG, Imbued::serial,
		Imbued::new);

	public Imbued {
		runes = dev.wildercord.spell.ExciseRules.boundedIds(runes, MAX_RUNES);
		charges = Math.max(0, Math.min(99, charges));
	}

	public Imbued withCharges(int charges) {
		return new Imbued(runes, charges, color, glint, maker, serial);
	}

	/** Whether this counts toward its maker's imbued items (anything imbued since the limit came in). */
	public boolean counted() {
		return serial != 0L && !maker.equals(NOBODY);
	}

	/** How an imbued item lets its spell go. */
	public enum Release {
		/** Blocks: placed, the block becomes a glyph that holds it. */
		PLACE,
		/** Bows and crossbows: with the next shots, where the arrow lands (one arrow of a crossbow's triple shot). */
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
