package dev.wildercord.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.NbtOps;
import com.mojang.serialization.RegistryOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Writes the entity and block entity fields 1.21.6+ save through {@code ValueOutput}. 1.21.5 has no
 * value API, so this is the same surface over a {@link CompoundTag}, encoding codecs with the level's
 * registries where the caller has them.
 */
public final class McValueOutput {

	private final CompoundTag tag;
	private final RegistryAccess registries;

	public McValueOutput(CompoundTag tag) {
		this(tag, RegistryAccess.EMPTY);
	}

	public McValueOutput(CompoundTag tag, RegistryAccess registries) {
		this.tag = tag;
		this.registries = registries == null ? RegistryAccess.EMPTY : registries;
	}

	/** The tag this writes, for code that also wants the raw nbt. */
	public CompoundTag tag() {
		return tag;
	}

	/** Encodes {@code value} with {@code codec} under {@code key}; a failed encode writes nothing. */
	public <T> void store(String key, Codec<T> codec, T value) {
		if (value == null) {
			return;
		}
		codec.encodeStart(ops(), value).result().ifPresent(encoded -> tag.put(key, encoded));
	}

	/** The list at {@code key}, written to one element at a time. */
	public <T> TypedOutputList<T> list(String key, Codec<T> codec) {
		return new TypedOutputList<>(key, codec);
	}

	public void putBoolean(String key, boolean value) {
		tag.putBoolean(key, value);
	}

	public void putByte(String key, byte value) {
		tag.putByte(key, value);
	}

	public void putInt(String key, int value) {
		tag.putInt(key, value);
	}

	public void putLong(String key, long value) {
		tag.putLong(key, value);
	}

	public void putFloat(String key, float value) {
		tag.putFloat(key, value);
	}

	public void putDouble(String key, double value) {
		tag.putDouble(key, value);
	}

	public void putString(String key, String value) {
		tag.putString(key, value);
	}

	private DynamicOps<Tag> ops() {
		return RegistryOps.of(NbtOps.INSTANCE, registries);
	}

	/** Appends encoded elements to the list stored under one key. */
	public final class TypedOutputList<T> {

		private final String key;
		private final Codec<T> codec;

		private TypedOutputList(String key, Codec<T> codec) {
			this.key = key;
			this.codec = codec;
		}

		/** Encodes and appends {@code value}; a failed encode appends nothing. */
		public void add(T value) {
			if (value == null) {
				return;
			}
			codec.encodeStart(ops(), value).result().ifPresent(encoded -> {
				ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
				list.add(encoded);
				tag.put(key, list);
			});
		}
	}
}
