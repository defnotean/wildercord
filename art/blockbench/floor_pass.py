"""Clearance pass for the exported Sword Master rig (run after exporting sword_master_v2.bbmodel's clips).

The Blockbench ground check plants the soles; this pass keeps the rest of the Master out of the floor and out of itself,
each correction the least that clears, eased in and out so nothing pops between frames:

- Where a windup would carry the sword hand or its hilt through the head, chest or off arm (Minecraft arms are too short
  to pass a hand over or in front of the head), the hand is drawn out to the sword side instead and the shoulder and elbow
  re-solved to reach it, the hand keeping its angle so the blade still points where it was keyed.
- Where the blade itself would still cut into the body or the ground, the wrist turns it clear.
- Where a coat tail would dip below the ground in a deep stance, it bunches up along its length as cloth meeting the floor
  does.

    python art/blockbench/floor_pass.py src/main/resources/assets/wildercord/rigs/sword_master.json
"""
import json
import math
import sys

import numpy as np

# Bones the hand and blade must stay out of, and how deep they may graze them in pixels. The sword arm itself holds the
# blade, the belt meets the pommel, and the sheath takes the blade at the victory, so those are left out.
CLEAR_OF = {"head": 0.25, "chest": 0.25, "arm_l": 0.25, "ponytail": 0.25}
# Clips whose blade goes where it's keyed regardless: the victory plants it in the ground and sheathes it.
BLADE_FREE = ("victory",)
FLOOR = 1.0
# Most the hand may drift a frame (pixels), the wrist turn (degrees) and a coat's bunching (scale).
HAND_RATE, TURN_RATE, BUNCH_RATE = 1.5, 25.0, 0.06


def qmul(a, b):
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    return np.array([aw * bx + ax * bw + ay * bz - az * by, aw * by - ax * bz + ay * bw + az * bx,
                     aw * bz + ax * by - ay * bx + az * bw, aw * bw - ax * bx - ay * by - az * bz])


def qaxis(axis, deg):
    h = math.radians(deg) / 2
    v = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}[axis] if isinstance(axis, str) else axis
    return np.array([v[0] * math.sin(h), v[1] * math.sin(h), v[2] * math.sin(h), math.cos(h)])


