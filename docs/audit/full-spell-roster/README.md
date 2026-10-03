# Full spell presentation audit

This is an evidence index for milestone B, not a claim that every spell has unique completed art.

## Runtime inventory

The built-in runtime registry contains 350 runes: 258 effects, 39 shapes, 36 modifiers and 17 links. All 258 effects have registered signatures. The signature registry contains 17 phase hooks and 182 cue sound entries. Visuals and sounds also dispatch through other source files; absent registry hooks do not establish missing presentation.

- [Runtime JSON](runtime-roster.json)
- [Runtime table](runtime-roster.md)
- [Textual source references](source-references.json)

Regenerate the runtime reports with `./gradlew spellPresentationAudit`. Source references are textual candidates, not confirmed ownership or verification. Each runtime row remains pending native review. Dynamic Knots/Woven runes and Aura techniques require separate inventories; rank, modifier, fusion, link, quality, shader and multiplayer combinations also remain in scope.

## Caster-centered formation correction

Self, Domain, Orbit and Trail elemental layers now use the caster anchor instead of the shared front focus. Orbit satellites circle the caster; Time and Void material branches use the selected anchor. Forward deliveries retain their front formation focus. Attached casting circles remain behind the caster.

FormationAnchorTest passed 18 native packet cases: six deliveries (Self, Domain, Orbit, Trail, Bolt, Beam), three aim pitches, ten elemental material layers. It checks actual emitted client particles and rear-circle placement. Three screenshots show the diagnostic rear camera; that camera places the rear circle between the camera and caster, so these are positional evidence, not representative first-person beauty captures.

The suite does not prove complete mana-paid casts, unique effect choreography, all 39 deliveries, launch timing, fusion behavior, shaders or real multiplayer. No spell mechanic, mana cost or timing changed in this correction.

## Next review steps

1. Confirm source ownership of mechanics and presentation, including dispatch outside Signatures.
2. Record formation, launch, travel, impact, aftermath and sound evidence per effect.
3. Exercise all deliveries with actual casts, prioritizing caster-centered, ground, delayed and linked releases.
4. Author effect-specific choreography and fused material interactions where the current presentation is shared.
5. Review secret/dynamic spells and Aura separately; Aura must not draw magic circles.
6. Verify reduced effects, shaders, multiplayer readability and measured performance.

The full living-world objective remains active. This increment adds no creature, item, ability, fusion or lore count.
