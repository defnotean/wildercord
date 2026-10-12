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

/** The Rimescale on the Wyrmling's mound: taken up, once each, once the Wyrmling is dead. */
public final class RimescaleBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(3, 0, 6, 13, 14, 10);

	public RimescaleBlock(Properties properties) {
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
		if (Rimescale.bound(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.rimescale.bound").withColor(0xA8C4D4));
			return InteractionResult.FAIL;
		}
		if (guarded(server, pos)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.rimescale.guarded").withColor(0xB0473C));
			return InteractionResult.FAIL;
		}
		player.setAttached(WildercordAttachments.RIMESCALE, true);
		player.setTicksFrozen(0);
		server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, .6F);
		server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.1F);
		dev.wildercord.cast.feel.Feels.sound(server, player.position(), "aura_frost_wyrmling_frost", .7F, 1.3F);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.35, 0.4, 0.35, 0.05);
		dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.03);
		player.sendOverlayMessage(Component.translatable("message.wildercord.rimescale.take").withColor(0xD6EEF8));
		return InteractionResult.SUCCESS;
	}

	/** Whether the Wyrmling still keeps its hall. */
	private static boolean guarded(ServerLevel level, BlockPos pos) {
		return !level.getEntitiesOfClass(FrostWyrmling.class, new AABB(pos).inflate(FrostWyrmlingRules.SCALE_GUARD), FrostWyrmling::isAlive).isEmpty();
	}

	@Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.SNOWFLAKE, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.2 + random.nextDouble() * 0.7, pos.getZ() + 0.5, 0, -0.01, 0);
		}
	}
}
