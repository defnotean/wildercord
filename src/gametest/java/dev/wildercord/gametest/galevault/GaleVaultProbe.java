package dev.wildercord.gametest.galevault;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.SwordMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Test-only mechanics experiment. Never pays Aura, registers a move, arms damage, or simulates entity travel. */
public final class GaleVaultProbe {
    public static final int TELL = 14, MAX_AIR = 24, LANDING_TELL = 6, RECOVERY = 36, RESERVATION = 80;
    public static final double FORWARD = .72, UP = .66;
    public enum Result {
        NOT_LAUNCHED, AIRBORNE, VALID_GROUND_CONTACT, BLOCKED_TAKEOFF, CEILING_CONTACT,
        WALL_CONTACT, BODY_OR_EXTERNAL_IMPULSE, FLUID_OR_MOTION_CHANGE, UNSAFE_SUPPORT,
        OUTSIDE_ACCEPTED_PAD, UNLOADED_GEOMETRY, OUTSIDE_BORDER, OBSTRUCTED_SWEEP,
        FLIGHT_TIMEOUT, OWNER_INVALID, NO_AI, CLOCK_GAP, INTERRUPTED, REMOVED
    }
    public enum Phase { GROUND_TELL, AIRBORNE, LANDING_TELL, RECOVERY, ABORTED, COMPLETE }
    public record Step(long tick, Vec3 before, Vec3 requested, Vec3 after, Vec3 velocity,
                       boolean ground, boolean below, boolean ceiling, boolean wall,
                       double fallBefore, double fallAfter) {}
    private static Trial owned;
    private GaleVaultProbe() {}

    public static Trial own(SwordMaster master, LivingEntity target) {
        if (owned != null) throw new AssertionError("Gale probe already owns a body");
        owned = new Trial(master, target);
        return owned;
    }
    public static boolean owns(Entity entity) { return owned != null && owned.master == entity; }
    public static Trial current(Entity entity) { return owns(entity) ? owned : null; }
    public static void close() { owned = null; }
    public static void assertIdle() { if (owned != null) throw new AssertionError("Leaked Gale probe owner"); }

    public static final class Trial {
        public final SwordMaster master;
        public final LivingEntity target;
        public final ServerLevel level;
        public final Vec3 origin, direction, predictedContact, predictedSettled;
        public final List<Step> steps = new ArrayList<>();
        public final List<Vec3> predicted;
        public final double auraBefore;
        public Phase phase = Phase.GROUND_TELL;
        public Result result = Result.NOT_LAUNCHED;
        public long armedAt = -1, launchedAt = -1, landedAt = -1, dryReleaseAt = -1, recoveryUntil = -1;
        public int impulses, travelCalls, dryReleases, nativeKnockbacks, fallCalls, rejectedFallCalls;
        public boolean airborneObserved, cancelled, ceilingObserved, wallObserved, suspendedObserved, flightTimedOut;
        public double apex, maximumFallDistance;
        private long lastTick = -1;
        private Vec3 priorTickVelocity, postTravelVelocity, moveStart, requested;
        private double moveFall;
        private boolean moveInFlight, travelThisTick;

