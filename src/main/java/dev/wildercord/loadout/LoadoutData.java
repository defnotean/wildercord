package dev.wildercord.loadout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's loadouts, in the order they were saved, and which one was last saved or loaded
 * ({@code current}, -1 for none), so the quick-switch key knows where to go next. Immutable: every
 * change returns a new instance, which is what makes the attachment save and sync it.
 *
 * <p>More than {@link LoadoutRules#MAX} are kept if they're there (a server that lowers the limit
 * mustn't delete anyone's); only saving a new one past it is refused.</p>
 */
public record LoadoutData(List<Loadout> loadouts, int current) {
	public static final LoadoutData EMPTY = new LoadoutData(List.of(), -1);

	public LoadoutData {
		loadouts = List.copyOf(loadouts);
		if (current < -1 || current >= loadouts.size()) {
			current = -1;
		}
	}

	public List<String> names() {
		return loadouts.stream().map(Loadout::name).toList();
	}

	public int size() {
		return loadouts.size();
	}

	public Loadout get(int index) {
		return index >= 0 && index < loadouts.size() ? loadouts.get(index) : null;
	}

	/** With {@code loadout} added at the end, and current. */
	public LoadoutData with(Loadout loadout) {
		List<Loadout> next = new ArrayList<>(loadouts);
		next.add(loadout);
		return new LoadoutData(next, next.size() - 1);
	}

	/** With loadout {@code index} replaced by {@code loadout}; {@code current} says whether it becomes the current one. */
	public LoadoutData replace(int index, Loadout loadout, boolean current) {
		List<Loadout> next = new ArrayList<>(loadouts);
		next.set(index, loadout);
		return new LoadoutData(next, current ? index : this.current);
	}

	public LoadoutData without(int index) {
		List<Loadout> next = new ArrayList<>(loadouts);
		next.remove(index);
		return new LoadoutData(next, LoadoutRules.afterDelete(current, index));
	}

	public LoadoutData withCurrent(int index) {
		return new LoadoutData(loadouts, index);
	}

	public static final Codec<LoadoutData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Loadout.CODEC.listOf().optionalFieldOf("loadouts", List.of()).forGetter(LoadoutData::loadouts),
		Codec.INT.optionalFieldOf("current", -1).forGetter(LoadoutData::current)
	).apply(i, LoadoutData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutData> STREAM_CODEC = StreamCodec.composite(
		Loadout.STREAM_CODEC.apply(ByteBufCodecs.list()), LoadoutData::loadouts,
		ByteBufCodecs.VAR_INT, LoadoutData::current,
		LoadoutData::new
	);
}
