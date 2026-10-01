package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.spell.MasteryRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/**
 * The mastery inscribed on a Spell Scroll by a caster whose spell is Adept or higher (see {@code cast.Inscriptions}): the
 * spell's identity, the traits they earned for it (never borrowed ones, never experience), its sigil and its rank, and
 * who inscribed it. Read aloud, the scroll casts with those traits; studied, it teaches the spell at rank I with them
 * borrowed. Also the scroll's tooltip picture: the sigil.
 *
 * @param key    the spell's identity (see {@link MasteryRules#key})
 * @param traits the inscriber's own traits for it, in rank order
 * @param seed   its sigil's seed
 * @param rank   the rank it had when inscribed
 * @param author who inscribed it
 */
public record Inscription(String key, List<String> traits, long seed, int rank, String author) implements TooltipComponent {
	public Inscription {
		traits = List.copyOf(traits.size() > MasteryRules.SLOTS ? traits.subList(0, MasteryRules.SLOTS) : traits);
	}

	public static final Codec<Inscription> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("key").forGetter(Inscription::key),
		Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(Inscription::traits),
		Codec.LONG.optionalFieldOf("seed", 1L).forGetter(Inscription::seed),
		Codec.INT.optionalFieldOf("rank", MasteryRules.ADEPT).forGetter(Inscription::rank),
		Codec.STRING.optionalFieldOf("author", "").forGetter(Inscription::author)
	).apply(i, Inscription::new));

	public static final StreamCodec<ByteBuf, Inscription> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(1024), Inscription::key,
		ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(MasteryRules.SLOTS)), Inscription::traits,
		ByteBufCodecs.VAR_LONG, Inscription::seed,
		ByteBufCodecs.VAR_INT, Inscription::rank,
		ByteBufCodecs.stringUtf8(64), Inscription::author,
		Inscription::new);

	public static final DataComponentType<Inscription> TYPE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("inscription"),
		DataComponentType.<Inscription>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build()
	);

	public static void init() {}
}
