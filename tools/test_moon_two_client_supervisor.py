"""Moon supervisor proof controls using synthetic files only; no native JVM starts."""
import copy
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import types
import unittest
from unittest.mock import patch
from PIL import Image
import test_two_client_supervisor as cast
from verify_articulated_render_receipts import image_evidence

s = cast.supervisor
ROOT = Path(__file__).resolve().parents[1]
MOON = json.loads((ROOT / s.MOON_CONTRACT).read_text())


class SelectionTests(unittest.TestCase):
    def test_only_four_fixed_profile_angle_shards_are_allowed(self):
        for profile in s.MOON_PROFILES:
            for angle in s.MOON_ANGLES:
                with self.subTest(profile=profile, angle=angle):
                    selected = s.selection('moon', profile, s.MOON_CASE, angle)
                    self.assertEqual(selected['cases'], [s.MOON_CASE])
                    self.assertNotEqual(selected['profiles']['host'][1], selected['profiles']['peer'][1])
        for args in [('moon','aura',s.MOON_CASE,'front_oblique'),('moon','moon-wide',None,'front_oblique'),
                     ('moon','moon-wide','another_case','front_oblique'),('moon','moon-wide',s.MOON_CASE,'rear'),
                     ('cast-receipt','aura',s.MOON_CASE,None)]:
            with self.subTest(args=args), self.assertRaises(ValueError): s.selection(*args)

    def test_cli_requires_case_and_angle_for_moon(self):
        args = ['build/launch.json','--output','build/run','--suite','moon','--profile','moon-wide',
                '--case',s.MOON_CASE,'--observer-angle','front_oblique']
        self.assertEqual(s.parse_args(args).case, s.MOON_CASE)
        with patch('sys.stderr',new=io.StringIO()), self.assertRaises(SystemExit): s.parse_args(args[:-2])

    def test_suite_defaults_and_hard_timeout_ceilings(self):
        moon=['build/launch.json','--output','build/run','--suite','moon','--profile','moon-wide','--case',s.MOON_CASE,'--observer-angle','front_oblique']
        cast_args=['build/launch.json','--output','build/run','--suite','cast-receipt','--profile','aura']
        self.assertEqual(s.parse_args(moon).timeout,180);self.assertEqual(s.parse_args(cast_args).timeout,900)
        self.assertEqual(s.parse_args(moon+['--timeout','180']).timeout,180)
        for timeout in ('181','900','901'):
            with self.subTest(timeout=timeout),patch('sys.stderr',new=io.StringIO()),self.assertRaises(SystemExit):s.parse_args(moon+['--timeout',timeout])
        self.assertEqual(s.parse_args(cast_args+['--timeout','900']).timeout,900)

    def test_receipt_nonce_is_generated_input_not_inherited_environment(self):
        with patch.dict(os.environ,{'WILDERCORD_MOON_RECEIPT_NONCE':'stale','JAVA_TOOL_OPTIONS':'bad'}):
            self.assertNotIn('WILDERCORD_MOON_RECEIPT_NONCE',s.environment({'environment':{}},Path('/tmp/game')))
            env=s.environment({'environment':{}},Path('/tmp/game'),'00000000-0000-4000-8000-000000000001')
            self.assertEqual(env['WILDERCORD_MOON_RECEIPT_NONCE'],'00000000-0000-4000-8000-000000000001')
            self.assertNotIn('JAVA_TOOL_OPTIONS',env)


