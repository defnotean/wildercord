package dev.wildercord.aura;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /wildercord aura ...}, for operators testing the path: teach a method outright, set the stage, add experience, fill
 * the aura, or make the waiting breakthrough. Each says where the player's aura stands afterwards.
 */
public final class AuraCommand {
	private AuraCommand() {}

	public static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("aura")
			.then(Commands.literal("method").then(Commands.argument("method", StringArgumentType.greedyString())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BreathingMethods.all().stream().map(BreathingMethod::id), builder))
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					BreathingMethod method = BreathingMethods.byId(StringArgumentType.getString(ctx, "method")).orElse(null);
					if (method == null) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.no_method"));
						return 0;
					}
					AuraMethods.learn(player, method, "command", true);
					return report(ctx);
				})))
			.then(Commands.literal("stage").then(Commands.argument("stage", IntegerArgumentType.integer(AuraRules.GLOW, AuraRules.MAX_STAGE))
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					AuraAttachments.Data data = Aura.data(player);
					if (!data.learned()) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.no_method"));
						return 0;
					}
					int stage = Math.min(AuraStages.highest(), IntegerArgumentType.getInteger(ctx, "stage"));
					Aura.set(player, data.withStage(stage).withXp(Math.max(data.xp(), AuraStages.threshold(stage)), data.practice()));
					return report(ctx);
				})))
			.then(Commands.literal("xp").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
				.executes(ctx -> {
					AuraExperience.grant(ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "amount"));
					return report(ctx);
				})))
			.then(Commands.literal("fill").executes(ctx -> {
				ServerPlayer player = ctx.getSource().getPlayerOrException();
				Aura.set(player, Aura.data(player).withAura(Aura.capacity(player)));
				return report(ctx);
			}))
			.then(Commands.literal("breakthrough").executes(ctx -> {
				AuraBreakthroughs.breakThrough(ctx.getSource().getPlayerOrException());
				return report(ctx);
			}))
			// A Way set outright (owing nothing), cleared ("none": as if never chosen), or the crossroads raised round the player.
			.then(Commands.literal("way")
				.then(Commands.literal("crossroads").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					if (!Crossroads.open(player, Crossroads.Reason.API)) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.way.no_crossroads"));
						return 0;
					}
					return report(ctx);
				}))
				.then(Commands.argument("way", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"),
						dev.wildercord.api.AuraApi.ways().stream().map(dev.wildercord.api.AuraApi.Way::id)), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						String id = StringArgumentType.getString(ctx, "way");
						if (!id.equals("none") && dev.wildercord.api.AuraApi.way(id).isEmpty()) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.way.unknown", id));
							return 0;
						}
						Crossroads.close(player, false);
						Ways.set(player, id.equals("none") ? "" : id);
						return report(ctx);
					})))
			// Techniques of one's own: a part learned or forgotten ("all" for every part a scroll carries), a technique's experience set,
			// or everything cleared.
			.then(Commands.literal("technique")
				.then(Commands.literal("learn").then(Commands.argument("part", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
						TechniqueRules.allParts().stream()), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						String part = StringArgumentType.getString(ctx, "part");
						if (part.equals("all")) {
							TechniqueRules.scrollParts().forEach(p -> Techniques.teach(player, p, "command"));
						} else if (TechniqueRules.family(part).isEmpty()) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.technique.unknown", part));
							return 0;
						} else {
							Techniques.teach(player, part, "command");
						}
						return techniques(ctx);
					})))
				.then(Commands.literal("forget").then(Commands.argument("part", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
						TechniqueRules.allParts().stream()), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						String part = StringArgumentType.getString(ctx, "part");
						if (part.equals("all")) {
							TechniqueRules.allParts().forEach(p -> Techniques.forget(player, p));
						} else {
							Techniques.forget(player, part);
						}
						return techniques(ctx);
					})))
				.then(Commands.literal("xp").then(Commands.argument("slot", IntegerArgumentType.integer(1, TechniqueRules.MAX_SLOTS))
					.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						if (!Techniques.setXp(player, IntegerArgumentType.getInteger(ctx, "slot") - 1, DoubleArgumentType.getDouble(ctx, "amount"))) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.technique.empty"));
							return 0;
						}
						return techniques(ctx);
					}))))
				.then(Commands.literal("clear").executes(ctx -> {
					Techniques.clear(ctx.getSource().getPlayerOrException());
					return techniques(ctx);
				})))
			// The bonded blade: what it stands at, the held blade bonded at once (no ceremony), its resonance or tier set, a name or a trait
			// given outright, the bond released, or the blade passed to another player (no disciple needed).
			.then(Commands.literal("blade")
				.executes(AuraCommand::blade)
				.then(Commands.literal("bond").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					if (!BondedBlades.bond(player, net.minecraft.world.InteractionHand.MAIN_HAND, "command")) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.blade.cant_bond"));
						return 0;
					}
					return blade(ctx);
				}))
				.then(Commands.literal("resonance").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(ctx -> {
					if (!BondedBlades.setResonance(ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "amount"))) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.blade.not_carried"));
						return 0;
					}
					return blade(ctx);
				})))
				.then(Commands.literal("tier").then(Commands.argument("tier", IntegerArgumentType.integer(BladeRules.BONDED, BladeRules.MAX_TIER)).executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					int tier = IntegerArgumentType.getInteger(ctx, "tier");
					if (tier >= BladeRules.SOULFORGED) {
						BondedBlades.addHistory(player, java.util.Map.of(BladeRules.BOSSES, BladeRules.SOULFORGED_BOSSES), java.util.Map.of());
					}
					if (!BondedBlades.setResonance(player, BladeRules.threshold(tier))) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.blade.not_carried"));
						return 0;
					}
					return blade(ctx);
				})))
				.then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
					if (!BondedBlades.rename(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"))) {
						return 0;
					}
					return blade(ctx);
				})))
				.then(Commands.literal("trait").then(Commands.argument("trait", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BladeRules.traits().stream().map(BladeRules.Trait::id), builder))
					.executes(ctx -> {
						String trait = StringArgumentType.getString(ctx, "trait");
						if (!BondedBlades.forceTrait(ctx.getSource().getPlayerOrException(), trait)) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.blade.unknown_trait", trait));
							return 0;
						}
						return blade(ctx);
					})))
				.then(Commands.literal("release").executes(ctx -> {
					if (!BondedBlades.release(ctx.getSource().getPlayerOrException())) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.blade.none"));
						return 0;
					}
					return 1;
				}))
				.then(Commands.literal("pass").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(ctx -> {
					ServerPlayer from = ctx.getSource().getPlayerOrException();
					ServerPlayer to = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
					String why = BondedBlades.passRefusal(from, to, false);
					if (why != null || !BondedBlades.pass(from, to, false)) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.blade.cant_pass", Component.translatable(why == null ? "-" : why)));
						return 0;
					}
					return 1;
				}))))
			// Sparring: a spar begun at once with another player (every check but the salute), or the one under way called off; the record.
			.then(Commands.literal("spar")
				.executes(AuraCommand::spar)
				.then(Commands.literal("with").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(ctx -> {
					ServerPlayer a = ctx.getSource().getPlayerOrException();
					ServerPlayer b = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
					SparRules.Refusal why = Spars.refusal(a, b);
					if (why != null || !Spars.start(a, b)) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.spar.cant",
							why == null ? Component.translatable("command.wildercord.aura.spar.not_ready") : Component.translatable(why.key(), b.getDisplayName())));
						return 0;
					}
					return 1;
				})))
				.then(Commands.literal("off").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					if (!Spars.sparring(player)) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.spar.none"));
						return 0;
					}
					Spars.callOff(player);
					return 1;
				})))
			// Masters and disciples: who's whose, a disciple taken at once (no ceremony), a bond ended.
			.then(Commands.literal("lineage")
				.executes(AuraCommand::lineage)
				.then(Commands.literal("take").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(ctx -> {
					ServerPlayer master = ctx.getSource().getPlayerOrException();
					ServerPlayer disciple = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
					LineageRules.Refusal why = Lineage.refusal(master, disciple);
					if (why != null || !Lineage.take(master, disciple, true)) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.lineage.cant",
							Component.translatable(why == null ? "command.wildercord.aura.lineage.self" : why.key(), disciple.getDisplayName(), master.getDisplayName())));
						return 0;
					}
					return lineage(ctx);
				})))
				.then(Commands.literal("end").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					ServerPlayer other = net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player");
					if (!Lineage.end(player, other.getUUID())) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.lineage.no_bond", other.getDisplayName()));
						return 0;
					}
					return lineage(ctx);
				}))));
	}

	/** A player's sparring record, and the spar they're in. */
	private static int spar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		SparRules.Log log = Spars.log(player);
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.spar.record", log.wins(), log.losses(), log.evens()), false);
		java.util.UUID partner = Spars.partner(player);
		if (partner != null) {
			net.minecraft.world.entity.player.Player other = player.level().getPlayerByUUID(partner);
			ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.spar.with", other == null ? Component.literal("?")
				: other.getDisplayName()), false);
		}
		return 1;
	}

	/** Who a player's master is, and their disciples. */
	private static int lineage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		LineageRegistry r = LineageRegistry.of(player.level().getServer());
		Component master = r.masterOf(player.getUUID()).<Component>map(b -> Component.literal(b.masterName())).orElse(Component.literal("-"));
		java.util.List<Component> disciples = new java.util.ArrayList<>();
		for (LineageRegistry.Bond b : r.disciplesOf(player.getUUID())) {
			disciples.add(Component.literal(b.discipleName()));
		}
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.lineage.state", master,
			disciples.isEmpty() ? Component.literal("-") : net.minecraft.network.chat.ComponentUtils.formatList(disciples, Component.literal(", "))), false);
		return 1;
	}

	/** What a player's bonded blade stands at: its name (or the weapon's), its tier, its resonance and its trait. */
	private static int blade(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		net.minecraft.world.item.ItemStack stack = BondedBlades.carried(player);
		BladeBond b = BondedBlades.bond(stack);
		if (b == null) {
			ctx.getSource().sendSuccess(() -> Component.translatable(BondedBlades.state(player).bonded() ? "command.wildercord.aura.blade.not_carried"
				: "command.wildercord.aura.blade.none"), false);
			return BondedBlades.state(player).bonded() ? 1 : 0;
		}
		Component trait = b.growth().trait().isEmpty() ? Component.literal("-") : Component.translatable(BladeRules.trait(b.growth().trait())
			.map(BladeRules.Trait::nameKey).orElse(b.growth().trait()));
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.blade.state", BondedBlades.name(stack, b),
			Component.translatable("aura.wildercord.blade.tier." + BladeRules.tierId(b.tier())), (int) b.growth().resonance(), trait), false);
		return 1;
	}

	/** What a player's techniques stand at: the parts they may write with, then each slot's technique and its rank. */
	private static int techniques(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		java.util.List<Component> parts = new java.util.ArrayList<>();
		for (String part : Techniques.known(player)) {
			parts.add(Component.translatable(TechniqueRules.nameKey(part)));
		}
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.technique.parts",
			net.minecraft.network.chat.ComponentUtils.formatList(parts, Component.literal(", "))), false);
		Techniques.Book book = Techniques.book(player);
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			Techniques.Written w = book.slot(i);
			if (w.empty()) {
				continue;
			}
			int slot = i + 1;
			Techniques.Honing h = book.honing(w.key());
			ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.technique.slot", slot, Component.literal(w.shownName()),
				Component.translatable(Techniques.rankKey(h.rank())), (int) h.xp()), false);
		}
		return 1;
	}

	private static int report(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AuraAttachments.Data data = Aura.data(player);
		Component method = Aura.method(player).<Component>map(m -> Component.translatable(m.nameKey())).orElse(Component.literal("-"));
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.state", method,
			Component.translatable("aura.wildercord.stage." + AuraStages.id(Aura.stage(player))), (int) data.xp(), (int) Aura.aura(player),
			Aura.capacity(player)), false);
		Ways.way(player).ifPresent(way -> ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.way.state",
			Component.translatable(way.nameKey()).withColor(0xFF000000 | way.color())), false));
		return 1;
	}
}
