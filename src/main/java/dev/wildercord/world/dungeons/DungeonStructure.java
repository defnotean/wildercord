package dev.wildercord.world.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Locale;
import java.util.Optional;

/**
 * A dimension dungeon: one structure type, told by its data ({@code "dungeon": "ember_sanctum"})
 * which of the three it is. Each dungeon's piece finds its own footing: a cave floor in the Nether,
 * an island's surface in the End, the sea floor under deep ocean.
 */
public class DungeonStructure extends Structure {
	public enum Kind implements StringRepresentable {
		EMBER_SANCTUM,
		ASTRAL_OBSERVATORY,
		DROWNED_SCRIPTORIUM;

		public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final MapCodec<DungeonStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		settingsCodec(instance),
		Kind.CODEC.fieldOf("dungeon").forGetter(structure -> structure.kind)
	).apply(instance, DungeonStructure::new));

	private final Kind kind;

	public DungeonStructure(Structure.StructureSettings settings, Kind kind) {
		super(settings);
		this.kind = kind;
	}

	@Override
	protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		return switch (kind) {
			case EMBER_SANCTUM -> EmberSanctumPiece.locate(context);
			case ASTRAL_OBSERVATORY -> AstralObservatoryPiece.locate(context);
			case DROWNED_SCRIPTORIUM -> Optional.empty();
		};
	}

	@Override
	public StructureType<?> type() {
		return DungeonWorldgen.DUNGEON;
	}
}
