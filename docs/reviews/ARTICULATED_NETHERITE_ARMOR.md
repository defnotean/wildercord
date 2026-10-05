# Original articulated netherite armor preview

This is a bounded follow-on to the original articulated body proof. It is local authoring work until its own native gate passes. The unarmored proof's native acceptance is separate and must also pass before any integration/default-on decision. No armor stats, hitboxes, item behavior, server timing, damage, movement authority or camera projection is changed.

## Development switches

The existing `-Dwildercord.articulated=true` still defaults off. Armored ownership additionally requires `-Dwildercord.articulated.armor=true`, also default off. The optional enhanced first-person chestplate arms additionally require `-Dwildercord.articulated.armorArms=true`, again default off.

Only exact vanilla netherite helmet, chestplate, leggings and boots in their corresponding slots are accepted. The actual `EQUIPPABLE` component must match both slot and netherite asset, with no custom camera overlay. Any subset is allowed. Enchantment and trim components are preserved. Empty slots do not need an armor preview switch. Other armor, altered equipment assets, modded gear, unknown render layers, capes, wings, invisibility and the previous unsupported poses/features retain the complete previous renderer. The system never removes an unsupported piece to claim compatibility.

Vanilla first-person player rendering draws skin and sleeves without chestplate geometry. The optional armor-arms switch intentionally enhances that presentation. It draws only the chestplate's original arm surfaces over the existing view arms, with outline zero and the unchanged native 70-degree hand projection. Netherite's transparent lower-arm texels stay transparent; there are no invented forearm gauntlets or gloves.

## Exactly one owner

`PlayerModel` no longer creates a full skin rig in every constructor. The actual primary model returned by `AvatarRenderer.getModel()` receives the explicit body-owned marker. The four vanilla armor `PlayerModel` instances remain unowned and never gain skin geometry or articulated item sockets. Tests that deliberately create a stand-in primary body must explicitly use that same ownership API.

At renderer construction, exactly one exact vanilla `HumanoidArmorLayer` is replaced in its existing list position. The adapter retains the original instance and dispatches either to it or to the articulated equipment path, never both. Unknown armor subclasses, duplicate armor layers and unsupported baked geometry disable the adapter. Extraction verifies readiness before the body can hide its rigid parts. Exact known render-layer classes replace broad package-prefix acceptance. Resource reload rebuilds renderer-local CPU geometry and material-renderer references.

## Geometry and materials

Fresh runtime `ModelLayers.PLAYER_ARMOR`/`PLAYER_SLIM_ARMOR` slot roots supply vanilla geometry and UVs without constructing armor `PlayerModel`s. Armor uses the original 64x32 UV layout and four-pixel arm widths even for slim skins. Only original exterior surfaces are subdivided around torso, elbow, wrist, knee and ankle transitions. Shared boundary points and narrow joint bands replace independently inflated internal cube caps; no internal caps, extra shell or repeated trim coverage is added. Runtime textures are referenced through Minecraft; the mod includes no Minecraft or third-party animation assets.

The dedicated `Model<Palette>` snapshots final resolved joint matrices after the body has incorporated the actual vanilla held-item/breathing baseline and bounded free-look head corrections. It uses immutable defensive matrix copies, captures vanilla helmet outer-hat visibility, and applies the captured state again during deferred model rendering. The primary root transform is applied at submission. No live entity, model part, mutable matrix or material cache is stored in a submitted palette. Each equipment stack is copied for material selection at the current submission, so equip changes and breakage stay live during cosmetic hit-stop.

All base, dye, trim, decal, glint and outline submissions go through the stock `EquipmentLayerRenderer.renderLayers`. Leggings use `HUMANOID_LEGGINGS`; other slots use `HUMANOID`. The stock renderer owns the texture lookup, dye tint, trim palette override, UV mapping, glint ordering, light and outline behavior. Every pass receives the same geometry model and immutable palette. No custom approximation of enchanted armor materials is introduced.

## Gate and remaining evidence

The dedicated fixture is `dev.wildercord.client.combat.ArticulatedArmorPresentationTest`. It is authored separately from the existing unarmored fixture. The lead owns descriptor, catalog and CI integration; merely compiling this file does not register or execute it.

Required source/offline checks include immutable palette isolation, source UV preservation, welded topology, slot masks, strict geometry rejection, finite deformations, partial/full armor eligibility and camera clearance. Required native checks include accepted Spellcut wearing Protection IV netherite, contrasting and netherite-on-netherite trims, real material submission order, both hands and skin widths, arm-only first person, equip/break/swap during hit-stop, resource reload, outline/invisibility/cape fallback, visible skin/armor seams, no added tears or poke-through, and exactly one submission owner.

Native rendering, mixin application, framebuffer/material correctness, skin enclosure and real play acceptance remain pending until the fixture and screenshot review run successfully. Source compilation and offline geometry evidence do not close those gates. Shader compatibility, custom equipment geometry, capes, other armor families and new animation clips are outside this slice.

