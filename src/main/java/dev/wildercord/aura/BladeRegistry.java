package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What only the server keeps of bonded blades, with the overworld's saved data: every bond ever made (whose it is, and whether it still
 * stands), so a blade turning up in a chest years later knows whether to go home or whether its bond is over; and the blades on their
 * way home to a swordsman who isn't here (taken from someone else's hands while their swordsman was away), handed over when they join.
 *
 * <p>A blade is never copied here: one on its way home is moved out of the hands that held it and into this list, and out of the list
 * into its swordsman's inventory, in one tick each.</p>
 */
public final class BladeRegistry extends SavedData {
	/**
	 * One bond.
	 *
	 * @param owner     whose it is now
	 * @param ownerName their name as last seen
	 * @param active    whether it still stands (false once released, or once its swordsman bonded another)
	 * @param since     when it was made (game time)
	 */
	public record Entry(UUID owner, String ownerName, boolean active, long since) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner),
			Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Entry::ownerName),
			Codec.BOOL.optionalFieldOf("active", true).forGetter(Entry::active),
			Codec.LONG.optionalFieldOf("since", 0L).forGetter(Entry::since)
		).apply(i, Entry::new));
	}

	static final Codec<BladeRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC).optionalFieldOf("bonds", Map.of()).forGetter(r -> r.bonds),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.CODEC.listOf()).optionalFieldOf("homeward", Map.of()).forGetter(r -> r.homeward)
	).apply(i, BladeRegistry::new));
	static final SavedDataType<BladeRegistry> TYPE = new SavedDataType<>(Wildercord.id("bonded_blades"), BladeRegistry::new, CODEC, null);

	private final Map<UUID, Entry> bonds = new HashMap<>();
	private final Map<UUID, List<ItemStack>> homeward = new HashMap<>();

	public BladeRegistry() {
	}

	private BladeRegistry(Map<UUID, Entry> bonds, Map<UUID, List<ItemStack>> homeward) {
		this.bonds.putAll(bonds);
		homeward.forEach((owner, list) -> this.homeward.put(owner, new ArrayList<>(list)));
	}

	public static BladeRegistry of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public Optional<Entry> entry(UUID bond) {
		return Optional.ofNullable(bond == null ? null : bonds.get(bond));
	}

	/** Whether {@code bond} still stands. An unknown bond (a blade from another world, or made by a command) counts as over. */
	public boolean active(UUID bond) {
		Entry e = bond == null ? null : bonds.get(bond);
		return e != null && e.active();
	}

	/** Whether {@code bond} still stands and is {@code owner}'s. */
	public boolean activeFor(UUID bond, UUID owner) {
		Entry e = bond == null ? null : bonds.get(bond);
		return e != null && e.active() && e.owner().equals(owner);
	}

	void register(UUID bond, UUID owner, String ownerName, long now) {
		bonds.put(bond, new Entry(owner, ownerName, true, now));
		setDirty();
	}

	/** Ends {@code bond}: its blade, wherever it is, is just a weapon once it's next seen. */
	void end(UUID bond) {
		Entry e = bonds.get(bond);
		if (e != null && e.active()) {
			bonds.put(bond, new Entry(e.owner(), e.ownerName(), false, e.since()));
			setDirty();
		}
	}

	/** Gives {@code bond} to another swordsman (a blade passed to a disciple). */
	void moveTo(UUID bond, UUID owner, String ownerName) {
		Entry e = bonds.get(bond);
		bonds.put(bond, new Entry(owner, ownerName, true, e == null ? 0 : e.since()));
		setDirty();
	}

	/** Every bond {@code owner} has that still stands (one, unless something went wrong). */
	List<UUID> standing(UUID owner) {
		List<UUID> out = new ArrayList<>();
		bonds.forEach((id, e) -> {
			if (e.active() && e.owner().equals(owner)) {
				out.add(id);
			}
		});
		return out;
	}

	/** Keeps {@code blade} for {@code owner} until they're here (moved here, never copied). */
	void holdFor(UUID owner, ItemStack blade) {
		homeward.computeIfAbsent(owner, k -> new ArrayList<>()).add(blade);
		setDirty();
	}

	/** Takes every blade kept for {@code owner} (they're theirs to hand over now). */
	List<ItemStack> takeFor(UUID owner) {
		List<ItemStack> list = homeward.remove(owner);
		if (list == null || list.isEmpty()) {
			return List.of();
		}
		setDirty();
		return list;
	}

	/** How many blades wait for {@code owner} (the game tests ask). */
	public int waitingFor(UUID owner) {
		List<ItemStack> list = homeward.get(owner);
		return list == null ? 0 : list.size();
	}
}
