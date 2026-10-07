"""Deterministic relay transport tests; no real sockets, ports, JVMs, or sleeps.

The fakes implement nonblocking recv fragmentation, partial writes, selector
readiness and monotonic waits. They do not replace native gameplay evidence.
"""
from collections import deque
from dataclasses import FrozenInstanceError
import errno
import hashlib
import json
import random
import selectors
import socket
from types import SimpleNamespace
import unittest

from native.stone_hinge_relay import (
    BackendIdentity, DIRECTION_NAMES, Limits, LOOPBACK, MAX_EVIDENCE_BYTES,
    PROFILES, RelayError, StoneHingeRelay, reserve_listeners,
)


class Clock:
    def __init__(self):
        self.now = 10.0

    def __call__(self):
        return self.now

    def advance(self, seconds):
        self.now += seconds


class FakeSocket:
    next_fd = 100

    def __init__(self, clock, family=socket.AF_INET, kind=socket.SOCK_STREAM):
        self.clock = clock
        self.family = family
        self.kind = kind
        self.fd = FakeSocket.next_fd
        FakeSocket.next_fd += 1
        self.local = (LOOPBACK, 40000 + self.fd)
        self.remote = None
        self.listening = False
        self.incoming = deque()
        self.accepts = deque()
        self.written = bytearray()
        self.send_sizes = deque()
        self.max_send = 65536
        self.writable = True
        self.connect_result = 0
        self.connect_error = 0
        self.closed = 0
        self.shutdowns = []
        self.shutdown_attempts = 0
        self.read_sizes = []
        self.send_times = []
        self.blocking = None
        self.shutdown_error = None
        self.send_seconds = 0.0

    def fileno(self):
        return self.fd

    def getsockname(self):
        return self.local

    def getpeername(self):
        return self.remote

    def getsockopt(self, level, option):
        assert level == socket.SOL_SOCKET
        return {socket.SO_TYPE: self.kind, socket.SO_ACCEPTCONN: self.listening,
                socket.SO_ERROR: self.connect_error}[option]

    def setblocking(self, blocking):
        self.blocking = blocking

    def bind(self, address):
        assert address == (LOOPBACK, 0)

    def listen(self, backlog):
        assert backlog == 1
        self.listening = True

    def accept(self):
        if not self.accepts:
            raise BlockingIOError()
        client = self.accepts.popleft()
        client.local = self.local
        client.remote = (LOOPBACK, 50000)
        return client, client.remote

    def connect_ex(self, address):
        assert address == (LOOPBACK, 25565), "Only the validated literal backend is allowed"
        self.remote = address
        return self.connect_result

    def recv(self, count):
        self.read_sizes.append(count)
        if not self.incoming:
            raise BlockingIOError()
        first = self.incoming.popleft()
        if isinstance(first, BaseException):
            raise first
        if len(first) > count:
            self.incoming.appendleft(first[count:])
        return first[:count]

    def send(self, data):
        result = self.send_sizes.popleft() if self.send_sizes else self.max_send
        if isinstance(result, BaseException):
            raise result
        if not self.writable:
            raise BlockingIOError()
        count = min(result, len(data))
        self.written.extend(data[:count])
        self.send_times.append(self.clock())
        self.clock.advance(self.send_seconds)
        return count

    def shutdown(self, how):
        assert how == socket.SHUT_WR
        self.shutdown_attempts += 1
        if self.shutdown_error:
            raise self.shutdown_error
        self.shutdowns.append((self.clock(), bytes(self.written)))

    def close(self):
        self.closed += 1


class Factory:
    def __init__(self, clock):
        self.clock = clock
        self.sockets = []
        self.configure = lambda sock: None

    def __call__(self, family, kind):
        sock = FakeSocket(self.clock, family, kind)
        self.configure(sock)
        self.sockets.append(sock)
        return sock


