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

/** The Coilroot's plant: eaten of, once each, once its guardian is dead. */
public final class CoilrootBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

	public CoilrootBlock(Properties properties) {
		super(properties);
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return eat(level, pos, player);
	}

	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return eat(level, pos, player);
	}

	private static InteractionResult eat(Level level, BlockPos pos, Player who) {
		if (!(level instanceof ServerLevel server) || !(who instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
		if (Coilroot.eaten(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.coilroot.eaten").withColor(0x9FBF7A));
			return InteractionResult.FAIL;
		}
		if (guarded(server, pos)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.coilroot.guarded").withColor(0xC9A46A));
			return InteractionResult.FAIL;
		}
		player.setAttached(WildercordAttachments.COILROOT, true);
		server.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.7F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.3F);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.2, player.getZ(), 40, 0.6, 0.2, 0.6, 0.6);
		player.sendOverlayMessage(Component.translatable("message.wildercord.coilroot.eat").withColor(0xB6E08A));
		return InteractionResult.SUCCESS;
	}

	/** Whether its guardian still lives nearby. */
	private static boolean guarded(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(RootcoilSerpent.class, new AABB(pos).inflate(SerpentRules.COILROOT_GUARD), RootcoilSerpent::isAlive).isEmpty();
	}

	@Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.4 + random.nextDouble() * 0.4,
				pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
		}
	}
}
