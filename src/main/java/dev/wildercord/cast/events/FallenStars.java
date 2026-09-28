package dev.wildercord.cast.events;

import com.mojang.math.Transformation;
import dev.wildercord.cast.BlockFx;
import dev.wildercord.cast.Casters;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Starfall craters. At night a star now and then streaks down somewhere 60 to 150 blocks from a
 * player and lands with a boom. Where the ground may be changed (the mobGriefing rule is on and a
 * player could dig there) it blasts a small, shallow crater out of the natural ground; otherwise it
 * only scorches it. A Fallen Star sits in the middle holding a Tier III (sometimes Tier IV) rune and a
 * Mana Crystal, and a column of light marks it for five minutes so players can race to it. When
 * someone comes close, two to four Runebound (one an Adept) rise to guard it.
 *
 * <p>It opens only once every guard is dead: one led far off, or out of loaded ground, still stands;
 * one that went without being killed (sent away by Peaceful, say) is replaced by a fresh one the
 * next time someone comes near, and it won't open on Peaceful at all. Nor does a restart let anyone
 * loot it: its guards are sent away as they load, so a star found guarded after one wakes new ones.</p>
 *
 * <p>Everything is temporary: once looted (or after twenty minutes) the star crumbles, its guards
 * go, and the crater fills back in, block for block. The crater's blocks are saved with the star,
 * so a restart can't leave it behind; guards and scorch marks are removed as their chunks load.
 * Where it lies is noted in {@link EventLedger} too, so no second star falls in its world while it
 * lies there, even across a restart.</p>
 */
public final class FallenStars {
	private FallenStars() {}

	private static final int STAR_LIGHT = 0xD8E4FF;
	private static final int STAR_GLOW = 0x9AB4FF;
	/** Ticks the star takes to fall. */
	private static final int FALL_TICKS = 50;

	private record Key(ResourceKey<Level> level, BlockPos pos) {}

	/** Stars falling or fallen this session, with their guards (and where each was last seen) and scorch marks. */
	private static final class Star {
		final Map<UUID, BlockPos> guards = new LinkedHashMap<>();
		final List<Display> scorch = new ArrayList<>();
		/** Guards that went without being killed, to be replaced the next time someone comes near. */
		int owed;
	}

	private static final Map<Key, Star> STARS = new HashMap<>();

	static void clear() {
		STARS.clear();
	}

	/** Whether a star is falling or lying in this world right now (one from before a restart too). */
	public static boolean any(ServerLevel level) {
		for (Key key : STARS.keySet()) {
			if (key.level() == level.dimension()) {
				return true;
			}
		}
		return EventLedger.of(level.getServer()).starLying(level.dimension(), level.getGameTime());
	}

	/** One of an event's monsters was killed: if it guarded a star, it no longer stands. */
	static void guardKilled(UUID id) {
		for (Star star : STARS.values()) {
			star.guards.remove(id);
		}
	}

