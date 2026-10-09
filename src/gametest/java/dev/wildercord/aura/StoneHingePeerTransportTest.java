package dev.wildercord.aura;

import com.google.gson.Gson;
import dev.wildercord.gametest.stonehinge.peer.StoneHingePeerProbe;
import dev.wildercord.gametest.stonehinge.peer.StoneHingePeerProbe.Identity;
import dev.wildercord.gametest.stonehinge.peer.StoneHingePeerProbe.Report;
import dev.wildercord.gametest.stonehinge.peer.StoneHingePeerFailure;
import dev.wildercord.gametest.stonehinge.mixin.StoneHingeConnectionAccess;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.level.ServerPlayer;

import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Fixed three-profile diagnostic selected only by its bounded supervisor. No ordinary gameplay admission. */
public final class StoneHingePeerTransportTest implements FabricClientGameTest {
    private static final Gson JSON = new Gson();
    private static final List<String> IDENTITY = List.of("nonce", "suite", "sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256", "hostUuid", "peerUuid", "runIdentity", "profile");
    private final Properties identity = new Properties();
    private final List<String> completed = new ArrayList<>();
    private Path directory;
    private String role, profile, currentCase = "not-armed";
    private UUID hostId, peerId;
    private long deadline, hostPid, peerPid;
    private int caseIndex;
    @Override public void runTest(ClientGameTestContext context) {
        configure(); StoneHingePeerProbe.register();
        Throwable original = null;
        try { if (role.equals("host")) host(context); else peer(context); }
        catch (Throwable failure) { original = failure; failure("role", failure, Map.of()); throw failure; }
        finally { StoneHingePeerFailure.cleanup(original, StoneHingePeerProbe::clear, failure -> failure("role-cleanup", failure, Map.of())); }
    }
    private void configure() {
        role = required("role"); check(role.equals("host") || role.equals("peer"), "Explicit supervised native role");
        directory = Path.of(required("directory")).toAbsolutePath().normalize();
        check(Files.isDirectory(directory) && !Files.isSymbolicLink(directory), "Fresh supervisor IPC directory");
        for (String key : IDENTITY) {
            String value = key.equals("prHeadSha") ? System.getProperty("wildercord.mp.prHeadSha") : required(key);
            check(value != null, "Explicit provenance " + key); identity.setProperty(key, value);
        }
        check(identity.getProperty("suite").equals("stone-hinge-peer"), "Only the isolated Stone Hinge diagnostic suite");
        profile = identity.getProperty("profile"); check(List.of("transparent", "symmetric", "asymmetric").contains(profile), "Exactly one fixed connection-lifetime delay profile");
        check(UUID.fromString(identity.getProperty("nonce")).toString().equals(identity.getProperty("nonce")), "Canonical fresh launch nonce");
        check(identity.getProperty("sourceHead").matches("[a-f0-9]{40}") && identity.getProperty("sourceHead").equals(identity.getProperty("checkoutSha"))
            && identity.getProperty("descriptorSha256").matches("[a-f0-9]{64}"), "Exact source and runtime descriptor");
        check(identity.getProperty("prHeadSha").isEmpty() || identity.getProperty("prHeadSha").matches("[a-f0-9]{40}"), "Optional explicit PR association");
        hostId = UUID.fromString(required("hostUuid")); peerId = UUID.fromString(required("peerUuid")); check(!hostId.equals(peerId), "Distinct real native profiles");
        int seconds = Integer.parseInt(required("timeoutSeconds")); check(seconds >= 60 && seconds <= 300, "At most 300 seconds per native profile");
        deadline = System.nanoTime() + (seconds - 10L) * 1_000_000_000L;
    }
    private void host(ClientGameTestContext c) {
        Properties settings = new Properties();
        settings.setProperty("server-ip", "127.0.0.1"); settings.setProperty("server-port", "0");
        settings.setProperty("max-players", "2"); settings.setProperty("white-list", "false");
        settings.setProperty("online-mode", "false"); settings.setProperty("enforce-secure-profile", "false");
        settings.setProperty("view-distance", "5"); settings.setProperty("simulation-distance", "5");
        try (var server = c.worldBuilder().createServer(settings)) {
            String phase = "server-listener";
            try {
                // No bind-close port guess and no direct server.connect(): both real clients use their owned relay.
                int port = server.computeOnServer(s -> {
                    var channels = ((dev.wildercord.gametest.stonehinge.mixin.StoneHingeServerConnectionAccess) s.getConnection()).stoneHinge$channels();
                    synchronized (channels) {
                        check(channels.size() == 1 && channels.getFirst().isSuccess() && channels.getFirst().channel().isActive(), "One actual retained native server listener");
                        var fields = new LinkedHashMap<String, String>(); endpoint(fields, "server", channels.getFirst().channel().localAddress());
                        return Integer.parseInt(fields.get("serverPort"));
                    }
                });
                write("server-ready", Map.of("port", Integer.toString(port)));
                phase = "clients-connect";
                Properties relay = relayReady(c); connect(c, relay, "ownerPort", hostId);
                await(c, () -> exists("peer-connected.properties") && server.computeOnServer(s -> s.getPlayerList().getPlayers().size() == 2
                    && s.getPlayerList().getPlayers().stream().allMatch(p -> p.connection.hasClientLoaded())), "Both actual relay clients join");
                read("peer-connected", "peer");
                server.runOnServer(s -> {
                    ServerPlayer owner = s.getPlayerList().getPlayer(hostId), observer = s.getPlayerList().getPlayer(peerId);
                    check(owner != null && observer != null && owner.connection.player == owner && observer.connection.player == observer, "Both exact current server bodies");
                    var values = new LinkedHashMap<String, String>();
                    endpoint(values, "ownerBackendRemote", owner.connection.getRemoteAddress());
                    endpoint(values, "observerBackendRemote", observer.connection.getRemoteAddress());
                    values.put("ownerEntity", Integer.toString(owner.getId())); values.put("observerEntity", Integer.toString(observer.getId()));
                    write("server-connected", values);
                });
                phase = "cases";
                new StoneHingePeerCases().run(c, server, hostId, peerId, identity.getProperty("nonce"), profile, new StoneHingePeerCases.Witness() {
                    public void arm(Identity id) {
                        check(caseIndex < StoneHingePeerCases.roster(profile).size() && id.name().equals(StoneHingePeerCases.roster(profile).get(caseIndex).name()), "Exact ordered case roster");
                        currentCase = id.name();
                        writeJson(stem() + "-identity.json", id);
                        write(stem() + "-arm", Map.of("case", id.name(), "identitySha256", sha(stem() + "-identity.json")));
                        await(c, () -> exists(stem() + "-armed.properties"), "Peer arms exact live body before native attack");
                        Properties armed = read(stem() + "-armed", "peer");
                        check(id.name().equals(armed.getProperty("case")) && sha(stem() + "-identity.json").equals(armed.getProperty("identitySha256")), "Peer arm binds nonce/case/body generation");
                    }
                    public void finish(Report report, boolean moved) {
                        writeJson(stem() + "-host.json", report);
                        write(stem() + "-ready", Map.of("case", report.identity().name(), "reportSha256", sha(stem() + "-host.json"), "moved", Boolean.toString(moved)));
                        await(c, () -> exists(stem() + "-seen.properties"), "Peer receives exact original tracker payload and converges");
                        Properties seen = read(stem() + "-seen", "peer");
                        check(report.identity().name().equals(seen.getProperty("case")) && sha(stem() + "-peer.json").equals(seen.getProperty("reportSha256")), "Peer receipt binds exact report bytes");
                        Report peer = readJson(stem() + "-peer.json", Report.class); StoneHingePeerProbe.verifyReports(report, peer, moved);
                        completed.add(report.identity().name()); write(stem() + "-passed", Map.of("case", report.identity().name(), "hostReportSha256", sha(stem() + "-host.json"), "peerReportSha256", sha(stem() + "-peer.json")));
                        caseIndex++;
                    }
                    public void failure(StoneHingePeerCases.Case which, String phase, Throwable failure, Map<String, String> details) {
                        var fields = new LinkedHashMap<>(details);
                        if (which != null) {
                            fields.put("case", which.name());
                            fields.put("caseIndex", Integer.toString(StoneHingePeerCases.roster(profile).indexOf(which)));
                        }
                        StoneHingePeerTransportTest.this.failure(phase, failure, fields);
                    }
                });
                phase = "roster-complete";
                check(completed.equals(roster()), "Complete exact profile roster");
                write("disconnect-peer", Map.of("cases", String.join(",", completed)));
                await(c, () -> exists("peer-disconnected.properties") && exists("peer-stone-hinge-passed.properties")
                    && server.computeOnServer(s -> s.getPlayerList().getPlayer(peerId) == null), "Peer cleanly disconnects");
                check(read("peer-stone-hinge-passed", "peer").getProperty("cases").equals(String.join(",", completed)), "Peer independently completed exact profile roster");
                phase = "host-disconnect";
                disconnect(c); write("host-stone-hinge-passed", Map.of("cases", String.join(",", completed)));
            } catch (Throwable failure) {
                // Publish while the server is still owned and live: close() may block or trigger relay EOF.
                failure(phase, failure, Map.of());
                throw failure;
            }
        }
    }
    private void peer(ClientGameTestContext c) {
        Properties relay = relayReady(c); connect(c, relay, "observerPort", peerId);
        for (String name : roster()) {
            currentCase = name;
            await(c, () -> exists(stem() + "-arm.properties"), "Host arms next fixed case");
            Properties arm = read(stem() + "-arm", "host");
            check(name.equals(arm.getProperty("case")) && sha(stem() + "-identity.json").equals(arm.getProperty("identitySha256")), "Exact case identity artifact");
            Identity id = readJson(stem() + "-identity.json", Identity.class);
            check(id.nonce().equals(identity.getProperty("nonce")) && id.profile().equals(profile) && id.name().equals(name)
                && id.ownerUuid().equals(hostId.toString()) && id.peerUuid().equals(peerId.toString()), "Nonce/profile/role-bound live body identity");
            c.waitFor(mc -> mc.level != null && mc.level.getPlayerByUUID(hostId) != null && mc.level.getPlayerByUUID(hostId).getId() == id.ownerEntity(), 100);
            c.runOnClient(mc -> StoneHingePeerProbe.startPeer(id));
            write(stem() + "-armed", Map.of("case", name, "identitySha256", sha(stem() + "-identity.json")));
            await(c, () -> exists(stem() + "-ready.properties"), "Native attack and real owner movement finish");
            Properties ready = read(stem() + "-ready", "host");
            check(name.equals(ready.getProperty("case")) && sha(stem() + "-host.json").equals(ready.getProperty("reportSha256")), "Exact original tracker report");
            Report host = readJson(stem() + "-host.json", Report.class);
            c.waitFor(mc -> StoneHingePeerProbe.converged(host), 120);
            c.waitTicks(5);
            Report report = c.computeOnClient(mc -> StoneHingePeerProbe.peerReport());
            StoneHingePeerProbe.verifyReports(host, report, Boolean.parseBoolean(ready.getProperty("moved")));
            writeJson(stem() + "-peer.json", report);
            write(stem() + "-seen", Map.of("case", name, "reportSha256", sha(stem() + "-peer.json")));
            await(c, () -> exists(stem() + "-passed.properties"), "Host verifies complete passive chain");
            read(stem() + "-passed", "host"); c.runOnClient(mc -> StoneHingePeerProbe.clear()); completed.add(name); caseIndex++;
        }
        await(c, () -> exists("disconnect-peer.properties"), "Host completes all fixed cases"); read("disconnect-peer", "host");
        disconnect(c); write("peer-stone-hinge-passed", Map.of("cases", String.join(",", completed)));
        write("peer-disconnected", Map.of("cases", String.join(",", completed)));
    }
    private Properties relayReady(ClientGameTestContext c) {
        await(c, () -> exists("relay-ready.properties"), "Supervisor publishes owned literal loopback relays");
        Properties relay = read("relay-ready", "supervisor");
        hostPid = Long.parseLong(relay.getProperty("hostPid")); peerPid = Long.parseLong(relay.getProperty("peerPid"));
        check(hostPid > 0 && peerPid > 0 && hostPid != peerPid && ProcessHandle.current().pid() == (role.equals("host") ? hostPid : peerPid), "Retained owned JVM PID identities");
        return relay;
    }
    private void connect(ClientGameTestContext c, Properties relay, String key, UUID uuid) {
        int port = Integer.parseInt(relay.getProperty(key)); check(port > 0 && port <= 65535, "Owned loopback relay port");
        String address = "127.0.0.1:" + port;
        c.runOnClient(mc -> ConnectScreen.startConnecting(mc.gui.screen(), mc, ServerAddress.parseString(address), new ServerData("Stone Hinge relay diagnostic", address, ServerData.Type.OTHER), false, null));
        c.waitFor(mc -> mc.player != null && mc.level != null && mc.getConnection() != null, 2400);
        c.runOnClient(mc -> {
            check(mc.player.getUUID().equals(uuid), "Actual native fixed profile");
            var channel = ((StoneHingeConnectionAccess) mc.getConnection().getConnection()).stoneHinge$channel();
            var fields = new LinkedHashMap<String, String>(); endpoint(fields, "clientLocal", channel.localAddress()); endpoint(fields, "clientRemote", channel.remoteAddress());
            check(Integer.parseInt(fields.get("clientRemotePort")) == port, "ConnectScreen connected to assigned owned relay only");
            fields.put("entity", Integer.toString(mc.player.getId())); write(role + "-connected", fields);
        });
    }
    private static void endpoint(Map<String, String> fields, String prefix, java.net.SocketAddress value) {
        check(value instanceof InetSocketAddress, "Real TCP endpoint"); var address = (InetSocketAddress) value;
        check(address.getAddress() != null && address.getAddress().getHostAddress().equals("127.0.0.1") && address.getPort() > 0, "Literal IPv4 loopback endpoint");
        fields.put(prefix + "Host", "127.0.0.1"); fields.put(prefix + "Port", Integer.toString(address.getPort()));
    }
    private static void disconnect(ClientGameTestContext c) {
        c.runOnClient(mc -> { if (mc.level != null) mc.disconnect(new TitleScreen(), false); mc.gui.setScreen(new TitleScreen()); });
        c.waitFor(mc -> mc.player == null && mc.level == null, 100);
    }
    private List<String> roster() { return StoneHingePeerCases.roster(profile).stream().map(Enum::name).toList(); }
    private String stem() { return String.format(Locale.ROOT, "case-%02d", caseIndex); }
    private void failure(String phase, Throwable original, Map<String, String> details) {
        StoneHingePeerFailure.report(original, () -> {
            if (exists(role + "-failure.properties")) return; // Keep the earliest pre-cleanup witness.
            var fields = new LinkedHashMap<>(StoneHingePeerFailure.describe(original));
            fields.put("phase", phase); fields.put("case", currentCase); fields.put("caseIndex", Integer.toString(caseIndex));
            fields.putAll(details);
            write(role + "-failure", fields);
        });
    }
    private void await(ClientGameTestContext c, BooleanSupplier condition, String why) {
        while (System.nanoTime() < deadline) {
            check(!exists("host-failure.properties") && !exists("peer-failure.properties"), "Both native roles remain healthy: " + why);
            if (condition.getAsBoolean()) return;
            c.waitTicks(1); // Advance actual client/server ticks; never hold a measurement phase or substitute a client stall for latency.
        }
        throw new AssertionError("Bounded native profile expired: " + why);
    }
    private Path path(String filename) {
        check(filename.matches("[a-z0-9-]+\\.(json|properties|tmp)"), "Bounded evidence filename");
        Path path = directory.resolve(filename); check(!Files.isSymbolicLink(path), "No linked evidence"); return path;
    }
    private boolean exists(String filename) { return Files.isRegularFile(path(filename)); }
    private Properties read(String name, String expectedRole) {
        Path file = path(name + ".properties");
        try (var input = Files.newInputStream(file)) {
            check(Files.size(file) <= 65536, "Bounded native witness"); Properties value = new Properties(); value.load(input);
            for (String key : IDENTITY) check(identity.getProperty(key).equals(value.getProperty(key)), "Native IPC identity " + key);
            check(expectedRole.equals(value.getProperty("role")), "Exact IPC role");
            if (expectedRole.equals("host") && hostPid != 0) check(Long.parseLong(value.getProperty("pid")) == hostPid, "Same owned host PID");
            if (expectedRole.equals("peer") && peerPid != 0) check(Long.parseLong(value.getProperty("pid")) == peerPid, "Same owned peer PID");
            return value;
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private void write(String name, Map<String, String> fields) {
        Properties value = new Properties(); value.putAll(identity); value.putAll(fields); value.setProperty("role", role); value.setProperty("pid", Long.toString(ProcessHandle.current().pid()));
        Path destination = path(name + ".properties"), temporary = path(name + ".tmp");
        check(!Files.exists(destination) && !Files.exists(temporary), "Write-once native witness");
        try { StoneHingePeerFailure.writeAtomic(destination, temporary, value);
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private void writeJson(String name, Object value) {
        byte[] bytes = JSON.toJson(value).getBytes(java.nio.charset.StandardCharsets.UTF_8); check(bytes.length <= 262144, "Bounded passive packet report");
        Path target = path(name), temporary = path(name.replace(".json", ".tmp")); check(!Files.exists(target), "Write-once packet report");
        try { Files.write(temporary, bytes, StandardOpenOption.CREATE_NEW); Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private <T> T readJson(String name, Class<T> type) {
        try { check(Files.size(path(name)) <= 262144, "Bounded passive packet report"); return JSON.fromJson(Files.readString(path(name)), type); }
        catch (java.io.IOException failure) { throw new AssertionError(failure); }
    }
    private String sha(String name) {
        try { check(Files.size(path(name)) <= 262144, "Bounded artifact hashing"); return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path(name)))); }
        catch (java.io.IOException | java.security.NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
    }
    private static String required(String key) { String v = System.getProperty("wildercord.mp." + key); check(v != null && !v.isBlank(), "Explicit supervisor property " + key); return v; }
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
}
