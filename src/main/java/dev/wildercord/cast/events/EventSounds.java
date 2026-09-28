package dev.wildercord.cast.events;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The world events' sounds, synthesised by {@code tools/sound_art.py} in the same key as the rest
 * (D major pentatonic), so a storm's arcs, a surge and the spells cast under it sound well together.
 */
public final class EventSounds {
	private EventSounds() {}

	/** A mana storm gathering: a low violet drone swelling under crackle and a rising run of bells. Heard from 48 blocks. */
	public static final SoundEvent STORM_START = register("storm_start");
	/** A mana storm passing: the shimmer drifts down and away. */
	public static final SoundEvent STORM_END = register("storm_end");
	/** A violet arc between two points of a ley line: a glassy crackle. */
	public static final SoundEvent STORM_ARC = register("storm_arc");
	/** A spell surging in a mana storm: a wobbling, rising whoop. */
	public static final SoundEvent SURGE = register("surge");
	/** A star falling: a long, falling whistle over a rushing roar. Heard from far away. */
	public static final SoundEvent STAR_FALL = register("star_fall");
	/** A star landing: a deep boom, shattering glass and a struck bell ringing out. Heard from far away. */
	public static final SoundEvent STAR_IMPACT = register("star_impact");
	/** A rift tearing open: ripping noise, a reversed bell and a dark drone. Heard from 48 blocks. */
	public static final SoundEvent RIFT_OPEN = register("rift_open");
	/** A rift sealing: everything rushes in to a thud and a bright chord. */
	public static final SoundEvent RIFT_CLOSE = register("rift_close");
	/** A wave pouring out of a rift: a low horn swelling through the tear. Heard from 48 blocks. */
	public static final SoundEvent RIFT_WAVE = register("rift_wave");

	private static SoundEvent register(String path) {
		Identifier id = Wildercord.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {}
}