class MoonFixture(cast.Fixture):
    def setUp(self):
        super().setUp()
        self.selected=s.selection('moon','moon-wide',s.MOON_CASE,'front_oblique')
        cast.write_json(self.root/s.MOON_CONTRACT,MOON)
        mixin='crimson-moon-multiplayer-gametest.mixins.json'
        cast.write_json(self.root/'src/gametest/resources'/mixin,{'required':True,'client':['FixedHook']})
        for args in [('add','src'),('-c','user.name=Test','-c','user.email=test@example.invalid','commit','-qm','moon fixture')]:
            s.git(self.root,*args)
        self.head=s.source_head(self.root)
        cast.write_json(self.descriptor,{'entrypoints':{'fabric-client-gametest':[s.MOON_ENTRYPOINT]},'mixins':[mixin]})
        cast.write_json(self.descriptor.parent/mixin,{'required':True,'client':['FixedHook']})
        cast.write_json(self.descriptor.parent/Path(s.MOON_CONTRACT).name,MOON)
        self.launch.update(suite='moon',maxTimeoutSeconds=180,sourceHead=self.head,checkoutSha=self.head,contract=s.MOON_CONTRACT,
                           contractSha256=s.digest(self.root/s.MOON_CONTRACT),descriptorSha256=s.digest(self.descriptor))
        self.launch['runtimeSha256'][str(self.descriptor.parent)]=s.digest(self.descriptor.parent)
        cast.write_json(self.launch_path,self.launch)


class MoonExportTests(MoonFixture):
    def test_fixed_export_and_contract_are_bound(self):
        self.assertEqual(s.load_contract(self.root,self.selected)['cases'],[s.MOON_CASE])
        self.assertEqual(len(s.validate_launch(self.launch,self.launch_path,self.root,self.selected)),64)
        changed=copy.deepcopy(MOON);changed['cases'].append('skipped')
        cast.write_json(self.root/s.MOON_CONTRACT,changed)
        with self.assertRaises(ValueError):s.load_contract(self.root,self.selected)

    def test_contract_requires_complete_ledger_and_accurate_proof_flags(self):
        for field,value in [('caseWitnesses',MOON['caseWitnesses'][:-1]),
                            ('proofFlags',{**MOON['proofFlags'],'serverReleaseFrameCorrespondenceVerified':True}),
                            ('proofFlags',{**MOON['proofFlags'],'pixelQualityReviewed':True})]:
            with self.subTest(field=field):
                changed=copy.deepcopy(MOON);changed[field]=value;cast.write_json(self.root/s.MOON_CONTRACT,changed)
                with self.assertRaises(ValueError):s.load_contract(self.root,self.selected)
    def test_moon_export_and_contract_cannot_restore_cast_timeout(self):
        for value in (None,181,900):
            with self.subTest(export=value):
                self.launch['maxTimeoutSeconds']=value
                with self.assertRaisesRegex(ValueError,'180-second'):s.validate_launch(self.launch,self.launch_path,self.root,self.selected)
        changed=copy.deepcopy(MOON);changed['limits']['maxTimeoutSeconds']=900;cast.write_json(self.root/s.MOON_CONTRACT,changed)
        with self.assertRaisesRegex(ValueError,'resource limits'):s.load_contract(self.root,self.selected)

    def test_owner_hooks_cannot_be_injected_twice(self):
        data=json.loads(self.descriptor.read_text());data['mixins'].append('crimson-moon-owner-gametest.mixins.json')
        cast.write_json(self.descriptor,data);self.launch['descriptorSha256']=s.digest(self.descriptor)
        with self.assertRaisesRegex(ValueError,'injected twice'):s.validate_launch(self.launch,self.launch_path,self.root,self.selected)

    def test_command_uses_fixed_original_profiles_and_bounded_properties(self):
        identity={**self.identity,'suite':'moon'}
        for profile in s.MOON_PROFILES:
            selected=s.selection('moon',profile,s.MOON_CASE,'reverse_oblique')
            args=s.command(self.launch,'host',self.root/'build/game',self.root/'build/ipc',identity,120,selected)
            self.assertEqual(args[args.index('--username')+1],s.MOON_PROFILES[profile][0])
            self.assertEqual(args[args.index('--uuid')+1],s.MOON_PROFILES[profile][1])
            self.assertIn('-Xmx2G',args)
            self.assertIn('-Dwildercord.moon.expectedSkin='+s.MOON_PROFILES[profile][2],args)
            self.assertIn('-Dwildercord.moon.observerAngle=reverse_oblique',args)


