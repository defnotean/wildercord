package dev.wildercord.aura.arts;

import com.mojang.math.Transformation;
import dev.wildercord.cast.BlockFx;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Scheduler;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Stone and ice that an art raises and lets sink: block displays (as the earth spells' heaved ground and stone spires are), never
 * the world's own blocks, so nothing is broken, placed or left behind in a claim or anywhere else. Each one grows, holds and
 * shrinks away on its own, and any left by a restart are taken away as their chunk loads ({@link BlockFx#fresh}).
 */
public final class ArtBlocks {
	private ArtBlocks() {}

	/** Displays an art has up now (for the tests, and a ceiling: an art adds none past {@link #MAX}). */
	private static int live;
	public static final int MAX = 192;

	/** The ground's own block under {@code at}, for stone that rises out of it (dirt for anything that isn't a plain block). */
	public static BlockState ground(ServerLevel level, Vec3 at) {
		BlockState state = ElementFx.groundBlock(level, at);
		if (state.getRenderShape() != RenderShape.MODEL || !state.getFluidState().isEmpty() || state.is(Blocks.GRASS_BLOCK)) {
			// Grass reads as a green slab rising: the soil under it reads as the earth heaving.
			return state.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
		}
		return state;
	}

	private static Display.BlockDisplay display(ServerLevel level, Vec3 at, BlockState state, Transformation start) {
		if (live >= MAX) {
			return null;
		}
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return null;
		}
		display.snapTo(at.x, at.y, at.z);
		display.setBlockState(state);
		display.setTransformation(start);
		BlockFx.fresh(display);
		level.addFreshEntity(display);
		live++;
		return display;
	}

	private static void tween(Display.BlockDisplay display, Transformation to, int ticks) {
		if (display.isRemoved()) {
			return;
		}
		display.setTransformationInterpolationDelay(0);
		display.setTransformationInterpolationDuration(ticks);
		display.setTransformation(to);
	}

	private static void discard(Display.BlockDisplay display) {
		if (!display.isRemoved()) {
			display.discard();
			live = Math.max(0, live - 1);
		}
	}

	private static Transformation shape(float x, float y, float z, Quaternionf turn, float w, float h, float d) {
		return new Transformation(new Vector3f(x, y, z), turn, new Vector3f(w, h, d), new Quaternionf());
	}

	/**
	 * A spire of {@code state} bursting up out of the ground at {@code base} to {@code height}, {@code width} across, leaning
	 * {@code lean} radians toward {@code yaw}: up in three ticks, standing {@code hold}, then sinking back in eight.
	 */
	public static void spire(ServerLevel level, Vec3 base, BlockState state, float width, float height, float yaw, float lean, int hold) {
		Quaternionf turn = new Quaternionf().rotateY(yaw).rotateX(lean);
		Display.BlockDisplay column = display(level, base, state, shape(-width / 2, -0.1F, -width / 2, turn, width, 0.05F, width));
		float cap = width * 0.62F;
		Quaternionf capTurn = new Quaternionf().rotateY(yaw + 0.785F).rotateX(lean);
		Display.BlockDisplay crown = display(level, base, state, shape(-cap / 2, -0.1F, -cap / 2, capTurn, cap, 0.05F, cap));
		if (column == null) {
			return;
		}
		Scheduler.later(1, () -> {
			tween(column, shape(-width / 2, -0.15F, -width / 2, turn, width, height * 0.78F, width), 3);
			if (crown != null) {
				tween(crown, shape(-cap / 2, height * 0.6F, -cap / 2, capTurn, cap, height * 0.4F, cap), 3);
			}
			dev.wildercord.cast.Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), base.add(0, 0.2, 0), 10, width * 0.4, 0.15);
		});
		Scheduler.later(4 + hold, () -> {
			tween(column, shape(-width / 2, -0.15F, -width / 2, turn, width, 0.04F, width), 8);
			if (crown != null) {
				tween(crown, shape(-cap / 2, -0.1F, -cap / 2, capTurn, cap, 0.04F, cap), 8);
			}
		});
		Scheduler.later(13 + hold, () -> {
			discard(column);
			if (crown != null) {
				discard(crown);
			}
		});
	}

	/** A slab of the ground tilting up out of the floor at {@code base} (as an earth spell heaves it), holding, and sinking back. */
	public static void slab(ServerLevel level, Vec3 base, BlockState state, float yaw, float size, float tilt, int delay, int hold) {
		Quaternionf flat = new Quaternionf().rotateY(yaw);
		Quaternionf tilted = new Quaternionf().rotateY(yaw).rotateX(tilt);
		Display.BlockDisplay display = display(level, base, state, shape(-size / 2, -0.55F, -size / 2, flat, size, 0.5F, size));
		if (display == null) {
			return;
		}
		Scheduler.later(Math.max(1, delay), () -> {
			tween(display, shape(-size / 2, -0.06F, -size / 2, tilted, size, 0.5F, size), 3);
			dev.wildercord.cast.Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), base.add(0, 0.2, 0), 4, 0.25, 0.1);
		});
		Scheduler.later(Math.max(1, delay) + 3 + hold, () -> tween(display, shape(-size / 2, -0.6F, -size / 2, flat, size, 0.5F, size), 8));
		Scheduler.later(Math.max(1, delay) + 12 + hold, () -> discard(display));
	}

	/** A thin sheet of {@code state} laid flat on the ground (an ice path), spreading in, lasting {@code life}, then melting away. */
	public static void sheet(ServerLevel level, Vec3 base, BlockState state, float size, float yaw, int delay, int life) {
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		Display.BlockDisplay display = display(level, base, state, shape(0, 0.01F, 0, turn, 0.01F, 0.01F, 0.01F));
		if (display == null) {
			return;
		}
		Scheduler.later(Math.max(1, delay), () -> tween(display, shape(-size / 2, 0.005F, -size / 2, turn, size, 0.07F, size), 3));
		Scheduler.later(Math.max(1, delay) + life, () -> tween(display, shape(-size * 0.3F, 0.0F, -size * 0.3F, turn, size * 0.6F, 0.01F, size * 0.6F), 12));
		Scheduler.later(Math.max(1, delay) + life + 13, () -> discard(display));
	}

	/**
	 * Something growing out of the ground at {@code base} (a bramble, a sapling's crown, a tuft of roots): {@code state} swelling from
	 * nothing to {@code size} across, its bottom at {@code lift} over the ground, turned {@code yaw}, after {@code delay} ticks, standing
	 * {@code hold}, then withering back to nothing in ten.
	 */
	public static void sprout(ServerLevel level, Vec3 base, BlockState state, float size, float lift, float yaw, int delay, int hold) {
		Quaternionf turn = new Quaternionf().rotateY(yaw);
		Display.BlockDisplay display = display(level, base, state, centred(turn, 0.01F, 0.01F, lift));
		if (display == null) {
			return;
		}
		Scheduler.later(Math.max(1, delay), () -> {
			tween(display, centred(turn, size, size, lift), 4);
			dev.wildercord.cast.Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, state), base.add(0, lift + size * 0.4, 0), 4, size * 0.3, 0.06);
		});
		Scheduler.later(Math.max(1, delay) + 4 + hold, () -> tween(display, centred(turn, size * 0.2F, size * 0.05F, lift), 10));
		Scheduler.later(Math.max(1, delay) + 15 + hold, () -> discard(display));
	}

	/** A block {@code width} across and {@code height} tall, turned by {@code turn} about its own middle, its bottom {@code lift} up. */
	private static Transformation centred(Quaternionf turn, float width, float height, float lift) {
		Vector3f corner = turn.transform(new Vector3f(-width / 2, 0, -width / 2));
		return new Transformation(new Vector3f(corner.x, lift, corner.z), turn, new Vector3f(width, height, width), new Quaternionf());
	}

	/** Displays an art has standing now (for the tests). */
	public static int live() {
		return live;
	}

	static void clear() {
		live = 0;
	}
}
