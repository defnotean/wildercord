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

/** The Weaver's Silk under the nest: taken up, once each, once the Matron is dead. */
public final class WeaversSilkBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 7, 13);

	public WeaversSilkBlock(Properties properties) {
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
		if (WeaversSilk.bound(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.weavers_silk.bound").withColor(0xC4BEB2));
			return InteractionResult.FAIL;
		}
		if (guarded(server, pos)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.weavers_silk.guarded").withColor(0xB0473C));
			return InteractionResult.FAIL;
		}
		player.setAttached(WildercordAttachments.WEAVERS_SILK, true);
		server.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, .9F, .7F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.1F);
		dev.wildercord.cast.feel.Feels.sound(server, player.position(), "aura_hollow_matron_climb", .7F, 1.3F);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.ITEM_COBWEB, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 24, 0.4, 0.3, 0.4, 0.05);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.ITEM_COBWEB, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.7, 0.4, 0.04);
		player.sendOverlayMessage(Component.translatable("message.wildercord.weavers_silk.take").withColor(0xE2DCCC));
		return InteractionResult.SUCCESS;
	}

	/** Whether the Matron still keeps her hall. */
	private static boolean guarded(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(HollowMatron.class, new AABB(pos).inflate(HollowMatronRules.SILK_GUARD), HollowMatron::isAlive).isEmpty();
	}

	@Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(6) == 0) {
			level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
		}
	}
}
