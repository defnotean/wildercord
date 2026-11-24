package dev.wildercord.bedrock;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BladeBond;
import dev.wildercord.aura.BladeRules;
import dev.wildercord.aura.BondedBlades;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.Clashes;
import dev.wildercord.aura.FormDash;
import dev.wildercord.aura.FormDashLessons;
import dev.wildercord.aura.FormDashRules;
import dev.wildercord.aura.Lineage;
import dev.wildercord.aura.MasterFormLessons;
import dev.wildercord.aura.MasterForms;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.StoneHingeRules;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.Techniques;
import dev.wildercord.aura.Unity;
import dev.wildercord.aura.WallTurnRules;
import dev.wildercord.aura.world.Duelist;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Complete Bedrock / Console UI and weapon interaction bridge for Wildercord's Aura / Knights
 * combat systems: Aura techniques, FormDash & Master Forms, Master's Arts, Sword Strings,
 * Custom Technique Writer, Bonded Blades, Lineage, Unity, and Sword Clashes.
 */
public final class BedrockAuraForms {
	private static final int[] ALL_DASH_FORMS = {
		FormDashRules.CINDER_LUNGE, FormDashRules.REED_SLIP, FormDashRules.AIR_STEP, FormDashRules.PLUNGE,
		FormDashRules.RIPOSTE, FormDashRules.SHOVE, FormDashRules.GUARD_BREAK, FormDashRules.SPELL_CUT
	};

	private static final Map<UUID, long[]> DASH_SEQ = new ConcurrentHashMap<>();
	private static final Map<UUID, long[]> MASTER_SEQ = new ConcurrentHashMap<>();
	private static final Queue<Runnable> NEXT_TICK = new ConcurrentLinkedQueue<>();
	private static volatile boolean initialized = false;

	private BedrockAuraForms() {}

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClientSide() || !(player instanceof ServerPlayer server) || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			if (!BedrockPlayers.isBedrock(server)) {
				return InteractionResult.PASS;
			}
			if (Clashes.clashing(server)) {
				Clashes.pressFor(server);
				return InteractionResult.SUCCESS;
			}
			ItemStack held = server.getItemInHand(hand);
			if (held.is(Aura.WEAPONS)) {
				if (server.isShiftKeyDown()) {
					openAuraHub(server);
					return InteractionResult.SUCCESS;
				}
				if (FormDash.followOpen(server) && FormDash.triggerCounter(server)) {
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int n = NEXT_TICK.size();
			for (int i = 0; i < n; i++) {
				Runnable task = NEXT_TICK.poll();
				if (task == null) {
					break;
				}
				task.run();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			DASH_SEQ.clear();
			MASTER_SEQ.clear();
			NEXT_TICK.clear();
		});
	}

	// ---- Quick command / keybind triggers

	public static void triggerSlash(ServerPlayer player) {
		Aura.press(player, AuraApi.Trigger.TAP);
	}

	public static void triggerGuard(ServerPlayer player) {
		Aura.press(player, AuraApi.Trigger.SNEAK_TAP);
	}

	public static void triggerStep(ServerPlayer player) {
		Aura.press(player, AuraApi.Trigger.DOUBLE_TAP);
	}

	public static void triggerDominion(ServerPlayer player) {
		Aura.press(player, AuraApi.Trigger.HOLD);
	}

	public static void triggerAwaken(ServerPlayer player) {
		Aura.press(player, AuraApi.Trigger.TAP_HOLD);
	}

	public static void triggerDash(ServerPlayer player) {
		if (MasterForms.data(player).equipped() != 0) {
			sendMasterForm(player, WallTurnRules.PRESS);
			return;
		}
		int equipped = FormDash.data(player).equipped();
		sendFormDash(player, WallTurnRules.PRESS, equipped);
	}

	public static void triggerUnity(ServerPlayer player) {
		if (!Unity.begin(player)) {
			String why = Unity.refusal(player);
			if (why != null && !why.isEmpty()) {
				player.sendSystemMessage(Component.translatable(why).withStyle(ChatFormatting.YELLOW));
			}
		}
	}

	public static void triggerClash(ServerPlayer player) {
		Clashes.pressFor(player);
	}

