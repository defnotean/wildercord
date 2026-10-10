package dev.wildercord.content;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class WildercordComponents {
	private WildercordComponents() {}

	/**
	 * Which rune a {@code wildercord:rune} item is, by rune id. Every rune shares the one
	 * item, so an add-on only needs a definition and a texture. Unknown ids are kept, not
	 * dropped: the item shows as a Silent Rune until its add-on returns.
	 */
	public static final DataComponentType<String> RUNE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("rune"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build()
	);

	/** A rune item's rank (2 or 3), made at the Fusion Altar; missing means rank I. Learning it ranks the rune up. */
	public static final DataComponentType<Integer> RANK = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("rank"),
		DataComponentType.<Integer>builder().persistent(Codec.intRange(1, 3)).networkSynchronized(ByteBufCodecs.VAR_INT).build()
	);

	/** The spell inscribed on a Spell Scroll. */
	public static final DataComponentType<ScrollSpell> SCROLL = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("scroll"),
		DataComponentType.<ScrollSpell>builder().persistent(ScrollSpell.CODEC).networkSynchronized(ScrollSpell.STREAM_CODEC).build()
	);

	/** A spell imbued into an item, with the charges it has left (see {@link Imbued}). */
	public static final DataComponentType<Imbued> IMBUED = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("imbued"),
		DataComponentType.<Imbued>builder().persistent(Imbued.CODEC).networkSynchronized(Imbued.STREAM_CODEC).build()
	);

	/** A flawed or corrupted rune's twist (see {@link dev.wildercord.spell.RuneTwistRules}), by id. */
	public static final DataComponentType<String> TWIST = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("twist"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build()
	);

	public static void init() {}
}
