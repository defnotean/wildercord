"""Original Moon artifact controls, including rejected/partial diagnostics."""
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import uuid

from PIL import Image
import curate_moon_frames as c
from native import run_paired_matrix as m
from native import launch_two_clients as s


class FrameHelper:
    def setUp(self):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup)
        self.root=Path(temp.name).resolve();self.game=self.root/'build/run/clientGameTest';self.game.mkdir(parents=True)
        self.nonce='00000000-0000-4000-8000-000000000001'
        self.make_frame(self.game, self.root/'articulated-native.log', 'wide')

    def make_frame(self,game,log,skin,angle=None,role='host',nonce=None):
        nonce=nonce or self.nonce
        name=c.PREFIX+('remote' if role=='peer' else 'fp')+'_'+skin+'_'+(angle+'_' if angle else '')+s.MOON_CASE
        screenshot=game/'screenshots'/(name+'.png');screenshot.parent.mkdir(parents=True,exist_ok=True)
        Image.new('RGBA',(3,3),'blue' if role=='peer' else 'red').save(screenshot)
        data=screenshot.read_bytes();pixels=c.image_evidence(data)
        receipt=game/'screenshots/crimson-moon-owner-receipts'/str(uuid.uuid4())/'000009.json';receipt.parent.mkdir(parents=True)
        record={'schemaVersion':3 if angle else 2,'launchNonce':nonce,'verified':True,'failures':[],
            'serverReleaseFrameCorrespondenceVerified':False,'pixelQualityReviewed':False,
            'expected':{'name':name,'view':'remote' if role=='peer' else 'fp','skin':skin,'owner':11,'uuid':s.MOON_PROFILES['moon-'+skin][1],
                        'activation':100,'mode':'articulated','hand':'RIGHT','armor':True,'shell':True,'phase':'ACTIVE','age':10},
            'copy':{'observations':{'actualSourceAge':'10.0'}},'callbackPixels':pixels,
            'image':{'relativeImagePath':str(screenshot.relative_to(game)),'pngBytes':len(data),'pngSha256':c.sha(data),'decodedPixels':pixels}}
        receipt.write_text(json.dumps(record))
        log.write_text(f'CRIMSON_MOON_OWNER_RECEIPT name={name} verified=true actualSourceAge=10.0 receipt={receipt} phaseBasis=authored_post_hitstop_source_palette serverReleaseFrameCorrespondenceVerified=false pixelQualityReviewed=false\n')
        self.name,self.png,self.receipt,self.record,self.log=name,screenshot,receipt,record,log
        return name,screenshot,receipt,record

