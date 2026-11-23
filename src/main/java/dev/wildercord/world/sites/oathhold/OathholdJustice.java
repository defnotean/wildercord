package dev.wildercord.world.sites.oathhold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.ServerState;
import dev.wildercord.Wildercord;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guard-witnessed crime persecution, temporary street-side Watch Stockade imprisonment overlooking
 * damaged houses, and visible NPC reconstruction crews for Oathhold.
 */
public final class OathholdJustice {
	private OathholdJustice() {}

	public record SavedBlock(BlockPos pos, BlockState state) {
		public static final Codec<SavedBlock> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(SavedBlock::pos),
			BlockState.CODEC.fieldOf("state").forGetter(SavedBlock::state)
		).apply(i, SavedBlock::new));
	}

	public static final class RepairSite {
		public final UUID id;
		public final BlockPos anchor;
		public long startRepairAt;
		public int totalOriginal;
		public final List<SavedBlock> remaining;

		public static final Codec<RepairSite> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(s -> s.id),
			BlockPos.CODEC.fieldOf("anchor").forGetter(s -> s.anchor),
			Codec.LONG.fieldOf("start_at").forGetter(s -> s.startRepairAt),
			Codec.INT.fieldOf("total").forGetter(s -> s.totalOriginal),
			SavedBlock.CODEC.listOf().fieldOf("remaining").forGetter(s -> List.copyOf(s.remaining))
		).apply(i, RepairSite::new));

		public RepairSite(UUID id, BlockPos anchor, long startRepairAt, int totalOriginal, List<SavedBlock> remaining) {
			this.id = id;
			this.anchor = anchor.immutable();
			this.startRepairAt = startRepairAt;
			this.totalOriginal = totalOriginal;
			this.remaining = new ArrayList<>(remaining);
		}
	}

	public static final class PrisonRecord {
		public final UUID prisonerId;
		public final BlockPos cellCenter;
		public final BlockPos lookTarget;
		public final long releaseAt;
		public final UUID siteId;
		public final List<SavedBlock> savedCellBlocks;

		public static final Codec<PrisonRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("prisoner").forGetter(r -> r.prisonerId),
			BlockPos.CODEC.fieldOf("cell").forGetter(r -> r.cellCenter),
			BlockPos.CODEC.fieldOf("look").forGetter(r -> r.lookTarget),
			Codec.LONG.fieldOf("release_at").forGetter(r -> r.releaseAt),
			UUIDUtil.CODEC.fieldOf("site").forGetter(r -> r.siteId),
			SavedBlock.CODEC.listOf().fieldOf("saved_cell").forGetter(r -> List.copyOf(r.savedCellBlocks))
		).apply(i, PrisonRecord::new));

		public PrisonRecord(UUID prisonerId, BlockPos cellCenter, BlockPos lookTarget, long releaseAt,
				UUID siteId, List<SavedBlock> savedCellBlocks) {
			this.prisonerId = prisonerId;
			this.cellCenter = cellCenter.immutable();
			this.lookTarget = lookTarget.immutable();
			this.releaseAt = releaseAt;
			this.siteId = siteId;
			this.savedCellBlocks = new ArrayList<>(savedCellBlocks);
		}
	}

	public static final class JusticeLedger extends SavedData {
		private static final Codec<JusticeLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
			RepairSite.CODEC.listOf().optionalFieldOf("repairs", List.of()).forGetter(l -> List.copyOf(l.repairs)),
			PrisonRecord.CODEC.listOf().optionalFieldOf("prisons", List.of()).forGetter(l -> List.copyOf(l.prisons))
		).apply(i, JusticeLedger::new));
		static final SavedDataType<JusticeLedger> TYPE = new SavedDataType<>("oathhold_justice", JusticeLedger::new, CODEC, null);

		final List<RepairSite> repairs = new ArrayList<>();
		final List<PrisonRecord> prisons = new ArrayList<>();

		public JusticeLedger() {}

		private JusticeLedger(List<RepairSite> repairs, List<PrisonRecord> prisons) {
			this.repairs.addAll(repairs);
			this.prisons.addAll(prisons);
		}

		public @Nullable PrisonRecord prisonFor(UUID playerId) {
			for (PrisonRecord r : prisons) {
				if (r.prisonerId.equals(playerId)) return r;
			}
			return null;
		}

		public @Nullable RepairSite repairFor(UUID siteId) {
			for (RepairSite s : repairs) {
				if (s.id.equals(siteId)) return s;
			}
			return null;
		}
	}

	private record Persecution(long until, BlockPos anchor, UUID siteId, boolean explosion) {}
	private record TntPlacer(UUID playerId, long placedAt) {}

	private static final Map<UUID, Persecution> PERSECUTION = new HashMap<>();
	private static final Map<Long, TntPlacer> TNT_PLACERS = new HashMap<>();
	private static final Map<UUID, List<UUID>> REPAIR_CREWS = new HashMap<>();
	private static final Map<UUID, UUID> PRISON_WARDENS = new HashMap<>();

	public static void init() {
		ServerState.forgetOnDisconnect(PERSECUTION);
		ServerState.resetOnStop(PERSECUTION);
		ServerState.resetOnStop(TNT_PLACERS);
		ServerState.resetOnStop(REPAIR_CREWS);
		ServerState.resetOnStop(PRISON_WARDENS);
		ServerTickEvents.END_SERVER_TICK.register(OathholdJustice::tick);
	}

	public static JusticeLedger ledger(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(JusticeLedger.TYPE);
	}

	public static void recordTntPlaced(ServerPlayer player, BlockPos pos, long now) {
		TNT_PLACERS.put(pos.asLong(), new TntPlacer(player.getUUID(), now));
		if (TNT_PLACERS.size() > 256) {
			TNT_PLACERS.entrySet().removeIf(e -> now - e.getValue().placedAt > 600L);
		}
	}

	public static @Nullable ServerPlayer findExplosionCulprit(ServerLevel level, ServerExplosion explosion, Vec3 center) {
		LivingEntity indirect = explosion.getIndirectSourceEntity();
		if (indirect instanceof ServerPlayer sp) return sp;
		Entity direct = explosion.getDirectSourceEntity();
		if (direct instanceof PrimedTnt tnt && tnt.getOwner() instanceof ServerPlayer sp) return sp;
		BlockPos approx = BlockPos.containing(center);
		long now = level.getGameTime();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					TntPlacer rec = TNT_PLACERS.get(approx.offset(dx, dy, dz).asLong());
					if (rec != null && now - rec.placedAt <= 600L) {
						ServerPlayer online = level.getServer().getPlayerList().getPlayer(rec.playerId);
						if (online != null) return online;
					}
				}
			}
		}
		return null;
	}

	public static boolean isGuardEntity(@Nullable Entity e) {
		if (e == null || !e.isAlive() || e.getTags().contains(OathholdLaw.ARENA_DUELIST_TAG)) return false;
		if (OathholdLaw.isKnightEntity(e)) return true;
		if (e instanceof IronGolem) return true;
		return e instanceof OathholdCitizen citizen && citizen.archetype() == OathholdCitizenRules.Archetype.CITY_WATCHMAN;
	}

	public static boolean isImprisoned(ServerPlayer player) {
		return ledger(player.level().getServer()).prisonFor(player.getUUID()) != null;
	}

	public static boolean isPersecuted(ServerPlayer player) {
		Persecution p = PERSECUTION.get(player.getUUID());
		if (p == null) return false;
		return player.level().getServer().overworld().getGameTime() < p.until;
	}

	public static long remainingSentenceTicks(ServerPlayer player) {
		PrisonRecord rec = ledger(player.level().getServer()).prisonFor(player.getUUID());
		if (rec == null) return 0L;
		return Math.max(0L, rec.releaseAt - player.level().getServer().overworld().getGameTime());
	}

	public static int activeRepairBlockCount(MinecraftServer server, BlockPos near, double radius) {
		double r2 = radius * radius;
		int count = 0;
		for (RepairSite site : ledger(server).repairs) {
			if (site.anchor.distSqr(near) <= r2) {
				count += site.remaining.size();
			}
		}
		return count;
	}

	public static boolean isProtectedPrisonBlock(MinecraftServer server, BlockPos pos) {
		for (PrisonRecord rec : ledger(server).prisons) {
			if (Math.abs(pos.getX() - rec.cellCenter.getX()) <= 2
					&& pos.getY() >= rec.cellCenter.getY() - 1 && pos.getY() <= rec.cellCenter.getY() + 3
					&& Math.abs(pos.getZ() - rec.cellCenter.getZ()) <= 2) {
				return true;
			}
		}
		return false;
	}

	public static boolean witnessedByGuard(ServerLevel level, ServerPlayer player, BlockPos crimePos, boolean isExplosion) {
		double maxRange = isExplosion ? OathholdLawRules.GUARD_EXPLOSION_WITNESS_RANGE : OathholdLawRules.GUARD_BREAK_WITNESS_RANGE;
		double earshot = isExplosion ? OathholdLawRules.GUARD_EXPLOSION_EARSHOT_RANGE : OathholdLawRules.GUARD_BREAK_EARSHOT_RANGE;
		AABB box = new AABB(crimePos).inflate(maxRange).minmax(player.getBoundingBox().inflate(maxRange));
		Vec3 crimeVec = Vec3.atCenterOf(crimePos);
		for (Mob guard : level.getEntitiesOfClass(Mob.class, box, OathholdJustice::isGuardEntity)) {
			if (guard.distanceToSqr(crimeVec) <= earshot * earshot || guard.distanceToSqr(player) <= earshot * earshot) {
				return true;
			}
			if (guard.distanceToSqr(player) <= maxRange * maxRange && guard.getSensing().hasLineOfSight(player)) {
				return true;
			}
			if (guard.distanceToSqr(crimeVec) <= maxRange * maxRange && hasClearBlockSight(level, guard, crimeVec, crimePos)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasClearBlockSight(ServerLevel level, Mob guard, Vec3 targetVec, BlockPos targetPos) {
		Vec3 eye = guard.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, targetVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, guard));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(targetPos)
			|| hit.getBlockPos().distSqr(targetPos) <= 2.0;
	}

	public static void onCityDamaged(ServerLevel level, @Nullable ServerPlayer culprit, BlockPos anchor,
			List<SavedBlock> brokenBlocks, boolean isExplosion) {
		if (brokenBlocks.isEmpty()) return;
		MinecraftServer server = level.getServer();
		long now = server.overworld().getGameTime();
		JusticeLedger data = ledger(server);

		RepairSite targetSite = null;
		for (RepairSite existing : data.repairs) {
			if (existing.anchor.distSqr(anchor) <= 24 * 24) {
				targetSite = existing;
				break;
			}
		}

		boolean createdNew = targetSite == null;
		if (targetSite == null) {
			targetSite = new RepairSite(UUID.randomUUID(), anchor, now + OathholdLawRules.UNWITNESSED_REBUILD_DELAY_TICKS,
				0, List.of());
			data.repairs.add(targetSite);
		}

		LongSet known = new LongOpenHashSet(targetSite.remaining.size() + brokenBlocks.size());
		for (SavedBlock existing : targetSite.remaining) {
			known.add(existing.pos().asLong());
		}
		int added = 0;
		for (SavedBlock b : brokenBlocks) {
			if (b.state().isAir() || isProtectedPrisonBlock(server, b.pos())) continue;
			if (known.add(b.pos().asLong())) {
				targetSite.remaining.add(new SavedBlock(b.pos().immutable(), b.state()));
				added++;
			}
		}
		targetSite.totalOriginal += added;
		sortRepairQueue(targetSite.remaining);
		data.setDirty();

		if (culprit != null && !culprit.isCreative() && !culprit.isSpectator() && !isImprisoned(culprit)
				&& witnessedByGuard(level, culprit, anchor, isExplosion)) {
			OathholdLaw.recordCrime(culprit, isExplosion ? OathholdLawRules.CrimeKind.DEMOLITION : OathholdLawRules.CrimeKind.VANDALISM);
			PERSECUTION.put(culprit.getUUID(), new Persecution(now + OathholdLawRules.PERSECUTION_DURATION_TICKS,
				targetSite.anchor, targetSite.id, isExplosion));
			targetSite.startRepairAt = Math.min(targetSite.startRepairAt, now + 25L);
			data.setDirty();
			dispatchGuardsForArrest(level, culprit);
			culprit.sendSystemMessage(Component.literal(
				"[Oathhold Watch] Guards witnessed your destruction! They are watching you - if you fall to one heart, you will be imprisoned while builders restore the house.")
				.withStyle(ChatFormatting.RED));
		} else if (!createdNew && data.prisonFor(culprit == null ? UUID.randomUUID() : culprit.getUUID()) != null) {
			targetSite.startRepairAt = Math.min(targetSite.startRepairAt, now + 20L);
			data.setDirty();
		}
	}

	private static void dispatchGuardsForArrest(ServerLevel level, ServerPlayer culprit) {
		OathholdLaw.alertNearbyKnights(level, culprit);
		AABB box = culprit.getBoundingBox().inflate(48.0);
		for (OathholdCitizen watchman : level.getEntitiesOfClass(OathholdCitizen.class, box,
				c -> c.isAlive() && c.archetype() == OathholdCitizenRules.Archetype.CITY_WATCHMAN)) {
			watchman.assignPursuit(culprit.getUUID());
		}
	}

	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer sp)) return true;
		if (isImprisoned(sp)) return false;
		if (isPersecuted(sp) && isGuardEntity(source.getEntity()) && sp.level() instanceof ServerLevel level) {
			// Guards watch instead of attacking; arrest only at one heart.
			Persecution p = PERSECUTION.get(sp.getUUID());
			if (sp.getHealth() <= 2.0F) {
				imprisonPlayer(level, sp, p);
			}
			return false;
		}
		return true;
	}

	public static void imprisonPlayer(ServerLevel level, ServerPlayer player, Persecution persecution) {
		if (isImprisoned(player)) return;
		MinecraftServer server = level.getServer();
		long now = server.overworld().getGameTime();
		JusticeLedger data = ledger(server);
		RepairSite site = data.repairFor(persecution.siteId);
		BlockPos lookTarget = site != null ? site.anchor : persecution.anchor;

		BlockPos cellCenter = findPrisonCellSpot(level, lookTarget, data);
		List<SavedBlock> savedCell = buildStockade(level, cellCenter, lookTarget);

		player.stopRiding();
		float[] angles = lookAngles(new Vec3(cellCenter.getX() + 0.5, cellCenter.getY() + 1.62, cellCenter.getZ() + 0.5),
			Vec3.atCenterOf(lookTarget));
		player.teleportTo(level, cellCenter.getX() + 0.5, cellCenter.getY(), cellCenter.getZ() + 0.5,
			java.util.Set.of(), angles[0], angles[1], false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(Math.max(player.getHealth(), Math.min(player.getMaxHealth(), 10.0F)));

		PERSECUTION.remove(player.getUUID());
		OathholdLaw.pacifyKnightsFor(player);
		clearWatchmanPursuits(level, player.getUUID());

		int damagedCount = site != null ? Math.max(site.totalOriginal, site.remaining.size()) : 4;
		long duration = OathholdLawRules.sentenceTicks(damagedCount, persecution.explosion);
		long releaseAt = now + duration;

		PrisonRecord record = new PrisonRecord(player.getUUID(), cellCenter, lookTarget, releaseAt, persecution.siteId, savedCell);
		data.prisons.add(record);
		if (site != null) {
			site.startRepairAt = Math.min(site.startRepairAt, now + 15L);
			ensureRepairCrew(level, site);
		}
		data.setDirty();

		spawnWardenOutsideCell(level, player.getUUID(), cellCenter, lookTarget);

		level.playSound(null, cellCenter, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.85F);
		level.playSound(null, cellCenter, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
		player.sendSystemMessage(Component.literal(
			"[Oathhold Watch] Arrested! Held in the Watch Stockade for " + (duration / 20L)
				+ "s while Master Builders reconstruct the damaged property.")
			.withStyle(ChatFormatting.GOLD));
	}

	private static BlockPos findPrisonCellSpot(ServerLevel level, BlockPos targetPos, JusticeLedger data) {
		BlockPos fallback = null;
		int baseY = targetPos.getY();
		for (int radius = 7; radius <= 18; radius += 2) {
			for (int[] dir : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}}) {
				int cx = targetPos.getX() + Math.round((float) (dir[0] * radius / Math.hypot(dir[0], dir[1])));
				int cz = targetPos.getZ() + Math.round((float) (dir[1] * radius / Math.hypot(dir[0], dir[1])));
				for (int dy = -2; dy <= 3; dy++) {
					BlockPos candidate = new BlockPos(cx, baseY + dy, cz);
					if (!level.hasChunkAt(candidate) || overlapsRepairs(candidate, data)) continue;
					if (!hasCellSupport(level, candidate)) continue;
					if (!hasClearInteriorAndView(level, candidate, targetPos)) {
						if (fallback == null) fallback = candidate;
						continue;
					}
					return candidate;
				}
			}
		}
		if (fallback != null) return fallback;
		return new BlockPos(targetPos.getX() + 8, Math.max(level.getMinY() + 4, baseY), targetPos.getZ() + 8);
	}

	private static boolean overlapsRepairs(BlockPos center, JusticeLedger data) {
		for (RepairSite site : data.repairs) {
			for (SavedBlock b : site.remaining) {
				if (Math.abs(b.pos().getX() - center.getX()) <= 3
						&& Math.abs(b.pos().getZ() - center.getZ()) <= 3
						&& Math.abs(b.pos().getY() - center.getY()) <= 4) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasCellSupport(ServerLevel level, BlockPos center) {
		int solid = 0;
		for (int dx = -2; dx <= 2; dx += 2) {
			for (int dz = -2; dz <= 2; dz += 2) {
				if (level.getBlockState(center.offset(dx, -1, dz)).isSolidRender()) solid++;
			}
		}
		return solid >= 5;
	}

	private static boolean hasClearInteriorAndView(ServerLevel level, BlockPos cell, BlockPos lookTarget) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					BlockState st = level.getBlockState(cell.offset(dx, dy, dz));
					if (!st.isAir() && !st.canBeReplaced()) return false;
				}
			}
		}
		Vec3 eye = new Vec3(cell.getX() + 0.5, cell.getY() + 1.62, cell.getZ() + 0.5);
		Vec3 aim = Vec3.atCenterOf(lookTarget);
		Vec3 dir = aim.subtract(eye);
		double len = dir.length();
		if (len < 1.0) return true;
		// Start just outside our future cell iron bars so street blocks are verified clear.
		Vec3 rayStart = eye.add(dir.scale(Math.min(2.8 / len, 0.45)));
		BlockHitResult hit = level.clip(new ClipContext(rayStart, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().distSqr(lookTarget) <= 9.0;
	}

	private static List<SavedBlock> buildStockade(ServerLevel level, BlockPos cell, BlockPos lookTarget) {
		List<SavedBlock> saved = new ArrayList<>(125);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -1; dy <= 3; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					BlockPos p = cell.offset(dx, dy, dz);
					saved.add(new SavedBlock(p.immutable(), level.getBlockState(p)));
				}
			}
		}

		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlock(cell.offset(dx, -1, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
				level.setBlock(cell.offset(dx, 3, dz), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
				boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
				for (int dy = 0; dy <= 2; dy++) {
					BlockPos p = cell.offset(dx, dy, dz);
					if (corner) {
						level.setBlock(p, (dy == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.POLISHED_DEEPSLATE).defaultBlockState(), 3);
					} else if (edge) {
						level.setBlock(p, Blocks.IRON_BARS.defaultBlockState(), 3);
					} else {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
		}
		level.setBlock(cell.offset(0, 2, 0),
			Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 3);
		return saved;
	}

	private static void spawnWardenOutsideCell(ServerLevel level, UUID prisonerId, BlockPos cell, BlockPos lookTarget) {
		Vec3 towardHouse = Vec3.atCenterOf(lookTarget).subtract(Vec3.atCenterOf(cell));
		Vec3 side = Math.abs(towardHouse.x) > Math.abs(towardHouse.z) ? new Vec3(0, 0, 3.4) : new Vec3(3.4, 0, 0);
		BlockPos guardPos = BlockPos.containing(cell.getX() + 0.5 + side.x, cell.getY(), cell.getZ() + 0.5 + side.z);
		OathholdCitizen warden = OathholdContent.CITIZEN.create(level, EntitySpawnReason.COMMAND);
		if (warden == null) return;
		warden.configure(OathholdCitizenRules.Archetype.CITY_WATCHMAN, guardPos.getX(), guardPos.getY(), guardPos.getZ(), guardPos, 2);
		warden.markTemporaryCrew(true);
		float[] angles = lookAngles(Vec3.atCenterOf(guardPos), Vec3.atCenterOf(cell));
		warden.snapTo(guardPos.getX() + 0.5, guardPos.getY(), guardPos.getZ() + 0.5, angles[0], 0.0F);
		warden.finalizeSpawn(level, level.getCurrentDifficultyAt(guardPos), EntitySpawnReason.COMMAND, null);
		level.addFreshEntity(warden);
		PRISON_WARDENS.put(prisonerId, warden.getUUID());
	}

	public static void forceRelease(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return;
		PrisonRecord rec = ledger(level.getServer()).prisonFor(player.getUUID());
		if (rec != null) {
			releasePrisoner(level.getServer(), rec);
		}
	}

	private static void releasePrisoner(MinecraftServer server, PrisonRecord record) {
		ServerLevel level = server.overworld();
		// Restore bottom-up without neighbor churn so street blocks match pre-stockade state cleanly.
		record.savedCellBlocks.sort((a, b) -> OathholdLawRules.compareForRebuild(
			a.pos().getX(), a.pos().getY(), a.pos().getZ(), !a.state().isAir(),
			b.pos().getX(), b.pos().getY(), b.pos().getZ(), !b.state().isAir()));
		for (SavedBlock b : record.savedCellBlocks) {
			level.setBlock(b.pos(), b.state(), 18);
		}

		UUID wardenId = PRISON_WARDENS.remove(record.prisonerId);
		if (wardenId != null) {
			Entity warden = level.getEntity(wardenId);
			if (warden != null) warden.discard();
		}

		JusticeLedger data = ledger(server);
		data.prisons.remove(record);
		data.setDirty();

		ServerPlayer player = server.getPlayerList().getPlayer(record.prisonerId);
		if (player != null) {
			OathholdLaw.clearCrime(player, true);
			clearWatchmanPursuits(level, player.getUUID());
			player.teleportTo(level, record.cellCenter.getX() + 0.5, record.cellCenter.getY(), record.cellCenter.getZ() + 0.5,
				java.util.Set.of(), player.getYRot(), player.getXRot(), false);
			level.playSound(null, record.cellCenter, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.9F, 1.0F);
			level.playSound(null, record.cellCenter, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.15F);
			player.sendSystemMessage(Component.literal(
				"[Oathhold Watch] Sentence served. You have been released from the Watch Stockade with a cleared record.")
				.withStyle(ChatFormatting.GREEN));
		}
	}

	private static void clearWatchmanPursuits(ServerLevel level, UUID playerId) {
		for (Entity e : level.getAllEntities()) {
			if (e instanceof OathholdCitizen citizen && playerId.equals(citizen.pursuitTargetId())) {
				citizen.clearPursuit();
			}
		}
	}

	private static void tick(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		long now = overworld.getGameTime();
		JusticeLedger data = ledger(server);

		tickPersecutions(server, overworld, now);
		tickPrisons(server, overworld, data, now);
		tickRepairs(overworld, data, now);

		if (data.repairs.isEmpty() && data.prisons.isEmpty() && now % 40L == 0L) {
			for (Entity e : overworld.getAllEntities()) {
				if (e instanceof OathholdCitizen citizen && citizen.isTemporaryCrew()) {
					citizen.discard();
				}
			}
		}
	}

	private static void tickPersecutions(MinecraftServer server, ServerLevel overworld, long now) {
		if (PERSECUTION.isEmpty()) return;
		Iterator<Map.Entry<UUID, Persecution>> it = PERSECUTION.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Persecution> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Persecution p = entry.getValue();
			if (player == null || now >= p.until || player.isCreative() || player.isSpectator() || !player.isAlive()) {
				it.remove();
				continue;
			}
			if (player.level() != overworld) continue;
			AABB captureBox = player.getBoundingBox().inflate(OathholdLawRules.ARREST_CAPTURE_RANGE);
			List<Mob> capturingGuards = overworld.getEntitiesOfClass(Mob.class, captureBox, OathholdJustice::isGuardEntity);
			if (!capturingGuards.isEmpty() && player.getHealth() <= 2.0F) {
				it.remove();
				imprisonPlayer(overworld, player, p);
				continue;
			}
			if (now % 20L == 0L) {
				dispatchGuardsForArrest(overworld, player);
			}
		}
	}

	private static void tickPrisons(MinecraftServer server, ServerLevel overworld, JusticeLedger data, long now) {
		if (data.prisons.isEmpty()) return;
		List<PrisonRecord> toRelease = new ArrayList<>();
		for (PrisonRecord rec : data.prisons) {
			ServerPlayer player = server.getPlayerList().getPlayer(rec.prisonerId);
			if (now >= rec.releaseAt || (player != null && (player.isCreative() || player.isSpectator()))) {
				toRelease.add(rec);
				continue;
			}
			if (player != null) {
				if (player.level() != overworld || !OathholdLawRules.isInsideCell(
						player.getX(), player.getY(), player.getZ(),
						rec.cellCenter.getX(), rec.cellCenter.getY(), rec.cellCenter.getZ())) {
					float[] angles = lookAngles(new Vec3(rec.cellCenter.getX() + 0.5, rec.cellCenter.getY() + 1.62, rec.cellCenter.getZ() + 0.5),
						Vec3.atCenterOf(rec.lookTarget));
					player.teleportTo(overworld, rec.cellCenter.getX() + 0.5, rec.cellCenter.getY(), rec.cellCenter.getZ() + 0.5,
						java.util.Set.of(), angles[0], angles[1], false);
					player.setDeltaMovement(Vec3.ZERO);
				}
				if (now % 20L == 0L) {
					long secs = Math.max(1L, (rec.releaseAt - now + 19L) / 20L);
					RepairSite site = data.repairFor(rec.siteId);
					String repairNote = site == null || site.remaining.isEmpty()
						? "House restoration complete"
						: "Watch Builders repairing: " + Math.max(0, site.totalOriginal - site.remaining.size()) + "/" + site.totalOriginal + " blocks";
					player.displayClientMessage(Component.literal("Imprisoned (" + secs + "s) — " + repairNote).withStyle(ChatFormatting.GOLD), true);
				}
			}
		}
		for (PrisonRecord rec : toRelease) {
			releasePrisoner(server, rec);
		}
	}

	private static void tickRepairs(ServerLevel level, JusticeLedger data, long now) {
		if (data.repairs.isEmpty()) return;
		Iterator<RepairSite> it = data.repairs.iterator();
		while (it.hasNext()) {
			RepairSite site = it.next();
			if (now < site.startRepairAt) continue;
			if (!level.hasChunkAt(site.anchor)) continue;

			List<OathholdCitizen> crew = ensureRepairCrew(level, site);
			if (site.remaining.isEmpty()) {
				releaseCrew(site.id, crew);
				it.remove();
				data.setDirty();
				continue;
			}

			if (now % OathholdLawRules.REBUILD_INTERVAL_TICKS != 0L) continue;

			SavedBlock next = site.remaining.removeFirst();
			data.setDirty();

			OathholdCitizen worker = crew.isEmpty() ? null : crew.get(site.remaining.size() % crew.size());
			if (worker != null) {
				worker.assignRepairTarget(next.pos());
				float[] angles = lookAngles(worker.position(), Vec3.atCenterOf(next.pos()));
				worker.setYRot(angles[0]);
				worker.setYHeadRot(angles[0]);
				worker.getLookControl().setLookAt(next.pos().getX() + 0.5, next.pos().getY() + 0.5, next.pos().getZ() + 0.5);
				worker.triggerWorkSwing();
			}

			BlockState state = next.state();
			level.setBlock(next.pos(), state, 18);
			placePartnerHalfIfPresent(level, site.remaining, next.pos(), state);
			level.playSound(null, next.pos(), state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.75F, 1.0F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
				next.pos().getX() + 0.5, next.pos().getY() + 0.5, next.pos().getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.05);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
				next.pos().getX() + 0.5, next.pos().getY() + 0.7, next.pos().getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);

			if (site.remaining.isEmpty()) {
				releaseCrew(site.id, crew);
				it.remove();
				data.setDirty();
			}
		}
	}

	/** Returns the linked half position for two-block fixtures (doors, beds, tall plants), or null. */
	public static @Nullable BlockPos partnerBlockPos(BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof DoorBlock) {
			return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		}
		if (state.getBlock() instanceof DoublePlantBlock) {
			return state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		}
		if (state.getBlock() instanceof BedBlock) {
			Direction facing = state.getValue(BedBlock.FACING);
			return pos.relative(state.getValue(BedBlock.PART) == BedPart.HEAD ? facing.getOpposite() : facing);
		}
		return null;
	}

	private static void placePartnerHalfIfPresent(ServerLevel level, List<SavedBlock> remaining, BlockPos pos, BlockState state) {
		BlockPos partnerPos = partnerBlockPos(pos, state);
		if (partnerPos == null) return;
		for (Iterator<SavedBlock> remIt = remaining.iterator(); remIt.hasNext(); ) {
			SavedBlock b = remIt.next();
			if (b.pos().equals(partnerPos)) {
				remIt.remove();
				level.setBlock(b.pos(), b.state(), 18);
				break;
			}
		}
	}

	private static List<OathholdCitizen> ensureRepairCrew(ServerLevel level, RepairSite site) {
		List<UUID> trackedIds = REPAIR_CREWS.computeIfAbsent(site.id, ignored -> new ArrayList<>());
		List<OathholdCitizen> alive = new ArrayList<>();
		for (Iterator<UUID> idIt = trackedIds.iterator(); idIt.hasNext(); ) {
			Entity e = level.getEntity(idIt.next());
			if (e instanceof OathholdCitizen c && c.isAlive()) {
				alive.add(c);
			} else {
				idIt.remove();
			}
		}
		int needed = OathholdLawRules.repairCrewSize(Math.max(site.totalOriginal, site.remaining.size()));
		if (alive.size() < needed) {
			AABB nearby = new AABB(site.anchor).inflate(28.0);
			for (OathholdCitizen c : level.getEntitiesOfClass(OathholdCitizen.class, nearby,
					c -> c.isAlive() && c.archetype() == OathholdCitizenRules.Archetype.CARPENTER_MASON && !alive.contains(c))) {
				alive.add(c);
				trackedIds.add(c.getUUID());
				if (alive.size() >= needed) break;
			}
		}
		int attempts = 0;
		while (alive.size() < needed && attempts < 12) {
			attempts++;
			int ox = Math.round(Mth.cos(attempts * 1.4F) * (4.0F + (attempts % 3) * 1.5F));
			int oz = Math.round(Mth.sin(attempts * 1.4F) * (4.0F + (attempts % 3) * 1.5F));
			BlockPos spawnPos = findCrewGround(level, site.anchor.offset(ox, 0, oz));
			OathholdCitizen builder = OathholdContent.CITIZEN.create(level, EntitySpawnReason.COMMAND);
			if (builder == null) break;
			builder.configure(OathholdCitizenRules.Archetype.CARPENTER_MASON, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), spawnPos, 6);
			builder.markTemporaryCrew(true);
			builder.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
			builder.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.COMMAND, null);
			level.addFreshEntity(builder);
			alive.add(builder);
			trackedIds.add(builder.getUUID());
		}
		return alive;
	}

	private static BlockPos findCrewGround(ServerLevel level, BlockPos near) {
		for (int dy = 2; dy >= -4; dy--) {
			BlockPos candidate = near.offset(0, dy, 0);
			if (level.getBlockState(candidate.below()).isSolidRender()
					&& level.getBlockState(candidate).isAir()
					&& level.getBlockState(candidate.above()).isAir()) {
				return candidate;
			}
		}
		return near;
	}

	private static void releaseCrew(UUID siteId, List<OathholdCitizen> crew) {
		REPAIR_CREWS.remove(siteId);
		for (OathholdCitizen c : crew) {
			c.clearRepairTarget();
			if (c.isTemporaryCrew()) {
				c.discard();
			}
		}
	}

	private static void sortRepairQueue(List<SavedBlock> list) {
		list.sort((a, b) -> OathholdLawRules.compareForRebuild(
			a.pos().getX(), a.pos().getY(), a.pos().getZ(), a.state().isSolidRender(),
			b.pos().getX(), b.pos().getY(), b.pos().getZ(), b.state().isSolidRender()));
	}

	private static float[] lookAngles(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		return new float[]{yaw, Mth.clamp(pitch, -65.0F, 65.0F)};
	}
}
