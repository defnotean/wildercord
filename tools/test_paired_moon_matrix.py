"""Fixed matrix adversarial controls. No Minecraft or Gradle process is started."""
import contextlib
import copy
import json
import os
from pathlib import Path
import signal
import tempfile
import types
import unittest
from unittest.mock import patch
import uuid

from native import run_paired_matrix as m
from native import launch_two_clients as s


class MatrixTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory(); self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.options = types.SimpleNamespace(output=self.root / 'build/native/matrix', total_timeout=1380, accepted_eula=None)
        self.identity = {'sourceHead': 'a'*40, 'checkoutSha': 'a'*40, 'prHeadSha': '',
                         'runId': '123', 'runAttempt': '1', 'job': 'connected-combat-native', 'repository': 'test/repo', 'eventName': 'push'}
        self.events = []
        self.locked = False
        self.clock = 0

    @contextlib.contextmanager
    def lock(self, root):
        self.assertFalse(self.locked)
        self.locked = True
        try: yield
        finally: self.locked = False

    def export(self, root, suite, path, log, deadline):
        self.assertTrue(self.locked)
        self.events.append(('export', suite))
        descriptor = self.root / 'build/descriptor.json'
        descriptor.parent.mkdir(parents=True, exist_ok=True)
        descriptor.write_text('{}')
        path.write_text(json.dumps({'descriptor': str(descriptor)}))
        self.clock += 100

    def child(self, options, root, *, already_locked):
        self.assertTrue(already_locked and self.locked)
        self.events.append(('child', options.suite, options.profile, options.observer_angle, options.timeout))
        options.output.mkdir()
        (options.output / 'result.json').write_text('{}')
        self.clock += 100

    def verified(self, base, group, identity, nonces):
        self.assertTrue(self.locked)
        nonce = str(uuid.uuid4()); nonces.add(nonce)
        return {'nonce': nonce, 'runIdentity': '123-1-'+nonce}

    @contextlib.contextmanager
    def mocks(self):
        with patch.object(s, 'ignored_output'), patch.object(s, 'accepted_eula'), patch.object(s, 'validate_launch'), \
             patch.object(s, 'supervisor_lock', self.lock), patch.object(s, 'run', side_effect=self.child) as child, \
             patch.object(m, 'provenance', return_value=self.identity), patch.object(m, 'export', side_effect=self.export), \
             patch.object(m, 'validate_group', side_effect=self.verified), patch.object(m, 'elapsed_guard', contextlib.nullcontext), \
             patch.object(m.time, 'monotonic', side_effect=lambda: self.clock), \
             patch.object(m, 'remaining', side_effect=lambda deadline: self.remaining(deadline)):
            yield child

    def remaining(self, deadline):
        s.require(deadline - self.clock >= 1, 'Paired matrix elapsed budget exhausted')
        return deadline - self.clock

    def report(self):
        return json.loads((self.options.output / 'matrix-result.json').read_text())

    def test_all_cast_cases_precede_four_exact_views_without_summing_maxima(self):
        # 900 + 4*180 exceeds 1380, but actual elapsed execution fits.
        self.assertGreater(900 + 4*180, self.options.total_timeout)
        with self.mocks(): result = m.run(self.options, self.root)
        self.assertEqual(result['status'], 'passed')
        self.assertEqual(self.events, [('export','cast-receipt'), ('child','cast-receipt','aura',None,900),
            ('export','moon'), ('child','moon','moon-wide','front_oblique',180),
            ('child','moon','moon-wide','reverse_oblique',180), ('child','moon','moon-slim','front_oblique',180),
            ('child','moon','moon-slim','reverse_oblique',180)])
        self.assertEqual(len(result['groups']),5)
        self.assertTrue(result['releaseImageDamageOrderVerified'])
        self.assertFalse(result['serverReleaseFrameCorrespondenceVerified'])
        self.assertFalse(self.locked)

    def test_actual_remaining_budget_clips_later_ceiling(self):
        self.options.total_timeout = 680
        with self.mocks(), self.assertRaisesRegex(ValueError, 'elapsed budget'): m.run(self.options,self.root)
        self.assertEqual(self.events[-1], ('child','moon','moon-slim','reverse_oblique',80))
        self.assertEqual(self.report()['status'],'failed')

    def test_cast_failure_stops_before_any_moon_and_preserves_missing_ledger(self):
        with self.mocks() as child:
            child.side_effect = RuntimeError('cast failure')
            with self.assertRaisesRegex(RuntimeError,'cast failure'): m.run(self.options,self.root)
        self.assertEqual(self.events,[('export','cast-receipt')])
        report=self.report(); self.assertEqual([g['status'] for g in report['groups']], ['failed']+['not_started']*4)
        self.assertIn('cast failure',report['error'])
        self.assertFalse(self.locked)

    def test_moon_failure_preserves_prior_success_and_stops_later_groups(self):
        with self.mocks() as child:
            calls=0
            def fail(options,root,**kw):
                nonlocal calls
                calls+=1
                if calls==3: raise TimeoutError('Moon deadline')
                return self.child(options,root,**kw)
            child.side_effect=fail
            with self.assertRaisesRegex(TimeoutError,'Moon deadline'):m.run(self.options,self.root)
        self.assertEqual([g['status'] for g in self.report()['groups']], ['passed','passed','failed','not_started','not_started'])

    def test_validation_failure_and_sigterm_do_not_advance_or_claim_success(self):
        for error in (ValueError('missing receipt'), InterruptedError('SIGTERM')):
            with self.subTest(error=error):
                self.options.output=self.root/'build/native'/str(uuid.uuid4())
                self.clock=0;self.events=[]
                with self.mocks(),patch.object(m,'validate_group',side_effect=error),self.assertRaises(type(error)):
                    m.run(self.options,self.root)
                self.assertEqual(self.report()['status'],'failed')
                self.assertEqual(len([e for e in self.events if e[0]=='child']),1)

    def test_dirty_source_preflight_persists_failure_before_any_export(self):
        with self.mocks(),patch.object(m,'provenance',side_effect=ValueError('dirty source')),self.assertRaises(ValueError):
            m.run(self.options,self.root)
        self.assertEqual(self.events,[])
        self.assertEqual(self.report()['status'],'failed')

    def test_cli_has_no_arbitrary_selectors_or_extended_timeout(self):
        for args in (['--suite','moon'],['--case','other'],['--total-timeout','1381'],['--command','sh']):
            with self.subTest(args=args),patch('sys.stderr'),self.assertRaises(SystemExit):m.parse_args(args)

    def test_export_timeout_terminates_owned_process_group(self):
        process=types.SimpleNamespace(pid=8123,wait=unittest.mock.Mock(side_effect=[TimeoutError('export timeout'),0]))
        with patch.object(m.subprocess,'Popen',return_value=process),patch.object(m.os,'killpg') as kill, \
             patch.object(m,'remaining',return_value=1), self.assertRaises(TimeoutError):
            m.export(self.root,'moon',self.root/'launch.json',self.root/'export.log',10)
        self.assertEqual(kill.call_args_list,[unittest.mock.call(8123,signal.SIGTERM),unittest.mock.call(8123,signal.SIGKILL)])

    def test_real_alarm_handler_is_one_shot_and_restored(self):
        previous=signal.getsignal(signal.SIGALRM)
        with self.assertRaises(TimeoutError):
            with m.elapsed_guard(60):signal.raise_signal(signal.SIGALRM)
        self.assertEqual(signal.getsignal(signal.SIGALRM),previous)
        self.assertEqual(signal.getitimer(signal.ITIMER_REAL),(0.0,0.0))


