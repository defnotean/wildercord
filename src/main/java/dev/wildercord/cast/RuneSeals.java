package dev.wildercord.cast;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spells meeting the Archive's Rune Seals (and its braziers). A player's spell of the right
 * element lights every seal of that element in the door it touched; when every element in a
 * door is lit, the door dissolves. Fire spells also light unlit campfires they land on, where world magic
 * may change blocks ({@link WorldMagic#mayChange}: not in a claim or spawn protection, nor on a server that
 * keeps spells off its blocks).
 */
public final class RuneSeals {
	private RuneSeals() {}

	private static final int MAX_DOOR = 96;

	static void onSpell(Cast cast, Cast.Hit hit, String element) {
		if (element.isEmpty() || !(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		ServerLevel level = cast.level;
		BlockPos center = hit.block() != null ? hit.block() : BlockPos.containing(hit.point());
		RuneSealBlock.Element wanted = RuneSealBlock.Element.of(element);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
			BlockState state = level.getBlockState(pos);
			if (element.equals("fire") && state.getBlock() instanceof CampfireBlock && !state.getValue(CampfireBlock.LIT) && WorldMagic.mayChange(cast, pos.immutable())) {
				level.setBlock(pos, state.setValue(CampfireBlock.LIT, true), Block.UPDATE_ALL);
				Fx.sound(level, Vec3.atCenterOf(pos), SoundEvents.FIRECHARGE_USE, 0.8F, 1.0F);
			}
			if (wanted != null && state.is(WildercordBlocks.RUNE_SEAL) && state.getValue(RuneSealBlock.ELEMENT) == wanted) {
				light(level, pos.immutable(), wanted, player);
				return;
			}
		}
	}

	/** The door a seal belongs to: every seal connected to it. */
	private static List<BlockPos> door(ServerLevel level, BlockPos start) {
		List<BlockPos> door = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty() && door.size() < MAX_DOOR) {
			BlockPos pos = queue.poll();
			if (!level.getBlockState(pos).is(WildercordBlocks.RUNE_SEAL)) {
				continue;
			}
			door.add(pos);
			for (Direction d : Direction.values()) {
				BlockPos next = pos.relative(d);
				if (seen.add(next)) {
					queue.add(next);
				}
			}
		}
		return door;
	}

	private static void light(ServerLevel level, BlockPos hitPos, RuneSealBlock.Element element, ServerPlayer player) {
		List<BlockPos> door = door(level, hitPos);
		int color = RuneColors.element(element.getSerializedName());
		boolean changed = false;
		for (BlockPos pos : door) {
			BlockState state = level.getBlockState(pos);
			if (state.getValue(RuneSealBlock.ELEMENT) == element && !state.getValue(RuneSealBlock.LIT)) {
				level.setBlock(pos, state.setValue(RuneSealBlock.LIT, true), Block.UPDATE_ALL);
				level.scheduleTick(pos, WildercordBlocks.RUNE_SEAL, RuneSealBlock.LIT_TICKS);
				Vfx.emit(level, new DustParticleOptions(color, 1.2F), Vec3.atCenterOf(pos), 4, 0.4, 0.0);
				changed = true;
			}
		}
		if (!changed) {
			return;
		}
		Fx.sound(level, Vec3.atCenterOf(hitPos), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
		Fx.sound(level, Vec3.atCenterOf(hitPos), SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.6F);
		EnumSet<RuneSealBlock.Element> unlit = EnumSet.noneOf(RuneSealBlock.Element.class);
		for (BlockPos pos : door) {
			BlockState state = level.getBlockState(pos);
			if (!state.getValue(RuneSealBlock.LIT)) {
				unlit.add(state.getValue(RuneSealBlock.ELEMENT));
			}
		}
		if (unlit.isEmpty()) {
			open(level, door, player);
		}
	}

	private static void open(ServerLevel level, List<BlockPos> door, ServerPlayer player) {
		Vec3 middle = Vec3.ZERO;
		for (BlockPos pos : door) {
			middle = middle.add(Vec3.atCenterOf(pos));
		}
		middle = middle.scale(1.0 / door.size());
		BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE_TILES.defaultBlockState());
		for (int i = 0; i < door.size(); i++) {
			BlockPos pos = door.get(i);
			BlockState state = level.getBlockState(pos);
			int color = RuneColors.element(state.getValue(RuneSealBlock.ELEMENT).getSerializedName());
			int delay = (int) Math.round(Vec3.atCenterOf(pos).distanceTo(middle) * 3);
			Scheduler.later(1 + delay, () -> {
				if (level.getBlockState(pos).is(WildercordBlocks.RUNE_SEAL)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					Vfx.emit(level, dust, Vec3.atCenterOf(pos), 10, 0.35, 0.05);
					Vfx.emit(level, new DustParticleOptions(color, 1.4F), Vec3.atCenterOf(pos), 6, 0.4, 0.0);
					Vfx.emit(level, ParticleTypes.ENCHANT, Vec3.atCenterOf(pos), 8, 0.4, 0.4);
				}
			});
		}
		Vec3 facing = player.position().subtract(middle);
		Sigils.circle(level, middle, new Vec3(facing.x, 0, facing.z).lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : new Vec3(facing.x, 0, facing.z),
			0xE8D8FF, 0xB8A0FF, 2.2F, 40);
		Sigils.send(level, SigilOption.flat(SigilOption.RING, 0xE8D8FF, 3.0F, 40, 0.1F), middle.add(0, -1.4, 0));
		Fx.sound(level, middle, SoundEvents.END_PORTAL_FRAME_FILL, 1.2F, 0.8F);
		Fx.sound(level, middle, SoundEvents.VAULT_OPEN_SHUTTER, 1.0F, 0.7F);
		for (ServerPlayer nearby : level.players()) {
			if (nearby.position().distanceTo(middle) < 24) {
				Grimoire.feat(nearby, Feats.SEAL);
			}
		}
	}
}
