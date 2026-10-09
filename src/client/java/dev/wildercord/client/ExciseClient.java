package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.ExciseState;
import dev.wildercord.net.ExciseCores;
import dev.wildercord.content.LightOption;
import dev.wildercord.net.ExciseInput;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.ExciseRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Fresh physical edges and essential paid-field tells. Menus release input, never cancel released magic. */
public final class ExciseClient {
    private ExciseClient() {}
    private static final int COUNT = dev.wildercord.gear.SpellSlots.ALL + 1;
    private static final boolean[] DOWN = new boolean[COUNT], BLOCKED = new boolean[COUNT], HANDLED = new boolean[COUNT];
    private static net.minecraft.client.player.LocalPlayer body;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static boolean playing;
    private static long nonce;
    private static KeyMapping lastBinding;
    private static final int INK = 0xB7D58B, WARNING = 0xD9B473;
    private static java.util.List<ExciseCores.Core> cores = java.util.List.of();
    private static net.minecraft.client.multiplayer.ClientLevel coreWorld;
    private static long coresAt;
    private static final java.util.Map<java.util.UUID, Long> OUTCOMES = new java.util.HashMap<>();

    public static void beginTick(Minecraft client) {
        boolean active = client.player != null && client.player.isAlive() && !client.player.isSpectator() && client.gui.screen() == null && client.isWindowActive();
        if (body != client.player || world != client.level) {
            body = client.player; world = client.level;
            cores = java.util.List.of(); OUTCOMES.clear();
            java.util.Arrays.fill(DOWN, false); java.util.Arrays.fill(HANDLED, false); java.util.Arrays.fill(BLOCKED, true);
            playing = false;
        }
        if (!active) {
            if (playing) for (int i = 0; i < COUNT; i++) if (HANDLED[i]) { send(RelayInputRules.CANCEL, i - 1); HANDLED[i] = false; }
            java.util.Arrays.fill(BLOCKED, true);
        }
        playing = active;
    }
    public static boolean key(Minecraft client, KeyMapping binding, int requested) {
        int index = requested + 1;
        boolean down = binding.isDown(), previous = DOWN[index];
        DOWN[index] = down;
        int slot = client.player == null ? -1 : requested < 0 ? Spellbooks.get(client.player).selected() : requested;
        boolean row = client.player != null && slot >= 0 && slot < dev.wildercord.gear.SpellSlots.ALL
            && ExciseRules.containsIds(Spellbooks.get(client.player).spells().get(slot));
        boolean handles = row || HANDLED[index];
        if (!handles) return false;
        // A tap shorter than one tick is up at both reads; its queued click still counts as one whole press.
        boolean clicked = false;
        while (binding.consumeClick()) clicked = true;
        if (!playing) return true;
        // A menu/focus transition clears mapped keys. Only an observed in-game release rearms this edge.
        if (!down) BLOCKED[index] = false;
        if (client.player.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)) { BLOCKED[index] = down; return true; }
        int edge = RelayInputRules.edge(down, previous, clicked);
        if (edge == RelayInputRules.DOWN && !BLOCKED[index]) {
            HANDLED[index] = true; lastBinding = binding;
            send(client.player.isShiftKeyDown() ? RelayInputRules.CANCEL : RelayInputRules.DOWN, requested);
        } else if (edge == RelayInputRules.TAP && !BLOCKED[index]) {
            lastBinding = binding;
            if (client.player.isShiftKeyDown()) send(RelayInputRules.CANCEL, requested);
            else { send(RelayInputRules.DOWN, requested); send(RelayInputRules.UP, requested); }
        } else if (edge == RelayInputRules.UP && HANDLED[index]) {
            send(RelayInputRules.UP, requested); HANDLED[index] = false;
        }
        return true;
    }
    private static void send(int action, int requested) {
        if (body != null && ClientPlayNetworking.canSend(ExciseInput.TYPE))
            ClientPlayNetworking.send(new ExciseInput(action, requested, ++nonce));
    }
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ExciseCores.TYPE, (packet, context) -> {
            cores = packet.cores(); coreWorld = context.client().level;
            coresAt = coreWorld == null ? 0 : coreWorld.getGameTime();
        });
        ClientTickEvents.END_CLIENT_TICK.register(ExciseClient::draw);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildercord.id("excise_hud"), (g, delta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return;
            ExciseState s = client.player.getAttached(ExciseState.VIEW);
            if (s == null || s.clock(client.level.getGameTime()) >= s.until()) return;
            long now = s.clock(client.level.getGameTime());
            String key = (lastBinding == null ? WildercordKeys.castKey() : lastBinding.getTranslatedKeyMessage()).getString();
            String line = s.phase() == ExciseState.HOLDING ? "Hold " + key + " · cutting the locked knot"
                : s.phase() == ExciseState.CUT ? "Knot cut · recovering" : "Thread broken · paid recovery";
            String hint = s.phase() == ExciseState.HOLDING ? "Keep sight and stay within one block; release cancels"
                : "Mana and twelve-second shared rest remain spent";
            int width = Math.min(Math.max(client.font.width(line), client.font.width(hint)) + 12, g.guiWidth() - 8);
            int x = (g.guiWidth() - width) / 2, y = g.guiHeight() - 115;
            g.fill(x, y, x + width, y + 35, 0xDC18261C);
            g.text(client.font, client.font.plainSubstrByWidth(line, width - 8), x + 4, y + 4, 0xFFE0EEC8, true);
            g.text(client.font, client.font.plainSubstrByWidth(hint, width - 8), x + 4, y + 16, 0xFFC5CDB6, true);
            double progress = Math.clamp((now - s.began()) / (double) Math.max(1, s.until() - s.began()), 0, 1);
            g.fill(x + 4, y + 30, x + 4 + (int)((width - 8) * progress), y + 32, 0xFFB7D58B);
        });
    }
    private static void line(Minecraft client, Vec3 from, Vec3 to, int color) {
        Vec3 d = to.subtract(from);
        client.level.addParticle(new LightOption(LightOption.RAY, color, (float)d.x, (float)d.y, (float)d.z, .018F, 0, 0, 0, 3), from.x, from.y, from.z, 0, 0, 0);
    }
    private static void ring(Minecraft client, Vec3 at, double radius, int color, float pitch, int ticks) {
        client.level.addParticle(new LightOption(LightOption.RING, color, (float)radius, (float)radius, 0, .017F, 0, pitch, 0, ticks), at.x, at.y, at.z, 0, 0, 0);
    }
    private static void draw(Minecraft client) {
        if (client.level == null || client.player == null || client.level.getGameTime() % 2 != 0) return;
        long now = client.level.getGameTime();
        if (coreWorld == client.level && now - coresAt <= 12) for (ExciseCores.Core core : cores) {
            if (now - coresAt >= core.remaining()) continue;
            // A readable living knot: two narrow loops and four root spokes, with no screen flash.
            ring(client, core.at(), .26, INK, -90, 3);
            ring(client, core.at(), .18, INK, 0, 3);
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2 + (now % 80) * .02;
                Vec3 root = core.at().add(Math.cos(angle) * .4, -.38, Math.sin(angle) * .4);
                line(client, core.at(), root, INK);
            }
        }
        int shown = 0;
        for (var player : client.level.players()) {
            ExciseState state = player.getAttached(ExciseState.VIEW);
            if (state == null || state.clock(now) >= state.until() || player.distanceToSqr(client.player) > 64 * 64 || shown++ >= 16) continue;
            Vec3 from = player == client.player && client.options.getCameraType().isFirstPerson()
                ? player.getEyePosition().add(player.getLookAngle().scale(.5)).add(0, -.25, 0)
                : player.getEyePosition().add(0, -.5, 0);
            if (state.phase() == ExciseState.HOLDING) {
                line(client, from, state.core(), INK);
                double left = Math.clamp((state.until() - state.clock(now)) / 16.0, 0, 1);
                ring(client, state.core(), .1 + left * .45, WARNING, 0, 3);
            } else if (!java.util.Objects.equals(OUTCOMES.put(player.getUUID(), state.began()), state.began())) {
                int color = state.phase() == ExciseState.CUT ? 0xE9D995 : 0x998884;
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 2;
                    Vec3 end = state.core().add(Math.cos(angle) * .65, .12, Math.sin(angle) * .65);
                    line(client, state.core().add(Math.cos(angle) * .24, 0, Math.sin(angle) * .24), end, color);
                }
            }
        }
    }
}
