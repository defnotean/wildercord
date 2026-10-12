package dev.wildercord.aura.world;

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

/**
 * The lantern at the foot of the Barrow Field's gibbet: taken down once the Lantern-Bearer lies still, by anyone not carrying one
 * already. It stays where it stands for whoever comes after.
 */
public final class BarrowLanternBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 9, 11);

	public BarrowLanternBlock(Properties properties) {
		super(properties);
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return take(level, pos, player);
	}

	@Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return take(level, pos, player);
	}

	private static InteractionResult take(Level level, BlockPos pos, Player who) {
		if (!(level instanceof ServerLevel server) || !(who instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
		if (BearersLantern.carries(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.bearers_lantern.bound").withColor(0x8AA8B0));
			return InteractionResult.FAIL;
		}
		if (guarded(server, pos)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.bearers_lantern.guarded").withColor(0xB0473C));
			return InteractionResult.FAIL;
		}
		BearersLantern.take(player);
		server.playSound(null, pos, SoundEvents.LANTERN_BREAK, SoundSource.PLAYERS, 1, .7F);
		server.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2F, .8F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, .5F, .9F);
		server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + .5, pos.getY() + .4, pos.getZ() + .5, 24, .25, .3, .25, .03);
		server.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 16, .4, .7, .4, .03);
		player.sendOverlayMessage(Component.translatable("message.wildercord.bearers_lantern.take").withColor(0xA8E4EC));
		return InteractionResult.SUCCESS;
	}

	/** Whether the Bearer still walks its field. */
	private static boolean guarded(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(LanternBearer.class, new AABB(pos).inflate(LanternBearerRules.LANTERN_GUARD), LanternBearer::isAlive).isEmpty();
	}

	@Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0)
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + .4 + random.nextDouble() * .2, pos.getY() + .35, pos.getZ() + .4 + random.nextDouble() * .2, 0, .01, 0);
		if (random.nextInt(12) == 0)
			level.addParticle(ParticleTypes.SOUL, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 0, .02, 0);
	}
}
