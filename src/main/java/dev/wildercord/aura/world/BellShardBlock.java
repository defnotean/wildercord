package dev.wildercord.aura.world;

import dev.wildercord.player.WildercordAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Bell Shard on the altar: a hand laid on it, once each, once its guardian is dead. */
public final class BellShardBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(4, 0, 5, 12, 9, 11);

	public BellShardBlock(Properties properties) {
		super(properties);
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return touch(level, pos, player);
	}

	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return touch(level, pos, player);
	}

	private static InteractionResult touch(Level level, BlockPos pos, Player who) {
		if (!(level instanceof ServerLevel server) || !(who instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
		if (BellShard.bound(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.bell_shard.bound").withColor(0x8FBFB0));
			return InteractionResult.FAIL;
		}
		if (guarded(server, pos)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.bell_shard.guarded").withColor(0xC9A46A));
			return InteractionResult.FAIL;
		}
		player.setAttached(WildercordAttachments.BELL_SHARD, true);
		server.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.1F);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.02);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.BUBBLE_POP, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
		player.sendOverlayMessage(Component.translatable("message.wildercord.bell_shard.touch").withColor(0x9FE0CC));
		return InteractionResult.SUCCESS;
	}

	/** Whether its guardian still keeps its vigil nearby. */
	private static boolean guarded(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(DrownedBellWarden.class, new AABB(pos).inflate(BellWardenRules.SHARD_GUARD), DrownedBellWarden::isAlive).isEmpty();
	}

	@Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(5) == 0) {
			level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.3 + random.nextDouble() * 0.4,
				pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
		}
	}
}
