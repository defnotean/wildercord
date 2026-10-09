package dev.wildercord.client.fx;

import dev.wildercord.content.AirflowOption;
import dev.wildercord.content.EarthOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Recipe-only regression shared by the Wind and field-fusion native suites. */
final class ThresherwindRecipeChecks {
    private static final String ID = "wildercord:thresherwind";
    private static final Vec3 FORWARD = new Vec3(0, 0, 1);
    private static final Vec3 RIGHT = new Vec3(-1, 0, 0);
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private record Emission(ParticleOptions option, Vec3 position) {}

    private ThresherwindRecipeChecks() {}

    static void verify() {
        for (boolean minimal : new boolean[] {false, true}) {
            int lanes = minimal ? 2 : 3;
            for (int age : new int[] {4, 8, 12}) {
                var flight = new ArrayList<Emission>();
                WindForms.flight(ID, age, 1, 1, Vec3.ZERO, FORWARD, minimal,
                    (option, position) -> flight.add(new Emission(option, position)));
                check(flight.size() == (minimal ? 10 : 15),
                    "Thresherwind flight count minimal=" + minimal + " age=" + age + " actual=" + flight.size());
                ingredients(flight, lanes, minimal ? 1 : 2, lanes, 5, minimal);
                var petals = materials(flight, MaterialOption.PETAL);
                check(petals.stream().noneMatch(e -> near(e.position.x, 0)), "Flight omits only the central leaf");
                if (!minimal) {
                    Vec3 a = petals.getFirst().position, b = petals.getLast().position;
                    check(near(a.x, -b.x) && near(a.y, b.y) && near(a.z, b.z) && Math.abs(a.x) > .09,
                        "Full flight retains symmetric outer leaves");
                }
                var direct = new ArrayList<Emission>();
                check(FieldFusionForms.flight(ID, age, 1, 1, Vec3.ZERO, FORWARD, minimal,
                    (option, position) -> direct.add(new Emission(option, position))), "Field flight dispatch");
                check(flight.equals(direct), "Wind and field flight retain the same recipe");
                var travel = new ArrayList<Emission>();
                check(FieldFusionForms.travel(ID, age, 1, Vec3.ZERO, RIGHT, UP, FORWARD, minimal,
                    (option, position) -> travel.add(new Emission(option, position))), "Field travel dispatch");
                check(flight.equals(travel), "Alternate travel retains the same budget and ingredients");
            }
            for (int beat : new int[] {1, 2}) {
                var preparation = new ArrayList<Emission>();
                check(FieldFusionForms.prepare(ID, beat, 1, Vec3.ZERO, RIGHT, UP, FORWARD, minimal,
                    (option, position) -> preparation.add(new Emission(option, position))), "Field preparation dispatch");
                int grain = minimal || beat == 1 ? 2 : 3;
                check(preparation.size() == (minimal ? 11 : beat == 1 ? 15 : 16),
                    "Preparation keeps its existing count minimal=" + minimal + " beat=" + beat);
                ingredients(preparation, lanes, lanes, grain, 12, minimal);
                check(materials(preparation, MaterialOption.PETAL).stream().filter(e -> near(e.position.x, 0)).count() == 1,
                    "Preparation retains the central leaf at both beats and qualities");
            }
        }
    }

    private static void ingredients(List<Emission> emissions, int lanes, int petals, int grain,
                                    int lifetime, boolean minimal) {
        int air = 0, roots = 0, grit = 0;
        for (var emission : emissions) {
            check(Double.isFinite(emission.position.lengthSqr()) && emission.position.length() < 1,
                "Compact finite Thresherwind body");
            switch (emission.option) {
                case AirflowOption flow -> {
                    air++;
                    check(flow.lifetime() == lifetime && flow.minimal() == minimal, "Airflow lifetime and quality");
                }
                case EarthOption earth -> {
                    check(earth.lifetime() == lifetime, "Stalk and grain lifetime");
                    if (earth.style() == EarthOption.ROOT) roots++;
                    else if (earth.style() == EarthOption.GRIT) grit++;
                    else throw new AssertionError("Unexpected Thresherwind earth material");
                }
                case MaterialOption material -> {
                    check(material.lifetime() == lifetime, "Supporting material lifetime");
                    check(material.style() == MaterialOption.WIND || material.style() == MaterialOption.PETAL,
                        "Only authored Wind and Life supporting materials");
                }
                default -> throw new AssertionError("Unexpected Thresherwind particle");
            }
        }
        check(air == lanes, "Every authored blade lane remains");
        check(roots == 2 * lanes, "Every lane retains both stalk fragments");
        check(grit == grain, "Every released grain remains");
        var wind = materials(emissions, MaterialOption.WIND);
        check(wind.size() == 1 && near(wind.getFirst().position.x, 0), "Central Wind material identity remains");
        check(materials(emissions, MaterialOption.PETAL).size() == petals, "Life leaf identity remains");
    }

    private static List<Emission> materials(List<Emission> emissions, int style) {
        return emissions.stream().filter(e -> e.option instanceof MaterialOption m && m.style() == style).toList();
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1.0e-9; }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Allows the same native assertions to run against the real recipe without launching a client. */
    public static void main(String[] args) {
        verify();
        System.out.println("Thresherwind recipes passed: Full flight=15, Minimal flight=10; preparation and ingredients retained");
    }
}
