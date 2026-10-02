package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.content.SigilOption;
import dev.wildercord.net.PacketThrottle;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Masters and disciples (the rules and numbers are {@link LineageRules}; the record of who is whose is {@link LineageRegistry}).
 *
 * <p><b>Taking a disciple.</b> The rites of the sword are held in the breathing stance with the other kneeling before you, as a blade is passed:
 * a swordsman of Form or above breathes, and one two stages or more below them kneels before them (sneaking close, the two facing each other).
 * Both are told who asks whom, and either may stand to refuse; held eight seconds (the asking, the binding: their two colours braiding between
 * them, then the seal), the disciple is theirs. One not yet breathing learns the master's method there and then; one breathing another way is
 * given the master's manual, to read if they choose.</p>
 *
 * <p><b>Being a disciple.</b> Aura experience comes faster within {@link LineageRules#NEAR} blocks of the master. Kneeling before the master in
 * the stance again is a <b>lesson</b> (once a day): a part of a technique the master knows and the disciple doesn't, or, for a disciple past Edge
 * who walks no Way, the crossroads raised. And besting the master in a spar, brought to one heart, is the master's trial: it makes a waiting
 * breakthrough into a stage below the master's own ({@link #sparred}).</p>
 *
 * <p><b>Being a master.</b> A share of each road a disciple walks, paid as they break through (kept for the master while they're away). Their own
 * bonded blade can be passed to a disciple in the passing ceremony ({@link AuraApi#allowBladePassing}).</p>
 *
 * <p><b>The end.</b> Either may end the bond from the Aura page's Lineage tab (asked twice), at no cost; it ends with honour when a disciple
 * reaches their master's stage, and the master's record keeps them.</p>
 */
public final class Lineage {
	private Lineage() {}

	// ------------------------------------------------------------------ what each screen knows

	/**
	 * One person on a swordsman's Lineage tab: who, their stage and colour (as last seen), when the bond was made (game time), whether they're
	 * here now, and (a master, for their disciple) near enough to quicken their learning.
	 */
	public record Entry(UUID id, String name, int stage, int color, long since, boolean online, boolean near) {
		public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Entry::id, ByteBufCodecs.stringUtf8(64),
			Entry::name, ByteBufCodecs.VAR_INT, Entry::stage, ByteBufCodecs.INT, Entry::color, ByteBufCodecs.VAR_LONG, Entry::since, ByteBufCodecs.BOOL,
			Entry::online, ByteBufCodecs.BOOL, Entry::near, Entry::new);
	}

	/**
	 * A swordsman's lineage as their own Aura page shows it: whether masters and disciples work here, their master (or none), their disciples,
	 * the disciples they've seen through to their own stage, how many they may keep, and when their next lesson may come (game time).
	 */
	public record View(boolean on, Optional<Entry> master, List<Entry> disciples, List<Entry> honoured, int most, long lessonAt) {
		public static final View NONE = new View(false, Optional.empty(), List.of(), List.of(), LineageRules.MAX_DISCIPLES, Long.MIN_VALUE);
		public static final StreamCodec<ByteBuf, View> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, View::on,
			ByteBufCodecs.optional(Entry.STREAM_CODEC), View::master, Entry.STREAM_CODEC.apply(ByteBufCodecs.list(16)), View::disciples,
			Entry.STREAM_CODEC.apply(ByteBufCodecs.list(16)), View::honoured, ByteBufCodecs.VAR_INT, View::most, ByteBufCodecs.VAR_LONG, View::lessonAt,
			View::new);
	}

	/** {@code player}'s lineage as their page shows it (rebuilt by the server when it changes; never saved: the registry is what's kept). */
	public static final AttachmentType<View> VIEW = AttachmentRegistry.create(
		Wildercord.id("lineage"),
		builder -> builder.syncWith(View.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/**
	 * A rite under way, as everyone near sees it ({@code kind} 1 a disciple taken, 2 a lesson): when it began, how long it runs, the colour it
	 * burns in, the other one's name, and whether this one is the master. Both of them carry it.
	 */
	public record Rite(int kind, long start, int ticks, int color, String other, boolean master) {
		public static final StreamCodec<ByteBuf, Rite> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Rite::kind, ByteBufCodecs.VAR_LONG,
			Rite::start, ByteBufCodecs.VAR_INT, Rite::ticks, ByteBufCodecs.INT, Rite::color, ByteBufCodecs.stringUtf8(64), Rite::other, ByteBufCodecs.BOOL,
			Rite::master, Rite::new);

		public static final int TAKE = 1;
		public static final int LESSON = 2;

		/** How far along it is at {@code now} (0 to 1). */
		public float progress(long now) {
			return ticks <= 0 ? 1 : Math.max(0, Math.min(1, (now - start) / (float) ticks));
		}
	}

	public static final AttachmentType<Rite> RITE = AttachmentRegistry.create(
		Wildercord.id("lineage_rite"),
		builder -> builder.syncWith(Rite.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/** The Lineage tab's ask to end a bond with {@code other} (their master, or one of their disciples). */
	public record End(UUID other) implements CustomPacketPayload {
		public static final Type<End> TYPE = new Type<>(Wildercord.id("lineage_end"));
		public static final StreamCodec<RegistryFriendlyByteBuf, End> CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, End::other, End::new).cast();

		@Override
		public Type<End> type() {
			return TYPE;
		}
	}

	// ------------------------------------------------------------------ reading

	/** Whether masters and disciples work for {@code player}: aura on, and the server's {@code mentorship} (a client: what its page was told). */
	public static boolean on(Player player) {
		if (player == null || !Aura.enabled(player)) {
			return false;
		}
		return player.level().isClientSide() ? view(player).on() : Config.get().aura().sparring().mentorship();
	}

	public static View view(Player player) {
		return player.getAttachedOrElse(VIEW, View.NONE);
	}

	/** {@code player}'s master's id, if they have one. Both sides (a client knows only its own). */
	public static Optional<UUID> masterOf(Player player) {
		if (player.level().isClientSide()) {
			return view(player).master().map(Entry::id);
		}
		return registry(player).flatMap(r -> r.masterOf(player.getUUID())).map(LineageRegistry.Bond::master);
	}

	/** {@code player}'s disciples' ids. Both sides (a client knows only its own). */
	public static List<UUID> disciplesOf(Player player) {
		if (player.level().isClientSide()) {
			return view(player).disciples().stream().map(Entry::id).toList();
		}
		return registry(player).map(r -> r.disciplesOf(player.getUUID()).stream().map(LineageRegistry.Bond::disciple).toList()).orElse(List.of());
	}

	/** Whether {@code disciple} is {@code master}'s disciple (server). */
	public static boolean isDisciple(Player master, Player disciple) {
		return master != null && disciple != null && registry(master).map(r -> r.bonded(master.getUUID(), disciple.getUUID())).orElse(false);
	}

	private static Optional<LineageRegistry> registry(Player player) {
		return player.level().getServer() == null ? Optional.empty() : Optional.of(LineageRegistry.of(player.level().getServer()));
	}

	/** Why {@code master} can't take {@code disciple} now, or null when they can. */
	public static LineageRules.Refusal refusal(ServerPlayer master, ServerPlayer disciple) {
		LineageRegistry r = LineageRegistry.of(master.level().getServer());
		return LineageRules.refusal(on(master), Aura.stage(master), Aura.stage(disciple), r.disciplesOf(master.getUUID()).size(),
			Config.get().aura().sparring().maxDisciples(), r.bonded(master.getUUID(), disciple.getUUID()), r.masterOf(disciple.getUUID()).isPresent());
	}

	// ------------------------------------------------------------------ the rites

	/** A rite under way: which, when it began, and the disciple. */
	private record Under(int kind, long start, UUID disciple) {}

	/** Each master's rite under way. */
	private static final Map<UUID, Under> RITES = new HashMap<>();
	/** The stance (by when it settled) a refusal or a "nothing to teach" was last said in, so it's said once a stance. */
	private static final Map<UUID, Long> TOLD = new HashMap<>();

	/** Whether {@code player} is holding a rite as a master now. */
	public static boolean underWay(ServerPlayer player) {
		return RITES.containsKey(player.getUUID());
	}

	/**
	 * Called every tick {@code master} holds the breathing stance (from {@code Aura}'s stance, after a bonded blade's ceremonies): one kneeling
	 * before them asks to be their disciple, or (already theirs) for a lesson.
	 */
	static void breathing(ServerPlayer master, AuraAttachments.State state, long now) {
		Under rite = RITES.get(master.getUUID());
		if (rite != null) {
			advance(master, rite, now);
			return;
		}
		if (now - state.settledAt() < LineageRules.SETTLE_BEFORE || BladeCeremony.busy(master) || Crossroads.standing(master)) {
			return;
		}
		ServerPlayer kneeling = kneeling(master);
		if (kneeling == null) {
			return;
		}
		LineageRegistry r = LineageRegistry.of(master.level().getServer());
		if (r.bonded(master.getUUID(), kneeling.getUUID())) {
			if (!on(master)) {
				return;
			}
			long last = r.masterOf(kneeling.getUUID()).map(LineageRegistry.Bond::lessonAt).orElse(Long.MIN_VALUE);
			if (LineageRules.lessonReady(last, now)) {
				begin(master, kneeling, LineageRules.LESSON_TICKS, Rite.LESSON, now);
			} else if (tellOnce(master, state)) {
				long left = Math.max(0, last + LineageRules.LESSON_REST - now);
				master.sendOverlayMessage(Component.translatable("message.wildercord.aura.lineage.lesson_later", kneeling.getDisplayName(),
					(left + 1199) / 1200).withColor(0xA89CC8));
			}
			return;
		}
		LineageRules.Refusal why = refusal(master, kneeling);
		if (why == null) {
			begin(master, kneeling, LineageRules.CEREMONY_TICKS, Rite.TAKE, now);
		} else if (why != LineageRules.Refusal.OFF && tellOnce(master, state)) {
			Component line = Component.translatable(why.key(), kneeling.getDisplayName(), master.getDisplayName()).withColor(0xA89CC8);
			master.sendOverlayMessage(line);
			kneeling.sendOverlayMessage(line);
		}
	}

	private static boolean tellOnce(ServerPlayer master, AuraAttachments.State state) {
		Long told = TOLD.get(master.getUUID());
		if (told != null && told == state.settledAt()) {
			return false;
		}
		TOLD.put(master.getUUID(), state.settledAt());
		return true;
	}

	/** The one kneeling nearest before {@code master}, or null. */
	private static ServerPlayer kneeling(ServerPlayer master) {
		ServerPlayer best = null;
		double nearest = Double.MAX_VALUE;
		for (ServerPlayer other : master.level().getEntitiesOfClass(ServerPlayer.class, master.getBoundingBox().inflate(LineageRules.REACH))) {
			if (other == master || !BladeCeremony.kneelsBefore(other, master)) {
				continue;
			}
			double d = other.distanceToSqr(master);
			if (d < nearest) {
				nearest = d;
				best = other;
			}
		}
		return best;
	}

	private static void begin(ServerPlayer master, ServerPlayer disciple, int ticks, int kind, long now) {
		RITES.put(master.getUUID(), new Under(kind, now, disciple.getUUID()));
		int mc = Spars.colour(master);
		master.setAttached(RITE, new Rite(kind, now, ticks, mc, disciple.getGameProfile().name(), true));
		disciple.setAttached(RITE, new Rite(kind, now, ticks, mc, master.getGameProfile().name(), false));
		ServerLevel level = master.level();
		if (kind == Rite.TAKE) {
			Feels.sound(level, master.position().add(0, 1, 0), "aura_lineage_ask", 0.9F, 1.0F);
			master.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.asks_master", disciple.getDisplayName()).withColor(0xE8D8B0));
			disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.asks_disciple", master.getDisplayName()).withColor(0xE8D8B0));
		} else {
			Feels.sound(level, master.position().add(0, 1, 0), "aura_lineage_lesson", 0.8F, 0.9F);
			master.sendOverlayMessage(Component.translatable("message.wildercord.aura.lineage.lesson_begins", disciple.getDisplayName()).withColor(0xFF000000 | mc));
			disciple.sendOverlayMessage(Component.translatable("message.wildercord.aura.lineage.lesson_begins_disciple", master.getDisplayName())
				.withColor(0xFF000000 | mc));
		}
	}

	private static void end(ServerPlayer master, Under rite) {
		RITES.remove(master.getUUID());
		master.removeAttached(RITE);
		ServerPlayer disciple = find(master, rite.disciple());
		if (disciple != null) {
			disciple.removeAttached(RITE);
		}
	}

	private static void cancel(ServerPlayer master, Under rite) {
		ServerPlayer disciple = find(master, rite.disciple());
		end(master, rite);
		Feels.sound(master.level(), master.position().add(0, 1, 0), "aura_bond_fail", 0.6F, 1.1F);
		Component line = Component.translatable("message.wildercord.aura.lineage.broken").withColor(0xC8A0A0);
		master.sendOverlayMessage(line);
		if (disciple != null) {
			disciple.sendOverlayMessage(line);
		}
	}

	/** The master's stance broke: so does any rite under way. */
	static void broken(ServerPlayer master) {
		Under rite = RITES.get(master.getUUID());
		if (rite != null) {
			cancel(master, rite);
		}
	}

	private static ServerPlayer find(ServerPlayer near, UUID id) {
		if (id == null) {
			return null;
		}
		ServerPlayer online = near.level().getServer().getPlayerList().getPlayer(id);
		if (online != null) {
			return online;
		}
		return near.level().getPlayerByUUID(id) instanceof ServerPlayer there && !there.isRemoved() ? there : null;
	}

	private static void advance(ServerPlayer master, Under rite, long now) {
		int t = (int) (now - rite.start());
		ServerPlayer disciple = find(master, rite.disciple());
		if (disciple == null || !BladeCeremony.kneelsBefore(disciple, master)) {
			cancel(master, rite);
			return;
		}
		if (rite.kind() == Rite.TAKE) {
			if (refusal(master, disciple) != null) {
				cancel(master, rite);
				return;
			}
			takeLook(master, disciple, t);
			if (t == LineageRules.ASK_END) {
				Feels.sound(master.level(), master.position().add(disciple.position()).scale(0.5).add(0, 1, 0), "aura_lineage_bind", 0.9F, 1.0F);
			}
			if (t >= LineageRules.CEREMONY_TICKS) {
				end(master, rite);
				take(master, disciple, true);
			}
		} else {
			lessonLook(master, disciple, t);
			if (t >= LineageRules.LESSON_TICKS) {
				end(master, rite);
				lesson(master, disciple);
			}
		}
	}

	// ------------------------------------------------------------------ taking a disciple

	/**
	 * Makes {@code disciple} {@code master}'s at once (the end of the ceremony, an add-on's own rite, an operator), every rule but the ceremony:
	 * the seal's moment when {@code ceremony}. Returns whether it was made.
	 */
	public static boolean take(ServerPlayer master, ServerPlayer disciple, boolean ceremony) {
		if (master == disciple || refusal(master, disciple) != null) {
			return false;
		}
		LineageRegistry r = LineageRegistry.of(master.level().getServer());
		long now = master.level().getGameTime();
		r.add(new LineageRegistry.Bond(master.getUUID(), master.getGameProfile().name(), disciple.getUUID(), disciple.getGameProfile().name(), now,
			Long.MIN_VALUE, 0, Aura.stage(master), Aura.stage(disciple), Spars.colour(master), Spars.colour(disciple)));
		Grimoire.unlock(master, "aura:lineage");
		Grimoire.unlock(disciple, "aura:lineage");
		sealed(master, disciple, ceremony);
		// The master's method: taught at once to one not yet breathing; to one breathing another way, the manual to read if they choose.
		Aura.method(master).ifPresent(method -> {
			AuraAttachments.Data data = Aura.data(disciple);
			Component name = Component.translatable(method.nameKey()).withColor(0xFF000000 | method.color());
			if (!data.learned()) {
				AuraMethods.learn(disciple, method, "master", true);
			} else if (!data.method().equals(method.id())) {
				ItemStack manual = AuraApi.manual(method.id());
				if (!disciple.getInventory().add(manual)) {
					disciple.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(disciple.level(), disciple.getX(), disciple.getY() + 0.5,
						disciple.getZ(), manual));
				}
				disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.manual", master.getDisplayName(), name).withColor(0xB8A8D8));
			}
		});
		refresh(master);
		refresh(disciple);
		for (AuraApi.MentorHook hook : AuraApi.mentorHooks()) {
			try {
				hook.bonded(master, disciple);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A mentorship hook threw; skipping it", e);
			}
		}
		return true;
	}

	/** The ceremony as it goes: the asking (a ring under the disciple), the binding (their colours braiding), the sealing (a ring closing in). */
	private static void takeLook(ServerPlayer master, ServerPlayer disciple, int t) {
		ServerLevel level = master.level();
		int mc = Spars.colour(master);
		int dc = Spars.colour(disciple);
		Vec3 from = master.position().add(0, 1.3, 0);
		Vec3 to = disciple.position().add(0, disciple.isCrouching() ? 1.05 : 1.4, 0);
		Vec3 middle = master.position().add(disciple.position()).scale(0.5);
		int phase = LineageRules.phase(t);
		ArtLight world = ArtLight.world(master);
		ArtLight show = ArtLight.spectacle(master);
		if (t % 20 == 0) {
			AuraFx.bodyAuraFlare(master, 22, 0.45F);
			AuraFx.bodyAuraFlare(disciple, 22, 0.25F + 0.004F * t);
		}
		if (phase == 0) {
			if (t % 10 == 0) {
				world.groundRing(disciple.position(), mc, 0.4, 1.3, 0.05, 12);
			}
			if (t % 4 == 0) {
				Motes.glows(level, disciple.position().add(0, 0.6, 0), 2, 0.3, dc, 0.08, 16, new Vec3(0, 0.03, 0), 0.01);
			}
			return;
		}
		if (t % 3 == 0) {
			Motes.seek(level, from, to, mc, 0.1, 18, 0.5);
		}
		if (t % 5 == 0) {
			Motes.seek(level, to, from, dc, 0.08, 18, -0.5);
		}
		float grown = Math.min(1, (t - LineageRules.ASK_END) / (float) (LineageRules.BIND_END - LineageRules.ASK_END));
		if (t % 10 == 0) {
			show.ground(middle, SigilOption.RING, AuraRules.mix(mc, dc, 0.35), 2.2 + 1.2 * grown, 12, 0.25);
			world.groundRing(middle, mc, 1.8 + 1.4 * grown, 1.8 + 1.4 * grown, 0.06, 12);
		}
		if (phase == 2) {
			if (t % 8 == 0) {
				show.groundRing(disciple.position(), AuraVfx.hot(mc, 0.4), 2.6, 0.5, 0.08, 9);
				AuraFx.burst(level, disciple, to, Vec3.ZERO, mc, 0.35F + 0.01F * (t - LineageRules.BIND_END), AuraFx.Burst.FLASH);
			}
			if (t % 4 == 0) {
				show.ray(from, to, mc, 0.05, 5).ray(from, to, AuraVfx.hot(mc, 0.55), 0.016, 5);
			}
		}
	}

	/** The seal: a burst at the disciple's brow, a column of the master's light round them, banners on both screens, and what it means said. */
	private static void sealed(ServerPlayer master, ServerPlayer disciple, boolean ceremony) {
		ServerLevel level = master.level();
		int mc = Spars.colour(master);
		Vec3 brow = disciple.position().add(0, disciple.isCrouching() ? 1.15 : 1.55, 0);
		Vec3 feet = disciple.position();
		Feels.sound(level, brow, "aura_lineage_seal", 1.0F, 1.0F);
		if (ceremony) {
			AuraFx.burst(level, disciple, brow, Vec3.ZERO, mc, 1.3F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.RING);
			ArtLight.spectacle(disciple).ray(feet.add(0, 0.1, 0), feet.add(0, 5.5, 0), mc, 0.24, 18).groundRing(feet, AuraVfx.hot(mc, 0.4), 0.6, 4.5, 0.12, 14);
			ArtLight.world(disciple).ray(feet.add(0, 2.6, 0), feet.add(0, 7.5, 0), mc, 0.18, 16);
			Motes.burst(level, brow, 14, mc, 0.12, 26, 0.1);
			AuraFx.bodyAuraFlare(master, 40, 0.8F);
			AuraFx.bodyAuraFlare(disciple, 50, 1.0F);
		}
		AuraFx.banner(disciple, Component.translatable("aura.wildercord.lineage.disciple_of"), Component.literal(master.getGameProfile().name()), mc,
			AuraFxRules.BannerKind.GRAND);
		AuraFx.banner(master, Component.translatable("aura.wildercord.lineage.a_disciple"), Component.literal(disciple.getGameProfile().name()),
			mc, AuraFxRules.BannerKind.ART);
		master.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.taken_master", disciple.getDisplayName()).withColor(0xE8D8B0));
		master.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.taken_master_how").withColor(0xB8A8D8));
		disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.taken_disciple", master.getDisplayName()).withColor(0xE8D8B0));
		disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.taken_disciple_how").withColor(0xB8A8D8));
	}

	// ------------------------------------------------------------------ a lesson

	private static void lessonLook(ServerPlayer master, ServerPlayer disciple, int t) {
		ServerLevel level = master.level();
		int mc = Spars.colour(master);
		Vec3 blade = BladeCeremony.hand(master);
		Vec3 to = disciple.position().add(0, disciple.isCrouching() ? 1.05 : 1.4, 0);
		if (t % 3 == 0) {
			Motes.seek(level, blade, to, mc, 0.1, 16, 0.4);
		}
		if (t % 10 == 0) {
			ArtLight.world(master).groundRing(master.position().add(disciple.position()).scale(0.5), mc, 1.6, 1.6, 0.05, 12);
		}
	}

	/** A lesson's end: a part of a technique the master knows for good and the disciple doesn't, or the crossroads raised, or nothing new. */
	private static void lesson(ServerPlayer master, ServerPlayer disciple) {
		LineageRegistry r = LineageRegistry.of(master.level().getServer());
		r.masterOf(disciple.getUUID()).ifPresent(b -> r.put(b.lessoned(master.level().getGameTime())));
		int mc = Spars.colour(master);
		Feels.sound(master.level(), disciple.position().add(0, 1, 0), "aura_lineage_lesson", 1.0F, 1.15F);
		AuraFx.burst(master.level(), disciple, disciple.position().add(0, 1.2, 0), Vec3.ZERO, mc, 0.7F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR);
		List<String> teachable = new ArrayList<>();
		if (Techniques.on(master) && Techniques.on(disciple)) {
			for (String part : TechniqueRules.allParts()) {
				if (TechniqueRules.scrollable(part) && Techniques.learned(master, part) && !Techniques.learned(disciple, part)) {
					teachable.add(part);
				}
			}
		}
		if (!teachable.isEmpty()) {
			String part = teachable.get(disciple.getRandom().nextInt(teachable.size()));
			disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.lesson_part", master.getDisplayName(),
				Component.translatable(TechniqueRules.nameKey(part))).withColor(0xE8D8B0));
			Techniques.teach(disciple, part, "master");
			master.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.lesson_given", disciple.getDisplayName(),
				Component.translatable(TechniqueRules.nameKey(part))).withColor(0xB8A8D8));
		} else if (Ways.on(disciple) && Aura.stage(disciple) >= WayRules.FROM && Ways.wayless(disciple)) {
			disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.lesson_way", master.getDisplayName()).withColor(0xE8D8B0));
			Crossroads.open(disciple, Crossroads.Reason.API);
		} else {
			disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.lesson_nothing", master.getDisplayName()).withColor(0xB8A8D8));
			master.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.lesson_nothing_master", disciple.getDisplayName())
				.withColor(0xB8A8D8));
		}
		refresh(disciple);
	}

	// ------------------------------------------------------------------ what each gains

	/** A disciple's aura experience as it's earned: faster within {@link LineageRules#NEAR} of their master (from {@code AuraExperience}). */
	static double near(ServerPlayer player, double xp) {
		if (xp <= 0 || !on(player)) {
			return xp;
		}
		return LineageRules.near(xp, masterNear(player), Config.get().aura().sparring().discipleGain());
	}

	/** Whether {@code player}'s master is within {@link LineageRules#NEAR} of them, in the same world. */
	public static boolean masterNear(ServerPlayer player) {
		LineageRegistry r = LineageRegistry.of(player.level().getServer());
		Optional<LineageRegistry.Bond> bond = r.masterOf(player.getUUID());
		if (bond.isEmpty()) {
			return false;
		}
		ServerPlayer master = find(player, bond.get().master());
		return master != null && master.level() == player.level() && master.isAlive()
			&& master.distanceToSqr(player) <= LineageRules.NEAR * LineageRules.NEAR;
	}

	/**
	 * {@code player} broke through into {@code stage} (from {@code AuraBreakthroughs}): their master earns a share of the road they walked (kept
	 * for them if they're away), and a disciple who has reached their master's stage graduates.
	 */
	static void brokeThrough(ServerPlayer player, int stage) {
		LineageRegistry r = LineageRegistry.of(player.level().getServer());
		Optional<LineageRegistry.Bond> found = r.masterOf(player.getUUID());
		if (found.isEmpty()) {
			return;
		}
		LineageRegistry.Bond bond = found.get();
		ServerPlayer master = find(player, bond.master());
		double share = on(player) ? LineageRules.share(stage, Config.get().aura().sparring().masterShare()) : 0;
		if (share > 0) {
			if (master != null && master.connection != null && master.level().getServer().getPlayerList().getPlayer(master.getUUID()) != null) {
				paid(master, share, player.getGameProfile().name(), stage);
			} else {
				r.put(bond.owing(share));
			}
			for (AuraApi.MentorHook hook : AuraApi.mentorHooks()) {
				try {
					hook.shared(bond.master(), player, share);
				} catch (RuntimeException e) {
					Wildercord.LOGGER.warn("A mentorship hook threw; skipping it", e);
				}
			}
		}
		int masterStage = master != null ? Aura.stage(master) : bond.masterStage();
		if (LineageRules.graduates(stage, masterStage)) {
			graduate(player, master, bond);
		} else {
			refresh(player);
			if (master != null) {
				refresh(master);
			}
		}
	}

	/** A master paid a share: experience, a soft glitter, and a word of whose road it was. */
	private static void paid(ServerPlayer master, double share, String discipleName, int stage) {
		double got = AuraExperience.earn(master, share, false);
		Feels.sound(master.level(), master.position().add(0, 1, 0), "aura_lineage_share", 0.8F, 1.0F);
		Motes.glows(master.level(), master.position().add(0, 1.2, 0), 8, 0.4, Spars.colour(master), 0.08, 26, new Vec3(0, 0.02, 0), 0.01);
		Component stageName = Component.translatable("aura.wildercord.stage." + AuraStages.id(stage));
		master.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.share", Component.literal(discipleName), stageName,
			String.format(java.util.Locale.ROOT, "%.0f", got)).withColor(0xE8D8B0));
	}

	/** A disciple who has reached their master's stage: the bond ends with honour, and the master's record keeps them. */
	private static void graduate(ServerPlayer disciple, ServerPlayer master, LineageRegistry.Bond bond) {
		LineageRegistry r = LineageRegistry.of(disciple.level().getServer());
		r.end(disciple.getUUID());
		r.honour(bond.master(), new LineageRegistry.Honoured(disciple.getUUID(), disciple.getGameProfile().name(), bond.since(),
			disciple.level().getGameTime(), Spars.colour(disciple)));
		int dc = Spars.colour(disciple);
		Feels.sound(disciple.level(), disciple.position().add(0, 1, 0), "aura_lineage_graduate", 1.0F, 1.0F);
		AuraFx.banner(disciple, Component.translatable("aura.wildercord.lineage.graduated"), Component.literal(bond.masterName()), dc,
			AuraFxRules.BannerKind.GRAND);
		disciple.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.graduated_disciple", Component.literal(bond.masterName()))
			.withColor(0xFFE8C46A));
		if (master != null) {
			Feels.sound(master.level(), master.position().add(0, 1, 0), "aura_lineage_graduate", 0.8F, 1.1F);
			AuraFx.banner(master, Component.translatable("aura.wildercord.lineage.graduated_master"), Component.literal(disciple.getGameProfile().name()),
				Spars.colour(master), AuraFxRules.BannerKind.ART);
			master.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.graduated_master_line", disciple.getDisplayName())
				.withColor(0xFFE8C46A));
			refresh(master);
		}
		refresh(disciple);
		ended(bond, "graduated");
	}

	/**
	 * A spar ended with {@code winner} bringing {@code loser} down (one heart when {@code knockout}; out of the ring otherwise): if the winner is
	 * the loser's disciple, their master is bested, and the master's trial may make a waiting breakthrough (from {@code Spars}).
	 */
	static void sparred(ServerPlayer winner, ServerPlayer loser, boolean knockout) {
		if (winner == null || loser == null || !on(winner) || !isDisciple(loser, winner)) {
			return;
		}
		for (AuraApi.MentorHook hook : AuraApi.mentorHooks()) {
			try {
				hook.bested(loser, winner);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A mentorship hook threw; skipping it", e);
			}
		}
		loser.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.bested_master", winner.getDisplayName()).withColor(0xE8D8B0));
		if (!knockout) {
			winner.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.bested_out").withColor(0xB8A8D8));
			return;
		}
		winner.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.bested", loser.getDisplayName()).withColor(0xFFE8C46A));
		if (!AuraBreakthroughs.ready(winner)) {
			return;
		}
		int next = Aura.stage(winner) + 1;
		if (!LineageRules.trialCounts(Aura.stage(loser), next)) {
			winner.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.trial_too_high", loser.getDisplayName()).withColor(0xB8A8D8));
			return;
		}
		AuraApi.completeTrial(winner, LineageRules.TRIAL);
	}

	// ------------------------------------------------------------------ the end of it

	/**
	 * Ends the bond between {@code player} and {@code other} (their master, or one of their disciples), at no cost, both told. Returns whether
	 * there was one.
	 */
	public static boolean end(ServerPlayer player, UUID other) {
		LineageRegistry r = LineageRegistry.of(player.level().getServer());
		Optional<LineageRegistry.Bond> bond = r.bonded(player.getUUID(), other) ? r.masterOf(other)
			: r.bonded(other, player.getUUID()) ? r.masterOf(player.getUUID()) : Optional.empty();
		if (bond.isEmpty()) {
			return false;
		}
		r.end(bond.get().disciple());
		ServerPlayer them = find(player, other);
		Component otherName = them != null ? them.getDisplayName() : Component.literal(bond.get().master().equals(other) ? bond.get().masterName()
			: bond.get().discipleName());
		Feels.sound(player.level(), player.position().add(0, 1, 0), "aura_lineage_release", 0.8F, 1.0F);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.ended", otherName).withColor(0xC8B89A));
		if (them != null) {
			them.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.ended_by", player.getDisplayName()).withColor(0xC8B89A));
			refresh(them);
		}
		refresh(player);
		ended(bond.get(), "released");
		return true;
	}

	private static void ended(LineageRegistry.Bond bond, String how) {
		for (AuraApi.MentorHook hook : AuraApi.mentorHooks()) {
			try {
				hook.ended(bond.master(), bond.disciple(), how);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.warn("A mentorship hook threw; skipping it", e);
			}
		}
	}

	// ------------------------------------------------------------------ the view

	/** Rebuilds {@code player}'s view of their lineage (only sent when it changed), and notes their name, stage and colour on their bonds. */
	public static void refresh(ServerPlayer player) {
		if (player.level().getServer() == null) {
			return;
		}
		LineageRegistry r = LineageRegistry.of(player.level().getServer());
		long now = player.level().getGameTime();
		String name = player.getGameProfile().name();
		int stage = Aura.stage(player);
		int color = Spars.colour(player);
		Optional<LineageRegistry.Bond> mine = r.masterOf(player.getUUID());
		mine.ifPresent(b -> {
			if (!b.discipleName().equals(name) || b.discipleStage() != stage || b.discipleColor() != color) {
				r.put(b.seenDisciple(name, stage, color));
			}
		});
		for (LineageRegistry.Bond b : r.disciplesOf(player.getUUID())) {
			if (!b.masterName().equals(name) || b.masterStage() != stage || b.masterColor() != color) {
				r.put(b.seenMaster(name, stage, color));
			}
		}
		Optional<Entry> master = r.masterOf(player.getUUID()).map(b -> {
			ServerPlayer m = find(player, b.master());
			boolean near = m != null && m.level() == player.level() && m.distanceToSqr(player) <= LineageRules.NEAR * LineageRules.NEAR;
			return new Entry(b.master(), m != null ? m.getGameProfile().name() : b.masterName(), m != null ? Aura.stage(m) : b.masterStage(),
				m != null ? Spars.colour(m) : b.masterColor(), b.since(), m != null, near);
		});
		List<Entry> disciples = new ArrayList<>();
		for (LineageRegistry.Bond b : r.disciplesOf(player.getUUID())) {
			ServerPlayer d = find(player, b.disciple());
			boolean near = d != null && d.level() == player.level() && d.distanceToSqr(player) <= LineageRules.NEAR * LineageRules.NEAR;
			disciples.add(new Entry(b.disciple(), d != null ? d.getGameProfile().name() : b.discipleName(), d != null ? Aura.stage(d) : b.discipleStage(),
				d != null ? Spars.colour(d) : b.discipleColor(), b.since(), d != null, near));
		}
		List<Entry> honoured = new ArrayList<>();
		for (LineageRegistry.Honoured h : r.honoured(player.getUUID())) {
			honoured.add(new Entry(h.disciple(), h.name(), 0, h.color(), h.graduated(), false, false));
		}
		long lessonAt = r.masterOf(player.getUUID()).map(b -> b.lessonAt() == Long.MIN_VALUE ? Long.MIN_VALUE : b.lessonAt() + LineageRules.LESSON_REST)
			.orElse(Long.MIN_VALUE);
		View view = new View(on(player), master, List.copyOf(disciples), List.copyOf(honoured), Config.get().aura().sparring().maxDisciples(), lessonAt);
		if (!view.equals(player.getAttached(VIEW))) {
			player.setAttached(VIEW, view);
		}
	}

	/** On joining: their view, and anything owed to them as a master paid now. */
	private static void joined(ServerPlayer player) {
		LineageRegistry r = LineageRegistry.of(player.level().getServer());
		double owed = r.collect(player.getUUID());
		if (owed > 0) {
			double got = AuraExperience.earn(player, owed, false);
			player.sendSystemMessage(Component.translatable("message.wildercord.aura.lineage.share_waiting", String.format(java.util.Locale.ROOT, "%.0f", got))
				.withColor(0xE8D8B0));
		}
		refresh(player);
		// Those bound to them see them here again.
		for (UUID id : disciplesOf(player)) {
			ServerPlayer d = find(player, id);
			if (d != null) {
				refresh(d);
			}
		}
		masterOf(player).map(id -> find(player, id)).ifPresent(Lineage::refresh);
	}

	private static void tick(MinecraftServer server) {
		if (!RITES.isEmpty()) {
			for (Map.Entry<UUID, Under> e : Map.copyOf(RITES).entrySet()) {
				ServerPlayer master = server.getPlayerList().getPlayer(e.getKey());
				if (master == null || !master.isAlive()) {
					RITES.remove(e.getKey());
					continue;
				}
				// The stance keeps a rite going; one that went a tick without it ends.
				if (!Aura.state(master).breathing()) {
					cancel(master, e.getValue());
				}
			}
		}
		// Every two seconds each swordsman's view follows who's near and who's here.
		if (server.getTickCount() % 40 == 0) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (Aura.stage(player) > AuraRules.NONE || player.hasAttached(VIEW)) {
					refresh(player);
				}
			}
		}
	}

	private static final PacketThrottle ENDS = new PacketThrottle(2, 10);

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(End.TYPE, End.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(End.TYPE, (payload, context) -> {
			if (ENDS.allow(context.player().getUUID(), context.server().getTickCount())) {
				end(context.player(), payload.other());
			}
		});
		// Every stage a disciple may reach by besting their master (it's checked against the master's stage when it's met).
		for (int s = AuraRules.FLOW; s <= AuraRules.SOVEREIGN; s++) {
			AuraApi.allowTrial(s, LineageRules.TRIAL);
		}
		// Step 9's passing: a master's bonded blade may pass to one of their disciples.
		AuraApi.allowBladePassing(Lineage::isDisciple);
		ServerTickEvents.END_SERVER_TICK.register(Lineage::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> joined(handler.player)));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			Under rite = RITES.remove(id);
			TOLD.remove(id);
			ENDS.forget(id);
			if (rite != null) {
				ServerPlayer d = server.getPlayerList().getPlayer(rite.disciple());
				if (d != null) {
					d.removeAttached(RITE);
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RITES.clear();
			TOLD.clear();
			ENDS.clear();
		});
	}

	/** Every lineage sound, for the tests. */
	public static final List<String> SOUNDS = List.of("aura_lineage_ask", "aura_lineage_bind", "aura_lineage_seal", "aura_lineage_lesson",
		"aura_lineage_release", "aura_lineage_graduate", "aura_lineage_share");
}
