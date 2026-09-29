"""The arithmetic behind docs/audit/spell-feel-shapes-cast.md, section 2.

Usage:  python docs/audit/shapes-cast-numbers.py path/to/runes.tsv

The TSV is a dump of Runes.all() (id, family, tier, cost, mult, element, kind, category, traits,
desc), tab separated, with a header row. Costs follow SpellCompiler.groupCost/partCost, which
SpellCompilerTest asserts, e.g. (3 + 8 * 1.1) * 2.4 for a split bolt, bolt + 2 + bolt for an echo.

Reference effect: Harm, 8 mana, 7 damage per landing at power 1. "dpm" is Harm damage per mana per
stationary target over one whole cast; "vs bolt" is relative to Bolt . Harm (0.59).
"""
import csv
import math
import sys

path = sys.argv[1] if len(sys.argv) > 1 else "runes.tsv"
rows = list(csv.DictReader(open(path, encoding="utf-8"), delimiter="\t"))
shapes = {r["id"]: r for r in rows if r["family"] == "SHAPE"}
H = 8.0
D = 7.0

# Per shape: (landings per target on a target that stays put, power per landing, area in m2 or 0).
# Counted from ShapeRunners, CastEngine, CraftedShapes and ExplorerShapes; see the report.
model = {
    "self": (1, 1, 0), "touch": (1, 1, 0), "bolt": (1, 1, 0), "beam": (1, 1, 0), "burst": (1, 1, math.pi * 16),
    "zone": (6, 1, math.pi * 9), "rain": (1.13, 1, math.pi * 16), "arc": (1, 1, math.pi * 4),
    "cone": (1, 1, 0.5 * 36 * math.pi / 3), "trail": (8, 1, 0), "wall": (6, 1, 7 * 3), "orbit": (8, 1, 0),
    "ring": (1, 1, math.pi * 49), "pillar": (1, 1, math.pi * 2.25), "wave": (1, 1, 3 * 14), "mine": (1, 1, math.pi * 9),
    "totem": (6, 1, math.pi * 25), "domain": (7, 1, math.pi * 81), "crescent": (1, 1, 5 * 16),
    "barrage": (8, 0.35, 0.5 * 3.6 ** 2 * math.radians(80)), "orb": (3, 1, math.pi * 4), "blitz": (1, 1, 8 * 2.4),
    "spark": (1, 0.75, 0), "ray": (1, 1, 0), "nova": (1, 1, math.pi * 6.25), "wisp": (1, 1, 0),
    "comet": (1, 1, math.pi * 9), "ricochet": (1, 1, 0), "cluster": (1, 1, math.pi * 1.5 ** 2 * 5), "lance": (1, 1, 0),
    "sweep": (1, 1, 0.5 * 100 * math.radians(100)), "prism": (1, 1, 0), "stream": (6, 0.35, 0),
    "vortex": (5, 1, math.pi * 3.24), "snare": (1, 1, math.pi * 6.25), "constellation": (1, 1, 0), "glaive": (2, 1, 0),
    "imprint": (1, 1, math.pi * 9), "latch": (4, 0.7, 0)}

out = []
for sid, r in shapes.items():
    c = float(r["cost"])
    m = float(r["mult"])
    t = int(r["tier"])
    hits, st, area = model[sid]
    cost = c + H * m
    dmg = D * hits * st
    out.append((sid, t, c, m, cost, hits, st, dmg, dmg / cost, area))
bolt = [o for o in out if o[0] == "bolt"][0][8]
print(f"{'shape':14}{'T':>2}{'cost':>6}{'mult':>6}{'Harm':>7}{'hits':>6}{'str':>5}{'dmg/tgt':>8}{'dpm':>6}{'vs bolt':>8}{'area':>7}")
for sid, t, c, m, cost, hits, st, dmg, eff, area in sorted(out, key=lambda o: -o[8]):
    print(f"{sid:14}{t:>2}{c:6.1f}{m:6.1f}{cost:7.1f}{hits:6.2f}{st:5.2f}{dmg:8.1f}{eff:6.2f}{eff / bolt:8.2f}{area:7.0f}")

print()
print("Area shapes: m2 per mana (Harm), one landing")
for sid in ("burst", "ring", "nova", "cone", "sweep", "wave", "crescent", "pillar", "comet", "arc", "mine", "snare", "imprint", "cluster", "blitz"):
    o = [x for x in out if x[0] == sid][0]
    print(f"  {sid:10} cost {o[4]:5.1f} area {o[9]:6.1f} -> {o[9] / o[4]:.2f} m2 per mana")

