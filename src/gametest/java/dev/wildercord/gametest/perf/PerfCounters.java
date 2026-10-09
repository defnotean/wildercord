package dev.wildercord.gametest.perf;

import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;

import java.util.concurrent.atomic.AtomicLong;

/** What the server sent while {@link #on}: packets (bundles opened), particle packets and the particles they carry. */
public final class PerfCounters {
	private PerfCounters() {}

	public static volatile boolean on;
	public static final AtomicLong PACKETS = new AtomicLong(), FAKE_PACKETS = new AtomicLong(), PARTICLE_PACKETS = new AtomicLong(), PARTICLES = new AtomicLong();

	public static void reset() { PACKETS.set(0); FAKE_PACKETS.set(0); PARTICLE_PACKETS.set(0); PARTICLES.set(0); }

	public static void sent(Packet<?> packet, boolean fake) {
		if (!on) return;
		if (packet instanceof BundlePacket<?> bundle) { for (Packet<?> sub : bundle.subPackets()) sent(sub, fake); return; }
		(fake ? FAKE_PACKETS : PACKETS).incrementAndGet();
		if (packet instanceof ClientboundLevelParticlesPacket particles) { PARTICLE_PACKETS.incrementAndGet(); PARTICLES.addAndGet(Math.max(1, particles.count())); }
	}
}
