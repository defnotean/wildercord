package dev.wildercord.gametest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Mechanical isolation and native-seam guards; these are not substitute trajectory runtime evidence. */
final class GaleVaultIsolationTest {
    private static final Path ROOT = Path.of("src/gametest/java/dev/wildercord/gametest/galevault");
    @Test void productionCannotReferenceTheExperiment() throws IOException {
        try (var files = Files.walk(Path.of("src/main"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".json")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("GaleVault") || source.contains("galevault") || source.contains("gale_vault"), file.toString());
            }
        }
        String descriptor = Files.readString(Path.of("src/gametest/resources/fabric.mod.json"));
        assertEquals(1, descriptor.split("dev.wildercord.aura.world.GaleVaultBallisticTest", -1).length - 1);
        assertEquals(1, descriptor.split("gale-vault-ballistic-gametest.mixins.json", -1).length - 1);
    }
    @Test void ownedProbeHasOneImpulseAndNoMotionOrDamageRepair() throws IOException {
        String source = Files.readString(ROOT.resolve("GaleVaultProbe.java"));
        assertEquals(1, source.split("master.setDeltaMovement\\(", -1).length - 1);
        for (String forbidden : List.of("master.move(", "master.setPos(", "master.teleport", "master.snapTo(",
            "setOnGround(", "resetFallDistance(", "setNoGravity(", "fallDistance =", "hurtServer(", "projected(", "whenLanded("))
            assertFalse(source.contains(forbidden), forbidden);
        assertTrue(source.contains("master.verticalCollisionBelow"));
        assertTrue(source.contains("requested.y < 0"));
        assertTrue(source.contains("getChunkNow(x, z) == null"));
        assertFalse(source.contains("getChunk(") || source.contains("hasChunk("));
    }
    @Test void onlyOwnedAiInputsMayBeCancelled() throws IOException {
        String mob = Files.readString(ROOT.resolve("mixin/GaleVaultMobMixin.java"));
        assertTrue(mob.indexOf("if (!GaleVaultProbe.owns(mob)) return;") < mob.indexOf("ci.cancel()"));
        assertTrue(mob.contains("mob.xxa = 0; mob.yya = 0; mob.zza = 0;"));
        for (String name : List.of("GaleVaultLivingMixin.java", "GaleVaultEntityMixin.java", "GaleVaultMasterMixin.java")) {
            String source = Files.readString(ROOT.resolve("mixin/" + name));
            assertFalse(source.contains("ci.cancel()") || source.contains("setReturnValue") || source.contains("@Redirect") || source.contains("@Overwrite"));
        }
    }
    @Test void passiveFallSeamForwardsExactlyOneOriginalCallAndCannotReplaceMeasurements() throws IOException {
        String mixin = Files.readString(ROOT.resolve("mixin/GaleVaultEntityMixin.java"));
        assertEquals(4, mixin.split("original.call\\(", -1).length - 1); // One owned and one unowned branch for each of the two original methods.
        assertTrue(mixin.contains("fallTrace.callback(() -> original.call(block, sourceLevel, state, pos, entity, argument)"));
        assertTrue(mixin.contains("fallTrace.scope(() -> original.call(actualY, ground, state, pos)"));
        assertFalse(mixin.contains("block.fallOn(") || mixin.contains("resetFallDistance(") || mixin.contains("causeFallDamage("));
        String probe = Files.readString(ROOT.resolve("GaleVaultProbe.java"));
        String observation = probe.substring(probe.indexOf("public FallOnReceipt beforeOriginalFallOn"), probe.indexOf("public void beforeMove"));
        assertTrue(observation.contains("entity != master || sourceLevel != level || block != state.getBlock() || !moveInFlight || launchedAt < 0"));
        assertFalse(observation.contains("maximumFallDistance =") || observation.contains("fallDistance =") || observation.contains("nativeFallReceipts.getLast()"));
        String fixture = Files.readString(Path.of("src/gametest/java/dev/wildercord/aura/world/GaleVaultBallisticTest.java"));
        assertTrue(fixture.contains("check(trial.maximumFallDistance > 2.5,"));
        assertTrue(fixture.contains("check(trial.apex > 2.7 && trial.apex < 2.9,"));
        assertTrue(fixture.indexOf("flat_native_contact_before_assertions") < fixture.indexOf("check(trial.result =="));
        assertTrue(fixture.indexOf("completed.report(\"before_cleanup\")") < fixture.indexOf("GaleVaultProbe.close()"));
    }
    @Test void exactObservationHelperPreservesFailuresAndNestedInvocationIdentity(@TempDir Path output) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        assertEquals(0, compiler.run(null, null, null, "--release", "25", "-proc:none", "-d", output.toString(),
            ROOT.resolve("GaleVaultFallTrace.java").toString(), ROOT.resolve("GaleVaultFallTraceChecks.java").toString()));
        try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var checks = loader.loadClass("dev.wildercord.gametest.galevault.GaleVaultFallTraceChecks");
            checks.getMethod("verify").invoke(null);
        }
    }
    @Test void pinnedNativeFallOnReceivesAccumulationBeforeTheOriginalReset() throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream("net/minecraft/world/entity/Entity.class")) {
            assertNotNull(stream);
            var model = ClassFile.of().parse(stream.readAllBytes());
            List<String> calls = new ArrayList<>();
            model.methods().stream().filter(m -> m.methodName().equalsString("checkFallDamage")).findFirst().orElseThrow().code().orElseThrow()
                .forEach(element -> { if (element instanceof InvokeInstruction call) calls.add(call.owner().asInternalName() + "." + call.name().stringValue()); });
            String fallOn = "net/minecraft/world/level/block/Block.fallOn", reset = "net/minecraft/world/entity/Entity.resetFallDistance";
            assertEquals(1, calls.stream().filter(fallOn::equals).count());
            assertEquals(1, calls.stream().filter(reset::equals).count());
            assertTrue(calls.indexOf(fallOn) < calls.indexOf(reset));
        }
    }
    @Test void pinnedNativeAiStepStillGatesTravelAndPushesAfterIt() throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream("net/minecraft/world/entity/LivingEntity.class")) {
            assertNotNull(stream);
            var model = ClassFile.of().parse(stream.readAllBytes());
            List<String> calls = new ArrayList<>();
            model.methods().stream().filter(m -> m.methodName().equalsString("aiStep")).findFirst().orElseThrow().code().orElseThrow()
                .forEach(element -> { if (element instanceof InvokeInstruction call) calls.add(call.name().stringValue()); });
            assertTrue(calls.lastIndexOf("isEffectiveAi") < calls.indexOf("travel") && calls.lastIndexOf("isEffectiveAi") >= 0);
            assertTrue(calls.indexOf("travel") < calls.indexOf("pushEntities") && calls.indexOf("pushEntities") >= 0);
        }
    }
}
