package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The crossroads: where a swordsman chooses their Way (the rules are {@link WayRules}). As the Edge breakthrough settles (or when a
 * swordsman past it with no Way holds the breathing stance a few seconds, or burns a Crossroads Incense at a place of power), a
 * standard of light rises for each Way on an arc in front of them: the Blade a sword planted point-down, the Bulwark a shield on its
 * pole, the Shadowstep a pillar of shadow trailing afterimages under a crescent, the Banner a pennant on its pole. The swordsman
 * strikes the one they mean to walk: the first strike leans toward it (it flares, the others dim, and what it gives is shown), a
 * second strike on it within a few seconds walks it. The chosen standard pours into them; the others shatter.
 *
 * <p>Everything is the server's: where the standards stand is decided here and kept on the swordsman ({@link #CROSSROADS}, told to
 * their client alone, which only names the standard under the crosshair), each strike is a swing the server saw ({@link #swung},
 * from the punch every swing sends), tested against the standards along the swordsman's look. The light is sent to everyone near:
 * others see a crossroads rise round them too.</p>
 */
public final class Crossroads {
	private Crossroads() {}

	/** Why a crossroads rose. */
	public enum Reason { BREAKTHROUGH, CALLED, INCENSE, API }

	/** One standard: the Way it stands for and where its foot is. */
	public record Standard(String way, Vec3 foot) {
		public static final StreamCodec<ByteBuf, Standard> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Standard::way, Vec3.STREAM_CODEC, Standard::foot, Standard::new);
	}

	/**
	 * A crossroads standing round its swordsman, in the server's game time.
	 *
	 * @param centre    where it rose (where its swordsman stood)
	 * @param standards one for each Way, left to right as they faced
	 * @param opened    when it rose
	 * @param until     when it fades unchosen
	 * @param leaning   the Way leaned toward by a first strike ("" for none)
	 * @param leanUntil when that lean lets go, unless struck again
	 * @param reason    why it rose ({@link Reason}'s ordinal)
	 */
	public record State(Vec3 centre, List<Standard> standards, long opened, long until, String leaning, long leanUntil, int reason) {
		public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			Vec3.STREAM_CODEC, State::centre, Standard.STREAM_CODEC.apply(ByteBufCodecs.list(8)), State::standards, ByteBufCodecs.VAR_LONG, State::opened,
			ByteBufCodecs.VAR_LONG, State::until, ByteBufCodecs.stringUtf8(128), State::leaning, ByteBufCodecs.VAR_LONG, State::leanUntil,
			ByteBufCodecs.VAR_INT, State::reason, State::new);

		public State {
			standards = List.copyOf(standards);
			leaning = leaning == null ? "" : leaning;
		}

		/** Whether it still stands at {@code now}. */
		public boolean standing(long now) {
			return now < until;
		}

		/** The Way leaned toward at {@code now}, or "". */
		public String leaningAt(long now) {
			return !leaning.isEmpty() && now < leanUntil ? leaning : "";
		}

		State lean(String way, long until) {
			return new State(centre, standards, opened, this.until, way, until, reason);
		}
	}

	/** A swordsman's crossroads while it stands: not saved (a moment, not a path), told to its swordsman alone. */
	public static final AttachmentType<State> CROSSROADS = AttachmentRegistry.create(
		Wildercord.id("aura_crossroads"),
		builder -> builder.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Every swordsman with a crossroads standing, for the upkeep. */
	private static final Set<UUID> STANDING = new HashSet<>();
	/** When each swordsman's stance may call another (after one faded unchosen). */
	private static final Map<UUID, Long> REST = new HashMap<>();
	/** The most standards one crossroads raises. */
	public static final int MOST = 6;

	// ------------------------------------------------------------------ reading (both sides)

	/** {@code player}'s crossroads, if one stands now. */
	public static State state(Player player) {
		State s = player.getAttached(CROSSROADS);
		return s != null && s.standing(player.level().getGameTime()) ? s : null;
	}

	/** Whether a crossroads stands round {@code player} now. */
	public static boolean standing(Player player) {
		return state(player) != null;
	}

	/**
	 * The standard {@code player}'s look meets now (within reach; of two, the one nearer the line of the look), or null. Both sides: the
	 * server for a strike, the swordsman's client to name the one under the crosshair. Blocks in the way aren't checked here (see
	 * {@link #seen}).
	 */
	public static Standard aimed(Player player, State s) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		double[] e = {eye.x, eye.y, eye.z};
		double[] l = {look.x, look.y, look.z};
		Standard best = null;
		double nearest = Double.MAX_VALUE;
		for (Standard standard : s.standards()) {
			double[] hit = WayRules.strike(e, l, new double[] {standard.foot().x, standard.foot().y, standard.foot().z});
			if (hit != null && hit[1] < nearest) {
				nearest = hit[1];
				best = standard;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ rising (server)

	/**
	 * Raises the crossroads round {@code player}: Ways on, Edge or above, walking no Way, none standing already, and room round them
	 * for every standard. Says why not where it matters. Returns whether it rose.
	 */
	public static boolean open(ServerPlayer player, Reason reason) {
		if (!player.isAlive() || player.isSpectator() || !Ways.on(player) || Aura.stage(player) < WayRules.FROM || Ways.way(player).isPresent()
				|| standing(player)) {
			return false;
		}
		List<AuraApi.Way> ways = AuraApi.ways();
		if (ways.isEmpty()) {
			return false;
		}
		ways = ways.subList(0, Math.min(MOST, ways.size()));
		List<Standard> placed = place(player, ways);
		if (placed == null) {
			player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.no_room").withColor(0xC8A0A0));
			REST.put(player.getUUID(), player.level().getGameTime() + WayRules.CALL_REST);
			return false;
		}
		long now = player.level().getGameTime();
		State s = new State(player.position(), placed, now, now + WayRules.CROSSROADS_TICKS, "", 0, reason.ordinal());
		player.setAttached(CROSSROADS, s);
		STANDING.add(player.getUUID());
		REST.remove(player.getUUID());
		// The moment: a low chord that holds every Way's note, the standards rising out of the ground, and the swordsman told.
		ServerLevel level = player.level();
		Feels.sound(level, player.position().add(0, 1, 0), "aura_crossroads", 1.0F, 1.0F);
		AuraFx.banner(player, Component.translatable("aura.wildercord.crossroads"), Component.translatable("aura.wildercord.crossroads.kicker"),
			0xE8D8B0, AuraFxRules.BannerKind.ART);
		AuraFx.bodyAuraFlare(player, 30, 0.5F);
		for (Standard standard : placed) {
			int color = color(standard.way());
			ArtLight.world(player).groundRing(standard.foot(), color, 0.1, 0.9, 0.07, 12)
				.ray(standard.foot(), standard.foot().add(0, WayRules.STANDARD_HEIGHT + 0.4, 0), AuraVfx.hot(color, 0.5), 0.05, 10);
		}
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.crossroads").withColor(0xE8D8B0));
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.crossroads_how").withColor(0xB8A8D8));
		Grimoire.unlock(player, "aura:crossroads");
		return true;
	}

	/**
	 * Where the standards stand: on an arc in front of the swordsman, left to right, each on the ground (a block's top within a couple
	 * of blocks of their feet, with room above it) and in plain sight of their eyes; nearer where there's no room, or failing that
	 * anywhere round them. Null when not every Way finds a place.
	 */
	static List<Standard> place(ServerPlayer player, List<AuraApi.Way> ways) {
		int n = ways.size();
		double facing = Math.toRadians(player.getYRot());
		for (double radius : WayRules.RADII) {
			List<Standard> out = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				Vec3 foot = ground(player, facing + Math.toRadians(WayRules.angle(i, n)), radius);
				if (foot == null) {
					break;
				}
				out.add(new Standard(ways.get(i).id(), foot));
			}
			if (out.size() == n) {
				return out;
			}
		}
		// Cramped in front: anywhere round the swordsman, in order, starting from their right.
		for (double radius : WayRules.RADII) {
			List<Standard> out = new ArrayList<>();
			for (int k = 0; k < 12 && out.size() < n; k++) {
				Vec3 foot = ground(player, facing + Math.PI * 2 * k / 12, radius);
				if (foot != null && out.stream().noneMatch(st -> st.foot().distanceTo(foot) < 1.2)) {
					out.add(new Standard(ways.get(out.size()).id(), foot));
				}
			}
			if (out.size() == n) {
				return out;
			}
		}
		return null;
	}

	/** The ground a standard can stand on {@code radius} out at {@code angle} (radians, as the swordsman's yaw), or null. */
	private static Vec3 ground(ServerPlayer player, double angle, double radius) {
		ServerLevel level = player.level();
		Vec3 base = player.position();
		// A yaw of 0 faces +z; the angle grows clockwise seen from above (to the swordsman's right).
		double x = base.x - Math.sin(angle) * radius;
		double z = base.z + Math.cos(angle) * radius;
		int top = (int) Math.floor(base.y) + 2;
		for (int y = top; y >= top - 5; y--) {
			BlockPos pos = BlockPos.containing(x, y, z);
			BlockPos below = pos.below();
			if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
				continue;
			}
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
					|| !level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty()) {
				return null;
			}
			double floor = below.getY() + level.getBlockState(below).getCollisionShape(level, below).max(net.minecraft.core.Direction.Axis.Y);
			Vec3 foot = new Vec3(x, floor, z);
			return seen(player, foot.add(0, 1.3, 0)) ? foot : null;
		}
		return null;
	}

	/** Whether {@code at} is in plain sight of {@code player}'s eyes (no block in between). */
	static boolean seen(ServerPlayer player, Vec3 at) {
		HitResult hit = player.level().clip(new ClipContext(player.getEyePosition(), at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.MISS;
	}

	// ------------------------------------------------------------------ the breathing stance calls it

	/**
	 * Every tick in the breathing stance ({@code Aura}): a swordsman past the crossroads with no Way, breathing {@link WayRules#CALL_TICKS}
	 * since the stance settled (and no stillness trial under way), calls it.
	 */
	static void breathing(ServerPlayer player, AuraAttachments.State state, long now) {
		if (state.stillness() > 0 || now - state.settledAt() < WayRules.CALL_TICKS || !Ways.wayless(player) || standing(player)
				|| BladeCeremony.busy(player)) {
			return;
		}
		Long rest = REST.get(player.getUUID());
		if (rest != null && now < rest) {
			return;
		}
		open(player, Reason.CALLED);
	}

	/** The Edge breakthrough was just made: the crossroads rises once its title has been read. */
	static void brokeThrough(ServerPlayer player) {
		if (!Ways.wayless(player)) {
			return;
		}
		Scheduler.later(WayRules.OPEN_DELAY, () -> {
			if (player.isAlive() && !player.hasDisconnected()) {
				open(player, Reason.BREAKTHROUGH);
			}
		});
	}

	// ------------------------------------------------------------------ striking a standard

	/** A swing {@code player} made (the punch every swing sends): if it meets a standard of their crossroads, it strikes it. */
	public static void swung(ServerPlayer player) {
		State s = state(player);
		if (s == null) {
			return;
		}
		Standard standard = aimed(player, s);
		if (standard == null || !seen(player, standard.foot().add(0, Math.min(WayRules.STANDARD_HEIGHT - 0.2, Math.max(0.3,
				player.getEyeY() - standard.foot().y)), 0))) {
			return;
		}
		if (!Aura.holdsWeapon(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.way.with_blade").withColor(0xA89CC8));
			return;
		}
		long now = player.level().getGameTime();
		if (standard.way().equals(s.leaningAt(now))) {
			choose(player, s, standard);
		} else {
			lean(player, s, standard, now);
		}
	}

	/** A first strike: the standard answers and the others dim; what the Way gives is told, and a second strike walks it. */
	private static void lean(ServerPlayer player, State s, Standard standard, long now) {
		AuraApi.Way way = AuraApi.way(standard.way()).orElse(null);
		if (way == null) {
			return;
		}
		player.setAttached(CROSSROADS, s.lean(way.id(), now + WayRules.LEAN_TICKS));
		int color = way.color();
		int index = s.standards().indexOf(standard);
		Vec3 head = standard.foot().add(0, WayRules.STANDARD_HEIGHT, 0);
		// It flares: a great flash at its head for everyone else (and in third person), a small one through the swordsman's own eyes.
		ArtLight.spectacle(player).flash(head, AuraVfx.hot(color, 0.4), 1.6F);
		ArtLight.world(player).flash(head, AuraVfx.hot(color, 0.4), 0.6F).groundRing(standard.foot(), color, 0.2, 1.4, 0.08, 14)
			.ray(standard.foot(), head.add(0, 0.6, 0), AuraVfx.hot(color, 0.6), 0.07, 10);
		Feels.sound(player.level(), head, "aura_way_lean", 1.0F, Feels.step(index));
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.way.lean", Component.translatable(way.nameKey())
			.withColor(0xFF000000 | color)).withColor(0xE8D8B0));
	}

	/** The second strike: the Way is walked, its standard pours into the swordsman, the others shatter. */
	private static void choose(ServerPlayer player, State s, Standard chosen) {
		AuraApi.Way way = AuraApi.way(chosen.way()).orElse(null);
		if (way == null || !Ways.choose(player, way.id())) {
			return;
		}
		close(player, false);
		ServerLevel level = player.level();
		int color = way.color();
		Vec3 chest = player.position().add(0, 1.1, 0);
		Vec3 heart = chosen.foot().add(0, 1.4, 0);
		// The chosen standard pours into its swordsman over a few moments, sinking as it goes (seen from outside: its light streaming
		// into the body; through their own eyes, the standard flaring out where it stood and a whisper of light low in the view).
		Vec3 foot = chosen.foot();
		for (int k = 0; k < 4; k++) {
			int step = k;
			Scheduler.later(step * 2, () -> {
				if (!player.isAlive() || player.level() != level) {
					return;
				}
				double top = WayRules.STANDARD_HEIGHT * (1 - 0.22 * step);
				Vec3 body = player.position().add(0, 1.1, 0);
				ArtLight.world(player).ray(foot, foot.add(0, top, 0), AuraVfx.hot(color, 0.5 + 0.1 * step), 0.09, 6);
				for (int i = 0; i < 3; i++) {
					Vec3 from = foot.add(0, top * (0.35 + 0.3 * i), 0);
					ArtLight.spectacle(player).ray(from, body.add(0, (i - 1) * 0.25, 0), AuraVfx.hot(color, 0.35 + 0.1 * i), 0.1 - 0.02 * step, 7);
				}
				Motes.fling(level, foot.add(0, top * 0.6, 0), body.subtract(foot.add(0, top * 0.6, 0)).normalize(), 0.35, AuraVfx.hot(color, 0.3), 0.12, 12,
					Vec3.ZERO);
			});
		}
		ArtLight.world(player).flash(heart, AuraVfx.hot(color, 0.5), 1.4F).groundRing(chosen.foot(), color, 0.2, 2.2, 0.1, 18);
		AuraFx.burst(level, player, chest, Vec3.ZERO, color, 1.8F, AuraFx.Burst.FLASH | AuraFx.Burst.RING | AuraFx.Burst.STAR);
		AuraFx.burst(level, player, player.position().add(0, 0.08, 0), new Vec3(0, 1, 0), color, 2.6F, AuraFx.Burst.RING | AuraFx.Burst.ECHO);
		AuraFx.bodyAuraFlare(player, 60, 1.0F);
		// The others break apart.
		for (Standard other : s.standards()) {
			if (other == chosen) {
				continue;
			}
			int c = color(other.way());
			Vec3 mid = other.foot().add(0, 1.3, 0);
			ArtLight.world(player).shards(mid, 1.1, 8, c, AuraVfx.hot(c, 0.5)).flash(mid, c, 1.0F);
			Motes.burst(level, mid, 10, c, 0.12, 22, 0.1);
		}
		Feels.sound(level, chest, "aura_way_chosen", 1.0F, 1.0F);
		Feels.sound(level, chest, voice(way.id()), 1.0F, 1.0F);
		AuraFx.banner(player, Component.translatable(way.nameKey()), Component.translatable("aura.wildercord.way.kicker"), color,
			AuraFxRules.BannerKind.GRAND);
		// What it gives now, said once in the chat: the creed, and each node reached.
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.chosen", Component.translatable(way.nameKey())
			.withColor(0xFF000000 | color)).withColor(0xE8D8B0));
		player.sendSystemMessage(Component.translatable(way.creedKey()).withStyle(net.minecraft.ChatFormatting.ITALIC).withColor(0xB8A8D8));
		int stage = Aura.stage(player);
		Ways.State ws = Ways.state(player);
		for (AuraApi.WayNode node : way.nodes()) {
			Component name = Component.translatable(node.nameKey()).withColor(0xFF000000 | color);
			Component stageName = Component.translatable("aura.wildercord.stage." + AuraStages.id(node.stage()));
			String key = stage < node.stage() ? "message.wildercord.aura.way.node_later"
				: WayRules.awake(node.stage(), ws.owed(), ws.settle()) ? "message.wildercord.aura.way.node_now" : "message.wildercord.aura.way.node_waking";
			player.sendSystemMessage(Component.translatable(key, name, stageName).withColor(0xC8C0D8));
		}
	}

	/** Closes {@code player}'s crossroads: chosen, or ({@code faded}) let go unchosen, the stance able to call it again after a rest. */
	static void close(ServerPlayer player, boolean faded) {
		State s = player.getAttached(CROSSROADS);
		player.removeAttached(CROSSROADS);
		STANDING.remove(player.getUUID());
		if (s == null || !faded) {
			return;
		}
		REST.put(player.getUUID(), player.level().getGameTime() + WayRules.CALL_REST);
		for (Standard standard : s.standards()) {
			int c = color(standard.way());
			Vec3 mid = standard.foot().add(0, 1.2, 0);
			Motes.burst(player.level(), mid, 6, AuraVfx.hot(c, 0.2), 0.06, 26, 0.05);
		}
		if (player.isAlive() && Ways.wayless(player)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.aura.way.faded").withColor(0xB8A8D8));
		}
	}

	// ------------------------------------------------------------------ the standards' light

	/** How often the standards are drawn again (ticks), and how long each drawing lasts (overlapping, so they burn steadily). */
	private static final int PERIOD = 4;
	private static final int LIFE = 9;

	private static void tick(MinecraftServer server) {
		if (STANDING.isEmpty()) {
			return;
		}
		for (UUID id : List.copyOf(STANDING)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				STANDING.remove(id);
				continue;
			}
			State s = player.getAttached(CROSSROADS);
			long now = player.level().getGameTime();
			if (s == null) {
				STANDING.remove(id);
				continue;
			}
			if (!s.standing(now) || !player.isAlive() || player.position().distanceTo(s.centre()) > WayRules.LEAVE || Ways.way(player).isPresent()
					|| !Ways.on(player)) {
				close(player, true);
				continue;
			}
			long age = now - s.opened();
			if (age % PERIOD == 0) {
				draw(player, s, age, now);
			}
		}
	}

	/** Draws every standard of {@code s}, rising for its first moments, the leaned one brighter and the rest dimmer. */
	private static void draw(ServerPlayer player, State s, long age, long now) {
		String leaning = s.leaningAt(now);
		float rise = (float) Math.min(1.0, (age + 2) / 12.0);
		long left = s.until() - now;
		for (Standard standard : s.standards()) {
			float emphasis = leaning.isEmpty() ? 1.0F : standard.way().equals(leaning) ? 1.6F : 0.45F;
			if (left < 60) {
				// Its last three seconds: the light thins as it fades.
				emphasis *= (float) Math.max(0.3, left / 60.0);
			}
			look(player, s, standard, rise, emphasis, age);
		}
	}

	/** One standard's light: each built-in Way's shape, an add-on's a plain pillar in its colour. */
	private static void look(ServerPlayer player, State s, Standard standard, float rise, float emphasis, long age) {
		int base = color(standard.way());
		int color = emphasis < 1 ? AuraRules.mix(base, 0x2A2438, 0.5) : emphasis > 1 ? AuraVfx.hot(base, 0.25) : base;
		int core = AuraVfx.hot(color, 0.55);
		ArtLight light = ArtLight.world(player);
		Vec3 foot = standard.foot();
		Vec3 up = new Vec3(0, 1, 0);
		// Square to the way from the crossroads' heart to the standard: the standard's own left-right, as its swordsman sees it.
		Vec3 out = foot.subtract(s.centre());
		out = new Vec3(out.x, 0, out.z);
		out = out.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : out.normalize();
		Vec3 side = new Vec3(-out.z, 0, out.x);
		double h = WayRules.STANDARD_HEIGHT * rise;
		double w = emphasis > 1 ? 1.45 : 1.0;
		light.ground(foot, SigilOption.RING, color, 0.5 * (emphasis > 1 ? 1.25 : 1.0), LIFE + 2, 0.04);
		switch (standard.way()) {
			case WayRules.BLADE -> {
				// A sword of light planted point-down: the blade, the guard across it, the grip and a bright pommel.
				double guard = 1.72 * rise;
				light.ray(foot.add(0, 0.02, 0), foot.add(0, guard, 0), core, 0.065 * w, LIFE);
				if (rise >= 1) {
					light.ray(at(foot, side, -0.42, guard), at(foot, side, 0.42, guard), color, 0.06 * w, LIFE)
						.ray(foot.add(0, guard, 0), foot.add(0, guard + 0.42, 0), AuraRules.mix(color, 0x3A2420, 0.35), 0.045 * w, LIFE)
						.flash(foot.add(0, guard + 0.5, 0), core, (float) (0.4 * w));
				}
			}
			case WayRules.BULWARK -> {
				// A kite shield of light on its pole: its rim, a cross on its face.
				light.ray(foot, foot.add(0, h, 0), AuraRules.mix(color, 0x203040, 0.3), 0.035 * w, LIFE);
				if (rise >= 1) {
					Vec3 tl = at(foot, side, -0.42, 1.95);
					Vec3 tr = at(foot, side, 0.42, 1.95);
					Vec3 ml = at(foot, side, -0.42, 1.42);
					Vec3 mr = at(foot, side, 0.42, 1.42);
					Vec3 tip = at(foot, side, 0, 0.82);
					double rim = 0.055 * w;
					light.ray(tl, tr, core, rim, LIFE).ray(tl, ml, core, rim, LIFE).ray(tr, mr, core, rim, LIFE).ray(ml, tip, core, rim, LIFE)
						.ray(mr, tip, core, rim, LIFE).ray(at(foot, side, 0, 1.88), at(foot, side, 0, 0.98), color, 0.04 * w, LIFE)
						.ray(at(foot, side, -0.36, 1.6), at(foot, side, 0.36, 1.6), color, 0.04 * w, LIFE);
				}
			}
			case WayRules.SHADOWSTEP -> {
				// A pillar of shadow with a thin bright heart, afterimages trailing off it, a crescent over it.
				light.bare().ray(foot, foot.add(0, h * 0.92, 0), color | ArtLight.DARK, 0.12 * w, LIFE);
				light.ray(foot, foot.add(0, h * 0.92, 0), core, 0.022 * w, LIFE);
				if (rise >= 1) {
					for (int i = 1; i <= 3; i++) {
						double off = -0.26 * i;
						int ghost = AuraRules.mix(color, 0x1A1424, 0.2 + 0.2 * i);
						light.ray(at(foot, side, off, 0.15), at(foot, side, off, 1.85 - 0.18 * i), ghost, 0.035 * w, LIFE);
					}
					Vec3 moon = foot.add(0, 2.25, 0);
					light.slash(moon, out.scale(-1), side.scale(-0.6).add(up.scale(0.8)), core, 0.36, 2.4, 0.07 * w, 0, LIFE);
				}
			}
			case WayRules.BANNER -> {
				// A pennant of light on its pole, stirring as if in a wind, flying in toward the middle of the arc (an outer standard's
				// would fly out of a first-person view).
				double pole = 2.75 * rise;
				Vec3 toMiddle = s.standards().get(s.standards().size() / 2).foot().subtract(foot);
				if (toMiddle.x * side.x + toMiddle.z * side.z < 0) {
					side = side.scale(-1);
				}
				light.ray(foot, foot.add(0, pole, 0), AuraRules.mix(color, 0x403018, 0.25), 0.04 * w, LIFE);
				if (rise >= 1) {
					double stir = Math.sin(age * 0.35) * 0.07;
					Vec3 top = foot.add(0, 2.68, 0);
					Vec3 bottom = foot.add(0, 2.06, 0);
					Vec3 tip1 = at(foot, side, 0.92, 2.6).add(out.scale(stir));
					Vec3 notch = at(foot, side, 0.64, 2.36).add(out.scale(stir * 0.6));
					Vec3 tip2 = at(foot, side, 0.92, 2.13).add(out.scale(stir));
					double edge = 0.045 * w;
					light.ray(top, tip1, core, edge, LIFE).ray(tip1, notch, core, edge, LIFE).ray(notch, tip2, core, edge, LIFE).ray(tip2, bottom, core, edge, LIFE);
					for (int i = 0; i < 3; i++) {
						double y = 2.55 - 0.17 * i;
						light.ray(foot.add(0, y, 0), at(foot, side, 0.66 - 0.08 * Math.abs(i - 1), y + 0.01).add(out.scale(stir * 0.7)), color, 0.07 * w, LIFE);
					}
					light.flash(foot.add(0, 2.86, 0), core, (float) (0.35 * w));
				}
			}
			default -> {
				light.ray(foot, foot.add(0, h, 0), core, 0.06 * w, LIFE);
				if (rise >= 1) {
					light.flash(foot.add(0, h + 0.1, 0), core, (float) (0.5 * w));
				}
			}
		}
		if (emphasis > 1 && age % (PERIOD * 4) == 0) {
			// Leaned toward: rings rising round it now and then.
			light.groundRing(foot, base, 0.25, 1.1, 0.05, 12);
		}
	}

	private static Vec3 at(Vec3 foot, Vec3 side, double across, double up) {
		return foot.add(side.scale(across)).add(0, up, 0);
	}

	/** A Way's colour, by id (an add-on's own, or a pale gold for one unknown). */
	static int color(String wayId) {
		return AuraApi.way(wayId).map(AuraApi.Way::color).orElse(0xE8D8B0);
	}

	/** Each built-in Way's own voice as it's chosen (over {@code aura_way_chosen}); an add-on's Way rings plain steel. */
	public static String voice(String wayId) {
		return WayRules.voice(wayId);
	}

	// ------------------------------------------------------------------ lifecycle

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Crossroads::tick);
		// A death, a change of world or leaving closes it (the stance can call it again later). A death's is closed after Fabric has
		// copied the new body's attachments over (Aura.AFTER_COPY); this one isn't copied, but the upkeep's list is the server's.
		ServerPlayerEvents.AFTER_RESPAWN.register(Aura.AFTER_COPY, (oldPlayer, newPlayer, alive) -> {
			STANDING.remove(oldPlayer.getUUID());
			newPlayer.removeAttached(CROSSROADS);
		});
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> {
			if (player.hasAttached(CROSSROADS)) {
				close(player, true);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			STANDING.remove(handler.player.getUUID());
			REST.remove(handler.player.getUUID());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STANDING.clear();
			REST.clear();
		});
	}
}
