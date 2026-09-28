package dev.wildercord.content;

import dev.wildercord.menu.FusionAltarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Fusion Altar: an amethyst table on a deepslate plinth. Right-click to open it, put runes in,
 * and it works out which fusion you mean: three of the same rune rank it up, two effects and an
 * amethyst shard combine into a new effect, and a Blank Rune and string tie one of your spells into
 * a Knot. The screen only asks; {@link FusionAltarMenu} checks everything on the server.
 */
public class FusionAltarBlock extends Block {
	public static final Component TITLE = Component.translatable("container.wildercord.fusion_altar");

	private static final VoxelShape SHAPE = Shapes.or(
		Block.box(1, 0, 1, 15, 3, 15),
		Block.box(3, 3, 3, 13, 10, 13),
		Block.box(0, 10, 0, 16, 14, 16));

	public FusionAltarBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			player.openMenu(state.getMenuProvider(level, pos));
			level.playSound(null, pos, WildercordSounds.ALTAR_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		return new SimpleMenuProvider((containerId, inventory, player) -> new FusionAltarMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
	}
}
