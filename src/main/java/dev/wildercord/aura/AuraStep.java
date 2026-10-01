package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aura Step (Form): double-tap the Aura key and aura carries you a few blocks in an instant, the way you're moving (ahead when
 * you stand still), leaving afterimages of you in your aura's colour along the way. Nothing solid is passed through (the path is
 * swept with your whole body, climbing a slab or a stair as walking would), it never carries you over a dungeon ward's edge, into
 * or out of a warded room, and it stops short of lava and fire. For its first moments nothing can touch you. It costs aura and has
 * a short cooldown; spent past empty it never forms, with backlash.
 *
 * <p>The move is the server's: a few short steps over a few ticks, each a small teleport, so every client sees the same dash
 * and the afterimages are drawn where it really went ({@link Stepped}).</p>
 */
public final class AuraStep {
	private AuraStep() {}

	/** Server to client: {@code entity} stepped from {@code from} to {@code to}, facing {@code yaw}, in {@code color}: its afterimages. */
	public record Stepped(int entity, Vec3 from, Vec3 to, float yaw, int color) implements CustomPacketPayload {
		public static final Type<Stepped> TYPE = new Type<>(Wildercord.id("aura_step"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Stepped> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Stepped::entity, Vec3.STREAM_CODEC, Stepped::from, Vec3.STREAM_CODEC, Stepped::to, ByteBufCodecs.FLOAT, Stepped::yaw,
			ByteBufCodecs.INT, Stepped::color, Stepped::new).cast();

		@Override
		public Type<Stepped> type() {
			return TYPE;
		}
	}

	/** Until when each stepping player can't be touched. */
	private static final Map<UUID, Long> UNTOUCHABLE = new HashMap<>();