class FakeSelector:
    def __init__(self, clock):
        self.clock = clock
        self.registered = {}
        self.waits = []
        self.closed = 0

    def register(self, sock, events):
        assert sock not in self.registered
        self.registered[sock] = events

    def modify(self, sock, events):
        assert sock in self.registered
        self.registered[sock] = events

    def unregister(self, sock):
        del self.registered[sock]

    def select(self, timeout):
        assert timeout >= 0
        self.waits.append(timeout)
        result = []
        for sock, events in self.registered.items():
            assert not sock.closed
            read = bool(sock.accepts if sock.listening else sock.incoming)
            mask = (selectors.EVENT_READ if read else 0) | (selectors.EVENT_WRITE if sock.writable else 0)
            if mask & events:
                result.append((SimpleNamespace(fileobj=sock), mask & events))
        if not result:
            self.clock.advance(timeout)
        return result

    def close(self):
        self.closed += 1
        self.registered.clear()


class Harness:
    def __init__(self, profile="transparent", limits=None, connect_result=0):
        self.clock = Clock()
        self.factory = Factory(self.clock)
        self.listeners = reserve_listeners(socket_factory=self.factory)
        self.selector = FakeSelector(self.clock)
        ready = {"nonce": "nonce-123", "pid": "1234", "port": "25565", "role": "host", "suite": "stone-hinge-peer"}
        self.identity = BackendIdentity.from_ready(ready, expected_nonce="nonce-123", expected_host_pid=1234)
        self.relay = StoneHingeRelay(self.identity, profile, listeners=self.listeners,
                                    limits=limits, clock=self.clock, socket_factory=self.factory,
                                    selector_factory=lambda: self.selector)
        self.clients = {role: FakeSocket(self.clock) for role in ("owner", "observer")}
        def configure(sock):
            sock.connect_result = connect_result
            sock.writable = connect_result == 0
        self.factory.configure = configure
        for role, client in self.clients.items():
            self.listeners[role].accepts.append(client)
        self.relay.pump(0)
        self.backends = {role: pair.backend for role, pair in self.relay._pairs.items()}

    def fill(self, data=b"traffic", eof=True):
        for sock in (*self.clients.values(), *self.backends.values()):
            sock.incoming.append(data)
            if eof:
                sock.incoming.append(b"")

    def finish(self, iterations=2000, advance=0.001):
        for _ in range(iterations):
            if self.relay.pump(0):
                return self.relay.assert_complete()
            self.clock.advance(advance)
        raise AssertionError("Fake relay did not complete")


