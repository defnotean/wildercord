package dev.wildercord.cast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** GameTest-only, bounded observations of the original Harm/Stasis case. Never queries a second ray or moves a body. */
public final class StasisDeliveryProbe implements AutoCloseable {
    private static volatile StasisDeliveryProbe active;
    private static boolean installed;
    private static final ThreadLocal<Beam> BEAM = new ThreadLocal<>();
    private final ServerPlayer owner;
    private final ServerLevel level;
    private final LivingEntity target;
    private final String ownerId, targetId;
    private final Scope scope;
    private final Map<Object, String> paid = new IdentityHashMap<>();
    private volatile long serverTick;
    private LocalPlayer clientOwner;
    private ClientLevel clientLevel;
    private long lastClientTick = Long.MIN_VALUE;
    private int beams;

    /** Identity and time window are also exercised without Minecraft by StasisDeliveryProbeScopeChecks. */
    static final class Scope {
        final Object owner, world;
        final long opened;
        volatile boolean closed;
        Scope(Object owner, Object world, long opened) { this.owner = owner; this.world = world; this.opened = opened; }
        boolean accepts(Object owner, Object world, long tick) {
            return !closed && this.owner == owner && this.world == world && tick >= opened && tick - opened < 10;
        }
    }

    private StasisDeliveryProbe(ServerPlayer owner, LivingEntity target) {
        this.owner = owner; this.level = owner.level(); this.target = target;
        ownerId = owner.getUUID().toString(); targetId = target.getUUID().toString();
        serverTick = level.getServer().getTickCount(); scope = new Scope(owner, level, serverTick);
    }

