package dev.wildercord.client.fx;

import dev.wildercord.cast.FlightBodies;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiConsumer;

/** Authored moving ice and water bodies; each recipe keeps the rune's ingredients and silhouette. */
final class FrostFlights {
    private FrostFlights() {}
    static final List<String> RUNES = FlightBodies.FROST;
    static boolean supports(String id) { return FlightBodies.supportsFrost(id); }

    static void draw(String id, int age, double scale, double length, Vec3 head, Vec3 velocity,
                     boolean minimal, BiConsumer<ParticleOptions, Vec3> emit) {
        if (!supports(id) || !Double.isFinite(velocity.lengthSqr())) return;
        var forward = velocity.lengthSqr() < .0001 ? new Vec3(0, 0, 1) : velocity.normalize();
        var right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < .0001 ? new Vec3(1, 0, 0) : right.normalize();
        var p = new Pen(head, right, right.cross(forward).normalize(), forward.scale(Math.clamp(length, 1, 2)),
            Math.clamp(scale, .4, 2), emit);
        double t = age * .26;
        switch (id.substring(11)) {
            case "basinfill" -> { // A cupped parcel carries descending droplets, with no ice facets.
                for(int i=0;i<(minimal?3:5);i++){double a=i*1.256+t*.4;
                    p.dot(MaterialOption.WATER,0x61C6E8,Math.cos(a)*.16,-.06+Math.sin(a)*.07,-.15,.09);
                    p.dot(MaterialOption.WATER,0xC1F3FF,Math.cos(a)*.1,-.12-(age%4)*.035,-.3-i*.05,.045);}
            }
            case "chill" -> { // A breath crescent carries a fine frozen lower edge.
                for (int i = 0; i < (minimal ? 2 : 3); i++)
                    p.dot(MaterialOption.VAPOUR, 0xBFDCE6, i * .14 - .14, .04 + .04 * Math.sin(t + i), -i * .12, .1);
                p.line(0xD7F7FF, -.17, -.1, 0, .17, -.1, -.12, .018);
                p.dot(MaterialOption.FROST, 0xA4D5E7, 0, -.1, .03, .045);
            }
            case "frost" -> { // Forks grow from an advancing contact spine.
                p.line(0xB5EBFF, 0, -.18, -.12, 0, .2, .1, .022);
                for (int side : new int[]{-1, 1}) {
                    p.line(0xC6F3FF, 0, -.05, 0, side * .19, .07, -.07, .016);
                    p.dot(MaterialOption.FROST, 0xB0DFF4, side * .16, .06, -.05, .06);
                }
            }
            case "freeze" -> { // Opposed plates slide about a cold latch.
                double gap = .15 + .025 * Math.sin(t);
                for (int side : new int[]{-1, 1}) {
                    p.line(0xBFEAFF, side * gap, -.18, -.1, side * gap, .18, .04, .028);
                    p.dot(MaterialOption.FROST, 0x93D5F5, side * gap, 0, 0, .09);
                }
                p.line(0xECFCFF, -gap, -.18, -.1, gap, -.18, -.1, .018);
            }
            case "tidebreath" -> { // Two water lungs breathe around a trailing exhalation.
                for (int side : new int[]{-1, 1})
                    p.dot(MaterialOption.WATER, 0x7BCDE4, side * (.1 + .025 * Math.sin(t)), .02, 0, .11);
                p.dot(MaterialOption.VAPOUR, 0xD8EFF0, 0, .1, -.24, .12);
            }
            case "icepath" -> { // Three flat lozenges advance along a cold track.
                for (int i = 0; i < (minimal ? 2 : 3); i++) {
                    double z = .12 - i * .2;
                    p.line(0xBBE6ED, -.17, -.08, z, 0, -.08, z + .1, .017);
                    p.line(0xBBE6ED, 0, -.08, z + .1, .17, -.08, z, .017);
                }
                p.dot(MaterialOption.FROST, 0xDCF7FF, 0, -.08, .16, .055);
            }
            case "bubble" -> { // Six membrane seams round about a clear air pocket.
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3, b = a + Math.PI / 3, r = .2 + .015 * Math.sin(t);
                    p.line(0xB5E8EE, Math.cos(a) * r, Math.sin(a) * r, 0,
                        Math.cos(b) * r, Math.sin(b) * r, 0, .015);
                }
                p.dot(MaterialOption.WATER, 0x80CEDB, 0, -.18, 0, .045);
            }
            case "coldsnap" -> { // Two biting jaws snap around a detached crystal.
                for (int side : new int[]{-1, 1}) {
                    p.line(0xC6F4FF, side * .2, .13, -.12, side * .08, 0, .12, .025);
                    p.line(0xC6F4FF, side * .08, 0, .12, side * .2, -.13, -.12, .025);
                }
                p.dot(MaterialOption.FROST, 0xE8FCFF, 0, 0, .13, .06);
            }
            case "icicle" -> { // A long ice needle with three facets at its shoulder.
                p.line(0xF2FCFF, 0, 0, .28, 0, 0, -.42, .025);
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2 / 3;
                    p.dot(MaterialOption.FROST, 0x8AC4ED, Math.cos(a) * .1, Math.sin(a) * .1, -.16, .055);
                }
            }
            case "frostward" -> { // A curved rim shelters a heavy lower crystal.
                p.line(0xC8EFF9, -.2, .12, -.06, 0, .22, .02, .022);
                p.line(0xC8EFF9, 0, .22, .02, .2, .12, -.06, .022);
                p.line(0xB8E4F0, -.2, .12, -.06, 0, -.22, .06, .022);
                p.line(0xB8E4F0, .2, .12, -.06, 0, -.22, .06, .022);
                p.dot(MaterialOption.FROST, 0x92CBE4, 0, -.13, .04, .07);
            }
            case "hail" -> { // Charged hailstones exchange a short conducting fork.
                for (int i = 0; i < (minimal ? 2 : 3); i++)
                    p.dot(MaterialOption.FROST, 0xC9DDF3, i * .12 - .12, .07 * Math.sin(t + i), -i * .12, .085);
                p.dot(MaterialOption.STORM, 0xE5D5FF, 0, .08, -.07, .055);
                p.line(0xEEDFFF, -.13, .06, -.1, .08, -.08, .03, .014);
            }
            case "blizzard" -> { // Wind carries a staggered snow front and a pale trailing cloud.
                p.dot(MaterialOption.WIND, 0xA9CCD5, 0, 0, -.15, .15);
                for (int i = 0; i < (minimal ? 2 : 4); i++) {
                    double a = t + i * 1.8;
                    p.dot(MaterialOption.FROST, 0xE5F6FF, Math.cos(a) * .2, Math.sin(a) * .12, -i * .09, .055);
                }
                p.dot(MaterialOption.VAPOUR, 0xDAE7EB, 0, .05, -.38, .13);
            }
            case "glacier" -> { // Stone bears an asymmetric, ice-armored shoulder.
                p.dot(MaterialOption.STONE, 0x74848B, 0, -.06, 0, .16);
                p.dot(MaterialOption.FROST, 0xA4D5E5, -.08, .1, .04, .13);
                p.line(0xDAF5F7, -.19, .1, -.05, .13, .19, .05, .024);
            }
            case "mirrorfrost" -> { // A tilted reflective pane carries an arcane light behind it.
                double tilt = .03 * Math.sin(t);
                p.line(0xD5EFF8, -.16, -.18, -.04, -.16, .18, .04, .018);
                p.line(0xD5EFF8, -.16, .18, .04, .16, .18, tilt, .018);
                p.line(0xD5EFF8, .16, .18, tilt, .16, -.18, -.04, .018);
                p.line(0xD5EFF8, .16, -.18, -.04, -.16, -.18, -.04, .018);
                p.dot(MaterialOption.ARCANE, 0xBAAFE0, 0, 0, -.12, .06);
                p.dot(MaterialOption.FROST, 0xE5F8FF, -.12, .14, .03, .05);
            }
            case "black_ice" -> { // A dark core remains visible between fractured ice blades.
                p.dot(MaterialOption.VOID, 0x354364, 0, 0, 0, .14);
                for (int side : new int[]{-1, 1}) {
                    p.line(0x7295C9, side * .18, -.13, -.16, side * .1, .15, .06, .02);
                    p.dot(MaterialOption.FROST, 0xA6BAD9, side * .12, .1, .04, .05);
                }
            }
            case "frostbloom" -> { // Ice petals open around a living seed rather than a frozen orb.
                for (int i = 0; i < (minimal ? 3 : 5); i++) {
                    double a = t * .3 + i * Math.PI * 2 / (minimal ? 3 : 5);
                    p.dot(MaterialOption.FROST, 0xCEE8EF, Math.cos(a) * .18, Math.sin(a) * .18, -.08, .065);
                }
                p.dot(MaterialOption.PETAL, 0xA6DFA7, 0, 0, .04, .08);
            }
            case "rime_seal" -> { // An inset score turns inside opposed cold corner brackets.
                for (int side : new int[]{-1, 1}) {
                    p.line(0xA3D4E8, side * .17, -.17, 0, side * .17, .17, 0, .02);
                    p.line(0xA3D4E8, side * .17, .17, 0, side * .05, .17, 0, .02);
                }
                p.dot(MaterialOption.ARCANE, 0xA7BAE3, 0, .06 * Math.sin(t), 0, .055);
                p.dot(MaterialOption.FROST, 0xD5F0FF, 0, -.12, .04, .06);
            }
            case "cryostasis" -> { // Ice closes about a paused, living time pulse.
                for (int side : new int[]{-1, 1}) {
                    p.line(0xAED9F1, 0, .23, 0, side * .16, 0, -.06, .02);
                    p.line(0xAED9F1, side * .16, 0, -.06, 0, -.23, 0, .02);
                }
                p.dot(MaterialOption.TIME, 0xD5BB83, 0, .07, -.04, .055);
                p.dot(MaterialOption.PETAL, 0x99CDA4, 0, -.08, -.04, .06);
                p.dot(MaterialOption.FROST, 0xC8E9FA, 0, .2, 0, .045);
            }
            case "frostbite" -> { // Two opposed ice teeth retain a bleeding central score.
                for (int side : new int[]{-1, 1}) {
                    p.line(0xB8D3E6, side * .18, .12, -.05, side * .06, 0, .12, .025);
                    p.dot(MaterialOption.FROST, 0x9CBBD6, side * .15, .08, 0, .07);
                }
                p.dot(MaterialOption.BLOOD, 0xA84F6B, 0, -.07, -.05, .065);
            }
            case "absolute_zero" -> { // Four cold fronts pinch a small dark frozen point.
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    p.line(0x91B9E9, Math.cos(a) * .23, Math.sin(a) * .23, -.09,
                        Math.cos(a) * .065, Math.sin(a) * .065, .04, .017);
                }
                p.dot(MaterialOption.FROST, 0x263D66, 0, 0, .06, .085);
            }
            case "tidal_lift" -> { // Three water sources travel through a small arched aqueduct.
                for (int i = -1; i <= 1; i++)
                    p.dot(MaterialOption.WATER, 0x7ACDE7, i * .16, .14 - i * i * .1, -.1, .075);
                p.line(0xC3F0F7, -.2, -.06, 0, 0, .2, .06, .016);
                p.line(0xC3F0F7, 0, .2, .06, .2, -.06, 0, .016);
            }
            case "rime_causeway" -> { // Ascending ice courses retain the wind that supports them.
                for (int i = 0; i < 3; i++)
                    p.line(0xC1E3EE, -.17, i * .1 - .1, .12 - i * .16,
                        .17, i * .1 - .1, .12 - i * .16, .035);
                p.dot(MaterialOption.WIND, 0xC0DCE2, 0, -.15, -.22, .1);
                p.dot(MaterialOption.FROST, 0xD4F4FF, 0, .1, -.2, .06);
            }
            case "avalanche" -> { // Snow-capped rocks tumble through a loose powder wake.
                for (int i = 0; i < (minimal ? 2 : 3); i++) {
                    p.dot(MaterialOption.STONE, 0x859098, i * .13 - .13, .06 * Math.sin(t + i), -i * .1, .09);
                    p.dot(MaterialOption.FROST, 0xEDF8FC, i * .13 - .13, .07, -i * .1, .06);
                }
                p.dot(MaterialOption.VAPOUR, 0xDEE6EA, 0, -.04, -.3, .13);
            }
            case "tidecall" -> { // Two breakers curl toward a foaming meeting point.
                for (int side : new int[]{-1, 1}) {
                    p.dot(MaterialOption.WATER, 0x59B6D3, side * .16, .02, -.06, .11);
                    p.line(0xA7E4EF, side * .2, -.1, -.1, side * .05, .12, .05, .025);
                }
                p.dot(MaterialOption.VAPOUR, 0xDAF0EC, 0, .11, .02, .075);
            }
            case "undertow" -> { // A twisting downward water bore carries sediment beneath it.
                for (int i = 0; i < (minimal ? 2 : 4); i++) {
                    double a = t + i * 1.6;
                    p.dot(MaterialOption.WATER, 0x4E9FBE, Math.cos(a) * .14, .13 - i * .08, -i * .09, .08);
                }
                p.dot(MaterialOption.STONE, 0x807E70, 0, -.19, -.22, .095);
            }
            case "hoarfrost" -> { // A rime fern carries alternating forks along a slender stem.
                p.line(0xCFE8EC, -.13, -.18, -.12, .13, .18, .08, .015);
                for (int i = 0; i < 3; i++) {
                    double y = i * .12 - .12, x = i * .08 - .08;
                    p.line(0xA7D6DF, x, y, 0, x + (i % 2 == 0 ? .13 : -.13), y + .06, -.08, .014);
                }
                p.dot(MaterialOption.FROST, 0xE7F8FB, .12, .18, .08, .04);
            }
            case "drowning_word" -> { // Wet channels fill a broken breath funnel.
                for (int side : new int[]{-1, 1}) {
                    p.line(0x7BB8D5, side * .18, .15, -.08, side * .06, -.16, .03, .023);
                    p.dot(MaterialOption.WATER, 0x368EB8, side * .1, .04, 0, .075);
                }
                p.dot(MaterialOption.WATER, 0x83CCDF, 0, -.18, -.04, .06);
            }
            case "tidewrit" -> { // Three wall courses retain a folded, misting upper lip.
                for (int i = 0; i < 3; i++) {
                    p.line(0x6AC4D9, -.2, i * .12 - .12, 0, .2, i * .12 - .12, .035 * Math.sin(t), .025);
                }
                p.dot(MaterialOption.WATER, 0x48ACC5, 0, 0, .03, .075);
                p.dot(MaterialOption.VAPOUR, 0xD5E9EB, 0, .19, -.07, .1);
            }
            case "tidehook" -> { // A hooked water barb pulls a short doubled tether behind it.
                p.line(0x83D7E4, 0, 0, -.35, 0, 0, .16, .018);
                p.line(0xB7F0F4, 0, 0, .16, .16, .12, .04, .025);
                p.line(0xB7F0F4, .16, .12, .04, .2, -.04, -.02, .025);
                p.dot(MaterialOption.WATER, 0x63BAD2, .16, .05, .02, .06);
            }
            case "current" -> { // Three flow lanes converge into a forward thrust.
                for (int i = -1; i <= 1; i++) {
                    p.line(0xA0DDE5, i * .14, -.06, -.28, i * .05, .04, .2, .019);
                }
                p.dot(MaterialOption.WATER, 0x66C2D2, 0, .04, .16, .075);
            }
            case "flash_freeze" -> { // A wet bead becomes crystal between opposed cold blades.
                boolean wet = age % 8 < 4;
                p.dot(wet ? MaterialOption.WATER : MaterialOption.FROST, wet ? 0x72C5DD : 0xD5F3FF, 0, 0, 0, .1);
                p.line(0xB5E7F6, -.2, -.1, -.05, .2, -.1, .04, .018);
                p.line(0xB5E7F6, -.2, .1, -.05, .2, .1, .04, .018);
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
            var start = at(x, y, z);
            var d = at(a, b, c).subtract(start);
            emit.accept(new LightOption(LightOption.RAY, color, (float)d.x, (float)d.y, (float)d.z,
                (float)(width * scale), 0, 0, 0, 5), start);
        }
    }
}
