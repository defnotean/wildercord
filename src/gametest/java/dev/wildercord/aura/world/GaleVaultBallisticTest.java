package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.gametest.galevault.GaleVaultProbe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Native one-impulse feasibility, not an ordinary attack, paid acceptance, renderer proof or damage implementation. */
public final class GaleVaultBallisticTest implements FabricClientGameTest {
    private SwordMaster master;
    private Mob target;
    private GaleVaultProbe.Trial trial;
    private Vec3 origin;
    private float targetHealth, masterHealth;

    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            try {
                context.waitTicks(40);
                for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
                    world.getServer().runCommand(command);
                world.getServer().runOnServer(server -> {
                    ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                    check(player.connection != null && player.connection.hasClientLoaded(), "The native client has loaded the world");
                    origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(player.level(), origin.x + 4, origin.y + 4, origin.z + 9, Set.of(), 180, 20, false);
                    Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.GaleVaultBallisticTest\",\"seed\":\"{}\"}", server.overworld().getSeed());
                });
                success(world, false);
                success(world, true);
                refusals(world);
                exitHazards(world);
                cancelledTell(world);
                crowded(world);
                collision(world, true);
                collision(world, false);
                knockedBack(world);
                noAi(world);
                falling(world);
                removed(world);
                Wildercord.LOGGER.info("GALE_VAULT_FEASIBILITY native_physics=true ordinary_attack=false selector_integration=false payment=false damage=false accepted_attack_count_unchanged=true");
            } finally {
                world.getServer().runOnServer(server -> clean());
            }
        } finally { origin = null; GaleVaultProbe.assertIdle(); }
        Wildercord.LOGGER.info("GALE_VAULT_BALLISTIC_COMPLETE native_physics=true ordinary_attack=false selector_integration=false payment=false damage=false accepted_attack_count_unchanged=true cleanup=complete");
    }

    private void start(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            clean();
            var level = server.overworld();
            for (int x = -5; x <= 10; x++) for (int z = -5; z <= 5; z++) {
                for (int y = -10; y <= 8; y++) level.setBlockAndUpdate(at(x, y, z), y == -1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
            target = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            check(master != null && target != null, "Registered native bodies exist");
            master.setDiscipline(MastersRules.GALE);
            master.snapTo(origin.x, origin.y, origin.z, -90, 0);
            target.setNoAi(true); target.setPersistenceRequired();
            target.snapTo(origin.x + 2.8, origin.y, origin.z, 90, 0);
            level.addFreshEntity(master); level.addFreshEntity(target);
            master.setTarget(target);
            trial = GaleVaultProbe.own(master, target);
            targetHealth = target.getHealth(); masterHealth = master.getHealth();
        });
        world.getServer().waitFor(server -> master.onGround() && master.verticalCollisionBelow, 30);
    }
    private void arm(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            trial.arm();
            check(!trial.cancelled && trial.armedAt >= 0, "The test-only fixed trajectory has lawful resident geometry");
            check(trial.impulses == 0, "Admission does not launch before its complete ground tell");
        });
    }
    private void launch(TestSingleplayerContext world) {
        world.getServer().waitFor(server -> trial.launchedAt >= 0, 30);
        world.getServer().runOnServer(server -> check(trial.launchedAt == trial.armedAt + GaleVaultProbe.TELL,
            "Exactly fourteen server ticks precede the single launch"));
    }
    private void complete(TestSingleplayerContext world) {
        world.getServer().waitFor(server -> trial.phase == GaleVaultProbe.Phase.COMPLETE, 130);
    }
    private void success(TestSingleplayerContext world, boolean movingTarget) {
        start(world); arm(world); launch(world);
        if (movingTarget) world.getServer().runOnServer(server -> {
            // Target fixture relocation only. The launched Master's body and velocity are never corrected.
            target.snapTo(origin.x - 2, origin.y, origin.z + 4, 90, 0);
            check(trial.direction.distanceToSqr(new Vec3(1, 0, 0)) < .000001, "The fixed eastward aim does not follow its moved target");
        });
        complete(world);
        world.getServer().runOnServer(server -> {
            check(trial.result == GaleVaultProbe.Result.VALID_GROUND_CONTACT && !trial.cancelled, "Only a real supported landing qualifies");
            check(trial.impulses == 1 && trial.airborneObserved && trial.landedAt - trial.launchedAt + 1 == 17,
                "One bounded impulse traverses seventeen native movement ticks");
            check(trial.apex > 2.7 && trial.apex < 2.9 && trial.maximumFallDistance > 2.5, "Real vertical position and fall history describe the arc");
            check(trial.dryReleases == 1 && trial.dryReleaseAt == trial.landedAt + 6, "Six full landing ticks precede one harmless future-phase marker");
            check(trial.recoveryUntil >= trial.armedAt + 80 && trial.recoveryUntil >= trial.dryReleaseAt + 36, "Reservation and post-release recovery have independent floors");
            var landing = trial.steps.stream().filter(s -> s.tick() == trial.landedAt).findFirst().orElseThrow();
            check(landing.requested().y < 0 && landing.ground() && landing.below(), "Landing is observed after actual downward native collision");
            check(trial.steps.stream().filter(s -> !s.ground()).allMatch(s -> Math.abs(s.after().z - origin.z) < .00001), "No airborne homing enters the locked trajectory");
            counterplay(); unchanged(); trial.report(movingTarget ? "target_moved_no_homing" : "flat_native_contact");
        });
    }
    private void counterplay() {
        Vec3 landing = trial.predictedSettled;
        check(trial.futureFanContains(landing.add(-1.5, 0, 0)), "Dry future fan has one low reverse interior");
        check(!trial.futureFanContains(landing.add(-1.5, .651, 0)), "A future timed jump above the exact feet ceiling is safe");
        check(!trial.futureFanContains(landing.add(-1.5, 0, 2.7)) && !trial.futureFanContains(landing.add(-1.5, 0, -2.7)), "Both supported side corridors leave the dry future fan");
        check(!trial.futureFanContains(origin) && !trial.futureFanContains(landing.add(.6, 0, 0)), "Takeoff underpass and behind-pad space are outside the future fan");
        check(!trial.futureFanContains(landing.add(-.449, 0, 0)) && !trial.futureFanContains(landing.add(-2.401, 0, 0)), "Near and far radial limits do not expand");
    }
    private void refusals(TestSingleplayerContext world) {
        for (String scenario : List.of("ceiling", "wall", "slab", "edge", "void", "hazard", "body_hazard", "exit_wall", "border", "resident_halo")) {
            start(world);
            world.getServer().runOnServer(server -> {
                var level = server.overworld(); var border = level.getWorldBorder();
                double oldSize = border.getSize(), oldX = border.getCenterX(), oldZ = border.getCenterZ();
                try {
                    switch (scenario) {
                        case "ceiling" -> level.setBlockAndUpdate(at(2, 3, 0), Blocks.STONE.defaultBlockState());
                        case "wall" -> { for (int y = 0; y < 5; y++) level.setBlockAndUpdate(at(2, y, 0), Blocks.STONE.defaultBlockState()); }
                        case "slab" -> level.setBlockAndUpdate(at(4, -1, 0), Blocks.STONE_SLAB.defaultBlockState());
                        case "edge" -> level.setBlockAndUpdate(at(4, -1, 0), Blocks.AIR.defaultBlockState());
                        case "void" -> { for (int x = 2; x <= 8; x++) for (int z = -3; z <= 3; z++) level.setBlockAndUpdate(at(x, -1, z), Blocks.AIR.defaultBlockState()); }
                        case "hazard" -> level.setBlockAndUpdate(at(4, -1, 0), Blocks.MAGMA_BLOCK.defaultBlockState());
                        case "body_hazard" -> level.setBlockAndUpdate(at(2, 2, 0), Blocks.COBWEB.defaultBlockState());
                        case "exit_wall" -> { for (int y = 0; y < 2; y++) level.setBlockAndUpdate(at(3, y, 3), Blocks.STONE.defaultBlockState()); }
                        case "border" -> { border.setCenter(origin.x, origin.z); border.setSize(8); }
                        case "resident_halo" -> {
                            AABB distant = new AABB(1_000_000, 181, 1_000_000, 1_000_001, 184, 1_000_001);
                            check(level.getChunkSource().getChunkNow(62500, 62500) == null, "The distant negative-control chunk is actually absent");
                            check(GaleVaultProbe.residentBounds(level, distant) == GaleVaultProbe.Result.UNLOADED_GEOMETRY, "Absent collision halo fails before any block read");
                            check(level.getChunkSource().getChunkNow(62500, 62500) == null, "The refusal never loads the absent chunk");
                            unchanged(); trial.report(scenario); return;
                        }
                    }
                    trial.arm();
                    check(trial.cancelled && trial.impulses == 0 && trial.dryReleases == 0, "Unsafe " + scenario + " refuses before launch");
                    if (scenario.equals("border")) check(trial.result == GaleVaultProbe.Result.OUTSIDE_BORDER, "Border refusal is explicit");
                    unchanged(); trial.report("refused_" + scenario);
                } finally { border.setCenter(oldX, oldZ); border.setSize(oldSize); }
            });
        }
    }
    private void exitHazards(TestSingleplayerContext world) {
        for (int side : new int[]{-1, 1}) for (boolean fluid : new boolean[]{false, true}) {
            start(world);
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                check(trial.terrain() == null, "The exit negative starts with an otherwise lawful arc and both safe exits");
                BlockPos hazard = at(3, 0, side * 3);
                var state = (fluid ? Blocks.WATER : Blocks.COBWEB).defaultBlockState();
                AABB exit = new AABB(trial.predictedSettled.x - 2.7, origin.y, origin.z + side * 2.7 - .4,
                    trial.predictedSettled.x + .4, origin.y + 2, origin.z + side * 2.7 + .4);
                check(exit.contains(Vec3.atCenterOf(hazard)), "The negative block is inside the specified side-exit body");
                check(trial.predicted.stream().noneMatch(point -> master.getType().getDimensions().makeBoundingBox(point).intersects(new AABB(hazard))),
                    "The negative block is outside the native arc, so an arc-only scan cannot reject it");
                check(level.getBlockState(hazard.below()).is(Blocks.STONE), "The side exit retains ordinary full stone support");
                level.setBlockAndUpdate(hazard, state);
                check(level.getBlockState(hazard).getCollisionShape(level, hazard).isEmpty(), "The exit hazard cannot be caught by collision geometry");
                check(!fluid || !level.getFluidState(hazard).isEmpty(), "The fluid negative contains actual native water");
                trial.arm();
                check(trial.cancelled && trial.result == GaleVaultProbe.Result.FLUID_OR_MOTION_CHANGE,
                    "Either side's fluid or collisionless hazard explicitly refuses the unsafe exit");
                check(trial.impulses == 0 && trial.dryReleases == 0, "Unsafe side exits refuse before launch or any future-phase marker");
                check(level.getBlockState(hazard.below()).is(Blocks.STONE), "Refusal leaves the side-exit support unchanged");
                unchanged(); trial.report("refused_exit_" + (side < 0 ? "negative_" : "positive_") + (fluid ? "water" : "cobweb"));
            });
        }
    }
    private void cancelledTell(TestSingleplayerContext world) {
        start(world); arm(world);
        world.getServer().runOnServer(server -> trial.abort(GaleVaultProbe.Result.INTERRUPTED));
        complete(world);
        world.getServer().runOnServer(server -> {
            check(trial.impulses == 0 && trial.dryReleases == 0 && trial.cancelled, "A tell cancellation never launches or substitutes an attack");
            check(trial.recoveryUntil >= trial.armedAt + 80, "An accepted tell cancellation keeps the reservation floor");
            unchanged(); trial.report("accepted_tell_interrupted");
        });
    }
    private void crowded(TestSingleplayerContext world) {
        start(world);
        world.getServer().runOnServer(server -> {
            var bodies = new java.util.ArrayList<Mob>();
            try {
                // Eight native full bodies cover takeoff, apex, pad and both lateral exits; one is a bystander.
                double[][] places = {{.9, 0}, {1.4, 0}, {2.8, 0}, {4.1, 0}, {3.2, -2.7}, {2.2, -2.7}, {3.2, 2.7}, {2.2, 2.7}};
                for (double[] place : places) {
                    Mob body = EntityTypes.VILLAGER.create(server.overworld(), EntitySpawnReason.COMMAND);
                    check(body != null, "A native crowd body exists");
                    body.setNoAi(true); body.snapTo(origin.x + place[0], origin.y, origin.z + place[1], 0, 0);
                    server.overworld().addFreshEntity(body); bodies.add(body);
                }
                trial.arm();
                check(trial.cancelled && trial.impulses == 0 && trial.dryReleases == 0, "A blocked full-body route cannot buy an arc or squeeze past bystanders");
                check(bodies.stream().allMatch(body -> body.getHealth() == body.getMaxHealth()), "All eight native crowd bodies stay unharmed");
                unchanged(); trial.report("eight_bodies_and_bystander_refusal");
            } finally { bodies.forEach(Mob::discard); }
        });
    }
    private void collision(TestSingleplayerContext world, boolean ceiling) {
        start(world); arm(world); launch(world);
        world.getServer().runOnServer(server -> {
            var level = server.overworld();
            if (ceiling) {
                for (int x = 1; x <= 4; x++) for (int z = -1; z <= 1; z++) level.setBlockAndUpdate(at(x, 3, z), Blocks.STONE.defaultBlockState());
            } else {
                for (int y = 0; y <= 5; y++) for (int z = -1; z <= 1; z++) level.setBlockAndUpdate(at(2, y, z), Blocks.STONE.defaultBlockState());
            }
        });
        world.getServer().waitFor(server -> ceiling ? trial.ceilingObserved : trial.wallObserved, 35);
        world.getServer().waitFor(server -> trial.landedAt >= 0, 50);
        complete(world);
        world.getServer().runOnServer(server -> {
            check(trial.cancelled && trial.dryReleases == 0 && trial.impulses == 1, "A native collision irreversibly cancels the future phase");
            check(ceiling ? trial.ceilingObserved : trial.wallObserved, "The obstacle was encountered by native Entity.move");
            check(trial.maximumFallDistance > 0, "Collision abort retains real descent history");
            unchanged(); trial.report(ceiling ? "dynamic_native_ceiling" : "dynamic_native_wall");
        });
    }
    private void knockedBack(TestSingleplayerContext world) {
        start(world); arm(world); launch(world);
        world.getServer().waitFor(server -> trial.steps.size() >= 4, 20);
        world.getServer().runOnServer(server -> {
            Vec3 before = master.getDeltaMovement();
            master.knockback(.7, 0, -1, master.damageSources().mobAttack(target), 0);
            Vec3 after = master.getDeltaMovement();
            check(trial.cancelled && trial.nativeKnockbacks == 1 && after.z > before.z + .1, "A real native midflight knockback stays effective");
            check(after.y == before.y, "Airborne native knockback retains its vertical component");
        });
        world.getServer().waitFor(server -> trial.landedAt >= 0, 60); complete(world);
        world.getServer().runOnServer(server -> {
            check(master.getZ() > origin.z + .5 && trial.dryReleases == 0 && trial.impulses == 1, "Cancelled flight preserves lateral displacement and cannot rearm");
            unchanged(); trial.report("native_midflight_knockback");
        });
    }
    private void noAi(TestSingleplayerContext world) {
        start(world); arm(world); launch(world);
        world.getServer().waitFor(server -> trial.steps.size() >= 4, 20);
        Vec3[] paused = new Vec3[2]; int[] travel = new int[1]; double[] fall = new double[1];
        world.getServer().runOnServer(server -> {
            paused[0] = master.position(); paused[1] = master.getDeltaMovement(); travel[0] = trial.travelCalls; fall[0] = master.fallDistance;
            master.setNoAi(true);
            check(trial.cancelled && trial.result == GaleVaultProbe.Result.NO_AI, "NoAI immediately and permanently cancels the attempt");
        });
        world.getServer().waitFor(server -> trial.suspendedObserved && server.overworld().getGameTime() >= trial.launchedAt + 10, 30);
        world.getServer().runOnServer(server -> {
            check(master.position().equals(paused[0]) && master.getDeltaMovement().equals(paused[1]) && master.fallDistance == fall[0], "Native NoAI suspends travel, gravity and fall accumulation");
            check(trial.travelCalls == travel[0], "No substitute travel runs during NoAI");
            master.setNoAi(false);
        });
        world.getServer().waitFor(server -> trial.landedAt >= 0, 80); complete(world);
        world.getServer().runOnServer(server -> {
            check(trial.dryReleases == 0 && trial.impulses == 1 && trial.result == GaleVaultProbe.Result.NO_AI, "Resume permits native falling but never revives the attempt");
            unchanged(); trial.report("no_ai_native_suspension_resume");
        });
    }
    private void falling(TestSingleplayerContext world) {
        start(world); arm(world); launch(world);
        world.getServer().runOnServer(server -> {
            for (int x = -3; x <= 9; x++) for (int z = -4; z <= 4; z++) {
                master.level().setBlockAndUpdate(at(x, -1, z), Blocks.AIR.defaultBlockState());
                master.level().setBlockAndUpdate(at(x, -9, z), Blocks.STONE.defaultBlockState());
            }
        });
        world.getServer().waitFor(server -> trial.landedAt >= 0, 80); complete(world);
        world.getServer().runOnServer(server -> {
            check(trial.cancelled && trial.dryReleases == 0 && trial.maximumFallDistance > 9 && trial.flightTimedOut, "Missing pad continues native falling and cannot arm a strike");
            check(trial.fallCalls > 0 && trial.rejectedFallCalls == trial.fallCalls && master.getHealth() == masterHealth,
                "Existing SwordMaster attribution policy rejects the real unattributed native fall damage call");
            check(master.getY() == origin.y - 8 && trial.recoveryUntil >= trial.landedAt + 36, "A lower native landing starts its own full harmless recovery");
            unchanged(); trial.report("removed_pad_native_fall_damage_filter");
        });
    }
    private void removed(TestSingleplayerContext world) {
        start(world); arm(world); launch(world);
        world.getServer().runOnServer(server -> {
            master.discard();
            check(trial.cancelled && trial.result == GaleVaultProbe.Result.REMOVED && trial.dryReleases == 0, "Removal invalidates the exact owned attempt");
            trial.report("removed_midair"); clean(); GaleVaultProbe.assertIdle();
        });
    }
    private void unchanged() {
        check(target.getHealth() == targetHealth && master.getHealth() == masterHealth, "The proof never grants aerial or landing damage");
        check(master.auraRemaining() == trial.auraBefore && master.attackAnimation() == 0, "No payment or ordinary move ID is claimed");
    }
    private BlockPos at(int x, int y, int z) { return BlockPos.containing(origin).offset(x, y, z); }
    private void clean() {
        GaleVaultProbe.close();
        if (master != null) master.discard(); if (target != null) target.discard();
        master = null; target = null; trial = null;
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
