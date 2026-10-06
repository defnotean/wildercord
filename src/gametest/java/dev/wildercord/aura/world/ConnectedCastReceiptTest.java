package dev.wildercord.aura.world;

import com.google.gson.JsonParser;
import dev.wildercord.cast.NextCounterPairedCases;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Supervised-only two-JVM/TCP fixture. Server assertions own combat; the real peer witnesses client delivery. */
public final class ConnectedCastReceiptTest implements FabricClientGameTest {
	private static final String SUITE = "cast-receipt";
	private static final List<String> IDENTITY = List.of("nonce", "suite", "sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256", "hostUuid", "peerUuid", "runIdentity");
	private final Properties identity = new Properties();
	private final List<String> completed = new ArrayList<>();
	private final List<String> observed = new ArrayList<>();
	private Path directory;
	private String role;
	private List<String> expected;
	private UUID hostId, peerId;
	private net.minecraft.client.player.LocalPlayer counterDeadBody;
	private long deadline, otherPid;

	@Override public void runTest(ClientGameTestContext context) {
		role = required("role");
		check(role.equals("host") || role.equals("peer"), "Explicit supervised host/peer role required");
		directory = Path.of(required("directory")).toAbsolutePath().normalize();
		check(Files.isDirectory(directory), "Supervisor creates the fresh bounded IPC directory");
		for (String key : IDENTITY) {
			String value = key.equals("prHeadSha") ? System.getProperty("wildercord.mp.prHeadSha") : required(key);
			check(value != null, "Explicit provenance property required: " + key); identity.setProperty(key, value);
		}
		check(SUITE.equals(identity.getProperty("suite")), "Only the cast-receipt supervisor selects this entrypoint");
		check(identity.getProperty("sourceHead").matches("[a-f0-9]{40}") && identity.getProperty("descriptorSha256").matches("[a-f0-9]{64}"),
			"Exact source and descriptor provenance are mandatory");
		check(identity.getProperty("checkoutSha").equals(identity.getProperty("sourceHead")), "Runtime source is the actual checkout, not an assumed PR head");
		check(identity.getProperty("prHeadSha").isEmpty() || identity.getProperty("prHeadSha").matches("[a-f0-9]{40}"), "PR-head association is optional and separately validated");
		UUID.fromString(identity.getProperty("nonce"));
		hostId = UUID.fromString(identity.getProperty("hostUuid")); peerId = UUID.fromString(identity.getProperty("peerUuid"));
		check(!hostId.equals(peerId), "The two actual profiles must differ");
		int seconds = Integer.parseInt(required("timeoutSeconds"));
		check(seconds >= 60 && seconds <= 900, "Supervisor budget is between 60 and 900 seconds");
		deadline = System.nanoTime() + (seconds - 10L) * 1_000_000_000L;
		expected = contract();
		try {
			if (role.equals("host")) host(context); else peer(context);
		} catch (Throwable failure) {
			write(role + "-failure", Map.of("error", failure.toString()));
			throw failure;
		}
	}

