package dev.wildercord.cast;

import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.content.CordTier;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.LessonPackRules;
import dev.wildercord.spell.LessonPackRules.Lesson;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * One fresh paid press places one owned object: a Tollgate threshold, a Lifeline thread or a Conduit rod.
 * A later fresh press on a standing thread or rod uses it for free; sneak and press lifts it. Every object dies with its owner.
 */
public final class LessonPackCasting {
    private LessonPackCasting() {}
    private static final Set<UUID> ADMITTING = new HashSet<>();
    private static final Map<ServerPlayer, RelayInputRules.Edges[]> INPUT = new WeakHashMap<>();
    private static final Map<ServerPlayer, Gate> GATES = new IdentityHashMap<>();
    private static final Map<ServerPlayer, Line> LINES = new IdentityHashMap<>();
    private static final Map<ServerPlayer, Rod> RODS = new IdentityHashMap<>();
    private static final Map<ServerPlayer, Long> LAST_DOWN = new WeakHashMap<>();
    private static final Map<ServerPlayer, String> REFUSED = new WeakHashMap<>();
    static final String MALFORMED_ROW = "This row holds a master lesson rune it cannot read. Rebuild it in the Codex.";

    private abstract static class Placed {
        final ServerPlayer player; final ReleasedArtOwner owner; final ServerLevel level; final Lesson lesson; final int slot;
        final LessonPackRules.Timeline time;
        boolean retired;
        Placed(ServerPlayer p, Lesson lesson, int slot, int ticks) {
            this.player = p; this.owner = ReleasedArtOwner.capture(p); this.level = p.level(); this.lesson = lesson; this.slot = slot;
            this.time = LessonPackRules.Timeline.start(now(p), ticks);
        }
        boolean valid() { return !retired && owner.valid() && !player.isSpectator() && !time.expired(now(player)); }
    }
    private static final class Gate extends Placed {
        final Vec3 centre; final double ax, az, casterAcross;
        final Set<UUID> tolled = new HashSet<>();
        final Map<UUID, Double> lastAcross = new HashMap<>();
        int tolls = LessonPackRules.GATE_TOLLS;
        boolean practiced;
        Gate(ServerPlayer p, int slot, Vec3 centre, double ax, double az) {
            super(p, Lesson.TOLLGATE, slot, LessonPackRules.GATE_TICKS);
            this.centre = centre; this.ax = ax; this.az = az;
            this.casterAcross = LessonPackRules.across(p.getX() - centre.x, p.getZ() - centre.z, ax, az);
        }
    }
    private static final class Line extends Placed {
        final LivingEntity ally;
        Line(ServerPlayer p, int slot, LivingEntity ally) { super(p, Lesson.LIFELINE, slot, LessonPackRules.LINE_TICKS); this.ally = ally; }
        @Override boolean valid() {
            return super.valid() && ally.isAlive() && !ally.isRemoved() && !ally.isSpectator() && ally.level() == level && Targets.canHelp(player, ally)
                && player.distanceToSqr(ally) <= LessonPackRules.LINE_BREAK * LessonPackRules.LINE_BREAK;
        }
    }
    private static final class Rod extends Placed {
        final Vec3 top;
        long spark = -1;
        Rod(ServerPlayer p, int slot, Vec3 top) { super(p, Lesson.CONDUIT, slot, LessonPackRules.ROD_TICKS); this.top = top; }
    }

