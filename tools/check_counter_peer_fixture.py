#!/usr/bin/env python3
"""Compile additive connected counter fixture and exercise original-runtime geometry negatives.

Never a client launch. The transformed Fabric fixture is compile-only. Native/PNG/mixin
application and the two-profile 54-case run remain unverified until a supervised native run.
"""
import argparse, hashlib, json, os, subprocess
from pathlib import Path
from earned_counter_offline import dependencies, java_bin
from freeze_brace_null_baseline import SOURCES as PURE
ROOT=Path(__file__).resolve().parents[1]
JAVA=[str(p.relative_to(ROOT)) for p in sorted((ROOT/'src/gametest/java').rglob('CounterPeer*.java'))]
JAVA+=['src/gametest/java/dev/wildercord/aura/world/ConnectedCastReceiptTest.java','tools/CheckCounterPeerEvidence.java','tools/CheckCounterPeerFailureExit.java',*PURE,
       'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java','src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java']
SOURCES=JAVA+['src/gametest/resources/cast-receipt-native-contract.json','src/gametest/resources/counter-peer-gametest.mixins.json',
             'tools/check_counter_peer_fixture.py','tools/native/export_two_client_launch.init.gradle','tools/native/launch_two_clients.py','tools/test_two_client_supervisor.py','build.gradle']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',type=Path,required=True);p.add_argument('--verification',type=Path,required=True);p.add_argument('--compiled',type=Path,required=True);a=p.parse_args()
 out=a.out.resolve();out.mkdir(parents=True,exist_ok=True)
 if any(out.iterdir()):raise ValueError('Fresh evidence directory required')
 receipt=a.compiled/'verification-receipt.json';support=json.loads(receipt.read_text())
 if not support.get('tracked_source_sha256_before') or support.get('tracked_source_sha256_before')!=support.get('tracked_source_sha256_after'):raise ValueError('Supporting source provenance not stable')
 if not {'main','client','gametest'}<={v['stage'] for v in support['stages'] if v['exit_code']==0}:raise ValueError('Supporting compile stages missing')
 jars,hashes=dependencies(a.verification);java=java_bin(a.verification);fixture=a.verification/'minecraft-26.3-fabric-compile.jar'
 if sha(fixture)!=json.loads((a.verification/'transform-receipt.json').read_text())['output_sha256']:raise ValueError('Compile-only fixture changed')
 classes=out/'classes';classes.mkdir();runtime=os.pathsep.join(map(str,[*jars,*[a.compiled/x for x in ['main','client','gametest']]]))
 before={s:sha(ROOT/s) for s in SOURCES};commands=[
 [str(java/'javac'),'--release','25','-proc:none','-sourcepath','','-cp',str(fixture)+os.pathsep+runtime,'-d',str(classes),*[str(ROOT/s) for s in JAVA]],
 [str(java/'java'),'-XX:-UsePerfData','-cp',str(classes)+os.pathsep+runtime,'dev.wildercord.gametest.CheckCounterPeerEvidence'],
 [str(java/'java'),'-XX:-UsePerfData','-cp',str(classes)+os.pathsep+runtime,'dev.wildercord.gametest.CheckCounterPeerFailureExit',str(out/'intentionally-absent-rejection-directory')]]
 report={'passed':False,'native':'not run','sourceBefore':before,'dependencySha256':hashes,'supportingReceiptSha256':sha(receipt),'compileOnlyFixtureSha256':sha(fixture),'commands':commands,'stages':[]}
 try:
  for name,expected,command in zip(['compile','geometry-and-phase-negatives','failure-persistence-exit'],[0,0,86],commands):
   with (out/(name+'.log')).open('w') as f:code=subprocess.run(command,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT).returncode
   report['stages'].append({'stage':name,'exitCode':code,'expectedExitCode':expected})
   if code!=expected:raise AssertionError(name+' failed')
  report['passed']=True
 finally:
  report['sourceAfter']={s:sha(ROOT/s) for s in SOURCES};report['sourceUnchanged']=report['sourceBefore']==report['sourceAfter'];report['passed'] &= report['sourceUnchanged'];(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
 if not report['passed']:raise AssertionError('Source changed during fixture verification')
 print(out/'report.json')
if __name__=='__main__':main()