	public static void triggerCounter(ServerPlayer player) {
		if (!FormDash.triggerCounter(player)) {
			player.sendOverlayMessage(Component.literal("No Spell Cut window active right now (hold an Aura weapon with Spell Cut learned).").withStyle(ChatFormatting.YELLOW));
		}
	}

	private static long nextSeq(Map<UUID, long[]> map, UUID playerId, long epoch) {
		long[] cur = map.get(playerId);
		if (cur == null || cur[0] != epoch) {
			long[] fresh = new long[] { epoch, 1L };
			map.put(playerId, fresh);
			return 1L;
		}
		cur[1] += 1L;
		return cur[1];
	}

	private static boolean sendFormDash(ServerPlayer player, int action, int form) {
		long epoch = FormDash.view(player).epoch();
		long seq = nextSeq(DASH_SEQ, player.getUUID(), epoch);
		boolean ok = FormDash.request(player, new FormDash.Action(action, form, epoch, seq));
		if (ok && action == WallTurnRules.PRESS) {
			NEXT_TICK.add(() -> {
				if (player.connection != null && !player.isRemoved()) {
					long relEpoch = FormDash.view(player).epoch();
					long relSeq = nextSeq(DASH_SEQ, player.getUUID(), relEpoch);
					FormDash.request(player, new FormDash.Action(WallTurnRules.RELEASE, form, relEpoch, relSeq));
				}
			});
		}
		return ok;
	}

	private static boolean sendMasterForm(ServerPlayer player, int action) {
		long epoch = MasterForms.view(player).epoch();
		long seq = nextSeq(MASTER_SEQ, player.getUUID(), epoch);
		boolean ok = MasterForms.request(player, new MasterForms.Action(action, epoch, seq));
		if (ok && action == WallTurnRules.PRESS) {
			NEXT_TICK.add(() -> {
				if (player.connection != null && !player.isRemoved()) {
					long relEpoch = MasterForms.view(player).epoch();
					long relSeq = nextSeq(MASTER_SEQ, player.getUUID(), relEpoch);
					MasterForms.request(player, new MasterForms.Action(WallTurnRules.RELEASE, relEpoch, relSeq));
				}
			});
		}
		return ok;
	}

	// ---- Cumulus Forms

	public static void openAuraHub(ServerPlayer player) {
		if (!BedrockBridge.isFloodgateLoaded() || !BedrockPlayers.isBedrock(player)) {
			int stage = Aura.stage(player);
			String stageId = stage > 0 ? AuraStages.get(stage).map(AuraStages.Stage::id).orElse("stage " + stage) : "unawakened";
			player.sendSystemMessage(Component.literal(String.format(
				"[Wildercord Aura] Stage: %s | Aura: %.0f/%d | Use /wc slash, /wc guard, /wc dash, /wc form, /wc art, /wc unity, /wc clash.",
				stageId, Aura.aura(player), Aura.capacity(player)
			)).withStyle(ChatFormatting.GOLD));
			return;
		}
		CumulusAura.openMain(player);
	}

