package dev.wildercord.cast;

import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.ExciseRules;
import dev.wildercord.spell.RelayGeometry;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An opt-in target index. Removing a receipt never changes a released Cast's existing ownership. */
public final class NativeZoneEmitters {
    private NativeZoneEmitters() {}
    public static final int MAXIMUM = 512, MAX_VISIBLE = 64;
    private static final Map<Long, Emitter> EMITTERS = new LinkedHashMap<>();
    private static long nextId;

    static final class Emitter {
        final long id = ++nextId;
        final Cast cast;
        final Vec3 center, core;
        final long created, expires;
        final ReleasedArtOwner owner;
        boolean excised, retired;
        int remaining;
        Emitter(Cast cast, Vec3 center, int lastPulse, int pulses) {
            this.cast = cast; this.center = center; this.core = center.add(0, .65, 0);
            this.remaining = pulses;
            this.created = clock(cast.level); this.expires = created + lastPulse + 2L;
            this.owner = cast.caster instanceof ServerPlayer p ? ReleasedArtOwner.capture(p) : null;
        }
        boolean targetable() {
            long tick = clock(cast.level);
            if (retired || excised || remaining <= 0 || tick < created || tick >= expires || !cast.alive()
                || owner != null && !owner.valid()) retired = true;
            return !retired;
        }
        /** The ONLY change to pulse execution: an explicit local cut. No shared cancellation or lifetime. */
        boolean allowsPulse() { return !excised; }
        boolean beginPulse() { if (excised) return false; remaining--; return true; }
    }

    static Emitter register(Cast cast, SpellPlan.Group group, SpellPlan.Link anchored, Vec3 center, int lastPulse, int pulses) {
        if (cast.passive || anchored != null || group.effects.stream().noneMatch(e -> e.effect.kind() == EffectKind.HARMFUL)
            || !finite(center) || lastPulse < 0 || EMITTERS.size() >= MAXIMUM) return null;
        Emitter emitter = new Emitter(cast, center, lastPulse, pulses);
        EMITTERS.put(emitter.id, emitter);
        return emitter;
    }
    static long clock(ServerLevel level) { return level.getServer().overworld().getGameTime(); }
    static boolean finite(Vec3 at) { return Double.isFinite(at.x) && Double.isFinite(at.y) && Double.isFinite(at.z); }
    static boolean loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
            && !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
    }
    /** Resident chunks and resolved identical ward identities are checked before any block clip. */
    static boolean clear(ServerPlayer player, Vec3 to) {
        ServerLevel level = player.level(); Vec3 from = player.getEyePosition();
        if (!finite(from) || !finite(to) || from.distanceToSqr(to) > ExciseRules.RANGE * ExciseRules.RANGE) return false;
        if (!loaded(level, player.blockPosition())) return false;
        var ward = DungeonWards.movementWard(level, player.blockPosition());
        if (!ward.known()) return false;
        var cells = RelayGeometry.cells(from.x, from.y, from.z, to.x, to.y, to.z);
        if (cells.isEmpty()) return false;
        for (var cell : cells) {
            BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
            if (!loaded(level, pos) || !ward.equals(DungeonWards.movementWard(level, pos))) return false;
        }
        var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < .0004;
    }
    static boolean eligible(ServerPlayer player, Emitter emitter) {
        if (emitter == null || EMITTERS.get(emitter.id) != emitter || !emitter.targetable() || emitter.cast.level != player.level()
            || !loaded(player.level(), emitter.cast.caster.blockPosition())
            || player.level().getEntity(emitter.cast.caster.getUUID()) != emitter.cast.caster
            || !Targets.canHarm(emitter.cast.caster, player) || !Targets.canHarm(player, emitter.cast.caster)) return false;
        return clear(player, emitter.core);
    }
    static Emitter select(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(), aim = player.getLookAngle();
        if (!finite(eye) || !finite(aim)) return null;
        Emitter selected = null; double closest = Double.POSITIVE_INFINITY;
        for (Emitter emitter : List.copyOf(EMITTERS.values())) {
            Vec3 delta = emitter.core.subtract(eye);
            double along = delta.dot(aim);
            if (along < 0 || along > ExciseRules.RANGE || delta.subtract(aim.scale(along)).lengthSqr() > ExciseRules.CORE_RADIUS * ExciseRules.CORE_RADIUS
                || along >= closest || !eligible(player, emitter)) continue;
            selected = emitter; closest = along;
        }
        return selected;
    }
    static boolean cut(ServerPlayer player, Emitter emitter) {
        if (!eligible(player, emitter)) return false;
        emitter.excised = true; emitter.retired = true;
        EMITTERS.remove(emitter.id, emitter);
        return true;
    }
    static List<Emitter> snapshot() { return List.copyOf(EMITTERS.values()); }
    public static void init() {
        dev.wildercord.net.ExciseCores.init();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            EMITTERS.values().removeIf(e -> e.cast.level.getServer() == server && !e.targetable());
            if (server.overworld().getGameTime() % 4 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!ServerPlayNetworking.canSend(player, dev.wildercord.net.ExciseCores.TYPE)) continue;
                var visible = EMITTERS.values().stream().filter(e -> e.cast.level == player.level() && e.core.distanceToSqr(player.position()) <= 48 * 48
                    && loaded(player.level(), BlockPos.containing(e.core)))
                    .sorted(java.util.Comparator.comparingDouble(e -> e.core.distanceToSqr(player.position())))
                    .limit(MAX_VISIBLE).map(e -> new dev.wildercord.net.ExciseCores.Core(e.id, e.core, e.expires - clock(e.cast.level))).toList();
                ServerPlayNetworking.send(player, new dev.wildercord.net.ExciseCores(visible));
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> EMITTERS.entrySet().removeIf(e -> e.getValue().cast.level.getServer() == server));
    }
}
