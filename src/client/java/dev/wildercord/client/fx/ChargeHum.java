package dev.wildercord.client.fx;

import dev.wildercord.cast.Charging;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * The hum of a spell being charged: a warm loop that follows the caster, climbing in pitch as the
 * charge builds (from 0.8 to 1.3 at full) and fading in and out with it. Like the circle in
 * {@link ChargeCircles}, every nearby client plays it from the synced charge.
 */
public final class ChargeHum extends AbstractTickableSoundInstance {
	/** The hum still following each caster's current charge, by entity id. */
	private static final Map<Integer, ChargeHum> PLAYING = new HashMap<>();

	private static final float LOW = 0.8F;
	private static final float HIGH = 1.3F;
	private static final float VOLUME = 0.6F;
	/** Ticks to fade in, and out once the charge ends. */
	private static final int FADE_IN = 6;
	private static final int FADE_OUT = 5;

	private final Player caster;
	private final long start;
	private float fade;

	/** Every client tick: starts a hum for each charge that doesn't have one yet. Each hum then looks after itself. */
	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null) {
			PLAYING.clear();
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		// A hum that is fading out keeps playing on its own; forgetting it lets a new charge start its own.
		PLAYING.values().removeIf(hum -> hum.isStopped() || !hum.charging());
		for (Player player : level.players()) {
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			if (charge == null || PLAYING.containsKey(player.getId())) {
				continue;
			}
			ChargeHum hum = new ChargeHum(player, charge.start());
			PLAYING.put(player.getId(), hum);
			mc.getSoundManager().play(hum);
		}
	}

	private ChargeHum(Player caster, long start) {
		super(WildercordSounds.CHARGE_LOOP, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.caster = caster;
		this.start = start;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.0F;
		this.pitch = LOW;
		follow();
	}

	/** Whether the charge this hum began with is still going. */
	private boolean charging() {
		WildercordAttachments.Charge charge = caster.getAttached(WildercordAttachments.CHARGE);
		return !caster.isRemoved() && charge != null && charge.start() == start;
	}

	@Override
	public void tick() {
		if (charging()) {
			fade = Math.min(1F, fade + 1F / FADE_IN);
		} else {
			fade -= 1F / FADE_OUT;
			if (fade <= 0) {
				stop();
				return;
			}
		}
		double progress = Mth.clamp((caster.level().getGameTime() - start) / (double) Charging.FULL, 0, 1);
		// Eased out: a quick climb at first that settles as the charge nears full.
		float eased = (float) (1 - (1 - progress) * (1 - progress));
		pitch = LOW + (HIGH - LOW) * eased;
		volume = VOLUME * fade * (0.75F + 0.25F * eased);
		follow();
	}

	/** At the caster's chest, so it moves with them. */
	private void follow() {
		x = caster.getX();
		y = caster.getY() + caster.getBbHeight() * 0.6;
		z = caster.getZ();
	}

	/** It starts at zero volume and fades in. */
	@Override
	public boolean canStartSilent() {
		return true;
	}
}
