package dev.wildercord.bedrock;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraSense;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.Clashes;
import dev.wildercord.aura.FormDash;
import dev.wildercord.aura.FormDashLessons;
import dev.wildercord.aura.MasterFormLessons;
import dev.wildercord.aura.MasterForms;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.cast.CampConcordFx;
import dev.wildercord.cast.MasteryChoices;
import dev.wildercord.cast.PlayerAffinities;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.WorldResonances;
import dev.wildercord.content.AirflowOption;
import dev.wildercord.content.CampOption;
import dev.wildercord.content.EarthOption;
import dev.wildercord.content.ExciseLesson;
import dev.wildercord.content.LifeOption;
import dev.wildercord.content.LightOption;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.content.MoteOption;
import dev.wildercord.content.PackLesson;
import dev.wildercord.content.RelayLesson;
import dev.wildercord.content.ReweaveLesson;
import dev.wildercord.content.RitualOption;
import dev.wildercord.content.ShieldOption;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.content.VoidOption;
import dev.wildercord.net.BreakthroughPayload;
import dev.wildercord.net.ExciseCores;
import dev.wildercord.net.FormationPayload;
import dev.wildercord.net.LifeOutcomePayload;
import dev.wildercord.net.NotebookPayload;
import dev.wildercord.net.TidewardCue;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.runesmith.Contracts;
import dev.wildercord.travel.Waypoints;
import dev.wildercord.wildlife.MossveilFilterCue;
import dev.wildercord.wildlife.RainshieldFx;
import dev.wildercord.wildlife.RootCarryFx;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates Wildercord's rich client-side magic VFX, Aura ribbons, Sword Clashes, live RuneBolt
 * comet trails, and interactive custom-payload menus into vanilla Bedrock/Geyser-safe particles,
 * titles, and Cumulus forms so Bedrock players see and interact with every visual effect.
 */
public final class BedrockVfxBridge {
	private static final int MAX_BUNDLE_PACKETS = 20;
	private static final int MAX_BOLTS_PER_PLAYER = 24;
	private static volatile boolean initialized = false;
	private static int tickCounter = 0;

