package dev.wildercord.gametest;

import org.junit.jupiter.api.Test;
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
