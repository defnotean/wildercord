package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** The spell written on a Spell Scroll: its runes (at most {@link #MAX_RUNES}), its name and who inscribed it. */
public record ScrollSpell(List<String> runes, String name, String author) {
	/** As many runes as a spell can hold; the network codec can't carry more. */
	public static final int MAX_RUNES = 16;

	public static final Codec<ScrollSpell> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.listOf().fieldOf("runes").forGetter(ScrollSpell::runes),
		Codec.STRING.optionalFieldOf("name", "").forGetter(ScrollSpell::name),
		Codec.STRING.optionalFieldOf("author", "").forGetter(ScrollSpell::author)
	).apply(i, ScrollSpell::new));

	public static final StreamCodec<ByteBuf, ScrollSpell> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_RUNES)), ScrollSpell::runes,
		ByteBufCodecs.STRING_UTF8, ScrollSpell::name,
		ByteBufCodecs.STRING_UTF8, ScrollSpell::author,
		ScrollSpell::new);

	public ScrollSpell {
		// A scroll made by command with more runes would disconnect everyone it was sent to.
		// Keep a refusing marker if truncation would hide Relay and leave a different, castable spell.
		runes = dev.wildercord.spell.RelayRules.boundedIds(runes, MAX_RUNES);
	}
}
