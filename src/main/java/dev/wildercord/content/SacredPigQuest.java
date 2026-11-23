package dev.wildercord.content;

import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** The sick sacred pig village quest: give everything, get the left nipple. */
public final class SacredPigQuest {
	public static final String PRIEST_TAG = "wildercord_sacred_pig_priest";
	public static final String PIG_TAG = "wildercord_sacred_pig";
	public static final String DONE_TAG = "wildercord_sacred_pig_done";
	private static final Map<UUID, Long> WARNED = new WeakHashMap<>();

	private SacredPigQuest() {}

	public static void init() {
		UseEntityCallback.EVENT.register((Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) -> {
			if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
			if (!entity.getTags().contains(PRIEST_TAG)) return InteractionResult.PASS;
			if (entity.getTags().contains(DONE_TAG)) {
				sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig.done").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
				return InteractionResult.SUCCESS;
			}
			long now = sp.level().getGameTime();
			Long warned = WARNED.get(sp.getUUID());
			if (player.isShiftKeyDown() && warned != null && now - warned < 600) {
				takeEverything(sp, entity);
				return InteractionResult.SUCCESS;
			}
			WARNED.put(sp.getUUID(), now);
			sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig.line1").withColor(0xC9A87C).withStyle(ChatFormatting.ITALIC));
			sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig.line2").withColor(0xE0C8A0));
			sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig.line3").withColor(0xFFAA88));
			sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig.confirm").withStyle(ChatFormatting.YELLOW));
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.8F, 0.8F);
			return InteractionResult.SUCCESS;
		});
	}

	private static void takeEverything(ServerPlayer sp, Entity priest) {
		// Take literally everything - armor, offhand, inventory, cursor.
		sp.getInventory().clearContent();
		sp.containerMenu.setCarried(ItemStack.EMPTY);
		sp.containerMenu.broadcastChanges();
		sp.inventoryMenu.broadcastFullState();
		ItemStack reward = new ItemStack(WildercordItems.SACRED_PIG_LEFT_NIPPLE);
		if (!sp.getInventory().add(reward)) {
			net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(sp.level(), sp.getX(), sp.getY() + 0.5, sp.getZ(), reward);
			sp.level().addFreshEntity(ie);
		}
		priest.addTag(DONE_TAG);
		sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig.complete1").withColor(0xF2D16B).withStyle(ChatFormatting.BOLD));
		sp.sendSystemMessage(Component.translatable("message.wildercord.sacred_pig.complete2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 0.7F);
		sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.6F);
		WARNED.remove(sp.getUUID());
	}

	/** Spawns the sick sacred pig and priest for structure generation. */
	public static void spawnNpc(ServerLevel level, double x, double y, double z) {
		Entity pig = create(level, "minecraft:pig");
		if (pig instanceof Pig p) {
			p.snapTo(x + 0.5, y, z + 0.5, 180.0F, 0.0F);
			p.setCustomName(Component.literal("Sacred Pig").withStyle(ChatFormatting.LIGHT_PURPLE));
			p.setCustomNameVisible(true);
			p.addTag(PIG_TAG);
			p.setNoAi(true);
			level.addFreshEntityWithPassengers(p);
		} else if (pig != null) {
			pig.snapTo(x + 0.5, y, z + 0.5, 180.0F, 0.0F);
			pig.setCustomName(Component.literal("Sacred Pig").withStyle(ChatFormatting.LIGHT_PURPLE));
			pig.setCustomNameVisible(true);
			pig.addTag(PIG_TAG);
			level.addFreshEntityWithPassengers(pig);
		}
		Entity priest = create(level, "minecraft:villager");
		if (priest instanceof Villager v) {
			v.snapTo(x + 2.5, y, z + 0.5, 180.0F, 0.0F);
			v.setCustomName(Component.literal("Pig Priest").withStyle(ChatFormatting.GOLD));
			v.setCustomNameVisible(true);
			v.addTag(PRIEST_TAG);
			v.setPersistenceRequired();
			level.addFreshEntityWithPassengers(v);
		}
	}

	@SuppressWarnings("unchecked")
	private static Entity create(ServerLevel level, String id) {
		return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
			.getOptional(net.minecraft.resources.ResourceLocation.parse(id))
			.map(t -> ((EntityType<? extends Entity>) t).create(level, EntitySpawnReason.STRUCTURE))
			.orElse(null);
	}
}
