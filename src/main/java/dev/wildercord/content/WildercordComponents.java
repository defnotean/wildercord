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

	public static void init() {}
}
