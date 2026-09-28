package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** The spell written on a Spell Scroll: its runes, its name and who inscribed it. */
public record ScrollSpell(List<String> runes, String name, String author) {
	public static final Codec<ScrollSpell> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.listOf().fieldOf("runes").forGetter(ScrollSpell::runes),
		Codec.STRING.optionalFieldOf("name", "").forGetter(ScrollSpell::name),
		Codec.STRING.optionalFieldOf("author", "").forGetter(ScrollSpell::author)
	).apply(i, ScrollSpell::new));

	public static final StreamCodec<ByteBuf, ScrollSpell> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), ScrollSpell::runes,
		ByteBufCodecs.STRING_UTF8, ScrollSpell::name,
		ByteBufCodecs.STRING_UTF8, ScrollSpell::author,
		ScrollSpell::new);

	public ScrollSpell {
		runes = List.copyOf(runes);
	}
}