def qmat(q):
    x, y, z, w = q
    return np.array([[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                     [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                     [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]])


def mquat(m):
    t = np.trace(m)
    if t > 0:
        s = math.sqrt(t + 1) * 2
        q = [(m[2, 1] - m[1, 2]) / s, (m[0, 2] - m[2, 0]) / s, (m[1, 0] - m[0, 1]) / s, s / 4]
    elif m[0, 0] > m[1, 1] and m[0, 0] > m[2, 2]:
        s = math.sqrt(1 + m[0, 0] - m[1, 1] - m[2, 2]) * 2
        q = [s / 4, (m[0, 1] + m[1, 0]) / s, (m[0, 2] + m[2, 0]) / s, (m[2, 1] - m[1, 2]) / s]
    elif m[1, 1] > m[2, 2]:
        s = math.sqrt(1 + m[1, 1] - m[0, 0] - m[2, 2]) * 2
        q = [(m[0, 1] + m[1, 0]) / s, s / 4, (m[1, 2] + m[2, 1]) / s, (m[0, 2] - m[2, 0]) / s]
    else:
        s = math.sqrt(1 + m[2, 2] - m[0, 0] - m[1, 1]) * 2
        q = [(m[0, 2] + m[2, 0]) / s, (m[1, 2] + m[2, 1]) / s, s / 4, (m[1, 0] - m[0, 1]) / s]
    q = np.array(q)
    return q / np.linalg.norm(q)


def swing(a, b):
    """The least rotation taking direction a onto direction b."""
    a, b = a / np.linalg.norm(a), b / np.linalg.norm(b)
    axis = np.cross(a, b)
    s = np.linalg.norm(axis)
    if s < 1e-9:
        return np.eye(3)
    return qmat(qaxis(axis / s, math.degrees(math.atan2(s, a @ b))))


def oriented(quads):
    """A cube's box along its own faces (a flat card's third axis is the cross of the other two): axes, low, high."""
    axes = []
    for q in quads:
        c = np.array([q[k * 5:k * 5 + 3] for k in range(4)])
        n = np.cross(c[1] - c[0], c[3] - c[0])
        if np.linalg.norm(n) < 1e-9:
            continue
        n /= np.linalg.norm(n)
        if all(abs(n @ a) < 0.99 for a in axes):
            axes.append(n)
        if len(axes) == 2:
            break
    axes.append(np.cross(axes[0], axes[1]))
    axes[1] = np.cross(axes[2], axes[0])
    axes = np.array([a / np.linalg.norm(a) for a in axes])
    proj = np.concatenate([quads[:, k * 5:k * 5 + 3] for k in range(4)]) @ axes.T
    return axes, proj.min(0), proj.max(0)


def ease(need, rate):
    """Never short of what's needed, and never changing faster than `rate` a frame."""
    out = list(need)
    for _ in range(len(out)):
        changed = False
        for f in range(len(out)):
            for g in (f - 1, f + 1):
                if 0 <= g < len(out) and out[g] - rate > out[f]:
                    out[f] = out[g] - rate
                    changed = True
        if not changed:
            break
    return out


class Rig:
    def __init__(self, rig):
        self.bones = rig["bones"]
        self.names = [b["name"] for b in self.bones]
        self.boxes = []
        for b in self.bones:
            q = np.array(b["quads"]).reshape(-1, 23)
            self.boxes.append([oriented(q[k:k + 6]) for k in range(0, len(q), 6)])
        self.verts = [np.concatenate([np.array(b["quads"]).reshape(-1, 23)[:, c * 5:c * 5 + 3] for c in range(4)])
                      if b["quads"] else np.zeros((0, 3)) for b in self.bones]
        # Hand and blade sampled densely enough that they can't slip past an edge between corners.
        self.sword, self.hand = self.index("sword"), self.index("hand_r")
        self.blade = self.dense(self.sword)
        self.fist = self.dense(self.hand)
        self.clear_of = [(self.index(n), d) for n, d in CLEAR_OF.items()]

    def index(self, name):
        return self.names.index(name)

    def dense(self, bone):
        pts = []
        for axes, lo, hi in self.boxes[bone]:
            g = np.meshgrid(*[np.linspace(lo[a], hi[a], max(3, int((hi[a] - lo[a]) / 0.5) + 1)) for a in range(3)], indexing="ij")
            pts.append(np.stack([x.ravel() for x in g], 1) @ axes)
        return np.concatenate(pts)

    def channel(self, clip, bone, key, f):
        track = clip["tracks"].get(self.names[bone], {})
        size = 4 if key == "q" else 3
        if key in track:
            return np.array(track[key][f * size:f * size + size])
        return np.array(self.bones[bone]["rot"] if key == "q" else self.bones[bone]["pos"] if key == "p" else [1, 1, 1])

    def matrices(self, clip, f, q=None):
        """Every bone's model matrix at frame f, with any bone's rotation replaced from `q`."""
        q = q or {}
        out = []
        for i, b in enumerate(self.bones):
            local = np.eye(4)
            local[:3, :3] = qmat(q[i] if i in q else self.channel(clip, i, "q", f)) * self.channel(clip, i, "s", f)
            local[:3, 3] = self.channel(clip, i, "p", f)
            out.append(local if b["parent"] < 0 else out[b["parent"]] @ local)
        return out

    def depth(self, m, pts, bone, mats):
        """How deep the points (local to matrix m) sink into bone's cubes."""
        o = mats[bone]
        if abs(np.linalg.det(o[:3, :3])) < 1e-6:
            return 0.0
        w = (m[:3, :3] @ pts.T).T + m[:3, 3]
        inv = np.linalg.inv(o)
        local = (inv[:3, :3] @ w.T).T + inv[:3, 3]
        deepest = 0.0
        for axes, lo, hi in self.boxes[bone]:
            p = local @ axes.T
            deepest = max(deepest, float(np.minimum(p - lo, hi - p).min(1).max()))
        return deepest

    def clear(self, mats, blade_free, hand=True, floor=True):
        if hand and any(self.depth(mats[self.hand], self.fist, b, mats) > d for b, d in self.clear_of):
            return False
        if blade_free:
            return True
        w = (mats[self.sword][:3, :3] @ self.blade.T).T + mats[self.sword][:3, 3]
        if floor and w[:, 1].min() < FLOOR:
            return False
        return all(self.depth(mats[self.sword], self.blade, b, mats) <= d for b, d in self.clear_of)


def write(clip, rig, bone, key, values):
    flat = []
    for v in values:
        flat.extend(float(round(x, 6)) for x in v)
    clip["tracks"].setdefault(rig.names[bone], {})[key] = flat


def reach(rig, clip, f, shift):
    """Rotations that carry the sword hand `shift` pixels (model space) from where it's keyed, hand angle unchanged."""
    arm, fore, hand = rig.index("arm_r"), rig.index("forearm_r"), rig.hand
    m = rig.matrices(clip, f)
    shoulder, elbow, wrist = m[arm][:3, 3], m[fore][:3, 3], m[hand][:3, 3]
    upper, lower = np.linalg.norm(elbow - shoulder), np.linalg.norm(wrist - elbow)
    target = wrist + shift
    to = target - shoulder
    dist = min(np.linalg.norm(to), upper + lower - 1e-4)
    to = to / np.linalg.norm(to)
    # The elbow bends in the plane it was keyed in, toward where it was.
    pole = (elbow - shoulder) - to * ((elbow - shoulder) @ to)
    pole = pole / np.linalg.norm(pole) if np.linalg.norm(pole) > 1e-6 else np.cross(to, [0, 0, 1])
    along = (upper * upper - lower * lower + dist * dist) / (2 * dist)
    new_elbow = shoulder + to * along + pole * math.sqrt(max(0.0, upper * upper - along * along))
    new_wrist = shoulder + to * dist
    arm_world = swing(elbow - shoulder, new_elbow - shoulder) @ m[arm][:3, :3]
    fore_world = arm_world @ np.linalg.inv(m[arm][:3, :3]) @ m[fore][:3, :3]
    bent_wrist = new_elbow + fore_world @ (np.linalg.inv(m[fore][:3, :3]) @ (wrist - elbow))
    fore_world = swing(bent_wrist - new_elbow, new_wrist - new_elbow) @ fore_world
    parent = m[rig.bones[arm]["parent"]][:3, :3]
    return {arm: mquat(np.linalg.inv(parent) @ arm_world),
            fore: mquat(np.linalg.inv(arm_world) @ fore_world),
            hand: mquat(np.linalg.inv(fore_world) @ m[hand][:3, :3])}


def keep_hilt_clear(rig, name, clip, report):
    frames = clip["frames"]
    # Out to the sword side of the chest, then forward: whichever clears with the least drift, for the whole clip.
    chest = rig.index("chest")
    need = [0.0] * frames
    for f in range(frames):
        m = rig.matrices(clip, f)
        # The hilt's drift settles where the hand and the blade it holds both come clear; the wrist sees to any floor.
        free = name in BLADE_FREE
        if rig.clear(m, free, floor=False):
            continue
        out = -m[chest][:3, 0] / np.linalg.norm(m[chest][:3, 0])
        for k in np.arange(0.25, 12.01, 0.25):
            if rig.clear(rig.matrices(clip, f, reach(rig, clip, f, out * k)), free, floor=False):
                need[f] = float(k)
                break
        else:
            need[f] = 12.0
    if not any(need):
        return
    drift = ease(need, HAND_RATE)
    poses = {b: [] for b in (rig.index("arm_r"), rig.index("forearm_r"), rig.hand)}
    for f in range(frames):
        m = rig.matrices(clip, f)
        out = -m[chest][:3, 0] / np.linalg.norm(m[chest][:3, 0])
        q = reach(rig, clip, f, out * drift[f]) if drift[f] > 0 else {}
        for b in poses:
            poses[b].append(q.get(b, rig.channel(clip, b, "q", f)))
    for b, values in poses.items():
        write(clip, rig, b, "q", values)
    report.append(f"{name}: sword hand drawn out to the side up to {max(drift):.1f} px over {sum(1 for d in drift if d > 0)} frames")


def turn_blade_clear(rig, name, clip, report):
    if name in BLADE_FREE:
        return
    frames = clip["frames"]
    base = [rig.channel(clip, rig.sword, "q", f) for f in range(frames)]
    # One wrist axis and direction all clip long, so the blade never swaps which way it turns mid-move.
    best = None
    for axis in ("y", "x"):
        for s in (1.0, -1.0):
            turns = []
            for f in range(frames):
                deg = next((d for d in range(0, 91) if
                            rig.clear(rig.matrices(clip, f, {rig.sword: qmul(base[f], qaxis(axis, d * s))}), False, False)), 90)
                turns.append(float(deg))
                if best is not None and deg > max(best[2]):
                    break
            if best is None or max(turns) < max(best[2]):
                best = (axis, s, turns)
    axis, s, need = best
    need += [0.0] * (frames - len(need))
    if not any(need):
        return
    turn = ease(need, TURN_RATE)
    write(clip, rig, rig.sword, "q", [(lambda q: q / np.linalg.norm(q))(qmul(base[f], qaxis(axis, turn[f] * s)))
                                      if turn[f] > 0 else base[f] for f in range(frames)])
    report.append(f"{name}: blade turned about {axis} up to {max(turn):.0f} deg over {sum(1 for t in turn if t > 0)} frames")


def bunch_coats(rig, name, clip, report):
    frames = clip["frames"]
    for coat in ("coat_l", "coat_r"):
        bone = rig.index(coat)
        need = [0.0] * frames
        for f in range(frames):
            scale = rig.channel(clip, bone, "s", f)

            def lowest(bunch):
                m = rig.matrices(clip, f)
                local = np.eye(4)
                local[:3, :3] = qmat(rig.channel(clip, bone, "q", f)) * (scale * np.array([1, 1 - bunch, 1]))
                local[:3, 3] = rig.channel(clip, bone, "p", f)
                w = m[rig.bones[bone]["parent"]] @ local
                return ((w[:3, :3] @ rig.verts[bone].T).T + w[:3, 3])[:, 1].min()

            if lowest(0) >= 0.1:
                continue
            lo, hi = 0.0, 0.7
            for _ in range(30):
                mid = (lo + hi) / 2
                lo, hi = (lo, mid) if lowest(mid) >= 0.1 else (mid, hi)
            need[f] = hi
        if not any(need):
            continue
        bunch = ease(need, BUNCH_RATE)
        write(clip, rig, bone, "s", [rig.channel(clip, bone, "s", f) * np.array([1, 1 - bunch[f], 1]) for f in range(frames)])
        report.append(f"{name}: {coat} bunched up to {max(bunch) * 100:.0f}% over {sum(1 for b in bunch if b > 0)} frames")


def main(path):
    data = json.load(open(path))
    rig = Rig(data)
    report = []
    for name, clip in data["clips"].items():
        keep_hilt_clear(rig, name, clip, report)
        turn_blade_clear(rig, name, clip, report)
        bunch_coats(rig, name, clip, report)
    json.dump(data, open(path, "w"), separators=(",", ":"))
    print("\n".join(report) or "nothing to clear")


if __name__ == "__main__":
    main(sys.argv[1])
