package dev.wildercord.loadout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.content.CordTier;
import dev.wildercord.gear.SpellSlots;
import dev.wildercord.player.Spellbook;
import dev.wildercord.spell.Passives;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * One saved Cord setup: every spell row's runes and custom names, the passives and which of them are
 * on, and the selected spell. Rune ids only: a loadout never holds a rune, it only remembers where
 * runes went, so loading one can't give anyone a rune they don't know (see {@link Loadouts#load}).
 */
public record Loadout(String name, List<List<String>> spells, List<String> names, List<List<String>> passives, int passivesOff, int selected) {
	public Loadout {
		String cleaned = LoadoutRules.name(name);
		name = cleaned == null ? "?" : cleaned;
		spells = Spellbook.sized(spells, SpellSlots.ALL, CordTier.MAX_SOCKETS);
		passives = Spellbook.sized(passives, Passives.MAX, Passives.SOCKETS);
		List<String> sizedNames = new ArrayList<>(SpellSlots.ALL);
		for (int i = 0; i < SpellSlots.ALL; i++) {
			sizedNames.add(i < names.size() ? dev.wildercord.spell.SpellNames.clean(names.get(i)) : "");
		}
		names = List.copyOf(sizedNames);
		passivesOff &= (1 << Passives.MAX) - 1;
		selected = Math.floorMod(selected, SpellSlots.ALL);
	}

	/** The Cord as {@code book} has it now, under {@code name}. */
	public static Loadout of(String name, Spellbook book) {
		return new Loadout(name, book.spells(), book.names(), book.passives(), book.passivesOff(), book.selected());
	}

	/**
	 * {@code book} with this loadout threaded in. Only the rows, names, passives, their switches and the
	 * selected spell change: the runes learned stay exactly as they were, so a rune the player doesn't
	 * know (or a Cord can't hold, or a socket or row the Cord doesn't have) stays threaded but quiet, as
	 * it does after changing to a smaller Cord.
	 */
	public Spellbook applyTo(Spellbook book) {
		return new Spellbook(book.learned(), spells, selected, book.starterGiven(), passives, passivesOff, names);
	}

	/** The same setup under another name. */
	public Loadout named(String name) {
		return new Loadout(name, spells, names, passives, passivesOff, selected);
	}

	public boolean passiveOn(int slot) {
		return (passivesOff & (1 << slot)) == 0;
	}

	public static final Codec<Loadout> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("name").forGetter(Loadout::name),
		Codec.STRING.listOf().listOf().optionalFieldOf("spells", List.of()).forGetter(Loadout::spells),
		Codec.STRING.listOf().optionalFieldOf("names", List.of()).forGetter(Loadout::names),
		Codec.STRING.listOf().listOf().optionalFieldOf("passives", List.of()).forGetter(Loadout::passives),
		Codec.INT.optionalFieldOf("passives_off", 0).forGetter(Loadout::passivesOff),
		Codec.INT.optionalFieldOf("selected", 0).forGetter(Loadout::selected)
	).apply(i, Loadout::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, Loadout> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, Loadout::name,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), Loadout::spells,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Loadout::names,
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), Loadout::passives,
		ByteBufCodecs.VAR_INT, Loadout::passivesOff,
		ByteBufCodecs.VAR_INT, Loadout::selected,
		Loadout::new
	);
}