        private Trial(SwordMaster master, LivingEntity target) {
            this.master = master; this.target = target; this.level = (ServerLevel) master.level();
            origin = master.position();
            direction = target.position().subtract(origin).multiply(1, 0, 1).normalize();
            if (direction.distanceToSqr(new Vec3(1, 0, 0)) > .000001) throw new AssertionError("This isolated corridor fixture requires fixed eastward aim");
            predicted = predict(origin, direction);
            predictedContact = predicted.get(16);
            predictedSettled = predicted.get(22);
            auraBefore = master.auraRemaining();
        }
        public void arm() {
            if (armedAt >= 0 || impulses != 0 || cancelled) throw new AssertionError("A probe attempt cannot be rearmed");
            if (!master.onGround() || !master.verticalCollisionBelow) throw new AssertionError("Admission needs native downward support");
            Result refusal = terrain();
            if (refusal != null) { abort(refusal); return; }
            if (master.isNoAi() || master.getDeltaMovement().horizontalDistanceSqr() > .000009) {
                abort(master.isNoAi() ? Result.NO_AI : Result.BODY_OR_EXTERNAL_IMPULSE); return;
            }
            armedAt = level.getGameTime();
            recoveryUntil = armedAt + RESERVATION;
        }
        public void beforeTick() {
            long now = level.getGameTime();
            if (lastTick >= 0 && now != lastTick + 1) abort(Result.CLOCK_GAP);
            lastTick = now; travelThisTick = false; postTravelVelocity = null;
            if (master.isNoAi()) { abort(Result.NO_AI); suspendedObserved = true; }
            if (!master.isAlive() || master.isRemoved()) abort(Result.OWNER_INVALID);
            if (!cancelled && launchedAt >= 0 && priorTickVelocity != null
                && master.getDeltaMovement().distanceToSqr(priorTickVelocity) > .000001) abort(Result.BODY_OR_EXTERNAL_IMPULSE);
            if (!cancelled && armedAt >= 0 && launchedAt < 0) {
                Result refusal = terrain(); if (refusal != null) abort(refusal);
            }
        }
        /** Runs only when vanilla elected to call travel; NoAI can never enter this seam. */
        public void beforeTravel() {
            travelThisTick = true; travelCalls++;
            if (armedAt >= 0 && launchedAt < 0 && !cancelled && level.getGameTime() >= armedAt + TELL) {
                if (!master.onGround() || !master.verticalCollisionBelow) { abort(Result.BLOCKED_TAKEOFF); return; }
                if (master.getDeltaMovement().horizontalDistanceSqr() > .000009) { abort(Result.BODY_OR_EXTERNAL_IMPULSE); return; }
                launchedAt = level.getGameTime(); impulses++;
                // The sole authored velocity write. Every later movement/drag/gravity/collision is vanilla.
                master.setDeltaMovement(direction.scale(FORWARD).add(0, UP, 0));
                phase = Phase.AIRBORNE; result = Result.AIRBORNE;
            }
        }
        public void afterTravel() { postTravelVelocity = master.getDeltaMovement(); }
        public void beforeMove(MoverType type, Vec3 delta) {
            moveInFlight = launchedAt >= 0 && type == MoverType.SELF;
            if (!moveInFlight) return;
            moveStart = master.position(); requested = delta; moveFall = master.fallDistance;
        }
        public void afterMove() {
            if (!moveInFlight) return;
            moveInFlight = false;
            boolean ceiling = requested.y > 0 && master.verticalCollision && !master.verticalCollisionBelow;
            boolean wall = master.horizontalCollision;
            var step = new Step(level.getGameTime(), moveStart, requested, master.position(), master.getDeltaMovement(),
                master.onGround(), master.verticalCollisionBelow, ceiling, wall, moveFall, master.fallDistance);
            steps.add(step);
            apex = Math.max(apex, master.getY() - origin.y);
            maximumFallDistance = Math.max(maximumFallDistance, Math.max(moveFall, master.fallDistance));
            ceilingObserved |= ceiling; wallObserved |= wall;
            if (!master.onGround() && master.getY() > origin.y + .05) airborneObserved = true;
            if (ceiling) abort(Result.CEILING_CONTACT);
            if (wall) abort(Result.WALL_CONTACT);
            if (requested.y < 0 && master.onGround() && master.verticalCollisionBelow && airborneObserved) {
                if (landedAt < 0) {
                    landedAt = level.getGameTime();
                    recoveryUntil = Math.max(recoveryUntil, landedAt + RECOVERY);
                    if (!cancelled) {
                        Result support = support(master.getBoundingBox());
                        if (support != null) abort(support);
                        else if (horizontalDistance(master.position(), predictedContact) > .25 || Math.abs(master.getY() - predictedContact.y) > .05)
                            abort(Result.OUTSIDE_ACCEPTED_PAD);
                        else { result = Result.VALID_GROUND_CONTACT; phase = Phase.LANDING_TELL; }
                    }
                }
            }
        }
        public void afterTick() {
            if (launchedAt >= 0) {
                if (!cancelled && postTravelVelocity != null && master.getDeltaMovement().distanceToSqr(postTravelVelocity) > .000001)
                    abort(Result.BODY_OR_EXTERNAL_IMPULSE);
                if (!cancelled && (master.isInWater() || master.isInLava() || master.onClimbable() || master.isNoGravity()))
                    abort(Result.FLUID_OR_MOTION_CHANGE);
                if (landedAt < 0 && level.getGameTime() - launchedAt + 1 >= MAX_AIR) { flightTimedOut = true; abort(Result.FLIGHT_TIMEOUT); }
                if (!cancelled && (phase == Phase.AIRBORNE || phase == Phase.LANDING_TELL)) { Result refusal = terrain(); if (refusal != null) abort(refusal); }
                if (!cancelled && phase == Phase.LANDING_TELL && level.getGameTime() >= landedAt + LANDING_TELL) {
                    Result refusal = terrain();
                    if (refusal != null) abort(refusal);
                    else if (horizontalDistance(master.position(), predictedSettled) > .12 || !master.onGround() || !master.verticalCollisionBelow)
                        abort(Result.OUTSIDE_ACCEPTED_PAD);
                    else {
                        // A phase marker only: no damage, animation ID, Aura debit, cooldown or attack callback.
                        dryReleases++; dryReleaseAt = level.getGameTime(); phase = Phase.RECOVERY;
                        recoveryUntil = Math.max(recoveryUntil, dryReleaseAt + RECOVERY);
                    }
                }
            }
            if (armedAt >= 0 && cancelled && phase == Phase.ABORTED && master.onGround() && master.verticalCollisionBelow) phase = Phase.RECOVERY;
            if (phase == Phase.RECOVERY && level.getGameTime() >= recoveryUntil) phase = Phase.COMPLETE;
            if (master.isNoAi() && travelThisTick) throw new AssertionError("Probe bypassed native NoAI travel suspension");
            priorTickVelocity = master.getDeltaMovement();
        }
        public void abort(Result why) {
            if (cancelled || phase == Phase.COMPLETE) return;
            cancelled = true; result = why; phase = Phase.ABORTED;
            if (master.onGround()) recoveryUntil = Math.max(recoveryUntil, level.getGameTime() + RECOVERY);
            // In particular, do not stop a falling body or erase any component of a real knockback.
        }
        public void knockedBack() { nativeKnockbacks++; if (armedAt >= 0) abort(Result.BODY_OR_EXTERNAL_IMPULSE); }
        public void fallDamage(boolean accepted) { fallCalls++; if (!accepted) rejectedFallCalls++; }

