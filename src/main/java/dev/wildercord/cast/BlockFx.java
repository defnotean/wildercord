package dev.wildercord.cast;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Spell visuals made of real blocks: block displays that grow, hold and shrink away (stone spires
 * bursting from the ground, ice closing around a frozen creature). They never touch the world's
 * blocks, and any left behind by a restart are removed as soon as their chunk loads.
 */
public final class BlockFx {
	private BlockFx() {}

	private static final String TAG = "wildercord.fx";

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Display && entity.entityTags().contains(TAG) && entity.tickCount == 0 && !FRESH.remove(entity)) {
				entity.discard();
			}
		});
	}

	/** Displays spawned this session: the load event lets these through and removes leftovers. */
	private static final java.util.Set<Entity> FRESH = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	private static Display.BlockDisplay display(ServerLevel level, Vec3 at, BlockState state, Transformation start) {
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return null;
		}
		display.snapTo(at.x, at.y, at.z);
		display.setBlockState(state);
		display.setTransformation(start);
		display.addTag(TAG);
		FRESH.add(display);
		level.addFreshEntity(display);
		return display;
	}

	private static Transformation box(float x, float y, float z, float w, float h, float d) {
		return new Transformation(new Vector3f(x, y, z), new Quaternionf(), new Vector3f(w, h, d), new Quaternionf());
	}

	/** Moves a display to a new shape, smoothly, over {@code ticks}. */
	private static void tween(Display.BlockDisplay display, Transformation to, int ticks) {
		if (display.isRemoved()) {
			return;
		}
		display.setTransformationInterpolationDelay(0);
		display.setTransformationInterpolationDuration(ticks);
		display.setTransformation(to);
	}

	/**
	 * A stone spire that bursts up out of the ground at {@code base}, stands a moment and sinks
	 * back: a column of dripstone crowned with a pointed tip.
	 */
	public static void spire(ServerLevel level, Vec3 base, float height, float width, int hold) {
		float w = width;
		float body = height * 0.62F;
		float tip = height - body;
		Display.BlockDisplay column = display(level, base, Blocks.DRIPSTONE_BLOCK.defaultBlockState(), box(-w / 2, -0.05F, -w / 2, w, 0.02F, w));
		BlockState tipState = Blocks.POINTED_DRIPSTONE.defaultBlockState()
			.setValue(SpeleothemBlock.TIP_DIRECTION, Direction.UP).setValue(SpeleothemBlock.THICKNESS, SpeleothemThickness.TIP);
		float tw = w * 1.7F;
		Display.BlockDisplay crown = display(level, base, tipState, box(-tw / 2, -0.05F, -tw / 2, tw, 0.02F, tw));
		if (column == null || crown == null) {
			return;
		}
		Scheduler.later(1, () -> {
			tween(column, box(-w / 2, -0.05F, -w / 2, w, body, w), 3);
			tween(crown, box(-tw / 2, body - 0.1F, -tw / 2, tw, tip + 0.1F, tw), 3);
		});
		Scheduler.later(4 + hold, () -> {
			tween(column, box(-w / 2, -0.05F, -w / 2, w, 0.02F, w), 8);
			tween(crown, box(-tw / 2, -0.05F, -tw / 2, tw, 0.02F, tw), 8);
		});
		Scheduler.later(13 + hold, () -> {
			column.discard();
			crown.discard();
		});
	}

	/** Ice closes around a creature, holds for {@code ticks}, then melts away. */
	public static void encase(ServerLevel level, Entity target, int ticks) {
		float w = target.getBbWidth() + 0.35F;
		float h = target.getBbHeight() + 0.25F;
		Vec3 at = target.position();
		Display.BlockDisplay ice = display(level, at, Blocks.ICE.defaultBlockState(), box(0, h / 2, 0, 0.01F, 0.01F, 0.01F));
		if (ice == null) {
			return;
		}
		Scheduler.later(1, () -> tween(ice, box(-w / 2, -0.1F, -w / 2, w, h, w), 3));
		Scheduler.later(ticks, () -> tween(ice, box(-w * 0.35F, 0, -w * 0.35F, w * 0.7F, 0.05F, w * 0.7F), 10));
		Scheduler.later(ticks + 11, ice::discard);
	}
}