class MoonLifecycleTests(MoonFixture):
    # The processes below are controlled fakes. Numeric image proof is exercised separately.
    def options(self):
        return types.SimpleNamespace(launch=self.launch_path,output=self.root/'build/native/moon',
            suite='moon',profile='moon-wide',case=s.MOON_CASE,observer_angle='front_oblique',timeout=180,accepted_eula=None)

    def fake_run(self,processes,failure=None):
        options=self.options();launched=[]
        def spawn(argv,**kwargs):
            self.assertFalse(kwargs['shell']);self.assertEqual(len(argv),2)
            proof=json.loads((options.output/'launch-proof.json').read_text())
            self.assertEqual(kwargs['env']['WILDERCORD_MOON_RECEIPT_NONCE'],proof['nonce'])
            self.assertEqual(proof['case'],s.MOON_CASE);self.assertEqual(proof['observerAngle'],'front_oblique')
            kwargs['stdout'].write(b'moon owned diagnostic\n')
            if failure is not None and len(launched)==1:raise failure
            process=processes[len(launched)];launched.append(process);return process
        with patch.dict(os.environ,{'GITHUB_ACTIONS':'true','CI_MINECRAFT_EULA_ACCEPTED':'true'}), \
             patch.object(s,'ignored_output'),patch.object(s,'source_head',return_value=self.head), \
             patch.object(s,'validate_launch',return_value='a'*64),patch.object(s.subprocess,'Popen',side_effect=spawn):
            return s.run(options,self.root)

    def test_programmatic_moon_run_above_ceiling_starts_no_process(self):
        for timeout in (181,900):
            options=self.options();options.timeout=timeout
            with self.subTest(timeout=timeout),patch.object(s.subprocess,'Popen') as spawn,self.assertRaisesRegex(ValueError,'180 seconds'):
                s.run(options,self.root)
            spawn.assert_not_called();self.assertFalse(options.output.exists())

    def test_moon_preflight_failure_starts_no_process(self):
        contract=copy.deepcopy(MOON);contract['expectedCount']=2;cast.write_json(self.root/s.MOON_CONTRACT,contract)
        with patch.object(s,'ignored_output'),patch.object(s.subprocess,'Popen') as spawn,self.assertRaises(ValueError):
            s.run(self.options(),self.root)
        spawn.assert_not_called();report=json.loads((self.options().output/'result.json').read_text())
        self.assertEqual(report['status'],'failed');self.assertEqual(report['stage'],'preflight');self.assertEqual(report['processes'],{})

    def test_moon_peer_launch_failure_stops_only_owned_host(self):
        host,unrelated=cast.FakeProcess(1101),cast.FakeProcess(9999)
        with self.assertRaises(OSError):self.fake_run([host,unrelated],OSError('peer start failed'))
        self.assertTrue(host.terminated);self.assertFalse(unrelated.terminated)
        report=json.loads((self.options().output/'result.json').read_text())
        self.assertEqual(report['status'],'failed');self.assertEqual(set(report['processes']),{'host'})
        self.assertEqual((self.options().output/'host.log').read_text(),'moon owned diagnostic\n')

    def test_moon_zero_exit_without_image_evidence_remains_failed(self):
        with self.assertRaises(ValueError):self.fake_run([cast.FakeProcess(1101,0),cast.FakeProcess(1102,0)])
        report=json.loads((self.options().output/'result.json').read_text())
        self.assertEqual(report['status'],'failed');self.assertNotIn('releaseImageDamageOrderVerified',report)

    def test_moon_result_uses_causal_label_only_after_validation(self):
        with patch.object(s,'validate_witnesses',return_value=list(s.MOON_TERMINALS)) as validation:
            report=self.fake_run([cast.FakeProcess(1101,0),cast.FakeProcess(1102,0)])
        self.assertEqual(validation.call_args.args[-1]['suite'],'moon')
        self.assertEqual(report['status'],'passed');self.assertTrue(report['releaseImageDamageOrderVerified'])
        self.assertFalse(report['serverReleaseFrameCorrespondenceVerified']);self.assertFalse(report['pixelQualityReviewed'])