print()
print("Modifiers: gain per cost multiplier")


def line(name, gain, cost, note=""):
    print(f"  {name:38} x{gain:.2f} for x{cost:.2f} -> {gain / cost:.2f}  {note}")


line("Amplify", 1.5, 1.6)
line("Amplify x2", 2.25, 1.6 ** 2)
line("Overcharge", 2.5, 3.0)
line("Focus (power part)", 1.5, 1.2, "and radius x0.5")
line("Frugal (effect part)", 0.6, 0.5)
line("Frugal x2", 0.36, 0.25)
line("Belated x1", 1.4, 1.25, "1.5 s late")
line("Belated x3", 2.744, 1.25 ** 3, "4.5 s late")
line("Kindled", 1.2, 1.4, "plus a 4 s burn")
line("Execute (under half health)", 2.0, 1.3, "conditional")
line("Trial Key (full health)", 1.6, 1.3, "conditional")
line("Vow", 2.0, 1.0, "cooldown x4")
line("Linger x1 (3 landings)", 3.0, 1.8, "effect cost only")
line("Linger x2 (5 landings)", 5.0, 1.8 ** 2)
line("Linger x3 (7 landings)", 7.0, 1.8 ** 3)
line("Extend on a lingering shape", 2.0, 1.4)
line("Quicken on Zone x1 (12 pulses)", 2.0, 1.2)
line("Quicken on Zone x2 (24 pulses)", 4.0, 1.44)
line("Quicken on Wall x2 (21 vs 6)", 21 / 6, 1.44)
line("Quicken on Domain x2 (25 vs 7)", 25 / 7, 1.44)
line("Quicken on Totem x2 (21 vs 6)", 21 / 6, 1.44)
line("Quicken on Latch x2 (16 vs 4)", 4.0, 1.44)
line("Quicken on Stream x1 (12 vs 6)", 2.0, 1.2)
line("Quicken on Barrage x1 (12 vs 8 blows)", 12 / 8, 1.2)
line("Split (3 bolts)", 3, 2.4)
line("Volley (3 shots)", 3, 2.4)
line("Split + Volley (9 bolts)", 9, 2.4 ** 2)
line("Pierce (up to 3 more in line)", 4, 1.3, "only if lined up")
line("Chain (up to 3 more within 6)", 4, 1.8, "only if crowded")
line("Widen (area)", 2.25, 1.5, "area, not power")

print()
print("Zone . Harm stacks: damage to one creature and cost")


def zone(q=0, lin=0, ext=0, amp=0, seconds=6):
    interval = max(5, round(20 / 2 ** q))
    pulses = max(1, round(seconds * 2 ** ext * 20 / interval))
    landings = pulses * (1 + min(6, 2 * lin))
    cost = (8 + H * 1.8 ** lin * 1.6 ** amp * 2.0) * 1.2 ** q * 1.4 ** ext
    return pulses, landings, landings * D * 1.5 ** amp, cost


for label, kw in (("plain", {}), ("Quicken", dict(q=1)), ("Quicken x2", dict(q=2)), ("Linger", dict(lin=1)), ("Amplify", dict(amp=1)),
                  ("Extend", dict(ext=1)), ("Linger + Quicken x2", dict(lin=1, q=2)), ("Linger + Quicken x2 + Extend", dict(lin=1, q=2, ext=1))):
    p, l, d, c = zone(**kw)
    print(f"  Zone.Harm {label:30} pulses {p:3d} landings {l:3d} dmg {d:7.1f} cost {c:6.1f} dpm {d / c:5.2f}")

print()
print("Proposed price by landings: multiplier + 0.15 per landing beyond the first")
for sid, extra in (("zone", 5), ("wall", 5), ("totem", 5), ("orbit", 7), ("trail", 7), ("vortex", 4), ("domain", 6)):
    r = shapes[sid]
    m2 = float(r["mult"]) + 0.15 * extra
    cost = float(r["cost"]) + H * m2
    hits, st, _ = model[sid]
    print(f"  {sid:8} mult {float(r['mult']):.2f} -> {m2:.2f}  cost {cost:5.1f}  dpm {D * hits * st / cost:.2f}")

print()
print("Blood Price and Heal: a 12-mana Heal restores 8 health, which pays for", 8 * 5, "mana of spells (", 8 * 5 / 12, "x )")
