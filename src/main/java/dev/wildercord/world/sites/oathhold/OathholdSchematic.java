package dev.wildercord.world.sites.oathhold;

import dev.wildercord.Wildercord;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and places bundled Sponge v3 WorldEdit schematics ({@code .schem}) into {@link OathholdCanvas}.
 */
public final class OathholdSchematic {
	private OathholdSchematic() {}

	private record Schematic(int width, int height, int length, BlockState[] palette, int[] indices) {}

	private static volatile Schematic house1;
	private static volatile Schematic house2;

	public static boolean placeHouse1(OathholdCanvas c, int baseY, int x0, int z0, int x1, int z1, boolean spawnResident, int seed) {
		Schematic s = house1;
		if (s == null) {
			s = load("/data/wildercord/oathhold/house1.schem");
			house1 = s;
		}
		return place(s, c, baseY, x0, z0, x1, z1, spawnResident, seed);
	}

	public static boolean placeHouse2(OathholdCanvas c, int baseY, int x0, int z0, int x1, int z1, boolean spawnResident, int seed) {
		Schematic s = house2;
		if (s == null) {
			s = load("/data/wildercord/oathhold/house2.schem");
			house2 = s;
		}
		return place(s, c, baseY, x0, z0, x1, z1, spawnResident, seed);
	}

	private static boolean place(Schematic s, OathholdCanvas c, int baseY, int x0, int z0, int x1, int z1, boolean spawnResident, int seed) {
		if (s == null) return false;
		Rotation rot = Rotation.values()[Math.floorMod(seed, 4)];
		int rw = (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) ? s.length : s.width;
		int rl = (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) ? s.width : s.length;
		int ox = x0 + ((x1 - x0 + 1) - rw) / 2;
		int oz = z0 + ((z1 - z0 + 1) - rl) / 2;
		if (!c.intersects(ox, oz, ox + rw - 1, oz + rl - 1)) return true;

		for (int y = 0; y < s.height; y++) {
			for (int z = 0; z < s.length; z++) {
				for (int x = 0; x < s.width; x++) {
					int idx = (y * s.length + z) * s.width + x;
					int pIdx = s.indices[idx];
					if (pIdx < 0 || pIdx >= s.palette.length) continue;
					BlockState state = s.palette[pIdx];
					if (state.isAir()) continue;
					int rx = switch (rot) {
						case NONE -> x;
						case CLOCKWISE_90 -> s.length - 1 - z;
						case CLOCKWISE_180 -> s.width - 1 - x;
						case COUNTERCLOCKWISE_90 -> z;
					};
					int rz = switch (rot) {
						case NONE -> z;
						case CLOCKWISE_90 -> x;
						case CLOCKWISE_180 -> s.length - 1 - z;
						case COUNTERCLOCKWISE_90 -> s.width - 1 - x;
					};
					c.set(ox + rx, baseY + y, oz + rz, state.rotate(rot));
				}
			}
		}
		if (spawnResident) {
			c.spawnCitizen(ox + s.width / 2, baseY + 1, oz + s.length / 2,
				OathholdCitizenRules.Archetype.TOWNSFOLK, 10, 0.0F);
		}
		return true;
	}

	private static Schematic load(String resourcePath) {
		try (InputStream in = OathholdSchematic.class.getResourceAsStream(resourcePath)) {
			if (in == null) return null;
			CompoundTag root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
			CompoundTag schem = root.contains("Schematic") ? root.getCompoundOrEmpty("Schematic") : root;
			int w = schem.getShortOr("Width", (short) 0) & 0xFFFF;
			int h = schem.getShortOr("Height", (short) 0) & 0xFFFF;
			int l = schem.getShortOr("Length", (short) 0) & 0xFFFF;
			CompoundTag blocks = schem.contains("Blocks") ? schem.getCompoundOrEmpty("Blocks") : schem;
			CompoundTag palTag = blocks.getCompoundOrEmpty("Palette");
			byte[] data = blocks.getByteArray("Data").orElse(new byte[0]);

			int maxId = -1;
			for (String key : palTag.keySet()) {
				maxId = Math.max(maxId, palTag.getIntOr(key, 0));
			}
			BlockState[] palette = new BlockState[Math.max(1, maxId + 1)];
			for (int i = 0; i < palette.length; i++) palette[i] = Blocks.AIR.defaultBlockState();
			for (String key : palTag.keySet()) {
				int id = palTag.getIntOr(key, 0);
				if (id >= 0 && id < palette.length) {
					try {
						palette[id] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, key, false).blockState();
					} catch (Exception ignored) {
						palette[id] = Blocks.AIR.defaultBlockState();
					}
				}
			}

			int[] indices = new int[w * h * l];
			int i = 0;
			int idx = 0;
			while (i < data.length && idx < indices.length) {
				int val = 0;
				int shift = 0;
				while (i < data.length) {
					int b = data[i++] & 0xFF;
					val |= (b & 0x7F) << shift;
					if ((b & 0x80) == 0) break;
					shift += 7;
				}
				indices[idx++] = val;
			}
			return new Schematic(w, h, l, palette, indices);
		} catch (Exception e) {
			Wildercord.LOGGER.warn("Failed to load {}", resourcePath, e);
			return null;
		}
	}
}
