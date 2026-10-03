package dev.wildercord.cast;

import com.google.gson.GsonBuilder;
import dev.wildercord.client.fx.FrameBenchmark;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Sustained native material workload, with fresh arenas and measured intervals rather than FPS promises. */
public final class LivingWorldPerformanceTest implements FabricClientGameTest {
    private record Result(String profile, String scene, int admittedCasts, String frames, String server) {}

    @Override public void runTest(ClientGameTestContext c) {
        var settings = c.computeOnClient(BenchmarkSettings::capture);
        var camera = c.computeOnClient(mc -> mc.options.getCameraType());
        var results = new ArrayList<Result>();
        var observed = new java.util.concurrent.atomic.AtomicReference<net.minecraft.server.level.ServerLevel>();
        var admitted = new java.util.concurrent.atomic.AtomicInteger();
        dev.wildercord.api.WildercordEvents.AFTER_CAST.register((player, slot, runes, mana) -> {
            if (player.level() == observed.get()) admitted.incrementAndGet();
        });
        try {
            for (String profile : List.of("performance", "balanced", "cinematic")) {
                // Reopening a fresh world clears scheduled work; earlier profile effects do not leak into the next.
                try (var world = c.worldBuilder().create()) {
                    c.waitTicks(40);
                    world.getServer().runOnServer(s -> {
                        var p = s.getPlayerList().getPlayers().getFirst();
                        p.setGameMode(GameType.CREATIVE);
                        PracticeRoom.enter(p);
                        check(PracticeRoom.targets(p.level(), 24, true) == 24, "24 moving practice targets");
                        Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
                        var book = Spellbooks.get(p).withStarterGiven();
                        for (var rune : Runes.all()) book = book.learn(rune.id());
                        Spellbooks.set(p, book);
                        p.setYRot(0);
                        p.setXRot(0);
                        observed.set(p.level());
                        admitted.set(0);
                    });
                    c.waitTicks(60);
                    c.runOnClient(mc -> {
                        mc.getWindow().setWindowed(1280, 720);
                        mc.resizeGui();
                        mc.options.setCameraType(CameraType.FIRST_PERSON);
                        if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                        mc.options.enableVsync().set(false);
                        mc.options.framerateLimit().set(120);
                        mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
                        MagicQuality.preset(profile);
                        mc.gui.toastManager().clear();
                        mc.gui.hud.getChat().clearMessages(false);
                        FrameBenchmark.start();
                    });
                    world.getServer().runOnServer(s -> VisualMetrics.reset());
                    var effects = List.of(Runes.PELT, Runes.VENOM, Runes.EMBER, Runes.WINDCUT);
                    for (int cast = 0; cast < 64; cast++) {
                        var effect = effects.get(cast % effects.size());
                        world.getServer().runOnServer(s -> {
                            var p = s.getPlayerList().getPlayers().getFirst();
                            SpellCaster.edit(p, 0, List.of(Runes.BOLT.id(), effect.id()));
                            Spellbooks.setReadyAt(p, 0, 0);
                            SpellCaster.cast(p, 0);
                        });
                        c.waitTicks(10);
                    }
                    String frames = c.computeOnClient(mc -> FrameBenchmark.lastResult());
                    check(!frames.isEmpty(), "Completed fresh 30-second frame sample for " + profile);
                    check(admitted.get() == 64, "All 64 material casts admitted for " + profile + ": " + admitted.get());
                    String server = world.getServer().computeOnServer(s -> VisualMetrics.report());
                    results.add(new Result(profile, "24 moving dummies; 64 alternating Pelt/Venom/Ember/Windcut Bolts", admitted.get(), frames, server));
                    c.takeScreenshot(TestScreenshotOptions.of("living_world_benchmark_" + profile).disableCounterPrefix());
                }
            }
            try {
                Files.writeString(Path.of("living-world-performance.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(results) + "\n", StandardCharsets.UTF_8);
            } catch (java.io.IOException e) { throw new RuntimeException("Cannot preserve measured results", e); }
        } finally {
            observed.set(null);
            c.runOnClient(mc -> { settings.restore(mc); mc.options.setCameraType(camera); });
        }
        check(c.computeOnClient(BenchmarkSettings::capture).equals(settings), "All changed preferences restored");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
