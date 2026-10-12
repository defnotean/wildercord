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

/** The Cinder Antler by the stump: taken up, once each, once its stag is dead. */
public final class CinderAntlerBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(2, 0, 6, 14, 8, 10);

	public CinderAntlerBlock(Properties properties) {
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
		if (CinderAntler.bound(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.cinder_antler.bound").withColor(0xC08A5C));
			return InteractionResult.FAIL;
		}
		if (guarded(server, pos)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.cinder_antler.guarded").withColor(0xC9A46A));
			return InteractionResult.FAIL;
		}
		player.setAttached(WildercordAttachments.CINDER_ANTLER, true);
		server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, .8F, .7F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.1F);
		dev.wildercord.cast.feel.Feels.sound(server, player.position(), "aura_ashen_stag_bellow", .6F, 1.4F);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 20, 0.4, 0.2, 0.4, 0.03);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.WHITE_ASH, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.6, 0.5, 0.05);
		player.sendOverlayMessage(Component.translatable("message.wildercord.cinder_antler.take").withColor(0xF08A3C));
		return InteractionResult.SUCCESS;
	}

	/** Whether its stag still keeps the glade. */
	private static boolean guarded(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(AshenStag.class, new AABB(pos).inflate(AshenStagRules.ANTLER_GUARD), AshenStag::isAlive).isEmpty();
	}

	@Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.3, pos.getZ() + 0.4 + random.nextDouble() * 0.2, 0, 0.02, 0);
		}
		if (random.nextInt(9) == 0) {
			level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.4, pos.getZ() + 0.5, 0, 0.01, 0);
		}
	}
}
