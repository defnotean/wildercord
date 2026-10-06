#!/usr/bin/env python3
"""Read-only bytecode audit for the Stone Hinge feasibility gate; never a native gameplay pass."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess


def method(text, signature):
    start = text.index('  ' + signature)
    following = re.search(r'\n  (?:public|protected|private|static|boolean|void) ', text[start + 3:])
    return text[start:] if following is None else text[start:start + 3 + following.start()]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--javap', default='javap')
    parser.add_argument('--minecraft-jar', type=Path, required=True)
    parser.add_argument('--fabric-entity-jar', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    def inspect(jar, name, verbose=False):
        return subprocess.check_output([args.javap, '-classpath', str(jar), '-c', '-p'] + (['-v'] if verbose else []) + [name], text=True)
    living = inspect(args.minecraft_jar, 'net.minecraft.world.entity.LivingEntity')
    player = inspect(args.minecraft_jar, 'net.minecraft.world.entity.player.Player')
    listener = inspect(args.minecraft_jar, 'net.minecraft.server.network.ServerGamePacketListenerImpl')
    fabric = inspect(args.fabric_entity_jar, 'net.fabricmc.fabric.mixin.entity.event.LivingEntityMixin', True)
    hurt = method(living, 'public boolean hurtServer(')
    wound = method(player, 'protected void actuallyHurt(')
    knock = method(living, 'public void knockback(double, double, double, net.minecraft.world.damagesource.DamageSource, float, boolean);')
    teleport = method(listener, 'public void teleport(net.minecraft.world.entity.PositionMoveRotation, java.util.Set<net.minecraft.world.entity.Relative>);')
    awaiting = method(listener, 'private boolean updateAwaitingTeleport();')
    handle = method(listener, 'public void handleMovePlayer(')
    dependencies = [{'path': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()} for path in (args.minecraft_jar, args.fabric_entity_jar)]
    checks = {
        'minecraft_matches_audited_26_3': dependencies[0]['sha256'] == '4508d006323f24fa02876310c192d739af56516eb259000ac50f0909a68c9a2d',
        'fabric_matches_audited_entity_events': dependencies[1]['sha256'] == 'a5a9e382e4f9875f7450dbf0d45221ca70afc00cf84aa02023104bb15a530ade',
        'wound_precedes_native_knockback': hurt.index('Method actuallyHurt:') < hurt.index('Method dealDefaultKnockback:'),
        'native_knockback_precedes_totem': hurt.index('Method dealDefaultKnockback:') < hurt.index('Method checkTotemDeathProtection:'),
        'absorption_write_precedes_health_write': wound.index('Method setAbsorptionAmount:') < wound.index('Method setHealth:'),
        'resistance_precedes_only_native_velocity_write': knock.index('Attributes.KNOCKBACK_RESISTANCE:') < knock.index('Method setDeltaMovement:') and knock.count('Method setDeltaMovement:') == 1,
        'zero_resisted_strength_returns_before_velocity_write': re.search(r'15: ifgt\s+19\s+18: return', knock) is not None,
        'native_horizontal_halving_preserved': knock.count('// double 2.0d') == 3,
        'grounded_and_airborne_y_branches_present': 'Method onGround:()Z' in knock and '// double 0.4d' in knock,
        'fabric_allow_damage_runs_before_native_wound': 'target="Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"' in method(fabric, 'private void beforeDamage('),
        'fabric_allow_death_runs_at_second_death_check': 'ordinal=1' in method(fabric, 'boolean beforeEntityKilled('),
        'fabric_after_damage_is_tail': 'value="TAIL"' in method(fabric, 'private void afterDamage('),
        'teleport_creates_pending_client_ack': 'Field awaitingPositionFromClient:' in teleport and 'ClientboundPlayerPositionPacket.of:' in teleport,
        'pending_teleport_rejects_normal_position_processing': 'Field awaitingPositionFromClient:' in awaiting and 'iconst_1' in awaiting
        and handle.index('Method updateAwaitingTeleport:') < handle.index('160: return') < handle.index('Method handlePlayerPositionChange:'),
    }
    receipt = {
        'status': 'bytecode-contract-passed' if all(checks.values()) else 'bytecode-contract-failed',
        'native_gameplay_executed': False,
        'stone_hinge_movement_approved': False,
        'dependencies': dependencies,
        'checks': checks,
    }
    body = json.dumps(receipt, indent=2) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(body)
    print(body, end='')
    return 0 if all(checks.values()) else 1


if __name__ == '__main__':
    raise SystemExit(main())
