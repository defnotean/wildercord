package dev.wildercord.compat;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Names a {@link SavedData} store the way 1.21.5's {@code SavedDataType} does: an id, a factory and a
 * codec. 1.21.4 only knows {@code SavedData.Factory}, so the codec is folded into the loader here and
 * the save side is the override each store class carries.
 */
public final class McSavedDataType<T extends SavedData> {

	public final String name;
	private final Supplier<T> factory;
	private final Codec<T> codec;

	public McSavedDataType(String name, Supplier<T> factory, Codec<T> codec) {
		this.name = name;
		this.factory = factory;
		this.codec = codec;
	}

	/** The 1.21.4 factory shape: fresh through the supplier, loaded by decoding the tag with the codec. */
	public SavedData.Factory<T> vanilla() {
		return new SavedData.Factory<>(factory, (tag, registries) -> decode(tag, registries));
	}

	public T decode(CompoundTag tag, HolderLookup.Provider registries) {
		return codec.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag).getOrThrow();
	}
}
