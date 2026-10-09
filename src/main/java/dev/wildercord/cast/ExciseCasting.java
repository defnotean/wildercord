package dev.wildercord.cast;

import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.content.CordTier;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.ExciseRules;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** One paid held action owns one exact emitter, never a Cast or a new target. */
public final class ExciseCasting {
    private ExciseCasting() {}
    private static final java.util.Set<java.util.UUID> ADMITTING = new java.util.HashSet<>();
    private static final Map<ServerPlayer, Focus> FOCI = new IdentityHashMap<>();
    private static final Map<ServerPlayer, RelayInputRules.Edges[]> INPUT = new WeakHashMap<>();
    private static final class Focus {
        final ServerPlayer player;
        final ReleasedArtOwner owner;
        final ServerLevel level;
        final NativeZoneEmitters.Emitter emitter;
        final ItemStack cord;
        final Vec3 start;
        final int slot, requested, selected, cost;
        final long began;
        boolean retired;
        Focus(ServerPlayer p, int slot, int requested, int cost, NativeZoneEmitters.Emitter emitter) {
            this.player = p; this.owner = ReleasedArtOwner.capture(p); this.level = p.level(); this.emitter = emitter;
            this.cord = Spellbooks.cord(p); this.start = p.position(); this.slot = slot; this.requested = requested; this.selected = Spellbooks.get(p).selected();
            this.cost = cost; this.began = now(p);
        }
        boolean valid() {
            return !retired && owner.valid() && available(player) && row(player, slot) && Spellbooks.cord(player) == cord
                && Spellbooks.get(player).selected() == selected && now(player) >= began
                && player.position().distanceToSqr(start) <= ExciseRules.MAX_DISPLACEMENT * ExciseRules.MAX_DISPLACEMENT
                && NativeZoneEmitters.eligible(player, emitter);
        }
    }
    public static long now(ServerPlayer p) { return NativeZoneEmitters.clock(p.level()); }
    public static long rest(ServerPlayer p) { return p.getAttachedOrElse(ExciseState.REST, 0L); }
    public static long recoveryUntil(ServerPlayer p) {
        long until = p.getAttachedOrElse(ExciseState.RECOVERY, 0L);
        return ExciseRules.recoveryLeft(now(p), until) > 0 ? until : 0;
    }
    public static boolean committed(ServerPlayer p) { return FOCI.containsKey(p) || recoveryUntil(p) != 0; }
    public static boolean blocking(ServerPlayer p) { return committed(p) || ADMITTING.contains(p.getUUID()); }
    public static boolean pending(ServerPlayer p) { return FOCI.containsKey(p); }
    public static boolean contains(ServerPlayer p, int requested) {
        int slot = requested < 0 ? Spellbooks.get(p).selected() : requested;
        return slot >= 0 && slot < dev.wildercord.gear.SpellSlots.ALL && ExciseRules.containsIds(Spellbooks.get(p).spells().get(slot));
    }
    private static boolean entitled(ServerPlayer p) { return MasterStudies.knowsExcise(p) && MasterStudies.eligibleExcise(p); }
    private static boolean row(ServerPlayer p, int slot) {
        return slot >= 0 && slot < CordTier.ECHO.spells && Spellbooks.tier(p) == CordTier.ECHO && entitled(p)
            && dev.wildercord.gear.Gear.spellOpen(p, CordTier.ECHO, slot)
            && ExciseRules.IDS.equals(Spellbooks.get(p).spells().get(slot)) && ExciseRules.IDS.stream().allMatch(Spellbooks.get(p)::knows);
    }
    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isRemoved() && !p.isSpectator() && !p.isSleeping() && !p.isPassenger()
            && p.level().getServer().getPlayerList().getPlayer(p.getUUID()) == p
            && !CastLock.locked(p) && !dev.wildercord.aura.arts.ArtWards.silenced(p) && !VoidTime.hushed(p) && !FusedFrostWards.sealed(p)
            && !dev.wildercord.aura.MastersArts.committed(p) && !dev.wildercord.aura.MasterForms.committed(p) && !RelayCircles.committed(p) && !RelayCircles.pending(p)
            && !dev.wildercord.aura.Clashes.holding(p) && !dev.wildercord.aura.AuraGuard.guarding(p) && !dev.wildercord.aura.Stance.opened(p)
            && p.containerMenu == p.inventoryMenu && !p.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)
            && Float.isFinite(p.getXRot()) && Float.isFinite(p.getYRot()) && NativeZoneEmitters.finite(p.position());
    }
    public static String problem(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= CordTier.ECHO.spells) return "Excise needs an ordinary Echo Cord slot.";
        if (!ExciseRules.IDS.equals(Spellbooks.get(p).spells().get(slot))) return ExciseRules.GRAMMAR_PROBLEM;
        if (!entitled(p)) return "Study The Root That Outlived Its Gardener in Grimoire > Master studies with active Circle XVI and Heartwood.";
        return row(p, slot) ? null : "Excise needs your Echo Cord and both learned runes.";
    }
    private static void fail(ServerPlayer p, String text) { p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(text).withColor(0xD4CEA2)); }
    public static void input(ServerPlayer p, int action, int requested, long nonce) {
        try (var admission = ActionAdmission.begin(p)) {
            if (admission == null) return;
            ADMITTING.add(p.getUUID());
            try {
            if (requested < -1 || requested >= dev.wildercord.gear.SpellSlots.ALL
                || !INPUT.computeIfAbsent(p, ignored -> java.util.stream.IntStream.range(0, dev.wildercord.gear.SpellSlots.ALL + 1)
                    .mapToObj(i -> new RelayInputRules.Edges()).toArray(RelayInputRules.Edges[]::new))[requested + 1].accept(action, nonce, now(p))) return;
            if (action == RelayInputRules.UP || action == RelayInputRules.CANCEL) {
                Focus held = FOCI.get(p); if (held != null && held.requested == requested) cancel(p); return;
            }
            if (action != RelayInputRules.DOWN || committed(p)) return;
            int slot = requested < 0 ? Spellbooks.get(p).selected() : requested;
            String problem = problem(p, slot);
            if (problem != null) { fail(p, problem); return; }
            if (!available(p)) return;
            long tick = now(p);
            if (tick < 0 || tick > Long.MAX_VALUE - ExciseRules.REST_TICKS) return;
            if (tick < rest(p) || p.level().getGameTime() < Spellbooks.readyAt(p, slot)) { fail(p, "Excise rests for twelve seconds, shared across slots."); return; }
            if (WildSurge.freeRecast(p, p.level().getGameTime())) { fail(p, "Excise cannot spend a free recast."); return; }
            NativeZoneEmitters.Emitter emitter = NativeZoneEmitters.select(p);
            if (emitter == null) { fail(p, "Aim at a visible hostile Zone knot within twelve blocks."); return; }
            var compiled = SpellCompiler.compile(ExciseRules.RUNES);
            int cost = Heart.manaCost(p, compiled, Mastery.costFactor(p, ExciseRules.RUNES));
            if (cost <= 0 || !affords(p, cost)) { fail(p, "Excise needs " + cost + " mana and cannot overcast."); return; }
            Focus focus = new Focus(p, slot, requested, cost, emitter);
            var book = Spellbooks.get(p); var gear = dev.wildercord.gear.Gear.of(p); int circles = Heart.active(p);
            if (!dev.wildercord.api.WildercordEvents.BEFORE_CAST.invoker().allow(p, slot, ExciseRules.RUNES, cost)) return;
            if (!admission.valid() || !focus.valid() || committed(p) || !book.equals(Spellbooks.get(p)) || !gear.equals(dev.wildercord.gear.Gear.of(p))
                || Heart.active(p) != circles || tick != now(p) || tick < rest(p) || p.level().getGameTime() < Spellbooks.readyAt(p, slot)
                || WildSurge.freeRecast(p, p.level().getGameTime()) || !affords(p, cost)) return;
            float mana = Spellbooks.mana(p);
            // Reserve before callbacks. No other action, secondary payment, retarget, overcast or refund.
            if (!p.isCreative()) Spellbooks.setMana(p, mana - cost);
            p.setAttached(ExciseState.REST, tick + ExciseRules.REST_TICKS);
            Spellbooks.setReadyAt(p, slot, p.level().getGameTime() + ExciseRules.REST_TICKS);
            FOCI.put(p, focus);
            if (!p.isCreative()) dev.wildercord.aura.Unity.manaSpent(p, cost);
            if (!focus.valid()) { cancel(p); return; }
            HeartCircles.condense(p, cost); PlayerAffinities.onCast(p, compiled.root(), cost, 0); HeartCircles.onCast(p);
            if (!focus.valid()) { cancel(p); return; }
            dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(p, slot, ExciseRules.RUNES, cost);
            if (!focus.valid()) cancel(p); else {
                publish(focus, ExciseState.HOLDING, tick + ExciseRules.COMMIT_TICKS);
                dev.wildercord.cast.feel.Feels.sound(p.level(), p.position(), "life_stinger_thorn", .3F, 1.12F);
            }
            } finally { ADMITTING.remove(p.getUUID()); }
        }
    }
    private static boolean affords(ServerPlayer p, int cost) { return p.isCreative() || Float.isFinite(Spellbooks.mana(p)) && Spellbooks.mana(p) >= cost; }
    private static void publish(Focus f, int phase, long until) {
        f.player.setAttached(ExciseState.VIEW, new ExciseState(phase, f.slot, f.emitter.id, phase == ExciseState.HOLDING ? f.began : now(f.player), until,
            f.emitter.core, now(f.player), f.player.level().getGameTime()));
    }
    private static void finish(Focus f, boolean cut) {
        if (!FOCI.remove(f.player, f)) return;
        f.retired = true;
        long until = ExciseRules.recoveryDeadline(now(f.player));
        f.player.setAttached(ExciseState.RECOVERY, until);
        publish(f, cut ? ExciseState.CUT : ExciseState.CANCELLED, until);
        if (f.player.level() == f.level && f.player.isAlive())
            dev.wildercord.cast.feel.Feels.sound(f.level, f.emitter.core, "life_stinger_thorn", .35F, cut ? 1.5F : .7F);
        if (cut) { Mastery.onExcise(f.player); MasterStudies.completeExcisePractice(f.player); }
    }
    public static void cancel(ServerPlayer p) { Focus f = FOCI.get(p); if (f != null) finish(f, false); }
    /** Lifecycle clears every body-bound reference; existing deadlines are never renewed by this cleanup. */
    public static void retire(ServerPlayer p) { cancel(p); INPUT.remove(p); p.removeAttached(ExciseState.VIEW); }
    private static void clearExpiredRecovery(ServerPlayer player) {
        if (recoveryUntil(player) != 0) return;
        if (player.hasAttached(ExciseState.RECOVERY)) player.removeAttached(ExciseState.RECOVERY);
        if (!FOCI.containsKey(player) && player.hasAttached(ExciseState.VIEW)) player.removeAttached(ExciseState.VIEW);
    }
    public static void init() {
        dev.wildercord.net.ExciseInput.init(); NativeZoneEmitters.init();
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (dev.wildercord.aura.MastersArtRules.interrupts(base, taken, blocked) && entity instanceof ServerPlayer p) cancel(p);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer p) retire(p); });
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p, from, to) -> retire(p));
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register(dev.wildercord.aura.Aura.AFTER_COPY, (old, fresh, alive) -> {
            retire(old);
            long until = recoveryUntil(old);
            if (until != 0) fresh.setAttached(ExciseState.RECOVERY, until); else fresh.removeAttached(ExciseState.RECOVERY);
            fresh.removeAttached(ExciseState.VIEW); INPUT.remove(fresh);
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handler.player.removeAttached(ExciseState.VIEW); INPUT.remove(handler.player); clearExpiredRecovery(handler.player);
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retire(handler.player));
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (Focus f : List.copyOf(FOCI.values())) {
                if (f.level.getServer() != server) continue;
                if (!f.valid()) { finish(f, false); continue; }
                if (now(f.player) >= f.began + ExciseRules.COMMIT_TICKS) {
                    try (var admission = ActionAdmission.begin(f.player)) {
                        if (admission == null || !admission.valid() || !f.valid()) finish(f, false);
                        else finish(f, NativeZoneEmitters.cut(f.player, f.emitter));
                    }
                }
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) clearExpiredRecovery(player);
        });
        // STOPPING is before the player save; STOPPED is too late to save paid cancellation recovery.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Focus focus : List.copyOf(FOCI.values())) if (focus.level.getServer() == server) retire(focus.player);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { FOCI.clear(); INPUT.clear(); ADMITTING.clear(); });
    }
}