	private void host(ClientGameTestContext context) {
		Properties properties = new Properties();
		properties.setProperty("server-ip", "127.0.0.1"); properties.setProperty("server-port", Integer.toString(localPort()));
		properties.setProperty("max-players", "2"); properties.setProperty("white-list", "false");
		properties.setProperty("online-mode", "false"); properties.setProperty("enforce-secure-profile", "false");
		properties.setProperty("view-distance", "5"); properties.setProperty("simulation-distance", "5");
		try (var server = context.worldBuilder().createServer(properties); var connection = server.connect()) {
			connection.waitForChunksDownload(); connection.waitForClientboundPackets();
			check(context.computeOnClient(mc -> mc.player.getUUID().equals(hostId)), "Actual host has the supervisor's fixed profile");
			write("host-ready", Map.of("port", Integer.toString(server.computeOnServer(s -> s.getPort()))));
			await(context, () -> exists("peer-connected") && server.computeOnServer(s -> s.getPlayerList().getPlayers().size() == 2
				&& s.getPlayerList().getPlayers().stream().allMatch(player -> player.connection.hasClientLoaded())), "Real peer joins and loads", false);
			otherPid = Long.parseLong(read("peer-connected", "peer").getProperty("pid"));
			check(otherPid > 0 && otherPid != ProcessHandle.current().pid(), "Peer is a distinct native client process");
			server.runOnServer(s -> {
				ServerPlayer host = s.getPlayerList().getPlayer(hostId), peer = s.getPlayerList().getPlayer(peerId);
				check(host != null && peer != null && host.connection.player == host && peer.connection.player == peer,
					"Both roles are exact current PlayerList bodies");
				check(!tcp(host).equals(tcp(peer)), "Both actual clients have distinct loopback TCP connections");
			});
			new CastHitReceiptConsistencyChecks().runConnectedPair(context, server, hostId, peerId,
				id -> observeCase(context, server, id), this::passCase);
			new NextCounterPairedCases().runConnectedPair(context, server, hostId, peerId,
				() -> respawnPeer(context, server),
				(id, proof) -> observeCase(context, server, id, proof, !id.equals(NextCounterPairedCases.CASES.get(2))), this::passCase);
			check(completed.equals(expected), "All preserved native scenarios passed in declared order");
			write("disconnect-peer", Map.of("cases", String.join(",", completed)));
			await(context, () -> exists("peer-disconnected") && exists("peer-cast-receipt-passed")
				&& server.computeOnServer(s -> s.getPlayerList().getPlayer(peerId) == null), "Real peer departs", false);
			check(read("peer-cast-receipt-passed", "peer").getProperty("cases").equals(String.join(",", completed)), "Peer witnessed the complete native case ledger");
			read("peer-disconnected", "peer");
			write("host-cast-receipt-passed", Map.of("cases", String.join(",", completed)));
		}
	}

	private void observeCase(ClientGameTestContext context, TestServerContext server, String id) {
		observeCase(context, server, id, Map.of(), true);
	}

	private void observeCase(ClientGameTestContext context, TestServerContext server, String id, Map<String, String> proof, boolean synchronize) {
		int index = observed.size();
		check(index == completed.size() && index < expected.size() && expected.get(index).equals(id), "No missing, duplicate or reordered native cases");
		// One actual synchronization tick publishes the already-verified outcome before the independent receiver observes it.
		// The reflected-flight case has already ticked and must be observed before its next ordinary collision step.
		if (synchronize) {
			long beforeSync = server.computeOnServer(s -> s.overworld().getGameTime());
			server.waitFor(s -> s.overworld().getGameTime() > beforeSync, 5);
		}
		Map<String, String> snapshot = server.computeOnServer(s -> {
			ServerPlayer peer = s.getPlayerList().getPlayer(peerId), host = s.getPlayerList().getPlayer(hostId);
			check(peer != null && host != null && peer.isAlive() && peer.connection.player == peer, "Recipient survives on its actual connection");
			var charge = peer.getAttached(WildercordAttachments.CHARGE);
			var sample = new HashMap<>(Map.of("case", id, "health", Float.toString(peer.getHealth()), "absorption", Float.toString(peer.getAbsorptionAmount()),
				"chargeStart", Long.toString(charge == null ? -1 : charge.start()), "recipientEntity", Integer.toString(peer.getId()),
				"actorEntity", Integer.toString(host.getId()), "serverTick", Long.toString(peer.level().getGameTime())));
			if (!proof.isEmpty()) {
				check(proof.keySet().stream().noneMatch(sample::containsKey), "Counter proof cannot replace shared outcome identity");
				sample.putAll(proof); sample.put("mana", Float.toString(Spellbooks.mana(peer)));
			}
			return Map.copyOf(sample);
		});
		String stem = String.format(java.util.Locale.ROOT, "case-%02d", index);
		write(stem + "-ready", snapshot);
		// Fabric pauses the server at this test phase. The separate peer can process packets without ageing
		// the server's 20-tick seal or letting later regeneration replace this observed delivery snapshot.
		await(context, () -> exists(stem + "-seen"), "Peer receives " + id, true);
		Properties seen = read(stem + "-seen", "peer");
		check(id.equals(seen.getProperty("case")) && Long.parseLong(seen.getProperty("pid")) == otherPid, "Expected peer acknowledges this exact case");
		if (!proof.isEmpty()) {
			for (String key : List.of("projectileUuid", "projectilePresent", "mana"))
				check(snapshot.get(key).equals(seen.getProperty(key)), "Peer acknowledges the exact native counter outcome: " + key);
			if (Boolean.parseBoolean(proof.get("projectilePresent"))) check("true".equals(seen.getProperty("replacementBody")), "Peer separately proves its new live body differs from the dead one");
		}
		observed.add(id);
	}

