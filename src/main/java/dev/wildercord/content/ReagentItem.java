package dev.wildercord.content;

import dev.wildercord.cast.TemporaryBlocks;
import dev.wildercord.mixin.AbstractFurnaceBlockEntityAccessor;
import dev.wildercord.world.ResidueRules.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Consumer;

/**
 * A reagent, and its small everyday use (kept modest: the altar is where reagents matter):
 * <ul>
 *   <li>Cinder Ash burns in a furnace, briefly but hot;</li>
 *   <li>an Everfrost Shard freezes a source of water into ice, or of lava into obsidian;</li>
 *   <li>a Fulgurite Shard scrapes a stage of weathering off copper;</li>
 *   <li>a Bottled Gale, drunk, lets you fall slowly for 30 seconds;</li>
 *   <li>a pinch of Geode Grit shows the ore around you, as Prospect does;</li>
 *   <li>a Wildbloom Petal grows a plant as bone meal does;</li>
 *   <li>Hollow Dust draws every loose item and orb within 10 blocks to you;</li>
 *   <li>Star Dust hangs a mote of starlight (light 12) in the air for five minutes;</li>
 *   <li>Hourglass Sand jumps a furnace ten seconds ahead, or grows a young animal two minutes older;</li>
 *   <li>a Sanguine Bead grows nether wart a stage.</li>
 * </ul>
 */
public class ReagentItem extends Item {
	/** How long Star Dust's light hangs, in ticks. */
	public static final int STARLIGHT_TICKS = 6000;
	/** How far Hollow Dust reaches. */
	public static final double HOLLOW_REACH = 10;
	/** How far Geode Grit listens for ore, and for how long it shows. */
	private static final double GRIT_RADIUS = 8;
	private static final int GRIT_TICKS = 200;

	public final Kind kind;

