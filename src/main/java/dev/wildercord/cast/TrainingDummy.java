package dev.wildercord.cast;

import com.mojang.math.Transformation;
import dev.wildercord.content.WildercordItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * A straw practice dummy for trying spells out. It never dies: every hit floats up as a number
 * (coloured by what kind of damage it was), and its name shows damage per second over the last
 * five seconds and the total of the current burst. Punch it while sneaking to pick it back up.
 */
public class TrainingDummy extends LivingEntity {
	private record Hit(long time, float amount) {}

	private final Deque<Hit> hits = new ArrayDeque<>();
	private float total;
	private float lastDamage;
	/** Actual health damage of the last hurt call, before this dummy restores itself. Zero for a rejected hit. */
	public float lastDamage() { return lastDamage; }
	private long lastHit;
	private long firstHit;
	private boolean practiceMoving;
	private double practiceX, practiceZ;
	public void setPracticeMoving(boolean moving) { practiceMoving=moving; practiceX=getX(); practiceZ=getZ(); }

	public TrainingDummy(EntityType<? extends TrainingDummy> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 1000.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		lastDamage = 0;
		// A sneaking punch picks it up; a sneaking caster's spell still hits it (for If Sneaking).
		if (source.getDirectEntity() instanceof ServerPlayer player && source.getEntity() == player && player.isShiftKeyDown()
				&& source.is(DamageTypes.PLAYER_ATTACK)) {
			pickUp(level, player);
			return true;
		}
		if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurtServer(level, source, damage);
		}
		float before = getHealth();
		boolean hurt = super.hurtServer(level, source, damage);
		float dealt = Math.max(0, before - getHealth());
		lastDamage = dealt;
		setHealth(getMaxHealth());
		if (dealt > 0.01F) {
			record(level, dealt, source);
		}
		return hurt;
	}

	private void record(ServerLevel level, float dealt, DamageSource source) {
		if(source.getEntity() instanceof ServerPlayer player&&(source.is(DamageTypes.MAGIC)||source.is(DamageTypes.INDIRECT_MAGIC)))SpellTrials.hit(player,dealt);
		long now = level.getGameTime();
		if (now - lastHit > 60) {
			hits.clear();
			total = 0;
			firstHit = now;
		}
		lastHit = now;
		hits.addLast(new Hit(now, dealt));
		total += dealt;
		number(level, dealt, colorOf(source));
		updateName(now);
	}

	private void updateName(long now) {
		while (!hits.isEmpty() && now - hits.peekFirst().time() > 100) {
			hits.removeFirst();
		}
		float recent = 0;
		for (Hit hit : hits) {
			recent += hit.amount();
		}
		double seconds = Math.max(1.0, Math.min(5.0, (now - firstHit) / 20.0));
		Component name = Component.translatable("entity.wildercord.training_dummy.dps",
			Component.literal(String.format(Locale.ROOT, "%.1f", recent / seconds)).withColor(0xFFE070),
			Component.literal(String.format(Locale.ROOT, "%.0f", total)).withColor(0xFFFFFF)).withColor(0xB8B0C8);
		setCustomName(name);
		setCustomNameVisible(true);
	}

	private static int colorOf(DamageSource source) {
		if (source.is(DamageTypeTags.IS_FIRE)) {
			return 0xFF8A3A;
		}
		if (source.is(DamageTypeTags.IS_FREEZING)) {
			return 0x9FE4FF;
		}
		if (source.is(DamageTypeTags.IS_LIGHTNING)) {
			return 0xFFF070;
		}
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			return 0xFFB040;
		}
		if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
			return 0xE890F0;
		}
		return 0xF0F0F0;
	}

	/** A number that pops out of the dummy, drifts up and fades. */
	private void number(ServerLevel level, float amount, int color) {
		Display.TextDisplay text = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (text == null) {
			return;
		}
		double side = (level.getRandom().nextDouble() - 0.5) * 0.9;
		Vec3 at = position().add(side, getBbHeight() + 0.65, (level.getRandom().nextDouble() - 0.5) * 0.9);
		text.snapTo(at.x, at.y, at.z);
		String shown = amount >= 10 ? String.format(Locale.ROOT, "%.0f", amount) : String.format(Locale.ROOT, "%.1f", amount);
		text.setText(Component.literal(shown).withColor(color).withStyle(ChatFormatting.BOLD));
		text.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		text.setBackgroundColor(0);
		text.setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));
		float scale = (float) Math.min(1.8, 0.8 + amount / 25.0);
		text.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
		text.setPosRotInterpolationDuration(14);
		BlockFx.fresh(text);
		level.addFreshEntity(text);
		Scheduler.later(2, () -> {
			if (!text.isRemoved()) {
				text.setPos(at.x, at.y + 0.9, at.z);
			}
		});
		Scheduler.later(18, text::discard);
	}

	private void pickUp(ServerLevel level, ServerPlayer player) {
		if (!player.isCreative()) {
			ItemEntity item = new ItemEntity(level, getX(), getY() + 0.5, getZ(), new ItemStack(WildercordItems.TRAINING_DUMMY));
			level.addFreshEntity(item);
		}
		Fx.sound(level, position(), SoundEvents.WOOL_BREAK, 1.0F, 0.8F);
		Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
			net.minecraft.world.level.block.Blocks.HAY_BLOCK.defaultBlockState()), position().add(0, 1, 0), 20, 0.3, 0.1);
		discard();
	}

	@Override
	public void tick() {
		super.tick();
		if (practiceMoving && level() instanceof ServerLevel level) {
			setDeltaMovement(Vec3.ZERO);
			setPos(practiceX + Math.sin(level.getGameTime()*.045+getId())*1.3,81,practiceZ);
		}
		if (level() instanceof ServerLevel level && lastHit > 0 && level.getGameTime() - lastHit > 80) {
			lastHit = 0;
			hits.clear();
			total = 0;
			setCustomName(null);
			setCustomNameVisible(false);
		}
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		// So a DPS nameplate saved mid-fight still clears once the dummy loads again.
		output.putLong("last_hit", lastHit);
		output.putBoolean("practice_moving", practiceMoving);
		output.putDouble("practice_x", practiceX); output.putDouble("practice_z", practiceZ);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		lastHit = input.getLongOr("last_hit", 0L);
		practiceMoving=input.getBooleanOr("practice_moving",false);
		practiceX=input.getDoubleOr("practice_x",getX()); practiceZ=input.getDoubleOr("practice_z",getZ());
	}
}
