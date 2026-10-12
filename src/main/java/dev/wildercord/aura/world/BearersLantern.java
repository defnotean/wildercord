package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Bearer's Lantern: the soul lantern the Lantern-Bearer leaves at the foot of its gibbet. While it walks the Barrow Field it will
 * not let it go; once it lies still, anyone may take it down, and with it the book a gravedigger left there, telling of the field. Lit,
 * it calls up the Barrow Steed, a pale dead horse that rises out of the ground under its bearer and carries them until they get down,
 * when it sinks back to dust; then the lantern must rest a while.
 */
public final class BearersLantern {
	private BearersLantern() {}

	private static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Wildercord.id("barrow_lantern"));
	/** The lantern as it stands at the gibbet's foot, there for the taking. */
	public static final BarrowLanternBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, KEY, new BarrowLanternBlock(BlockBehaviour.Properties.of()
		.setId(KEY).noOcclusion().strength(-1, 3600000).noLootTable().sound(SoundType.LANTERN).lightLevel(state -> 10)
		.pushReaction(PushReaction.IMMOVEABLE)));
	private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("bearers_lantern"));
	/** The lantern as it is carried: lit, it calls up the Barrow Steed. */
	public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
		new LanternItem(new Item.Properties().setId(ITEM_KEY).stacksTo(1).rarity(Rarity.EPIC)));

	/** Whether this player has taken the lantern down before. */
	public static boolean bound(Player player) {
		return player.getAttachedOrElse(WildercordAttachments.BARROW_LANTERN, false);
	}

	/** Whether this player carries a Bearer's Lantern now. */
	static boolean carries(Player player) {
		return player.getInventory().contains(stack -> stack.is(ITEM));
	}

	/** Take the lantern down: the lantern itself, and the first time the gravedigger's book with it. */
	static void take(ServerPlayer player) {
		boolean first = !bound(player);
		player.setAttached(WildercordAttachments.BARROW_LANTERN, true);
		give(player, new ItemStack(ITEM));
		if (first) give(player, book());
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
	}

	/** The gravedigger's book: what happened on the Barrow Field, and what still walks it. */
	static ItemStack book() {
		var book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough("The Barrow Field"), "a gravedigger", 0,
			List.of(Filterable.passThrough(Component.translatable("book.wildercord.barrow.1")),
				Filterable.passThrough(Component.translatable("book.wildercord.barrow.2")),
				Filterable.passThrough(Component.translatable("book.wildercord.barrow.3")),
				Filterable.passThrough(Component.translatable("book.wildercord.barrow.4"))), true));
		return book;
	}

	/**
	 * The Barrow Steed rises under {@code player}: tame, saddled and past harm, swifter than any living horse, and it carries them until
	 * they get down (or die), when it sinks back into the earth.
	 */
	static boolean steed(ServerPlayer player) {
		ServerLevel level = player.level();
		SkeletonHorse horse = EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.TRIGGERED);
		if (horse == null) return false;
		horse.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
		horse.setTamed(true);
		horse.setOwner(player);
		horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
		horse.setPermanentlyInvulnerable(true);
		horse.setPersistenceRequired();
		horse.setCustomName(Component.translatable("entity.wildercord.barrow_steed"));
		var speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.setBaseValue(LanternBearerRules.STEED_SPEED);
		var jump = horse.getAttribute(Attributes.JUMP_STRENGTH);
		if (jump != null) jump.setBaseValue(LanternBearerRules.STEED_JUMP);
		// Saved with an end time, as a summoned wolf is, so a restart can never leave it behind for good (see Spirits).
		horse.setAttached(WildercordAttachments.SPIRIT_UNTIL, level.getGameTime() + LanternBearerRules.STEED_LINGER);
		level.addFreshEntity(horse);
		player.startRiding(horse, true, true);
		var feet = horse.position();
		level.sendParticles(ParticleTypes.SOUL, feet.x, feet.y + .3, feet.z, 24, .7, .3, .7, .03);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, feet.x, feet.y + .1, feet.z, 30, .9, .05, .9, .02);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, feet.x, feet.y + .6, feet.z, 12, .6, .4, .6, .01);
		Feels.sound(level, feet, "aura_lantern_bearer_rise", .8F, 1.2F);
		level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.SKELETON_HORSE_AMBIENT, SoundSource.PLAYERS, 1, .8F);
		ride(level, horse, player, 0);
		return true;
	}

	/** Every few ticks: soul-fire trailing from its hooves while it is ridden, and once its rider has been off it a while, it sinks away. */
	private static void ride(ServerLevel level, SkeletonHorse horse, ServerPlayer player, int empty) {
		Scheduler.later(LanternBearerRules.STEED_CHECK, () -> {
			if (horse.isRemoved()) return;
			int now = horse.hasPassenger(player) ? 0 : empty + 1;
			if (!LanternBearerRules.steedStays(player.isAlive() && !player.hasDisconnected(), now)) {
				horse.ejectPassengers();
				var at = horse.getBoundingBox().getCenter();
				level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 20, .5, .6, .5, .03);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 16, .5, .6, .5, .02);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.SKELETON_HORSE_DEATH, SoundSource.PLAYERS, .6F, 1.3F);
				horse.discard();
				return;
			}
			horse.setAttached(WildercordAttachments.SPIRIT_UNTIL, level.getGameTime() + LanternBearerRules.STEED_LINGER);
			var feet = horse.position();
			if (horse.getDeltaMovement().horizontalDistanceSqr() > 1e-3)
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, feet.x, feet.y + .15, feet.z, 2, .25, .05, .25, .005);
			if (level.getRandom().nextInt(3) == 0)
				level.sendParticles(ParticleTypes.SOUL, feet.x, feet.y + 1.1, feet.z, 1, .3, .3, .3, .01);
			ride(level, horse, player, now);
		});
	}

	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(ITEM));
	}

	/** The lantern carried: lit, it calls up the Barrow Steed, then must rest. */
	public static final class LanternItem extends Item {
		public LanternItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (!(player instanceof ServerPlayer server)) return InteractionResult.SUCCESS;
			ItemStack stack = player.getItemInHand(hand);
			if (player.isPassenger() || player.isSpectator()) {
				server.sendOverlayMessage(Component.translatable("message.wildercord.bearers_lantern.mounted").withColor(0x8AA8B0));
				return InteractionResult.FAIL;
			}
			if (!steed(server)) return InteractionResult.FAIL;
			player.getCooldowns().addCooldown(stack, LanternBearerRules.STEED_REST);
			return InteractionResult.SUCCESS;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
			out.accept(Component.translatable("item.wildercord.bearers_lantern.lore").withStyle(ChatFormatting.ITALIC).withColor(0x8AC8D0));
			out.accept(Component.translatable("item.wildercord.bearers_lantern.use").withColor(0xB8D8A8));
		}
	}
}
