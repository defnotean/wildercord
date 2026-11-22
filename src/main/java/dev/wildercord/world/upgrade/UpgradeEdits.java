package dev.wildercord.world.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import dev.wildercord.compat.McSavedDataType;
import java.util.*;

/** Conservative future player-edit evidence. Missing entries NEVER prove old terrain was untouched. */
public final class UpgradeEdits extends SavedData {
	private static final int MAX_CHUNKS=65536;
	private final Set<Long> chunks=new HashSet<>();
	private boolean saturated;
	private static final Codec<UpgradeEdits> CODEC=RecordCodecBuilder.create(i->i.group(
		Codec.LONG.listOf().fieldOf("chunks").forGetter(e->e.chunks.stream().sorted().toList()),
		Codec.BOOL.optionalFieldOf("saturated",false).forGetter(e->e.saturated)
	).apply(i,UpgradeEdits::new));
	private static final McSavedDataType<UpgradeEdits> TYPE=new McSavedDataType<>("upgrade_known_edits", UpgradeEdits::new, CODEC);

	@Override
	public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
		return (net.minecraft.nbt.CompoundTag) CODEC.encodeStart(
			net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries), this)
			.result().orElseThrow();
	}
	public UpgradeEdits() {}
	private UpgradeEdits(List<Long> chunks,boolean saturated){this.chunks.addAll(chunks.stream().limit(MAX_CHUNKS).toList());this.saturated=saturated || chunks.size()>MAX_CHUNKS;}
	public static void successfulPlayerEdit(ServerLevel level,BlockPos point) {
		var data=dev.wildercord.compat.McSavedData.computeIfAbsent(level.getDataStorage(), TYPE);
		long key=net.minecraft.world.level.ChunkPos.asLong(point);
		if(data.saturated || data.chunks.contains(key))return;
		if(data.chunks.size()>=MAX_CHUNKS)data.saturated=true;else data.chunks.add(key);
		data.setDirty();
	}
	static boolean edited(ServerLevel level,int x,int z) {
		var data=dev.wildercord.compat.McSavedData.get(level.getDataStorage(), TYPE);
		return data!=null && (data.saturated || data.chunks.contains(net.minecraft.world.level.ChunkPos.asLong(x,z)));
	}
}
