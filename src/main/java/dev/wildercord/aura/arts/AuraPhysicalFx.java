package dev.wildercord.aura.arts;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Light;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Sword impacts use cuts and floor damage. Spell-only elemental seals never enter this path. */
public final class AuraPhysicalFx {
	private AuraPhysicalFx() {}
	private static void cut(ServerLevel level, Vec3 at, int color, double radius, double tilt, double phase, double span, double width, int life) {
		Vec3 n = ElementFx.tilted(tilt, phase);
		Light.slash(level, at, n, ElementFx.inPlane(n, phase), color, radius, span, width, 2, life);
	}
	public static void fireImpact(ServerLevel level, Vec3 at, double size) {
		AuraFx.groundScar(level, at, 1.25 * size, 70, 0);
		for (int i = 0; i < 3; i++) cut(level, at.add(0, i * 0.1, 0), 0xE7A16D, size, 1.1, i * 2.1, 1.2, 0.06, 6 + i);
	}
	public static void frostImpact(ServerLevel level, Vec3 at, double size) {
		AuraFx.groundScar(level, at, 1.4 * size, 80, 2);
		for (int i = 0; i < 4; i++) cut(level, at, 0xACC1CE, size * 0.8, 0.5, i * Math.PI / 2, 0.8, 0.035, 8);
	}
	public static void stormImpact(ServerLevel level, Vec3 at, double size) {
		AuraFx.groundScar(level, at, size, 60, 0);
		cut(level, at, 0xC5CED8, size * 1.2, 1.0, 0.3, 1.5, 0.04, 5);
		cut(level, at.add(0, 0.1, 0), 0x8592A1, size, 1.2, 2.3, 1.1, 0.03, 7);
	}
	public static void windImpact(ServerLevel level, Vec3 at, double size) {
		for (int i = 0; i < 3; i++) cut(level, at.add(0, i * 0.14, 0), 0xB3C5C2, size * (1 + i * 0.15), 0.3, i * 1.8, 2.1, 0.025, 7 + i);
		AuraFx.groundScar(level, at, size, 50, 2);
	}
	public static void earthImpact(ServerLevel level, Vec3 at, double size) {
		AuraFx.groundScar(level, at, 1.8 * size, 100, 1);
		cut(level, at, 0xAA9680, size, 1.1, 0.6, 1.7, 0.11, 8);
	}
	public static void voidImpact(ServerLevel level, Vec3 at, double size) {
		AuraFx.groundScar(level, at, 1.5 * size, 90, 0);
		cut(level, at, 0x736B82, size, 0.8, 0, 1.6, 0.065, 8);
		cut(level, at, 0x968EAB, size * 0.7, 1.2, Math.PI, 1.4, 0.035, 6);
	}
	public static void arcaneImpact(ServerLevel level, Vec3 at, double size) {
		AuraFx.groundScar(level, at, 1.2 * size, 80, 2);
		for (int i = 0; i < 4; i++) cut(level, at, 0xCDBAA7, size, 1.0, i * Math.PI / 2, 0.6, 0.03, 5 + i);
	}
	public static void timeImpact(ServerLevel level, Vec3 at, double size) {
		AuraFx.groundScar(level, at, size, 100, 2);
		cut(level, at, 0xBAA580, size, 0.8, 0.3, 1.6, 0.05, 10);
		cut(level, at.add(0.12, 0, 0), 0xD0C3AC, size * 0.9, 0.8, 0.3, 1.6, 0.025, 16);
	}
	public static void frostCreep(ServerLevel level, Vec3 at, double radius, int life) { AuraFx.groundScar(level, at, radius, life, 2); }
	public static void crack(ServerLevel level, Vec3 at, double radius, int life) { AuraFx.groundScar(level, at, radius, life, 0); }
	public static void gustRing(ServerLevel level, Vec3 at, double radius) { AuraFx.groundScar(level, at, radius, 60, 2); }
	public static void shatterRing(ServerLevel level, Vec3 at, double radius) { frostImpact(level, at, radius); }
	public static void implode(ServerLevel level, Vec3 at, double radius, int life) {
		AuraFx.groundScar(level, at, radius, life, 1);
		cut(level, at, 0x82778C, radius, 1.0, 0, 2.0, 0.055, Math.min(20, life));
	}
	public static void pulse(ServerLevel level, Vec3 at, Vec3 normal, double radius) {
		Light.slash(level, at, normal, ElementFx.perp(normal), 0xAA7772, radius, 1.5, 0.04, 2, 8);
	}
	public static void clock(ServerLevel level, Vec3 at, Vec3 normal, double radius, int ticks, boolean backward) {
		Light.slash(level, at, normal, ElementFx.perp(normal).scale(backward ? -1 : 1), 0xC5B18D, radius, 1.8, 0.045, 2, ticks);
	}
}
