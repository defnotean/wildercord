# Authored fire formation sequences

This increment changes the client formation phase, preserving existing server deliveries, mechanics, impact displays and voices. Each recipe emits original material sprites or narrow shaped light through the existing bounded canvas. Full and Minimal modes preserve the recipe silhouette; Minimal removes midpoint detail. The second beat tightens the assembly and rune-specific moving points continue their own preparation.

The source audit found existing per-rune impact/aftermath displays in FireBloodVfx and supporting dispatch classes, while SpellFormations still used a generic element layer for fire. This addresses that formation gap. It does not establish unique travel/launch for every shape/effect pairing.

| Rune | Authored preparation |
|---|---|
| Ember | Hooked spark closes onto a single bright cinder. |
| Fire | Three climbing flame tongues lean toward release. |
| Flashfire | Opposed shutter edges compress around a white slit. |
| Explode | Four sparks converge on a pressure heart. |
| Meteor | Stone fragments weld beside a descending diagonal fire tail. |
| Inferno | Serrated flame crown closes its teeth. |
| Primer | Pink hooked fuse shortens toward a suspended charge. |
| Kindling | Crossed splinters catch at their intersection. |
| Firestorm | Wind ribbon helix braids around a flame column. |
| Steam | Wet lobes close, venting vapor through their seam. |
| Sunscorch | Six short solar rays fold around a bright core. |
| Soulfire | Blue flame twist opposes a contracting dark wake. |
| Blazecall | Three hooked tongues gather into a flame spear. |
| Cinderbrand | Angular open brand closes around a hot central mark. |
| Ashen Veil | Two ash curtains flank an open aim line. |
| Cinderheart | Paired ember lobes close into a bright heart. |
| Searing Edge | A thin hot blade develops an ember bevel. |
| Fireward | Shield shoulders join at a lower point. |
| Smelt | Heated grate gathers a descending metallic droplet. |
| Hellmouth | Black jaw opens between opposed hot teeth. |
| Starfire | Five hot points draw a crossed pink star. |
| Everburn | Flame passes through a broken hourglass. |
| Conflagration | Five flame fronts gather for a white ignition. |
| Phoenix Pyre | Feathered flame wings lift beside a green renewal stem. |
| Seethe | Heated water beads lift into vapor. |
| Skyburst | High fire comb prepares three falling lanes. |
| Cinder Bulwark | Staggered stone courses join with ember mortar. |
| Boiling Surge | Wet curling crest carries hot vapor on its leading edge. |

Bloodboil is also authored as rising blood drops between heated needles. Its runtime element is blood, so it is not counted among the 28 fire effects.

Fire recipes dispatch only for exact built-in namespace IDs. Multiple authored effects retain their separate preparations; non-fire supporting element layers remain. These are decorative client emissions, with no terrain editing, damage, new particles/assets, sound events or server tasks. Existing own/other particle budgets and event bounds still apply.

## Delivery and verification limits

The new preparations use the existing caster anchor for Self/Domain/Orbit/Trail and front focus for other shapes. Rain/ground/summon target-aware staging and Burst/Nova caster-versus-linked origin need further review; the compact formation packet currently has no target point. Those delivery requirements remain open.

FireFormationTest exercises 28 fire recipes through encoded native packets in Full and Minimal modes, compares finite nonduplicate recipe emissions for all 29 authored IDs and checks evolving beats, and performs a Survival Ember Cord cast with mana payment. Native packet screenshots show the formation fixture, not every full effect/delivery combination. Per-effect launch, travel, impact, sound, first/third-person and shader/multiplayer review remains incomplete.

## Later initial-group staging correction

Ordinary initial groups now choose caster or aimed-terrain placement separately. Burst/Nova/Ring/Wave join caster modes; Zone/Rain/Wall/Pillar/Mine/Totem/Vortex use current aim. Rain has a cloud-height anchor and ground marker. Fire recipes follow each group's anchor and its isolated effect list. The earlier anchor limitation above is historical for these ordinary initial groups. Linked continuation, secret-specific and remaining shape staging still require work.
