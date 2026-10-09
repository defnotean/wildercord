# Funded Aura Armour shell adapter

This original client-only preview lets the articulated player body retain normal automatic Aura Armour at stage four and above. It does not lower aura, clear the shell, alter payments or server windows, change mitigation, or copy external animation or texture assets. It is separate from the exact-cost shell-down native baseline.

The complete articulated backend remains **off by default** (`wildercord.articulated`). Inside that opt-in backend, `wildercord.articulated.auraShell` defaults to `true`; setting it to `false` restores the entire previous body, item and shell path for a funded player. Supported netherite armor and camera-space armor remain subject to their existing independent switches. All other compatibility gates remain unchanged, including unknown layers, unsupported equipment, afterimages, posture, locomotion and occupied offhand.

## Ownership and material contract

- Each exact primary `PlayerModel` gets one reload-owned shell adapter from its one exact `AuraShellLayer`. Extraction retains that adapter only if it belongs to the renderer's actual primary body and the layer occurs exactly once. Subclasses, absent adapters and changed geometry keep the complete fallback.
- The adapter validates the entire runtime shell bake against the authored standard/slim shell, then uses only the six original body surfaces. The original .55-pixel stand-off and 64×64 UV mapping are preserved. The existing original `ArticulatedArmorMesh` subdivides exterior quads around the accepted rig's joints, with welded seams and no internal caps. Its explicit shell path adds the existing sleeves' .25-pixel cap projection only inside bent arm collars; that guard vanishes at bind and the rigid ends. Ordinary armor deformation is unchanged. Skin second layers remain excluded exactly as in the original shell.
- After applying the final body pose (including baseline arms and free head look), world submission captures a defensive immutable palette. It backs out no gameplay state. First-person shell geometry contains only both arms and captures the final camera-space rig after the skin setup, alongside the existing armor and sword socket path.
- Shell submission stays at order 1 with the exact original standard/slim eyes material, full-bright light, original per-frame ARGB, no overlay, null UV remapping and outline zero. The shell's color/alpha are integer values on the queued submission, so later player extraction cannot replace them. No rigid shell is submitted when the articulated shell owns that frame.
- The adapter owns only renderer-local CPU geometry and immutable pending state. Resource reload reconstructs it. No runtime texture, attachment or server-rule definition is changed.

The cached official 26.3 `ModelFeatureRenderer.prepareModel` invokes each queued model's `setupAnim` with that submission's stored state immediately before drawing. Fabric rendering 27.0.14+901a437c5d's `EntityRenderersMixin.createAvatarRenderer` invokes the layer-registration event after the `AvatarRenderer` constructor finishes; the existing constructor-tail primary-body install therefore precedes shell adapter construction.

## Validation performed

Frozen verification tag: `funded-aura-shell-final-20261005-2148`, using the reconstructed independent verifier and official cached JDK 25.0.4.1+1.

- All 780 main, 252 client, 142 test and 329 game-test Java sources compiled against the verified original 26.3 APIs and Fabric metadata. The verifier reported identical source hashes before/after.
- All 1,245 JUnit tests passed, including new shell topology and deformation checks for every original player art, both hands and both skin widths.
- `CheckArticulatedAuraShell` passed against pristine publisher Minecraft jars: 3,992 world/view pose snapshots and 40 rejected source variants. Checks include original affine UV samples, six-body/two-arm surface masks, wrong-width/missing/posed/altered-UV rejection, finite normals/vertices and bit-exact A/B/A replay after both rig and caller-matrix mutation.
- `CheckArticulatedAuraShellClearance` samples the actual original skin and all skin overlays against the shell at 1/8-tick intervals in world and view poses. All 4,742,496 surface samples remained inside; all 1,213,568 sampled triangles faced outward. This specifically guards the closed sleeve caps that the thinner shell must cover.
- All 23 Python client-suite tests passed. The existing `articulated` suite now contains four native entries; no extra CI job was added.

Evidence under `/workspace/shared/wildercord_compile_verification/runs/funded-aura-shell-final-20261005-2148/`: `report.json`, `source-before.json`, `source-after.json`, `junit.log`, both shell helper logs and the exact helper invocation JSON files. These are local verification artifacts, not published results.

Independent read-only review found no remaining blocking source issue after the shell-specific sleeve-cap correction and strict cancellation assertions. The reviewer independently recompiled the corrected classes and reproduced the zero-escape/zero-reversal containment sweep plus the original UV/palette checks. This review does not replace native acceptance.

## Authored native acceptance, not executed

`ArticulatedAuraShellPresentationTest` is registered in the existing articulated suite. It creates ordinary stage-four/stone and stage-five/gale players with 100 aura, uses real rebindable key input, verifies accepted payment and automatic server/client shell state, and captures all three supported player arts with both dominant hands, skin or full Protection IV gold-trimmed netherite, and front/back/first-person cameras. It checks shell/body/armor ownership through available simulation ticks, expiry and cancellation, real reload reconstruction, the both-hand first-person submission path, and the complete rigid fallback when the adapter is disabled.

Separate clearly labelled synthetic probes compare every shell material argument with the original rigid shell, alternate extracted states with different poses/colors/armor, replay deferred A/B/A geometry, and exercise unknown layer, unsupported armor, offhand, afterimage and missing-owner fallbacks. No injected synthetic state is used for the funded screenshots. Screenshot names/logs report the pre-capture phase only; exact rendered phase and impact-pixel coverage require frame review.

Required native review still includes:

- no naked frame, duplicated rigid shell, detached shell or mismatched hand at attack edges;
- standard and slim player skins in actual client captures (the current account's actual skin is logged, never inferred from the test label);
- shell alpha/color/flare, enchanted armor/trim and transparency in front/back/first-person views;
- first-person near-plane/crosshair/HUD clearance across supported sizes and free-look bounds;
- two real funded players alternating in the same scene, including style/resource/armor changes and hit-stop;
- supported texture packs, resource reload and shader/shadow-pass behavior.

**Native execution, native pixel review, Loom execution and renderer promotion have not passed in this patch.** The preview stays default-off until that acceptance is performed. Offline pose/geometry checks and authored test registration do not establish native visual quality.
