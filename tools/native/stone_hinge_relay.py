"""Bounded, opaque TCP delay relay for the Stone Hinge two-client gate.

The supervisor validates the complete server-ready receipt and the owned host
process before calling BackendIdentity.from_ready. Reserve the two endpoints
once, give them to the clients, and retain them until relay.close(). A relay owns
at most six sockets: two listeners, two accepted clients, two backend sockets.
It never decodes game traffic, resolves a hostname, launches or kills processes.

Call pump() regularly from the supervisor's existing bounded process loop.
assert_complete() requires both clients and all four orderly drained streams;
an early close is a failed run. Reports contain exact aggregate counters and
continuous stream/event hashes, not payloads or an unbounded per-packet log.
"""
from __future__ import annotations

from collections import deque
from dataclasses import asdict, dataclass, field
import errno
import hashlib
import json
import math
import re
import selectors
import socket
import time
from types import MappingProxyType


LOOPBACK = "127.0.0.1"
ROLES = ("owner", "observer")
DIRECTION_NAMES = ("owner_up", "owner_down", "observer_up", "observer_down")
PROFILES = MappingProxyType({
    "transparent": (0, 0, 0, 0),
    "symmetric": (50, 50, 50, 50),
    "asymmetric": (100, 25, 25, 100),
})
MAX_EVIDENCE_BYTES = 16 * 1024 * 1024


class RelayError(RuntimeError):
    """The profile is invalid; the relay has already closed its owned sockets."""


def _positive_int(value, name, maximum):
    if isinstance(value, str) and re.fullmatch(r"[1-9][0-9]{0,18}", value):
        value = int(value)
    if type(value) is not int or not 1 <= value <= maximum:
        raise ValueError("Invalid " + name)
    return value


