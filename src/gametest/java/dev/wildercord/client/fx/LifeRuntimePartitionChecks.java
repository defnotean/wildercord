package dev.wildercord.client.fx;

import dev.wildercord.cast.ExcisePlayableTest;
import dev.wildercord.cast.LifeOutcomes;
import dev.wildercord.client.ExciseClient;
import dev.wildercord.spell.ExciseRules;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A catalogue/grammar contract, never evidence that the separate held Excise runtime was executed. */
public final class LifeRuntimePartitionChecks {
    private LifeRuntimePartitionChecks() {}
    public static final String EXCISE_ORDINARY = "dev.wildercord.cast.ExcisePlayableTest";
    public static final String EXCISE_PRESENTATION = "dev.wildercord.client.ExciseClient";
    public static final String REQUIRED_SUITE = "diagnostic-life-excise";

    /** All original 31 generic preparations/flights remain mandatory; exactly Excise uses its own route. */
    public static List<String> genericIds() {
        verify();
        return LifeForms.RUNES.stream().map(path -> "wildercord:" + path).sorted().toList();
    }
    public static Set<String> genericPaths() {
        verify();
        return Set.copyOf(LifeForms.RUNES);
    }
    public static void verify() {
        Set<String> generic = Set.copyOf(LifeForms.RUNES);
        check(LifeForms.RUNES.size() == 31 && generic.size() == 31, "Keep all 31 reviewed generic Life preparations/flights");
        check(dev.wildercord.cast.FlightBodies.LIFE.size() == 31 && Set.copyOf(dev.wildercord.cast.FlightBodies.LIFE).equals(generic), "Server and client retain the same exact generic31 flight roster");
        check(!dev.wildercord.cast.FlightBodies.supportsLife(ExciseRules.ID)
            && !dev.wildercord.cast.FlightBodies.covers("wildercord:heal," + ExciseRules.ID), "Mixed generic/custom identity cannot claim generic flight coverage");
        check(!generic.contains("excise"), "Excise cannot silently enter generic Bolt/formation coverage");
        var runtime = Runes.all().stream().filter(r -> r.family() == RuneFamily.EFFECT && r.element().equals("life"))
            .map(r -> r.path()).collect(java.util.stream.Collectors.toSet());
        var complete = new HashSet<>(generic); complete.add("excise");
        check(runtime.size() == 32 && runtime.equals(complete), "Runtime Life32 must be exactly generic31 plus dedicated Excise1");
        var dedicated = new HashSet<>(runtime); dedicated.removeAll(generic);
        check(dedicated.equals(Set.of("excise")), "Only the exact registered Excise takes the custom held route");
        check(Runes.get(ExciseRules.ID).orElseThrow() == Runes.EXCISE, "Dedicated partition names the registered rune");
        var exact = SpellCompiler.compile(ExciseRules.RUNES);
        check(!exact.isEmpty() && exact.warnings().isEmpty() && exact.root().groups.size() == 1
            && exact.root().groups.getFirst().shape == Runes.BEAM && exact.root().link == null
            && exact.root().groups.getFirst().effects.size() == 1 && exact.root().groups.getFirst().effects.getFirst().effect == Runes.EXCISE
            && Math.abs(exact.cost() - 36) < 1e-9, "Excise's only active grammar is the native Beam + Excise pair");
        for (var shape : List.of(Runes.BOLT, Runes.SELF, Runes.ZONE, Runes.TOUCH)) {
            var refused = SpellCompiler.compile(List.of(shape, Runes.EXCISE));
            check(refused.isEmpty() && refused.cost() == 0 && refused.warnings().contains(ExciseRules.GRAMMAR_PROBLEM),
                "Generic " + shape.name() + " + Excise is a refusal, never a successful gallery cast");
        }
        var stored = SpellCompiler.compileStored(ExciseRules.RUNES);
        check(stored.isEmpty() && stored.cost() == 0 && stored.warnings().contains(ExciseRules.STORAGE_PROBLEM), "Storage cannot carry the held lesson");
        check(!dev.wildercord.aura.RuneEtchingRules.accepts(Runes.EXCISE) && dev.wildercord.aura.RuneEtchingRules.price(Runes.EXCISE) == -1,
            "Etched-blade storage refuses Excise before pricing/payment");
        check(!LifeForms.supports(ExciseRules.ID), "Excise has no generic material preparation");
        var invented = new ArrayList<Object>();
        LifeForms.prepare(ExciseRules.ID, 1, 1, Vec3.ZERO, new Vec3(1,0,0), new Vec3(0,1,0), new Vec3(0,0,1), false, (o,p) -> invented.add(o));
        LifeFlights.draw(ExciseRules.ID, 4, 1, 1, Vec3.ZERO, new Vec3(0,0,1), false, (o,p) -> invented.add(o));
        check(invented.isEmpty(), "Generic Life preparation/flight must not fabricate an Excise success");
        boolean refusedOutcome = false;
        try { new LifeOutcomes.Observation("excise", LifeOutcomes.Moment.APPLY, Vec3.ZERO, null, 1, 1, 0); }
        catch (IllegalArgumentException expected) { refusedOutcome = true; }
        check(refusedOutcome, "Excise outcomes belong to its held core state, not generic Life effect observations");
        check(FabricClientGameTest.class.isAssignableFrom(ExcisePlayableTest.class) && ExcisePlayableTest.class.getName().equals(EXCISE_ORDINARY)
            && ExciseClient.class.getName().equals(EXCISE_PRESENTATION), "The separately mandatory ordinary class and custom client route remain wired");
    }
    /** Focused contract execution only: does not launch a native player, render a frame or prove the held action. */
    public static void main(String[] args) {
        verify();
        System.out.println("LIFE_RUNTIME_PARTITION_CONTRACT generic=31 dedicated=1 total=32; required=" + REQUIRED_SUITE
            + "; ordinary=" + EXCISE_ORDINARY + "; presentation=" + EXCISE_PRESENTATION + "; native_and_visual_execution=unproved");
    }
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
}