class MoonEvidenceTests(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup);self.base=Path(temp.name);self.ipc=self.base/'ipc';self.ipc.mkdir()
        self.selected=s.selection('moon','moon-wide',s.MOON_CASE,'front_oblique');self.pids={'host':111,'peer':222}
        self.jobs=[(role,types.SimpleNamespace(pid=pid)) for role,pid in self.pids.items()]
        self.identity={'nonce':'00000000-0000-4000-8000-000000000001','suite':'moon','sourceHead':'a'*40,'checkoutSha':'a'*40,
                       'prHeadSha':'','descriptorSha256':'b'*64,'hostUuid':s.MOON_PROFILES['moon-wide'][1],
                       'peerUuid':s.PROFILES['peer'][1],'runIdentity':'123-1-test','case':s.MOON_CASE,'expectedSkin':'wide','observerAngle':'front_oblique'}
        self.action={'actorEntity':'11','observerEntity':'22','actorUuid':self.identity['hostUuid'],'observerUuid':self.identity['peerUuid'],
                     'acceptedTick':'100','move':'19','windup':'10','recovery':'20'}
        self.values={};self.records={}
        self.write('case-00-ready','host',{key:self.action[key] for key in ('actorEntity','observerEntity','actorUuid','observerUuid')})
        self.write('case-00-accepted','host',self.action)
        self.write('case-00-release','host',{**self.action,'releaseTick':'110','completionOffset':'10','marks':'FULL,FULL,FULL,LOW'})
        release_sha=s.digest(self.ipc/'case-00-release.properties')
        for role in ('host','peer'):
            game=self.base/role;game.mkdir();png=game/(role+'.png');Image.new('RGBA',(2,2),'red' if role=='host' else 'blue').save(png)
            data=png.read_bytes();pixels=image_evidence(data);name=role+'_capture';frame={'activation':100,'move':19,'left':False,'master':False,'yaw':0.0,'pitch':0.0,'tilt':0.0,'footwork':False,'velocity':'n/a','pose':'a'*64};palette={'activation':100,'move':19,'phase':'ACTIVE','age':10.25,'classic':frame,'articulated':{**frame,'velocity':'NaN','pose':'ACTIVE:'+('b'*64)+':'+('c'*64)}}
            binding={'source':1,'state':2,'model':3,'item':4,'skinMaterial':5,'owner':11,'uuid':self.identity['hostUuid'],'skin':'WIDE','texture':'minecraft:skin','hand':'RIGHT','palette':palette,'matched':True}
            kinds=['view_submit','view_deferred','view_item'] if role=='host' else ['body_submit','body','world_item']
            passes=[{'kind':kind,'binding':{**copy.deepcopy(binding),'item':4 if role=='peer' or 'item' in kind else 0},'geometry':{'operation':'source_oracle','expected':[0,1,0],'actual':[0,1,0],'maxError':0,'matched':True},'segmented':True,'rigid':False} for kind in kinds]
            observation={'owner':self.identity['hostUuid'],'observerUuid':self.identity['hostUuid'] if role=='host' else self.identity['peerUuid'],
                         'observerEntity':'11' if role=='host' else '22','observerCoverage':'false' if role=='host' else 'true',
                         'pairedRole':role,'observerAngle':'front_oblique','releaseReceiptSha256':release_sha,'serverReleaseTick':'110',
                         'releaseReadSequence':'6','sourceFrameSequence':'7','actualSourceAge':'10.25','connectedSkinTexture':'minecraft:skin','connectedSkinModel':'WIDE',
                         'originalSkinMaterialIdentity':'5','submittedSkinMaterialIdentity':'5',
                         'ordinaryHandAdmission':'true','handEquipKnown':'true','handSameItem':'true','handEquipping':'false','worldHandEligible':'true'}
            record={'schemaVersion':3,'launchNonce':self.identity['nonce'],'verified':True,'failures':[],'scopeCleanupVerified':True,
                    'releaseObservedBeforeSource':True,'serverReleaseFrameCorrespondenceVerified':False,'pixelQualityReviewed':False,
                    'expected':{'name':name,'view':'fp' if role=='host' else 'remote','owner':11,'uuid':self.identity['hostUuid'],'activation':100,
                                'mode':'articulated','hand':'RIGHT','skin':'wide','armor':True,'shell':True,'phase':'ACTIVE','age':10},
                    'copy':{'observations':observation,'passes':passes,'extractSequence':8,'renderSequence':9,'copySequence':10,'width':2,'height':2},
                    'image':{'relativeImagePath':png.name,'pngBytes':len(data),'pngSha256':hashlib.sha256(data).hexdigest(),'decodedPixels':pixels},'callbackPixels':pixels}
            self.records[role]=record
            fields={**self.action,'view':record['expected']['view'],'screenshotName':name,'receiptRelativePath':role+'.json','pngRelativePath':png.name,
                    'pngSha256':record['image']['pngSha256'],'callbackPixelSha256':pixels['sha256'],'actualSourceAge':'10.25','releaseReceiptSha256':release_sha,
                    'scopeCleanupVerified':'true','releaseObservedBeforeSource':'true','serverReleaseFrameCorrespondenceVerified':'false','releaseImageDamageOrderVerified':'false'}
            self.write(role+'-case-00-observed',role,fields)
        self.rebind()
        for name,role in s.MOON_TERMINALS.items():self.write(name[:-11],role,{**self.action,'serverReleaseFrameCorrespondenceVerified':'false','releaseImageDamageOrderVerified':'true','releaseTick':'110','firstDamageTick':'115','firstDamageAgeTicks':'15','firstDamageDelayTicks':'5'})

    def write(self,name,role,fields):
        self.values[name]={**self.identity,'role':role,'pid':str(self.pids[role]),'cases':s.MOON_CASE,'serverReleaseFrameCorrespondenceVerified':'false',**fields};self.flush(name)
    def flush(self,name):
        (self.ipc/(name+'.properties')).write_text(''.join(key+'='+str(value)+'\n' for key,value in self.values[name].items()),encoding='iso-8859-1')
    def rebind(self):
        passed={**self.action,'releaseTick':'110','completionOffset':'10','firstDamageTick':'115','firstDamageAgeTicks':'15','firstDamageDelayTicks':'5',
                'serverReleaseFrameCorrespondenceVerified':'false','releaseImageDamageOrderVerified':'true'}
        for role in ('host','peer'):
            path=self.base/role/(role+'.json');path.write_text(json.dumps(self.records[role]));v=self.values[role+'-case-00-observed'];v['receiptSha256']=s.digest(path);self.flush(role+'-case-00-observed')
            passed[role+'ObservedBeforeFirstDamage']='true';passed[role+'ObservedWitnessSha256']=s.digest(self.ipc/(role+'-case-00-observed.properties'))
            for field in ('pngSha256','callbackPixelSha256','receiptSha256'):passed[role+field[0].upper()+field[1:]]=v[field]
        self.write('case-00-passed','host',passed)
    def verify(self):return s.validate_witnesses(self.ipc,self.identity,[s.MOON_CASE],self.jobs,self.selected)

    def test_complete_causal_interval_is_accepted_without_exact_frame_claim(self):self.assertEqual(self.verify(),list(s.MOON_TERMINALS))
    def test_wrong_role_pid_nonce_and_missing_terminal_are_rejected(self):
        for key,value in [('pid','111'),('nonce','stale'),('cases','another_case')]:
            with self.subTest(key=key):
                old=self.values['peer-moon-passed'][key];self.values['peer-moon-passed'][key]=value;self.flush('peer-moon-passed')
                with self.assertRaises(ValueError):self.verify()
                self.values['peer-moon-passed'][key]=old;self.flush('peer-moon-passed')
        (self.ipc/'peer-disconnected.properties').unlink()
        with self.assertRaises(ValueError):self.verify()
    def test_stale_geometry_frame_observer_item_and_skin_are_rejected_even_if_rehashed(self):
        mutations=[lambda r:r['copy']['passes'][0]['geometry']['actual'].__setitem__(0,1),
                   lambda r:r['copy']['passes'][0]['binding']['palette'].__setitem__('activation',99),
                   lambda r:r['copy']['passes'][-1]['binding'].__setitem__('item',0),
                   lambda r:r['copy']['observations'].__setitem__('observerUuid',self.identity['hostUuid']),
                   lambda r:r['copy']['passes'][0]['binding'].__setitem__('texture','other:skin'),
                   lambda r:r['copy']['passes'].pop(),
                   lambda r:r['copy']['observations'].__setitem__('sourceFrameSequence','5'),
                   lambda r:r.__setitem__('scopeCleanupVerified',False)]
        original=copy.deepcopy(self.records['peer'])
        for mutate in mutations:
            with self.subTest(mutation=mutate):
                self.records['peer']=copy.deepcopy(original);mutate(self.records['peer']);self.rebind()
                with self.assertRaises(ValueError):self.verify()
        self.records['peer']=original;self.rebind();self.verify()
    def test_retimed_phase_and_nonfinite_geometry_are_rejected(self):
        original=copy.deepcopy(self.records['host'])
        for value in [9.999,11,float('nan'),float('inf')]:
            with self.subTest(age=value):
                self.values['host-case-00-observed']['actualSourceAge']=str(value);self.rebind()
                with self.assertRaises(ValueError):self.verify()
        self.values['host-case-00-observed']['actualSourceAge']='10.25'
        self.records['host']['copy']['passes'][0]['geometry']['actual'][0]=float('nan');self.rebind()
        with self.assertRaises(ValueError):self.verify()
    def test_png_tampering_or_callback_substitution_is_rejected(self):
        p=self.base/'host/host.png';data=p.read_bytes();p.write_bytes(data+b'extra')
        with self.assertRaises(ValueError):self.verify()
        p.write_bytes(data);self.records['host']['callbackPixels']['sha256']='a'*64;self.rebind()
        with self.assertRaises(ValueError):self.verify()
    def test_exact_frame_overclaims_and_missing_first_damage_order_are_rejected(self):
        for field,value in [('serverReleaseFrameCorrespondenceVerified','true'),('releaseImageDamageOrderVerified','false'),
                            ('peerObservedBeforeFirstDamage','false'),('firstDamageAgeTicks','16'),('firstDamageDelayTicks','6')]:
            old=self.values['case-00-passed'][field];self.values['case-00-passed'][field]=value;self.flush('case-00-passed')
            with self.subTest(field=field),self.assertRaises(ValueError):self.verify()
            self.values['case-00-passed'][field]=old;self.flush('case-00-passed')
        self.verify()
    def test_every_witness_keeps_exact_frame_unverified(self):
        for name in self.values:
            with self.subTest(witness=name):
                self.values[name]['serverReleaseFrameCorrespondenceVerified']='true';self.flush(name)
                with self.assertRaises(ValueError):self.verify()
                self.values[name]['serverReleaseFrameCorrespondenceVerified']='false';self.flush(name)
        self.verify()
    def test_terminal_damage_fields_cannot_diverge(self):
        for filename in s.MOON_TERMINALS:
            name=filename[:-11]
            for field,value in [('releaseTick','111'),('firstDamageTick','116'),('firstDamageAgeTicks','14'),('firstDamageDelayTicks','4')]:
                with self.subTest(witness=name,field=field):
                    old=self.values[name][field];self.values[name][field]=value;self.flush(name)
                    with self.assertRaises(ValueError):self.verify()
                    self.values[name][field]=old;self.flush(name)
        self.verify()
    def test_source_frame_must_bridge_release_read_to_extraction_for_each_role(self):
        for role in ('host','peer'):
            original=copy.deepcopy(self.records[role])
            self.records[role]['copy']['observations'].update(releaseReadSequence='10',sourceFrameSequence='11');self.rebind()
            with self.subTest(role=role),self.assertRaisesRegex(ValueError,'one strict sequence'):self.verify()
            self.records[role]=original;self.rebind()
        self.verify()

    def test_complete_nested_frame_schemas_bind_both_roles(self):
        mutations=[lambda p:p.update(classic=None,articulated=None),
                   lambda p:[p[k].update(activation=99) for k in ('classic','articulated')]]
        for kind in ('classic','articulated'):
            for key,value in [('activation',99),('move',0),('left',True),('master',True),('yaw',float('nan')),
                              ('pitch','0'),('tilt',float('inf')),('footwork','false'),('velocity','unknown'),('pose','')]:
                mutations.append(lambda p,kind=kind,key=key,value=value:p[kind].__setitem__(key,value))
            mutations.append(lambda p,kind=kind:p[kind].pop('pitch'))
        for role in ('host','peer'):
            original=copy.deepcopy(self.records[role])
            for index,mutate in enumerate(mutations):
                with self.subTest(role=role,mutation=index):
                    self.records[role]=copy.deepcopy(original)
                    for render in self.records[role]['copy']['passes']:mutate(render['binding']['palette'])
                    self.rebind()
                    with self.assertRaises(ValueError):self.verify()
            self.records[role]=original;self.rebind()
        self.verify()

    def test_legitimate_classic_and_articulated_aim_identities_need_not_equal(self):
        for record in self.records.values():
            for render in record['copy']['passes']:
                render['binding']['palette']['classic'].update(yaw=2.5,pitch=1.0,tilt=.2)
                render['binding']['palette']['articulated'].update(yaw=2.0,pitch=1.2,velocity='0x0.0p0')
        self.rebind();self.verify()

    def test_remote_item_identity_cannot_change_between_passes(self):
        self.records['peer']['copy']['passes'][-1]['binding']['item']=999;self.rebind()
        with self.assertRaisesRegex(ValueError,'pass identity changed'):self.verify()

    def test_exact_singleton_pass_kinds_and_articulated_backend_are_required(self):
        for role in ('host','peer'):
            original=copy.deepcopy(self.records[role])
            mutations=[lambda r:r['copy']['passes'].append(copy.deepcopy(r['copy']['passes'][0])),
                       lambda r:r['copy']['passes'][0].__setitem__('kind','unknown'),
                       lambda r:[p.update(segmented=False,rigid=True) for p in r['copy']['passes']]]
            for index,mutate in enumerate(mutations):
                with self.subTest(role=role,mutation=index):
                    self.records[role]=copy.deepcopy(original);mutate(self.records[role]);self.rebind()
                    with self.assertRaises(ValueError):self.verify()
            self.records[role]=original;self.rebind()
        self.verify()

    def test_original_skin_material_and_eligible_hand_cannot_be_absent(self):
        for role in ('host','peer'):
            original=copy.deepcopy(self.records[role])
            def remove_texture(r):
                r['copy']['observations'].pop('connectedSkinTexture')
                for render in r['copy']['passes']:render['binding'].pop('texture')
            mutations=[remove_texture,
                       lambda r:r['copy']['observations'].__setitem__('connectedSkinTexture',''),
                       lambda r:r['copy']['observations'].pop('connectedSkinModel'),
                       lambda r:r['copy']['observations'].pop('originalSkinMaterialIdentity'),
                       lambda r:r['copy']['observations'].__setitem__('submittedSkinMaterialIdentity','999'),
                       lambda r:r['copy']['observations'].__setitem__('ordinaryHandAdmission' if role=='host' else 'worldHandEligible','false')]
            for index,mutate in enumerate(mutations):
                with self.subTest(role=role,mutation=index):
                    self.records[role]=copy.deepcopy(original);mutate(self.records[role]);self.rebind()
                    with self.assertRaises(ValueError):self.verify()
            self.records[role]=original;self.rebind()
        self.verify()

    def test_every_terminal_binds_the_complete_accepted_action(self):
        for filename in s.MOON_TERMINALS:
            name=filename[:-11]
            for field,value in self.action.items():
                with self.subTest(witness=name,field=field):
                    self.values[name][field]='wrong';self.flush(name)
                    with self.assertRaisesRegex(ValueError,'accepted Moon action'):self.verify()
                    self.values[name][field]=value;self.flush(name)
        self.verify()

    def test_image_paths_cannot_escape_role_directory(self):
        self.values['peer-case-00-observed']['receiptRelativePath']='../host/host.json';self.flush('peer-case-00-observed')
        with self.assertRaises(ValueError):self.verify()


if __name__=='__main__':unittest.main()