	/** The guards of the star at {@code pos} (for the tests). */
	public static List<Mob> guards(ServerLevel level, BlockPos pos) {
		List<Mob> mobs = new ArrayList<>();
		Star star = STARS.get(new Key(level.dimension(), pos));
		if (star != null) {
			for (UUID id : star.guards.keySet()) {
				if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
					mobs.add(mob);
				}
			}
		}
		return mobs;
	}

	// ------------------------------------------------------------------ falling

	/**
	 * Sends a star down near {@code by}: 60 to 150 blocks away, or, {@code here}, a few blocks in
	 * front of them. Returns where it will land (it takes a couple of seconds), or null if there was
	 * nowhere fit to land: the ground must be loaded, solid, dry and somewhere {@code by} could build.
	 */
	public static BlockPos fall(ServerLevel level, ServerPlayer by, boolean here) {
		BlockPos land = here ? landingHere(level, by) : landingNear(level, by);
		if (land == null) {
			return null;
		}
		STARS.put(new Key(level.dimension(), land), new Star());
		EventLedger.of(level.getServer()).starFell(level.dimension(), land, level.getGameTime() + FALL_TICKS + EventRules.STAR_LIFETIME + 100);
		RandomSource random = level.getRandom();
		Vec3 ground = Vec3.atBottomCenterOf(land);
		double a = random.nextDouble() * Math.PI * 2;
		Vec3 start = ground.add(Math.cos(a) * 70, 110, Math.sin(a) * 70);
		Vec3 path = ground.subtract(start);
		WorldEvents.farSound(level, start.lerp(ground, 0.5), EventSounds.STAR_FALL, 220, 1.0F);
		for (ServerPlayer player : level.players()) {
			double d = player.position().distanceTo(ground);
			if (d <= 220 && d > 12) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.star_falls", WorldEvents.direction(player.position(), ground))
					.withColor(STAR_LIGHT));
			}
		}
		for (int t = 0; t < FALL_TICKS; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				// It speeds up as it falls.
				double f = Math.pow((tick + 1) / (double) FALL_TICKS, 1.6);
				double g = Math.pow(Math.max(0, tick - 3) / (double) FALL_TICKS, 1.6);
				Vec3 p = start.add(path.scale(f));
				Vec3 back = start.add(path.scale(g));
				WorldEvents.far(level, new LightOption(LightOption.ORB, STAR_LIGHT, 0.7F, 0, 0, 0.02F, 0, 0, 0, 3), p);
				Vec3 d = p.subtract(back);
				WorldEvents.far(level, new LightOption(LightOption.RAY, STAR_GLOW, (float) d.x, (float) d.y, (float) d.z, 0.45F, 0, 0, 0, 14), back);
				if (tick % 3 == 0) {
					WorldEvents.far(level, SigilOption.glow(STAR_LIGHT, 3.0F), p);
				}
			});
		}
		Scheduler.later(FALL_TICKS + 1, () -> land(level, land, by));
		return land;
	}

	/** Somewhere 60 to 150 blocks from the player, on loaded, solid, dry ground they could build on. */
	private static BlockPos landingNear(ServerLevel level, ServerPlayer by) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < 12; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = EventRules.STAR_MIN_DISTANCE + random.nextDouble() * (EventRules.STAR_MAX_DISTANCE - EventRules.STAR_MIN_DISTANCE);
			BlockPos land = fit(level, by, (int) Math.floor(by.getX() + Math.cos(a) * r), (int) Math.floor(by.getZ() + Math.sin(a) * r));
			if (land != null) {
				return land;
			}
		}
		return null;
	}

	/** A few blocks in front of the player (the command's {@code here}). */
	private static BlockPos landingHere(ServerLevel level, ServerPlayer by) {
		Vec3 look = by.getLookAngle().multiply(1, 0, 1);
		look = look.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.normalize();
		for (int d = 9; d <= 16; d++) {
			BlockPos land = fit(level, by, (int) Math.floor(by.getX() + look.x * d), (int) Math.floor(by.getZ() + look.z * d));
			if (land != null) {
				return land;
			}
		}
		return null;
	}

	/** The cell a star would lie in at (x, z), or null if it can't land there. */
	private static BlockPos fit(ServerLevel level, ServerPlayer by, int x, int z) {
		BlockPos column = new BlockPos(x, level.getSeaLevel(), z);
		if (!level.isLoaded(column) || !level.isLoaded(column.offset(8, 0, 8)) || !level.isLoaded(column.offset(-8, 0, -8))
				|| !level.isLoaded(column.offset(8, 0, -8)) || !level.isLoaded(column.offset(-8, 0, 8))) {
			return null;
		}
		int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos surface = new BlockPos(x, h - 1, z);
		BlockState ground = level.getBlockState(surface);
		if (!ground.isSolidRender() || ground.is(BlockTags.LEAVES) || !level.getFluidState(surface).isEmpty()) {
			return null;
		}
		// Keep clear of water and lava: a crater beside them would flood.
		for (BlockPos p : BlockPos.betweenClosed(surface.offset(-5, -2, -5), surface.offset(5, 1, 5))) {
			if (!level.getFluidState(p).isEmpty()) {
				return null;
			}
		}
		BlockPos cell = surface.above();
		return level.getBlockState(cell).canBeReplaced() && Casters.mayEdit(by, level, cell) ? cell.immutable() : null;
	}

	/** It lands: a boom, a flash and a shockwave, the crater (or scorch marks), and the star itself. */
	private static void land(ServerLevel level, BlockPos cell, ServerPlayer by) {
		Key key = new Key(level.dimension(), cell);
		Star star = STARS.get(key);
		if (star == null || !level.isLoaded(cell)) {
			// Its ground went unloaded while it fell: it burns up instead.
			STARS.remove(key);
			EventLedger.of(level.getServer()).starGone(level.dimension(), cell);
			return;
		}
		Vec3 ground = Vec3.atBottomCenterOf(cell);
		WorldEvents.farSound(level, ground, EventSounds.STAR_IMPACT, 240, 1.0F);
		ScreenFx.shake(level, ground, 0.9F, 48);
		WorldEvents.far(level, SigilOption.glow(STAR_LIGHT, 9.0F), ground.add(0, 1.2, 0));
		Light.groundRing(level, ground, STAR_GLOW, 0.5, 11.0, 0.3, 16);
		Light.groundRing(level, ground, STAR_LIGHT, 0.3, 7.0, 0.18, 12);
		Fx.send(level, ParticleTypes.EXPLOSION_EMITTER, ground.x, ground.y + 0.5, ground.z, 1, 0, 0, 0, 0);
		Fx.send(level, ParticleTypes.END_ROD, ground.x, ground.y + 1, ground.z, 30, 1.2, 1.0, 1.2, 0.25);
		Motes.clouds(level, ground.add(0, 0.6, 0), 12, 1.4, 0x9A948C, 2.4, 60, new Vec3(0, 0.03, 0), 0.05, 0.4);
		boolean dig = by != null && !by.isRemoved() && level.getGameRules().get(GameRules.MOB_GRIEFING);
		List<FallenStarBlockEntity.Changed> changed = dig ? carve(level, cell, by) : List.of();
		BlockPos at = cell;
		if (!changed.isEmpty()) {
			// The star sits on the floor of its crater.
			at = floorOf(level, cell);
		} else {
			scorch(level, cell, star);
		}
		if (!level.getBlockState(at).canBeReplaced()) {
			at = cell;
		}
		if (!at.equals(cell)) {
			STARS.remove(key);
			key = new Key(level.dimension(), at);
			STARS.put(key, star);
			EventLedger ledger = EventLedger.of(level.getServer());
			ledger.starGone(level.dimension(), cell);
			ledger.starFell(level.dimension(), at, level.getGameTime() + EventRules.STAR_LIFETIME + 100);
		}
		level.setBlock(at, EventContent.FALLEN_STAR.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(at) instanceof FallenStarBlockEntity entity) {
			RandomSource random = level.getRandom();
			RuneDef rune = EventRules.rewardRune("starfall", EventRules.starRuneTier(random.nextDouble()), random.nextDouble());
			long now = level.getGameTime();
			entity.rune = rune.id();
			entity.beaconUntil = now + EventRules.STAR_BEACON_TICKS;
			entity.fadeAt = now + EventRules.STAR_LIFETIME;
			entity.crater.addAll(changed);
			entity.setChanged();
		}
	}

	/** Where the crater's floor is under the star's cell (the cell itself if nothing was dug). */
	private static BlockPos floorOf(ServerLevel level, BlockPos cell) {
		BlockPos.MutableBlockPos p = cell.mutable();
		for (int i = 0; i < 4 && level.getBlockState(p.below()).isAir(); i++) {
			p.move(0, -1, 0);
		}
		return p.immutable();
	}

	/**
	 * Blasts a shallow bowl out of the natural ground round the star (dirt, stone, sand, gravel and
	 * the plants on them; never anything built, never a block with contents), and scorches its floor.
	 * Every change is returned so it can be put back.
	 */
	private static List<FallenStarBlockEntity.Changed> carve(ServerLevel level, BlockPos cell, ServerPlayer by) {
		List<FallenStarBlockEntity.Changed> changed = new ArrayList<>();
		RandomSource random = level.getRandom();
		double r2max = EventRules.CRATER_RADIUS * EventRules.CRATER_RADIUS;
		int reach = (int) Math.ceil(EventRules.CRATER_RADIUS);
		int surfaceY = cell.getY() - 1;
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				double r2 = dx * dx + dz * dz;
				if (r2 > r2max) {
					continue;
				}
				int depth = (int) Math.round(2.3 * (1 - r2 / r2max));
				int floor = surfaceY - depth;
				int x = cell.getX() + dx;
				int z = cell.getZ() + dz;
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				if (Math.abs(top - 1 - surfaceY) > 3) {
					continue;
				}
				// From the top down: plants first, then the ground under them.
				for (int y = Math.max(top, surfaceY + 2); y > floor && changed.size() < EventRules.CRATER_MAX_BLOCKS; y--) {
					BlockPos p = new BlockPos(x, y, z);
					BlockState state = level.getBlockState(p);
					if (state.isAir()) {
						continue;
					}
					if (!natural(level, p, state) || !Casters.mayEdit(by, level, p)) {
						break;
					}
					changed.add(new FallenStarBlockEntity.Changed(p, state));
					level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
				// The floor is scorched.
				BlockPos f = new BlockPos(x, floor, z);
				BlockState was = level.getBlockState(f);
				if (depth > 0 && changed.size() < EventRules.CRATER_MAX_BLOCKS && natural(level, f, was) && !was.canBeReplaced() && random.nextFloat() < 0.6F
						&& Casters.mayEdit(by, level, f)) {
					changed.add(new FallenStarBlockEntity.Changed(f, was));
					level.setBlock(f, (random.nextFloat() < 0.7F ? Blocks.BLACKSTONE : Blocks.SMOOTH_BASALT).defaultBlockState(),
						Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
		}
		return changed;
	}

	/** The natural ground a crater may take: earth, stone, sand and gravel, and the plants and snow on them. */
	private static boolean natural(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.hasBlockEntity() || !state.getFluidState().isEmpty() || state.getDestroySpeed(level, pos) < 0) {
			return false;
		}
		return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
			|| state.is(Blocks.SANDSTONE) || state.is(Blocks.SNOW) || state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.FLOWERS)
			|| state.is(BlockTags.SMALL_FLOWERS) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN);
	}

	/** Where the ground mustn't be changed: dark scorch marks laid over it instead (block displays, gone with the star). */
	private static void scorch(ServerLevel level, BlockPos cell, Star star) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < 12; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = i < 3 ? random.nextDouble() * 1.2 : 1.0 + random.nextDouble() * 3.0;
			int x = (int) Math.floor(cell.getX() + 0.5 + Math.cos(a) * r);
			int z = (int) Math.floor(cell.getZ() + 0.5 + Math.sin(a) * r);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (Math.abs(y - cell.getY()) > 2) {
				continue;
			}
			Display.BlockDisplay mark = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
			if (mark == null) {
				continue;
			}
			float size = 0.7F + random.nextFloat() * 0.5F;
			mark.snapTo(x + 0.5, y + 0.005 + i * 0.0015, z + 0.5);
			mark.setBlockState((i % 3 == 0 ? Blocks.BLACKSTONE : Blocks.COAL_BLOCK).defaultBlockState());
			mark.setTransformation(new Transformation(new Vector3f(-size / 2, 0, -size / 2),
				new Quaternionf().rotationY((float) (random.nextDouble() * Math.PI)), new Vector3f(size, 0.01F, size), new Quaternionf()));
			BlockFx.fresh(mark);
			level.addFreshEntity(mark);
			star.scorch.add(mark);
		}
	}

	// ------------------------------------------------------------------ lying there

	/**
	 * The star's memory this session. A star lying since before a restart has none: its guards were
	 * sent away as they loaded, so if it was guarded, new ones wake when someone comes near.
	 */
	private static Star star(ServerLevel level, BlockPos pos, FallenStarBlockEntity entity) {
		Key key = new Key(level.dimension(), pos);
		Star star = STARS.get(key);
		if (star == null) {
			star = new Star();
			STARS.put(key, star);
			if (entity.guarded) {
				entity.guarded = false;
				entity.setChanged();
			}
		}
		return star;
	}

	/**
	 * Keeps track of its guards: where each was last seen, and which went without being killed (not
	 * there, though the ground where it was last seen is loaded), to be replaced.
	 */
	private static void countGuards(ServerLevel level, Star star) {
		for (Iterator<Map.Entry<UUID, BlockPos>> it = star.guards.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, BlockPos> guard = it.next();
			if (level.getEntity(guard.getKey()) instanceof Mob mob && mob.isAlive()) {
				guard.setValue(mob.blockPosition());
			} else if (level.isPositionEntityTicking(guard.getValue())) {
				// Not killed (that would have been heard), and not in unloaded ground: sent away somehow.
				WorldEvents.forget(guard.getKey());
				it.remove();
				star.owed++;
			}
		}
	}

	/** Once a second, from the star's block entity: the beacon, and its guards waking when someone comes near. */
	static void tickStar(ServerLevel level, BlockPos pos, FallenStarBlockEntity entity, long now) {
		Star star = star(level, pos, entity);
		countGuards(level, star);
		Vec3 base = Vec3.atBottomCenterOf(pos);
		if (now < entity.beaconUntil) {
			// A column of starlight, seen from far off.
			WorldEvents.far(level, new LightOption(LightOption.RAY, STAR_GLOW, 0, 96, 0, 0.55F, 0, 0, 0, 30), base.add(0, 0.6, 0));
			WorldEvents.far(level, new LightOption(LightOption.RAY, STAR_LIGHT, 0, 96, 0, 0.16F, 0, 0, 0, 30), base.add(0, 0.6, 0));
			if (now % 60 == 0) {
				Sigils.send(level, SigilOption.flat(SigilOption.STAR, STAR_GLOW, 2.6F, 64, 0.02F), base.add(0, 0.05, 0));
			}
		}
		if (!entity.guarded && level.getNearestPlayer(base.x, base.y, base.z, EventRules.STAR_GUARD_WAKE, false) instanceof ServerPlayer near
				&& !near.isSpectator()) {
			entity.guarded = true;
			entity.setChanged();
			if (level.getDifficulty() != Difficulty.PEACEFUL) {
				wakeGuards(level, pos, near, EventRules.guards(level.getRandom().nextDouble()));
			}
		} else if (entity.guarded && star.owed > 0 && level.getDifficulty() != Difficulty.PEACEFUL
				&& level.getNearestPlayer(base.x, base.y, base.z, EventRules.STAR_GUARD_WAKE, false) instanceof ServerPlayer near && !near.isSpectator()) {
			// Guards that went without being killed are replaced.
			int owed = star.owed;
			star.owed = 0;
			wakeGuards(level, pos, near, owed);
		}
	}

	private static final List<EntityType<? extends Mob>> GUARDS = List.of(EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.ZOMBIE);

	/** {@code count} Runebound (two to four, at first) rise round the star, the first an Adept illager. */
	private static void wakeGuards(ServerLevel level, BlockPos pos, ServerPlayer near, int count) {
		Star star = STARS.computeIfAbsent(new Key(level.dimension(), pos), k -> new Star());
		RandomSource random = level.getRandom();
		double a0 = random.nextDouble() * Math.PI * 2;
		int risen = 0;
		for (int i = 0; i < count; i++) {
			double a = a0 + Math.PI * 2 * i / count;
			Vec3 at = WorldEvents.standingSpot(level, Vec3.atBottomCenterOf(pos).add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
			if (at == null) {
				continue;
			}
			boolean adept = i == 0;
			Mob guard = WorldEvents.spawnRunebound(level, adept ? EntityTypes.VINDICATOR : GUARDS.get(i % GUARDS.size()), at, adept, near);
			if (guard != null) {
				risen++;
				star.guards.put(guard.getUUID(), guard.blockPosition());
				Sigils.ground(level, at, STAR_GLOW, STAR_LIGHT, 1.4F, 30);
				Fx.send(level, ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 10, 0.3, 0.6, 0.3, 0.06);
			}
		}
		// Any that found nowhere to stand come the next time someone is near (unless none could: then it lies unguarded).
		if (risen > 0) {
			star.owed += count - risen;
		}
		Fx.sound(level, Vec3.atCenterOf(pos), SoundEvents.EVOKER_PREPARE_SUMMON, 1.2F, 1.2F);
		near.sendOverlayMessage(Component.translatable("message.wildercord.star_guarded").withColor(STAR_LIGHT));
	}

	// ------------------------------------------------------------------ looting

	/**
	 * A player uses the star: refused on Peaceful, and while any of its guards stands (however far off,
	 * or not yet risen); otherwise it breaks open and crumbles.
	 */
	public static void open(ServerLevel level, BlockPos pos, ServerPlayer player) {
		if (!(level.getBlockEntity(pos) instanceof FallenStarBlockEntity entity)) {
			return;
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.star_peaceful").withColor(0xB8A8D8));
			Fx.sound(level, Vec3.atCenterOf(pos), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.6F);
			return;
		}
		Star star = star(level, pos, entity);
		countGuards(level, star);
		if (!entity.guarded) {
			// Nobody came near it before (or its guards were lost to a restart): they rise now.
			entity.guarded = true;
			entity.setChanged();
			wakeGuards(level, pos, player, EventRules.guards(level.getRandom().nextDouble()));
		} else if (star.owed > 0) {
			int owed = star.owed;
			star.owed = 0;
			wakeGuards(level, pos, player, owed);
		}
		int standing = star.guards.size() + star.owed;
		if (standing > 0) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.star_held", standing).withColor(0xFF9C9C));
			Fx.sound(level, Vec3.atCenterOf(pos), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.6F);
			return;
		}
		Vec3 at = Vec3.atCenterOf(pos);
		RuneDef rune = Runes.get(entity.rune).orElseGet(() -> EventRules.rewardRune("starfall", 3, level.getRandom().nextDouble()));
		drop(level, at, RuneItem.stack(rune));
		drop(level, at, new ItemStack(WildercordItems.MANA_CRYSTAL));
		ExperienceOrb.award(level, at, EventRules.STAR_XP);
		Grimoire.feat(player, Feats.STARGAZER);
		player.sendOverlayMessage(Component.translatable("message.wildercord.star_looted").withColor(STAR_LIGHT));
		crumble(level, pos, entity, true);
	}

	private static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, at.x, at.y + 0.3, at.z, stack);
		item.setDeltaMovement(level.getRandom().nextGaussian() * 0.05, 0.25, level.getRandom().nextGaussian() * 0.05);
		item.setGlowingTag(true);
		level.addFreshEntity(item);
	}

	/**
	 * The star crumbles to dust (looted) or its light goes out (faded): its guards and scorch marks
	 * go, and the crater fills back in, putting back only blocks nobody has built over since.
	 */
	static void crumble(ServerLevel level, BlockPos pos, FallenStarBlockEntity entity, boolean looted) {
		Vec3 at = Vec3.atCenterOf(pos);
		List<FallenStarBlockEntity.Changed> crater = new ArrayList<>(entity.crater);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		Star star = STARS.remove(new Key(level.dimension(), pos));
		EventLedger.of(level.getServer()).starGone(level.dimension(), pos);
		if (star != null) {
			for (UUID id : star.guards.keySet()) {
				if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
					WorldEvents.vanish(level, mob);
				}
			}
			star.scorch.forEach(Display::discard);
		}
		// The land knits back together, from the bottom up.
		crater.sort((a, b) -> Integer.compare(a.pos().getY(), b.pos().getY()));
		for (FallenStarBlockEntity.Changed c : crater) {
			BlockState now = level.getBlockState(c.pos());
			if (level.isLoaded(c.pos()) && (now.isAir() || now.is(Blocks.BLACKSTONE) || now.is(Blocks.SMOOTH_BASALT))) {
				level.setBlock(c.pos(), c.was(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
		}
		if (!crater.isEmpty()) {
			Fx.send(level, ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 16, 2.0, 0.5, 2.0, 0);
		}
		Sigils.flash(level, at, STAR_LIGHT, looted ? 3.0F : 1.6F);
		ElementFx.groundRing(level, Vec3.atBottomCenterOf(pos), STAR_GLOW, 0.2, 3.0, 0.08, 10);
		Fx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, looted ? 24 : 10, 0.4, 0.4, 0.4, 0.12);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, looted ? 0.8F : 1.2F);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.6F);
	}
}
