package dev.wildercord.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.content.CordTier;
import dev.wildercord.spell.Passives;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a player knows and has threaded. Immutable: every edit returns a new
 * instance, which is what makes the data attachment save and sync it.
 *
 * <p>Rune ids are kept as plain strings even when no rune with that id is loaded, so
 * removing an add-on never deletes anything (its runes go silent instead).</p>
 */
public record Spellbook(List<String> learned, List<List<String>> spells, int selected, boolean starterGiven,
		List<List<String>> passives, int passivesOff) {
	public static final Spellbook EMPTY = new Spellbook(List.of(), List.of(), 0, false, List.of(), 0);

	public Spellbook(List<String> learned, List<List<String>> spells, int selected, boolean starterGiven) {
		this(learned, spells, selected, starterGiven, List.of(), 0);
	}

	public Spellbook {
		learned = List.copyOf(learned);
		spells = sized(spells, CordTier.MAX_SPELLS, CordTier.MAX_SOCKETS);
		passives = sized(passives, Passives.MAX, Passives.SOCKETS);
		selected = Math.floorMod(selected, CordTier.MAX_SPELLS);
		passivesOff &= (1 << Passives.MAX) - 1;
	}

	private static List<List<String>> sized(List<List<String>> lists, int count, int sockets) {
		List<List<String>> sized = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			List<String> spell = i < lists.size() ? lists.get(i) : List.of();
			sized.add(List.copyOf(spell.subList(0, Math.min(spell.size(), sockets))));
		}
		return List.copyOf(sized);
	}

	public static final Codec<Spellbook> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.listOf().optionalFieldOf("learned", List.of()).forGetter(Spellbook::learned),
		Codec.STRING.listOf().listOf().optionalFieldOf("spells", List.of()).forGetter(Spellbook::spells),
		Codec.INT.optionalFieldOf("selected", 0).forGetter(Spellbook::selected),
		Codec.BOOL.optionalFieldOf("starter_given", false).forGetter(Spellbook::starterGiven),
		Codec.STRING.listOf().listOf().optionalFieldOf("passives", List.of()).forGetter(Spellbook::passives),
		Codec.INT.optionalFieldOf("passives_off", 0).forGetter(Spellbook::passivesOff)
	).apply(i, Spellbook::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Spellbook> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Spellbook::learned,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), Spellbook::spells,
		ByteBufCodecs.VAR_INT, Spellbook::selected,
		ByteBufCodecs.BOOL, Spellbook::starterGiven,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), Spellbook::passives,
		ByteBufCodecs.VAR_INT, Spellbook::passivesOff,
		Spellbook::new
	);

	public boolean knows(String runeId) {
		return learned.contains(runeId);
	}

	public Spellbook learn(String runeId) {
		if (knows(runeId)) {
			return this;
		}
		List<String> next = new ArrayList<>(learned);
		next.add(runeId);
		return new Spellbook(next, spells, selected, starterGiven, passives, passivesOff);
	}

	public Spellbook withSpell(int index, List<String> runes) {
		List<List<String>> next = new ArrayList<>(spells);
		next.set(index, runes);
		return new Spellbook(learned, next, selected, starterGiven, passives, passivesOff);
	}

	public Spellbook withPassive(int index, List<String> runes) {
		List<List<String>> next = new ArrayList<>(passives);
		next.set(index, runes);
		return new Spellbook(learned, spells, selected, starterGiven, next, passivesOff);
	}

	public boolean passiveOn(int index) {
		return (passivesOff & (1 << index)) == 0;
	}

	public Spellbook withPassiveOn(int index, boolean on) {
		int off = on ? passivesOff & ~(1 << index) : passivesOff | (1 << index);
		return new Spellbook(learned, spells, selected, starterGiven, passives, off);
	}

	public Spellbook withSelected(int index) {
		return new Spellbook(learned, spells, index, starterGiven, passives, passivesOff);
	}

	public Spellbook withStarterGiven() {
		return new Spellbook(learned, spells, selected, true, passives, passivesOff);
	}
}