class ProvenanceTests(unittest.TestCase):
    def test_ci_requires_exact_head_run_job_and_pr_event(self):
        identity={'GITHUB_ACTIONS':'true','GITHUB_SHA':'a'*40,'GITHUB_RUN_ID':'123','GITHUB_RUN_ATTEMPT':'1',
                  'GITHUB_JOB':'connected-combat-native','GITHUB_REPOSITORY':'org/repo','GITHUB_EVENT_NAME':'push'}
        with patch.object(s,'source_head',return_value='a'*40):
            self.assertEqual(m.provenance(Path('/unused'),identity)['checkoutSha'],'a'*40)
            for key,value in [('GITHUB_SHA','b'*40),('GITHUB_RUN_ID',''),('GITHUB_RUN_ATTEMPT','0'),('GITHUB_JOB','other'),('GITHUB_REPOSITORY',''),('GITHUB_EVENT_NAME','workflow_dispatch')]:
                with self.subTest(key=key),self.assertRaises(ValueError):m.provenance(Path('/unused'),{**identity,key:value})
            with tempfile.TemporaryDirectory() as td:
                event=Path(td)/'event.json';event.write_text(json.dumps({'pull_request':{'head':{'sha':'b'*40}}}))
                env={**identity,'GITHUB_EVENT_NAME':'pull_request','WILDERCORD_PR_HEAD_SHA':'b'*40,'GITHUB_EVENT_PATH':str(event)}
                self.assertEqual(m.provenance(Path('/unused'),env)['prHeadSha'],'b'*40)
                env['WILDERCORD_PR_HEAD_SHA']='c'*40
                with self.assertRaises(ValueError):m.provenance(Path('/unused'),env)

    def test_paths_refuse_symlink_or_traversal(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td);(root/'link').symlink_to('/tmp')
            for value in ('../x','link/x','/tmp/x'):
                with self.subTest(value=value),self.assertRaises(ValueError):m.safe_path(root,value)