	private void respawnPeer(ClientGameTestContext context, TestServerContext server) {
		ServerPlayer prior = server.computeOnServer(s -> s.getPlayerList().getPlayer(peerId));
		check(prior != null && !prior.isAlive(), "Counter respawn begins with the actual dead shooter body");
		write("counter-respawn-peer", Map.of("priorEntity", Integer.toString(prior.getId()), "uuid", peerId.toString()));
		// Publish the actual death packet, then wait for the separate client to send its ordinary respawn request.
		context.waitTicks(1);
		await(context, () -> exists("counter-respawn-requested"), "Peer sends its native respawn packet", true);
		Properties request = read("counter-respawn-requested", "peer");
		check(request.getProperty("priorEntity").equals(Integer.toString(prior.getId())) && request.getProperty("uuid").equals(peerId.toString()),
			"Respawn request names the exact dead peer body and unchanged profile");
		server.waitFor(s -> {
			var next = s.getPlayerList().getPlayer(peerId);
			return next != null && next != prior && next.isAlive() && next.connection.player == next;
		}, 4);
		await(context, () -> exists("counter-peer-respawned"), "Peer receives its genuine replacement body", true);
		Properties result = read("counter-peer-respawned", "peer");
		server.runOnServer(s -> {
			var next = s.getPlayerList().getPlayer(peerId);
			check(!prior.isAlive() && next != prior && next.isAlive() && next.connection.player == next && next.getUUID().equals(prior.getUUID())
				&& result.getProperty("uuid").equals(peerId.toString()) && Integer.parseInt(result.getProperty("priorEntity")) == prior.getId()
				&& Integer.parseInt(result.getProperty("replacementEntity")) == next.getId(),
				"Both roles prove the actual replacement body, same profile UUID, and permanently dead original body");
		});
	}

	private void passCase(String id) {
		int index = completed.size();
		check(index < expected.size() && expected.get(index).equals(id) && observed.size() == index + 1
			&& observed.get(index).equals(id), "Exactly one live peer observation precedes final case success");
		completed.add(id);
		write(String.format(java.util.Locale.ROOT, "case-%02d-passed", index), Map.of("case", id));
	}

