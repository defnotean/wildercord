package dev.wildercord.cast.events;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the world events remember across a restart (kept with the overworld): when each world may
 * next see a star fall or a rift open, when each region may next have a storm, and where stars are
 * lying (and until when), so a star fallen before a restart still counts as the one in its world.
 * Times are game times, which every world shares.
 */
public final class EventLedger extends SavedData {
	/** A star lying in a world, until it fades. */
	record Star(String level, BlockPos pos, long until) {
		static final Codec<Star> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("level").forGetter(Star::level),
			BlockPos.CODEC.fieldOf("pos").forGetter(Star::pos),
			Codec.LONG.fieldOf("until").forGetter(Star::until)
		).apply(i, Star::new));
	}

	static final Codec<EventLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("next", Map.of()).forGetter(ledger -> ledger.next),
		Star.CODEC.listOf().optionalFieldOf("stars", List.of()).forGetter(ledger -> ledger.stars)
	).apply(i, EventLedger::new));
	static final SavedDataType<EventLedger> TYPE = new SavedDataType<>(Wildercord.id("world_events"), EventLedger::new, CODEC, null);

	/** When each thing may next happen: {@code star:<world>}, {@code rift:<world>}, {@code storm:<region>}. */
	private final Map<String, Long> next = new HashMap<>();
	private final List<Star> stars = new ArrayList<>();

	public EventLedger() {
	}

	private EventLedger(Map<String, Long> next, List<Star> stars) {
		this.next.putAll(next);
		this.stars.addAll(stars);
	}

	static EventLedger of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	private static String world(ResourceKey<Level> level) {
		return level.identifier().toString();
	}

	static String star(ResourceKey<Level> level) {
		return "star:" + world(level);
	}

	static String rift(ResourceKey<Level> level) {
		return "rift:" + world(level);
	}

	static String storm(long region) {
		return "storm:" + region;
	}

	/** When {@code key} may next happen (0: any time). */
	long next(String key) {
		return next.getOrDefault(key, 0L);
	}

	/** {@code key} may not happen again before {@code at}; anything long past is forgotten while we're here. */
	void setNext(String key, long at, long now) {
		next.values().removeIf(t -> t < now);
		next.put(key, at);
		setDirty();
	}

	/** Whether a star is lying in this world (one not yet faded, even from before a restart). */
	boolean starLying(ResourceKey<Level> level, long now) {
		String w = world(level);
		for (Star star : stars) {
			if (star.level().equals(w) && star.until() > now) {
				return true;
			}
		}
		return false;
	}

	void starFell(ResourceKey<Level> level, BlockPos pos, long until) {
		stars.removeIf(s -> s.until() < until - EventRules.STAR_LIFETIME * 2L);
		stars.add(new Star(world(level), pos.immutable(), until));
		setDirty();
	}

	/** The star at {@code pos} is gone (looted, faded, or it burnt up, or it moved into its crater). */
	void starGone(ResourceKey<Level> level, BlockPos pos) {
		String w = world(level);
		if (stars.removeIf(s -> s.level().equals(w) && s.pos().equals(pos))) {
			setDirty();
		}
	}
}