class GroupValidationTests(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup);self.base=Path(temp.name)
        self.identity={'sourceHead':'a'*40,'checkoutSha':'a'*40,'prHeadSha':'','runId':'123','runAttempt':'1','job':'connected-combat-native'}
        self.group=m.GROUPS[1];selected=m.selected(self.group)
        self.result={**self.identity,**{k:selected[k] for k in ('suite','profile','cases','case','expectedSkin','observerAngle')},
            'status':'passed','nonce':'00000000-0000-4000-8000-000000000001','runIdentity':'123-1-00000000-0000-4000-8000-000000000001',
            'descriptorSha256':'b'*64,'launchSha256':'c'*64,'heapMiBPerJvm':2048,'timeoutSeconds':180,
            'hostUuid':selected['profiles']['host'][1],'peerUuid':selected['profiles']['peer'][1],
            'ci':{'GITHUB_RUN_ID':'123','GITHUB_RUN_ATTEMPT':'1','GITHUB_JOB':'connected-combat-native'},
            'processes':{'host':{'pid':111,'exit':0},'peer':{'pid':222,'exit':0}},'witnesses':list(s.MOON_TERMINALS),
            'serverReleaseFrameCorrespondenceVerified':False,'releaseImageDamageOrderVerified':True,'pixelQualityReviewed':False}
        self.flush()
    def flush(self):
        (self.base/'result.json').write_text(json.dumps(self.result))
        (self.base/'launch-proof.json').write_text(json.dumps({**self.result,'status':'started'}))
    def test_missing_cases_provenance_nonce_process_or_false_proof_rejected(self):
        original=copy.deepcopy(self.result)
        changes=[('cases',[]),('observerAngle','wrong'),('expectedSkin','slim'),('nonce','stale'),('checkoutSha','b'*40),
                 ('timeoutSeconds',181),('heapMiBPerJvm',4096),('serverReleaseFrameCorrespondenceVerified',True),
                 ('pixelQualityReviewed',True),('releaseImageDamageOrderVerified',False),('witnesses',[]),
                 ('processes',{'host':{'pid':111,'exit':0}}),('ci',{})]
        with patch.object(s,'validate_witnesses',return_value=list(s.MOON_TERMINALS)):
            self.assertEqual(m.validate_group(self.base,self.group,self.identity,set())['status'],'passed')
            for key,value in changes:
                with self.subTest(key=key):
                    self.result={**original,key:value};self.flush()
                    with self.assertRaises((ValueError,KeyError)):m.validate_group(self.base,self.group,self.identity,set())
            self.result=original;self.flush()
            with self.assertRaisesRegex(ValueError,'nonce'):m.validate_group(self.base,self.group,self.identity,{original['nonce']})

    def test_nonzero_exit_or_reused_pid_rejected(self):
        self.result['processes']['peer']['exit']=1;self.flush()
        with self.assertRaises(ValueError):m.validate_group(self.base,self.group,self.identity,set())
        self.result['processes']['peer']={'pid':111,'exit':0};self.flush()
        with self.assertRaises(ValueError):m.validate_group(self.base,self.group,self.identity,set())

    def test_launch_proof_missing_nonce_rejected_even_if_result_passed(self):
        proof={**self.result,'status':'started'};proof.pop('nonce');(self.base/'launch-proof.json').write_text(json.dumps(proof))
        with self.assertRaisesRegex(ValueError,'Launch proof'):m.validate_group(self.base,self.group,self.identity,set())