        /** Conservative fixed corridor, both exits, and piecewise swept full bodies. Never loads chunks. */
        public Result terrain() {
            AABB corridor = new AABB(origin.x - 3, origin.y, origin.z - 3,
                origin.x + 8, origin.y + 6, origin.z + 3);
            Result bounds = residentBounds(level, corridor); if (bounds != null) return bounds;
            // This initial eastward fixture deliberately requires flat ordinary stone-like full support.
            for (int x = (int) Math.floor(corridor.minX); x < Math.ceil(corridor.maxX); x++)
                for (int z = (int) Math.floor(corridor.minZ); z < Math.ceil(corridor.maxZ); z++) {
                    Result support = support(new AABB(x + .01, origin.y, z + .01, x + .99, origin.y + 1.95, z + .99));
                    if (support != null) return support;
                }
            AABB previous = master.getType().getDimensions().makeBoundingBox(origin);
            for (Vec3 point : predicted) {
                AABB body = master.getType().getDimensions().makeBoundingBox(point);
                AABB swept = previous.minmax(body);
                Result hazard = bodyHazards(swept); if (hazard != null) return hazard;
                if (!level.noCollision(master, swept)) return Result.OBSTRUCTED_SWEEP;
                if (!level.getEntitiesOfClass(LivingEntity.class, swept, entity -> entity != master && entity.isAlive()).isEmpty())
                    return Result.BODY_OR_EXTERNAL_IMPULSE;
                previous = body;
            }
            for (double side : new double[]{-2.7, 2.7}) {
                AABB exit = new AABB(predictedSettled.x - 2.7, origin.y, origin.z + side - .4,
                    predictedSettled.x + .4, origin.y + 2, origin.z + side + .4);
                Result hazard = bodyHazards(exit); if (hazard != null) return hazard;
                if (!level.noCollision(master, exit) || !level.getEntitiesOfClass(LivingEntity.class, exit, entity -> entity != master).isEmpty())
                    return Result.OBSTRUCTED_SWEEP;
            }
            return null;
        }
        /** The same bounded body-volume check applies to every arc segment and to both escape corridors. */
        private Result bodyHazards(AABB body) {
            Result bounds = residentBounds(level, body); if (bounds != null) return bounds;
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY + .001, body.minZ),
                BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
                var state = level.getBlockState(pos);
                if (!state.getFluidState().isEmpty() || hazardous(state)) return Result.FLUID_OR_MOTION_CHANGE;
            }
            return null;
        }
        private Result support(AABB body) {
            if (Math.abs(body.minY - origin.y) > .05) return Result.UNSAFE_SUPPORT;
            AABB strip = new AABB(body.minX, origin.y - .05, body.minZ, body.maxX, origin.y, body.maxZ);
            Result bounds = residentBounds(level, strip); if (bounds != null) return bounds;
            int y = (int) Math.floor(origin.y - .05);
            for (int x = (int) Math.floor(body.minX + .001); x <= Math.floor(body.maxX - .001); x++)
                for (int z = (int) Math.floor(body.minZ + .001); z <= Math.floor(body.maxZ - .001); z++) {
                    BlockPos pos = new BlockPos(x, y, z); var state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty() || hazardous(state)) return Result.FLUID_OR_MOTION_CHANGE;
                    if (!Block.isShapeFullBlock(state.getCollisionShape(level, pos)) || Math.abs(state.getBlock().getFriction() - .6) > .0001)
                        return Result.UNSAFE_SUPPORT;
                }
            return null;
        }
        public boolean futureFanContains(Vec3 feet) {
            Vec3 relative = feet.subtract(predictedSettled);
            double radius = relative.horizontalDistance();
            return relative.y >= -.05 && relative.y <= .65 && radius >= .45 && radius <= 2.4
                && relative.multiply(1, 0, 1).normalize().dot(direction.scale(-1)) >= Math.cos(Math.PI / 4);
        }
        public void report(String scenario) {
            Wildercord.LOGGER.info("GALE_VAULT_BALLISTIC scenario={} result={} phase={} impulses={} travel={} flight={} apex={} contact={} settled={} fallDistance={} fallCalls={} rejectedFall={} dryReleases={} cancelled={} no_ai_suspended={} paid_ordinary_attack=false damage_enabled=false",
                scenario, result, phase, impulses, travelCalls, landedAt < 0 ? -1 : landedAt - launchedAt + 1,
                apex, landedAt < 0 ? "none" : steps.stream().filter(s -> s.tick == landedAt).findFirst().map(Step::after).orElse(null),
                master.position(), maximumFallDistance, fallCalls, rejectedFallCalls, dryReleases, cancelled, suspendedObserved);
            for (Step step : steps) Wildercord.LOGGER.info("GALE_VAULT_NATIVE_STEP scenario={} sample={}", scenario, step);
        }
    }
    public static Result residentBounds(ServerLevel level, AABB body) {
        // Collision shapes may extend from neighboring blocks: certify the entire one-block query halo before any read.
        AABB halo = body.inflate(1);
        if (halo.minY < level.getMinY() || halo.maxY >= level.getMaxY()) return Result.UNLOADED_GEOMETRY;
        if (!level.getWorldBorder().isWithinBounds(halo.minX, halo.minZ) || !level.getWorldBorder().isWithinBounds(halo.maxX, halo.maxZ))
            return Result.OUTSIDE_BORDER;
        for (int x = ((int) Math.floor(halo.minX)) >> 4; x <= ((int) Math.floor(halo.maxX)) >> 4; x++)
            for (int z = ((int) Math.floor(halo.minZ)) >> 4; z <= ((int) Math.floor(halo.maxZ)) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return Result.UNLOADED_GEOMETRY;
        return null;
    }
    private static boolean hazardous(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CLIMBABLE) || state.is(Blocks.MAGMA_BLOCK)
            || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.CACTUS)
            || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW)
            || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK)
            || state.is(Blocks.COBWEB);
    }
    /** An admission estimate to compare against native evidence; never moves an entity or repairs a discrepancy. */
    private static List<Vec3> predict(Vec3 start, Vec3 direction) {
        List<Vec3> points = new ArrayList<>(); double forward = 0, height = 0, vx = FORWARD, vy = UP;
        boolean ground = true;
        for (int tick = 0; tick < 23; tick++) {
            if (Math.abs(vx) < .003) vx = 0;
            if (Math.abs(vy) < .003) vy = 0;
            forward += vx; height += vy;
            boolean landed = height <= 0 && vy < 0;
            if (landed) { height = 0; vy = 0; }
            points.add(start.add(direction.scale(forward)).add(0, height, 0));
            vx *= ground ? (float) (.6F * .91F) : .91F;
            vy = (vy - .08) * .98F;
            ground = landed;
        }
        return List.copyOf(points);
    }
    private static double horizontalDistance(Vec3 a, Vec3 b) { return a.subtract(b).horizontalDistance(); }
}