    /** One server-wide clock so a dimension change can neither shorten nor renew a rest. */
    public static long now(ServerPlayer p) { return p.level().getServer().overworld().getGameTime(); }
    public static long rest(ServerPlayer p, Lesson lesson) { return p.getAttachedOrElse(LessonPackState.rest(lesson), 0L); }
    private static Lesson lessonAt(ServerPlayer p, int slot) {
        return slot >= 0 && slot < dev.wildercord.gear.SpellSlots.ALL ? LessonPackRules.lessonOfIds(Spellbooks.get(p).spells().get(slot)) : null;
    }
    public static boolean contains(ServerPlayer p, int requested) { return lessonAt(p, requested < 0 ? Spellbooks.get(p).selected() : requested) != null; }
    private static boolean entitled(ServerPlayer p, Lesson lesson) { return MasterStudies.knows(p, lesson) && MasterStudies.eligible(p, lesson); }
    private static boolean row(ServerPlayer p, int slot, Lesson lesson) {
        return slot >= 0 && slot < CordTier.ECHO.spells && Spellbooks.tier(p) == CordTier.ECHO && entitled(p, lesson)
            && dev.wildercord.gear.Gear.spellOpen(p, CordTier.ECHO, slot)
            && lesson.ids.equals(Spellbooks.get(p).spells().get(slot)) && lesson.ids.stream().allMatch(Spellbooks.get(p)::knows);
    }
    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isRemoved() && !p.isSpectator() && !p.isSleeping() && !p.isPassenger()
            && p.level().getServer().getPlayerList().getPlayer(p.getUUID()) == p
            && !CastLock.locked(p) && !dev.wildercord.aura.arts.ArtWards.silenced(p) && !VoidTime.hushed(p) && !FusedFrostWards.sealed(p)
            && !dev.wildercord.aura.MastersArts.committed(p) && !dev.wildercord.aura.MasterForms.committed(p) && !RelayCircles.committed(p) && !RelayCircles.pending(p)
            && !dev.wildercord.aura.Clashes.holding(p) && !dev.wildercord.aura.AuraGuard.guarding(p) && !dev.wildercord.aura.Stance.opened(p)
            && !ExciseCasting.committed(p) && p.containerMenu == p.inventoryMenu && !p.hasAttached(WildercordAttachments.CHARGE)
            && Float.isFinite(p.getXRot()) && Float.isFinite(p.getYRot()) && NativeZoneEmitters.finite(p.position());
    }
    /** Mid-admission or mid-spark: other actions wait, as they do for Excise. */
    public static boolean blocking(ServerPlayer p) {
        Rod rod = RODS.get(p);
        return ADMITTING.contains(p.getUUID()) || rod != null && rod.spark >= 0;
    }
    /** Why a threaded ally cannot be (or stay) threaded: checked when threading and again at the reel. */
    private static String allyProblem(ServerPlayer p, LivingEntity ally) {
        String name = ally.getName().getString();
        if (ally.level() != p.level() || !ally.isAlive() || ally.isRemoved()) return "The Lifeline needs an ally here with you.";
        if (ally.isSpectator() || ally instanceof Player body && body.isCreative()) return name + " cannot be threaded now.";
        if (ally.isSleeping()) return name + " is asleep.";
        if (ally instanceof ServerPlayer f && (dev.wildercord.duel.Duels.inDuel(f) || dev.wildercord.aura.world.DuelistDuels.inDuel(f) || dev.wildercord.aura.Spars.sparring(f)))
            return name + " is in a duel.";
        if (!ally.level().getEntitiesOfClass(dev.wildercord.aura.world.SwordMaster.class, ally.getBoundingBox().inflate(dev.wildercord.aura.world.MastersRules.ARENA_RADIUS * 2),
            m -> m.canHarmParticipant(ally) || m.acceptsHarmFrom(ally)).isEmpty()) return name + " is in a Sword Master's trial.";
        if (ally instanceof ServerPlayer f && (dev.wildercord.aura.MasterForms.committed(f) || dev.wildercord.aura.MastersArts.committed(f) || ExciseCasting.blocking(f)
            || blocking(f) || RelayCircles.committed(f) || ActionAdmission.busy(f) || f.hasAttached(WildercordAttachments.CHARGE)
            || dev.wildercord.aura.Clashes.holding(f) || dev.wildercord.aura.AuraGuard.guarding(f) || dev.wildercord.aura.Stance.opened(f)))
            return name + " is in the middle of something.";
        return null;
    }
    public static String problem(ServerPlayer p, int slot) {
        Lesson lesson = lessonAt(p, slot);
        if (lesson == null) return null;
        if (LessonPackRules.malformedIds(Spellbooks.get(p).spells().get(slot))) return MALFORMED_ROW;
        if (slot >= CordTier.ECHO.spells) return lesson.name + " needs an ordinary Echo Cord slot.";
        if (!lesson.ids.equals(Spellbooks.get(p).spells().get(slot))) return lesson.grammarProblem;
        if (!entitled(p, lesson)) return "Study " + lesson.title + " in Grimoire > Master studies with active Circle " + lesson.numeral + " and " + lesson.featName + ".";
        return row(p, slot, lesson) ? null : lesson.name + " needs your Echo Cord and both learned runes.";
    }
    private static void fail(ServerPlayer p, String text) { REFUSED.put(p, text); p.sendOverlayMessage(Component.literal(text).withColor(0xD4CEA2)); }
    static String lastRefusal(ServerPlayer p) { return REFUSED.get(p); }
    static void forgetRefusal(ServerPlayer p) { REFUSED.remove(p); }
    static boolean sparking(ServerPlayer p) { Rod rod = RODS.get(p); return rod != null && rod.spark >= 0; }

    public static void input(ServerPlayer p, int action, int requested, long nonce) {
        try (var admission = ActionAdmission.begin(p)) {
            if (admission == null) return;
            ADMITTING.add(p.getUUID());
            try {
                if (requested < -1 || requested >= dev.wildercord.gear.SpellSlots.ALL) return;
                int slot = requested < 0 ? Spellbooks.get(p).selected() : requested;
                if (slot < 0 || slot >= dev.wildercord.gear.SpellSlots.ALL) return;
                long tick = now(p);
                if (!INPUT.computeIfAbsent(p, ignored -> java.util.stream.IntStream.range(0, dev.wildercord.gear.SpellSlots.ALL)
                        .mapToObj(i -> new RelayInputRules.Edges()).toArray(RelayInputRules.Edges[]::new))[slot].accept(action, nonce, tick)) return;
                if (action == RelayInputRules.DOWN) { Long last = LAST_DOWN.put(p, tick); if (last != null && last == tick) return; }
                Lesson lesson = lessonAt(p, slot);
                if (lesson == null || action == RelayInputRules.UP) return;
                if (LessonPackRules.malformedIds(Spellbooks.get(p).spells().get(slot))) { fail(p, MALFORMED_ROW); return; }
                if (action == RelayInputRules.CANCEL) { lift(p, lesson, true); return; }
                if (action != RelayInputRules.DOWN) return;
                String problem = problem(p, slot);
                if (problem != null) { fail(p, problem); return; }
                if (!available(p)) return;
                // A standing thread or rod is used, not re-bought: the reel and the arrival cost nothing more.
                Line line = LINES.get(p);
                if (lesson == Lesson.LIFELINE && line != null && line.valid()) { reel(line); return; }
                Rod rod = RODS.get(p);
                if (lesson == Lesson.CONDUIT && rod != null && rod.valid()) { spark(rod); return; }
                pay(p, admission, slot, lesson);
            } finally { ADMITTING.remove(p.getUUID()); }
        }
    }

    private static void pay(ServerPlayer p, ActionAdmission admission, int slot, Lesson lesson) {
        long tick = now(p);
        if (tick < 0 || tick > Long.MAX_VALUE - lesson.restTicks) return;
        if (tick < rest(p, lesson) || p.level().getGameTime() < Spellbooks.readyAt(p, slot)) {
            fail(p, lesson.name + " rests for " + lesson.seconds() + " seconds, shared across slots."); return;
        }
        if (WildSurge.freeRecast(p, p.level().getGameTime())) { fail(p, lesson.name + " cannot spend a free recast."); return; }
        Placed placed = switch (lesson) { case TOLLGATE -> gate(p, slot); case LIFELINE -> thread(p, slot); case CONDUIT -> plant(p, slot); };
        if (placed == null) return;
        var compiled = SpellCompiler.compile(lesson.runes);
        int cost = Heart.manaCost(p, compiled, Mastery.costFactor(p, lesson.runes));
        if (cost <= 0 || !affords(p, cost)) { fail(p, lesson.name + " needs " + cost + " mana and cannot overcast."); return; }
        var book = Spellbooks.get(p); var gear = dev.wildercord.gear.Gear.of(p); int circles = Heart.active(p);
        if (!dev.wildercord.api.WildercordEvents.BEFORE_CAST.invoker().allow(p, slot, lesson.runes, cost)) return;
        if (!admission.valid() || !placed.valid() || !available(p) || !row(p, slot, lesson) || !book.equals(Spellbooks.get(p))
            || !gear.equals(dev.wildercord.gear.Gear.of(p)) || Heart.active(p) != circles || tick != now(p) || tick < rest(p, lesson)
            || p.level().getGameTime() < Spellbooks.readyAt(p, slot) || WildSurge.freeRecast(p, p.level().getGameTime()) || !affords(p, cost)) return;
        // Pay before callbacks. One payment, no overcast, no refund.
        if (!p.isCreative()) Spellbooks.setMana(p, Spellbooks.mana(p) - cost);
        p.setAttached(LessonPackState.rest(lesson), tick + lesson.restTicks);
        Spellbooks.setReadyAt(p, slot, p.level().getGameTime() + lesson.restTicks);
        lift(p, lesson, false);
        switch (placed) { case Gate g -> GATES.put(p, g); case Line l -> LINES.put(p, l); case Rod r -> RODS.put(p, r); default -> {} }
        if (!p.isCreative()) dev.wildercord.aura.Unity.manaSpent(p, cost);
        HeartCircles.condense(p, cost); PlayerAffinities.onCast(p, compiled.root(), cost, 0); HeartCircles.onCast(p);
        dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(p, slot, lesson.runes, cost);
        p.setAttached(WildercordAttachments.CAST_POSE, new WildercordAttachments.CastPose(lesson.shape.id(), p.level().getGameTime()));
        FormationVfx.send(p, Vfx.theme(lesson.rune), lesson.runes);
        publish(placed);
        switch (placed) {
            case Gate g -> Fx.sound(g.level, g.centre, SoundEvents.BELL_BLOCK, .5F, .7F);
            case Line l -> {
                Fx.sound(l.level, l.ally.position(), SoundEvents.LEAD_TIED, .9F, 1.3F);
                if (l.ally instanceof ServerPlayer friend) friend.sendSystemMessage(Component.translatable("message.wildercord.lifeline_lesson.offered", p.getDisplayName()));
            }
            case Rod r -> Fx.sound(r.level, r.top, SoundEvents.LIGHTNING_BOLT_IMPACT, .35F, 1.7F);
            default -> {}
        }
    }
    private static boolean affords(ServerPlayer p, int cost) { return p.isCreative() || Float.isFinite(Spellbooks.mana(p)) && Spellbooks.mana(p) >= cost; }

    /** Floor in sight that the caster may build on, within the same movement ward as the caster. */
    private static Vec3 floor(ServerPlayer p, double range, String lessonName) {
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP || !p.level().getFluidState(hit.getBlockPos()).isEmpty()) {
            fail(p, lessonName + " needs open ground in sight within " + (int) range + " blocks."); return null;
        }
        BlockPos above = hit.getBlockPos().above();
        if (!Casters.mayEdit(p, p.level(), above) || !sameWard(p.level(), above, p.blockPosition())) {
            fail(p, lessonName + " cannot be set on ground you may not change."); return null;
        }
        return hit.getLocation();
    }
    private static boolean sameWard(ServerLevel level, BlockPos a, BlockPos b) {
        var wa = DungeonWards.movementWard(level, a);
        return wa.known() && wa.equals(DungeonWards.movementWard(level, b));
    }
    private static Gate gate(ServerPlayer p, int slot) {
        Vec3 at = floor(p, LessonPackRules.GATE_RANGE, "Tollgate");
        if (at == null) return null;
        Vec3 look = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
        if (look.lengthSqr() < 1e-4) look = Vec3.directionFromRotation(0, p.getYRot());
        look = look.normalize();
        return new Gate(p, slot, at, -look.z, look.x);
    }
    private static Line thread(ServerPlayer p, int slot) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(LessonPackRules.LINE_RANGE));
        var hit = ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
            e -> e != p && e instanceof LivingEntity && !e.isSpectator() && Targets.canHelp(p, e), LessonPackRules.LINE_RANGE * LessonPackRules.LINE_RANGE);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity ally) || !p.hasLineOfSight(ally)) {
            fail(p, "Lifeline needs an ally in sight within 16 blocks."); return null;
        }
        String problem = allyProblem(p, ally);
        if (problem != null) { fail(p, problem); return null; }
        return new Line(p, slot, ally);
    }
    private static Rod plant(ServerPlayer p, int slot) {
        Vec3 at = floor(p, LessonPackRules.ROD_RANGE, "Conduit");
        return at == null ? null : new Rod(p, slot, at);
    }

    /** The ally consents by not crouching; the landing must be safe, buildable and in the caster's own ward. */
    private static void reel(Line line) {
        ServerPlayer p = line.player; LivingEntity ally = line.ally; ServerLevel level = line.level;
        boolean crouching = ally instanceof Player friend && friend.isShiftKeyDown();
        if (crouching && LessonPackRules.consentOpen(line.time.created(), now(p))) { refuse(line); return; }
        if (crouching) { fail(p, ally.getName().getString() + " is holding fast."); return; }
        if (!LessonPackRules.mayReel(line.time.created(), now(p), false)) { fail(p, ally.getName().getString() + " has a moment to refuse the Lifeline."); return; }
        String problem = p.level() != level ? "The Lifeline needs an ally here with you." : allyProblem(p, ally);
        if (problem != null) { fail(p, problem); return; }
        if (ally.isPassenger() || ally.isVehicle() || !p.hasLineOfSight(ally)) { fail(p, "The Lifeline needs a clear, free ally."); return; }
        if (!Casters.mayEdit(p, level, ally.blockPosition()) || !sameWard(level, ally.blockPosition(), p.blockPosition())) {
            fail(p, "The Lifeline cannot pull from there."); return;
        }
        Vec3 best = null; double bestDistance = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            Vec3 feet = new Vec3(Math.floor(p.getX()) + dx + .5, p.getY(), Math.floor(p.getZ()) + dz + .5);
            BlockPos cell = BlockPos.containing(feet);
            if (!Effects.safeSpot(level, ally, feet) || !level.getFluidState(cell).isEmpty() || !level.getFluidState(cell.above()).isEmpty()
                || !Casters.mayEdit(p, level, cell) || !sameWard(level, cell, p.blockPosition())) continue;
            double d = feet.distanceToSqr(ally.position());
            if (d < bestDistance) { best = feet; bestDistance = d; }
        }
        if (best == null) { fail(p, "No safe ground beside you for the Lifeline."); return; }
        if (ally.level() != level || p.level() != level) return;
        Vec3 from = ally.position();
        if (ally instanceof ServerPlayer friend) friend.teleportTo(level, best.x, best.y, best.z, Set.of(), friend.getYRot(), friend.getXRot(), false);
        else { ally.teleportTo(best.x, best.y, best.z); if (ally instanceof Mob mob) mob.getNavigation().stop(); }
        ally.setDeltaMovement(Vec3.ZERO); ally.resetFallDistance();
        Light.ray(level, from.add(0, ally.getBbHeight() * .5, 0), best.add(0, ally.getBbHeight() * .5, 0), Vfx.theme(line.lesson.rune).primary(), .08, 10);
        Fx.sound(level, best, SoundEvents.AMETHYST_BLOCK_RESONATE, .9F, 1.4F);
        retire(line);
        MasterStudies.completePractice(p, Lesson.LIFELINE);
    }

    /** A player who crouches while the consent gap is open declines the thread; the caster's payment stays spent. */
    private static void refuse(Line line) {
        retire(line);
        fail(line.player, line.ally.getName().getString() + " refused the Lifeline.");
        if (line.ally instanceof ServerPlayer friend) friend.sendSystemMessage(Component.translatable("message.wildercord.lifeline_lesson.refused"));
    }

    private static void spark(Rod rod) {
        if (rod.spark >= 0) return;
        rod.spark = now(rod.player);
        rod.player.setAttached(WildercordAttachments.CAST_POSE, new WildercordAttachments.CastPose(rod.lesson.shape.id(), rod.player.level().getGameTime()));
        Fx.sound(rod.level, rod.player.position(), SoundEvents.BEACON_POWER_SELECT, .7F, 1.8F);
        publish(rod);
    }
    /** Arrival needs the rod in sight and in reach, a safe landing in the same ward, and no foe grounding it. */
    private static void arrive(Rod rod) {
        ServerPlayer p = rod.player; ServerLevel level = rod.level;
        rod.spark = -1;
        if (p.level() != level) { retire(rod); return; }
        Vec3 eye = p.getEyePosition(), aim = rod.top.add(0, .5, 0);
        BlockPos cell = BlockPos.containing(rod.top);
        if (!available(p) || p.position().distanceToSqr(rod.top) > LessonPackRules.ROD_ARRIVE * LessonPackRules.ROD_ARRIVE
            || level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() != HitResult.Type.MISS) {
            fail(p, "The Conduit rod is out of sight or reach."); publish(rod); return;
        }
        if (!Casters.mayEdit(p, level, cell) || !sameWard(level, cell, p.blockPosition()) || !Effects.safeSpot(level, p, rod.top)) {
            fail(p, "The Conduit rod has no safe landing."); publish(rod); return;
        }
        boolean grounded = !level.getEntitiesOfClass(LivingEntity.class, new AABB(rod.top, rod.top).inflate(LessonPackRules.ROD_GROUND + 1),
            e -> e instanceof Enemy && Targets.canHarm(p, e) && LessonPackRules.grounded(e.position().distanceTo(rod.top))).isEmpty();
        if (grounded) {
            fail(p, "A foe grounds the Conduit rod.");
            Fx.sound(level, rod.top, SoundEvents.BEACON_DEACTIVATE, .6F, 1.6F); publish(rod); return;
        }
        Vec3 from = p.position();
        p.teleportTo(level, rod.top.x, rod.top.y, rod.top.z, Set.of(), p.getYRot(), p.getXRot(), false);
        p.setDeltaMovement(Vec3.ZERO); p.resetFallDistance();
        int color = Vfx.theme(rod.lesson.rune).primary();
        Light.ray(level, from.add(0, 1, 0), rod.top.add(0, 1, 0), color, .12, 8);
        Fx.particle(level, ParticleTypes.ELECTRIC_SPARK, rod.top.add(0, 1, 0), 24, .5, .1);
        Fx.sound(level, rod.top, SoundEvents.TRIDENT_THUNDER, .45F, 1.9F);
        retire(rod);
        MasterStudies.completePractice(p, Lesson.CONDUIT);
    }

    private static void toll(Gate g) { Effects.withSource(g.player, () -> tollWithin(g)); }
    private static void tollWithin(Gate g) {
        AABB box = new AABB(g.centre, g.centre).inflate(LessonPackRules.GATE_HALF_LENGTH + 1.5, 2.5, LessonPackRules.GATE_HALF_LENGTH + 1.5);
        Vec3 normal = new Vec3(-g.az, 0, g.ax);
        BlockPos home = BlockPos.containing(g.centre);
        for (LivingEntity e : g.level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != g.player && (e instanceof Enemy || e instanceof Player) && Targets.canHarm(g.player, e))) {
            if (g.tolled.contains(e.getUUID())) continue;
            double dx = e.getX() - g.centre.x, dz = e.getZ() - g.centre.z;
            double along = LessonPackRules.along(dx, dz, g.ax, g.az), across = LessonPackRules.across(dx, dz, g.ax, g.az);
            Double previous = g.lastAcross.put(e.getUUID(), across);
            if (!LessonPackRules.atGate(along, across, e.getY() - g.centre.y, e.getBbWidth() * .5)) continue;
            if (dev.wildercord.party.Parties.blocksCurrentHarm(e) || !Casters.mayEdit(g.player, g.level, e.blockPosition())
                || !sameWard(g.level, e.blockPosition(), home)) continue;
            int side = LessonPackRules.side(previous == null ? across : previous, across, g.casterAcross);
            g.tolled.add(e.getUUID()); g.lastAcross.remove(e.getUUID());
            if (!VoidTime.anchored(e)) e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            Effects.push(e, normal.scale(side * LessonPackRules.GATE_PUSH).add(0, .15, 0));
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, LessonPackRules.GATE_SLOW_TICKS, 1, false, true), g.player);
            Fx.sound(g.level, e.position(), SoundEvents.BELL_BLOCK, .8F, 1.2F + .15F * (LessonPackRules.GATE_TOLLS - g.tolls));
            Fx.particle(g.level, ParticleTypes.WAX_OFF, e.position().add(0, e.getBbHeight() * .5, 0), 10, .3, .05);
            if (e instanceof Enemy && !g.practiced) { g.practiced = true; MasterStudies.completePractice(g.player, Lesson.TOLLGATE); }
            if (--g.tolls <= 0) { retire(g); return; }
        }
    }
    private static void draw(Placed placed, long tick) {
        if (tick % 4 != 0) return;
        int color = Vfx.theme(placed.lesson.rune).primary();
        switch (placed) {
            case Gate g -> {
                Vec3 side = new Vec3(g.ax, 0, g.az).scale(LessonPackRules.GATE_HALF_LENGTH), a = g.centre.subtract(side), b = g.centre.add(side);
                Vec3 up = new Vec3(0, LessonPackRules.GATE_HEIGHT, 0);
                Light.ray(g.level, a, a.add(up), color, .14, 6); Light.ray(g.level, b, b.add(up), color, .14, 6);
                Light.ray(g.level, a.add(0, .08, 0), b.add(0, .08, 0), color, .07, 6);
                for (int i = 0; i < g.tolls; i++) Light.orb(g.level, a.add(up).add(0, .25 + i * .3, 0), color, .08, 6);
            }
            case Line l -> Light.ray(l.level, l.player.position().add(0, 1.1, 0), l.ally.position().add(0, l.ally.getBbHeight() * .6, 0), color, .03, 6);
            case Rod r -> {
                Light.ray(r.level, r.top, r.top.add(0, 2.2, 0), color, r.spark >= 0 ? .14 : .07, 6);
                if (r.spark >= 0) Fx.particle(r.player.level(), ParticleTypes.ELECTRIC_SPARK, r.player.position().add(0, 1, 0), 4, .4, .05);
            }
            default -> {}
        }
    }

    private static void publish(Placed placed) {
        Vec3 at = switch (placed) { case Gate g -> g.centre; case Line l -> l.ally.position(); case Rod r -> r.top; default -> Vec3.ZERO; };
        long until = placed instanceof Rod r && r.spark >= 0 ? r.spark + LessonPackRules.SPARK_TICKS : placed.time.expires();
        placed.player.setAttached(LessonPackState.VIEW, new LessonPackState(placed.lesson.ordinal(), placed.slot, until, at, now(placed.player),
            placed.player.level().getGameTime()));
    }
    private static void retire(Placed placed) {
        if (placed.retired) return;
        placed.retired = true;
        boolean removed = switch (placed) {
            case Gate g -> GATES.remove(g.player, g); case Line l -> LINES.remove(l.player, l); case Rod r -> RODS.remove(r.player, r); default -> false;
        };
        if (placed instanceof Line l && removed && l.level == l.player.level()) Fx.sound(l.level, l.player.position(), SoundEvents.LEAD_BREAK, .5F, 1.2F);
        var view = placed.player.getAttached(LessonPackState.VIEW);
        if (view != null && view.lesson() == placed.lesson.ordinal()) placed.player.removeAttached(LessonPackState.VIEW);
    }
    /** Sneak-press, or a fresh payment, lifts the lesson's standing object. Rest already paid is never refunded. */
    private static void lift(ServerPlayer p, Lesson lesson, boolean told) {
        Placed placed = switch (lesson) { case TOLLGATE -> GATES.get(p); case LIFELINE -> LINES.get(p); case CONDUIT -> RODS.get(p); };
        if (placed == null) return;
        retire(placed);
        if (told) fail(p, lesson.name + " lifted.");
    }
    /** Damage, a cast lock or a changed book breaks a pending Conduit spark; the rod stays. */
    public static void cancelSpark(ServerPlayer p) {
        Rod rod = RODS.get(p);
        if (rod != null && rod.spark >= 0) { rod.spark = -1; publish(rod); Fx.sound(rod.level, p.position(), SoundEvents.BEACON_DEACTIVATE, .5F, 1.9F); }
    }
    /** Lifecycle clears every body-bound object; rest deadlines are never renewed by this cleanup. */
    public static void retire(ServerPlayer p) {
        for (Placed placed : new Placed[] {GATES.get(p), LINES.get(p), RODS.get(p)}) if (placed != null) retire(placed);
        INPUT.remove(p); LAST_DOWN.remove(p); p.removeAttached(LessonPackState.VIEW);
    }
    /** Lifeline threads to this body break when it leaves. */
    private static void releaseAlly(LivingEntity body) {
        for (Line line : List.copyOf(LINES.values())) if (line.ally == body) retire(line);
    }
    public static boolean standing(ServerPlayer p, Lesson lesson) {
        return switch (lesson) { case TOLLGATE -> GATES.containsKey(p); case LIFELINE -> LINES.containsKey(p); case CONDUIT -> RODS.containsKey(p); };
    }

    public static void init() {
        dev.wildercord.net.LessonPackInput.init();
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (dev.wildercord.aura.MastersArtRules.interrupts(base, taken, blocked) && entity instanceof ServerPlayer p) cancelSpark(p);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer p) retire(p); releaseAlly(entity); });
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p, from, to) -> { retire(p); releaseAlly(p); });
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register(dev.wildercord.aura.Aura.AFTER_COPY, (old, fresh, alive) -> {
            retire(old); releaseAlly(old); fresh.removeAttached(LessonPackState.VIEW); INPUT.remove(fresh);
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handler.player.removeAttached(LessonPackState.VIEW); INPUT.remove(handler.player);
        });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { retire(handler.player); releaseAlly(handler.player); });
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            List<Placed> all = new java.util.ArrayList<>(GATES.values()); all.addAll(LINES.values()); all.addAll(RODS.values());
            for (Placed placed : all) {
                if (placed.level.getServer() != server || placed.retired) continue;
                if (!placed.valid()) { retire(placed); continue; }
                long tick = now(placed.player);
                if (placed instanceof Gate g) toll(g);
                if (placed instanceof Line l && l.ally instanceof Player friend && friend.isShiftKeyDown()
                    && LessonPackRules.consentOpen(l.time.created(), tick)) { refuse(l); continue; }
                if (placed instanceof Rod r && r.spark >= 0) {
                    if (!available(r.player)) cancelSpark(r.player);
                    else if (LessonPackRules.sparked(r.spark, tick)) {
                        try (var admission = ActionAdmission.begin(r.player)) {
                            if (admission == null || !admission.valid()) cancelSpark(r.player); else arrive(r);
                        }
                    }
                }
                if (!placed.retired) draw(placed, tick);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) retire(p);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { GATES.clear(); LINES.clear(); RODS.clear(); INPUT.clear(); LAST_DOWN.clear(); REFUSED.clear(); ADMITTING.clear(); });
    }
}
