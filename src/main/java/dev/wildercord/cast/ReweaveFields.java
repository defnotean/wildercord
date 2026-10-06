package dev.wildercord.cast;

import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.content.CordTier;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RelayGeometry;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.ReweaveRules;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Package-private mechanism proof, started only by the native feasibility suite. Not registered in
 * Wildercord, SpellCaster, a packet, editor, lesson, item, storage or passive path. Policy is provisional.
 */
final class ReweaveFields {
    private ReweaveFields() {}
    private static final Map<ServerPlayer, Field> FIELDS = new IdentityHashMap<>();
    private static final Map<ServerPlayer, RelayInputRules.Edges> INPUT = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> REST = new WeakHashMap<>();
    private static boolean initialized;

    private static final class Field {
        final ServerPlayer player;
        final ReleasedArtOwner owner;
        final ServerLevel level;
        final Vec3 center;
        final BlockPos floor;
        final int slot, cost;
        final DungeonWards.MovementWard ward;
        final SpellCompiler.Compiled compiled;
        ReweaveRules.Timeline timeline;
        Vec3 direction, impactPoint;
        Cast cast;
        boolean retired, impactActive;
        long observed;
        Field(ServerPlayer p, int slot, int cost, Vec3 center, BlockPos floor, SpellCompiler.Compiled compiled) {
            this.player = p; this.owner = ReleasedArtOwner.capture(p); this.level = p.level();
            this.slot = slot; this.cost = cost; this.center = center; this.floor = floor.immutable(); this.compiled = compiled;
            this.ward = DungeonWards.movementWard(level, p.blockPosition());
            this.timeline = ReweaveRules.Timeline.start(now(p)); this.observed = now(p);
        }
        boolean valid() {
            long tick = now(player);
            if (retired || FIELDS.get(player) != this || !owner.valid() || tick < observed || timeline.expired(tick)) {
                retired = true; return false;
            }
            observed = tick;
            return true;
        }
        AABB bounds(Vec3 lane) {
            if (lane == null) return new AABB(center.x - 2, center.y - .13, center.z - 2,
                center.x + 2, center.y + ReweaveRules.HEIGHT, center.z + 2);
            Vec3 end = center.add(lane.scale(ReweaveRules.LANE_LENGTH));
            double half = ReweaveRules.LANE_WIDTH / 2;
            return new AABB(Math.min(center.x, end.x) - half, center.y - .13, Math.min(center.z, end.z) - half,
                Math.max(center.x, end.x) + half, center.y + ReweaveRules.HEIGHT, Math.max(center.z, end.z) + half);
        }
        boolean geometry(Vec3 lane) {
            if (!finite(player.getEyePosition()) || !loaded(level, floor) || !sameWard(this, player.blockPosition())
                || level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) return false;
            AABB box = bounds(lane);
            // Conservative finite box: every queried chunk/ward cell is checked before clip/entity access.
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
                if (!loaded(level, pos) || !sameWard(this, pos)) return false;
            if (!clear(this, player.getEyePosition(), center)) return false;
            if (lane != null) {
                Vec3 side = new Vec3(-lane.z, 0, lane.x).scale(ReweaveRules.LANE_WIDTH / 2);
                for (int edge = -1; edge <= 1; edge++) {
                    Vec3 start = center.add(side.scale(edge)), end = start.add(lane.scale(ReweaveRules.LANE_LENGTH));
                    if (!clear(this, center, start) || !clear(this, start, end)) return false;
                }
            }
            return true;
        }
        boolean admits(Entity e) {
            if (!impactActive || !valid() || timeline.warning(now(player)) || !(e instanceof LivingEntity) || e.level() != level || !e.isAlive() || e.isRemoved()
                || level.getEntity(e.getUUID()) != e || !Targets.canHarm(player, e) || !geometry(direction)) return false;
            Vec3 delta = e.position().subtract(center);
            if (delta.y < -.13 || delta.y > ReweaveRules.HEIGHT) return false;
            boolean inside = direction == null ? ReweaveRules.insideDisc(delta.x, delta.z)
                : ReweaveRules.insideLane(delta.x, delta.z, direction.x, direction.z);
            return inside && impactReadsLoaded(e.getBoundingBox().getCenter()) && clear(this, center, e.getBoundingBox().getCenter())
                && clear(this, player.getEyePosition(), e.getBoundingBox().getCenter());
        }
        private boolean impactValid() {
            return impactActive && impactPoint != null && valid() && geometry(direction) && impactReadsLoaded(impactPoint);
        }
        private boolean impactReadsLoaded(Vec3 at) {
            // Harm's Rune-Seal and cosmetic shimmer hooks read this halo even when block writes are denied.
            int reach = dev.wildercord.spell.WorldRules.SHIMMER_REACH;
            BlockPos center = BlockPos.containing(at);
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach / 2 - 1, -reach), center.offset(reach, reach / 2 + 1, reach)))
                if (!loaded(level, pos) || !DungeonWards.movementWard(level, pos).known()) return false;
            return true;
        }
    }

    /** Read-only evidence view; the payment reference is opaque and never an authorization token. */
    record View(Vec3 center, Vec3 direction, long created, long expires, int nextBeat, long convertedAt,
                int cost, Object payment, Object identity, double power) {}
    static View view(ServerPlayer player) {
        Field f = FIELDS.get(player);
        return f == null || !f.valid() ? null : new View(f.center, f.direction, f.timeline.created(), f.timeline.expires(),
            f.timeline.nextBeat(), f.timeline.convertedAt(), f.cost, f.cast.payment(), f.cast.identity(), f.cast.power);
    }
    static long rest(ServerPlayer player) { return REST.getOrDefault(player, 0L); }
    static long now(ServerPlayer player) { return player.level().getServer().overworld().getGameTime(); }
    static boolean entitled(ServerPlayer player) {
        return ReweaveRules.eligible(Heart.active(player), Heart.discovered(player, "feat:" + Feats.TIDE_SCRIBE), Heart.discovered(player, ReweaveRules.STUDY));
    }
    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isRemoved() && !p.isSpectator() && !p.isSleeping() && !p.isPassenger()
            && p.level().getServer().getPlayerList().getPlayer(p.getUUID()) == p && entitled(p)
            && !CastLock.locked(p) && !dev.wildercord.aura.arts.ArtWards.silenced(p) && !VoidTime.hushed(p) && !FusedFrostWards.sealed(p)
            && !dev.wildercord.aura.MastersArts.committed(p) && !RelayCircles.committed(p)
            && p.containerMenu == p.inventoryMenu && !p.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)
            && Float.isFinite(p.getXRot()) && Float.isFinite(p.getYRot()) && finite(p.getEyePosition());
    }
    private static boolean row(ServerPlayer p, int slot) {
        return slot >= 0 && slot < dev.wildercord.gear.SpellSlots.ALL && Spellbooks.tier(p) == CordTier.ECHO
            && dev.wildercord.gear.Gear.spellOpen(p, CordTier.ECHO, slot)
            && Spellbooks.get(p).spells().get(slot).equals(ReweaveRules.IDS)
            && ReweaveRules.IDS.stream().allMatch(Spellbooks.get(p)::knows);
    }

    /** Authoritative production-style payment route, deliberately not wired to any player input. */
    static void input(ServerPlayer p, int action, int slot, long nonce) {
        try (var admission = ActionAdmission.begin(p)) {
            if (admission == null || slot < 0 || slot >= dev.wildercord.gear.SpellSlots.ALL
                || !INPUT.computeIfAbsent(p, ignored -> new RelayInputRules.Edges()).accept(action, nonce, now(p))) return;
            if (action == RelayInputRules.CANCEL) { cancel(p); return; }
            if (action != RelayInputRules.DOWN || !available(p) || !row(p, slot)) return;
            Field previous = FIELDS.get(p);
            if (previous != null) {
                if (!previous.valid()) { cancel(p); return; }
                if (slot == previous.slot) convert(previous);
                return; // A second field is never a replacement/refund route in this proof.
            }
            place(p, slot, admission);
        }
    }
    private static void place(ServerPlayer p, int slot, ActionAdmission admission) {
        long tick = now(p);
        if (tick < rest(p) || p.level().getGameTime() < Spellbooks.readyAt(p, slot) || WildSurge.freeRecast(p, p.level().getGameTime())) return;
        Vec3 eye = p.getEyePosition(), aim = eye.add(p.getLookAngle().scale(ReweaveRules.PLACE_RANGE));
        if (!pathLoaded(p.level(), eye, aim)) return;
        var hit = p.level().clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return;
        Vec3 at = hit.getLocation().add(0, .12, 0);
        var compiled = SpellCompiler.compile(ReweaveRules.RUNES);
        int cost = Heart.manaCost(p, compiled, Mastery.costFactor(p, ReweaveRules.RUNES));
        if (cost <= 0 || !Float.isFinite(Spellbooks.mana(p)) || Spellbooks.mana(p) < cost) return;
        Field field = new Field(p, slot, cost, at, hit.getBlockPos(), compiled);
        if (!field.owner.valid() || !field.geometry(null)) return;
        var cord = Spellbooks.cord(p); var book = Spellbooks.get(p); int circles = Heart.active(p);
        if (!dev.wildercord.api.WildercordEvents.BEFORE_CAST.invoker().allow(p, slot, ReweaveRules.RUNES, cost)) return;
        if (!admission.valid() || !field.owner.valid() || !available(p) || !row(p, slot) || FIELDS.containsKey(p)
            || cord != Spellbooks.cord(p) || !book.equals(Spellbooks.get(p)) || circles != Heart.active(p)
            || !field.geometry(null) || p.getEyePosition().distanceTo(at) > ReweaveRules.PLACE_RANGE + .13
            || tick < rest(p) || Spellbooks.readyAt(p, slot) > p.level().getGameTime()
            || WildSurge.freeRecast(p, p.level().getGameTime()) || !Float.isFinite(Spellbooks.mana(p)) || Spellbooks.mana(p) < cost) return;
        float mana = Spellbooks.mana(p);
        Heart.Bonuses bonuses = Heart.bonuses(p, mana >= dev.wildercord.player.Mana.max(p) - .5F);
        bonuses = bonuses.withPower(bonuses.power() * Mastery.powerFactor(p, ReweaveRules.RUNES));
        field.cast = new Cast(p, 1, bonuses, false, null, new Cast.Info(compiled.root(), 2, Heart.leaning(p), ReweaveRules.RUNES))
            .weigh(compiled.cost()).damagePrice(cost).gear(dev.wildercord.gear.Gear.of(p)).withAffinity()
            .admission(field::admits).blockAdmission(ignored -> false).lifetime(field::impactValid).incoming(at);
        // Reserve exactly one field/payment/rest before progression callbacks can re-enter casting.
        Spellbooks.setMana(p, mana - cost);
        REST.put(p, tick + ReweaveRules.REST_TICKS);
        Spellbooks.setReadyAt(p, slot, p.level().getGameTime() + ReweaveRules.REST_TICKS);
        FIELDS.put(p, field);
        dev.wildercord.aura.Unity.manaSpent(p, cost);
        if (!field.valid()) { cancel(p); return; }
        // Unity/add-on callbacks may change ward references after payment. Recheck before Mastery's legacy structure lookup.
        if (!field.geometry(null)) { cancel(p); return; }
        Mastery.onCast(p, slot, ReweaveRules.RUNES, field.cast, cost);
        HeartCircles.condense(p, cost);
        PlayerAffinities.onCast(p, compiled.root(), cost, 0);
        HeartCircles.onCast(p);
        if (!field.valid()) { cancel(p); return; }
        dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(p, slot, ReweaveRules.RUNES, cost);
        if (!field.valid()) cancel(p);
    }
    private static void convert(Field field) {
        long tick = now(field.player);
        var converted = field.timeline.convert(tick);
        if (converted == null) return;
        Vec3 eye = field.player.getEyePosition(), far = eye.add(field.player.getLookAngle().scale(ReweaveRules.PLACE_RANGE));
        if (!pathLoaded(field.level, eye, far)) return;
        var hit = field.level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, field.player));
        Vec3 aim = hit.getType() == HitResult.Type.MISS ? far : hit.getLocation();
        Vec3 delta = aim.subtract(field.center).multiply(1, 0, 1);
        if (!finite(delta) || delta.lengthSqr() < .01) return;
        Vec3 direction = delta.normalize();
        if (!field.valid() || !field.geometry(direction)) return;
        // This is the only rewrite: same Cast/payment/center/expiry, no price, progression or statistics refresh.
        field.timeline = converted;
        field.direction = direction;
    }
    private static void tick(Field field) {
        if (!field.valid()) { cancel(field.player); return; }
        var advance = field.timeline.advance(now(field.player));
        field.timeline = advance.timeline(); // Consume before any callback; warnings and late ticks get no replay.
        if (!advance.strike() || !field.geometry(field.direction)) return;
        try (var admission = ActionAdmission.begin(field.player)) {
            if (admission == null) return;
            List<LivingEntity> targets = new ArrayList<>();
            field.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class), field.bounds(field.direction),
                e -> e != field.player && e.isAlive(), targets, Cast.MAX_ENTITIES + 1);
            if (targets.size() > Cast.MAX_ENTITIES) return;
            targets.sort(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(field.center)).thenComparingInt(Entity::getId));
            field.impactActive = true;
            try {
                for (LivingEntity target : targets) {
                    if (!admission.valid() || !field.valid()) break;
                    field.impactPoint = target.getBoundingBox().getCenter();
                    if (!field.admits(target)) continue;
                    Vec3 direction = field.direction == null ? target.position().subtract(field.center).normalize() : field.direction;
                    Cast beat = field.cast.child(ReweaveRules.BEAT_POWER); // Shared budget: deliberately never pulse()/again().
                    Effects.lingering(() -> CastEngine.onHit(beat, field.compiled.root().groups.getFirst(),
                        new Cast.Hit(List.of(target), target.getBoundingBox().getCenter(), direction, field.center, null, null, false), null));
                }
            } finally { field.impactActive = false; field.impactPoint = null; }
        }
        if (!field.valid()) cancel(field.player);
    }
    static void cancel(ServerPlayer p) {
        Field field = FIELDS.remove(p);
        if (field != null) { field.retired = true; field.cast.cancel(); dev.wildercord.aura.ResonantStrikes.retire(field.cast); }
    }
    private static boolean finite(Vec3 p) { return Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z); }
    private static boolean loaded(ServerLevel level, BlockPos p) {
        return level.hasChunkAt(p) && !level.isOutsideBuildHeight(p) && level.getWorldBorder().isWithinBounds(p);
    }
    private static boolean pathLoaded(ServerLevel level, Vec3 from, Vec3 to) {
        var cells = RelayGeometry.cells(from.x, from.y, from.z, to.x, to.y, to.z);
        if (cells.isEmpty()) return false;
        for (var cell : cells) if (!loaded(level, new BlockPos(cell.x(), cell.y(), cell.z()))) return false;
        return true;
    }
    /** Unknown structure references refuse admission; only resident FULL chunks may supply ward rooms. */
    private static boolean sameWard(Field f, BlockPos pos) {
        return f.ward.known() && f.ward.equals(DungeonWards.movementWard(f.level, pos));
    }
    private static boolean clear(Field f, Vec3 from, Vec3 to) {
        if (!pathLoaded(f.level, from, to)) return false;
        for (var cell : RelayGeometry.cells(from.x, from.y, from.z, to.x, to.y, to.z))
            if (!sameWard(f, new BlockPos(cell.x(), cell.y(), cell.z()))) return false;
        var hit = f.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, f.player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < .0004;
    }
    /** Only the native mechanism test calls this; intentionally absent from Wildercord.init. */
    static void initFeasibility() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (Field field : List.copyOf(FIELDS.values())) if (field.level.getServer() == server) tick(field);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            for (ServerPlayer p : List.copyOf(FIELDS.keySet())) if (p.level().getServer() == server) cancel(p);
            INPUT.clear(); REST.clear();
        });
    }
}