class RelayTests(unittest.TestCase):
    def test_endpoint_proof_survives_owned_socket_cleanup(self):
        h = Harness()
        h.fill(); h.finish()
        before = h.relay.assert_complete()["connections"]
        for role in ("owner", "observer"):
            self.assertEqual(before[role]["clientLocal"], list(h.listeners[role].local))
            self.assertEqual(before[role]["clientRemote"], list(h.clients[role].remote))
            self.assertEqual(before[role]["backendLocal"], list(h.backends[role].local))
            self.assertEqual(before[role]["backendRemote"], [LOOPBACK, 25565])
            self.assertTrue(before[role]["connected"])
        h.relay.close()
        self.assertEqual(h.relay.report()["connections"], before)


    def test_identity_rejects_wrong_or_malformed_receipt(self):
        ready = {"nonce": "nonce-123", "pid": "1234", "port": "25565", "role": "host", "suite": "stone-hinge-peer"}
        for key, value in (("nonce", "other"), ("pid", "1235"), ("pid", True), ("port", "0"),
                           ("port", "65536"), ("port", "example.org:25565"), ("role", "peer"),
                           ("suite", "cast-receipt")):
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                BackendIdentity.from_ready({**ready, key: value}, expected_nonce="nonce-123", expected_host_pid=1234)
        identity = BackendIdentity.from_ready(ready, expected_nonce="nonce-123", expected_host_pid=1234)
        with self.assertRaises(FrozenInstanceError):
            identity.backend_port = 1

    def test_only_fixed_immutable_profiles_and_literal_destinations(self):
        for profile, expected in PROFILES.items():
            h = Harness(profile)
            self.addCleanup(h.relay.close)
            self.assertEqual(tuple(h.relay.report()["delaysMs"].values()), expected)
            self.assertEqual({s.remote for s in h.backends.values()}, {(LOOPBACK, 25565)})
            self.assertEqual(set(h.relay.endpoints), {"owner", "observer"})
            self.assertTrue(all(s.blocking is False for s in h.relay._owned))
            with self.assertRaises(AttributeError):
                h.relay.profile = "transparent"
            with self.assertRaises(TypeError):
                PROFILES[profile] = (1, 1, 1, 1)

    def test_fragments_partial_writes_fifo_hashes_and_delays(self):
        for profile in PROFILES:
            with self.subTest(profile=profile):
                h = Harness(profile)
                self.addCleanup(h.relay.close)
                expected = {}
                for index, (name, direction) in enumerate(h.relay._directions.items()):
                    fragments = [bytes([index]) * 11, b"\x00\xffmiddle", b"tail"]
                    expected[name] = b"".join(fragments)
                    direction.source.incoming.extend([*fragments, b""])
                    direction.sink.max_send = 3
                    direction.sink.send_sizes.append(BlockingIOError())
                report = h.finish(advance=0.001)
                for name, direction in h.relay._directions.items():
                    evidence = report["directions"][name]
                    self.assertEqual(direction.sink.written, expected[name])
                    digest = hashlib.sha256(expected[name]).hexdigest()
                    self.assertEqual(evidence["readSha256"], digest)
                    self.assertEqual(evidence["writeSha256"], digest)
                    self.assertEqual(evidence["writeOffsetRange"], [0, len(expected[name])])
                    self.assertTrue(evidence["offsetsContiguous"])
                    self.assertGreater(evidence["writeCalls"], evidence["readChunks"])
                    self.assertGreaterEqual(evidence["residenceMinSeconds"] + 1e-12, direction.delay)
                    self.assertLessEqual(evidence["releaseLatenessMaxSeconds"], .25)
                    self.assertEqual(direction.sink.shutdowns[-1][1], expected[name])
                h.relay.close()
                self.assertEqual(h.relay.assert_complete()["status"], "passed")

    def test_random_fragmentation_preserves_binary_streams(self):
        rng = random.Random(384)
        h = Harness("asymmetric")
        self.addCleanup(h.relay.close)
        expected = {}
        for name, direction in h.relay._directions.items():
            body = rng.randbytes(32 * 1024)
            expected[name] = body
            while body:
                size = rng.randint(1, 2400)
                direction.source.incoming.append(body[:size])
                body = body[size:]
            direction.source.incoming.append(b"")
            direction.sink.max_send = 257
        report = h.finish(advance=.0005)
        for name, direction in h.relay._directions.items():
            self.assertEqual(direction.sink.written, expected[name])
            self.assertEqual(report["directions"][name]["readBytes"], len(expected[name]))
        self.assertLess(len(json.dumps(report).encode()), MAX_EVIDENCE_BYTES)

    def test_release_deadline_drives_selector_without_early_write(self):
        h = Harness("symmetric")
        self.addCleanup(h.relay.close)
        h.fill()
        h.relay.pump(0)
        self.assertFalse(any(sock.written for sock in h.relay._owned))
        h.relay.pump(1)
        self.assertAlmostEqual(h.selector.waits[-1], .05)
        self.assertFalse(any(sock.written for sock in h.relay._owned))
        h.relay.pump(0)
        self.assertTrue(h.relay.complete)
        h.relay.assert_complete()

    def test_reads_are_bounded_to_four_times_16k_per_direction(self):
        h = Harness("symmetric")
        self.addCleanup(h.relay.close)
        h.fill(b"x" * 200000, eof=False)
        h.relay.pump(0)
        for direction in h.relay._directions.values():
            self.assertEqual(direction.read_bytes, 4 * 16384)
            self.assertEqual(direction.source.read_sizes, [16384] * 4)

    def test_queue_byte_and_chunk_caps_pause_then_resume_reads(self):
        for limits in (Limits(queue_bytes=6, read_bytes=4), Limits(queue_chunks=2, read_bytes=2)):
            h = Harness("asymmetric", limits)
            self.addCleanup(h.relay.close)
            h.fill(b"abcdefghijk")
            h.relay.pump(0)
            before = h.relay._directions["owner_up"].read_bytes
            h.relay.pump(0)
            self.assertEqual(h.relay._directions["owner_up"].read_bytes, before)
            report = h.finish(advance=.005)
            for direction in report["directions"].values():
                self.assertLessEqual(direction["queueHighBytes"], limits.queue_bytes)
                self.assertLessEqual(direction["queueHighChunks"], limits.queue_chunks)
                self.assertGreater(direction["backpressureEvents"], 0)
                self.assertEqual(direction["readBytes"], 11)

    def test_total_queue_cap_pauses_and_recovers(self):
        h = Harness("symmetric", Limits(total_queue_bytes=6, read_bytes=4))
        self.addCleanup(h.relay.close)
        h.fill(b"12345")
        report = h.finish(advance=.005)
        self.assertLessEqual(report["queueHighBytes"], 6)
        self.assertEqual(report["totalReadBytes"], 20)
        self.assertEqual(report["totalQueuedBytes"], 0)

    def test_partial_write_prefix_stays_inside_physical_buffer_cap(self):
        h = Harness(limits=Limits(queue_bytes=8, read_bytes=8))
        self.addCleanup(h.relay.close)
        h.clients["owner"].incoming.extend([b"abcdefgh", b"tail", b""])
        h.backends["owner"].max_send = 1
        h.relay.pump(0)
        h.relay.pump(0)
        direction = h.relay._directions["owner_up"]
        self.assertEqual(direction.queued_bytes, 4)
        self.assertEqual(direction.buffered_bytes, 8)
        self.assertEqual(direction.read_bytes, 8)
        self.assertEqual(h.relay._capacity(direction), 0)
        self.assertLessEqual(h.relay.report()["queueHighBytes"], 8)

    def test_write_finishing_after_deadline_invalidates_even_if_queue_drained(self):
        h = Harness()
        h.fill()
        h.relay.pump(0)
        h.backends["owner"].send_seconds = .251
        with self.assertRaisesRegex(RelayError, "release_late"):
            h.relay.pump(0)
        evidence = h.relay.report()["directions"]["owner_up"]
        self.assertEqual(evidence["writtenBytes"], len(b"traffic"))
        self.assertGreater(evidence["releaseLatenessMaxSeconds"], .25)

    def test_transfer_caps_fail_closed_and_exact_limit_eof_is_allowed(self):
        for limits, data in ((Limits(stream_bytes=10), b"x" * 11),
                             (Limits(total_stream_bytes=12), b"xxxx")):
            h = Harness(limits=limits)
            h.fill(data)
            with self.assertRaisesRegex(RelayError, "stream_cap"):
                h.relay.pump(0)
            self.assertTrue(h.relay.closed)
            self.assertTrue(all(s.closed == 1 for s in h.relay._owned))
        h = Harness(limits=Limits(stream_bytes=10, total_stream_bytes=40))
        self.addCleanup(h.relay.close)
        h.fill(b"0123456789")
        self.assertEqual(h.finish()["totalReadBytes"], 40)

    def test_limits_cannot_be_disabled_or_expanded(self):
        for kwargs in ({"queue_bytes": 1024 * 1024 + 1}, {"lateness_seconds": .251},
                       {"connect_seconds": float("inf")}, {"reads_per_pump": 4.0},
                       {"queue_chunks": 0}, {"oldest_seconds": True}):
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                Limits(**kwargs)

    def test_stalled_sink_and_late_supervisor_invalidate_run(self):
        for stalled in (False, True):
            h = Harness("symmetric")
            h.fill(eof=False)
            h.relay.pump(0)
            if stalled:
                for sink in h.relay._owned:
                    sink.writable = False
            h.clock.advance(.301)
            with self.assertRaisesRegex(RelayError, "release_late"):
                h.relay.pump(0)
            self.assertEqual(h.relay.report()["status"], "failed")
            self.assertTrue(all(s.closed == 1 for s in h.relay._owned))

    def test_oldest_byte_and_backpressure_deadlines(self):
        for limits, code in ((Limits(oldest_seconds=.02), "oldest_timeout"),
                             (Limits(queue_bytes=4, backpressure_seconds=.02), "backpressure_timeout")):
            h = Harness("asymmetric", limits)
            h.fill(eof=False)
            h.relay.pump(0)
            h.clock.advance(.021)
            with self.assertRaisesRegex(RelayError, code):
                h.relay.pump(0)
            self.assertTrue(h.relay.closed)

    def test_connect_timeout_and_wrong_connected_endpoint(self):
        h = Harness(connect_result=errno.EINPROGRESS)
        h.clock.advance(5)
        with self.assertRaisesRegex(RelayError, "connect_timeout"):
            h.relay.pump(0)
        h = Harness(connect_result=errno.EINPROGRESS)
        h.backends["owner"].remote = ("127.0.0.2", 25565)
        h.backends["owner"].writable = True
        with self.assertRaisesRegex(RelayError, "connect"):
            h.relay.pump(0)

    def test_connect_error_fails_before_reading_game_bytes(self):
        h = Harness(connect_result=errno.EINPROGRESS)
        h.clients["owner"].incoming.append(b"must stay unread")
        h.backends["owner"].connect_error = errno.ECONNREFUSED
        h.backends["owner"].writable = True
        with self.assertRaisesRegex(RelayError, "connect"):
            h.relay.pump(0)
        self.assertEqual(h.clients["owner"].read_sizes, [])

    def test_halfclose_drains_before_fin_and_allows_return_traffic(self):
        h = Harness("symmetric")
        self.addCleanup(h.relay.close)
        for client in h.clients.values():
            client.incoming.extend([b"request", b""])
        h.relay.pump(0)
        self.assertTrue(all(not backend.shutdowns for backend in h.backends.values()))
        h.clock.advance(.05)
        h.relay.pump(0)
        for backend in h.backends.values():
            self.assertEqual(backend.shutdowns[-1][1], b"request")
            backend.incoming.extend([b"response", b""])
        self.assertFalse(h.relay.complete)
        h.finish()
        for client in h.clients.values():
            self.assertEqual(client.written, b"response")

    def test_halfclose_missing_reverse_eof_hits_drain_deadline(self):
        h = Harness()
        for client in h.clients.values():
            client.incoming.extend([b"request", b""])
        h.relay.pump(0)
        h.relay.pump(0)
        h.clock.advance(5)
        with self.assertRaisesRegex(RelayError, "drain_timeout"):
            h.relay.pump(0)

    def _assert_failed_closed_without_reentry(self, h, code="io"):
        evidence = h.relay.report()
        self.assertEqual(evidence["status"], "failed")
        self.assertEqual(evidence["failure"]["code"], code)
        self.assertFalse(h.relay.complete)
        attempts = [sock.shutdown_attempts for sock in h.relay._owned]
        with self.assertRaisesRegex(RelayError, code):
            h.relay.pump(0)
        with self.assertRaises(RelayError):
            h.relay.assert_complete()
        h.relay.close()
        h.relay.close()
        self.assertEqual(h.relay.report(), evidence)
        self.assertEqual([sock.shutdown_attempts for sock in h.relay._owned], attempts)
        self.assertTrue(all(sock.closed == 1 for sock in h.relay._owned))
        self.assertEqual(h.selector.closed, 1)

    def test_drained_enotconn_is_recorded_for_either_or_both_halves(self):
        for role in ("owner", "observer"):
            for closed_halves in (("up",), ("down",), ("up", "down")):
                with self.subTest(role=role, closed_halves=closed_halves):
                    h = Harness()
                    self.addCleanup(h.relay.close)
                    for suffix in closed_halves:
                        h.relay._directions[role + "_" + suffix].sink.shutdown_error = OSError(
                            errno.ENOTCONN, "already closed")
                    h.fill()
                    evidence = h.finish()
                    for name, direction in h.relay._directions.items():
                        accepted = name in {role + "_" + suffix for suffix in closed_halves}
                        self.assertEqual(evidence["directions"][name]["terminalReason"],
                                         "already_closed_after_drain" if accepted else "shutdown_wr")
                        self.assertTrue(evidence["directions"][name]["writeHalfClosed"])
                        self.assertEqual(direction.sink.shutdown_attempts, 1)
                        self.assertEqual(len(direction.sink.shutdowns), 0 if accepted else 1)
                    self.assertTrue(h.relay.pump(0))
                    h.relay.close()
                    closed_evidence = h.relay.assert_complete()
                    self.assertEqual(closed_evidence["directions"], evidence["directions"])
                    self.assertTrue(h.relay.pump(0))
                    h.relay.close()
                    self.assertTrue(all(sock.closed == 1 for sock in h.relay._owned))
                    self.assertTrue(all(d.sink.shutdown_attempts == 1 for d in h.relay._directions.values()))
                    self.assertEqual(h.selector.closed, 1)

    def test_drained_enotconn_accepts_either_eof_order_independently_per_pair(self):
        for role in ("owner", "observer"):
            for first_suffix, last_suffix in (("up", "down"), ("down", "up")):
                with self.subTest(role=role, first_eof=first_suffix):
                    h = Harness()
                    self.addCleanup(h.relay.close)
                    h.fill(eof=False)
                    h.relay.pump(0)
                    h.relay.pump(0)
                    first = h.relay._directions[role + "_" + first_suffix]
                    last = h.relay._directions[role + "_" + last_suffix]
                    first.source.incoming.append(b"")
                    self.assertFalse(h.relay.pump(0))
                    self.assertTrue(first.eof and first.shutdown)
                    self.assertFalse(last.eof)
                    last.sink.shutdown_error = OSError(errno.ENOTCONN, "already closed")
                    last.source.incoming.append(b"")
                    self.assertFalse(h.relay.pump(0))
                    self.assertEqual(last.terminal_reason, "already_closed_after_drain")
                    self.assertEqual(first.terminal_reason, "shutdown_wr")
                    other = "observer" if role == "owner" else "owner"
                    self.assertFalse(h.relay._directions[other + "_up"].eof)
                    h.clients[other].incoming.append(b"")
                    h.backends[other].incoming.append(b"")
                    self.assertEqual(h.finish()["status"], "passed")

    def test_enotconn_rejects_pending_bytes_in_reverse_half(self):
        for suffix, reverse in (("up", "down"), ("down", "up")):
            with self.subTest(half=suffix):
                h = Harness()
                target = h.relay._directions["owner_" + suffix]
                pending = h.relay._directions["owner_" + reverse]
                target.sink.shutdown_error = OSError(errno.ENOTCONN, "pending reverse bytes")
                h.fill()
                h.relay.pump(0)
                pending.sink.writable = False
                with self.assertRaisesRegex(RelayError, "io"):
                    h.relay.pump(0)
                self.assertTrue(target.eof and pending.eof)
                self.assertTrue(pending.queue)
                self.assertGreater(pending.queued_bytes, 0)
                self.assertFalse(target.shutdown)
                self.assertIsNone(target.terminal_reason)
                self._assert_failed_closed_without_reentry(h)

    def test_enotconn_requires_reverse_eof_from_the_same_pair(self):
        for role in ("owner", "observer"):
            for suffix in ("up", "down"):
                with self.subTest(role=role, half=suffix):
                    h = Harness()
                    h.fill(eof=False)
                    h.relay.pump(0)
                    h.relay.pump(0)
                    target = h.relay._directions[role + "_" + suffix]
                    other = "observer" if role == "owner" else "owner"
                    h.clients[other].incoming.append(b"")
                    h.backends[other].incoming.append(b"")
                    target.source.incoming.append(b"")
                    target.sink.shutdown_error = OSError(errno.ENOTCONN, "missing reverse EOF")
                    with self.assertRaisesRegex(RelayError, "io"):
                        h.relay.pump(0)
                    self.assertTrue(h.relay._directions[other + "_up"].eof)
                    self.assertTrue(h.relay._directions[other + "_down"].eof)
                    self.assertIsNone(target.terminal_reason)
                    self._assert_failed_closed_without_reentry(h)

    def test_enotconn_rejects_incomplete_stream_proof_in_either_half(self):
        for suffix in ("up", "down"):
            for bad_suffix in ("up", "down"):
                for fault in ("queued_bytes", "buffered_bytes", "byte_count", "hash", "empty"):
                    with self.subTest(half=suffix, bad_half=bad_suffix, fault=fault):
                        h = Harness()
                        h.fill(eof=False)
                        h.relay.pump(0)
                        h.relay.pump(0)
                        target = h.relay._directions["owner_" + suffix]
                        bad = h.relay._directions["owner_" + bad_suffix]
                        if fault in ("queued_bytes", "buffered_bytes"):
                            setattr(bad, fault, 1)
                        elif fault == "byte_count":
                            bad.written_bytes -= 1
                        elif fault == "hash":
                            bad.write_hash.update(b"mismatch")
                        else:
                            bad.read_bytes = bad.written_bytes = 0
                            bad.read_hash = hashlib.sha256()
                            bad.write_hash = hashlib.sha256()
                        target.sink.shutdown_error = OSError(errno.ENOTCONN, "incomplete byte proof")
                        for sock in (*h.clients.values(), *h.backends.values()):
                            sock.incoming.append(b"")
                        with self.assertRaisesRegex(RelayError, "io"):
                            h.relay.pump(0)
                        self.assertTrue(target.eof and bad.eof)
                        self.assertFalse(target.shutdown)
                        self.assertIsNone(target.terminal_reason)
                        self._assert_failed_closed_without_reentry(h)

    def test_other_shutdown_errors_fail_even_after_verified_drain(self):
        for error in (errno.ECONNRESET, errno.EPIPE, errno.EINVAL, errno.EBADF, None):
            for suffix in ("up", "down"):
                with self.subTest(error=error, half=suffix):
                    h = Harness()
                    target = h.relay._directions["owner_" + suffix]
                    target.sink.shutdown_error = OSError(error, "shutdown failed")
                    h.fill()
                    with self.assertRaisesRegex(RelayError, "io"):
                        h.finish()
                    for direction in h.relay._directions.values():
                        self.assertTrue(direction.eof)
                        self.assertFalse(direction.queue)
                        self.assertEqual(direction.read_bytes, direction.written_bytes)
                        self.assertEqual(direction.read_hash.digest(), direction.write_hash.digest())
                    self.assertIsNone(target.terminal_reason)
                    self._assert_failed_closed_without_reentry(h)

    def test_enotconn_during_read_write_or_connect_still_fails(self):
        for stage in ("read", "write", "connect"):
            with self.subTest(stage=stage):
                h = Harness(connect_result=errno.EINPROGRESS if stage == "connect" else 0)
                h.fill()
                if stage == "read":
                    h.clients["owner"].incoming.appendleft(OSError(errno.ENOTCONN, "recv failed"))
                elif stage == "write":
                    h.backends["owner"].send_sizes.append(OSError(errno.ENOTCONN, "send failed"))
                else:
                    h.backends["owner"].connect_error = errno.ENOTCONN
                    h.backends["owner"].writable = True
                    h.backends["owner"].shutdown_error = OSError(errno.ENOTCONN, "never connected")
                code = "connect" if stage == "connect" else "io"
                with self.assertRaisesRegex(RelayError, code):
                    h.finish()
                if stage == "connect":
                    self.assertFalse(h.relay.report()["connections"]["owner"]["connected"])
                    self.assertEqual(h.clients["owner"].read_sizes, [])
                self.assertTrue(all(d.terminal_reason is None for d in h.relay._directions.values()))
                self._assert_failed_closed_without_reentry(h, code)

    def test_reset_read_write_and_shutdown_fail_closed(self):
        for stage in ("read", "write", "shutdown"):
            h = Harness()
            h.fill()
            if stage == "read":
                h.clients["owner"].incoming.appendleft(ConnectionResetError("reset"))
            elif stage == "write":
                h.backends["owner"].send_sizes.append(BrokenPipeError("reset"))
            else:
                h.backends["owner"].shutdown_error = ConnectionResetError("reset")
            with self.assertRaisesRegex(RelayError, "io"):
                h.finish()
            self.assertTrue(all(s.closed == 1 for s in h.relay._owned))
            self.assertEqual(h.selector.closed, 1)

    def test_extra_client_is_rejected_without_seventh_socket(self):
        h = Harness()
        outsider = FakeSocket(h.clock)
        h.listeners["owner"].accepts.append(outsider)
        with self.assertRaisesRegex(RelayError, "extra_client"):
            h.relay.pump(0)
        self.assertEqual(h.relay.report()["ownedSocketsHigh"], 6)
        self.assertEqual(outsider.closed, 0)
        self.assertEqual(len(h.listeners["owner"].accepts), 1)

    def test_cleanup_is_idempotent_and_does_not_touch_unowned_socket(self):
        h = Harness()
        unrelated = FakeSocket(h.clock)
        h.relay.close()
        h.relay.close()
        self.assertEqual(h.relay.report()["failure"]["code"], "early_close")
        self.assertTrue(all(s.closed == 1 for s in h.relay._owned))
        self.assertEqual(h.selector.closed, 1)
        self.assertEqual(unrelated.closed, 0)
        with self.assertRaises(RelayError):
            h.relay.assert_complete()

    def test_constructor_failure_closes_passed_listeners(self):
        for mismatch in ("profile", "address", "same_backend_port"):
            clock = Clock()
            factory = Factory(clock)
            listeners = reserve_listeners(socket_factory=factory)
            if mismatch == "address":
                listeners["owner"].local = ("0.0.0.0", 12345)
            elif mismatch == "same_backend_port":
                listeners["owner"].local = (LOOPBACK, 25565)
            with self.assertRaises(ValueError):
                StoneHingeRelay(BackendIdentity("nonce", 1234, 25565),
                                "unknown" if mismatch == "profile" else "transparent",
                                listeners=listeners, clock=clock)
            self.assertTrue(all(s.closed == 1 for s in listeners.values()))

    def test_reservation_failure_closes_first_listener(self):
        clock = Clock()
        first = FakeSocket(clock)
        attempts = 0
        def factory(*args):
            nonlocal attempts
            attempts += 1
            if attempts == 2:
                raise PermissionError("test denial")
            return first
        with self.assertRaises(PermissionError):
            reserve_listeners(socket_factory=factory)
        self.assertEqual(first.closed, 1)

    def test_backwards_clock_fails_closed(self):
        h = Harness()
        h.clock.advance(-1)
        with self.assertRaisesRegex(RelayError, "clock"):
            h.relay.pump(0)
        self.assertTrue(h.relay.closed)

    def test_empty_streams_cannot_pass_proof_assertion(self):
        h = Harness()
        h.fill(b"")
        with self.assertRaisesRegex(RelayError, "stream_mismatch"):
            h.relay.pump(0)
        with self.assertRaises(RelayError):
            h.relay.assert_complete()

    def test_offset_violation_invalidates_continuous_stream_evidence(self):
        h = Harness()
        h.fill()
        h.relay.pump(0)
        h.relay._directions["owner_up"].queue[0].offset += 1
        with self.assertRaisesRegex(RelayError, "offset"):
            h.relay.pump(0)
        self.assertFalse(h.relay.report()["directions"]["owner_up"]["offsetsContiguous"])


if __name__ == "__main__":
    unittest.main()
