package dev.wildercord.content;

import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.world.LeyLines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Keeps a Wellstone's state in step with the ley line under it and shares its mana with people nearby. */
public class WellstoneBlockEntity extends BlockEntity {
	public static final double RANGE = 12.0;
	private static final int PURPLE = 0xB8A0FF;

	public WellstoneBlockEntity(BlockPos pos, BlockState state) {
		super(WildercordBlocks.WELLSTONE_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, WellstoneBlockEntity entity) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		long time = server.getGameTime();
		if ((time + pos.asLong()) % 20 != 0) {
			return;
		}
		boolean onLine = level.dimension() == Level.OVERWORLD
			&& LeyLines.strength(LeyWalker.seed(server), pos.getX() + 0.5, pos.getZ() + 0.5) > 0.3;
		if (onLine != state.getValue(WellstoneBlock.ACTIVE)) {
			level.setBlock(pos, state.setValue(WellstoneBlock.ACTIVE, onLine), Block.UPDATE_ALL);
			Vec3 top = Vec3.atCenterOf(pos).add(0, 0.6, 0);
			if (onLine) {
				server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.4F);
				server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
				dev.wildercord.cast.Sigils.ground(server, Vec3.atBottomCenterOf(pos).add(0, 1.02, 0), PURPLE, 0xE8E0FF, 2.4F, 60);
				ServerPlayer nearest = server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 10, false) instanceof ServerPlayer p ? p : null;
				if (nearest != null) {
					Grimoire.feat(nearest, Feats.WELLSTONE);
				}
			} else {
				server.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
			}
			dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.END_ROD, top.x, top.y, top.z, 12, 0.3, 0.3, 0.3, 0.05);
		}
		if (!onLine) {
			return;
		}
		for (ServerPlayer player : server.players()) {
			if (player.position().distanceTo(Vec3.atCenterOf(pos)) <= RANGE) {
				player.setAttached(WildercordAttachments.WELL_UNTIL, time + 45);
			}
		}
		Vec3 base = Vec3.atBottomCenterOf(pos).add(0, 1.02, 0);
		if (time % 60 < 20) {
			dev.wildercord.cast.Sigils.layer(server, base, new Vec3(0, 1, 0), SigilOption.CIRCLE, PURPLE, 1.6F, 64, 0.03F);
			dev.wildercord.cast.Sigils.layer(server, base.add(0, 0.01, 0), new Vec3(0, 1, 0), SigilOption.RING, 0xE8E0FF, 2.2F, 64, -0.05F);
		}
		// Mana drawn up out of the ground, streaming into the stone.
		for (int i = 0; i < 3; i++) {
			double a = server.getRandom().nextDouble() * Math.PI * 2;
			double r = 2 + server.getRandom().nextDouble() * 3;
			Vec3 from = base.add(Math.cos(a) * r, -0.9, Math.sin(a) * r);
			dev.wildercord.cast.Fx.sendParticles(server, new TrailParticleOption(base.add(0, 0.2, 0), PURPLE, 20), from.x, from.y, from.z, 1, 0, 0, 0, 0);
		}
		dev.wildercord.cast.Fx.sendParticles(server, new DustParticleOptions(0xE8E0FF, 1.0F), base.x, base.y + 0.4, base.z, 3, 0.25, 0.3, 0.25, 0);
		if (time % 80 == 0) {
			server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 0.9F);
		}
	}
}
