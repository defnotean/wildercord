package dev.wildercord.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.NbtOps;
import com.mojang.serialization.RegistryOps;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Reads the entity and block entity fields 1.21.6+ save through {@code ValueInput}. 1.21.5 has no
 * value API, so this is the same surface over a {@link CompoundTag}, encoding codecs with the level's
 * registries where the caller has them.
 */
public final class McValueInput {

	private final CompoundTag tag;
	private final RegistryAccess registries;

	public McValueInput(CompoundTag tag) {
		this(tag, RegistryAccess.EMPTY);
	}

	public McValueInput(CompoundTag tag, RegistryAccess registries) {
		this.tag = tag;
		this.registries = registries == null ? RegistryAccess.EMPTY : registries;
	}

	/** The tag this reads, for code that also wants the raw nbt. */
	public CompoundTag tag() {
		return tag;
	}

	/** Decodes {@code key} with {@code codec}, or empty when it is absent or malformed. */
	public <T> Optional<T> read(String key, Codec<T> codec) {
		Tag value = tag.get(key);
		if (value == null) {
			return Optional.empty();
		}
		DataResult<T> decoded = codec.parse(ops(), value);
		return decoded.result();
	}

	/** Decodes the list at {@code key}; an absent or unreadable list reads as empty. */
	public <T> TypedInputList<T> listOrEmpty(String key, Codec<T> codec) {
		TypedInputList<T> values = new TypedInputList<>();
		Tag stored = tag.get(key);
		if (!(stored instanceof ListTag list)) {
			return values;
		}
		for (Tag element : list) {
			codec.parse(ops(), element).result().ifPresent(values::add);
		}
		return values;
	}

	public boolean getBooleanOr(String key, boolean fallback) {
		return tag.contains(key) ? tag.getBoolean(key) : fallback;
	}

	public byte getByteOr(String key, byte fallback) {
		return tag.contains(key) ? tag.getByte(key) : fallback;
	}

	public int getIntOr(String key, int fallback) {
		return tag.contains(key) ? tag.getInt(key) : fallback;
	}

	public long getLongOr(String key, long fallback) {
		return tag.contains(key) ? tag.getLong(key) : fallback;
	}

	public float getFloatOr(String key, float fallback) {
		return tag.contains(key) ? tag.getFloat(key) : fallback;
	}

	public double getDoubleOr(String key, double fallback) {
		return tag.contains(key) ? tag.getDouble(key) : fallback;
	}

	public String getStringOr(String key, String fallback) {
		return tag.contains(key) ? tag.getString(key) : fallback;
	}

	private DynamicOps<Tag> ops() {
		return RegistryOps.of(NbtOps.INSTANCE, registries);
	}

	/** A decoded list, which reads as the list it holds. */
	public static final class TypedInputList<T> extends ArrayList<T> {

		private static final long serialVersionUID = 1L;

		private TypedInputList() {}
	}
}