class FrameTests(FrameHelper, unittest.TestCase):
    def frame(self):
        return c.frame(self.game,self.log,self.name,self.nonce,'wide')

    def test_exact_logged_sequence_nine_is_original_and_crc_verified(self):
        item,png,receipt=self.frame()
        self.assertEqual(item['originalReceipt'],str(self.receipt.relative_to(self.game)))
        self.assertTrue(item['originalReceipt'].endswith('/000009.json'))
        self.assertTrue(item['allPngChunkCrcsVerified'])
        self.assertEqual(png,self.png.read_bytes());self.assertEqual(receipt,self.receipt.read_bytes())
        self.assertFalse(item['serverReleaseFrameCorrespondenceVerified'])

    def test_stale_nonce_wrong_case_skin_or_proof_flag_rejected(self):
        for changed in [{'launchNonce':str(uuid.uuid4())},{'serverReleaseFrameCorrespondenceVerified':True},{'pixelQualityReviewed':True},
                        {'expected':{**self.record['expected'],'name':'other'}},{'expected':{**self.record['expected'],'skin':'slim'}},
                        {'expected':{**self.record['expected'],'activation':None}}]:
            with self.subTest(changed=changed):
                self.receipt.write_text(json.dumps({**self.record,**changed}))
                with self.assertRaises(ValueError):self.frame()
        self.receipt.write_text(json.dumps(self.record))

    def test_wrong_png_bytes_and_hashes_or_callback_are_rejected(self):
        raw=self.png.read_bytes()
        self.png.write_bytes(raw[:-1]+bytes([raw[-1]^1]))
        with self.assertRaisesRegex(ValueError,'CRC'):self.frame()
        self.png.write_bytes(raw)
        for image in [{**self.record['image'],'pngSha256':'b'*64},{**self.record['image'],'relativeImagePath':'../outside.png'},
                      {**self.record['image'],'decodedPixels':{}}]:
            self.receipt.write_text(json.dumps({**self.record,'image':image}))
            with self.assertRaises(ValueError):self.frame()

    def test_missing_duplicate_outside_and_symlink_receipt_rejected(self):
        original=self.log.read_text()
        for text in ['',original+original,original.replace(str(self.receipt),'/tmp/other/000009.json')]:
            self.log.write_text(text)
            with self.assertRaises(ValueError):self.frame()
        self.log.write_text(original)
        contents=self.receipt.read_bytes();self.receipt.unlink();outside=self.root/'outside.json';outside.write_bytes(contents);self.receipt.symlink_to(outside)
        with self.assertRaisesRegex(ValueError,'Symlink'):self.frame()

    def test_ancestor_symlink_rejected_even_when_target_is_inside_game(self):
        directory=self.receipt.parent; moved=directory.with_name(str(uuid.uuid4()));directory.rename(moved);directory.symlink_to(moved,target_is_directory=True)
        with self.assertRaisesRegex(ValueError,'Symlink'):self.frame()

    def test_native_rejection_retained_only_as_diagnostic_original(self):
        self.record.update(verified=False,failures=['wrong_rendered_beat']);self.receipt.write_text(json.dumps(self.record))
        self.log.write_text(self.log.read_text().replace('verified=true','verified=false'))
        item,png,receipt=self.frame();item.update(group='owner',role='wide',diagnostic=True,causalGroupVerified=False)
        report=c.package(self.root,Path('artifacts/review/out'),{},[(item,png,receipt)])
        self.assertTrue(report['frames'][0]['diagnostic']);self.assertFalse(report['frames'][0]['recordReportsVerified'])
        self.assertEqual(report['frames'][0]['nativeRejections'],['wrong_rendered_beat'])
        self.assertTrue(report['frames'][0]['png'].startswith('diagnostic-'))

    def test_budget_counts_original_png_receipt_and_manifest_without_reencoding(self):
        item,png,receipt=self.frame();item.update(group='owner',role='wide',diagnostic=True,causalGroupVerified=False)
        with patch.object(c,'BUDGET',c.RESERVE+len(png)+len(receipt)):
            report=c.package(self.root,Path('artifacts/review/bounded'),{},[(item,png,receipt)])
        self.assertFalse(report['frames'][0]['included']);self.assertIn('full native artifact',report['frames'][0]['omission'])
        self.assertFalse(list((self.root/'artifacts/review/bounded').glob('*.png')))
        report=c.package(self.root,Path('artifacts/review/full'),{},[(item,png,receipt)])
        out=self.root/'artifacts/review/full'
        self.assertLess(sum(p.stat().st_size for p in out.iterdir()),14_000_000)
        self.assertEqual((out/report['frames'][0]['png']).read_bytes(),png)

    def test_owner_requires_current_launch_then_collects_only_actual_skin(self):
        launch_path=self.root/'artifacts/review/moon-owner-launch.json';launch_path.parent.mkdir(parents=True)
        launch={'schemaVersion':1,'purpose':'owner-fp-native-diagnostic','nonce':self.nonce,'checkoutSha':'a'*40,'runId':'123','runAttempt':'1',
                'remoteObserverCoverage':False,'serverReleaseFrameCorrespondenceVerified':False,
                'selection':{'entries':['dev.wildercord.client.combat.CrimsonMoonCaptureTest']}}
        launch_path.write_text(json.dumps(launch))
        identity={'checkedOutSha':'a'*40,'runId':'123','runAttempt':'1','headSha':'b'*40}
        with patch('curate_masters_frames.provenance',return_value=identity):
            report=c.owner(self.root,Path('artifacts/review/owner'))
            self.assertEqual(len(report['frames']),1)
            self.assertFalse(report['matrixVerified']);self.assertFalse(report['remoteObserverCoverage'])
            self.assertTrue(report['frames'][0]['diagnostic'])
            launch['runId']='old';launch_path.write_text(json.dumps(launch))
            with self.assertRaises(ValueError):c.owner(self.root,Path('artifacts/review/stale'))

    def test_owner_missing_json_is_explicit_and_never_verified(self):
        launch_path=self.root/'artifacts/review/moon-owner-launch.json';launch_path.parent.mkdir(parents=True)
        launch_path.write_text(json.dumps({'schemaVersion':1,'purpose':'owner-fp-native-diagnostic','nonce':self.nonce,'checkoutSha':'a'*40,'runId':'123','runAttempt':'1',
            'remoteObserverCoverage':False,'serverReleaseFrameCorrespondenceVerified':False,'selection':{'entries':['dev.wildercord.client.combat.CrimsonMoonCaptureTest']}}))
        self.receipt.unlink()
        with patch('curate_masters_frames.provenance',return_value={'checkedOutSha':'a'*40,'runId':'123','runAttempt':'1'}):
            report=c.owner(self.root,Path('artifacts/review/missing'))
        self.assertEqual(report['frames'],[]);self.assertTrue(report['errors']);self.assertFalse(report['matrixVerified'])