    static StasisDeliveryProbe open(ServerPlayer owner, LivingEntity target, RuneDef attack) {
        if (!attack.equals(Runes.HARM)) return null;
        if (!installed) {
            installed = true;
            ServerTickEvents.END_SERVER_TICK.register(server -> {
                var probe = active;
                if (probe != null && server == probe.level.getServer()) probe.serverTick();
            });
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                var probe = active;
                if (probe != null) probe.clientTick(client);
            });
        }
        var probe = new StasisDeliveryProbe(owner, target);
        active = probe;
        return probe;
    }

    /** Called at the real paid Cast construction boundary, only inside the fixture's current cast attempt. */
    void paid(ServerPlayer player, List<RuneDef> runes, Cast cast) {
        if (!scope.accepts(player, player.level(), level.getServer().getTickCount()) || cast.caster != owner || cast.level != level
                || runes.size() != 2 || !runes.getFirst().equals(Runes.BEAM)
                || !(runes.get(1).equals(Runes.STASIS) || runes.get(1).equals(Runes.HARM))) return;
        paid.put(cast.identity(), runes.get(1).id());
        safe(() -> { var row = row("paid_cast"); row.addProperty("cast", id(cast.identity())); row.addProperty("effect", runes.get(1).id()); emit(row); });
    }

    private void serverTick() {
        serverTick = level.getServer().getTickCount();
        if (!scope.accepts(owner, owner.level(), serverTick) || level.getServer().getPlayerList().getPlayer(owner.getUUID()) != owner) return;
        safe(() -> {
            var row = row("server_motion"); row.add("ownerState", body(owner));
            row.addProperty("input", owner.getLastClientInput().toString());
            row.add("residentTerrain", terrain(level, owner.blockPosition())); emit(row);
        });
    }

    private void clientTick(Minecraft client) {
        long tick = serverTick;
        if (scope.closed || active != this || tick < scope.opened || tick - scope.opened >= 10 || tick == lastClientTick
                || client.getSingleplayerServer() != level.getServer() || client.player == null || client.level == null
                || client.player.level() != client.level
                || !client.player.getUUID().toString().equals(ownerId) || client.level.dimension() != level.dimension()) return;
        if (clientOwner == null) { clientOwner = client.player; clientLevel = client.level; }
        if (client.player != clientOwner || client.level != clientLevel) return;
        lastClientTick = tick;
        safe(() -> {
            var row = new JsonObject(); row.addProperty("event", "client_motion"); row.addProperty("case", "wildercord:harm");
            row.addProperty("owner", ownerId); row.addProperty("expectedTarget", targetId);
            row.addProperty("observedServerTick", tick); row.addProperty("clientGameTime", client.level.getGameTime());
            row.add("ownerState", body(client.player)); row.addProperty("input", client.player.input.keyPresses.toString());
            row.addProperty("sentInput", client.player.getLastSentInput().toString());
            row.addProperty("jumpDown", client.options.keyJump.isDown()); row.addProperty("shiftDown", client.options.keyShift.isDown());
            row.addProperty("forwardDown", client.options.keyUp.isDown()); row.addProperty("backDown", client.options.keyDown.isDown());
            row.addProperty("leftDown", client.options.keyLeft.isDown()); row.addProperty("rightDown", client.options.keyRight.isDown());
            row.add("residentTerrain", terrain(client.level, client.player.blockPosition())); emit(row);
        });
    }

    private static final class Beam {
        final StasisDeliveryProbe probe;
        final Cast cast;
        final Vec3 from, direction;
        BlockHitResult block;
        int hits;
        Beam(StasisDeliveryProbe probe, Cast cast, Vec3 from, Vec3 direction) {
            this.probe = probe; this.cast = cast; this.from = from; this.direction = direction;
        }
    }

    /** Wraps the actual Beam exactly once. Nested unrelated beams cannot attach their results to this one. */
    public static void beam(Cast cast, SpellPlan.Group group, Vec3 from, Vec3 direction, Runnable original) {
        Beam previous = BEAM.get();
        var probe = active;
        Beam observed = probe != null && probe.scope.accepts(cast.caster, cast.level, cast.level.getServer().getTickCount())
            && probe.paid.containsKey(cast.identity()) && group.shape.equals(Runes.BEAM)
            && group.effects.size() == 1 && group.effects.getFirst().effect.id().equals(probe.paid.get(cast.identity())) && probe.beams++ < 4
            ? new Beam(probe, cast, from, direction) : null;
        if (observed == null && previous == null) { original.run(); return; }
        BEAM.set(observed);
        boolean returned = false;
        try {
            if (observed != null) safe(() -> observed.probe.beamRow(observed, "beam_enter", false));
            original.run(); returned = true;
        } finally {
            try {
                boolean result = returned;
                if (observed != null) safe(() -> observed.probe.beamRow(observed, "beam_exit", result));
            } finally {
                if (previous == null) BEAM.remove(); else BEAM.set(previous);
            }
        }
    }

    /** Receives the original clip's returned object; never clips or accesses terrain again. */
    public static void clipped(BlockHitResult result) { var beam = BEAM.get(); if (beam != null) beam.block = result; }
    public static void hit(Cast cast, Cast.Hit hit) {
        var beam = BEAM.get();
        if (beam == null || beam.cast != cast) return;
        beam.hits++;
        safe(() -> {
            var row = beam.probe.row("beam_hit"); row.addProperty("cast", id(cast.identity()));
            row.addProperty("expectedTargetHit", hit.entities().stream().anyMatch(e -> e == beam.probe.target));
            row.addProperty("actualTargets", hit.entities().stream().map(e -> e.getUUID() + "/" + e.getId()).toList().toString());
            row.addProperty("point", hit.point().toString()); emit(row);
        });
    }

    private void beamRow(Beam beam, String event, boolean returned) {
        var row = row(event); row.addProperty("cast", id(beam.cast.identity())); row.addProperty("effect", paid.get(beam.cast.identity()));
        row.addProperty("from", beam.from.toString()); row.addProperty("direction", beam.direction.toString());
        row.addProperty("targetBounds", target.getBoundingBox().toString());
        row.addProperty("returned", returned); row.addProperty("hitCalls", beam.hits);
        if (beam.block != null) {
            row.addProperty("clipType", beam.block.getType().toString()); row.addProperty("clipLocation", beam.block.getLocation().toString());
            row.addProperty("clipBlock", beam.block.getBlockPos().toString());
            row.addProperty("rayIntersectsExpectedTarget", target.getBoundingBox().inflate(0.3).clip(beam.from, beam.block.getLocation()).isPresent()
                || target.getBoundingBox().contains(beam.from));
        }
        row.add("ownerState", body(owner)); emit(row);
    }

    private JsonObject row(String event) {
        var row = new JsonObject(); row.addProperty("event", event); row.addProperty("case", "wildercord:harm");
        row.addProperty("owner", ownerId); row.addProperty("expectedTarget", targetId);
        row.addProperty("serverTick", level.getServer().getTickCount()); row.addProperty("gameTime", level.getGameTime());
        row.addProperty("targetRegistered", level.getEntity(target.getUUID()) == target); return row;
    }

    private static JsonObject body(Player player) {
        var row = new JsonObject(); row.addProperty("position", player.position().toString()); row.addProperty("eye", player.getEyePosition().toString());
        row.addProperty("aim", player.getLookAngle().toString()); row.addProperty("velocity", player.getDeltaMovement().toString());
        row.addProperty("bounds", player.getBoundingBox().toString());
        row.addProperty("alive", player.isAlive()); row.addProperty("removed", player.isRemoved());
        row.addProperty("onGround", player.onGround()); row.addProperty("noGravity", player.isNoGravity());
        row.addProperty("mayfly", player.getAbilities().mayfly); row.addProperty("flying", player.getAbilities().flying);
        row.addProperty("flySpeed", player.getAbilities().getFlyingSpeed()); row.addProperty("inWater", player.isInWater());
        row.addProperty("inLava", player.isInLava()); row.addProperty("horizontalCollision", player.horizontalCollision);
        row.addProperty("verticalCollision", player.verticalCollision); row.addProperty("pose", player.getPose().toString());
        row.addProperty("effects", player.getActiveEffects().stream().map(Object::toString).sorted().toList().toString()); return row;
    }

    /** Only four existing cells at feet/support/head/above; missing chunks are reported without loading. */
    private static JsonArray terrain(Level level, BlockPos feet) {
        var rows = new JsonArray();
        for (int dy = -1; dy <= 2; dy++) {
            BlockPos at = feet.above(dy); LevelChunk chunk = level instanceof ServerLevel server
                ? server.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4)
                : ((ClientLevel) level).getChunkSource().getChunk(at.getX() >> 4, at.getZ() >> 4, ChunkStatus.FULL, false);
            var row = new JsonObject(); row.addProperty("position", at.toString()); row.addProperty("resident", chunk != null);
            if (chunk != null) { var state = chunk.getBlockState(at); row.addProperty("block", state.toString()); row.addProperty("fluid", state.getFluidState().toString()); }
            rows.add(row);
        }
        return rows;
    }

    private static String id(Object value) { return Integer.toHexString(System.identityHashCode(value)); }
    private static void emit(JsonObject row) { System.out.println("WILDERCORD_STASIS_DELIVERY " + row); }
    private static void safe(Runnable observation) { try { observation.run(); } catch (Throwable ignored) { /* Observations cannot replace gameplay or its exception. */ } }
    @Override public void close() { scope.closed = true; if (active == this) active = null; paid.clear(); }
}
