package dev.wildercord.content;

import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.cast.WildercordEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** Places a Training Dummy on the block clicked, facing you. */
public class TrainingDummyItem extends Item {
	public TrainingDummyItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getClickedFace() == Direction.DOWN) {
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		Vec3 at = Vec3.atBottomCenterOf(pos);
		TrainingDummy dummy = WildercordEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (dummy == null || !level.noCollision(dummy, dummy.getDimensions(dummy.getPose()).makeBoundingBox(at))) {
			return InteractionResult.FAIL;
		}
		float yaw = context.getPlayer() == null ? 0 : context.getPlayer().getYRot() + 180;
		dummy.snapTo(at.x, at.y, at.z, yaw, 0);
		dummy.setYHeadRot(yaw);
		dummy.setYBodyRot(yaw);
		level.addFreshEntity(dummy);
		level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		ItemStack stack = context.getItemInHand();
		if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<net.minecraft.network.chat.Component> builder, TooltipFlag flag) {
		builder.accept(net.minecraft.network.chat.Component.translatable("tooltip.wildercord.training_dummy").withStyle(net.minecraft.ChatFormatting.GRAY));
	}
}
