package dev.wildercord.client.fx;

import dev.wildercord.cast.FlightBodies;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Twenty authored storm flight forms: charge has a conductor, a shape, and a material purpose. */
final class StormFlights {
    private StormFlights() {}
    static final List<String> RUNES = FlightBodies.STORM;
    static boolean supports(String id) { return FlightBodies.supportsStorm(id); }

    static void draw(String id, int age, double scale, double length, Vec3 head, Vec3 velocity,
                     boolean minimal, BiConsumer<ParticleOptions, Vec3> emit) {
        if (!supports(id) || !Double.isFinite(velocity.lengthSqr())) return;
        var forward = velocity.lengthSqr() < .0001 ? new Vec3(0, 0, 1) : velocity.normalize();
        var right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < .0001 ? new Vec3(1, 0, 0) : right.normalize();
        var p = new Pen(head, right, right.cross(forward).normalize(), forward.scale(Math.clamp(length, 1, 2)),
            Math.clamp(scale, .4, 2), emit);
        double t = age * .31;
        switch (id.substring(11)) {
            case "shock" -> { // An unequal conductor fork advances toward two different contacts.
                double contact = (age / 3 % 2 == 0) ? -.18 : .1;
                p.arc(0xFFF3C6, 0, -.12, -.2, contact, .09, .1);
                p.line(0xE1ECFF, 0, -.12, -.2, .1, .2, .04, .019);
                p.dot(MaterialOption.STORM, 0xFFEDB8, -.18, .09, .1, .065);
            }
            case "jolt" -> { // A broken clamp carries interruption across its empty center.
                double gap = .08 + .07 * Math.abs(Math.sin(t * 1.7));
                for (int side : new int[]{-1, 1}) {
                    p.line(0xB2D6F8, side * gap, .17, -.04, side * gap, -.14, .04, .022);
                    p.line(0xB2D6F8, side * gap, -.14, .04, side * .055, -.14, .04, .022);
                }
                p.dot(MaterialOption.STORM, 0x96BFEF, 0, .03, .07, .07);
            }
            case "lightning" -> { // A forward leader meets two staggered return branches.
                double leader = -.12 + .36 * ((age % 6) / 5.0);
                p.arc(0xFFFCE7, 0, .09, -.35, 0, -.04, leader);
                p.line(0xD2E1F8, -.17, -.13, -.09, 0, -.04, .14, .018);
                p.line(0xD2E1F8, .15, .14, -.18, 0, -.04, .14, .018);
                p.dot(MaterialOption.STORM, 0xFFFFFF, 0, -.04, .14, .075);
            }
            case "thunderclap" -> { // Pressure brackets widen about a small flash before the crack.
                double span = .11 + .13 * ((age % 8) / 7.0);
                for (int side : new int[]{-1, 1}) {
                    p.line(0xD7E5FC, side * .08, -.16, -.08, side * span, 0, 0, .026);
                    p.line(0xD7E5FC, side * span, 0, 0, side * .08, .16, -.08, .026);
                }
                p.dot(MaterialOption.STORM, 0xFFFBE1, 0, 0, .06, .09);
            }
            case "ripple" -> { // A quiet half-sun carries warm light, never electrical fragments.
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI * (.12 + i * .25) + .12 * Math.sin(t * .45);
                    p.line(0xFFE4A1, Math.cos(a) * .08, Math.sin(a) * .08, 0,
                        Math.cos(a) * .21, Math.sin(a) * .21, -.04, .015);
                }
                p.dot(MaterialOption.ARCANE, 0xFFF1BE, 0, -.055, .04, .07);
            }
            case "thunderbird" -> { // Branching feathers beat about a charged beak and narrow tail.
                double lift = .05 * Math.sin(t);
                for (int side : new int[]{-1, 1}) for (int i = 0; i < (minimal ? 1 : 2); i++) {
                    p.line(0xD8ECFF, side * .055, 0, .04, side * (.23 + i * .1), .08 + lift, -.1 - i * .1, .02);
                }
                p.line(0xFFF2B9, 0, 0, .19, 0, -.08, -.23, .02);
                p.dot(MaterialOption.STORM, 0xECF6FF, 0, 0, .15, .06);
            }
            case "stormheart" -> { // Charged heart lobes shelter behind a serrated lower guard.
                for (int side : new int[]{-1, 1})
                    p.dot(MaterialOption.STORM, 0xA6C5F6, side * (.07 + .035 * Math.pow(Math.max(0, Math.sin(t * 1.3)), 2)), .06, 0, .085);
                p.line(0xDAE9FF, -.18, -.07, -.02, -.06, -.14, .05, .02);
                p.line(0xDAE9FF, -.06, -.14, .05, .06, -.09, .05, .02);
                p.line(0xDAE9FF, .06, -.09, .05, .18, -.14, -.02, .02);
            }
            case "galvanize" -> { // Three red terminals ride a copper contact plate.
                p.dot(MaterialOption.STONE, 0xAC7456, 0, -.1, -.05, .12);
                p.line(0xD49570, -.2, -.12, -.02, .2, -.12, -.02, .025);
                for (int i = -1; i <= 1; i++) {
                    p.line(0xFF9B76, i * .12, -.06, -.04, i * .12, .14, .05, .018);
                    p.dot(MaterialOption.STORM, 0xF25D3F, i * .12, .12, .05, i == age / 2 % 3 - 1 ? .07 : .025);
                }
            }
            case "tempest" -> { // Wind bends the outward charge and its returning leader.
                p.dot(MaterialOption.WIND, 0xBEDDD4, Math.cos(t) * .13, Math.sin(t) * .13, -.12, .14);
                p.arc(0xD4F4ED, -.18, -.1, -.15, .18, .12, .09);
                p.line(0xB4DACF, .18, .12, .09, .11, -.1, -.05, .017);
                p.dot(MaterialOption.STORM, 0xEAFBF2, .17, .07 + .03 * Math.sin(t), .04, .055);
            }
            case "plasma" -> { // Two electrical rails heat a carried flame inside a violet sheath.
                for (int side : new int[]{-1, 1}) {
                    p.line(0xC797EF, side * .12, -.025, -.28, side * .09, .025, .17, .02);
                    p.dot(MaterialOption.STORM, 0xD7B2F8, side * .1, .04 * Math.sin(t + side), -.04, .055);
                }
                p.dot(MaterialOption.EMBER, 0xF3ADD0, 0, 0, -.12 + .3 * ((age % 6) / 5.0), .085 + .025 * Math.abs(Math.sin(t * 2)));
            }
            case "surge" -> { // Living growth conducts current to two lifted contact tips.
                p.line(0x7CCB91, 0, -.2, -.1, 0, .04, .04, .023);
                p.dot(MaterialOption.PETAL, 0x71C78B, -.08 - .025 * Math.sin(t * .7), -.07, -.04, .08);
                for (int side : new int[]{-1, 1}) {
                    p.line(0xD4F4BC, 0, .04, .04, side * .15, .17, -.04, .018);
                    p.dot(MaterialOption.STORM, 0xD7F7BF, side * .15, .17, -.04, .05);
                }
            }
            case "magnetize" -> { // Filings pull toward an iron core between opposed open field loops.
                for (int side : new int[]{-1, 1}) {
                    p.line(0xE1C382, side * .08, -.15, 0, side * .21, 0, -.03, .015);
                    p.line(0xE1C382, side * .21, 0, -.03, side * .08, .15, 0, .015);
                    p.dot(MaterialOption.STONE, 0x8D959B, side * (.21 - .15 * ((age % 10) / 9.0)), 0, .04, .055);
                }
                p.dot(MaterialOption.STORM, 0xEAD79D, 0, -.02, .04, .045);
                p.dot(MaterialOption.STONE, 0x747E88, 0, 0, 0, .075);
            }
            case "riftbolt" -> { // A dark slit parts around a charged, jagged leading seam.
                p.dot(MaterialOption.VOID, 0x4F3267, 0, 0, -.03, .07 + .08 * Math.abs(Math.sin(t * .6)));
                for (int side : new int[]{-1, 1})
                    p.line(0xBA8CDE, side * .12, -.18, -.1, side * .05, .18, .04, .016);
                p.arc(0xE1C8F7, -.06, -.14, -.09, .04, .13, .1);
                p.dot(MaterialOption.STORM, 0xCA9AF4, .04, .13, .1, .045);
            }
            case "stormweave" -> { // Four marked nodes tighten a charged crossing stitch.
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 4 + i * Math.PI / 2 + .16 * Math.sin(t + i), r = .17 + .025 * Math.sin(t + i * Math.PI);
                    p.dot(MaterialOption.ARCANE, 0xD5A2D4, Math.cos(a) * r, Math.sin(a) * r, -.04, .045);
                }
                p.line(0xEABAEF, -.12, -.12, -.04, .12, .12, .04, .018);
                p.line(0xEABAEF, -.12, .12, -.04, .12, -.12, .04, .018);
                p.dot(MaterialOption.STORM, 0xF4DBFC, 0, 0, .04, .06);
            }
            case "stormclock" -> { // A charged hand advances between three appointment marks.
                for (int i = 0; i < 3; i++) {
                    double a = -Math.PI / 2 + i * Math.PI * 2 / 3;
                    p.dot(MaterialOption.TIME, 0xD7BC7F, Math.cos(a) * .19, Math.sin(a) * .19, -.04, .055);
                }
                p.arc(0xF5E4B4, 0, 0, 0, Math.cos(t * .4) * .16, Math.sin(t * .4) * .16, .02);
                p.dot(MaterialOption.STORM, 0xDDE9FA, 0, 0, .08, .055);
            }
            case "thunderhead" -> { // A heavy cloud sheds rain beside its own downward leader.
                for (int i = -1; i <= 1; i++)
                    p.dot(MaterialOption.VAPOUR, i == 0 ? 0x748494 : 0xA3B0BE, i * .12, .11, -.06, .09);
                for (int side : new int[]{-1, 1})
                    p.dot(MaterialOption.WATER, 0x82B6CF, side * .12, .015 - .22 * (((age + (side > 0 ? 3 : 0)) % 8) / 7.0), .01, .04);
                p.arc(0xE1EFFF, .04, .05, .01, -.03, -.19, .08);
                p.dot(MaterialOption.STORM, 0xD2E5FA, -.03, -.15, .06, .045);
            }
            case "frostwire" -> { // Three crystalline contacts carry a narrow running current.
                for (int i = -1; i <= 1; i++)
                    p.dot(MaterialOption.FROST, 0xADDEF0, i * .16, i == 0 ? .06 : -.04, -.04, .065);
                p.arc(0xDDF7FF, -.16, -.04, -.04, .16, -.04, .04);
                p.dot(MaterialOption.STORM, 0xC5EDFA, -.16 + .32 * ((age % 8) / 7.0), .01, .04, .045);
            }
            case "thunderstep" -> { // A copper-lit toe marks the end of a broken arrival leader.
                p.line(0xD8E9FA, -.13, .17, -.22, .02, -.03, .08, .022);
                p.line(0xFFF0B8, -.1, -.16, -.09, .12, -.16, -.09, .02);
                p.line(0xFFF0B8, .12, -.16, -.09, .16, -.16, .12, .02);
                p.line(0xFFF0B8, .16, -.16, .12, -.06, -.16, .12, .02);
                p.dot(MaterialOption.STORM, 0xEBF7FF, .1, .12 - .24 * ((age % 6) / 5.0), .1, .06);
            }
            case "thunder_tide" -> { // Charged brackets suspend two real-water beads in flight.
                for (int side : new int[]{-1, 1}) {
                    double x = side * (.1 + .035 * Math.sin(t + side));
                    p.dot(MaterialOption.WATER, 0x6CBBCF, x, 0, .04, .085);
                    p.line(0xC3EEED, x - .06, -.13, -.04, x - .06, .13, .04, .015);
                    p.line(0xC3EEED, x - .06, .13, .04, x + .06, .13, .04, .015);
                    p.dot(MaterialOption.STORM, 0xDEF7F5, x, .13, .04, .04);
                }
            }
            case "thunder_walk" -> { // Three charged copper footings travel over a supporting wind.
                for (int i = 0; i < 3; i++) {
                    double lift = i == age / 2 % 3 ? .05 * Math.sin(t) : 0;
                    p.dot(MaterialOption.STONE, 0xB27D59, i * .12 - .12, i * .07 - .1 + lift, .1 - i * .13, .07);
                    p.dot(MaterialOption.STORM, 0xF7EAB8, i * .12 - .12, i * .07 - .04 + lift, .1 - i * .13, .035);
                }
                p.dot(MaterialOption.WIND, 0xBFDDD1, 0, -.14, -.18, .1);
            }
            default -> { }
        }
    }

    private record Pen(Vec3 head, Vec3 right, Vec3 up, Vec3 forward, double scale,
                       BiConsumer<ParticleOptions, Vec3> emit) {
        Vec3 at(double x, double y, double z) {
            return head.add(right.scale(x * scale)).add(up.scale(y * scale)).add(forward.scale(z * scale));
        }
        void dot(int style, int color, double x, double y, double z, double size) {
            emit.accept(new MaterialOption(style, color, (float)Math.clamp(size * scale, .02, .8), 5), at(x, y, z));
        }
        void line(int color, double x, double y, double z, double a, double b, double c, double width) {
            stroke(LightOption.RAY, color, x, y, z, a, b, c, width);
        }
        void arc(int color, double x, double y, double z, double a, double b, double c) {
            stroke(LightOption.ARC, color, x, y, z, a, b, c, .018);
        }
        void stroke(int kind, int color, double x, double y, double z, double a, double b, double c, double width) {
            var start = at(x, y, z);var d = at(a, b, c).subtract(start);
            emit.accept(new LightOption(kind, color, (float)d.x, (float)d.y, (float)d.z, (float)(width * scale),
                kind == LightOption.ARC ? 1 : 0, 0, kind == LightOption.ARC ? .2F : 0, 5), start);
        }
    }
}
