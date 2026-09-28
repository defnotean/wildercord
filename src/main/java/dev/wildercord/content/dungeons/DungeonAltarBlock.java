package dev.wildercord.content.dungeons;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The altar at the heart of a dimension dungeon's arena, like the Archive's lectern: its boss rises
 * from it the first time a player comes near, and it goes quiet afterwards.
 */
public class DungeonAltarBlock extends Block implements EntityBlock {
	/** Which dungeon's altar, and so which boss wakes. */
	public enum Kind implements StringRepresentable {
		/** The Ember Sanctum's forge-altar: the Cinder Warden. */
		CINDER(12),
		/** The Astral Observatory's star-altar: the Star-Eater. */
		ASTRAL(14),
		/** The Drowned Scriptorium's core: the Tide Scribe. */
		TIDE(14);

		/** How near a player must come to wake it. */
		public final int reach;

		Kind(int reach) {
			this.reach = reach;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** Wakes this altar's boss. */
		void wake(ServerLevel level, BlockPos pos) {
			switch (this) {
				case CINDER -> dev.wildercord.cast.CinderWarden.rise(level, pos);
				case ASTRAL -> dev.wildercord.cast.StarEater.rise(level, pos);
				case TIDE -> {
				}
			}
		}
	}

	public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
	public static final BooleanProperty AWAKE = BooleanProperty.create("awake");
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

	public DungeonAltarBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(KIND, Kind.CINDER).setValue(AWAKE, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DungeonAltarBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != DungeonBlocks.ALTAR_ENTITY || state.getValue(AWAKE) ? null
			: (BlockEntityTicker<T>) (BlockEntityTicker<DungeonAltarBlockEntity>) DungeonAltarBlockEntity::serverTick;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(KIND, AWAKE);
	}
}
