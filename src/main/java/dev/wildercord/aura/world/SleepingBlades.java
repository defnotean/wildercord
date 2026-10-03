package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.BondedBlades;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** The sleeping blade, claimed by intent and kept by the existing authoritative bond registry. */
public final class SleepingBlades {
	private SleepingBlades() {}
	private static final ResourceKey<net.minecraft.world.level.block.Block> STONE_KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("sleeping_blade_stone"));
	public static final SleepingBladeStone STONE = Registry.register(BuiltInRegistries.BLOCK, STONE_KEY,
		new SleepingBladeStone(BlockBehaviour.Properties.of().setId(STONE_KEY).strength(-1, 3600000).sound(SoundType.STONE).noOcclusion().noLootTable()));
	public static final BlockEntityType<SleepingBladeEntity> STONE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		Wildercord.id("sleeping_blade_stone"), FabricBlockEntityTypeBuilder.create(SleepingBladeEntity::new, STONE).build());
	private static final ResourceKey<Item> BLADE_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("oathkeeper"));
	public static final Item BLADE = Registry.register(BuiltInRegistries.ITEM, BLADE_KEY,
		new Item(new Item.Properties().setId(BLADE_KEY).sword(ToolMaterial.DIAMOND, 3, -2.4F).fireResistant().rarity(Rarity.EPIC)) {
			@Override public void appendHoverText(ItemStack stack, TooltipContext c, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
				out.accept(Component.translatable("item.wildercord.oathkeeper.lore").withColor(0xB8BFC6));
				out.accept(Component.translatable("item.wildercord.oathkeeper.use").withColor(0xD9BD7E));
			}
		});
	private record Draw(ServerLevel level, BlockPos at, Vec3 start, int elapsed) {}
	private static final Map<UUID, Draw> DRAWS = new HashMap<>();
	/** Bounded active work, exposed for runtime diagnostics. Dormant sites do not enter this map. */
	public static int activeDraws() { return DRAWS.size(); }
	public static boolean drawingAt(ServerLevel level, BlockPos at) {
		return DRAWS.values().stream().anyMatch(d -> d.level() == level && d.at().equals(at));
	}
	public static boolean held(Player p) {
		return p != null && p.getMainHandItem().is(BLADE) && BondedBlades.on(p) && BondedBlades.heldTier(p) > 0;
	}
	public static double guardCost(Player p) { return held(p) ? SleepingBladeRules.guardCost() : 1; }
	public static ItemStack history() {
		var s = new ItemStack(Items.WRITTEN_BOOK);
		s.set(DataComponents.ITEM_MODEL, Wildercord.id("last_oath"));
		s.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("The Last Oath"), "The Marchkeepers", 0,
			List.of(Filterable.passThrough(Component.translatable("book.wildercord.last_oath.1")),
				Filterable.passThrough(Component.translatable("book.wildercord.last_oath.2")),
				Filterable.passThrough(Component.translatable("book.wildercord.last_oath.3"))), true));
		return s;
	}
	private static void history(ServerPlayer p) {
		if (!Grimoire.unlock(p, "aura:sleeping_blade")) return;
		var book = history(); if (!p.getInventory().add(book)) p.drop(book, false, net.minecraft.util.Prediction.SERVER_ONLY);
	}
	public static void begin(ServerPlayer p, BlockPos at) {
		if (!Config.get().auraWorld().sleepingBlades() || !p.isAlive() || p.isSpectator() || !p.level().hasChunkAt(at)
				|| p.distanceToSqr(Vec3.atCenterOf(at)) > SleepingBladeRules.REACH * SleepingBladeRules.REACH) return;
		if (!(p.level().getBlockEntity(at) instanceof SleepingBladeEntity stone) || !stone.authentic()) { say(p, "empty"); return; }
		if (stone.claimedBy() != null) { stone.phase(4); history(p); say(p, "drawn"); return; }
		if (!BondedBlades.on(p)) { say(p, "off"); return; }
		if (BondedBlades.standing(p)) { say(p, "bonded"); return; }
		if (!ready(p, p.level(), at, p.position())) { say(p, "intent"); return; }
		if (DRAWS.containsKey(p.getUUID())) return;
		if (drawingAt(p.level(), at)) { say(p, "busy"); return; }
		stone.phase(0); DRAWS.put(p.getUUID(), new Draw(p.level(), at.immutable(), p.position(), 0));
		p.level().scheduleTick(at, STONE, 20);
		Feels.sound(p.level(), Vec3.atCenterOf(at), "aura_blade_stone_listen", .7F, 1); say(p, "begin");
	}
	private static boolean ready(ServerPlayer p, ServerLevel level, BlockPos at, Vec3 start) {
		return SleepingBladeRules.intent(Aura.stage(p), BondedBlades.on(p), p.getMainHandItem().isEmpty(), BondedBlades.standing(p))
			&& !p.isSpectator() && !p.isPassenger() && SleepingBladeRules.holding(p.isAlive(), p.isShiftKeyDown(), p.onGround(), p.level() == level,
				p.hurtTime > 0, p.position().distanceToSqr(start), p.distanceToSqr(Vec3.atCenterOf(at)))
			&& level.hasChunkAt(at) && level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(), Vec3.atCenterOf(at),
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p)).getBlockPos().equals(at);
	}
	private static void say(ServerPlayer p, String id) { p.sendOverlayMessage(Component.translatable("message.wildercord.sleeping_blade." + id)); }
	private static void tick(net.minecraft.server.MinecraftServer server) {
		if (server.getTickCount() % 5 != 0 || DRAWS.isEmpty()) return;
		var it = DRAWS.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next(); var d = e.getValue(); var p = server.getPlayerList().getPlayer(e.getKey());
			SleepingBladeEntity stone = d.level().hasChunkAt(d.at()) && d.level().getBlockEntity(d.at()) instanceof SleepingBladeEntity s ? s : null;
			if (p == null || !Config.get().auraWorld().sleepingBlades() || !ready(p, d.level(), d.at(), d.start())
					|| stone == null || !stone.authentic() || stone.claimedBy() != null) {
				it.remove(); if (stone != null && stone.claimedBy() == null) stone.phase(0); if (p != null) say(p, "broken"); continue;
			}
			int elapsed = d.elapsed() + 5;
			if (elapsed < SleepingBladeRules.DRAW_TICKS) {
				e.setValue(new Draw(d.level(), d.at(), d.start(), elapsed)); stone.phase(Math.min(3, elapsed / 30));
				if (elapsed % 30 == 0) {
					Feels.sound(d.level(), Vec3.atCenterOf(d.at()), "aura_blade_stone_strain", .65F, .85F + elapsed / 240F);
					AuraFx.groundScar(d.level(), Vec3.atBottomCenterOf(d.at()), 1.5 + elapsed / 60F, 35, 2);
				}
				if (elapsed % 20 == 0) p.sendOverlayMessage(Component.translatable("message.wildercord.sleeping_blade.progress", elapsed / 20, 6));
				continue;
			}
			it.remove(); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BLADE));
			if (!AuraApi.bondBlade(p, InteractionHand.MAIN_HAND, "sleeping_blade")) {
				p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); stone.phase(0); say(p, "failed"); continue;
			}
			stone.claim(p.getUUID()); AuraApi.addResonance(p, SleepingBladeRules.RESONANCE, "sleeping_blade"); history(p);
			Feels.sound(d.level(), Vec3.atCenterOf(d.at()), "aura_blade_stone_draw", .85F, 1);
			AuraFx.groundScar(d.level(), Vec3.atBottomCenterOf(d.at()), 3.5, 100, 2); say(p, "claimed");
		}
	}
	public static void init() {
		UseBlockCallback.EVENT.register((p, level, hand, hit) -> {
			if (!level.getBlockState(hit.getBlockPos()).is(STONE)) return InteractionResult.PASS;
			if (p instanceof ServerPlayer player && hand == InteractionHand.MAIN_HAND) begin(player, hit.getBlockPos());
			return InteractionResult.SUCCESS;
		});
		AuraApi.onMomentum((p, amount, source) -> {
			if (!held(p)) return amount;
			if (source.equals("guard")) {
				Feels.sound(p.level(), p.position(), "aura_oathkeeper_answer", .6F, 1);
				AuraFx.groundScar(p.level(), p.position(), 1.5, 45, 2);
			}
			return amount * SleepingBladeRules.momentum(source);
		});
		ServerTickEvents.END_SERVER_TICK.register(SleepingBlades::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> DRAWS.clear());
	}
}