	private void peer(ClientGameTestContext context) {
		await(context, () -> exists("host-ready"), "Host publishes its loopback server", false);
		Properties ready = read("host-ready", "host"); otherPid = Long.parseLong(ready.getProperty("pid"));
		check(otherPid > 0 && otherPid != ProcessHandle.current().pid(), "Host is a separate native process");
		int port = Integer.parseInt(ready.getProperty("port")); check(port > 0 && port <= 65535, "Actual loopback port supplied");
		String address = "127.0.0.1:" + port;
		try {
			context.runOnClient(mc -> ConnectScreen.startConnecting(mc.gui.screen(), mc, ServerAddress.parseString(address),
				new ServerData("Connected cast receipt fixture", address, ServerData.Type.OTHER), false, null));
			context.waitFor(mc -> mc.player != null && mc.level != null && mc.getConnection() != null, 2400);
			check(context.computeOnClient(mc -> mc.player.getUUID().equals(peerId)), "Actual peer has the distinct fixed profile");
			write("peer-connected", Map.of());
			for (int i = 0; i < expected.size(); i++) {
				String stem = String.format(java.util.Locale.ROOT, "case-%02d", i), id = expected.get(i);
				if (id.equals(NextCounterPairedCases.CASES.get(2))) respawnAsPeer(context);
				await(context, () -> exists(stem + "-ready"), "Native case " + id, false);
				Properties sample = read(stem + "-ready", "host");
				check(id.equals(sample.getProperty("case")), "Live case follows the immutable contract");
				float health = Float.parseFloat(sample.getProperty("health")), absorption = Float.parseFloat(sample.getProperty("absorption"));
				long chargeStart = Long.parseLong(sample.getProperty("chargeStart"));
				int recipient = Integer.parseInt(sample.getProperty("recipientEntity")), actor = Integer.parseInt(sample.getProperty("actorEntity"));
				context.waitFor(mc -> {
					if (mc.player == null || mc.level == null || !mc.player.getUUID().equals(peerId) || mc.player.getId() != recipient
						|| mc.level.getPlayerByUUID(hostId) == null || mc.level.getPlayerByUUID(hostId).getId() != actor) return false;
					var charge = mc.player.getAttached(WildercordAttachments.CHARGE);
					if (Math.abs(mc.player.getHealth() - health) >= .001F || Math.abs(mc.player.getAbsorptionAmount() - absorption) >= .001F
						|| !(charge == null ? chargeStart == -1 : charge.start() == chargeStart)) return false;
					if (sample.containsKey("projectileUuid")) {
						if (Math.abs(Spellbooks.mana(mc.player) - Float.parseFloat(sample.getProperty("mana"))) >= .001F) return false;
						var bolt = mc.level.getEntity(Integer.parseInt(sample.getProperty("projectileEntity")));
						if (Boolean.parseBoolean(sample.getProperty("projectilePresent"))) {
							return bolt != null && bolt.isAlive() && bolt.getUUID().equals(UUID.fromString(sample.getProperty("projectileUuid")))
								&& mc.player.getId() == Integer.parseInt(sample.getProperty("replacementEntity"))
								&& counterDeadBody != null && mc.player != counterDeadBody && counterDeadBody.isDeadOrDying()
								&& mc.player.getUUID().equals(UUID.fromString(sample.getProperty("replacementUuid")));
						}
						return bolt == null;
					}
					return true;
				}, 240);
				var seen = new HashMap<>(Map.of("case", id, "recipientEntity", Integer.toString(recipient), "actorEntity", Integer.toString(actor)));
				if (sample.containsKey("projectileUuid")) {
					seen.put("projectileUuid", sample.getProperty("projectileUuid")); seen.put("projectilePresent", sample.getProperty("projectilePresent"));
					seen.put("mana", sample.getProperty("mana"));
					if (Boolean.parseBoolean(sample.getProperty("projectilePresent"))) seen.put("replacementBody", "true");
				}
				write(stem + "-seen", seen);
				await(context, () -> exists(stem + "-passed"), "Remaining server assertions pass for " + id, false);
				check(id.equals(read(stem + "-passed", "host").getProperty("case")), "Host confirms this exact case after all strict probes");
				completed.add(id);
			}
			await(context, () -> exists("disconnect-peer"), "Native assertions complete before departure", false);
			check(read("disconnect-peer", "host").getProperty("cases").equals(String.join(",", completed)), "Both roles finish the same complete 38-case ledger");
			context.runOnClient(mc -> mc.disconnect(new TitleScreen(), false));
			context.waitFor(mc -> mc.player == null && mc.level == null, 300);
			write("peer-disconnected", Map.of("cases", String.join(",", completed)));
			write("peer-cast-receipt-passed", Map.of("cases", String.join(",", completed)));
			await(context, () -> exists("host-cast-receipt-passed"), "Host observes connection removal", false);
			read("host-cast-receipt-passed", "host");
		} finally { context.runOnClient(mc -> { if (mc.level != null) mc.disconnect(new TitleScreen(), false); }); }
	}