	static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Stepped.TYPE, Stepped.CODEC);
	}

	static void forget(UUID id) {
		UNTOUCHABLE.remove(id);
	}

	static void clear() {
		UNTOUCHABLE.clear();
	}

	/** The technique: step. */
	public static boolean step(ServerPlayer player) {
		long now = player.level().getGameTime();
		if (!Aura.holdsWeapon(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.no_weapon").withColor(0xA89CC8));
			return false;
		}
		AuraPresence.Timers timers = AuraPresence.timers(player);
		if (now < timers.stepReadyAt() || player.isPassenger() || player.isSleeping() || player.isFallFlying()) {
			return false;
		}
		WildercordConfig.AuraHeights settings = Config.get().aura().heights();
		Vec3 dir = direction(player);
		List<Vec3> path = path(player, dir, settings.stepDistance());
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 0.5) {
			// Up against a wall (or a ward's edge): there's nowhere to step.
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.step_blocked").withColor(0xA89CC8));
			AuraVfx.stepBlocked(player, Aura.color(player), dir);
			return false;
		}
		AuraRules.Spend paid = Aura.spend(player, settings.stepCost(), "step");
		AuraPresence.timers(player, AuraPresence.timers(player).stepReady(now + settings.stepCooldownTicks()));
		if (paid.backlash()) {
			// Spent past empty: the step never forms.
			return false;
		}
		go(player, path, dir);
		return true;
	}

	/** Which way a step goes: the way the player is moving (as their keys say), or ahead of them, level. */
	static Vec3 direction(ServerPlayer player) {
		Vec3 moving = player.getLastClientMoveIntent();
		Vec3 flat = new Vec3(moving.x, 0, moving.z);
		if (flat.lengthSqr() > 1.0E-4) {
			return flat.normalize();
		}
		Vec3 look = player.getViewVector(1.0F);
		flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
	}

	/**
	 * Where a step along {@code dir} can go, in probe-sized pieces from the player's feet: the whole body swept along it, a slab
	 * or a stair climbed, and stopped by anything solid, a dungeon ward's edge, the world border, lava or fire. The first point is
	 * where the player stands.
	 */
	static List<Vec3> path(ServerPlayer player, Vec3 dir, double distance) {
		ServerLevel level = player.level();
		Vec3 start = player.position();
		boolean warded = DungeonWards.warded(level, player.blockPosition());
		List<Vec3> points = new ArrayList<>();
		points.add(start);
		Vec3 at = start;
		AABB body = player.getBoundingBox();
		int pieces = (int) Math.floor(distance / AuraRules.STEP_PROBE + 1.0E-6);
		for (int i = 0; i < pieces; i++) {
			Vec3 next = at.add(dir.scale(AuraRules.STEP_PROBE));
			AABB box = body.move(next.subtract(start));
			if (!level.noCollision(player, box)) {
				Vec3 up = next.add(0, AuraRules.STEP_CLIMB, 0);
				AABB raised = body.move(up.subtract(start));
				if (!level.noCollision(player, raised)) {
					break;
				}
				next = up;
				box = raised;
			}
			if (DungeonWards.warded(level, BlockPos.containing(next)) != warded || !level.getWorldBorder().isWithinBounds(next.x, next.z)
					|| level.getBlockStates(box.inflate(0, 0.25, 0)).anyMatch(s -> s.getFluidState().is(FluidTags.LAVA) || s.is(BlockTags.FIRE))) {
				break;
			}
			points.add(next);
			at = next;
		}
		return points;
	}

	/** The dash itself: a few short teleports, the afterimages, light along the way, and a moment no harm can touch. */
	private static void go(ServerPlayer player, List<Vec3> path, Vec3 dir) {
		ServerLevel level = player.level();
		long now = level.getGameTime();
		int color = Aura.color(player);
		Vec3 from = path.getFirst();
		Vec3 to = path.getLast();
		UNTOUCHABLE.put(player.getUUID(), now + AuraRules.STEP_GUARD_TICKS);
		float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
		// Everyone who can see the player draws the afterimages; the player too (seen in third person).
		Stepped stepped = new Stepped(player.getId(), from, to, yaw, color);
		for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
			if (ServerPlayNetworking.canSend(viewer, Stepped.TYPE)) {
				ServerPlayNetworking.send(viewer, stepped);
			}
		}
		if (ServerPlayNetworking.canSend(player, Stepped.TYPE)) {
			ServerPlayNetworking.send(player, stepped);
		}
		Aura.sound(player, "aura_step", 1.0F, 1.0F);
		AuraVfx.stepStart(level, from, dir, color);
		int ticks = AuraRules.STEP_TICKS;
		for (int i = 1; i <= ticks; i++) {
			Vec3 point = path.get(Math.min(path.size() - 1, (int) Math.round((path.size() - 1) * i / (double) ticks)));
			Vec3 prev = path.get(Math.min(path.size() - 1, (int) Math.round((path.size() - 1) * (i - 1) / (double) ticks)));
			boolean last = i == ticks;
			Scheduler.later(i - 1, () -> {
				if (!player.isAlive() || player.level() != level || player.isPassenger()) {
					return;
				}
				// The view stays where the player has it: only the body moves.
				player.teleportTo(level, point.x, point.y, point.z, Relative.ROTATION, 0.0F, 0.0F, false);
				player.resetFallDistance();
				AuraVfx.stepTrail(level, prev, point, color);
				if (last) {
					// It lands where it was aimed: no slide on past the end.
					player.setDeltaMovement(Vec3.ZERO);
					AuraVfx.stepEnd(level, point, dir, color);
				}
			});
		}
	}

	/** Whether harm reaching {@code target} now falls in a step's untouchable moment (nothing that bypasses invulnerability does). */
	public static boolean untouchable(LivingEntity target, DamageSource source) {
		if (!(target instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		Long until = UNTOUCHABLE.get(player.getUUID());
		if (until == null) {
			return false;
		}
		if (player.level().getGameTime() > until) {
			UNTOUCHABLE.remove(player.getUUID());
			return false;
		}
		return true;
	}
}
