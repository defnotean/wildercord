package dev.wildercord.gametest;

import dev.wildercord.cast.events.Tribulation;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.duel.Arena;
import dev.wildercord.duel.Duels;
import dev.wildercord.guild.GuildRules;
import dev.wildercord.guild.Guilds;
import dev.wildercord.guild.Mentors;
import dev.wildercord.party.Parties;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * The 0.13 social features between two real clients on one dedicated server, over loopback TCP: a coven founded, an
 * invitation sent and accepted, the coven's bonus; a mentor's offer that outlasts the apprentice logging out and back in;
 * a party's tribulation that takes the second player in; and a ranked bout at an arena stone. The host drives every step,
 * and the peer types its own commands and reports the messages it really received. Run only by
 * {@code tools/native/launch_social_clients.py}.
 */
public final class WildercordSocialTwoClientsTest implements FabricClientGameTest {
	private static final List<Component> MESSAGES = new CopyOnWriteArrayList<>();
	private static final BlockPos STONE = new BlockPos(2, 101, 2);

	private Path directory;
	private String nonce, role;
	private int step, port;
	private long deadline;

	@Override
	public void runTest(ClientGameTestContext c) {
		directory = Path.of(required("directory")).toAbsolutePath();
		nonce = required("nonce");
		role = required("role");
		deadline = System.nanoTime() + (Long.parseLong(required("timeoutSeconds")) - 15) * 1_000_000_000L;
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> MESSAGES.add(message));
		try {
			if (role.equals("host")) host(c);
			else if (role.equals("peer")) peer(c);
			else throw new AssertionError("Explicit host/peer role required");
		} catch (Throwable error) {
			write(role + "-failure", Map.of("error", String.valueOf(error), "step", Integer.toString(step)));
			throw error;
		}
	}

	private void host(ClientGameTestContext c) {
		var properties = new Properties();
		properties.setProperty("server-ip", "127.0.0.1");
		properties.setProperty("server-port", Integer.toString(localPort()));
		properties.setProperty("max-players", "2");
		properties.setProperty("white-list", "false");
		properties.setProperty("online-mode", "false");
		properties.setProperty("enforce-secure-profile", "false");
		properties.setProperty("view-distance", "5");
		properties.setProperty("simulation-distance", "5");
		properties.setProperty("difficulty", "normal");
		try (var server = c.worldBuilder().createServer(properties); var connection = server.connect()) {
			connection.waitForChunksDownload();
			UUID host = c.computeOnClient(mc -> mc.player.getUUID());
			port = server.computeOnServer(MinecraftServer::getPort);
			write("host-ready", Map.of("port", Integer.toString(port)));
			await(c, () -> exists("peer-connected") && players(server) == 2, "the peer joins over TCP");
			UUID peer = UUID.fromString(read("peer-connected").getProperty("uuid"));
			String peerName = read("peer-connected").getProperty("name");
			String hostName = c.computeOnClient(mc -> mc.getUser().getName());
			server.runCommand("difficulty normal");
			server.runCommand("gamerule spawn_mobs false");
			server.runOnServer(s -> {
				ServerPlayer h = s.getPlayerList().getPlayer(host), p = s.getPlayerList().getPlayer(peer);
				check(h != null && p != null && h != p, "both real players are on the server");
				check(tcp(h) && tcp(p), "both players are on real loopback sockets");
				for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
					s.overworld().setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 101; y <= 105; y++) s.overworld().setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				for (ServerPlayer player : List.of(h, p)) {
					player.setGameMode(GameType.SURVIVAL);
					player.teleportTo(s.overworld(), player == h ? .5 : 4.5, 101, .5, Set.<Relative>of(), 0, 0, false);
				}
				h.setAttached(WildercordAttachments.CIRCLES, 10);
				p.setAttached(WildercordAttachments.CIRCLES, 1);
				h.getInventory().add(new ItemStack(Items.EMERALD, GuildRules.FOUND_COST));
			});

			// A coven: founded and invited to by typed commands, accepted by the peer's own command.
			type(c, "coven found Testers");
			await(c, () -> onServer(server, s -> Guilds.ledger(s).roster().of(host, GuildRules.Kind.COVEN) != null), "the host founds a coven");
			type(c, "coven invite " + peerName);
			peerDo(c, "expect", "message.wildercord.coven.invited");
			peerDo(c, "command", "coven accept");
			await(c, () -> onServer(server, s -> Guilds.ledger(s).roster().of(peer, GuildRules.Kind.COVEN) != null), "the peer joins the coven");
			check(onServer(server, s -> Guilds.ledger(s).roster().of(peer, GuildRules.Kind.COVEN).name().equals("Testers")), "into the host's coven");
			check(onServer(server, s -> Guilds.bonus(s.getPlayerList().getPlayer(host), GuildRules.Kind.COVEN) > 1.0), "a fellow member nearby earns the bonus");
			report("coven");

			// Mentoring: the offer outlasts the apprentice logging out and back in.
			type(c, "mentor take " + peerName);
			peerDo(c, "expect", "message.wildercord.mentor.offered");
			check(onServer(server, s -> Mentors.ledger(s).offered(peer)), "the offer is held");
			peerDo(c, "logout", "");
			await(c, () -> players(server) == 1, "the peer is gone from the server");
			check(onServer(server, s -> Mentors.ledger(s).offered(peer)), "the offer outlasts the logout");
			peerDo(c, "login", "");
			await(c, () -> players(server) == 2, "the peer is back");
			peerDo(c, "command", "mentor accept");
			await(c, () -> onServer(server, s -> host.equals(Mentors.ledger(s).book().mentorOf(peer))), "the peer is apprenticed");
			check(onServer(server, s -> Mentors.gain(s.getPlayerList().getPlayer(peer)) > 1.0), "an apprentice near the mentor learns faster");
			report("mentoring");

			// A party, then a tribulation the peer is drawn into.
			type(c, "party invite " + peerName);
			peerDo(c, "command", "party accept " + hostName);
			await(c, () -> onServer(server, s -> Parties.members(s.getPlayerList().getPlayer(host)).contains(peer)), "the peer joins the party");
			check(onServer(server, s -> Tribulation.begin(s.getPlayerList().getPlayer(host), 5)), "the host's tribulation begins");
			check(onServer(server, s -> Tribulation.party(s.getPlayerList().getPlayer(host)) == 1), "the peer fights it beside the host");
			check(onServer(server, s -> Tribulation.engaged(s.getPlayerList().getPlayer(peer))), "and is held in it");
			server.runOnServer(s -> Tribulation.cancel(s.getPlayerList().getPlayer(host)));
			check(!onServer(server, s -> Tribulation.engaged(s.getPlayerList().getPlayer(peer))), "ending it lets the peer go");
			report("coop_tribulation");

			// The arena: both step up to one stone and a ranked bout begins.
			server.runOnServer(s -> {
				s.overworld().setBlockAndUpdate(STONE, WildercordBlocks.ARENA_STONE.defaultBlockState());
				ServerPlayer h = s.getPlayerList().getPlayer(host), p = s.getPlayerList().getPlayer(peer);
				Duels.rested(h);
				Duels.rested(p);
				Arena.use(s.overworld(), STONE, h);
				check(!Duels.inDuel(h), "one caster alone only waits");
				Arena.use(s.overworld(), STONE, p);
				check(Duels.inDuel(h) && Duels.inDuel(p) && Duels.opponents(host, peer), "the second caster starts the bout");
			});
			peerDo(c, "expect", "message.wildercord.arena.begins");
			server.runOnServer(s -> Duels.callOff(s.getPlayerList().getPlayer(host)));
			report("arena");

			// The peer leaves its loop on "finish" without a peer-step answer; peer-disconnected is its answer.
			write(stepName("step"), Map.of("action", "finish", "arg", ""));
			await(c, () -> exists("peer-disconnected") && players(server) == 1, "the peer leaves");
			write("host-social-passed", Map.of("cases", "coven,mentoring,coop_tribulation,arena"));
		}
	}

	private void peer(ClientGameTestContext c) {
		await(c, () -> exists("host-ready"), "the host publishes its server");
		port = Integer.parseInt(read("host-ready").getProperty("port"));
		connect(c);
		write("peer-connected", Map.of("uuid", c.computeOnClient(mc -> mc.player.getUUID()).toString(),
			"name", c.computeOnClient(mc -> mc.getUser().getName())));
		try {
			while (true) {
				String name = stepName("step");
				await(c, () -> exists(name), "the host's next step");
				Properties order = read(name);
				String action = order.getProperty("action"), arg = order.getProperty("arg", "");
				if (action.equals("finish")) break;
				switch (action) {
					case "command" -> c.runOnClient(mc -> mc.player.connection.sendCommand(arg));
					case "expect" -> await(c, () -> MESSAGES.stream().anyMatch(m -> has(m, arg)), "the peer receives " + arg);
					case "logout" -> disconnect(c);
					case "login" -> connect(c);
					default -> throw new AssertionError("Unknown step " + action);
				}
				write(stepName("peer-step"), Map.of("action", action));
				step++;
			}
		} finally {
			disconnect(c);
			write("peer-disconnected", Map.of());
		}
		write("peer-social-passed", Map.of("steps", Integer.toString(step)));
	}

	/** The host's own client types a command, as a player would. */
	private static void type(ClientGameTestContext c, String command) {
		c.runOnClient(mc -> mc.player.connection.sendCommand(command));
		c.waitTicks(5);
	}

	/** Tells the peer to do one thing and waits until it has. */
	private void peerDo(ClientGameTestContext c, String action, String arg) {
		write(stepName("step"), Map.of("action", action, "arg", arg));
		String done = stepName("peer-step");
		await(c, () -> exists(done), "the peer does " + action + " " + arg);
		step++;
		c.waitTicks(5);
	}

	private void report(String name) {
		write("case-" + name + "-passed", Map.of("case", name));
	}

	private String stepName(String prefix) {
		return String.format(Locale.ROOT, "%s-%02d", prefix, step);
	}

	private void connect(ClientGameTestContext c) {
		String address = "127.0.0.1:" + port;
		c.runOnClient(mc -> ConnectScreen.startConnecting(mc.gui.screen(), mc, ServerAddress.parseString(address),
			new ServerData("Wildercord social fixture", address, ServerData.Type.OTHER), false, null));
		c.waitFor(mc -> mc.player != null && mc.level != null && mc.getConnection() != null, 2400);
	}

	private static void disconnect(ClientGameTestContext c) {
		c.runOnClient(mc -> {
			if (mc.level != null) mc.disconnect(new TitleScreen(), false);
			mc.gui.setScreen(new TitleScreen());
		});
		c.waitFor(mc -> mc.player == null && mc.level == null, 200);
	}

	private static boolean has(Component message, String key) {
		if (message.getContents() instanceof TranslatableContents t) {
			if (t.getKey().equals(key)) return true;
			for (Object arg : t.getArgs()) if (arg instanceof Component inner && has(inner, key)) return true;
		}
		for (Component sibling : message.getSiblings()) if (has(sibling, key)) return true;
		return false;
	}

	private static int players(TestDedicatedServerContext server) {
		return server.computeOnServer(s -> s.getPlayerList().getPlayers().size());
	}

	private static boolean onServer(TestDedicatedServerContext server, Function<MinecraftServer, Boolean> check) {
		return server.computeOnServer(check::apply);
	}

	private static boolean tcp(ServerPlayer player) {
		return player.connection.getRemoteAddress() instanceof InetSocketAddress address && address.getAddress().isLoopbackAddress();
	}

	private static int localPort() {
		try (var socket = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
			return socket.getLocalPort();
		} catch (java.io.IOException failure) {
			throw new AssertionError("Could not reserve a local port", failure);
		}
	}

	private void await(ClientGameTestContext c, BooleanSupplier condition, String reason) {
		while (System.nanoTime() < deadline) {
			check(!exists("host-failure") && !exists("peer-failure"), "both clients stay healthy: " + reason);
			if (condition.getAsBoolean()) return;
			c.waitTicks(1);
		}
		throw new AssertionError("Timed out: " + reason);
	}

	private boolean exists(String name) {
		Path file = directory.resolve(name + ".properties");
		return Files.exists(file) && nonce.equals(read(name).getProperty("nonce"));
	}

	private Properties read(String name) {
		try (var input = Files.newInputStream(directory.resolve(name + ".properties"))) {
			var p = new Properties();
			p.load(input);
			return p;
		} catch (java.io.IOException error) {
			throw new AssertionError(error);
		}
	}

	private void write(String name, Map<String, String> values) {
		try {
			var p = new Properties();
			p.setProperty("nonce", nonce);
			p.setProperty("role", role);
			p.putAll(values);
			Path temp = directory.resolve(name + ".tmp");
			try (var output = Files.newOutputStream(temp)) {
				p.store(output, "Wildercord social two-client step");
			}
			Files.move(temp, directory.resolve(name + ".properties"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.io.IOException error) {
			throw new AssertionError(error);
		}
	}

	private static String required(String key) {
		String value = System.getProperty("wildercord.mp." + key);
		check(value != null && !value.isBlank(), "Supervisor property " + key);
		return value;
	}

	private static void check(boolean value, String why) {
		if (!value) throw new AssertionError(why);
	}
}