	private void respawnAsPeer(ClientGameTestContext context) {
		await(context, () -> exists("counter-respawn-peer"), "Counter host confirms actual shooter death", false);
		Properties command = read("counter-respawn-peer", "host");
		int prior = Integer.parseInt(command.getProperty("priorEntity"));
		check(peerId.toString().equals(command.getProperty("uuid")), "Respawn command selects this existing peer profile");
		context.waitFor(mc -> mc.player != null && mc.player.getUUID().equals(peerId) && mc.player.getId() == prior && mc.player.isDeadOrDying(), 240);
		counterDeadBody = context.computeOnClient(mc -> mc.player);
		context.runOnClient(mc -> mc.player.respawn());
		write("counter-respawn-requested", Map.of("priorEntity", Integer.toString(prior), "uuid", peerId.toString()));
		// Vanilla deliberately reuses the entity ID across respawn; the live object identity must actually change.
		context.waitFor(mc -> mc.player != null && mc.player != counterDeadBody && mc.player.getUUID().equals(peerId) && mc.player.isAlive()
			&& !(mc.gui.screen() instanceof DeathScreen), 240);
		write("counter-peer-respawned", Map.of("priorEntity", Integer.toString(prior), "uuid", peerId.toString(),
			"replacementEntity", context.computeOnClient(mc -> Integer.toString(mc.player.getId()))));
	}

	private void await(ClientGameTestContext context, BooleanSupplier condition, String reason, boolean freezeServerPhase) {
		long end = Math.min(deadline, System.nanoTime() + (freezeServerPhase ? 15 : 840) * 1_000_000_000L);
		while (System.nanoTime() < end) {
			check(!Files.exists(directory.resolve("host-failure.properties")) && !Files.exists(directory.resolve("peer-failure.properties")), "Both owned processes remain healthy: " + reason);
			if (condition.getAsBoolean()) return;
			if (!freezeServerPhase) context.waitTicks(1);
			else try { Thread.sleep(10); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
		}
		throw new AssertionError("Finite native handshake expired: " + reason);
	}
	private boolean exists(String name) { return Files.isRegularFile(directory.resolve(name + ".properties")); }
	private Properties read(String name, String expectedRole) {
		try (var input = Files.newInputStream(directory.resolve(name + ".properties"))) {
			Properties value = new Properties(); value.load(input);
			for (String key : IDENTITY) check(identity.getProperty(key).equals(value.getProperty(key)), "Native IPC identity matches: " + key);
			check(expectedRole.equals(value.getProperty("role")), "Native IPC has the expected role");
			if (otherPid != 0) check(Long.parseLong(value.getProperty("pid")) == otherPid, "Native IPC keeps its original other process");
			return value;
		} catch (java.io.IOException failure) { throw new AssertionError(failure); }
	}
	private void write(String name, Map<String, String> fields) {
		try {
			Properties value = new Properties(); value.putAll(identity); value.putAll(fields);
			value.setProperty("role", role); value.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
			Path destination = directory.resolve(name + ".properties"), temporary = directory.resolve(name + ".tmp");
			check(!Files.exists(destination), "Native witness is written once: " + name);
			try (var output = Files.newOutputStream(temporary)) { value.store(output, "Bounded native cast receipt witness"); }
			Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (java.io.IOException failure) { throw new AssertionError(failure); }
	}
	private static List<String> contract() {
		try (var input = ConnectedCastReceiptTest.class.getResourceAsStream("/cast-receipt-native-contract.json")) {
			check(input != null, "Source-controlled native case contract is packaged");
			var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
			var cases = new ArrayList<String>(); json.getAsJsonArray("cases").forEach(value -> cases.add(value.getAsString()));
			var compiled = new ArrayList<>(CastHitReceiptConsistencyChecks.expectedCases()); compiled.addAll(NextCounterPairedCases.CASES);
			check(json.get("expectedCount").getAsInt() == 38 && cases.equals(compiled), "Contract preserves all 35 compiled combat scenarios plus exactly three paired counters");
			return List.copyOf(cases);
		} catch (java.io.IOException failure) { throw new AssertionError(failure); }
	}
	private static String required(String key) {
		String value = System.getProperty("wildercord.mp." + key);
		check(value != null && !value.isBlank(), "Explicit supervised property required: " + key); return value;
	}
	private static int localPort() {
		try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); }
		catch (java.io.IOException failure) { throw new AssertionError(failure); }
	}
	private static InetSocketAddress tcp(ServerPlayer player) {
		check(player.connection.getRemoteAddress() instanceof InetSocketAddress, "Native profile has a TCP socket");
		var address = (InetSocketAddress) player.connection.getRemoteAddress();
		check(address.getAddress().isLoopbackAddress() && address.getPort() > 0, "Native fixture connections are local TCP"); return address;
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
