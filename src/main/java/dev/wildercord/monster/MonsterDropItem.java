package dev.wildercord.monster;

import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * What the monsters of the wilds drop, each with a line of lore and a line saying what it's good for. The plain ones
 * (Shadow Pelt, Bog Gland) are brewing ingredients; the rest have uses of their own, below.
 */
public class MonsterDropItem extends Item {
	private final String id;

	public MonsterDropItem(String id, Properties properties) {
		super(properties);
		this.id = id;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("item.wildercord." + id + ".lore").withStyle(ChatFormatting.ITALIC).withColor(0x9A94A8));
		out.accept(Component.translatable("item.wildercord." + id + ".use").withColor(0xB8D8A8));
	}

	/** Living Bramble: thrown, it roots the creature it hits for two seconds (see {@link ThrownBramble}). */
	public static class LivingBramble extends MonsterDropItem {
		public LivingBramble(Properties properties) {
			super("living_bramble", properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
				0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
			if (level instanceof ServerLevel server) {
				Projectile.spawnProjectileFromRotation(ThrownBramble::new, server, stack, player, 0.0F, 1.3F, 1.0F);
			}
			player.getCooldowns().addCooldown(stack, 10);
			player.awardStat(Stats.ITEM_USED.get(this));
			stack.consume(1, player);
			return InteractionResult.SUCCESS;
		}
	}

	/** A Storm Feather: used, a gust lifts you a few blocks and you drift down after (once a second). */
	public static class StormFeather extends MonsterDropItem {
		public static final int COOLDOWN = 20;

		public StormFeather(Properties properties) {
			super("storm_feather", properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x * 1.2, Math.max(v.y, 0) + 0.85, v.z * 1.2);
			player.resetFallDistance();
			if (level instanceof ServerLevel server) {
				if (player instanceof ServerPlayer sp) {
					MonsterMagic.sync(sp);
				}
				player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
				dev.wildercord.cast.ElementFx.gustRing(server, player.position(), 1.2);
				dev.wildercord.cast.ElementFx.sparks(server, player.position().add(0, 0.3, 0), 4, 0.15);
				server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8F, 1.3F);
			}
			player.getCooldowns().addCooldown(stack, COOLDOWN);
			player.awardStat(Stats.ITEM_USED.get(this));
			stack.consume(1, player);
			return InteractionResult.SUCCESS;
		}
	}

	/** Mana Gel: eaten, it gives back a little mana at once. */
	public static class ManaGel extends MonsterDropItem {
		/** The mana a gel gives back. */
		public static final float MANA = 15.0F;

		public ManaGel(Properties properties) {
			super("mana_gel", properties);
		}

		@Override
		public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
			if (entity instanceof ServerPlayer player) {
				Spellbooks.setMana(player, Math.min(Mana.max(player), Spellbooks.mana(player) + MANA));
				dev.wildercord.cast.ElementFx.shimmer((ServerLevel) level, player.getBoundingBox().getCenter(), 0.4, 6);
			}
			return super.finishUsingItem(stack, level, entity);
		}
	}
}