	public static void openDashAndForms(ServerPlayer player) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusAura.openFormsMenu(player);
		} else {
			triggerDash(player);
		}
	}

	public static void openMastersArts(ServerPlayer player) {
		if (BedrockBridge.isFloodgateLoaded() && BedrockPlayers.isBedrock(player)) {
			CumulusAura.openArtsMenu(player);
		} else {
			MastersArts.activate(player, 0);
		}
	}

	private static final class CumulusAura {
		static void openMain(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			int stage = Aura.stage(player);
			String stageLabel = stage > 0
				? AuraStages.get(stage).map(s -> s.id().toUpperCase()).orElse("Stage " + stage)
				: "Unawakened";
			String method = Aura.method(player).map(BreathingMethod::id).orElse("None");
			String element = Aura.element(player);
			String way = AuraApi.wayOf(player).map(AuraApi.Way::id).orElse("None");
			BondedBlades.State bladeState = BondedBlades.state(player);
			String bladeName = bladeState.bondId() == null ? "Unbonded"
				: (bladeState.shown().name().isEmpty() ? "Bonded Blade" : bladeState.shown().name())
					+ " (" + BladeRules.tierId(bladeState.shown().tier()) + ")";

			String content = String.format(
				"Stage: %s  |  Method: %s%s\nAura Pool: %.0f / %d  |  Coated: %s\nWay: %s  |  Unity: %s\nBlade: %s",
				stageLabel,
				method,
				element.isEmpty() ? "" : " (" + element + ")",
				Aura.aura(player),
				Aura.capacity(player),
				Aura.coated(player) ? "Ready" : "No",
				way,
				Unity.active(player) ? "ACTIVE" : "Inactive",
				bladeName
			);

			SimpleForm form = SimpleForm.builder()
				.title("Wildercord Aura & Knights")
				.content(content)
				.button("Aura Techniques (Slash / Guard / Step / Dominion)")
				.button("Field Forms & Master Forms (Dash / Wall Turn)")
				.button("Master's Arts & Sword Strings")
				.button("Write & Manage Own Techniques")
				.button("Bonded Blade & Lineage")
				.button(Clashes.clashing(player) ? "PRESS CLASH NOW / Unity" : "Unity & Clash Controls")
				.button("Back to Magic Hub")
				.validResultHandler(resp -> server.execute(() -> {
					if (!player.isAlive()) return;
					switch (resp.clickedButtonId()) {
						case 0 -> openTriggersMenu(player);
						case 1 -> openFormsMenu(player);
						case 2 -> openArtsMenu(player);
						case 3 -> openTechniquesMenu(player);
						case 4 -> openBladeAndLineageMenu(player);
						case 5 -> openUnityClashMenu(player);
						default -> BedrockForms.openHub(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openTriggersMenu(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			List<AuraApi.Technique> all = AuraApi.techniques();
			int stage = Aura.stage(player);
			SimpleForm.Builder b = SimpleForm.builder()
				.title("Aura Key Techniques")
				.content(String.format("Aura: %.0f / %d  |  Coated: %s",
					Aura.aura(player), Aura.capacity(player), Aura.coated(player) ? "Yes" : "No"));
			for (AuraApi.Technique t : all) {
				String lock = stage >= t.stage() ? String.format("%.0f aura", t.cost()) : "Stage " + t.stage();
				b.button(t.id().toUpperCase() + " (" + lock + ")");
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < all.size()) {
					Aura.press(player, all.get(idx).trigger());
				} else {
					openMain(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		static void openFormsMenu(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			FormDash.Progress dp = FormDash.data(player);
			MasterForms.Progress mp = MasterForms.data(player);

			String equippedLabel = mp.equipped() == MasterForms.WALL_TURN ? "Wall Turn"
				: mp.equipped() == MasterForms.STONE_HINGE ? "Stone Hinge"
				: dp.equipped() != 0 ? FormDash.name(dp.equipped())
				: "None";

			List<Runnable> actions = new ArrayList<>();
			SimpleForm.Builder b = SimpleForm.builder()
				.title("Field Forms & Master Forms")
				.content("Equipped Active Form: " + equippedLabel);

			b.button("Activate Equipped Form (" + equippedLabel + ")");
			actions.add(() -> triggerDash(player));

			b.button("Unequip Active Form Slot");
			actions.add(() -> {
				sendFormDash(player, WallTurnRules.UNEQUIP, 0);
				sendMasterForm(player, WallTurnRules.UNEQUIP);
			});

			b.button("Trigger Earned Counter (/wc counter)");
			actions.add(() -> triggerCounter(player));

			for (int formId : ALL_DASH_FORMS) {
				if (!dp.knows(formId)) continue;
				String label = FormDash.name(formId);
				if (FormDashRules.slot(formId)) {
					b.button("Equip Field Form: " + label);
					actions.add(() -> {
						if (sendFormDash(player, WallTurnRules.EQUIP, formId)) {
							player.sendSystemMessage(Component.literal("Equipped " + label).withStyle(ChatFormatting.GREEN));
						}
					});
				} else {
					b.button("Trigger Follow-Up: " + label);
					actions.add(() -> sendFormDash(player, WallTurnRules.PRESS, formId));
				}
			}

			if (mp.knows(MasterForms.WALL_TURN)) {
				b.button("Equip Master Form: Wall Turn");
				actions.add(() -> sendMasterForm(player, WallTurnRules.EQUIP));
			}
			if (mp.knows(MasterForms.STONE_HINGE)) {
				b.button("Equip Master Form: Stone Hinge");
				actions.add(() -> sendMasterForm(player, StoneHingeRules.EQUIP));
			}

			// Nearby Duelist teachers offering a form lesson
			for (Duelist teacher : player.level().getEntitiesOfClass(Duelist.class, player.getBoundingBox().inflate(6.0))) {
				if (teacher.isRemoved() || !teacher.isAlive() || teacher.leaving() || teacher.inDuel()) {
					continue;
				}
				int df = FormDashLessons.next(player, teacher);
				if (df == 0) df = FormDashLessons.form(player, teacher);
				if (df != 0 && FormDash.eligible(player, df) && !dp.knows(df)) {
					final int learnId = df;
					final Duelist tRef = teacher;
					b.button("Learn from " + tRef.getName().getString() + ": " + FormDash.name(learnId));
					actions.add(() -> learnDashFromTeacher(player, tRef, learnId));
				}
				int mf = MasterFormLessons.form(teacher);
				if (mf != 0 && MasterForms.eligibleLesson(player, mf) && !mp.knows(mf)) {
					final int learnMf = mf;
					final Duelist tRef = teacher;
					b.button("Learn Master Form from " + tRef.getName().getString() + ": " + MasterFormLessons.key(learnMf));
					actions.add(() -> learnMasterFormFromTeacher(player, tRef, learnMf));
				}
			}

			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx >= 0 && idx < actions.size()) {
					actions.get(idx).run();
				} else {
					openMain(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void learnDashFromTeacher(ServerPlayer player, Duelist teacher, int form) {
			if (teacher.isRemoved() || !teacher.isAlive() || teacher.level() != player.level()
				|| teacher.inDuel() || teacher.leaving() || teacher.distanceToSqr(player) > 36
				|| !teacher.hasLineOfSight(player)) {
				player.sendSystemMessage(Component.literal("Teacher is out of sight or busy.").withStyle(ChatFormatting.RED));
				return;
			}
			if (FormDash.learn(player, form)) {
				if (player.getInventory().getFreeSlot() >= 0) {
					player.getInventory().add(FormDashLessons.book(form));
				}
				player.sendSystemMessage(Component.translatable("message.wildercord.field_form.learned." + FormDash.name(form))
					.withStyle(ChatFormatting.GREEN));
			}
		}

		private static void learnMasterFormFromTeacher(ServerPlayer player, Duelist teacher, int form) {
			if (teacher.isRemoved() || !teacher.isAlive() || teacher.level() != player.level()
				|| teacher.inDuel() || teacher.leaving() || teacher.distanceToSqr(player) > 36
				|| !teacher.hasLineOfSight(player)) {
				player.sendSystemMessage(Component.literal("Teacher is out of sight or busy.").withStyle(ChatFormatting.RED));
				return;
			}
			if (MasterForms.learn(player, form)) {
				if (player.getInventory().getFreeSlot() >= 0) {
					player.getInventory().add(MasterFormLessons.book(form));
				}
				player.sendSystemMessage(Component.translatable("message.wildercord." + MasterFormLessons.key(form) + ".learned")
					.withStyle(ChatFormatting.GREEN));
			}
		}

		static void openArtsMenu(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			List<Runnable> actions = new ArrayList<>();
			SimpleForm.Builder b = SimpleForm.builder()
				.title("Master's Arts & Sword Strings")
				.content(String.format("Hold an Aura weapon to perform.\nAura: %.0f / %d  |  Stage: %d",
					Aura.aura(player), Aura.capacity(player), Aura.stage(player)));

			for (int ord = 0; ord < 3; ord++) {
				MastersArtRules.Move mv = MastersArtRules.move(ord);
				if (mv == null) continue;
				final int moveIdx = ord;
				b.button(String.format("Master's Art: %s (Stg %d, %.0f aura)", mv.id().toUpperCase(), mv.stage(), mv.cost()));
				actions.add(() -> MastersArts.activate(player, moveIdx));
			}

			List<AuraApi.StringArt> arts = AuraApi.stringsOf(player);
			for (AuraApi.StringArt art : arts) {
				boolean rested = SwordStrings.rested(player, art);
				double cost = SwordStrings.price(player, art);
				b.button(String.format("String: %s [%.0f aura%s]",
					art.id(), cost, rested ? "" : " - resting"));
				actions.add(() -> {
					List<Integer> marks = art.string().tokens().stream().map(SwordString.Token::bit).toList();
					SwordStrings.perform(player, art, marks);
				});
			}

			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx >= 0 && idx < actions.size()) {
					actions.get(idx).run();
				} else {
					openMain(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openTechniquesMenu(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			Techniques.Book book = Techniques.book(player);
			int unlockedSlots = Techniques.slots(player);

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Write Own Aura Techniques")
				.content("Unlocked Slots: " + unlockedSlots + " / " + TechniqueRules.MAX_SLOTS
					+ "\nKnown Parts: " + Techniques.known(player).size());

			for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
				Techniques.Written w = book.slot(i);
				String label = w.empty() ? "(Empty)" : w.shownName() + " [" + w.stroke() + " / " + w.release() + " / " + w.intent() + "]";
				b.button("Slot " + (i + 1) + (i < unlockedSlots ? "" : " [Locked]") + ": " + label);
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < TechniqueRules.MAX_SLOTS) {
					openTechniqueSlotMenu(player, idx);
				} else {
					openMain(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openTechniqueSlotMenu(ServerPlayer player, int slot) {
			MinecraftServer server = player.level().getServer();
			Techniques.Book book = Techniques.book(player);
			Techniques.Written w = book.slot(slot);
			String info;
			if (w.empty()) {
				info = "Slot " + (slot + 1) + " is empty.";
			} else {
				Techniques.Honing h = book.honing(w.key());
				info = String.format("Slot %d: %s\nStroke: %s | Release: %s | Intent: %s\nString: %s\nRank: %d (%.0f xp) | Temper: %s | Edge: %s",
					slot + 1, w.shownName(), w.stroke(), w.release(), w.intent(), w.string(),
					h.rank(), h.xp(), h.temper().isEmpty() ? "None" : h.temper(), h.edge().isEmpty() ? "None" : h.edge());
			}

			SimpleForm.Builder b = SimpleForm.builder()
				.title("Technique Slot " + (slot + 1))
				.content(info)
				.button("Write / Rewrite Technique");
			if (!w.empty()) {
				b.button("Choose Temper / Edge (Honed / Keen)");
				b.button("Inscribe Part onto Scroll (Peerless)");
				b.button("Erase Slot");
			}
			b.button("Back");

			b.validResultHandler(resp -> server.execute(() -> {
				int id = resp.clickedButtonId();
				if (id == 0) {
					openTechniqueWriterForm(player, slot);
				} else if (!w.empty() && id == 1) {
					openTechniqueRefineForm(player, slot);
				} else if (!w.empty() && id == 2) {
					openTechniqueInscribeForm(player, slot);
				} else if (!w.empty() && id == 3) {
					Techniques.erase(player, slot);
					openTechniquesMenu(player);
				} else {
					openTechniquesMenu(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openTechniqueWriterForm(ServerPlayer player, int slot) {
			MinecraftServer server = player.level().getServer();
			Techniques.Written existing = Techniques.book(player).slot(slot);

			List<String> strokes = knownOfFamily(player, TechniqueRules.Family.STROKE);
			List<String> releases = knownOfFamily(player, TechniqueRules.Family.RELEASE);
			List<String> intents = knownOfFamily(player, TechniqueRules.Family.INTENT);

			if (strokes.isEmpty() || releases.isEmpty() || intents.isEmpty()) {
				player.sendSystemMessage(Component.literal("Unlock Aura Edge stage and technique parts first.").withStyle(ChatFormatting.YELLOW));
				return;
			}

			int defStroke = Math.max(0, strokes.indexOf(existing.stroke()));
			int defRel = Math.max(0, releases.indexOf(existing.release()));
			int defIntent = Math.max(0, intents.indexOf(existing.intent()));
			String defStr = existing.string().isEmpty() ? "swing swing" : existing.string();

			CustomForm form = CustomForm.builder()
				.title("Write Technique Slot " + (slot + 1))
				.input("Technique Name (blank = auto)", "My Technique", existing.name())
				.dropdown("Stroke", strokes, defStroke)
				.dropdown("Release", releases, defRel)
				.dropdown("Intent", intents, defIntent)
				.input("Sword String (tokens: swing, full, low, leap, run, counter, step)", "swing swing", defStr)
				.validResultHandler(resp -> server.execute(() -> {
					String name = resp.asInput(0);
					String st = strokes.get(resp.asDropdown(1));
					String rl = releases.get(resp.asDropdown(2));
					String in = intents.get(resp.asDropdown(3));
					String sw = resp.asInput(4);
					Techniques.write(player, new Techniques.Write(
						slot,
						name == null ? "" : name,
						st, rl, in,
						(sw == null || sw.isBlank()) ? "swing swing" : sw.trim()
					));
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openTechniqueRefineForm(ServerPlayer player, int slot) {
			MinecraftServer server = player.level().getServer();
			Techniques.Book book = Techniques.book(player);
			Techniques.Written w = book.slot(slot);
			Techniques.Honing h = book.honing(w.key());

			List<String> tempers = new ArrayList<>();
			tempers.add("(As written)");
			tempers.addAll(TechniqueRules.TEMPERS);
			List<String> edges = new ArrayList<>();
			edges.add("(As written)");
			edges.addAll(TechniqueRules.EDGES);

			int defT = h.temper().isEmpty() ? 0 : Math.max(0, tempers.indexOf(h.temper()));
			int defE = h.edge().isEmpty() ? 0 : Math.max(0, edges.indexOf(h.edge()));

			CustomForm form = CustomForm.builder()
				.title("Temper & Edge: " + w.shownName())
				.dropdown("Temper (Honed rank+)", tempers, defT)
				.dropdown("Edge (Keen rank+)", edges, defE)
				.validResultHandler(resp -> server.execute(() -> {
					int tIdx = resp.asDropdown(0);
					int eIdx = resp.asDropdown(1);
					String tVal = tIdx <= 0 ? "" : tempers.get(tIdx);
					String eVal = eIdx <= 0 ? "" : edges.get(eIdx);
					if (!tVal.equals(h.temper())) {
						Techniques.choose(player, slot, false, tVal);
					}
					if (!eVal.equals(h.edge())) {
						Techniques.choose(player, slot, true, eVal);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openTechniqueInscribeForm(ServerPlayer player, int slot) {
			MinecraftServer server = player.level().getServer();
			Techniques.Written w = Techniques.book(player).slot(slot);
			SimpleForm form = SimpleForm.builder()
				.title("Inscribe Part (" + w.shownName() + ")")
				.content("Needs Peerless rank on this technique and Paper/Scrolls.")
				.button("Stroke: " + w.stroke())
				.button("Release: " + w.release())
				.button("Intent: " + w.intent())
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					switch (resp.clickedButtonId()) {
						case 0 -> Techniques.inscribe(player, slot, w.stroke());
						case 1 -> Techniques.inscribe(player, slot, w.release());
						case 2 -> Techniques.inscribe(player, slot, w.intent());
						default -> openTechniqueSlotMenu(player, slot);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static List<String> knownOfFamily(ServerPlayer player, TechniqueRules.Family family) {
			List<String> out = new ArrayList<>();
			for (String id : TechniqueRules.parts(family)) {
				if (Techniques.knows(player, id)) {
					out.add(id);
				}
			}
			return out;
		}

		private static void openBladeAndLineageMenu(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			BondedBlades.State bs = BondedBlades.state(player);
			Lineage.View lin = Lineage.view(player);

			StringBuilder info = new StringBuilder();
			if (bs.bondId() == null) {
				info.append("Bonded Blade: None bonded.\n");
			} else {
				ItemStack blade = BondedBlades.carried(player);
				BladeBond bb = BondedBlades.bond(blade);
				info.append("Bonded Blade: ")
					.append(bs.shown().name().isEmpty() ? "Unnamed" : bs.shown().name())
					.append(" [").append(BladeRules.tierId(bs.shown().tier())).append("]\n");
				if (bb != null && !bb.growth().trait().isEmpty()) {
					info.append("Trait: ").append(bb.growth().trait()).append('\n');
				}
			}
			lin.master().ifPresent(m -> info.append("Master: ").append(m.name()).append(" (Stage ").append(m.stage()).append(")\n"));
			info.append("Disciples: ").append(lin.disciples().size()).append(" / ").append(lin.most());

			List<Runnable> actions = new ArrayList<>();
			SimpleForm.Builder b = SimpleForm.builder()
				.title("Bonded Blade & Lineage")
				.content(info.toString());

			if (bs.bondId() == null) {
				b.button("Bond Held Weapon (Main Hand)");
				actions.add(() -> {
					String refuse = BondedBlades.bondRefusal(player);
					if (refuse != null) {
						player.sendOverlayMessage(Component.translatable(refuse).withColor(0xA89CC8));
					} else {
						BondedBlades.bond(player, InteractionHand.MAIN_HAND, "bedrock");
					}
				});
			} else {
				b.button("Rename Bonded Blade (Named+)");
				actions.add(() -> openRenameBladeForm(player));

				ItemStack blade = BondedBlades.carried(player);
				BladeBond bb = BondedBlades.bond(blade);
				if (bb != null && !bb.growth().offer().isEmpty()) {
					b.button("Choose Awakened Blade Trait");
					actions.add(() -> openBladeTraitForm(player, bb.growth().offer()));
				}

				b.button("Release Blade Bond (Confirm)");
				actions.add(() -> openConfirmReleaseBlade(player));
			}

			lin.master().ifPresent(m -> {
				b.button("End Bond with Master (" + m.name() + ")");
				actions.add(() -> {
					Lineage.end(player, m.id());
					openMain(player);
				});
			});
			for (Lineage.Entry d : lin.disciples()) {
				b.button("Dismiss Disciple (" + d.name() + ")");
				actions.add(() -> {
					Lineage.end(player, d.id());
					openMain(player);
				});
			}

			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx >= 0 && idx < actions.size()) {
					actions.get(idx).run();
				} else {
					openMain(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openRenameBladeForm(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			CustomForm form = CustomForm.builder()
				.title("Name Bonded Blade")
				.input("New Blade Name", "Dawnbreaker")
				.validResultHandler(resp -> server.execute(() -> {
					String name = resp.asInput(0);
					if (name != null && !name.isBlank()) {
						BondedBlades.rename(player, name);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openBladeTraitForm(ServerPlayer player, List<String> offered) {
			MinecraftServer server = player.level().getServer();
			SimpleForm.Builder b = SimpleForm.builder()
				.title("Choose Blade Trait")
				.content("Select one offered trait for your Awakened blade:");
			for (String t : offered) {
				b.button(t.replace('_', ' ').toUpperCase());
			}
			b.button("Back");
			b.validResultHandler(resp -> server.execute(() -> {
				int idx = resp.clickedButtonId();
				if (idx < offered.size()) {
					BondedBlades.choose(player, offered.get(idx));
				} else {
					openBladeAndLineageMenu(player);
				}
			}));
			FloodgateApi.getInstance().sendForm(player.getUUID(), b.build());
		}

		private static void openConfirmReleaseBlade(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			SimpleForm form = SimpleForm.builder()
				.title("Release Bonded Blade?")
				.content("Releasing your bond turns the blade back into ordinary steel.")
				.button("Yes, Release Bond")
				.button("Cancel")
				.validResultHandler(resp -> server.execute(() -> {
					if (resp.clickedButtonId() == 0) {
						BondedBlades.release(player);
					} else {
						openBladeAndLineageMenu(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}

		private static void openUnityClashMenu(ServerPlayer player) {
			MinecraftServer server = player.level().getServer();
			boolean clashing = Clashes.clashing(player);
			boolean unityOn = Unity.active(player);
			SimpleForm form = SimpleForm.builder()
				.title("Unity & Sword Clashes")
				.content("Unity: " + (unityOn ? "ACTIVE" : "Ready / Not Active")
					+ "\nActive Sword Clash: " + (clashing ? "YES — press Clash now!" : "None"))
				.button("Activate Unity")
				.button("Press Sword Clash Beat")
				.button("Back")
				.validResultHandler(resp -> server.execute(() -> {
					switch (resp.clickedButtonId()) {
						case 0 -> triggerUnity(player);
						case 1 -> Clashes.pressFor(player);
						default -> openMain(player);
					}
				}))
				.build();
			FloodgateApi.getInstance().sendForm(player.getUUID(), form);
		}
	}
}