class PairedCollectionTests(FrameHelper, unittest.TestCase):
    def setUp(self):
        super().setUp()
        self.source=self.root/'build/native/paired-matrix';self.source.mkdir(parents=True)
        self.identity={'sourceHead':'a'*40,'checkoutSha':'a'*40,'prHeadSha':'','runId':'123','runAttempt':'1','job':'connected-combat-native'}
        self.matrix={'provenance':self.identity,'status':'passed','groups':[], 'serverReleaseFrameCorrespondenceVerified':False,
                     'pixelQualityReviewed':False,'releaseImageDamageOrderVerified':True};self.results={}
        for index,group in enumerate(m.GROUPS):
            base=self.source/group['id'];base.mkdir()
            selection=m.selected(group);nonce=str(uuid.UUID(int=index+1))
            result={**self.identity,**selection,'status':'passed','nonce':nonce,'runIdentity':'123-1-'+nonce,
                    'ci':{'GITHUB_RUN_ID':'123','GITHUB_RUN_ATTEMPT':'1','GITHUB_JOB':'connected-combat-native'}}
            descriptor=self.source/(group['suite']+'-descriptor.json');descriptor.write_text('{}')
            launch=self.source/(group['suite']+'-launch.json');launch.write_text(json.dumps({'descriptorSha256':s.digest(descriptor)}))
            result.update(launchSha256=s.digest(launch),descriptorSha256=s.digest(descriptor))
            (base/'result.json').write_text(json.dumps(result));(base/'launch-proof.json').write_text(json.dumps({**result,'status':'started'}))
            self.matrix['groups'].append({**group,'status':'passed','nonce':nonce,'runIdentity':result['runIdentity'],'resultSha256':s.digest(base/'result.json')})
            self.results[group['id']]=result
            if group['suite']=='moon':
                for role in ('host','peer'):
                    (base/role).mkdir()
                    name,png,receipt,record=self.make_frame(base/role,base/(role+'.log'),selection['expectedSkin'],group['observerAngle'],role,nonce)
                    ipc=base/'ipc';ipc.mkdir(exist_ok=True)
                    bindings={'receiptRelativePath':str(receipt.relative_to(base/role)),'receiptSha256':s.digest(receipt),
                              'pngRelativePath':str(png.relative_to(base/role)),'pngSha256':s.digest(png),
                              'callbackPixelSha256':record['callbackPixels']['sha256'],'screenshotName':name}
                    (ipc/(role+'-case-00-observed.properties')).write_text(''.join(k+'='+v+'\n' for k,v in bindings.items()))
        self.flush_matrix()
    def flush_matrix(self):
        (self.source/'matrix-result.json').write_text(json.dumps(self.matrix))
    def collect(self):
        with patch.object(m,'provenance',return_value=self.identity),patch.object(m,'validate_group',side_effect=lambda base,*args:self.results[base.name]):
            return c.paired(self.root,self.source.relative_to(self.root),Path('artifacts/review/paired'))
    def test_complete_fixed_matrix_retains_eight_originals_and_observer_priority(self):
        report=self.collect()
        self.assertTrue(report['matrixVerified']);self.assertEqual(len(report['frames']),8)
        self.assertTrue(all(f['role']=='peer' for f in report['frames'][:4]))
        self.assertTrue(all(f['causalGroupVerified'] for f in report['frames']))
        self.assertTrue(all(not f['serverReleaseFrameCorrespondenceVerified'] for f in report['frames']))
        self.assertEqual(len(set(f['nonce'] for f in report['frames'])),4)
    def test_failed_group_remains_diagnostic_and_missing_groups_are_reported(self):
        self.matrix['groups'][2]['status']='failed';self.matrix['status']='failed';self.flush_matrix()
        report=self.collect();self.assertFalse(report['matrixVerified'])
        failed=[f for f in report['frames'] if f['group']==m.GROUPS[2]['id']]
        self.assertEqual(len(failed),2);self.assertTrue(all(f['diagnostic'] and not f['releaseImageDamageOrderVerified'] for f in failed))
    def test_missing_group_or_wrong_exact_head_rejects_summary(self):
        self.matrix['groups'].pop();self.flush_matrix()
        with self.assertRaisesRegex(ValueError,'ledger'):self.collect()
    def test_changed_export_hash_cannot_promote_original_diagnostics(self):
        (self.source/'moon-launch.json').write_text('{}')
        report=self.collect();self.assertFalse(report['matrixVerified'])
        self.assertTrue(all(f['diagnostic'] for f in report['frames']))
    def test_missing_receipt_is_reported_while_other_originals_survive(self):
        self.receipt.unlink()
        report=self.collect();self.assertFalse(report['matrixVerified']);self.assertEqual(len(report['frames']),7);self.assertTrue(report['missing'])
    def test_different_current_nonce_logged_receipt_cannot_inherit_causal_claim(self):
        record=json.loads(self.receipt.read_text());record['expected']['activation']=999;record['verified']=False;record['failures']=['different_action']
        self.receipt.write_text(json.dumps(record))
        report=self.collect();self.assertFalse(report['matrixVerified'])
        selected=[f for f in report['frames'] if f['group']==m.GROUPS[-1]['id'] and f['role']=='peer'][0]
        self.assertTrue(selected['diagnostic']);self.assertFalse(selected['causalGroupVerified'])
        self.assertTrue(selected['png'].startswith('diagnostic-'))


class WorkflowTests(unittest.TestCase):
    def test_existing_job_caps_full_artifact_and_exact_owner_collector_are_retained(self):
        text=(Path(__file__).resolve().parents[1]/'.github/workflows/build.yml').read_text()
        job=text.split('  connected-combat-native:',1)[1].split('\n  articulated-native:',1)[0]
        self.assertIn('timeout-minutes: 60',job);self.assertIn('timeout-minutes: 51',job)
        self.assertIn('run_paired_matrix.py',job);self.assertIn('build/native/**/screenshots/**',job)
        self.assertIn("if: always()",job);self.assertIn('curate_moon_frames.py',job)
        self.assertNotIn('crimson-moon-owner-receipts/*/000001.json',text)
        self.assertIn('curate_moon_frames.py --owner',text)


if __name__=='__main__':unittest.main()