	private BedrockVfxBridge() {}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if ((++tickCounter % 2) != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.connection == null || !BedrockPlayers.isBedrock(player)) {
					continue;
				}
				emitNearbyBoltTrails(player);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> tickCounter = 0);
	}

	private static void emitNearbyBoltTrails(ServerPlayer player) {
		List<RuneBolt> bolts = player.level().getEntitiesOfClass(RuneBolt.class, player.getBoundingBox().inflate(48.0));
		if (bolts.isEmpty()) {
			return;
		}
		int emitted = 0;
		for (RuneBolt bolt : bolts) {
			if (bolt.isRemoved()) {
				continue;
			}
			int rgb = normalizeRgb(bolt.getEntityData().get(RuneBolt.DATA_COLOR));
			List<Packet<? super ClientGamePacketListener>> pkts = new ArrayList<>(2);
			pkts.add(single(new DustParticleOptions(rgb, 1.5F), false, false,
				bolt.getX(), bolt.getY(), bolt.getZ(), 0.08F, 0.08F, 0.08F, 0.0F, 3));
			pkts.add(single(ParticleTypes.END_ROD, false, false,
				bolt.getX(), bolt.getY(), bolt.getZ(), 0.04F, 0.04F, 0.04F, 0.01F, 1));
			player.connection.send(new ClientboundBundlePacket(pkts));
			if (++emitted >= MAX_BOLTS_PER_PLAYER) {
				break;
			}
		}
	}

	// ---- Pure math & color helpers (unit-tested without Minecraft bootstrap)

	public static int normalizeRgb(int color) {
		int rgb = color & 0xFFFFFF;
		return rgb == 0 ? 0xB070FF : rgb;
	}

	public static float clampScale(float scale) {
		return Math.clamp(Float.isFinite(scale) ? scale : 1.2F, 0.5F, 3.0F);
	}

	public static double[][] basisFromYawPitch(float yawDeg, float pitchDeg) {
		if (pitchDeg <= -85.0F || pitchDeg >= 85.0F) {
			return new double[][] { { 1.0, 0.0, 0.0 }, { 0.0, 0.0, 1.0 } };
		}
		double yawRad = Math.toRadians(yawDeg);
		double pitchRad = Math.toRadians(pitchDeg);
		double cosP = Math.cos(pitchRad);
		double nx = -Math.sin(yawRad) * cosP;
		double ny = -Math.sin(pitchRad);
		double nz = Math.cos(yawRad) * cosP;
		return basisFromNormal(nx, ny, nz);
	}

	public static double[][] basisFromNormal(double nx, double ny, double nz) {
		double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (!Double.isFinite(nLen) || nLen < 1.0E-5) {
			return new double[][] { { 1.0, 0.0, 0.0 }, { 0.0, 1.0, 0.0 } };
		}
		nx /= nLen;
		ny /= nLen;
		nz /= nLen;
		double rx = Math.abs(ny) > 0.9 ? 1.0 : 0.0;
		double ry = Math.abs(ny) > 0.9 ? 0.0 : 1.0;
		double rz = 0.0;
		// u = normalize(cross(ref, n))
		double ux = ry * nz - rz * ny;
		double uy = rz * nx - rx * nz;
		double uz = rx * ny - ry * nx;
		double uLen = Math.sqrt(ux * ux + uy * uy + uz * uz);
		if (uLen < 1.0E-5) {
			return new double[][] { { 1.0, 0.0, 0.0 }, { 0.0, 0.0, 1.0 } };
		}
		ux /= uLen;
		uy /= uLen;
		uz /= uLen;
		// v = normalize(cross(n, u))
		double vx = ny * uz - nz * uy;
		double vy = nz * ux - nx * uz;
		double vz = nx * uy - ny * ux;
		double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
		if (vLen > 1.0E-5) {
			vx /= vLen;
			vy /= vLen;
			vz /= vLen;
		}
		return new double[][] { { ux, uy, uz }, { vx, vy, vz } };
	}

	public static double[] quadraticBezier(double cx, double cy, double cz, double ex, double ey, double ez, double t) {
		double clampedT = Math.clamp(t, 0.0, 1.0);
		double u = 1.0 - clampedT;
		double w1 = 2.0 * u * clampedT;
		double w2 = clampedT * clampedT;
		return new double[] { w1 * cx + w2 * ex, w1 * cy + w2 * ey, w1 * cz + w2 * ez };
	}

	// ---- Custom Wildercord Particle Expansion

	public static Packet<?> translateParticlePacket(ClientboundLevelParticlesPacket packet) {
		ParticleOptions orig = packet.particle();
		Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(orig.getType());
		if (!BedrockMappings.isModdedId(id)) {
			return packet;
		}

		boolean ov = packet.overrideLimiter();
		boolean always = packet.alwaysShow();
		double x = packet.x();
		double y = packet.y();
		double z = packet.z();

		if (orig instanceof SpellCircleOption sc) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(16);
			double[][] basis = basisFromYawPitch(sc.yaw(), sc.pitch());
			double r = Math.clamp(Float.isFinite(sc.radius()) ? sc.radius() : 1.0F, 0.4, 8.0);
			addRing(out, dust(sc.color(), 1.5F), x, y, z, basis, r, 14, ov, always);
			out.add(single(ParticleTypes.ENCHANT, ov, always, x, y + 0.15, z, 0.25F, 0.15F, 0.25F, 0.2F, 4));
			out.add(single(ParticleTypes.END_ROD, ov, always, x, y + 0.1, z, 0.05F, 0.05F, 0.05F, 0.02F, 1));
			return bundleOrSingle(out);
		}

		if (orig instanceof LightOption lo) {
			return translateLightOption(lo, ov, always, x, y, z);
		}

		if (orig instanceof SigilOption so) {
			if (so.style() == SigilOption.GLOW) {
				List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
				out.add(single(dust(so.color(), 2.0F), ov, always, x, y, z, 0.25F, 0.25F, 0.25F, 0.0F, 4));
				out.add(single(ParticleTypes.END_ROD, ov, always, x, y, z, 0.15F, 0.15F, 0.15F, 0.02F, 2));
				return bundleOrSingle(out);
			}
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(13);
			double[][] basis = basisFromYawPitch(so.yaw(), so.pitch());
			double r = Math.clamp(Float.isFinite(so.size()) ? so.size() : 1.0F, 0.4, 6.0);
			addRing(out, dust(so.color(), 1.4F), x, y, z, basis, r, 12, ov, always);
			out.add(single(ParticleTypes.ENCHANT, ov, always, x, y + 0.1, z, 0.2F, 0.1F, 0.2F, 0.15F, 3));
			return bundleOrSingle(out);
		}

		if (orig instanceof ShieldOption sh) {
			double nx = sh.dx();
			double ny = sh.dy();
			double nz = sh.dz();
			double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (!Double.isFinite(len) || len < 1.0E-5) {
				nx = 0.0;
				ny = 0.0;
				nz = 1.0;
			} else {
				nx /= len;
				ny /= len;
				nz /= len;
			}
			double off = Math.clamp(Float.isFinite(sh.offset()) ? sh.offset() : 1.0F, 0.0, 3.0);
			double cx = x + nx * off;
			double cy = y + ny * off;
			double cz = z + nz * off;
			double[][] basis = basisFromNormal(nx, ny, nz);
			double r = Math.clamp(Float.isFinite(sh.radius()) ? sh.radius() : 1.0F, 0.5, 4.0);
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(11);
			addRing(out, dust(sh.color(), 1.5F), cx, cy, cz, basis, r, 10, ov, always);
			out.add(single(sh.kind() == ShieldOption.BREAK ? ParticleTypes.CRIT : ParticleTypes.END_ROD,
				ov, always, cx, cy, cz, 0.2F, 0.2F, 0.2F, 0.05F, 4));
			return bundleOrSingle(out);
		}

		if (orig instanceof AirflowOption af) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(9);
			DustParticleOptions d = dust(af.color(), 1.1F);
			Vec3 ctrl = af.control();
			Vec3 end = af.end();
			for (int i = 0; i < 7; i++) {
				double t = (i + 1) / 7.0;
				double[] pt = quadraticBezier(ctrl.x, ctrl.y, ctrl.z, end.x, end.y, end.z, t);
				out.add(single(d, ov, always, x + pt[0], y + pt[1], z + pt[2], 0.04F, 0.04F, 0.04F, 0.0F, 1));
			}
			double[] mid = quadraticBezier(ctrl.x, ctrl.y, ctrl.z, end.x, end.y, end.z, 0.5);
			out.add(single(ParticleTypes.CLOUD, ov, always, x + mid[0], y + mid[1], z + mid[2], 0.05F, 0.05F, 0.05F, 0.01F, 1));
			out.add(single(ParticleTypes.CLOUD, ov, always, x + end.x, y + end.y, z + end.z, 0.05F, 0.05F, 0.05F, 0.01F, 1));
			return bundleOrSingle(out);
		}

		if (orig instanceof RitualOption ro) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(11);
			double r = 1.2 + Math.clamp(Float.isFinite(ro.progress()) ? ro.progress() : 0.0F, 0.0F, 1.0F) * 0.6;
			double[][] xz = basisFromYawPitch(0.0F, -90.0F);
			addRing(out, dust(ro.color(), 1.3F), x, y + 0.08, z, xz, r, 10, ov, always);
			out.add(single(ParticleTypes.END_ROD, ov, always, x, y + 0.2, z, 0.1F, 0.4F, 0.1F, 0.02F, 2));
			return bundleOrSingle(out);
		}

		if (orig instanceof MoteOption mo) {
			ParticleOptions opt = mo.kind() == MoteOption.CLOUD
				? ParticleTypes.CLOUD
				: dust(mo.color(), clampScale(mo.size() * 6.0F));
			return replaceParticle(opt, packet);
		}

		if (orig instanceof MaterialOption ma) {
			return replaceParticle(dust(ma.color(), clampScale(ma.size() * 7.0F)), packet);
		}

		if (orig instanceof LifeOption lo) {
			return replaceParticle(dust(lo.color(), clampScale(lo.size() * 5.0F)), packet);
		}

		if (orig instanceof VoidOption vo) {
			return replaceParticle(dust(vo.color(), clampScale(vo.size() * 5.0F)), packet);
		}

		if (orig instanceof EarthOption eo) {
			return replaceParticle(dust(eo.color(), clampScale(eo.size() * 5.0F)), packet);
		}

		if (orig instanceof CampOption co) {
			return replaceParticle(dust(co.color(), clampScale(co.size() * 5.0F)), packet);
		}

		return replaceParticle(BedrockMappings.translateParticle(orig), packet);
	}

	private static Packet<?> translateLightOption(LightOption lo, boolean ov, boolean always, double x, double y, double z) {
		DustParticleOptions d = dust(lo.color(), 1.55F);
		return switch (lo.kind()) {
			case LightOption.RING -> {
				List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(12);
				double[][] basis = basisFromYawPitch(lo.yaw(), lo.pitch());
				double rawR = Math.max(lo.a(), (lo.a() + lo.b()) * 0.5F);
				double r = Math.clamp(Double.isFinite(rawR) ? rawR : 1.5, 0.4, 10.0);
				addRing(out, d, x, y, z, basis, r, 12, ov, always);
				yield bundleOrSingle(out);
			}
			case LightOption.RAY -> {
				List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(14);
				double dx = Double.isFinite(lo.a()) ? Math.clamp(lo.a(), -32.0, 32.0) : 0.0;
				double dy = Double.isFinite(lo.b()) ? Math.clamp(lo.b(), -32.0, 32.0) : 0.0;
				double dz = Double.isFinite(lo.c()) ? Math.clamp(lo.c(), -32.0, 32.0) : 0.0;
				for (int i = 0; i < 12; i++) {
					double t = i / 11.0;
					out.add(single(d, ov, always, x + dx * t, y + dy * t, z + dz * t, 0.03F, 0.03F, 0.03F, 0.0F, 1));
				}
				out.add(single(ParticleTypes.END_ROD, ov, always, x + dx * 0.5, y + dy * 0.5, z + dz * 0.5, 0.05F, 0.05F, 0.05F, 0.01F, 1));
				out.add(single(ParticleTypes.END_ROD, ov, always, x + dx, y + dy, z + dz, 0.08F, 0.08F, 0.08F, 0.02F, 2));
				yield bundleOrSingle(out);
			}
			case LightOption.SLASH -> {
				List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(11);
				double[][] basis = basisFromYawPitch(lo.yaw(), lo.pitch());
				double r = Math.clamp(Float.isFinite(lo.a()) ? lo.a() : 1.5F, 0.6, 6.0);
				double span = Math.clamp(Float.isFinite(lo.b()) ? lo.b() : 2.0F, 0.3, Math.PI * 2.0);
				double center = Float.isFinite(lo.roll()) ? lo.roll() : 0.0;
				double start = center - span * 0.5;
				for (int i = 0; i < 10; i++) {
					double ang = start + span * (i / 9.0);
					double c = Math.cos(ang) * r;
					double s = Math.sin(ang) * r;
					double px = x + basis[0][0] * c + basis[1][0] * s;
					double py = y + basis[0][1] * c + basis[1][1] * s;
					double pz = z + basis[0][2] * c + basis[1][2] * s;
					out.add(single(d, ov, always, px, py, pz, 0.03F, 0.03F, 0.03F, 0.0F, 1));
				}
				double mc = Math.cos(center) * r;
				double ms = Math.sin(center) * r;
				out.add(single(ParticleTypes.SWEEP_ATTACK, ov, always,
					x + basis[0][0] * mc + basis[1][0] * ms,
					y + basis[0][1] * mc + basis[1][1] * ms,
					z + basis[0][2] * mc + basis[1][2] * ms,
					0.0F, 0.0F, 0.0F, 0.0F, 1));
				yield bundleOrSingle(out);
			}
			case LightOption.ORB -> {
				List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
				float radius = Float.isFinite(lo.a()) ? lo.a() : 0.5F;
				float spread = Math.clamp(radius * 0.4F, 0.1F, 0.8F);
				out.add(single(dust(lo.color(), clampScale(radius * 1.5F + 0.8F)), ov, always, x, y, z, spread, spread, spread, 0.0F, 6));
				out.add(single(ParticleTypes.END_ROD, ov, always, x, y, z, 0.12F, 0.12F, 0.12F, 0.02F, 2));
				yield bundleOrSingle(out);
			}
			case LightOption.ARC -> {
				List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(13);
				double dx = Double.isFinite(lo.a()) ? Math.clamp(lo.a(), -32.0, 32.0) : 0.0;
				double dy = Double.isFinite(lo.b()) ? Math.clamp(lo.b(), -32.0, 32.0) : 0.0;
				double dz = Double.isFinite(lo.c()) ? Math.clamp(lo.c(), -32.0, 32.0) : 0.0;
				double hLen = Math.sqrt(dx * dx + dz * dz);
				double px = hLen > 1.0E-4 ? -dz / hLen : 1.0;
				double pz = hLen > 1.0E-4 ? dx / hLen : 0.0;
				DustParticleOptions arcDust = dust(lo.color(), 1.3F);
				for (int i = 0; i < 10; i++) {
					double t = i / 9.0;
					double jag = (i == 0 || i == 9) ? 0.0 : ((i % 2 == 0) ? 0.22 : -0.22);
					out.add(single(arcDust, ov, always,
						x + dx * t + px * jag, y + dy * t, z + dz * t + pz * jag,
						0.03F, 0.03F, 0.03F, 0.0F, 1));
				}
				for (int j = 0; j < 3; j++) {
					double t = (j + 1) / 4.0;
					out.add(single(ParticleTypes.ELECTRIC_SPARK, ov, always,
						x + dx * t, y + dy * t, z + dz * t, 0.1F, 0.1F, 0.1F, 0.03F, 2));
				}
				yield bundleOrSingle(out);
			}
			default -> single(d, ov, always, x, y, z, 0.1F, 0.1F, 0.1F, 0.0F, 2);
		};
	}

	// ---- S2C Custom Payload VFX & Interactive Menu Translation

	public static Packet<?> translateCustomPayload(CustomPacketPayload payload, ServerPlayer player) {
		if (payload instanceof AuraFx.Trail trail) {
			Entity e = player.level().getEntity(trail.entity());
			if (e == null) {
				return null;
			}
			int pts = (trail.flags() & AuraFx.ORDINARY) != 0 ? 5 : 8;
			return drawSlashArc(e, trail.color(), trail.mirror(), trail.entity() == player.getId(), pts);
		}

		if (payload instanceof AuraFx.Impact imp) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(3);
			Vec3 at = imp.at();
			out.add(single(dust(imp.color(), 1.7F), true, false, at.x, at.y, at.z, 0.35F, 0.35F, 0.35F, 0.0F, 8));
			out.add(single(ParticleTypes.CRIT, true, false, at.x, at.y, at.z, 0.3F, 0.3F, 0.3F, 0.1F, 5));
			out.add(single(ParticleTypes.ELECTRIC_SPARK, true, false, at.x, at.y, at.z, 0.3F, 0.3F, 0.3F, 0.1F, 3));
			return bundleOrSingle(out);
		}

		if (payload instanceof AuraFx.GroundScar scar) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(11);
			double r = Math.clamp(Float.isFinite(scar.radius()) ? scar.radius() : 1.5F, 0.6, 6.0);
			double[][] xz = basisFromYawPitch(0.0F, -90.0F);
			addRing(out, dust(0xD86C34, 1.4F), scar.at().x, scar.at().y + 0.1, scar.at().z, xz, r, 10, true, false);
			out.add(single(ParticleTypes.FLAME, true, false, scar.at().x, scar.at().y + 0.15, scar.at().z, 0.3F, 0.08F, 0.3F, 0.02F, 4));
			return bundleOrSingle(out);
		}

		if (payload instanceof AuraFx.BurstCue burst) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(9);
			double r = Math.clamp((Float.isFinite(burst.size()) ? burst.size() : 2.0F) * 0.5, 0.5, 4.0);
			double[][] xz = basisFromYawPitch(0.0F, -90.0F);
			addRing(out, dust(burst.color(), clampScale(burst.size())), burst.at().x, burst.at().y, burst.at().z, xz, r, 8, true, false);
			out.add(single(ParticleTypes.END_ROD, true, false, burst.at().x, burst.at().y, burst.at().z, 0.25F, 0.25F, 0.25F, 0.04F, 4));
			return bundleOrSingle(out);
		}

		if (payload instanceof AuraFx.Standard std) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(6);
			DustParticleOptions d = dust(std.color(), 1.6F);
			for (int i = 0; i < 6; i++) {
				out.add(single(d, true, false, std.at().x, std.at().y + 0.4 * i, std.at().z, 0.08F, 0.05F, 0.08F, 0.0F, 1));
			}
			return bundleOrSingle(out);
		}

		if (payload instanceof AuraFx.Flare flare) {
			Entity e = player.level().getEntity(flare.entity());
			if (e == null) {
				return null;
			}
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
			out.add(single(dust(0xFFD878, 1.5F), false, false, e.getX(), e.getY() + 1.0, e.getZ(), 0.5F, 0.5F, 0.5F, 0.0F, 10));
			out.add(single(ParticleTypes.END_ROD, false, false, e.getX(), e.getY() + 1.0, e.getZ(), 0.4F, 0.4F, 0.4F, 0.02F, 4));
			return bundleOrSingle(out);
		}

		if (payload instanceof AuraFx.Shown shown) {
			return translateParticlePacket(single(shown.particle(), true, false,
				shown.at().x, shown.at().y, shown.at().z, 0.0F, 0.0F, 0.0F, 0.0F, 1));
		}

		if (payload instanceof AuraFx.Banner banner) {
			if (banner.entity() != player.getId()) {
				return null;
			}
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(3);
			out.add(new ClientboundSetTitlesAnimationPacket(4, 24, 8));
			out.add(new ClientboundSetTitleTextPacket(banner.name().copy().withColor(0xFF000000 | normalizeRgb(banner.color()))));
			out.add(new ClientboundSetSubtitleTextPacket(banner.kicker()));
			return new ClientboundBundlePacket(out);
		}

		if (payload instanceof AuraStep.Stepped step) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(7);
			DustParticleOptions d = dust(step.color(), 1.4F);
			Vec3 from = step.from();
			Vec3 to = step.to();
			for (int i = 0; i < 6; i++) {
				double t = i / 5.0;
				out.add(single(d, false, false,
					from.x + (to.x - from.x) * t,
					from.y + 0.8 + (to.y - from.y) * t,
					from.z + (to.z - from.z) * t,
					0.06F, 0.06F, 0.06F, 0.0F, 1));
			}
			out.add(single(ParticleTypes.CLOUD, false, false, from.x, from.y + 0.5, from.z, 0.2F, 0.2F, 0.2F, 0.02F, 3));
			return bundleOrSingle(out);
		}

		if (payload instanceof Clashes.Begin beg) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(6);
			Vec3 at = beg.at();
			out.add(single(dust(beg.colorA(), 1.8F), true, false, at.x, at.y, at.z, 0.25F, 0.25F, 0.25F, 0.0F, 6));
			out.add(single(dust(beg.colorB(), 1.8F), true, false, at.x, at.y, at.z, 0.25F, 0.25F, 0.25F, 0.0F, 6));
			out.add(single(ParticleTypes.ELECTRIC_SPARK, true, false, at.x, at.y, at.z, 0.35F, 0.35F, 0.35F, 0.1F, 8));
			if (beg.a() == player.getId() || beg.b() == player.getId()) {
				out.add(new ClientboundSetTitlesAnimationPacket(2, 24, 6));
				out.add(new ClientboundSetTitleTextPacket(Component.literal("SWORD CLASH!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
				out.add(new ClientboundSetSubtitleTextPacket(Component.literal("Right-click on the beat!").withStyle(ChatFormatting.YELLOW)));
			}
			return bundleOrSingle(out);
		}

		if (payload instanceof Clashes.End end) {
			if (!Clashes.clashing(player)) {
				return null;
			}
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
			out.add(new ClientboundSetTitlesAnimationPacket(2, 24, 8));
			out.add(new ClientboundSetSubtitleTextPacket(Component.literal("Clash Finished (" + end.scoreA() + " - " + end.scoreB() + ")")
				.withStyle(ChatFormatting.GOLD)));
			return new ClientboundBundlePacket(out);
		}

		if (payload instanceof MasteryChoices.Rise rise) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(11);
			out.add(new ClientboundSetTitlesAnimationPacket(6, 40, 12));
			out.add(new ClientboundSetTitleTextPacket(Component.literal(rise.name()).withColor(0xFF000000 | normalizeRgb(rise.color()))));
			out.add(new ClientboundSetSubtitleTextPacket(Component.literal("Mastery Rank " + rise.rank()
				+ (rise.choice() ? " — Use /wc -> Mastery!" : "")).withStyle(ChatFormatting.GOLD)));
			addRing(out, dust(rise.color(), 1.4F), player.getX(), player.getY() + 1.0, player.getZ(),
				basisFromYawPitch(0.0F, -90.0F), 1.4, 8, true, false);
			return bundleOrSingle(out);
		}

		if (payload instanceof MasteryChoices.Title title) {
			if (title.caster() != player.getId()) {
				return null;
			}
			return new ClientboundSetSubtitleTextPacket(Component.literal(title.name()).withColor(0xFF000000 | normalizeRgb(title.color())));
		}

		if (payload instanceof BreakthroughPayload bp) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(15);
			out.add(new ClientboundSetTitlesAnimationPacket(8, 50, 16));
			out.add(new ClientboundSetTitleTextPacket(Component.literal("Circle " + bp.circle() + " Formed!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
			out.add(new ClientboundSetSubtitleTextPacket(Component.literal(bp.tribulation() ? "Tribulation Overcome" : "Heart Breakthrough").withStyle(ChatFormatting.GOLD)));
			addRing(out, dust(0xF5D06A, 1.5F), player.getX(), player.getY() + 1.0, player.getZ(),
				basisFromYawPitch(0.0F, -90.0F), 1.8, 12, true, false);
			return bundleOrSingle(out);
		}

		if (payload instanceof PlayerAffinities.Rise aff) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
			out.add(new ClientboundSetTitlesAnimationPacket(4, 30, 10));
			out.add(new ClientboundSetSubtitleTextPacket(Component.literal("Affinity Raised: " + aff.element().toUpperCase() + " Lv." + aff.level())
				.withStyle(ChatFormatting.LIGHT_PURPLE)));
			return new ClientboundBundlePacket(out);
		}

		if (payload instanceof WorldResonances.Revealed res) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(3);
			out.add(new ClientboundSetTitlesAnimationPacket(6, 40, 14));
			out.add(new ClientboundSetTitleTextPacket(Component.literal(res.name()).withColor(0xFF000000 | normalizeRgb(res.color()))));
			out.add(new ClientboundSetSubtitleTextPacket(Component.literal("World Resonance Revealed").withStyle(ChatFormatting.AQUA)));
			return new ClientboundBundlePacket(out);
		}

		if (payload instanceof FormationPayload fm) {
			Entity caster = player.level().getEntity(fm.caster());
			Vec3 center;
			if (fm.placement() == FormationPayload.FIXED) {
				center = fm.anchor();
			} else if (fm.placement() == FormationPayload.AIMED) {
				center = caster != null
					? caster.getEyePosition().add(caster.getLookAngle().scale(Math.clamp(fm.aimRange() * 0.45, 3.0, 12.0)))
					: fm.anchor();
			} else if (caster != null) {
				center = fm.placement() == FormationPayload.LEGACY
					? caster.getEyePosition().add(caster.getLookAngle().scale(1.4))
					: caster.position().add(0.0, 0.15, 0.0);
			} else {
				return null;
			}
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(14);
			double r = Math.clamp(Float.isFinite(fm.scale()) ? fm.scale() : 1.3F, 0.7, 4.5);
			addRing(out, dust(fm.color(), 1.5F), center.x, center.y, center.z, basisFromYawPitch(0.0F, -90.0F), r, 12, true, false);
			out.add(single(ParticleTypes.ENCHANT, true, false, center.x, center.y + 0.15, center.z, 0.25F, 0.15F, 0.25F, 0.2F, 4));
			out.add(single(ParticleTypes.END_ROD, true, false, center.x, center.y + 0.1, center.z, 0.1F, 0.1F, 0.1F, 0.02F, 2));
			return bundleOrSingle(out);
		}

		if (payload instanceof Waypoints.Track tr) {
			BedrockHud.setTrackedWaypoint(player.getUUID(), tr);
			return null;
		}

		if (payload instanceof ExciseCores ec) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>();
			DustParticleOptions d = dust(0xB7D58B, 1.5F);
			int cap = Math.min(ec.cores().size(), 10);
			for (int i = 0; i < cap && out.size() + 2 <= MAX_BUNDLE_PACKETS; i++) {
				Vec3 c = ec.cores().get(i).at();
				out.add(single(d, false, false, c.x, c.y + 0.3, c.z, 0.25F, 0.25F, 0.25F, 0.0F, 3));
				out.add(single(ParticleTypes.END_ROD, false, false, c.x, c.y + 0.4, c.z, 0.15F, 0.15F, 0.15F, 0.02F, 1));
			}
			return bundleOrSingle(out);
		}

		if (payload instanceof LifeOutcomePayload life) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(6);
			Vec3 a = life.anchor();
			out.add(single(dust(0x6EDC64, 1.4F), false, false, a.x, a.y + 0.4, a.z, 0.35F, 0.35F, 0.35F, 0.0F, 6));
			out.add(single(ParticleTypes.HAPPY_VILLAGER, false, false, a.x, a.y + 0.5, a.z, 0.3F, 0.3F, 0.3F, 0.02F, 4));
			Vec3 sec = life.secondary();
			if (sec != null) {
				DustParticleOptions trailDust = dust(0x98FFA0, 1.2F);
				for (int i = 1; i <= 4; i++) {
					double t = i / 4.0;
					out.add(single(trailDust, false, false,
						a.x + (sec.x - a.x) * t, a.y + 0.4 + (sec.y - a.y) * t, a.z + (sec.z - a.z) * t,
						0.05F, 0.05F, 0.05F, 0.0F, 1));
				}
			}
			return bundleOrSingle(out);
		}

		if (payload instanceof CampConcordFx.Event camp) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(5);
			DustParticleOptions d = dust(0xF5A84A, 1.3F);
			Vec3 from = camp.from();
			Vec3 to = camp.to();
			for (int i = 0; i < 4; i++) {
				double t = i / 3.0;
				out.add(single(d, false, false,
					from.x + (to.x - from.x) * t, from.y + 0.5 + (to.y - from.y) * t, from.z + (to.z - from.z) * t,
					0.08F, 0.08F, 0.08F, 0.0F, 1));
			}
			out.add(single(ParticleTypes.FLAME, false, false, to.x, to.y + 0.6, to.z, 0.2F, 0.2F, 0.2F, 0.02F, 3));
			return bundleOrSingle(out);
		}

		if (payload instanceof TidewardCue tide) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>();
			DustParticleOptions d = dust(0x4AD4E8, 1.4F);
			if (!tide.feet().isEmpty()) {
				int cap = Math.min(tide.feet().size(), 8);
				for (int i = 0; i < cap; i++) {
					BlockPos fp = tide.feet().get(i);
					out.add(single(d, false, false, fp.getX() + 0.5, fp.getY() + 0.2, fp.getZ() + 0.5, 0.2F, 0.08F, 0.2F, 0.0F, 2));
				}
			} else if (tide.entity() > 0) {
				Entity target = player.level().getEntity(tide.entity());
				if (target != null) {
					addRing(out, d, target.getX(), target.getY() + 0.25, target.getZ(), basisFromYawPitch(0.0F, -90.0F), 1.4, 8, true, false);
					out.add(single(ParticleTypes.END_ROD, true, false, target.getX(), target.getY() + 0.6, target.getZ(), 0.25F, 0.25F, 0.25F, 0.03F, 3));
				}
			}
			return bundleOrSingle(out);
		}

		if (payload instanceof AuraSense.Sensed sensed) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>();
			DustParticleOptions d = dust(sensed.color(), 1.6F);
			int count = 0;
			for (int id : sensed.ids()) {
				Entity e = player.level().getEntity(id);
				if (e == null) {
					continue;
				}
				out.add(single(d, true, false, e.getX(), e.getY() + e.getBbHeight() + 0.35, e.getZ(), 0.2F, 0.15F, 0.2F, 0.0F, 4));
				if (++count >= 8) {
					break;
				}
			}
			return bundleOrSingle(out);
		}

		if (payload instanceof FormDash.Event fd) {
			Entity e = player.level().getEntity(fd.entity());
			if (e == null) {
				return null;
			}
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
			out.add(single(dust(0xFFB454, 1.4F), false, false, e.getX(), e.getY() + 0.7, e.getZ(), 0.35F, 0.35F, 0.35F, 0.0F, 6));
			out.add(single(ParticleTypes.CLOUD, false, false, e.getX(), e.getY() + 0.3, e.getZ(), 0.25F, 0.15F, 0.25F, 0.04F, 3));
			return bundleOrSingle(out);
		}

		if (payload instanceof MasterForms.Event mf) {
			Entity e = player.level().getEntity(mf.entity());
			if (e == null) {
				return null;
			}
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
			out.add(single(dust(0xE8C87A, 1.6F), false, false, e.getX(), e.getY() + 0.9, e.getZ(), 0.4F, 0.4F, 0.4F, 0.0F, 8));
			out.add(single(ParticleTypes.END_ROD, false, false, e.getX(), e.getY() + 0.9, e.getZ(), 0.3F, 0.3F, 0.3F, 0.04F, 3));
			return bundleOrSingle(out);
		}

		if (payload instanceof MastersArts.Performed ma) {
			Entity e = player.level().getEntity(ma.entity());
			return e == null ? null : drawSlashArc(e, 0xFFE070, false, e.getId() == player.getId(), 8);
		}

		if (payload instanceof SwordStrings.Cue) {
			Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
			return single(ParticleTypes.END_ROD, false, false, at.x, at.y, at.z, 0.15F, 0.15F, 0.15F, 0.02F, 3);
		}

		if (payload instanceof Clashes.Mark mark) {
			if (!Clashes.clashing(player)) {
				return null;
			}
			MinecraftServer srvMark = player.level().getServer();
			if (srvMark != null) {
				srvMark.execute(() -> player.sendOverlayMessage(
					Component.literal("CLASH BEAT! Score: " + mark.scoreA() + " - " + mark.scoreB()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
				));
			}
			Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.8));
			return single(ParticleTypes.ELECTRIC_SPARK, true, false, at.x, at.y, at.z, 0.25F, 0.25F, 0.25F, 0.08F, 5);
		}

		if (payload instanceof WildercordNetworking.ScreenFx sfx) {
			if (sfx.kind() == WildercordNetworking.ScreenFx.TINT || sfx.strength() < 0.4F) {
				return null;
			}
			return single(ParticleTypes.CRIT, false, false, player.getX(), player.getY() + 0.5, player.getZ(), 0.35F, 0.2F, 0.35F, 0.05F, 4);
		}

		if (payload instanceof MossveilFilterCue cue) {
			Entity pet = player.level().getEntity(cue.pet());
			if (pet == null) {
				return null;
			}
			return single(ParticleTypes.CHERRY_LEAVES, false, false, pet.getX(), pet.getY() + 0.6, pet.getZ(), 0.35F, 0.25F, 0.35F, 0.02F, 4);
		}

		if (payload instanceof RainshieldFx.Event rain) {
			Entity carrier = player.level().getEntity(rain.carrierId());
			if (carrier == null) {
				return null;
			}
			return single(dust(0x6ED4FF, 1.3F), false, false, carrier.getX(), carrier.getY() + 1.0, carrier.getZ(), 0.35F, 0.35F, 0.35F, 0.0F, 5);
		}

		if (payload instanceof RootCarryFx.Event root) {
			List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(2);
			out.add(single(dust(0x8E6C3E, 1.2F), false, false, root.from().getX() + 0.5, root.from().getY() + 0.6, root.from().getZ() + 0.5, 0.25F, 0.25F, 0.25F, 0.0F, 3));
			out.add(single(dust(0x6EDC64, 1.2F), false, false, root.to().getX() + 0.5, root.to().getY() + 0.6, root.to().getZ() + 0.5, 0.25F, 0.25F, 0.25F, 0.0F, 3));
			return bundleOrSingle(out);
		}

		// Auto-open interactive Cumulus forms & server notices on the server thread for Bedrock players
		MinecraftServer srv = player.level().getServer();
		if (srv != null) {
			if (payload instanceof RelayLesson.Show show) {
				srv.execute(() -> BedrockForms.openRelayLessonFromServer(player, show.page()));
				return null;
			}
			if (payload instanceof ReweaveLesson.Show show) {
				srv.execute(() -> BedrockForms.openReweaveLessonFromServer(player, show.page()));
				return null;
			}
			if (payload instanceof ExciseLesson.Show show) {
				srv.execute(() -> BedrockForms.openExciseLessonFromServer(player, show.page()));
				return null;
			}
			if (payload instanceof PackLesson.Show show) {
				srv.execute(() -> BedrockForms.openPackLessonFromServer(player, show.lesson(), show.page()));
				return null;
			}
			if (payload instanceof FormDashLessons.Open || payload instanceof MasterFormLessons.Open) {
				srv.execute(() -> BedrockAuraForms.openDashAndForms(player));
				return null;
			}
			if (payload instanceof Contracts.ShowBoard board) {
				srv.execute(() -> BedrockForms.openRunesmithContracts(player, board));
				return null;
			}
			if (payload instanceof NotebookPayload nb) {
				srv.execute(() -> BedrockForms.openRuneNotebook(player, nb));
				return null;
			}
			if (payload instanceof SwordStrings.Refused ref) {
				srv.execute(() -> player.sendOverlayMessage(
					Component.literal("Sword String '" + ref.art() + "' not ready.").withStyle(ChatFormatting.YELLOW)
				));
				return null;
			}
			if (payload instanceof WildercordNetworking.Discovery disc) {
				srv.execute(() -> player.sendOverlayMessage(
					Component.literal("Discovered: " + disc.key()).withStyle(ChatFormatting.LIGHT_PURPLE)
				));
				return null;
			}
		}

		return null;
	}

	private static Packet<?> drawSlashArc(Entity e, int color, boolean mirror, boolean isSelf, int pts) {
		Vec3 look = e.getLookAngle();
		Vec3 eye = e.getEyePosition();
		Vec3 origin = eye.add(look.scale(isSelf ? 1.9 : 1.5));
		double[][] basis = basisFromNormal(look.x, look.y, look.z);
		List<Packet<? super ClientGamePacketListener>> out = new ArrayList<>(pts + 1);
		DustParticleOptions d = dust(color, 1.5F);
		double flip = mirror ? -1.0 : 1.0;
		for (int i = 0; i < pts; i++) {
			double u = (i / (double) Math.max(1, pts - 1)) * 2.0 - 1.0;
			double rightOff = u * 1.1;
			double upOff = (u * 0.35 + (1.0 - u * u) * 0.2) * flip;
			out.add(single(d, false, false,
				origin.x + basis[0][0] * rightOff + basis[1][0] * upOff,
				origin.y + basis[0][1] * rightOff + basis[1][1] * upOff,
				origin.z + basis[0][2] * rightOff + basis[1][2] * upOff,
				0.03F, 0.03F, 0.03F, 0.0F, 1));
		}
		out.add(single(ParticleTypes.SWEEP_ATTACK, false, false, origin.x, origin.y, origin.z, 0.0F, 0.0F, 0.0F, 0.0F, 1));
		return bundleOrSingle(out);
	}

	private static DustParticleOptions dust(int color, float scale) {
		return new DustParticleOptions(normalizeRgb(color), clampScale(scale));
	}

	private static ClientboundLevelParticlesPacket replaceParticle(ParticleOptions opt, ClientboundLevelParticlesPacket orig) {
		return new ClientboundLevelParticlesPacket(
			opt,
			orig.overrideLimiter(),
			orig.alwaysShow(),
			orig.x(),
			orig.y(),
			orig.z(),
			orig.xDist(),
			orig.yDist(),
			orig.zDist(),
			orig.xMaxSpeed(),
			orig.yMaxSpeed(),
			orig.zMaxSpeed(),
			orig.count(),
			orig.randomizationType()
		);
	}

	private static ClientboundLevelParticlesPacket single(
		ParticleOptions opt, boolean overrideLimiter, boolean alwaysShow,
		double x, double y, double z, float dx, float dy, float dz, float speed, int count
	) {
		return new ClientboundLevelParticlesPacket(opt, overrideLimiter, alwaysShow, x, y, z, dx, dy, dz, speed, count);
	}

	private static void addRing(
		List<Packet<? super ClientGamePacketListener>> out, ParticleOptions opt,
		double cx, double cy, double cz, double[][] basis, double radius, int points,
		boolean overrideLimiter, boolean alwaysShow
	) {
		int n = Math.max(4, Math.min(MAX_BUNDLE_PACKETS - out.size(), points));
		for (int i = 0; i < n; i++) {
			double ang = (Math.PI * 2.0 * i) / n;
			double c = Math.cos(ang) * radius;
			double s = Math.sin(ang) * radius;
			double px = cx + basis[0][0] * c + basis[1][0] * s;
			double py = cy + basis[0][1] * c + basis[1][1] * s;
			double pz = cz + basis[0][2] * c + basis[1][2] * s;
			out.add(single(opt, overrideLimiter, alwaysShow, px, py, pz, 0.02F, 0.02F, 0.02F, 0.0F, 1));
		}
	}

	private static Packet<?> bundleOrSingle(List<Packet<? super ClientGamePacketListener>> out) {
		if (out.isEmpty()) {
			return null;
		}
		if (out.size() == 1) {
			return out.getFirst();
		}
		if (out.size() > MAX_BUNDLE_PACKETS) {
			return new ClientboundBundlePacket(new ArrayList<>(out.subList(0, MAX_BUNDLE_PACKETS)));
		}
		return new ClientboundBundlePacket(out);
	}
}