	public ReagentItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("item.wildercord." + kind.reagent + ".altar").withColor(0xC8A8F0));
		out.accept(Component.translatable("item.wildercord." + kind.reagent + ".use").withColor(0xB8B0C8));
	}

	// ------------------------------------------------------------------ used on a block

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		BlockState state = level.getBlockState(pos);
		if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
			return InteractionResult.PASS;
		}
		switch (kind) {
			case FULGURITE -> {
				var scraped = WeatheringCopper.getPrevious(state);
				if (scraped.isEmpty()) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					level.setBlockAndUpdate(pos, scraped.get());
					level.levelEvent(null, LevelEvent.PARTICLES_SCRAPE, pos, 0);
					level.playSound(null, pos, SoundEvents.AXE_SCRAPE.value(), SoundSource.BLOCKS, 1.0F, 1.3F);
					use(stack, player);
				}
				return InteractionResult.SUCCESS;
			}
			case WILDBLOOM -> {
				if (!BoneMealItem.growCrop(stack, level, pos)) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
				}
				return InteractionResult.SUCCESS;
			}
			case STILLED_SAND -> {
				if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) {
					return InteractionResult.PASS;
				}
				AbstractFurnaceBlockEntityAccessor smelting = (AbstractFurnaceBlockEntityAccessor) furnace;
				int timer = smelting.wildercord$cookingTimer();
				int total = smelting.wildercord$cookingTotalTime();
				int fuel = smelting.wildercord$litTimeRemaining();
				if (timer <= 0 || fuel <= 0 || total <= 0 || furnace.getItem(0).isEmpty()) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					// Never all the way: the furnace still finishes the smelt itself, on its next tick.
					int skip = Math.min(200, Math.min(fuel, total - 1 - timer));
					smelting.wildercord$setCookingTimer(timer + Math.max(0, skip));
					furnace.setChanged();
					level.playSound(null, pos, SoundEvents.SAND_FALL, SoundSource.BLOCKS, 0.8F, 1.4F);
					use(stack, player);
				}
				return InteractionResult.SUCCESS;
			}
			case BLOODMOSS -> {
				if (!state.is(Blocks.NETHER_WART) || state.getValue(NetherWartBlock.AGE) >= 3) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					level.setBlockAndUpdate(pos, state.setValue(NetherWartBlock.AGE, state.getValue(NetherWartBlock.AGE) + 1));
					level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 6);
					use(stack, player);
				}
				return InteractionResult.SUCCESS;
			}
			case STAR_GLYPH -> {
				BlockPos at = pos.relative(context.getClickedFace());
				if (!level.getBlockState(at).isAir() || level.isOutsideBuildHeight(at)) {
					return InteractionResult.PASS;
				}
				if (level instanceof ServerLevel server) {
					if (player instanceof ServerPlayer sp && !server.mayInteract(sp, at)) {
						return InteractionResult.PASS;
					}
					// The light is a spell's passing block (written down, so it goes even after a restart, and drops nothing).
					BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12);
					server.setBlockAndUpdate(at, light);
					TemporaryBlocks.put(server, at, light, Blocks.AIR.defaultBlockState(), server.getGameTime() + STARLIGHT_TICKS);
					server.sendParticles(new MaterialOption(MaterialOption.ARCANE, 0xFFD8FA, 0.12F, 40), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.25,
						0.25, 0.25, 0.02);
					server.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.5F);
					use(stack, player);
				}
				return InteractionResult.SUCCESS;
			}
			default -> {
				return super.useOn(context);
			}
		}
	}

	// ------------------------------------------------------------------ used in the air

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		switch (kind) {
			case EVERFROST -> {
				BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
				if (hit.getType() != HitResult.Type.BLOCK) {
					return InteractionResult.PASS;
				}
				BlockPos pos = hit.getBlockPos();
				FluidState fluid = level.getFluidState(pos);
				boolean water = fluid.is(Fluids.WATER) && fluid.isSource();
				boolean lava = fluid.is(Fluids.LAVA) && fluid.isSource();
				if (!water && !lava || !player.mayUseItemAt(pos, hit.getDirection(), stack) || !level.mayInteract(player, pos)) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide()) {
					level.setBlockAndUpdate(pos, (water ? Blocks.ICE : Blocks.OBSIDIAN).defaultBlockState());
					level.playSound(null, pos, water ? SoundEvents.GLASS_PLACE : SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.2F);
					((ServerLevel) level).sendParticles(new MaterialOption(MaterialOption.FROST, 0xD8F6FF, 0.12F, 30), pos.getX() + 0.5, pos.getY() + 1.0,
						pos.getZ() + 0.5, 10, 0.35, 0.1, 0.35, 0.02);
					use(stack, player);
				}
				return InteractionResult.SUCCESS;
			}
			case RIVEN_STONE -> {
				if (player.getCooldowns().isOnCooldown(stack)) {
					return InteractionResult.PASS;
				}
				if (player instanceof ServerPlayer server) {
					dev.wildercord.cast.CraftedRunes.prospect(server, GRIT_RADIUS, GRIT_TICKS);
					server.level().playSound(null, server.blockPosition(), SoundEvents.TUFF_BREAK, SoundSource.PLAYERS, 0.8F, 0.7F);
					player.getCooldowns().addCooldown(stack, 40);
					use(stack, player);
				}
				return InteractionResult.SUCCESS;
			}
			case VOID_SCAR -> {
				if (player.getCooldowns().isOnCooldown(stack)) {
					return InteractionResult.PASS;
				}
				if (level instanceof ServerLevel server) {
					int drawn = 0;
					for (Entity loose : server.getEntities((Entity) null, player.getBoundingBox().inflate(HOLLOW_REACH),
							e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
						server.sendParticles(new MaterialOption(MaterialOption.VOID, 0x1A0830, 0.14F, 16), loose.getX(), loose.getY() + 0.2, loose.getZ(), 4, 0.1, 0.1,
							0.1, 0.01);
						loose.teleportTo(player.getX(), player.getY() + 0.2, player.getZ());
						loose.setDeltaMovement(0, 0, 0);
						drawn++;
					}
					server.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, drawn > 0 ? 0.6F : 1.4F);
					player.getCooldowns().addCooldown(stack, 20);
					use(stack, player);
				}
				return InteractionResult.SUCCESS;
			}
			default -> {
				return super.use(level, player, hand);
			}
		}
	}

	// ------------------------------------------------------------------ used on a creature

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
		if (kind != Kind.STILLED_SAND || !(target instanceof AgeableMob young) || !young.isBaby()) {
			return super.interactLivingEntity(stack, player, target, hand);
		}
		if (player.level() instanceof ServerLevel server) {
			young.ageUp(120, true);
			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, young.getX(), young.getY() + young.getBbHeight(), young.getZ(), 6, 0.3, 0.3, 0.3, 0);
			use(stack, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** One used up (none in creative). */
	private static void use(ItemStack stack, Player player) {
		if (player == null || !player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
	}
}
