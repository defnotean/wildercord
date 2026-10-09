---
title: Combat Presentation
parent: Sword Masters
nav_order: 6
---

# Combat Presentation

## What it is

Combat presentation is a small page of personal display choices for sword combat: how bodies and arms animate, and whether the camera shakes. It only changes what **you** see. Damage, movement, costs and timing are the same whatever you pick, and other players never see your choice.

## How to get it

Open **Magic visual settings**, then click **Combat presentation...**. There are two ways in:

- Give the **Magic visual settings** key a binding under **Wildercord** in Minecraft's Controls. It has no key by default.
- Use the settings link in the Grimoire's Life journal on the Cord screen.

Choices are saved on your computer in `config/wildercord-visuals.json`.

## How to use it

The page has two switches. Nothing changes until you press **Apply choices**. Opening the page or pressing Back changes nothing.

| Setting | Options | Default |
|---|---|---|
| **Combat animations** | Classic, Articulated (experimental) | Classic |
| **Camera** | Stable, Default | Your existing camera until your first Apply. The page then offers Stable. |

**Classic** keeps Wildercord's normal animations and effects.

**Articulated (experimental)** is an opt-in look for selected sword arts and Master attacks, with bending elbows, knees and wrists and separately drawn first-person arms. It does not animate every art. It works with bare skin or a matching set of vanilla netherite armor, including a funded Aura Armour shell. With other armor, something in your offhand, a cape, extra skin layers or an unsupported pose, you see the whole Classic look instead. Visual mods and shaders are not checked.

**Stable** turns off view bob, hurt bob, Wildercord screen shake, impact nudges and field-of-view punches. Mouse look and your aim stay free. It works with Classic or Articulated. **Default** follows Minecraft's own options and the Camera motion setting in Magic visuals.

**Reset draft** sets Classic + Stable. You still need to press Apply.

**Support, camera and accessibility** opens a short help book with the same information.

## Tips

- For less motion, pick **Stable**, then in Magic visuals set Impact Off, a calm or off body aura, subtle or off trails, and turn on Reduced flash. Even then, not every effect is guaranteed flash-free.
- If a switch is greyed out, the game was started with a launch option that overrides it. The page says so and lists the active options on the last help page. The options are `wildercord.articulated`, `.stableCamera`, `.armor`, `.armorArms` and `.auraShell`, set to `true` or `false`. Reset cannot remove them; change your launcher's arguments instead.
- If the page says your saved choices were invalid, safe defaults are in use. Press Apply to repair them.