@dataclass(frozen=True)
class BackendIdentity:
    nonce: str
    host_pid: int
    backend_port: int

    def __post_init__(self):
        if not isinstance(self.nonce, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", self.nonce):
            raise ValueError("Invalid backend nonce")
        if type(self.host_pid) is not int or not 0 < self.host_pid <= 2**31 - 1:
            raise ValueError("Invalid host PID")
        if type(self.backend_port) is not int or not 0 < self.backend_port <= 65535:
            raise ValueError("Invalid backend port")

    @classmethod
    def from_ready(cls, ready, *, expected_nonce, expected_host_pid):
        """Bind to an already provenance-validated server-ready properties map.

        No address from the receipt is used: destinations are always literal
        IPv4 loopback. This does not replace supervisor PID/receipt validation.
        """
        pid = _positive_int(ready.get("pid"), "ready PID", 2**31 - 1)
        expected_pid = _positive_int(expected_host_pid, "expected host PID", 2**31 - 1)
        if (ready.get("nonce") != expected_nonce or pid != expected_pid
                or ready.get("role") != "host" or ready.get("suite") != "stone-hinge-peer"):
            raise ValueError("Server-ready backend identity mismatch")
        return cls(expected_nonce, pid, _positive_int(ready.get("port"), "ready port", 65535))


@dataclass(frozen=True)
class Limits:
    """Production ceilings; callers may only tighten them (useful in tests)."""
    read_bytes: int = 16 * 1024
    reads_per_pump: int = 4
    queue_bytes: int = 1024 * 1024
    queue_chunks: int = 1024
    total_queue_bytes: int = 4 * 1024 * 1024
    stream_bytes: int = 256 * 1024 * 1024
    total_stream_bytes: int = 1024 * 1024 * 1024
    oldest_seconds: float = 2.0
    backpressure_seconds: float = 2.0
    connect_seconds: float = 5.0
    drain_seconds: float = 5.0
    lateness_seconds: float = 0.250

    def __post_init__(self):
        for key, value in asdict(self).items():
            maximum = self.__dataclass_fields__[key].default
            if (isinstance(value, bool) or not isinstance(value, (int, float))
                    or not math.isfinite(value) or not 0 < value <= maximum
                    or (isinstance(maximum, int) and type(value) is not int)):
                raise ValueError("Relay limit must be positive and cannot exceed ceiling: " + key)


def reserve_listeners(*, socket_factory=socket.socket):
    """Create exactly two retained IPv4 loopback listeners, never probe/rebind."""
    listeners = {}
    try:
        for role in ROLES:
            listener = socket_factory(socket.AF_INET, socket.SOCK_STREAM)
            listeners[role] = listener
            listener.setblocking(False)
            listener.bind((LOOPBACK, 0))
            listener.listen(1)
        return listeners
    except BaseException:
        for listener in listeners.values():
            try:
                listener.close()
            except OSError:
                pass
        raise


@dataclass
class _Chunk:
    data: bytes
    offset: int
    received: float
    release: float
    sent: int = 0


@dataclass
class _Direction:
    name: str
    source: object
    sink: object
    delay: float
    queue: deque = field(default_factory=deque)
    queued_bytes: int = 0
    buffered_bytes: int = 0
    queue_high_bytes: int = 0
    queue_high_chunks: int = 0
    read_bytes: int = 0
    written_bytes: int = 0
    read_chunks: int = 0
    write_calls: int = 0
    read_hash: object = field(default_factory=hashlib.sha256)
    write_hash: object = field(default_factory=hashlib.sha256)
    event_hash: object = field(default_factory=hashlib.sha256)
    eof: bool = False
    shutdown: bool = False
    terminal_reason: str | None = None
    blocked_since: float | None = None
    backpressure_events: int = 0
    backpressure_max: float = 0.0
    residence_sum: float = 0.0
    residence_min: float | None = None
    residence_max: float = 0.0
    lateness_max: float = 0.0


@dataclass
class _Pair:
    role: str
    client: object
    backend: object
    started: float
    connected: bool = False
    eof_at: float | None = None
    endpoints: dict = field(default_factory=dict)


class StoneHingeRelay:
    def __init__(self, identity, profile, *, listeners, limits=None,
                 clock=time.monotonic, socket_factory=socket.socket,
                 selector_factory=selectors.DefaultSelector):
        """Take ownership of the supplied listeners, including on init failure."""
        self._owned = list(listeners.values())
        self._selector = None
        self._closed = False
        self._failure = None
        self._cleanup_errors = []
        self._pairs = {}
        self._directions = {}
        self._registered = {}
        self._endpoints = {}
        self._clock = clock
        self._last_time = None
        self._socket_factory = socket_factory
        self._identity = identity
        self._profile = profile
        self._listeners = dict(listeners)
        self._limits = limits if limits is not None else Limits()
        self._total_read = 0
        self._total_queue = 0
        self._total_buffered = 0
        self._queue_high = 0
        self._socket_high = len(self._owned)
        try:
            if type(identity) is not BackendIdentity or profile not in PROFILES:
                raise ValueError("A validated immutable backend identity and fixed profile are required")
            if type(self._limits) is not Limits:
                raise ValueError("Invalid relay limits")
            if set(listeners) != set(ROLES) or len({id(s) for s in self._owned}) != 2:
                raise ValueError("Exactly two distinct owner/observer listeners are required")
            ports = set()
            fds = set()
            for role, listener in listeners.items():
                address = listener.getsockname()
                if (listener.family != socket.AF_INET
                        or listener.getsockopt(socket.SOL_SOCKET, socket.SO_TYPE) != socket.SOCK_STREAM
                        or not listener.getsockopt(socket.SOL_SOCKET, socket.SO_ACCEPTCONN)
                        or address[0] != LOOPBACK or not 0 < address[1] <= 65535
                        or address[1] == identity.backend_port or address[1] in ports
                        or listener.fileno() < 0 or listener.fileno() in fds):
                    raise ValueError("Listeners must be distinct retained literal IPv4 loopback TCP endpoints")
                ports.add(address[1])
                fds.add(listener.fileno())
                listener.setblocking(False)
                self._endpoints[role] = (LOOPBACK, address[1])
            self._started = self._now()
            self._selector = selector_factory()
            self._sync(self._started)
        except BaseException:
            self._cleanup()
            raise

    @property
    def identity(self):
        return self._identity

    @property
    def profile(self):
        return self._profile

    @property
    def endpoints(self):
        return dict(self._endpoints)

    @property
    def complete(self):
        return (not self._failure and not self._cleanup_errors and len(self._directions) == 4
                and all(d.eof and d.shutdown and not d.queue for d in self._directions.values()))

    @property
    def closed(self):
        return self._closed

    def _now(self):
        now = self._clock()
        if not math.isfinite(now) or (self._last_time is not None and now < self._last_time):
            self._fail("clock", "monotonic clock moved backwards or became non-finite")
        self._last_time = now
        return now

    def _fail(self, code, detail):
        if self._failure is None:
            self._failure = {"code": code, "detail": detail}
        self._cleanup()
        raise RelayError(code + ": " + detail)

    def _cleanup(self):
        if self._closed:
            return
        self._closed = True
        if self._selector is not None:
            try:
                self._selector.close()
            except Exception as error:
                self._cleanup_errors.append(type(error).__name__)
        for owned in dict.fromkeys(self._owned):
            try:
                owned.close()
            except Exception as error:
                self._cleanup_errors.append(type(error).__name__)
        self._registered.clear()

    def close(self):
        """Idempotent cleanup of owned descriptors only; early closure fails."""
        if not self._closed and not self.complete and self._failure is None:
            self._failure = {"code": "early_close", "detail": "closed before four orderly drained EOFs"}
        self._cleanup()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()

    def _capacity(self, direction):
        if len(direction.queue) >= self._limits.queue_chunks:
            return 0
        return min(self._limits.read_bytes,
                   self._limits.queue_bytes - direction.buffered_bytes,
                   self._limits.total_queue_bytes - self._total_buffered)

    def _sync(self, now):
        events = {listener: selectors.EVENT_READ for listener in self._listeners.values()}
        for pair in self._pairs.values():
            if not pair.connected:
                events[pair.backend] = selectors.EVENT_WRITE
                continue
            for suffix in ("up", "down"):
                direction = self._directions[pair.role + "_" + suffix]
                if not direction.eof and self._capacity(direction) > 0:
                    events[direction.source] = events.get(direction.source, 0) | selectors.EVENT_READ
                if direction.queue and direction.queue[0].release <= now:
                    events[direction.sink] = events.get(direction.sink, 0) | selectors.EVENT_WRITE
        for sock in list(self._registered):
            if sock not in events:
                self._selector.unregister(sock)
                del self._registered[sock]
        for sock, mask in events.items():
            if sock not in self._registered:
                self._selector.register(sock, mask)
            elif self._registered[sock] != mask:
                self._selector.modify(sock, mask)
            self._registered[sock] = mask

    def _accept(self, role, now):
        # Do not accept an extra descriptor: even rejection must stay <=6 sockets.
        if role in self._pairs:
            self._fail("extra_client", role + " listener became readable after its one client")
        try:
            client, address = self._listeners[role].accept()
        except BlockingIOError:
            return
        self._owned.append(client)
        self._socket_high = max(self._socket_high, len(self._owned))
        if address[0] != LOOPBACK or client.family != socket.AF_INET:
            self._fail("client_address", "accepted client is not literal IPv4 loopback")
        client.setblocking(False)
        backend = self._socket_factory(socket.AF_INET, socket.SOCK_STREAM)
        self._owned.append(backend)
        self._socket_high = max(self._socket_high, len(self._owned))
        if self._socket_high > 6:
            self._fail("socket_cap", "more than six owned sockets")
        backend.setblocking(False)
        pair = _Pair(role, client, backend, now)
        pair.endpoints = {"clientLocal": self._endpoint(client.getsockname()),
                          "clientRemote": self._endpoint(client.getpeername())}
        if tuple(pair.endpoints["clientLocal"]) != self._endpoints[role] or tuple(pair.endpoints["clientRemote"]) != address:
            self._fail("client_endpoint", role + " accepted endpoint identity mismatch")
        self._pairs[role] = pair
        offset = 2 * ROLES.index(role)
        for suffix, source, sink, delay in (
                ("up", client, backend, PROFILES[self._profile][offset]),
                ("down", backend, client, PROFILES[self._profile][offset + 1])):
            name = role + "_" + suffix
            self._directions[name] = _Direction(name, source, sink, delay / 1000.0)
        result = backend.connect_ex((LOOPBACK, self._identity.backend_port))
        if result == 0:
            self._connected(pair)
        elif result not in (errno.EINPROGRESS, errno.EWOULDBLOCK, errno.EALREADY, errno.EINTR):
            self._fail("connect", role + " connect errno " + str(result))

    def _endpoint(self, value):
        if (not isinstance(value, tuple) or len(value) != 2 or value[0] != LOOPBACK
                or type(value[1]) is not int or not 0 < value[1] <= 65535):
            self._fail("endpoint", "owned socket endpoint is not literal IPv4 loopback")
        return list(value)

    def _connected(self, pair):
        error = pair.backend.getsockopt(socket.SOL_SOCKET, socket.SO_ERROR)
        if error or pair.backend.getpeername() != (LOOPBACK, self._identity.backend_port):
            self._fail("connect", pair.role + " backend endpoint/error mismatch: " + str(error))
        pair.endpoints.update(backendLocal=self._endpoint(pair.backend.getsockname()),
                              backendRemote=self._endpoint(pair.backend.getpeername()))
        pair.connected = True

    def _event(self, direction, kind, offset, count, now, release):
        # Fixed-sized streaming evidence: every event contributes, none retained.
        record = [kind, offset, count, now, release]
        direction.event_hash.update(json.dumps(record, separators=(",", ":"), allow_nan=False).encode() + b"\n")

    def _read(self, direction, now):
        for _ in range(self._limits.reads_per_pump):
            capacity = self._capacity(direction)
            if direction.eof or capacity <= 0:
                break
            # At the transfer limit allow only a one-byte sentinel to distinguish
            # a legal exact-limit EOF from a stream that exceeds its ceiling.
            count = min(capacity, max(1, self._limits.stream_bytes - direction.read_bytes),
                        max(1, self._limits.total_stream_bytes - self._total_read))
            try:
                data = direction.source.recv(count)
            except BlockingIOError:
                break
            now = self._now()
            if not data:
                direction.eof = True
                pair = self._pairs[direction.name.split("_")[0]]
                if pair.eof_at is None:
                    pair.eof_at = now
                self._event(direction, "eof", direction.read_bytes, 0, now, now)
                break
            offset = direction.read_bytes
            direction.read_bytes += len(data)
            self._total_read += len(data)
            direction.read_hash.update(data)
            direction.read_chunks += 1
            self._event(direction, "read", offset, len(data), now, now + direction.delay)
            if (direction.read_bytes > self._limits.stream_bytes
                    or self._total_read > self._limits.total_stream_bytes):
                self._fail("stream_cap", direction.name + " exceeds transfer ceiling")
            if len(data) > capacity:
                self._fail("queue_cap", "recv exceeded requested bounded capacity")
            direction.queue.append(_Chunk(data, offset, now, now + direction.delay))
            direction.queued_bytes += len(data)
            direction.buffered_bytes += len(data)
            self._total_queue += len(data)
            self._total_buffered += len(data)
            direction.queue_high_bytes = max(direction.queue_high_bytes, direction.buffered_bytes)
            direction.queue_high_chunks = max(direction.queue_high_chunks, len(direction.queue))
            self._queue_high = max(self._queue_high, self._total_buffered)

    def _write(self, direction):
        # Four writes as well as four reads keep any one ready stream bounded.
        for _ in range(self._limits.reads_per_pump):
            now = self._now()
            if not direction.queue or direction.queue[0].release > now:
                return
            chunk = direction.queue[0]
            if now - chunk.release > self._limits.lateness_seconds:
                self._fail("release_late", direction.name + " missed release deadline")
            if chunk.offset + chunk.sent != direction.written_bytes:
                self._fail("offset", direction.name + " noncontiguous write offset")
            remaining = memoryview(chunk.data)[chunk.sent:]
            try:
                written = direction.sink.send(remaining)
            except BlockingIOError:
                return
            finished = self._now()
            if not 0 < written <= len(remaining):
                self._fail("write", direction.name + " invalid/zero socket write")
            direction.write_hash.update(remaining[:written])
            self._event(direction, "write", direction.written_bytes, written, finished, chunk.release)
            direction.written_bytes += written
            direction.write_calls += 1
            direction.queued_bytes -= written
            self._total_queue -= written
            chunk.sent += written
            residence = finished - chunk.received
            direction.residence_sum += residence * written
            direction.residence_max = max(direction.residence_max, residence)
            direction.residence_min = residence if direction.residence_min is None else min(direction.residence_min, residence)
            direction.lateness_max = max(direction.lateness_max, finished - chunk.release)
            if chunk.sent == len(chunk.data):
                direction.queue.popleft()
                direction.buffered_bytes -= len(chunk.data)
                self._total_buffered -= len(chunk.data)
            if finished - chunk.release > self._limits.lateness_seconds:
                self._fail("release_late", direction.name + " socket write finished after release deadline")

    def _maintain(self, now):
        for pair in self._pairs.values():
            if not pair.connected and now - pair.started >= self._limits.connect_seconds:
                self._fail("connect_timeout", pair.role + " backend connect exceeded deadline")
            directions = [self._directions[pair.role + "_" + suffix] for suffix in ("up", "down")]
            if (pair.eof_at is not None and now - pair.eof_at >= self._limits.drain_seconds
                    and not all(d.eof and d.shutdown and not d.queue for d in directions)):
                self._fail("drain_timeout", pair.role + " did not finish both halves after EOF")
            for direction in directions:
                if direction.queue:
                    head = direction.queue[0]
                    if now - head.received >= self._limits.oldest_seconds:
                        self._fail("oldest_timeout", direction.name + " oldest queued byte exceeded deadline")
                    if now - head.release > self._limits.lateness_seconds:
                        self._fail("release_late", direction.name + " queued byte missed release deadline")
                blocked = not direction.eof and self._capacity(direction) <= 0
                if blocked and direction.blocked_since is None:
                    direction.blocked_since = now
                    direction.backpressure_events += 1
                if direction.blocked_since is not None:
                    elapsed = now - direction.blocked_since
                    direction.backpressure_max = max(direction.backpressure_max, elapsed)
                    if blocked and elapsed >= self._limits.backpressure_seconds:
                        self._fail("backpressure_timeout", direction.name + " remained backpressured")
                    if not blocked:
                        direction.blocked_since = None
                if pair.connected and direction.eof and not direction.queue and not direction.shutdown:
                    try:
                        direction.sink.shutdown(socket.SHUT_WR)
                    except OSError as error:
                        # A connected peer can finish closing before SHUT_WR.
                        # Only actual EOF and complete byte proof in BOTH
                        # directions of this same pair establish a clean drain.
                        if error.errno != errno.ENOTCONN or not all(
                                d.eof and not d.queue and d.queued_bytes == 0
                                and d.buffered_bytes == 0 and d.read_bytes > 0
                                and d.read_bytes == d.written_bytes
                                and d.read_hash.digest() == d.write_hash.digest()
                                for d in directions):
                            raise
                        direction.terminal_reason = "already_closed_after_drain"
                    else:
                        direction.terminal_reason = "shutdown_wr"
                    direction.shutdown = True
        if self.complete:
            for direction in self._directions.values():
                if (direction.read_bytes == 0 or direction.read_bytes != direction.written_bytes
                        or direction.read_hash.digest() != direction.write_hash.digest()):
                    self._fail("stream_mismatch", direction.name + " empty or mismatched continuous stream")

    def _wait_seconds(self, now, timeout):
        deadlines = [now + timeout]
        for pair in self._pairs.values():
            if not pair.connected:
                deadlines.append(pair.started + self._limits.connect_seconds)
            if pair.eof_at is not None:
                directions = [self._directions[pair.role + "_" + suffix] for suffix in ("up", "down")]
                if not all(d.eof and d.shutdown and not d.queue for d in directions):
                    deadlines.append(pair.eof_at + self._limits.drain_seconds)
        for direction in self._directions.values():
            if direction.queue:
                chunk = direction.queue[0]
                deadlines.extend((chunk.received + self._limits.oldest_seconds,
                                  chunk.release + self._limits.lateness_seconds))
                if chunk.release > now:
                    deadlines.append(chunk.release)
            if direction.blocked_since is not None:
                deadlines.append(direction.blocked_since + self._limits.backpressure_seconds)
        return max(0.0, min(deadlines) - now)

    def pump(self, timeout=0.05):
        """Run one bounded selector iteration; return whether all streams ended.

        Delays are monotonic deadlines. The timeout is shortened for each next
        release/deadline and is never an unconditional sleep or a per-packet wait.
        Any failure closes all owned sockets before raising RelayError.
        """
        if self._failure:
            raise RelayError(self._failure["code"] + ": " + self._failure["detail"])
        if self._closed:
            return self.complete
        if isinstance(timeout, bool) or not isinstance(timeout, (int, float)) or not math.isfinite(timeout) or not 0 <= timeout <= 1:
            self._fail("timeout", "pump timeout must be between zero and one second")
        try:
            now = self._now()
            self._maintain(now)
            self._sync(now)
            ready = self._selector.select(self._wait_seconds(now, timeout))
            now = self._now()
            self._maintain(now)
            masks = {key.fileobj: mask for key, mask in ready}
            for role, listener in self._listeners.items():
                if masks.get(listener, 0) & selectors.EVENT_READ:
                    self._accept(role, now)
            for pair in self._pairs.values():
                if not pair.connected and masks.get(pair.backend, 0) & selectors.EVENT_WRITE:
                    self._connected(pair)
            for direction in self._directions.values():
                if self._pairs[direction.name.split("_")[0]].connected:
                    if masks.get(direction.source, 0) & selectors.EVENT_READ:
                        self._read(direction, now)
                    if masks.get(direction.sink, 0) & selectors.EVENT_WRITE:
                        self._write(direction)
            self._maintain(self._now())
            self._sync(self._last_time)
            return self.complete
        except RelayError:
            raise
        except (OSError, ValueError, KeyError) as error:
            self._fail("io", type(error).__name__ + ": " + str(error)[:256])

    def report(self):
        """Fixed-size evidence; hashes and offset counters span the whole stream."""
        streams = {}
        for name in DIRECTION_NAMES:
            direction = self._directions.get(name)
            if direction is None:
                streams[name] = {"connected": False}
                continue
            streams[name] = {
                "connected": self._pairs[name.split("_")[0]].connected,
                "delayMs": direction.delay * 1000,
                "readBytes": direction.read_bytes, "writtenBytes": direction.written_bytes,
                "readOffsetRange": [0, direction.read_bytes], "writeOffsetRange": [0, direction.written_bytes],
                "offsetsContiguous": not self._failure or self._failure["code"] != "offset",
                "readSha256": direction.read_hash.hexdigest(), "writeSha256": direction.write_hash.hexdigest(),
                "eventsSha256": direction.event_hash.hexdigest(),
                "readChunks": direction.read_chunks, "writeCalls": direction.write_calls,
                "queuedBytes": direction.queued_bytes, "bufferedBytes": direction.buffered_bytes,
                "queueHighBytes": direction.queue_high_bytes,
                "queueHighChunks": direction.queue_high_chunks,
                "eof": direction.eof, "writeHalfClosed": direction.shutdown,
                "terminalReason": direction.terminal_reason,
                "backpressureEvents": direction.backpressure_events,
                "backpressureMaxSeconds": direction.backpressure_max,
                "residenceMinSeconds": direction.residence_min,
                "residenceMaxSeconds": direction.residence_max,
                "residenceByteMeanSeconds": direction.residence_sum / direction.written_bytes if direction.written_bytes else None,
                "releaseLatenessMaxSeconds": direction.lateness_max,
            }
        result = {"schemaVersion": 1, "status": "failed" if self._failure or self._cleanup_errors else "passed" if self.complete else "running",
                  "identity": asdict(self._identity), "profile": self._profile,
                  "delaysMs": dict(zip(DIRECTION_NAMES, PROFILES[self._profile])),
                  "ownerPort": self._endpoints["owner"][1], "observerPort": self._endpoints["observer"][1],
                  "backendAddress": LOOPBACK, "limits": asdict(self._limits),
                  "totalReadBytes": self._total_read, "totalQueuedBytes": self._total_queue,
                  "totalBufferedBytes": self._total_buffered,
                  "queueHighBytes": self._queue_high, "ownedSocketsHigh": self._socket_high,
                  "closed": self._closed, "failure": self._failure, "cleanupErrors": list(self._cleanup_errors),
                  "directions": streams,
                  "connections": {role: {**pair.endpoints, "connected": pair.connected}
                                  for role, pair in self._pairs.items()}}
        if len(json.dumps(result, separators=(",", ":"), allow_nan=False).encode()) > MAX_EVIDENCE_BYTES:
            self._fail("evidence_cap", "aggregate profile evidence exceeded 16 MiB")
        return result

    def assert_complete(self):
        """Require orderly completion plus identical, nonempty byte streams."""
        if self._failure or self._cleanup_errors or not self.complete:
            raise RelayError("Relay did not complete cleanly: " + str(self._failure or self._cleanup_errors or "incomplete"))
        for direction in self._directions.values():
            if (direction.read_bytes == 0 or direction.read_bytes != direction.written_bytes
                    or direction.read_hash.digest() != direction.write_hash.digest()):
                self._fail("stream_mismatch", direction.name + " empty or mismatched continuous stream")
        return self.report()
