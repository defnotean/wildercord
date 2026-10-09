# Combat presentation

Open **Magic visual settings** from the Grimoire's Life journal, or bind that
action in Minecraft's Controls menu. Choose **Combat presentation…** to see the
local animation and camera choices. This page is newly implemented; its native
UI and compatibility checks must pass before release acceptance.

**Classic** is the default. **Articulated** is an experimental opt-in for selected
sword arts and Master moves, with original bent elbows, knees and hand poses.
It does not animate every form. The choice affects this client's presentation;
server damage, input, movement, prices and attack timing do not change.

Changes stay in the page until **Apply** succeeds. Back and Escape discard the
draft. Preferences survive a restart. Reset stages **Classic + Stable camera**
and still requires Apply; the existing performance/balanced/cinematic presets
leave these combat choices alone. Opening the page does not change an existing
player's camera. The first explicit choice offers Stable camera.

## Camera and accessibility

**Stable** suppresses view/hurt bob, Wildercord camera shakes, impact nudges and
FOV punches. Mouse look stays free. It remains selected with Classic or an
equipment fallback. **Default** follows Minecraft's camera options and the
existing Magic visuals camera-motion setting.

For less motion, combine Stable with Impact Off, Calm/Off body aura and
Subtle/Off trails in Magic visuals. Reduced flash now removes the funded Aura
Armour shell's pulse and white hit brightening on both rendering backends. It
also softens selected existing effects; it does not make every effect flash-free,
and Impact Off can still leave a small visual flash.

## Supported equipment and fallbacks

The limited profile supports bare skin or the reviewed vanilla netherite pieces,
including supported trim/enchantment passes and normally funded Aura Armour.
Other armor, an occupied offhand, capes, unknown renderer layers or unsupported
poses retain the complete existing body, item, armor and shell presentation.
An unsupported state does not change the saved camera choice. Shader/resource
packs and real multiplayer performance still require compatibility acceptance.

Launch arguments take priority over the saved switches. The help page lists only
the known active switches and their parsed true/false/invalid status. An explicit
false wins too. Apply and Reset cannot remove a launch argument; remove it from
the launcher if you want the in-game choice to control that option.

Malformed or newer-version preferences are reported in the page and preserved.
A failed save keeps the prior live choice and leaves the draft available to
retry. Unknown preference fields are retained when a supported file is saved.