class CleanupDeadlineTests(unittest.TestCase):
    def test_global_alarm_cannot_interrupt_owned_jvm_cleanup_or_result(self):
        import test_moon_two_client_supervisor as existing
        import test_two_client_supervisor as cast
        fixture=existing.MoonLifecycleTests();fixture.setUp();self.addCleanup(fixture.doCleanups)
        supervisor=existing.s;original_run=supervisor.run;original_stop=supervisor.stop_owned
        previous=signal.getsignal(signal.SIGALRM)
        def alarm(signum,frame):raise TimeoutError('outer deadline')
        signal.signal(signal.SIGALRM,alarm)
        self.addCleanup(signal.signal,signal.SIGALRM,previous)
        observed=[]
        def cleanup(jobs):
            observed.append(signal.getsignal(signal.SIGALRM))
            signal.raise_signal(signal.SIGALRM)
            return original_stop(jobs)
        with patch.object(supervisor,'run',side_effect=lambda options,root:original_run(options,root,already_locked=True)), \
             patch.object(supervisor,'stop_owned',side_effect=cleanup), \
             patch.object(supervisor,'validate_witnesses',return_value=list(supervisor.MOON_TERMINALS)):
            result=fixture.fake_run([cast.FakeProcess(1101,0),cast.FakeProcess(1102,0)])
        self.assertEqual(observed,[signal.SIG_IGN])
        self.assertEqual(signal.getsignal(signal.SIGALRM),alarm)
        self.assertTrue((fixture.options().output/'result.json').is_file())
        self.assertEqual(result['status'],'passed')

    def test_export_cleanup_ignores_global_alarm_then_restores_it(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td);waits=0
            def wait(timeout):
                nonlocal waits
                waits+=1
                if waits==2:
                    self.assertEqual(signal.getsignal(signal.SIGALRM),signal.SIG_IGN)
                    signal.raise_signal(signal.SIGALRM)
                return 0
            process=types.SimpleNamespace(pid=8123,wait=wait)
            before=signal.getsignal(signal.SIGALRM)
            with patch.object(m.subprocess,'Popen',return_value=process),patch.object(m.os,'killpg'),patch.object(m,'remaining',return_value=5):
                m.export(root,'moon',root/'launch.json',root/'export.log',10)
            self.assertEqual(signal.getsignal(signal.SIGALRM),before)


if __name__=='__main__':unittest.main()