## Frozen local source gate: 2026-10-05 13:02 UTC

The source snapshot includes the independently reviewed slim-grip correction (`f0c6b00f`, reapplied locally as `474669a1`) before this armor delta. Its pristine `PlayerModel.translateToHand` regression still passes 288 cases, with maximum error 0.000000617 blocks under the unchanged 0.001-block seam tolerance.

Full Java 25 verifier `armor-netherite-20261005-1302` completed at 13:02:44 UTC:

- All 757 main, 246 client, 127 unit-test and 293 native-test source files compile.
- All 1,145 JUnit tests pass, with zero skipped, aborted or failed. Five new pure tests cover armor topology, UV coverage, welded edge ownership, immutable outputs, pose ordering and invalid-input rejection.
- All 3,350 source/configuration fingerprints remain unchanged throughout the gate.
- No Loom launch, mixin runtime, native armor fixture or game screenshot ran in this gate.

The independently reproducible CPU geometry audit (`python3 tools/check_articulated_armor.py --out <fresh-output-directory> --verification <verified-cache>`) compiles the actual production mesh and tests the pristine runtime slot bakes. It records source and dependency hashes:

- 712 source-derived bind faces preserve positions, normals, original UV coverage and mirrored winding.
- 1,727,568 sampled skin, sleeve, jacket and pants points have zero armor protrusions. This includes transparent armor regions; transparency is not used to excuse enclosure failures.
- 603,720 samples of overlapping inner/outer equipment shells remain enclosed, with a minimum sampled separation of 0.17529 model pixels.
- 454,080 sampled deformed triangles have positive orientation relative to their resolved source surface. The minimum orientation cosine is 0.07263, so shallow inside-bend surfaces still need native silhouette review.
- The largest localized collar correction relative to ordinary linear skinning is 0.80749 model pixels. It changes no bind-pose position or texture coordinate and does not add a second surface pass.
- 41,796 optional first-person armor placements across both widths/hands and a 9x9 free-look grid stay behind the 0.05-block near plane; the minimum margin is 0.27035 blocks.
- In 516 canonical-aim first-person frames at the actual 70-degree hand projection, alpha-tested armor adds no coverage at the crosshair or sampled central rectangle. These are armor-only samples; native skin/sword/material composition is not established by this check.

All geometry checks use eighth-tick samples and finite surface grids. They do not prove every continuous pose, arbitrary modded palette, nonadjacent triangle collision, opposite-arm intersection or GPU precision behavior. Source-driven front/side/back and first-person contact sheets have been inspected separately; they are explicitly offline previews, omit native trim/glint and are not evidence of a native pass.

The native fixture compiles with primary ownership, full/partial Protection IV netherite, original vanilla material receipt parity (including netherite-on-netherite, dyed/undyed leather and a synthetic decal probe), final free-look/baseline matrices, per-frame helmet-hat visibility, deferred A/B/A replay, equipment changes during hit-stop, fallbacks and resource reload. Its live capture loops cover both hands, full/partial armor, first person and third-person front/back, using the test account's actual skin width. Wide/slim coverage comes from native baked-model probes, not a false claim of two different live account skins. The class still requires the lead's dedicated descriptor/CI registration and native execution. This source gate does not authorize integration or default-on.

## Independent review corrections: 2026-10-05 13:19 UTC

Independent review found two fail-closed gaps in the first frozen armor commit. Individual cubes were validated without requiring a complete expected slot inventory, and an unchanged netherite asset ID could resolve to a resource-pack definition that adds rigid wings. Both paths now preserve the complete previous renderer:

- Every geometry constructor requires its intended equipment slot. The complete exact cube-bearing path inventory and slot-specific deformation are checked before applying an arm-only filter. Empty roots, missing arms/body/helmet hat, wrong-slot meshes, extra regions and wrong slot inflation are rejected. This is separate from valid empty equipment slots at runtime.
- Readiness and submissions inspect the actual reload-owned `EquipmentAssetManager`. Stock humanoid, leggings, baby, horse and nautilus entries remain valid. A nonempty wings or unknown layer type, or missing humanoid/leggings materials, disables articulated ownership. Texture/dye/trim replacements inside supported layer types remain valid.
- A shared pristine-runtime/native-fixture runner executes 99 positive and negative input cases. The native fixture additionally checks actual render-state extraction against a synthetic wing-bearing asset manager without changing game resources. Native execution remains pending.

Full frozen verifier `armor-reviewed-20261005-1319` passed at 13:19:46 UTC: 757 main, 247 client, 127 unit-test and 294 native-test sources compile; all 1,145 JUnit tests pass with no skips or failures; all 3,352 source/configuration fingerprints are unchanged. The complete offline geometry, overlap, orientation and camera audit was rerun successfully with the 99 input checks. Deformation, UV output and the earlier geometric metrics are unchanged. The source-driven sheets were regenerated with the current constructor contract. Exact-hash independent rereview is the next gate; no native pass, registration, integration or default-on is claimed.
